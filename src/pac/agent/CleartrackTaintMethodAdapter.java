package pac.agent;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import pac.agent.hierarchy.ClassHierarchy;
import pac.org.objectweb.asm.Handle;
import pac.org.objectweb.asm.Label;
import pac.org.objectweb.asm.Opcodes;
import pac.org.objectweb.asm.Type;
import pac.org.objectweb.asm.tree.AbstractInsnNode;
import pac.org.objectweb.asm.tree.FieldInsnNode;
import pac.org.objectweb.asm.tree.InsnList;
import pac.org.objectweb.asm.tree.InsnNode;
import pac.org.objectweb.asm.tree.LabelNode;
import pac.org.objectweb.asm.tree.LdcInsnNode;
import pac.org.objectweb.asm.tree.MethodInsnNode;
import pac.org.objectweb.asm.tree.MethodNode;
import pac.org.objectweb.asm.tree.TryCatchBlockNode;
import pac.org.objectweb.asm.tree.TypeInsnNode;
import pac.org.objectweb.asm.tree.VarInsnNode;
import pac.org.objectweb.asm.tree.analysis.BasicValue;
import pac.org.objectweb.asm.tree.analysis.Frame;
import pac.util.Ansi;
import pac.util.AsmUtils;
import static pac.util.AsmUtils.FIELD_SUFFIX;
import static pac.util.AsmUtils.TRUSTED_OPCODE;
import static pac.util.AsmUtils.UNKNOWN_OPCODE;
import pac.util.Ret;

/**
 * This is used by {@code CleartrackTaintClassAdapter} to translate a
 * {@code MethodNode} into one that will be instrumented upon visiting the
 * instructions. The instrumented copy will contain new type signatures that
 * will allow for the new primitive types, as well as instrumented instructions
 * that will push/pop primitive taint to/from the stack.
 * 
 * @author jeikenberry
 */
public class CleartrackTaintMethodAdapter extends MethodNode {
    public static final ClassHierarchy hierarchy = ClassHierarchy.getInstance();

    /** old param reg to new param reg map */
    private Map<Integer, Integer> paramMap;

    /** new param local to shadow taint map */
    private Map<Integer, Integer> localTaintMap;

    private MethodNode originalMethod;
    private Frame<BasicValue>[] frames;

    private String owner;

    private int varOffset, instOffset, retObjReg, nextFreeReg, ownerAccess;
    private boolean isStatic, inJdk, dynamicMode;

    private static int removeVarargs(int access) {
        if ((access & Opcodes.ACC_VARARGS) == Opcodes.ACC_VARARGS)
            return access - Opcodes.ACC_VARARGS;
        return access;
    }

    public CleartrackTaintMethodAdapter(String owner, int ownerAccess, Frame<BasicValue>[] frames, MethodNode methodNode,
                                       boolean dynamicMode) {
        super(Opcodes.ASM5, removeVarargs(methodNode.access), methodNode.name, toPrimitiveDesc(methodNode.desc),
                CleartrackSignatureWriter.instrumentSignature(methodNode.signature),
                methodNode.exceptions.toArray(new String[0]));

        this.inJdk = hierarchy.isJdkClass(owner);
        this.dynamicMode = dynamicMode;

        this.frames = frames;

        this.originalMethod = methodNode;
        this.owner = owner;
        this.ownerAccess = ownerAccess;

        varOffset = 0;
        instOffset = 0;
        paramMap = new LinkedHashMap<Integer, Integer>();
        localTaintMap = new LinkedHashMap<Integer, Integer>();

        isStatic = AsmUtils.hasModifiers(methodNode.access, Opcodes.ACC_STATIC);
        int reg = isStatic ? 0 : 1;
        Type[] argTypes = Type.getArgumentTypes(methodNode.desc);
        for (int i = 0; i < argTypes.length; i++) {
            Type newArgType = AsmUtils.toPrimitiveType(argTypes[i]);
            paramMap.put(reg, reg + varOffset);
            if (newArgType != null) {
                switch (newArgType.getSort()) {
                case Type.INT:
                    varOffset++;
                    break;
                }
            }
            reg++;
            if (argTypes[i].getSize() == 2)
                reg++;
        }
        retObjReg = reg + varOffset;
        varOffset++;
        // We could end up with verify errors in some cases if we don't
        // take the max of maxLocals and reg (i.e. synchronized native
        // methods).  In this case, maxLocals will be zero, and since we
        // add code to synchronize prior to invoking the original native
        // method, we will have potentially stomped over the value of a
        // parameter.
        nextFreeReg = Math.max(methodNode.maxLocals, reg) + varOffset;
    }

    public static String toPrimitiveDesc(String desc) {
        Type returnType = Type.getReturnType(desc);

        Type primReturnType = AsmUtils.toPrimitiveArrayType(returnType);
        if (primReturnType != null)
            returnType = primReturnType;
        List<Type> newTypes = new LinkedList<Type>();
        Type[] argTypes = Type.getArgumentTypes(desc);
        for (int i = 0; i < argTypes.length; i++) {
            Type newArgType = AsmUtils.toPrimitiveType(argTypes[i]);
            if (newArgType == null) {
                newTypes.add(argTypes[i]);
            } else {
                switch (newArgType.getSort()) {
                case Type.INT:
                    newTypes.add(argTypes[i]);
                    break;
                }
                newTypes.add(newArgType);
            }
        }
        newTypes.add(Type.getType("Lpac/util/Ret;"));
        return Type.getMethodDescriptor(returnType, newTypes.toArray(new Type[0]));
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        // update the local register
        boolean isParam = paramMap.containsKey(index);
        if (!isStatic && index == 0) {
            // the "this" var should never change.
        } else if (isParam) {
            index = paramMap.get(index);
        } else {
            index += varOffset;
            if (!localTaintMap.containsKey(index))
                localTaintMap.put(index, nextFreeReg++);
        }

        // add the local
        super.visitLocalVariable(name, desc, signature, start, end, index);

        // add an additional taint local in the case of a primitive local type
        Type origType = Type.getType(desc);
        Type primType = AsmUtils.toPrimitiveType(origType);
        if (primType != null) {
            switch (primType.getSort()) {
            case Type.INT:
                // varOffset++;
                int taintIndex = isParam ? index + origType.getSize() : localTaintMap.get(index);
                super.visitLocalVariable(name + FIELD_SUFFIX, primType.getDescriptor(), signature, start, end,
                                         taintIndex);
                break;
            }
        }

    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        int size = 1;
        boolean loadPrimitive = false, storePrimitive = false;
        switch (opcode) {
        case Opcodes.LLOAD:
        case Opcodes.DLOAD:
            size = 2;
        case Opcodes.ILOAD:
        case Opcodes.FLOAD:
            loadPrimitive = true;
            break;
        case Opcodes.LSTORE:
        case Opcodes.DSTORE:
            size = 2;
        case Opcodes.ISTORE:
        case Opcodes.FSTORE:
            storePrimitive = true;
            break;
        }

        boolean isParam = paramMap.containsKey(var);
        if (!isStatic && var == 0) {
            // the "this" var should never change.
        } else if (isParam) {
            isParam = isReallyParameter(opcode, var);
            // If the type of parameter does not line up with the type of
            // store/load, then treat this parameter as a local.
            if (!isParam) {
                Ansi.warn("var %s is not really a type specified by (%s) in %s%s at offset %s", owner, var, opcode,
                          originalMethod.name, originalMethod.desc, varOffset);
                if (!localTaintMap.containsKey(var))
                    localTaintMap.put(var, nextFreeReg++);
            } else {
                var = paramMap.get(var);
            }
        } else {
            var += varOffset;
            if ((loadPrimitive || storePrimitive) && !localTaintMap.containsKey(var))
                localTaintMap.put(var, nextFreeReg++);
        }

        if (loadPrimitive) {
            super.visitVarInsn(opcode, var);
            super.visitVarInsn(Opcodes.ILOAD, isParam ? var + size : localTaintMap.get(var));
        } else if (storePrimitive) {
            super.visitVarInsn(Opcodes.ISTORE, isParam ? var + size : localTaintMap.get(var));
            super.visitVarInsn(opcode, var);
        } else {
            super.visitVarInsn(opcode, var);
        }

        instOffset++;
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        boolean isParam = paramMap.containsKey(var);
        if (isParam) {
            var = paramMap.get(var);
        } else {
            var += varOffset;
            if (!localTaintMap.containsKey(var))
                localTaintMap.put(var, nextFreeReg++);
        }
        super.visitIincInsn(var, increment);
        instOffset++;
    }

    /**
     * This method should only be called on load/store instructions, where var
     * refers to a valid parameter register.
     * 
     * @param opcode
     *            of the store/load instruction
     * @param var
     *            register of the store/load
     * @return true iff the instruction at opcode matches with the respective
     *         argument in the descriptor.
     */
    private boolean isReallyParameter(int opcode, int var) {
        Type[] types = Type.getArgumentTypes(originalMethod.desc);
        int idx = var;
        int first = isStatic ? 0 : 1;
        int paramSort = -1;
        for (int i = 0; i < types.length; i++) {
            if (idx == first) {
                paramSort = types[i].getSort();
                break;
            }
            switch (types[i].getSort()) {
            case Type.LONG:
            case Type.DOUBLE:
                idx -= 2;
                break;
            default:
                idx -= 1;
                break;
            }
        }
        if (paramSort < 0)
            throw new RuntimeException("malformed method descriptor: " + originalMethod.desc);
        switch (opcode) {
        case Opcodes.ILOAD:
        case Opcodes.ISTORE:
            return paramSort == Type.BOOLEAN || paramSort == Type.BYTE || paramSort == Type.SHORT
                    || paramSort == Type.CHAR || paramSort == Type.INT;
        case Opcodes.FLOAD:
        case Opcodes.FSTORE:
            return paramSort == Type.FLOAT;
        case Opcodes.LLOAD:
        case Opcodes.LSTORE:
            return paramSort == Type.LONG;
        case Opcodes.DLOAD:
        case Opcodes.DSTORE:
            return paramSort == Type.DOUBLE;
        default:
            return paramSort == Type.OBJECT || paramSort == Type.ARRAY;
        }
    }

    private Type toPrimitiveType(int operand) {
        switch (operand) {
        case Opcodes.T_BOOLEAN:
            return Type.getObjectType("pac/wrap/BooleanArrayTaint");
        case Opcodes.T_BYTE:
            return Type.getObjectType("pac/wrap/ByteArrayTaint");
        case Opcodes.T_CHAR:
            return Type.getObjectType("pac/wrap/CharArrayTaint");
        case Opcodes.T_DOUBLE:
            return Type.getObjectType("pac/wrap/DoubleArrayTaint");
        case Opcodes.T_FLOAT:
            return Type.getObjectType("pac/wrap/FloatArrayTaint");
        case Opcodes.T_INT:
            return Type.getObjectType("pac/wrap/IntArrayTaint");
        case Opcodes.T_LONG:
            return Type.getObjectType("pac/wrap/LongArrayTaint");
        case Opcodes.T_SHORT:
            return Type.getObjectType("pac/wrap/ShortArrayTaint");
        }
        return null;
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        switch (opcode) {
        case Opcodes.NEWARRAY:
            Type arrayType = toPrimitiveType(operand);
            super.visitMethodInsn(Opcodes.INVOKESTATIC, arrayType.getInternalName(),
                                  shouldCheckOverflow(opcode) ? "newArray" : "newArray_noCheck",
                                  Type.getMethodDescriptor(arrayType, Type.INT_TYPE, Type.INT_TYPE), false);
            break;
        default:
            super.visitIntInsn(opcode, operand); // bipush or sipush
            super.visitInsn(TRUSTED_OPCODE); // push trust constant;
            break;
        }
        instOffset++;
    }

    @Override
    public void visitLdcInsn(Object cst) {
        super.visitLdcInsn(cst);
        if (cst instanceof Number || cst instanceof Boolean || cst instanceof Character) {
            super.visitInsn(TRUSTED_OPCODE); // add trust instruction
        } else if (cst instanceof String) { // trust string constants
            super.visitMethodInsn(Opcodes.INVOKESTATIC, "pac/util/TaintUtils", "trustConstant",
                                  "(Ljava/lang/String;)Ljava/lang/String;", false);
        }
        instOffset++;
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        Type objType = Type.getObjectType(type);
        if (AsmUtils.isPrimitiveArrayType(objType))
            type = AsmUtils.toPrimitiveType(objType).getInternalName();

        switch (opcode) {
        case Opcodes.ANEWARRAY:
            if (!shouldCheckOverflow(opcode)) {
                super.visitInsn(Opcodes.POP);
            } else {
                visitNumberInsn(0);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "pac/wrap/ArrayTaint", "validateNewArrayLength", "(III)I",
                                      false);
            }
            super.visitTypeInsn(opcode, type);
            break;
        case Opcodes.INSTANCEOF:
            super.visitTypeInsn(opcode, type);
            super.visitInsn(TRUSTED_OPCODE); // push trust constant
            break;
        case Opcodes.NEW:
            if (hierarchy.isDangerousClass(objType))
                type = ClassHierarchy.getDangerousSublassName(type);
        default: // must be a CHECKCAST instruction
            super.visitTypeInsn(opcode, type);
            break;
        }
        instOffset++;
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        boolean noCheck = !shouldCheckOverflow(opcode);
        if (noCheck) {
            switch (opcode) {
            case Opcodes.IFEQ:
            case Opcodes.IFNE:
            case Opcodes.IFLT:
            case Opcodes.IFGE:
            case Opcodes.IFGT:
            case Opcodes.IFLE:
                super.visitInsn(Opcodes.POP);
                break;
            case Opcodes.IF_ICMPEQ:
            case Opcodes.IF_ICMPNE:
            case Opcodes.IF_ICMPLT:
            case Opcodes.IF_ICMPGE:
            case Opcodes.IF_ICMPGT:
            case Opcodes.IF_ICMPLE:
                super.visitInsn(Opcodes.POP);
                super.visitInsn(Opcodes.SWAP);
                super.visitInsn(Opcodes.POP);
                break;
            case Opcodes.IF_ACMPEQ:
            case Opcodes.IF_ACMPNE: { // no taint to be popped
                opcode = visitAcmpInsn(opcode);
                break;
            }
            }
        } else {
            String cmpStr = null;
            boolean binaryOp = false;
            int taint = nextFreeReg++;
            switch (opcode) {
            case Opcodes.IF_ICMPLT:
                binaryOp = true;
            case Opcodes.IFLT:
                cmpStr = "cmp (>=)";
                break;
            case Opcodes.IF_ICMPGE:
                binaryOp = true;
            case Opcodes.IFGE:
                cmpStr = "cmp (<)";
                break;
            case Opcodes.IF_ICMPGT:
                binaryOp = true;
            case Opcodes.IFGT:
                cmpStr = "cmp (<=)";
                break;
            case Opcodes.IF_ICMPLE:
                binaryOp = true;
            case Opcodes.IFLE:
                cmpStr = "cmp (>)";
                break;
            case Opcodes.IF_ACMPEQ:
            case Opcodes.IF_ACMPNE: // no taint to be popped
                opcode = visitAcmpInsn(opcode);
                break;
            }

            if (cmpStr != null) { // handle integer comparisons...
                if (binaryOp) {
                    super.visitVarInsn(Opcodes.ISTORE, taint);
                    super.visitInsn(Opcodes.SWAP);
                    super.visitVarInsn(Opcodes.ILOAD, taint);
                } else {
                    super.visitInsn(TRUSTED_OPCODE);
                }
                super.visitLdcInsn("int compare");
                super.visitLdcInsn(cmpStr);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "pac/util/Overflows", "checkOverflow",
                                      "(IILjava/lang/String;Ljava/lang/String;)V", false);
            }
        }

        super.visitJumpInsn(opcode, label);
        instOffset++;
    }

    /**
     * Visit ACMP_EQ and ACMP_NE instructions.  We only need to replace this
     * instruction with a call to our wrapper method handling this instruction
     * if the stack at this instructions matches one of the following:
     *   o ... -> pac/wrap/TaintableArray -> pac/wrap/TaintableArray
     *   o ... -> pac/wrap/TaintableArray -> java/lang/Object
     *   o ... -> java/lang/Object -> pac/wrap/TaintableArray
     *   o ... -> java/lang/String -> java/lang/String
     *   o ... -> java/lang/String -> java/lang/Object
     *   o ... -> java/lang/Object -> java/lang/String
     *   o ... -> java/lang/Object -> java/lang/Object
     *   
     * @param opcode int of the instruction opcode.
     * @return the (possibly modified) opcode.
     */
    private int visitAcmpInsn(int opcode) {
        // Let's be smart about whether we really need to replace the ACMP
        // instruction with a wrapped version.  In many cases, this is
        // unnecessary.  Fortunately, we only need to look at the top two
        // words on the stack.
        Frame<BasicValue> curInstFrame = frames[instOffset];
        Type top = null, second = null;
        if (curInstFrame != null) { // unreachable code
            int size = curInstFrame.getStackSize();
            if (size >= 1)
                top = curInstFrame.getStack(size - 1).getType();
            if (size >= 2)
                second = curInstFrame.getStack(size - 2).getType();
        }

        if (top == null || second == null) {
            super.visitMethodInsn(Opcodes.INVOKESTATIC, "pac/inst/taint/InstructionInstrumentation", "acmp",
                                  "(Ljava/lang/Object;Ljava/lang/Object;)I", false);
        } else {
            // If the type is a primitive array type, ensure it is only
            // of single dimension.
            Type topPrimArrayType = AsmUtils.toPrimitiveArrayType(top);
            boolean topIsPrimArray = topPrimArrayType != null && topPrimArrayType.getSort() == Type.OBJECT;
            boolean topIsObject = top.getSort() == Type.OBJECT && top.getInternalName().equals("java/lang/Object");
            boolean topIsString = top.getSort() == Type.OBJECT && top.getInternalName().equals("java/lang/String");
            Type secondPrimArrayType = AsmUtils.toPrimitiveArrayType(second);
            boolean secondIsPrimArray = secondPrimArrayType != null && secondPrimArrayType.getSort() == Type.OBJECT;
            boolean secondIsObject = second.getSort() == Type.OBJECT
                    && second.getInternalName().equals("java/lang/Object");
            boolean secondIsString = second.getSort() == Type.OBJECT
                    && second.getInternalName().equals("java/lang/String");

            if (topIsPrimArray) {
                if (secondIsPrimArray) {
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "pac/inst/taint/InstructionInstrumentation", "acmp",
                                          "(Lpac/wrap/TaintableArray;Lpac/wrap/TaintableArray;)I", false);
                } else if (secondIsObject) {
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "pac/inst/taint/InstructionInstrumentation", "acmp",
                                          "(Ljava/lang/Object;Lpac/wrap/TaintableArray;)I", false);
                } else {
                    // optimization: no special check needed
                    return opcode;
                }
            } else if (secondIsPrimArray && topIsObject) {
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "pac/inst/taint/InstructionInstrumentation", "acmp",
                                      "(Lpac/wrap/TaintableArray;Ljava/lang/Object;)I", false);
            } else if (topIsString) {
                if (secondIsString) {
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "pac/inst/taint/InstructionInstrumentation", "acmp",
                                          "(Ljava/lang/String;Ljava/lang/String;)I", false);
                } else if (secondIsObject) {
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "pac/inst/taint/InstructionInstrumentation", "acmp",
                                          "(Ljava/lang/Object;Ljava/lang/String;)I", false);
                } else {
                    // optimization: no special check needed
                    return opcode;
                }
            } else if (secondIsString && topIsObject) {
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "pac/inst/taint/InstructionInstrumentation", "acmp",
                                      "(Ljava/lang/String;Ljava/lang/Object;)I", false);
            } else if (topIsObject && secondIsObject) {
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "pac/inst/taint/InstructionInstrumentation", "acmp",
                                      "(Ljava/lang/Object;Ljava/lang/Object;)I", false);
            } else {
                // optimization: no special check needed
                return opcode;
            }
        }

        // We have altered this ACMP instruction to invoke a wrapped
        // version of this instruction that will an int of the comparison
        // result.  We therefor need to replace the original instruction
        // with IFEQ/IFNE depending on if the original instruction was
        // IF_ACMPEQ/IF_ACMPNE (respectively).
        return opcode == Opcodes.IF_ACMPEQ ? Opcodes.IFEQ : Opcodes.IFNE;
    }

    /**
     * Called to determine whether an overflow check should be
     * inserted.  We should not check overflow on ==/!= branch
     * instructions, since it's very unlikely for a number to
     * overflow and the ==/!= no longer return the same result.
     * 
     * @param opcode of the instruction that can overflow.
     * @return true iff we should check overflow for this
     *   instruction.
     */
    private boolean shouldCheckOverflow(int opcode) {
        return !(inJdk || opcode == Opcodes.IFEQ || opcode == Opcodes.IFNE || opcode == Opcodes.IF_ICMPEQ
                || opcode == Opcodes.IF_ICMPNE
                || (this.owner.equals("org/junit/internal/MethodSorter$1") && this.name.equals("compare")) // Junit has a bug in this
        // method (as in it does not sort correctly).  We find this
        // bug.
        );
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        super.visitInsn(Opcodes.POP); // pop the taint of the int on the stack;
        super.visitLookupSwitchInsn(dflt, keys, labels);
        instOffset++;
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        super.visitInsn(Opcodes.POP); // pop the taint of the int on the stack;
        super.visitTableSwitchInsn(min, max, dflt, labels);
        instOffset++;
    }

    public static int getLoadOpcode(Type type) {
        switch (type.getSort()) {
        case Type.ARRAY:
        case Type.OBJECT:
            return Opcodes.ALOAD;
        case Type.BOOLEAN:
        case Type.BYTE:
        case Type.SHORT:
        case Type.CHAR:
        case Type.INT:
            return Opcodes.ILOAD;
        case Type.FLOAT:
            return Opcodes.FLOAD;
        case Type.LONG:
            return Opcodes.LLOAD;
        case Type.DOUBLE:
            return Opcodes.DLOAD;
        }
        return -1;
    }

    public static int getStoreOpcode(Type type) {
        switch (type.getSort()) {
        case Type.ARRAY:
        case Type.OBJECT:
            return Opcodes.ASTORE;
        case Type.BOOLEAN:
        case Type.BYTE:
        case Type.SHORT:
        case Type.CHAR:
        case Type.INT:
            return Opcodes.ISTORE;
        case Type.FLOAT:
            return Opcodes.FSTORE;
        case Type.LONG:
            return Opcodes.LSTORE;
        case Type.DOUBLE:
            return Opcodes.DSTORE;
        }
        return -1;
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        // TODO investigate that this is really how we handle these instructions.

        Type[] argTypes = Type.getArgumentTypes(desc);

        Frame<BasicValue> curInstFrame = frames[instOffset];
        int size = 0;
        if (curInstFrame != null) { // unreachable code
            size = curInstFrame.getStackSize();
        }

        // we may not need to store away into temps if no argument contains taint
        boolean hasTaintToPop = false;
        for (int i = 0; i < argTypes.length; i++) {
            int stackIdx = size - argTypes.length + i;
            Type stackType = stackIdx < 0 || argTypes == null ? null : frames[instOffset].getStack(stackIdx).getType();
            hasTaintToPop |= AsmUtils.isPrimitiveType(argTypes[i]) | (AsmUtils.canBePrimitiveArrayType(argTypes[i])
                    && (stackType == null || AsmUtils.canBePrimitiveArrayType(stackType)));
        }

        // only store params into temps if there is at least one param with taint...
        if (hasTaintToPop) {
            // store away all args into temps and pop any taint encountered.
            for (int i = argTypes.length - 1; i >= 0; i--) {
                int stackIdx = size - argTypes.length + i;
                Type typeOnStack = stackIdx < 0 || argTypes == null ? null
                        : frames[instOffset].getStack(stackIdx).getType();
                visitToValueType(argTypes[i], typeOnStack);
                super.visitVarInsn(getStoreOpcode(argTypes[i]), nextFreeReg++);
                if (argTypes[i].getSize() == 2)
                    nextFreeReg++;
            }

            // push back arguments onto the stack (in order)
            int firstReg = nextFreeReg - 1;
            for (int i = 0; i < argTypes.length; i++) {
                if (argTypes[i].getSize() == 2)
                    firstReg--;
                super.visitVarInsn(getLoadOpcode(argTypes[i]), firstReg--);
            }
        }

        super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);

        Type returnType = Type.getReturnType(desc);
        visitToTaintType(returnType, UNKNOWN_OPCODE);

        instOffset++;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {

        // Junit tests will only have the default constructor
        MethodInsnNode methodInsnNode = new MethodInsnNode(opcode, owner, name, desc, itf);
        boolean skipped = hierarchy.isSkipped(methodInsnNode, inJdk);

        // We need to handle a special case of when the owner is actually
        // a dangerous type or if the owner is a primtive array type...
        if (owner.charAt(0) == '[') { // method invocations on an
                                      // array...
                                      // We need to replace [I with IntArrayTaint, for example.
            Type type = Type.getType(owner);
            if (AsmUtils.isPrimitiveArrayType(type)) {
                type = AsmUtils.toPrimitiveArrayType(type);
                switch (type.getSort()) {
                case Type.ARRAY:
                    owner = type.getDescriptor();
                    break;
                default:
                    owner = type.getInternalName();
                    break;
                }
            }
        } else if ((name.equals("<init>") || opcode == Opcodes.INVOKESTATIC || owner.equals(this.owner))
                && hierarchy.isDangerousClass(owner)) {
            owner = ClassHierarchy.getDangerousSublassName(owner);
        } else if (name.equals("clone") && owner.equals("java/lang/Object") && desc.equals("()Ljava/lang/Object;")) {
            // Check for archaic invocations of Object.clone() on primitive arrays.
            // These need to invoke the wrapped call to clone().
            Frame<BasicValue> curInstFrame = frames[instOffset];
            Type top = null;
            if (curInstFrame != null) {
                int size = curInstFrame.getStackSize();
                if (size >= 1) {
                    top = curInstFrame.getStack(size - 1).getType();
                    if (top != null) {
                        switch (top.getSort()) {
                        case Type.ARRAY:
                            // Check if the array element has a corresponding
                            // wrapped taint array type.  If so, then replace
                            // the owner of the invocation with this class name.
                            Type arrayTaintType = AsmUtils.toWrappedArrayType(top.getElementType());
                            if (arrayTaintType != null)
                                owner = arrayTaintType.getInternalName();
                            break;
                        }
                    }
                }
            }
        }

        // we need to convert between taint types and non taint-tracked types
        // for skipped methods
        if (skipped) {
            Type[] argTypes = Type.getArgumentTypes(desc);

            Frame<BasicValue> curInstFrame = frames[instOffset];
            int size = 0;
            if (curInstFrame != null) { // unreachable code
                size = curInstFrame.getStackSize();
            }

            // Hack for code that requires information from the stack, since the
            // stack will now be off by one.
            // boolean isStackDependent = name.equals("getCallerClass") &&
            // owner.equals("sun/reflect/Reflection");

            // we may not need to store away into temps if no argument contains
            // taint
            boolean hasTaintToPop = false;
            for (int i = 0; i < argTypes.length; i++) {
                int stackIdx = size - argTypes.length + i;
                Type stackType = stackIdx < 0 || argTypes == null ? null
                        : frames[instOffset].getStack(stackIdx).getType();
                hasTaintToPop |= AsmUtils.isPrimitiveType(argTypes[i]) | (AsmUtils.canBePrimitiveArrayType(argTypes[i])
                        && (stackType == null || AsmUtils.canBePrimitiveArrayType(stackType)));
            }

            // only store params into temps if there is at least
            // one param with taint...
            if (hasTaintToPop) {
                // store away all args into temps and pop any taint encountered.
                for (int i = argTypes.length - 1; i >= 0; i--) {
                    int stackIdx = size - argTypes.length + i;
                    Type typeOnStack = stackIdx < 0 || argTypes == null ? null
                            : frames[instOffset].getStack(stackIdx).getType();
                    visitToValueType(argTypes[i], typeOnStack);
                    super.visitVarInsn(getStoreOpcode(argTypes[i]), nextFreeReg++);
                    if (argTypes[i].getSize() == 2)
                        nextFreeReg++;
                }

                // push back arguments onto the stack (in order)
                int firstReg = nextFreeReg - 1;
                for (int i = 0; i < argTypes.length; i++) {
                    if (argTypes[i].getSize() == 2)
                        firstReg--;
                    super.visitVarInsn(getLoadOpcode(argTypes[i]), firstReg--);
                }
            }

            // if (isStackDependent) {
            // super.visitInsn(Opcodes.ICONST_1);
            // super.visitInsn(Opcodes.IADD);
            // }

            visitMethodInsnBefore(owner, name, desc, desc);

            // invoke the original (uninstrumented) method
            super.visitMethodInsn(opcode, owner, name, desc, itf);

            visitMethodInsnAfter(owner, name, desc, desc);

            // convert all return types to a tainted type with unknown
            // taint.
            visitToTaintType(Type.getReturnType(desc), UNKNOWN_OPCODE);
        } else {
            // The call is instrumented, so the only thing we need to do
            // is push the Ret object on the stack prior to the invocation,
            // then acquire the taint out of the Ret object and push it
            // onto the stack after the return type.
            super.visitVarInsn(Opcodes.ALOAD, retObjReg);

            String newDesc = toPrimitiveDesc(desc);

            visitMethodInsnBefore(owner, name, desc, newDesc);

            // invoke the instrumented copy
            super.visitMethodInsn(opcode, owner, name, newDesc, itf);

            visitMethodInsnAfter(owner, name, desc, newDesc);

            Type returnType = Type.getReturnType(newDesc);
            switch (returnType.getSort()) {
            case Type.VOID:
            case Type.OBJECT:
            case Type.ARRAY:
                break;
            default:
                super.visitVarInsn(Opcodes.ALOAD, retObjReg);
                super.visitFieldInsn(Opcodes.GETFIELD, "pac/util/Ret", "taint", "I");
                break;
            }
        }

        instOffset++;
    }

    //#if bootstrap==false
    private pac.config.AbstractConfig getConfigInstance() {
        return dynamicMode ? pac.config.BaseConfig.getInstance() : pac.config.ConfigFile.getInstance();
    }
    //#endif

    private void visitMethodInsnBefore(String owner, String name, String desc, String newDesc) {
        //#if bootstrap==false
        pac.config.Desc d = null;
        if (!hierarchy.isJdkClass(this.owner)) {
            // we only want to load configuration desc rules for methods invoked
            // in application methods...
            String descStr = owner + "." + name + desc.substring(0, desc.indexOf(')') + 1);
            d = getConfigInstance().getDescriptor(descStr);
        }
        //#endif

        //#if bootstrap==false
        if (d != null) {
            pac.config.Chk[] beforeChks = d.getBeforeChks();
            if (beforeChks != null) {
                instrumentBeforeChecks(d, owner, newDesc);
            }
        }
        //#endif
    }

    private void visitMethodInsnAfter(String owner, String name, String desc, String newDesc) {
        //#if bootstrap==false
        pac.config.Desc d = null;
        if (!hierarchy.isJdkClass(this.owner)) {
            // we only want to load configuration desc rules for methods invoked
            // in application methods...
            String descStr = owner + "." + name + desc.substring(0, desc.indexOf(')') + 1);
            d = getConfigInstance().getDescriptor(descStr);
        }
        //#endif

        //#if bootstrap==false
        if (d != null) {
            pac.config.Chk afterChk = d.getAfterChk();
            if (afterChk != null && !newDesc.startsWith(owner + ".<init>")) {
                instrumentAfterCheck(d, owner);
            }
        }
        //#endif
    }

    private void visitToTaintType(Type origType, int taintOpcode) {
        Type primType = AsmUtils.toPrimitiveType(origType);
        if (primType != null) { // the return type is either a primitive, or
                                // primitive array
            switch (primType.getSort()) {
            case Type.INT:
                super.visitInsn(taintOpcode);
                break;
            default:
                Type arrayTaintType = AsmUtils.toWrappedArrayType(origType.getElementType());
                super.visitMethodInsn(Opcodes.INVOKESTATIC, arrayTaintType.getInternalName(), "toTaintArray",
                                      "(" + origType.getDescriptor() + ")" + primType.getDescriptor(), false);
            }
        } else if (AsmUtils.canBePrimitiveArrayType(origType)) {
            // the return type is an Object, Object[], Object[][], etc...
            super.visitMethodInsn(Opcodes.INVOKESTATIC, "pac/wrap/ArrayTaint", "toTaintArray",
                                  "(Ljava/lang/Object;)Ljava/lang/Object;", false);
            if (origType.getSort() == Type.ARRAY) // need to cast back to array
                                                  // of objects
                super.visitTypeInsn(Opcodes.CHECKCAST, origType.getInternalName());
        }
    }

    private void visitToValueType(Type origType, Type typeOnStack) {
        Type primType = AsmUtils.toPrimitiveType(origType);
        if (primType == null) { // must be an Object or Object[]...
            if (typeOnStack != null && AsmUtils.isPrimitiveArrayType(typeOnStack)) {
                primType = AsmUtils.toPrimitiveType(typeOnStack);
                Type arrayTaintType = AsmUtils.toWrappedArrayType(typeOnStack.getElementType());
                super.visitMethodInsn(Opcodes.INVOKESTATIC, arrayTaintType.getInternalName(), "toValueArray",
                                      "(" + primType.getDescriptor() + ")" + typeOnStack.getDescriptor(), false);
            } else if (typeOnStack == null || AsmUtils.canBePrimitiveArrayType(typeOnStack)) {
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "pac/wrap/ArrayTaint", "toValueArray",
                                      "(Ljava/lang/Object;)Ljava/lang/Object;", false);
                if (origType.getSort() == Type.ARRAY) // need to cast
                                                      // back to array
                                                      // of objects
                    super.visitTypeInsn(Opcodes.CHECKCAST, origType.getInternalName());
            }
        } else {
            switch (primType.getSort()) {
            case Type.INT:
                super.visitInsn(Opcodes.POP);
                break;
            default:
                Type arrayTaintType = AsmUtils.toWrappedArrayType(origType.getElementType());
                super.visitMethodInsn(Opcodes.INVOKESTATIC, arrayTaintType.getInternalName(), "toValueArray",
                                      "(" + primType.getDescriptor() + ")" + origType.getDescriptor(), false);
            }
        }
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        FieldInsnNode fieldInsnNode = new FieldInsnNode(opcode, owner, name, desc);

        if (hierarchy.isDangerousClass(owner)) {
            owner = ClassHierarchy.getDangerousSublassName(owner);
        }

        boolean skipped = hierarchy.isSkipped(fieldInsnNode);
        Type fieldType = Type.getType(desc);

        Type primType = AsmUtils.toPrimitiveType(fieldType);
        if (primType == null && !AsmUtils.canBePrimitiveArrayType(fieldType)) {
            // no shadow variable, so leave the instruction as is.
            super.visitFieldInsn(opcode, owner, name, desc);
            instOffset++;
            return;
        }

        // from this point on either the field is a primitive, primitive array,
        // or an object/object[] that can be a primitive array...
        boolean expectingObject = false;
        switch (opcode) {
        case Opcodes.GETFIELD:
            expectingObject = true;
        case Opcodes.GETSTATIC: {
            if (!skipped && hierarchy.isConstant(fieldInsnNode)) {
                // constant fields that are not skipped have taint that do
                // not need updated...
                boolean doubleType = false;
                if (primType != null) {
                    switch (primType.getSort()) {
                    case Type.OBJECT:
                    case Type.ARRAY:
                        break;
                    case Type.LONG:
                    case Type.DOUBLE:
                        doubleType = true;
                    default:
                        if (expectingObject)
                            super.visitInsn(Opcodes.DUP);
                        super.visitFieldInsn(opcode, owner, name, desc);
                    }
                    if (expectingObject) {
                        if (doubleType) {
                            super.visitInsn(Opcodes.DUP2_X1);
                            super.visitInsn(Opcodes.POP2);
                        } else {
                            super.visitInsn(Opcodes.SWAP);
                        }
                    }
                    super.visitFieldInsn(opcode, owner, AsmUtils.getFieldInstName(owner, name, desc),
                                         primType.getDescriptor());
                } else {
                    super.visitFieldInsn(opcode, owner, AsmUtils.getFieldInstName(owner, name, desc), desc);
                }
            } else if (skipped) {
                super.visitFieldInsn(opcode, owner, name, desc);
                visitToTaintType(fieldType, UNKNOWN_OPCODE);
            } else { // this field may have a taint field
                if (primType != null && primType.getSort() == Type.INT) {
                    // we get from a field that is a primitive
                    if (expectingObject)
                        super.visitInsn(Opcodes.DUP);
                    super.visitFieldInsn(opcode, owner, name, desc);
                    if (expectingObject) {
                        if (fieldType.getSize() == 2) {
                            super.visitInsn(Opcodes.DUP2_X1);
                            super.visitInsn(Opcodes.POP2);
                        } else {
                            super.visitInsn(Opcodes.SWAP);
                        }
                    }
                    super.visitFieldInsn(opcode, owner, AsmUtils.getFieldInstName(owner, name, desc),
                                         primType.getDescriptor());
                } else { // must be a primitive array, Object, Object[], etc...
                    if (expectingObject)
                        super.visitInsn(Opcodes.DUP);
                    super.visitFieldInsn(opcode, owner, name + FIELD_SUFFIX,
                                         primType == null ? desc : primType.getDescriptor());
                    super.visitInsn(Opcodes.DUP);
                    Label fieldSet = new Label();
                    super.visitJumpInsn(Opcodes.IFNONNULL, fieldSet);
                    super.visitInsn(Opcodes.POP);
                    if (expectingObject) {
                        super.visitInsn(Opcodes.DUP);
                        super.visitFieldInsn(opcode, owner, name, desc);

                        visitToTaintType(fieldType, UNKNOWN_OPCODE);
                        super.visitInsn(Opcodes.DUP_X1);
                        super.visitFieldInsn(Opcodes.PUTFIELD, owner, AsmUtils.getFieldInstName(owner, name, desc),
                                             primType == null ? desc : primType.getDescriptor());

                        Label fieldNotSet = new Label();
                        super.visitJumpInsn(Opcodes.GOTO, fieldNotSet);
                        super.visitLabel(fieldSet);
                        super.visitInsn(Opcodes.SWAP);
                        super.visitInsn(Opcodes.POP);
                        super.visitLabel(fieldNotSet);
                    } else {
                        super.visitFieldInsn(opcode, owner, name, desc);

                        visitToTaintType(fieldType, UNKNOWN_OPCODE);
                        super.visitInsn(Opcodes.DUP);
                        super.visitFieldInsn(Opcodes.PUTSTATIC, owner, AsmUtils.getFieldInstName(owner, name, desc),
                                             primType == null ? desc : primType.getDescriptor());

                        super.visitLabel(fieldSet);
                    }
                }
            }
            break;
        }
        case Opcodes.PUTFIELD:
            expectingObject = true;
        case Opcodes.PUTSTATIC: {
            Frame<BasicValue> curInstFrame = frames[instOffset];
            Type top = null;
            if (curInstFrame != null) { // unreachable code
                int size = curInstFrame.getStackSize();
                if (size >= 1)
                    top = curInstFrame.getStack(size - 1).getType();
            }

            if (skipped) {
                visitToValueType(fieldType, top);
                super.visitFieldInsn(opcode, owner, name, desc);
            } else {
                if (primType == null || primType.getSort() != Type.INT) { // must be an Object, Object[], or primitive array...
                    super.visitInsn(expectingObject ? Opcodes.DUP2 : Opcodes.DUP);
                    super.visitFieldInsn(opcode, owner, AsmUtils.getFieldInstName(owner, name, desc),
                                         primType == null ? desc : primType.getDescriptor());
                    visitToValueType(fieldType, top);
                    super.visitFieldInsn(opcode, owner, name, desc);
                } else {
                    if (expectingObject) {
                        int taintVar = nextFreeReg++;
                        super.visitVarInsn(Opcodes.ISTORE, taintVar);
                        if (fieldType.getSize() == 2) {
                            super.visitInsn(Opcodes.DUP2_X1);
                            super.visitInsn(Opcodes.POP2);
                            super.visitInsn(Opcodes.DUP_X2);
                        } else {
                            super.visitInsn(Opcodes.SWAP);
                            super.visitInsn(Opcodes.DUP_X1);
                        }
                        super.visitVarInsn(Opcodes.ILOAD, taintVar);
                    }
                    super.visitFieldInsn(opcode, owner, AsmUtils.getFieldInstName(owner, name, desc),
                                         primType.getDescriptor());
                    super.visitFieldInsn(opcode, owner, name, desc);
                }
            }
            break;
        }
        }
        instOffset++;
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        boolean checkOverflow = shouldCheckOverflow(Opcodes.MULTIANEWARRAY);

        Type arrayType = Type.getType(desc);
        Type primArrayType = AsmUtils.toPrimitiveArrayType(arrayType);

        if (arrayType.getDimensions() == dims && primArrayType != null) {
            // In some weird bytecode, it's possible that the array is a single
            // dimension primitive array.
            Type eleType = primArrayType.getSort() != Type.ARRAY ? primArrayType : primArrayType.getElementType();
            StringBuilder newArrayDesc = new StringBuilder("()");
            for (int i = 0; i < dims; i++)
                newArrayDesc.insert(1, "II");
            newArrayDesc.append(primArrayType.getDescriptor());
            super.visitMethodInsn(Opcodes.INVOKESTATIC, eleType.getInternalName(),
                                  checkOverflow ? "newArray" : "newArray_noCheck", newArrayDesc.toString(), false);
        } else {
            if (primArrayType != null)
                desc = primArrayType.getDescriptor();

            // store away all args into temps and pop any taint
            // encountered.
            for (int i = dims - 1; i >= 0; i--) {
                if (!checkOverflow) {
                    super.visitInsn(Opcodes.POP);
                } else {
                    visitNumberInsn(i);
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "pac/wrap/ArrayTaint", "validateNewArrayLength",
                                          "(III)I", false);
                }
                super.visitVarInsn(Opcodes.ISTORE, nextFreeReg++);
            }

            // push back arguments onto the stack (in order)
            int firstReg = nextFreeReg - 1;
            for (int i = 0; i < dims; i++) {
                super.visitVarInsn(Opcodes.ILOAD, firstReg--);
            }

            super.visitMultiANewArrayInsn(desc, dims);
        }
        instOffset++;
    }

    /**
     * Simulate an array store by invoking the store() method in the appropriate
     * array taint class.
     * 
     * @param arrayEleType
     *            Type of elements in the array.
     */
    private void visitArrayStore(Type arrayEleType) {
        Type arrayTaintType = AsmUtils.toWrappedArrayType(arrayEleType);
        super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, arrayTaintType.getInternalName(),
                              shouldCheckOverflow(Opcodes.ASTORE) ? "store" : "store_noCheck",
                              Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE, Type.INT_TYPE, arrayEleType,
                                                       Type.INT_TYPE),
                              false);
    }

    /**
     * Simulate an array load by invoking the load() method in the appropriate
     * array taint class.
     * 
     * @param arrayEleType
     *            Type of elements in the array.
     */
    private void visitArrayLoad(Type arrayEleType) {
        Type arrayTaintType = AsmUtils.toWrappedArrayType(arrayEleType);
        super.visitVarInsn(Opcodes.ALOAD, retObjReg);
        super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, arrayTaintType.getInternalName(),
                              shouldCheckOverflow(Opcodes.ALOAD) ? "load" : "load_noCheck",
                              Type.getMethodDescriptor(arrayEleType, Type.INT_TYPE, Type.INT_TYPE,
                                                       Type.getType(Ret.class)),
                              false);
        super.visitVarInsn(Opcodes.ALOAD, retObjReg);
        super.visitFieldInsn(Opcodes.GETFIELD, "pac/util/Ret", "taint", "I");
    }

    private static boolean isStream(String className) {
        return hierarchy.isA(className, "java/io/InputStream") || hierarchy.isA(className, "java/io/Reader")
                || hierarchy.isA(className, "java/io/OutputStream") || hierarchy.isA(className, "java/io/Writer");
    }

    private static String getStreamType(String className) {
        if (hierarchy.isA(className, "java/io/InputStream"))
            return "java/io/InputStream";
        if (hierarchy.isA(className, "java/io/OutputStream"))
            return "java/io/OutputStream";
        if (hierarchy.isA(className, "java/io/Writer"))
            return "java/io/Writer";
        if (hierarchy.isA(className, "java/io/Reader"))
            return "java/io/Reader";
        return null;
    }

    @Override
    public void visitInsn(int opcode) {
        Frame<BasicValue> curInstFrame = frames[instOffset];
        Type top = null, second = null, third = null;
        if (curInstFrame != null) { // unreachable code
            int size = curInstFrame.getStackSize();
            if (size >= 1)
                top = curInstFrame.getStack(size - 1).getType();
            if (size >= 2)
                second = curInstFrame.getStack(size - 2).getType();
            if (size >= 3)
                third = curInstFrame.getStack(size - 3).getType();
        }

        switch (opcode) {
        // primitive constants...
        case Opcodes.ICONST_M1:
        case Opcodes.ICONST_0:
        case Opcodes.ICONST_1:
        case Opcodes.ICONST_2:
        case Opcodes.ICONST_3:
        case Opcodes.ICONST_4:
        case Opcodes.ICONST_5:
        case Opcodes.LCONST_0:
        case Opcodes.LCONST_1:
        case Opcodes.FCONST_0:
        case Opcodes.FCONST_1:
        case Opcodes.FCONST_2:
        case Opcodes.DCONST_0:
        case Opcodes.DCONST_1:
            super.visitInsn(opcode);
            super.visitInsn(TRUSTED_OPCODE);
            break;

        // loading out of array...
        case Opcodes.IALOAD:
            visitArrayLoad(Type.INT_TYPE);
            break;
        case Opcodes.LALOAD:
            visitArrayLoad(Type.LONG_TYPE);
            break;
        case Opcodes.FALOAD:
            visitArrayLoad(Type.FLOAT_TYPE);
            break;
        case Opcodes.DALOAD:
            visitArrayLoad(Type.DOUBLE_TYPE);
            break;
        case Opcodes.BALOAD:
            if (second != null && second.getSort() == Type.ARRAY && second.getElementType().getSort() == Type.BOOLEAN)
                visitArrayLoad(Type.BOOLEAN_TYPE);
            else
                visitArrayLoad(Type.BYTE_TYPE);
            break;
        case Opcodes.CALOAD:
            visitArrayLoad(Type.CHAR_TYPE);
            break;
        case Opcodes.SALOAD:
            visitArrayLoad(Type.SHORT_TYPE);
            break;
        case Opcodes.AALOAD:
            if (!shouldCheckOverflow(opcode)) {
                super.visitInsn(Opcodes.POP);
                super.visitInsn(opcode);
            } else {
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "pac/wrap/ArrayTaint", "load",
                                      "([Ljava/lang/Object;II)Ljava/lang/Object;", false);

                Type castType;
                if (AsmUtils.isPrimitiveArrayType(second)) {
                    castType = AsmUtils.toPrimitiveArrayType(second);
                } else {
                    castType = second;
                }
                castType = Type.getType(castType.getDescriptor().substring(1)); // remove one array level

                String typeDesc = castType.getInternalName();
                if (!typeDesc.equals("null;") && !typeDesc.equals("java/lang/Object"))
                    super.visitTypeInsn(Opcodes.CHECKCAST, typeDesc);
            }
            break;

        // store into an array...
        case Opcodes.IASTORE:
            visitArrayStore(Type.INT_TYPE);
            break;
        case Opcodes.LASTORE:
            visitArrayStore(Type.LONG_TYPE);
            break;
        case Opcodes.FASTORE:
            visitArrayStore(Type.FLOAT_TYPE);
            break;
        case Opcodes.DASTORE:
            visitArrayStore(Type.DOUBLE_TYPE);
            break;
        case Opcodes.BASTORE:
            if (third != null && third.getSort() == Type.ARRAY && third.getElementType().getSort() == Type.BOOLEAN)
                visitArrayStore(Type.BOOLEAN_TYPE);
            else
                visitArrayStore(Type.BYTE_TYPE);
            break;
        case Opcodes.CASTORE:
            visitArrayStore(Type.CHAR_TYPE);
            break;
        case Opcodes.SASTORE:
            visitArrayStore(Type.SHORT_TYPE);
            break;
        case Opcodes.AASTORE:
            if (!shouldCheckOverflow(opcode)) {
                int value = nextFreeReg++;
                super.visitVarInsn(Opcodes.ASTORE, value);
                super.visitInsn(Opcodes.POP);
                super.visitVarInsn(Opcodes.ALOAD, value);
                super.visitInsn(opcode);
            } else {
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "pac/wrap/ArrayTaint", "store",
                                      "([Ljava/lang/Object;IILjava/lang/Object;)V", false);
            }
            break;

        // stack operations...
        case Opcodes.POP:
        case Opcodes.DUP:
        case Opcodes.DUP_X1:
        case Opcodes.DUP_X2:
        case Opcodes.SWAP:
        case Opcodes.POP2:
        case Opcodes.DUP2:
        case Opcodes.DUP2_X1:
        case Opcodes.DUP2_X2:
            visitStackOperation(opcode);
            break;

        // binary operations (single)...
        /*
         * ISTORE tmp1, SWAP, ISTORE tmp2, (IADD), ILOAD tmp1, ILOAD tmp2, IOR
         */
        case Opcodes.IADD:
            visitOperation("iadd", Type.INT_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.FADD:
            visitOperation("fadd", Type.FLOAT_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.ISUB:
            visitOperation("isub", Type.INT_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.FSUB:
            visitOperation("fsub", Type.FLOAT_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.IMUL:
            visitOperation("imul", Type.INT_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.FMUL:
            visitOperation("fmul", Type.FLOAT_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.IDIV:
            visitOperation("idiv", Type.INT_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.FDIV:
            visitOperation("fdiv", Type.FLOAT_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.IREM:
            visitOperation("irem", Type.INT_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.FREM:
            visitOperation("frem", Type.FLOAT_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.ISHL:
            visitOperation("ishl", Type.INT_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.ISHR:
            visitOperation("ishr", Type.INT_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.IUSHR:
            visitOperation("iushr", Type.INT_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.IAND:
            visitOperation("iand", Type.INT_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.IOR:
            visitOperation("ior", Type.INT_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.IXOR:
            visitOperation("ixor", Type.INT_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.LSHL:
            visitOperation("lshl", Type.LONG_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.LSHR:
            visitOperation("lshr", Type.LONG_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.LUSHR:
            visitOperation("lushr", Type.LONG_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.FCMPL:
            visitOperation("fcmpl", Type.FLOAT_TYPE.getDescriptor(), 2, Type.INT_TYPE.getDescriptor());
            break;
        case Opcodes.FCMPG:
            visitOperation("fcmpg", Type.FLOAT_TYPE.getDescriptor(), 2, Type.INT_TYPE.getDescriptor());
            break;
        // {
        // visitBinaryOperation(opcode, false);
        // }

        // binary operations (double)...
        /*
         * ISTORE tmp1, DUP2_X1, POP2, ISTORE tmp2, (DADD), ILOAD tmp1, ILOAD
         * tmp2, IOR
         */
        case Opcodes.LADD:
            visitOperation("ladd", Type.LONG_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.DADD:
            visitOperation("dadd", Type.DOUBLE_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.LSUB:
            visitOperation("lsub", Type.LONG_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.DSUB:
            visitOperation("dsub", Type.DOUBLE_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.LMUL:
            visitOperation("lmul", Type.LONG_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.DMUL:
            visitOperation("dmul", Type.DOUBLE_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.LDIV:
            visitOperation("ldiv", Type.LONG_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.DDIV:
            visitOperation("ddiv", Type.DOUBLE_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.LREM:
            visitOperation("lrem", Type.LONG_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.DREM:
            visitOperation("drem", Type.DOUBLE_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.LAND:
            visitOperation("land", Type.LONG_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.LOR:
            visitOperation("lor", Type.LONG_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.LXOR:
            visitOperation("lxor", Type.LONG_TYPE.getDescriptor(), 2);
            break;
        case Opcodes.LCMP:
            visitOperation("lcmp", Type.LONG_TYPE.getDescriptor(), 2, Type.INT_TYPE.getDescriptor());
            break;
        case Opcodes.DCMPL:
            visitOperation("dcmpl", Type.DOUBLE_TYPE.getDescriptor(), 2, Type.INT_TYPE.getDescriptor());
            break;
        case Opcodes.DCMPG:
            visitOperation("dcmpg", Type.DOUBLE_TYPE.getDescriptor(), 2, Type.INT_TYPE.getDescriptor());
            break;
        // {
        // visitBinaryOperation(opcode, true);
        // }

        // unary operations (single to single)...
        case Opcodes.I2F:
            visitOperation("i2f", Type.INT_TYPE.getDescriptor(), 1, Type.FLOAT_TYPE.getDescriptor());
            break;
        case Opcodes.F2I:
            visitOperation("f2i", Type.FLOAT_TYPE.getDescriptor(), 1, Type.INT_TYPE.getDescriptor());
            break;
        case Opcodes.I2B:
            visitOperation(inJdk ? "i2b" : "i2b_app", Type.INT_TYPE.getDescriptor(), 1, Type.BYTE_TYPE.getDescriptor());
            break;
        case Opcodes.I2C:
            visitOperation(inJdk ? "i2c" : "i2c_app", Type.INT_TYPE.getDescriptor(), 1, Type.CHAR_TYPE.getDescriptor());
            break;
        case Opcodes.I2S:
            visitOperation(inJdk ? "i2s" : "i2s_app", Type.INT_TYPE.getDescriptor(), 1,
                           Type.SHORT_TYPE.getDescriptor());
            break;
        case Opcodes.INEG:
            visitOperation("ineg", Type.INT_TYPE.getDescriptor(), 1);
            break;
        case Opcodes.FNEG:
            visitOperation("fneg", Type.FLOAT_TYPE.getDescriptor(), 1);
            break;
        // super.visitInsn(Opcodes.SWAP);
        // super.visitInsn(opcode);
        // super.visitInsn(Opcodes.SWAP);
        // break;

        // unary operations (single to double)...
        case Opcodes.I2L:
            visitOperation("i2l", Type.INT_TYPE.getDescriptor(), 1, Type.LONG_TYPE.getDescriptor());
            break;
        case Opcodes.I2D:
            visitOperation("i2d", Type.INT_TYPE.getDescriptor(), 1, Type.DOUBLE_TYPE.getDescriptor());
            break;
        case Opcodes.F2L:
            visitOperation("f2l", Type.FLOAT_TYPE.getDescriptor(), 1, Type.LONG_TYPE.getDescriptor());
            break;
        case Opcodes.F2D:
            visitOperation("f2d", Type.FLOAT_TYPE.getDescriptor(), 1, Type.DOUBLE_TYPE.getDescriptor());
            break;
        // super.visitInsn(Opcodes.SWAP);
        // super.visitInsn(opcode);
        // super.visitInsn(Opcodes.DUP2_X1);
        // super.visitInsn(Opcodes.POP2);
        // break;

        // unary operations (double to single)...
        case Opcodes.L2I:
            visitOperation(inJdk ? "l2i" : "l2i_app", Type.LONG_TYPE.getDescriptor(), 1, Type.INT_TYPE.getDescriptor());
            break;
        case Opcodes.L2F:
            visitOperation("l2f", Type.LONG_TYPE.getDescriptor(), 1, Type.FLOAT_TYPE.getDescriptor());
            break;
        case Opcodes.D2I:
            visitOperation("d2i", Type.DOUBLE_TYPE.getDescriptor(), 1, Type.INT_TYPE.getDescriptor());
            break;
        case Opcodes.D2F:
            visitOperation("d2f", Type.DOUBLE_TYPE.getDescriptor(), 1, Type.FLOAT_TYPE.getDescriptor());
            break;
        // super.visitInsn(Opcodes.DUP_X2);
        // super.visitInsn(Opcodes.POP);
        // super.visitInsn(opcode);
        // super.visitInsn(Opcodes.SWAP);
        // break;

        // unary operations (double to double)...
        case Opcodes.L2D:
            visitOperation("l2d", Type.LONG_TYPE.getDescriptor(), 1, Type.DOUBLE_TYPE.getDescriptor());
            break;
        case Opcodes.D2L:
            visitOperation("d2l", Type.DOUBLE_TYPE.getDescriptor(), 1, Type.LONG_TYPE.getDescriptor());
            break;
        case Opcodes.LNEG:
            visitOperation("lneg", Type.LONG_TYPE.getDescriptor(), 1);
            break;
        case Opcodes.DNEG:
            visitOperation("dneg", Type.DOUBLE_TYPE.getDescriptor(), 1);
            break;
        // super.visitInsn(Opcodes.DUP_X2);
        // super.visitInsn(Opcodes.POP);
        // super.visitInsn(opcode);
        // super.visitInsn(Opcodes.DUP2_X1);
        // super.visitInsn(Opcodes.POP2);
        // break;

        // return instructions...
        case Opcodes.IRETURN:
        case Opcodes.LRETURN:
        case Opcodes.FRETURN:
        case Opcodes.DRETURN:
            super.visitVarInsn(Opcodes.ALOAD, retObjReg);
            super.visitInsn(Opcodes.SWAP);
            super.visitFieldInsn(Opcodes.PUTFIELD, "pac/util/Ret", "taint", "I");
            super.visitInsn(opcode);
            break;

        case Opcodes.ARRAYLENGTH:
            if (top != null) {
                switch (top.getSort()) {
                case Type.ARRAY:
                    Type eleType = top.getElementType();
                    Type arrayClass = AsmUtils.toWrappedArrayType(top.getElementType());
                    if (arrayClass == null || top.getDimensions() > 1) {
                        // we know this will be an actual array, so leave the
                        // instruction as is...
                        super.visitInsn(opcode);
                        super.visitInsn(TRUSTED_OPCODE);
                    } else {
                        // super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                        // arrayClass, "length", "()I");
                        super.visitFieldInsn(Opcodes.GETFIELD, arrayClass.getInternalName(), "value",
                                             "[" + eleType.getDescriptor());
                        super.visitInsn(Opcodes.ARRAYLENGTH);
                        super.visitInsn(TRUSTED_OPCODE);
                    }
                    break;
                default:
                    // we don't know the real type, so call the generic
                    // arraylength method
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "pac/wrap/ArrayTaint", "length",
                                          "(Ljava/lang/Object;)I", false);
                    super.visitInsn(TRUSTED_OPCODE);
                    break;
                }
            } else { // this should never happen, presuming that we calculate
                     // the stacks correctly...
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "pac/wrap/ArrayTaint", "length", "(Ljava/lang/Object;)I",
                                      false);
                super.visitInsn(TRUSTED_OPCODE);
            }
            break;

        case Opcodes.RETURN:
            if (this.name.equals("<init>") && isStream(this.owner)) {
                // we want to propagate the taint for input streams and output
                // streams from the one that was passed to the constructor to
                // "this" object...

                super.visitVarInsn(Opcodes.ALOAD, 0);
                super.visitInsn(Opcodes.ICONST_0); // taint
                super.visitInsn(Opcodes.ICONST_0); // hasUniformTaint
                int reg = 1;
                boolean hasStream = false;
                Type[] types = Type.getArgumentTypes(this.desc);
                for (Type argType : types) {
                    if (argType.getSort() == Type.OBJECT && isStream(argType.getInternalName())) {
                        super.visitVarInsn(Opcodes.ALOAD, reg);
                        Label trueCond = new Label();
                        super.visitJumpInsn(Opcodes.IFNULL, trueCond);

                        // Backstop confinement propagation...
                        super.visitVarInsn(Opcodes.ALOAD, 0);
                        super.visitVarInsn(Opcodes.ALOAD, reg);
                        super.visitFieldInsn(Opcodes.GETFIELD, getStreamType(argType.getInternalName()), "ss_socktype",
                                             "Lpac/inst/taint/SocketInstrumentation$SocketType;");
                        super.visitFieldInsn(Opcodes.PUTFIELD, getStreamType(this.owner), "ss_socktype",
                                             "Lpac/inst/taint/SocketInstrumentation$SocketType;");

                        // Taint propagation...
                        super.visitVarInsn(Opcodes.ALOAD, reg);
                        super.visitFieldInsn(Opcodes.GETFIELD, getStreamType(argType.getInternalName()),
                                             "ss_hasUniformTaint", "Z");
                        super.visitInsn(Opcodes.IOR);
                        super.visitInsn(Opcodes.SWAP);
                        super.visitVarInsn(Opcodes.ALOAD, reg);
                        super.visitFieldInsn(Opcodes.GETFIELD, getStreamType(argType.getInternalName()), "ss_taint",
                                             "I");
                        super.visitInsn(Opcodes.IOR);
                        super.visitInsn(Opcodes.SWAP);
                        super.visitLabel(trueCond);
                        hasStream = true;
                    }
                    reg += argType.getSize();
                }

                if (hasStream) {
                    super.visitVarInsn(Opcodes.ALOAD, 0);
                    super.visitInsn(Opcodes.SWAP);
                    super.visitFieldInsn(Opcodes.PUTFIELD, getStreamType(this.owner), "ss_hasUniformTaint", "Z");
                    super.visitFieldInsn(Opcodes.PUTFIELD, getStreamType(this.owner), "ss_taint", "I");
                }
            }
            // these instructions should remain unchanged...
        case Opcodes.JSR:
        case Opcodes.RET:
        case Opcodes.ARETURN:
        case Opcodes.ATHROW:
        case Opcodes.NOP:
        case Opcodes.ACONST_NULL:
            super.visitInsn(opcode);
            break;
        case Opcodes.MONITORENTER:
            if (!inJdk
                    //#if bootstrap==false
                    && getConfigInstance().areDeadlocksEnabled()
            //#endif
            ) {
                super.visitFieldInsn(Opcodes.GETSTATIC, "pac/util/ThreadMonitor", "INSTANCE",
                                     "Lpac/util/ThreadMonitor;");
                super.visitInsn(Opcodes.SWAP);
                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "pac/util/ThreadMonitor", "monitorenter",
                                      "(Ljava/lang/Object;)V", false);
            } else {
                super.visitInsn(opcode);
            }
            break;
        case Opcodes.MONITOREXIT:
            if (!inJdk
                    //#if bootstrap==false
                    && getConfigInstance().areDeadlocksEnabled()
            //#endif
            ) {
                super.visitFieldInsn(Opcodes.GETSTATIC, "pac/util/ThreadMonitor", "INSTANCE",
                                     "Lpac/util/ThreadMonitor;");
                super.visitInsn(Opcodes.SWAP);
                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "pac/util/ThreadMonitor", "monitorexit",
                                      "(Ljava/lang/Object;)V", false);
            } else {
                super.visitInsn(opcode);
            }
            break;
        }
        instOffset++;
    }

    /**
     * Called on binary/unary operations that will mimic the original
     * instruction. The new instructions will propagate taint and calculate
     * overflow/underflow/infinity taint bits.
     * 
     * @param methodName
     *            String of the method to be called in the
     *            InstructionInstrumentation class.
     * @param typeDesc
     *            String of the ASM type desc of the type of operation (i.e.
     *            int, long, double, etc...)
     * @param numOfOperands
     *            int of the number of operands for this instruction.
     */
    private void visitOperation(String methodName, String typeDesc, int numOfOperands) {
        visitOperation(methodName, typeDesc, numOfOperands, typeDesc);
    }

    /**
     * Called on binary/unary operations that will mimic the original
     * instruction. The new instructions will propagate taint and calculate
     * overflow/underflow/infinity taint bits.
     * 
     * @param methodName
     *            String of the method to be called in the
     *            InstructionInstrumentation class.
     * @param typeDesc
     *            String of the ASM type desc of the type of operation (i.e.
     *            int, long, double, etc...)
     * @param numOfOperands
     *            int of the number of operands for this instruction.
     * @param returnType
     *            String of the ASM type desc that will be returned from this
     *            operation (i.e. int, long, double, etc...)
     */
    private void visitOperation(String methodName, String typeDesc, int numOfOperands, String returnType) {
        boolean lastOperandIsInt = methodName.equals("lushr") || methodName.equals("lshr") || methodName.equals("lshl");
        StringBuilder buf = new StringBuilder("(");
        for (int i = 0; i < numOfOperands; i++) {
            if (lastOperandIsInt && i == numOfOperands - 1)
                buf.append(Type.INT_TYPE.getDescriptor());
            else
                buf.append(typeDesc);
            buf.append(Type.INT_TYPE.getDescriptor());
        }
        buf.append("Lpac/util/Ret;)");
        buf.append(returnType);
        super.visitVarInsn(Opcodes.ALOAD, retObjReg);
        super.visitMethodInsn(Opcodes.INVOKESTATIC, "pac/inst/taint/InstructionInstrumentation", methodName,
                              buf.toString(), false);
        super.visitVarInsn(Opcodes.ALOAD, retObjReg);
        super.visitFieldInsn(Opcodes.GETFIELD, "pac/util/Ret", "taint", "I");
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);
        instOffset++;
    }

    @Override
    public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
        // No sense adding these, since they'll be recomputed when the class
        // is written.
        instOffset++;
    }

    @Override
    public void visitLabel(Label label) {
        super.visitLabel(label);
        instOffset++;
    }

    /**
     * Called when visiting a stack operation (i.e. POP, POP2, SWAP, DUP, DUP2,
     * etc...). These operations change since taint is interleaved on the stack.
     * So we must consider all possible stack combinations, by looking at the
     * verified stack.
     * 
     * @param opcode
     */
    private void visitStackOperation(int opcode) {
        Frame<BasicValue> curInstFrame = frames[instOffset];
        int size = curInstFrame == null ? 0 : curInstFrame.getStackSize();
        if (size == 0) { // unreachable code
            super.visitInsn(opcode);
            return;
        }

        Type top = curInstFrame.getStack(size - 1).getType();
        Type second = size >= 2 ? curInstFrame.getStack(size - 2).getType() : null;
        Type third = size >= 3 ? curInstFrame.getStack(size - 3).getType() : null;
        Type forth = size >= 4 ? curInstFrame.getStack(size - 4).getType() : null;

        switch (opcode) {
        // stack operations...
        case Opcodes.POP:
            switch (top.getSort()) {
            case Type.OBJECT:
            case Type.ARRAY:
                super.visitInsn(Opcodes.POP);
                break;
            default:
                super.visitInsn(Opcodes.POP2);
                break;
            }
            break;
        case Opcodes.DUP:
            switch (top.getSort()) {
            case Type.OBJECT:
            case Type.ARRAY:
                super.visitInsn(Opcodes.DUP);
                break;
            default:
                super.visitInsn(Opcodes.DUP2);
                break;
            }
            break;
        case Opcodes.DUP_X1:
            switch (top.getSort()) {
            case Type.OBJECT:
            case Type.ARRAY:
                if (second != null) {
                    switch (second.getSort()) {
                    case Type.OBJECT:
                    case Type.ARRAY:
                        super.visitInsn(Opcodes.DUP_X1);
                        break;
                    default:
                        super.visitInsn(Opcodes.DUP_X2);
                        break;
                    }
                }
                break;
            default:
                if (second != null) {
                    switch (second.getSort()) {
                    case Type.OBJECT:
                    case Type.ARRAY:
                        super.visitInsn(Opcodes.DUP2_X1);
                        break;
                    default:
                        super.visitInsn(Opcodes.DUP2_X2);
                        break;
                    }
                }
            }
            break;
        case Opcodes.DUP_X2:
            visitDupx2(top, second, third);
            break;
        case Opcodes.SWAP:
            switch (top.getSort()) {
            case Type.OBJECT:
            case Type.ARRAY:
                if (second != null) {
                    switch (second.getSort()) {
                    case Type.OBJECT:
                    case Type.ARRAY:
                        super.visitInsn(Opcodes.SWAP);
                        break;
                    default:
                        super.visitInsn(Opcodes.DUP_X2);
                        super.visitInsn(Opcodes.POP);
                        break;
                    }
                }
                break;
            default:
                if (second != null) {
                    switch (second.getSort()) {
                    case Type.OBJECT:
                    case Type.ARRAY:
                        super.visitInsn(Opcodes.DUP2_X1);
                        super.visitInsn(Opcodes.POP2);
                        break;
                    default:
                        super.visitInsn(Opcodes.DUP2_X2);
                        super.visitInsn(Opcodes.POP2);
                        break;
                    }
                }
            }
            break;
        case Opcodes.POP2:
            switch (top.getSort()) {
            case Type.OBJECT:
            case Type.ARRAY:
                switch (second.getSort()) {
                case Type.OBJECT:
                case Type.ARRAY:
                    super.visitInsn(Opcodes.POP2);
                    break;
                default:
                    super.visitInsn(Opcodes.POP);
                    super.visitInsn(Opcodes.POP2);
                    break;
                }
                break;
            case Type.DOUBLE:
            case Type.LONG:
                super.visitInsn(Opcodes.POP);
                super.visitInsn(Opcodes.POP2);
                break;
            default:
                switch (second.getSort()) {
                case Type.OBJECT:
                case Type.ARRAY:
                    super.visitInsn(Opcodes.POP2);
                    super.visitInsn(Opcodes.POP);
                    break;
                default:
                    super.visitInsn(Opcodes.POP2);
                    super.visitInsn(Opcodes.POP2);
                    break;
                }
            }
            break;
        case Opcodes.DUP2:
            /*
             * D AA AI IA II
             */
            switch (top.getSort()) {
            case Type.OBJECT:
            case Type.ARRAY:
                switch (second.getSort()) {
                case Type.OBJECT:
                case Type.ARRAY:
                    super.visitInsn(Opcodes.DUP2);
                    break;
                default: {
                    int tmp = nextFreeReg++;
                    super.visitVarInsn(Opcodes.ASTORE, tmp);
                    super.visitInsn(Opcodes.DUP2);
                    super.visitVarInsn(Opcodes.ALOAD, tmp);
                    super.visitInsn(Opcodes.DUP_X2);
                    break;
                }
                }
                break;
            case Type.DOUBLE:
            case Type.LONG: {
                int tmp = nextFreeReg++;
                super.visitVarInsn(Opcodes.ISTORE, tmp);
                super.visitInsn(Opcodes.DUP2);
                super.visitVarInsn(Opcodes.ILOAD, tmp);
                super.visitInsn(Opcodes.DUP_X2);
                break;
            }
            default:
                switch (second.getSort()) {
                case Type.OBJECT:
                case Type.ARRAY: {
                    int tmp = nextFreeReg++;
                    super.visitVarInsn(Opcodes.ISTORE, tmp);
                    super.visitInsn(Opcodes.DUP2);
                    super.visitVarInsn(Opcodes.ILOAD, tmp);
                    super.visitInsn(Opcodes.DUP_X2);
                    break;
                }
                default: {
                    // I2 It I1 It
                    int taint1 = nextFreeReg++;
                    int val1 = nextFreeReg++;
                    int taint2 = nextFreeReg++;
                    int val2 = nextFreeReg++;
                    super.visitVarInsn(Opcodes.ISTORE, taint1);
                    super.visitVarInsn(getStoreOpcode(top), val1);
                    super.visitVarInsn(Opcodes.ISTORE, taint2);
                    super.visitVarInsn(getStoreOpcode(second), val2);
                    super.visitVarInsn(getLoadOpcode(second), val2);
                    super.visitVarInsn(Opcodes.ILOAD, taint2);
                    super.visitVarInsn(getLoadOpcode(second), val1);
                    super.visitVarInsn(Opcodes.ILOAD, taint1);
                    super.visitVarInsn(getLoadOpcode(second), val2);
                    super.visitVarInsn(Opcodes.ILOAD, taint2);
                    super.visitVarInsn(getLoadOpcode(second), val1);
                    super.visitVarInsn(Opcodes.ILOAD, taint1);
                    // throw new
                    // RuntimeException("stack operations on double words not supported yet: "
                    // + top + ", " + second + ", " + third + ", " + forth);
                }
                }
            }
            break;
        case Opcodes.DUP2_X1:
            visitDup2x1(top, second, third);
            break;
        case Opcodes.DUP2_X2:
            visitDup2x2(top, second, third, forth);
            break;
        }
    }

    private void visitDupx2(Type top, Type second, Type third) {
        // AAA (DUP_X2) AAI (DUP_X3) AIA (DUP_X3) AII (DUP_X4)
        // IAA (DUP2_X2) IAI (DUP2_X3) IIA (DUP2_X3) III (DUP2_X4)
        // AD (DUP_X2) ID (DUP2_X2)
        switch (top.getSort()) {
        case Type.OBJECT:
        case Type.ARRAY:
            switch (second.getSort()) {
            case Type.OBJECT:
            case Type.ARRAY:
                switch (third.getSort()) {
                case Type.OBJECT:
                case Type.ARRAY:
                    super.visitInsn(Opcodes.DUP_X2);
                    return;
                default:
                    break;
                }
                break;
            case Type.DOUBLE:
            case Type.LONG: {
                int taintVar = nextFreeReg++;
                super.visitInsn(Opcodes.SWAP);
                super.visitVarInsn(Opcodes.ISTORE, taintVar);
                super.visitInsn(Opcodes.DUP_X2);
                super.visitVarInsn(Opcodes.ILOAD, taintVar);
                super.visitInsn(Opcodes.SWAP);
                return;
            }
            default:
                switch (third.getSort()) {
                case Type.OBJECT:
                case Type.ARRAY: {
                    int objVar = nextFreeReg++;
                    int taintVar = nextFreeReg++;
                    super.visitVarInsn(Opcodes.ASTORE, objVar);
                    super.visitVarInsn(Opcodes.ISTORE, taintVar);
                    super.visitVarInsn(Opcodes.ALOAD, objVar);
                    super.visitInsn(Opcodes.DUP_X2);
                    super.visitInsn(Opcodes.POP);
                    super.visitVarInsn(Opcodes.ILOAD, taintVar);
                    super.visitVarInsn(Opcodes.ALOAD, objVar);
                    return;
                }
                default:
                    break;
                }
            }
            break;
        default:
            switch (second.getSort()) {
            case Type.OBJECT:
            case Type.ARRAY:
            case Type.DOUBLE:
            case Type.LONG:
                break;
            default:
                int taintVar1 = nextFreeReg++;
                int valVar1 = nextFreeReg++;
                int taintVar2 = nextFreeReg++;
                int valVar2 = nextFreeReg++;
                super.visitVarInsn(Opcodes.ISTORE, taintVar1);
                super.visitVarInsn(top.getSort() == Type.FLOAT ? Opcodes.FSTORE : Opcodes.ISTORE, valVar1);
                super.visitVarInsn(Opcodes.ISTORE, taintVar2);
                super.visitVarInsn(second.getSort() == Type.FLOAT ? Opcodes.FSTORE : Opcodes.ISTORE, valVar2);
                super.visitVarInsn(top.getSort() == Type.FLOAT ? Opcodes.FLOAD : Opcodes.ILOAD, valVar1);
                super.visitVarInsn(Opcodes.ILOAD, taintVar1);

                switch (third.getSort()) {
                case Type.OBJECT:
                case Type.ARRAY:
                    super.visitInsn(Opcodes.DUP2_X1);
                    break;
                default:
                    super.visitInsn(Opcodes.DUP2_X2);
                    break;
                }

                super.visitVarInsn(second.getSort() == Type.FLOAT ? Opcodes.FLOAD : Opcodes.ILOAD, valVar2);
                super.visitVarInsn(Opcodes.ILOAD, taintVar2);
                super.visitInsn(Opcodes.DUP2_X2);
                super.visitInsn(Opcodes.POP2);
                return;
            }
        }
        throw new RuntimeException(
                "stack operations on double words not supported yet: " + top + ", " + second + ", " + third);
    }

    /*
     * I D1 D2 A
     */
    private void visitDup2x1(Type top, Type second, Type third) {
        /*
         * DI DA AAA AAI AIA AII IAA IAI IIA III
         */
        switch (top.getSort()) {
        case Type.OBJECT:
        case Type.ARRAY:
            switch (second.getSort()) {
            case Type.OBJECT:
            case Type.ARRAY:
                switch (third.getSort()) {
                case Type.OBJECT:
                case Type.ARRAY:
                    super.visitInsn(Opcodes.DUP2_X1);
                    return;
                }
            }
            break;
        case Type.DOUBLE:
        case Type.LONG:
            switch (second.getSort()) {
            case Type.OBJECT:
            case Type.ARRAY:
                /*
                 * TOP OF STACK -> I D1 D2 A => I D1 D2 A I D1 D2
                 */
                int taint1 = nextFreeReg++;
                int doubleWord = nextFreeReg++;
                nextFreeReg++;
                int object = nextFreeReg++;
                super.visitVarInsn(Opcodes.ISTORE, taint1);
                super.visitVarInsn(top.getSort() == Type.LONG ? Opcodes.LSTORE : Opcodes.DSTORE, doubleWord);
                super.visitVarInsn(Opcodes.ASTORE, object);
                super.visitVarInsn(top.getSort() == Type.LONG ? Opcodes.LLOAD : Opcodes.DLOAD, doubleWord);
                super.visitVarInsn(Opcodes.ILOAD, taint1);
                super.visitVarInsn(Opcodes.ALOAD, object);
                super.visitVarInsn(top.getSort() == Type.LONG ? Opcodes.LLOAD : Opcodes.DLOAD, doubleWord);
                super.visitVarInsn(Opcodes.ILOAD, taint1);
                return;
            default:
            }
            break;
        default:
            switch (second.getSort()) {
            case Type.OBJECT:
            case Type.ARRAY:
                switch (second.getSort()) {
                case Type.OBJECT:
                case Type.ARRAY: {
                    /*
                     * IA A -> IIA A
                     */
                    int taint = nextFreeReg++;
                    int firstObj = nextFreeReg++;
                    super.visitVarInsn(Opcodes.ISTORE, taint);
                    super.visitInsn(Opcodes.SWAP);
                    super.visitInsn(Opcodes.DUP_X2);
                    super.visitVarInsn(Opcodes.ASTORE, firstObj);
                    super.visitInsn(Opcodes.DUP_X1);
                    super.visitVarInsn(Opcodes.ILOAD, taint);
                    super.visitInsn(Opcodes.DUP_X2);
                    super.visitVarInsn(Opcodes.ALOAD, firstObj);
                    super.visitInsn(Opcodes.DUP_X2);
                    super.visitInsn(Opcodes.POP);
                    return;
                }
                default:
                }
                break;
            default:
            }
        }
        throw new RuntimeException(
                "stack operations on double words not supported yet: " + top + ", " + second + ", " + third);
    }

    private void visitDup2x2(Type top, Type second, Type third, Type forth) {

        /*
         * DD DAA DAI DIA- DII AAD AID IAD IID AAAA AAAI AAIA AAII AIAA AIAI
         * AIIA AIII IAAA IAAI IAIA IAII IIAA IIAI IIIA IIII
         */
        // DIA
        switch (top.getSort()) {
        case Type.OBJECT:
        case Type.ARRAY:
            break;
        case Type.DOUBLE:
        case Type.LONG: {
            switch (second.getSort()) {
            case Type.OBJECT:
            case Type.ARRAY:
                break;
            case Type.DOUBLE:
            case Type.LONG: {
                // J1 J2 I J1 J2 I
                int longTaint = nextFreeReg++;
                int longVal = nextFreeReg++;
                nextFreeReg++;
                int longTaint2 = nextFreeReg++;
                int longVal2 = nextFreeReg++;
                nextFreeReg++;
                super.visitVarInsn(Opcodes.ISTORE, longTaint);
                super.visitVarInsn(top.getSort() == Type.DOUBLE ? Opcodes.DSTORE : Opcodes.LSTORE, longVal);
                super.visitVarInsn(Opcodes.ISTORE, longTaint2);
                super.visitVarInsn(second.getSort() == Type.DOUBLE ? Opcodes.DSTORE : Opcodes.LSTORE, longVal2);
                super.visitVarInsn(top.getSort() == Type.DOUBLE ? Opcodes.DLOAD : Opcodes.LLOAD, longVal);
                super.visitVarInsn(Opcodes.ILOAD, longTaint);
                super.visitVarInsn(second.getSort() == Type.DOUBLE ? Opcodes.DLOAD : Opcodes.LLOAD, longVal2);
                super.visitVarInsn(Opcodes.ILOAD, longTaint2);
                super.visitVarInsn(top.getSort() == Type.DOUBLE ? Opcodes.DLOAD : Opcodes.LLOAD, longVal);
                super.visitVarInsn(Opcodes.ILOAD, longTaint);
                return;
            }
            default:
                switch (third.getSort()) {
                case Type.OBJECT:
                case Type.ARRAY:
                    // ID II A -> ID II A ID
                    int longTaint = nextFreeReg++;
                    int longVal = nextFreeReg++;
                    nextFreeReg++;
                    int intTaint = nextFreeReg++;
                    int intVal = nextFreeReg++;
                    int objVal = nextFreeReg++;
                    super.visitVarInsn(Opcodes.ISTORE, longTaint);
                    super.visitVarInsn(top.getSort() == Type.DOUBLE ? Opcodes.DSTORE : Opcodes.LSTORE, longVal);
                    super.visitVarInsn(Opcodes.ISTORE, intTaint);
                    super.visitVarInsn(third.getSort() == Type.FLOAT ? Opcodes.FSTORE : Opcodes.ISTORE, intVal);
                    super.visitVarInsn(Opcodes.ASTORE, objVal);
                    super.visitVarInsn(top.getSort() == Type.DOUBLE ? Opcodes.DLOAD : Opcodes.LLOAD, longVal);
                    super.visitVarInsn(Opcodes.ILOAD, longTaint);
                    super.visitVarInsn(Opcodes.ALOAD, objVal);
                    super.visitVarInsn(third.getSort() == Type.FLOAT ? Opcodes.FLOAD : Opcodes.ILOAD, intVal);
                    super.visitVarInsn(Opcodes.ILOAD, intTaint);
                    super.visitVarInsn(top.getSort() == Type.DOUBLE ? Opcodes.DLOAD : Opcodes.LLOAD, longVal);
                    super.visitVarInsn(Opcodes.ILOAD, longTaint);
                    return;
                case Type.DOUBLE:
                case Type.LONG:
                    break;
                default:

                }
            }
            break;
        }
        default:
        }
        throw new RuntimeException("stack operations on double words not supported yet: " + top + ", " + second + ", "
                + third + ", " + forth);
    }

    protected static void callUninstrumented(String owner, MethodNode methodNode, String originalMethodDesc) {
        if (methodNode.localVariables != null)
            methodNode.localVariables.clear();
        if (methodNode.tryCatchBlocks != null)
            methodNode.tryCatchBlocks.clear();
        if (methodNode.instructions != null)
            methodNode.instructions.clear();
        else
            methodNode.instructions = new InsnList();

        // remove the native access modifier and add instructions
        // to call the original native method on the original parameters.
        if (AsmUtils.hasModifiers(methodNode.access, Opcodes.ACC_NATIVE))
            methodNode.access = methodNode.access - Opcodes.ACC_NATIVE;
        if (AsmUtils.hasModifiers(methodNode.access, Opcodes.ACC_ABSTRACT))
            methodNode.access = methodNode.access - Opcodes.ACC_ABSTRACT;

        boolean isStatic = AsmUtils.hasModifiers(methodNode.access, Opcodes.ACC_STATIC);
        int idx = 0;
        if (!isStatic) {
            methodNode.instructions.add(new VarInsnNode(Opcodes.ALOAD, idx++));
        }

        Type[] argTypes = Type.getArgumentTypes(originalMethodDesc);
        for (int i = 0; i < argTypes.length; i++) {
            methodNode.instructions.add(new VarInsnNode(getLoadOpcode(argTypes[i]), idx++));
            switch (argTypes[i].getSort()) {
            case Type.OBJECT:
                if (AsmUtils.canBePrimitiveArrayType(argTypes[i])) {
                    methodNode.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "pac/wrap/ArrayTaint",
                            "toValueArray", "(Ljava/lang/Object;)Ljava/lang/Object;", false));
                }
                break;
            case Type.ARRAY:
                Type primType = AsmUtils.toPrimitiveArrayType(argTypes[i]);
                if (primType != null) {
                    Type arrayTaintType = AsmUtils.toWrappedArrayType(argTypes[i].getElementType());
                    methodNode.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                            arrayTaintType.getInternalName(), "toValueArray",
                            "(" + primType.getDescriptor() + ")" + argTypes[i].getDescriptor(), false));
                } else if (AsmUtils.canBePrimitiveArrayType(argTypes[i])) {
                    methodNode.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "pac/wrap/ArrayTaint",
                            "toValueArray", "(Ljava/lang/Object;)Ljava/lang/Object;", false));
                    methodNode.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, argTypes[i].getInternalName()));
                }
                break;
            case Type.LONG:
            case Type.DOUBLE:
                idx += 2;
                break;
            default:
                idx++;
                break;
            }
        }

        int retObjReg = idx;

        methodNode.instructions.add(new MethodInsnNode(!isStatic ? Opcodes.INVOKEVIRTUAL : Opcodes.INVOKESTATIC, owner,
                methodNode.name, originalMethodDesc, false));

        int returnOpcode;
        Type returnType = Type.getReturnType(originalMethodDesc);
        switch (returnType.getSort()) {
        case Type.ARRAY:
        case Type.OBJECT:
            returnOpcode = Opcodes.ARETURN;
            break;
        case Type.VOID:
            returnOpcode = Opcodes.RETURN;
            break;
        case Type.DOUBLE:
            returnOpcode = Opcodes.DRETURN;
            break;
        case Type.LONG:
            returnOpcode = Opcodes.LRETURN;
            break;
        case Type.FLOAT:
            returnOpcode = Opcodes.FRETURN;
            break;
        default:
            returnOpcode = Opcodes.IRETURN;
            break;
        }

        if (returnOpcode == Opcodes.ARETURN) {
            Type primType = AsmUtils.toPrimitiveArrayType(returnType);
            if (primType != null) {
                Type arrayTaintType = AsmUtils.toWrappedArrayType(returnType.getElementType());
                methodNode.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, arrayTaintType.getInternalName(),
                        "toTaintArray", "(" + returnType.getDescriptor() + ")" + primType.getDescriptor(), false));
            } else if (AsmUtils.canBePrimitiveArrayType(returnType)) {
                methodNode.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "pac/wrap/ArrayTaint",
                        "toTaintArray", "(Ljava/lang/Object;)Ljava/lang/Object;", false));
                methodNode.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, returnType.getInternalName()));
            }
        } else if (returnOpcode != Opcodes.RETURN) {
            methodNode.instructions.add(new VarInsnNode(Opcodes.ALOAD, retObjReg)); // set the
                                                                                    // return
                                                                                    // type to
                                                                                    // unknown
            methodNode.instructions.add(new InsnNode(UNKNOWN_OPCODE));
            methodNode.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, "pac/util/Ret", "taint", "I"));
        }
        methodNode.instructions.add(new InsnNode(returnOpcode));
    }

    @Override
    public void visitEnd() {
        // reset the descriptor for static class block methods, since these cannot
        // obviously take arguments.
        if (this.name.equals("<clinit>")) {
            // reset the method descriptor back to the original descriptor
            // and initialize Ret variable at the start of the static
            // class block...
            this.desc = "()V";
            InsnList insnList = new InsnList();
            insnList.add(new TypeInsnNode(Opcodes.NEW, "pac/util/Ret"));
            insnList.add(new InsnNode(Opcodes.DUP));
            insnList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "pac/util/Ret", "<init>", "()V", false));
            insnList.add(new VarInsnNode(Opcodes.ASTORE, retObjReg));
            this.instructions.insert(insnList);
        } else if (AsmUtils.hasModifiers(this.access, Opcodes.ACC_NATIVE)
                || (AsmUtils.hasModifiers(this.access, Opcodes.ACC_ABSTRACT)
                        && !AsmUtils.hasModifiers(this.ownerAccess, Opcodes.ACC_INTERFACE))) {
            /* FIXME This isn't exactly right... We shouldn't need to do this
             * for abstract methods since this is corrected for (see references to 
             * ClassHierarchy.overridesInstrumentedMethod()).  The problem is that
             * some dangerous types extend abstract classes.  Consider the following
             * example:
             *
             * Number n = uninstCall();  // n is Integer, but not CleartrackInteger
             * int x = n.intValue(Ret);  // AbstractMethodError!
             */
            callUninstrumented(owner, this, originalMethod.desc);
        }

        if (!inJdk
                //#if bootstrap==false
                && getConfigInstance().areDeadlocksEnabled()
                //#endif
                && AsmUtils.hasModifiers(this.access, Opcodes.ACC_SYNCHRONIZED)) {
            // initialize labels
            LabelNode startTry = new LabelNode();
            LabelNode firstTry = startTry;
            LabelNode endTry = new LabelNode();
            LabelNode startHandler = new LabelNode();
            LabelNode endHandler = new LabelNode();

            // We need to turn off the synchronized modifier on both methods.
            // Synchronization in the copied method will happen via ReentrantLocks,
            // and the original method will call the copied method since we are
            // in an application method.  So the original method will not need to
            // be synchronized.
            this.access = this.access & ~Opcodes.ACC_SYNCHRONIZED;
            originalMethod.access = originalMethod.access & ~Opcodes.ACC_SYNCHRONIZED;

            // set the monitor register
            int monitorReg = nextFreeReg++;

            // add instructions to exit the lock at each return instruction
            for (AbstractInsnNode insnNode = instructions.getFirst(); insnNode != null; insnNode = insnNode.getNext()) {
                switch (insnNode.getOpcode()) {
                case Opcodes.RETURN:
                case Opcodes.IRETURN:
                case Opcodes.ARETURN:
                case Opcodes.FRETURN:
                case Opcodes.LRETURN:
                case Opcodes.DRETURN: {
                    InsnList insnList = new InsnList();
                    insnList.add(new FieldInsnNode(Opcodes.GETSTATIC, "pac/util/ThreadMonitor", "INSTANCE",
                            "Lpac/util/ThreadMonitor;"));
                    insnList.add(new VarInsnNode(Opcodes.ALOAD, monitorReg));
                    insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "pac/util/ThreadMonitor", "monitorexit",
                            "(Ljava/lang/Object;)V", false));
                    insnList.add(endTry);
                    tryCatchBlocks.add(new TryCatchBlockNode(startTry, endTry, startHandler, null));
                    instructions.insertBefore(insnNode, insnList);
                    startTry = new LabelNode();
                    endTry = new LabelNode();
                    instructions.insert(insnNode, startTry);
                }
                }
            }

            // add instructions to enter the lock
            InsnList insnList = new InsnList();
            insnList.add(new FieldInsnNode(Opcodes.GETSTATIC, "pac/util/ThreadMonitor", "INSTANCE",
                    "Lpac/util/ThreadMonitor;"));
            if (isStatic)
                insnList.add(new LdcInsnNode(Type.getObjectType(owner)));
            else
                insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
            insnList.add(new InsnNode(Opcodes.DUP));
            insnList.add(new VarInsnNode(Opcodes.ASTORE, monitorReg));
            insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "pac/util/ThreadMonitor", "monitorenter",
                    "(Ljava/lang/Object;)V", false));
            insnList.add(firstTry);
            instructions.insert(insnList);

            // add instructions to exit the lock (in the finally block for each return)
            instructions.add(startHandler);
            instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, "pac/util/ThreadMonitor", "INSTANCE",
                    "Lpac/util/ThreadMonitor;"));
            instructions.add(new VarInsnNode(Opcodes.ALOAD, monitorReg));
            instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "pac/util/ThreadMonitor", "monitorexit",
                    "(Ljava/lang/Object;)V", false));
            instructions.add(endHandler);
            instructions.add(new InsnNode(Opcodes.ATHROW));
            instructions.add(new LabelNode());
            tryCatchBlocks.add(new TryCatchBlockNode(startHandler, endHandler, startHandler, null));
        }
    }

    private void visitNumberInsn(int n) {
        switch (n) {
        case -1:
            super.visitInsn(Opcodes.ICONST_M1);
            break;
        case 0:
            super.visitInsn(Opcodes.ICONST_0);
            break;
        case 1:
            super.visitInsn(Opcodes.ICONST_1);
            break;
        case 2:
            super.visitInsn(Opcodes.ICONST_2);
            break;
        case 3:
            super.visitInsn(Opcodes.ICONST_3);
            break;
        case 4:
            super.visitInsn(Opcodes.ICONST_4);
            break;
        case 5:
            super.visitInsn(Opcodes.ICONST_5);
            break;
        default:
            if (n <= Byte.MAX_VALUE && n >= Byte.MIN_VALUE)
                super.visitIntInsn(Opcodes.BIPUSH, n);
            else if (n <= Short.MAX_VALUE && n >= Short.MIN_VALUE)
                super.visitIntInsn(Opcodes.SIPUSH, n);
            else
                super.visitLdcInsn(n);
        }
    }

    // ********************************************************************************
    // THE METHODS BELOW WILL ONLY BE ADDED AFTER THE INSTRUMENTATION HAS BEEN
    // PROPERLY
    // BOOTSTRAPPED...
    // ********************************************************************************

    //#if bootstrap==false
    private void instrumentBeforeChecks(pac.config.Desc d, String methodOwner, String methodDesc) {
        Label skipLabel = new Label();

        // Insert descriptions precondition to skip check based on a static
        // method (that takes no arguments and returns a boolean)...
        pac.config.Desc.Condition descCond = d.getCondition();
        if (descCond != null) {
            String method = descCond.getMethod();
            if (method != null) {
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "pac/config/BaseConfig", "getInstance",
                                      "()Lpac/config/BaseConfig;", false);
                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "pac/config/BaseConfig", method, "()Z", false);
                super.visitJumpInsn(Opcodes.IFEQ, skipLabel);
            }
        }

        // Construct an array of the method arguments...
        pac.config.Desc.Condition[] conds = d.getBeforeConditions();
        visitNumberInsn(conds.length);
        super.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");

        // Mark whether or not this check is on an instrumented method
        Type[] types = Type.getArgumentTypes(methodDesc);
        boolean isInstrumented = false;
        if (types.length > 0 && types[types.length - 1].getSort() == Type.OBJECT
                && types[types.length - 1].getInternalName().equals("pac/util/Ret")) {
            isInstrumented = true;
        }

        Stack<Label> labelStack = new Stack<Label>();

        // descend through types and push arguments onto list...
        int k = conds.length - 1;
        for (int i = types.length - 1; i >= 0; i--) {
            Type primType;
            if (isInstrumented)
                primType = i < 1 || types[i].getSort() != Type.INT ? null : AsmUtils.toWrappedType(types[i - 1]);
            else
                primType = AsmUtils.toWrappedType(types[i]);
            String primClass = primType == null ? null : primType.getInternalName();

            if (isInstrumented && i == types.length - 1) { // pop the Ret
                                                           // object
                super.visitInsn(Opcodes.DUP_X1);
                super.visitInsn(Opcodes.SWAP);
                super.visitInsn(Opcodes.POP2);
                continue;
            }

            if (primClass != null && isInstrumented) {
                if (types[i].getSize() == 2) {
                    throw new RuntimeException("doubles in desc items are not supported yet!");
                } else {
                    super.visitInsn(Opcodes.DUP_X2);
                    super.visitInsn(Opcodes.DUP_X2);
                    super.visitInsn(Opcodes.POP);
                    visitNumberInsn(k);
                    super.visitInsn(Opcodes.DUP_X2);
                    super.visitInsn(Opcodes.POP);
                }
            } else {
                super.visitInsn(Opcodes.DUP_X1);
                super.visitInsn(Opcodes.SWAP);
                visitNumberInsn(k);
                super.visitInsn(Opcodes.SWAP);
            }

            if (conds[k] != null)
                super.visitInsn(Opcodes.DUP_X2);

            // insert instructions to convert all primitives to a wrapped
            // object.
            if (primClass != null) {
                if (isInstrumented) {
                    super.visitVarInsn(Opcodes.ALOAD, retObjReg);
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, primClass, "valueOf",
                                          "(" + types[i - 1].getDescriptor() + "ILpac/util/Ret;)L" + primClass + ";",
                                          false);
                    i--;
                } else {
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, primClass, "valueOf",
                                          "(" + types[i].getDescriptor() + ")L" + primClass + ";", false);
                }
            }

            super.visitInsn(Opcodes.AASTORE);

            // If this argument has a condition then insert a condition that
            // will allow the skip to check and pop the arguments back into
            // their
            // place so the method can resume as normal...
            if (conds[k] != null) {
                String methodStr = conds[k].getMethod();
                if (methodStr != null) {
                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, methodOwner, methodStr, "()Ljava/lang/String;",
                                          hierarchy.isInterface(Type.getType(methodOwner)));
                }
                super.visitLdcInsn(conds[k].getValue());
                super.visitInsn(Opcodes.SWAP);
                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z",
                                      false);
                Label label = new Label();
                super.visitJumpInsn(conds[k].getOp(), label);
                labelStack.push(label);
            } else {
                labelStack.push(null);
            }

            k--;
        }

        // call notify on argument list and description...
        super.visitLdcInsn(d.getCommand()); // maxStack + 3 at this
                                            // point
        super.visitMethodInsn(Opcodes.INVOKESTATIC, "pac/config/Notify", "run_checks",
                              "([Ljava/lang/Object;Ljava/lang/String;)[Ljava/lang/Object;", false);

        // iterate over types and push the (potentially new) arguments
        // onto the stack...
        k = 0;
        for (int i = 0; i < types.length; i++) {
            if (isInstrumented && i == types.length - 1) { // we must put Ret obj
                                                           // back on the stack
                super.visitInsn(Opcodes.POP);
                super.visitVarInsn(Opcodes.ALOAD, retObjReg);
                super.visitLabel(skipLabel);
                return;
            }

            Label label = labelStack.pop();
            if (label != null)
                super.visitLabel(label);
            super.visitInsn(Opcodes.DUP);
            visitNumberInsn(k);
            super.visitInsn(Opcodes.AALOAD);

            // cast Object back to the argument type...
            Type primType = AsmUtils.toWrappedType(types[i]);
            if (primType != null) {
                String primClass = primType.getInternalName();
                if (isInstrumented) {
                    super.visitTypeInsn(Opcodes.CHECKCAST, primClass);
                    super.visitVarInsn(Opcodes.ALOAD, retObjReg);
                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, primClass, types[i].getClassName() + "Value",
                                          "(Lpac/util/Ret;)" + types[i].getDescriptor(), false);
                    super.visitVarInsn(Opcodes.ALOAD, retObjReg);
                    super.visitFieldInsn(Opcodes.GETFIELD, "pac/util/Ret", "taint", "I");
                    super.visitInsn(Opcodes.DUP2_X1);
                    super.visitInsn(Opcodes.POP2);
                    i++;
                } else {
                    super.visitTypeInsn(Opcodes.CHECKCAST, primClass);
                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, primClass, types[i].getClassName() + "Value",
                                          "()" + types[i].getDescriptor(), false);
                    super.visitInsn(Opcodes.SWAP);
                }
            } else {
                super.visitTypeInsn(Opcodes.CHECKCAST, types[i].getInternalName());
                super.visitInsn(Opcodes.SWAP);
            }
            k++;
        }

        super.visitInsn(Opcodes.POP);
        super.visitLabel(skipLabel);
        // methodNode.instructions.insertBefore(methodInsnNode, insnList);
        // methodNode.maxStack += 3;
    }

    private void instrumentAfterCheck(pac.config.Desc d, String methodOwner) {
        // Acquire the object type from the top of the stack, just after
        // the method invocation.  If there is no object on top of the
        // stack, then ignore this check.  It's possible that an Object
        // can be created and never stored, for example.  If the check
        // happened to be on this constructor, we would fail here.
        if (instOffset + 1 >= frames.length)
            return;
        Frame<BasicValue> frame = frames[instOffset + 1];
        if (frame == null)
            return;
        int top = frame.getStackSize() - 1;
        if (top < 0)
            return;
        Type objType = frame.getStack(top).getType();
        if (objType.getSort() != Type.OBJECT)
            return;
        String className = objType.getInternalName();

        Label label = new Label();
        pac.config.Desc.Condition descCond = d.getCondition();

        // Insert descriptions precondition to skip check based on a static
        // method (that takes no arguments and returns a boolean)...
        if (descCond != null) {
            String method = descCond.getMethod();
            if (method != null) {
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "pac/config/BaseConfig", "getInstance",
                                      "()Lpac/config/BaseConfig;", false);
                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "pac/config/BaseConfig", method, "()Z", false);
                super.visitJumpInsn(Opcodes.IFEQ, label);
            }
        }

        // Insert an after condition that operates on either the constructed
        // object (if method was a constructer) or the returned object...
        pac.config.Desc.Condition cond = d.getAfterCondition();
        if (cond != null) {
            super.visitInsn(Opcodes.DUP);
            String methodStr = cond.getMethod();
            if (methodStr != null) {
                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, methodOwner, methodStr, "()Ljava/lang/String;",
                                      hierarchy.isInterface(Type.getType(methodOwner)));
            }
            super.visitLdcInsn(cond.getValue());
            super.visitInsn(Opcodes.SWAP);
            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
            super.visitJumpInsn(cond.getOp(), label);
        }

        super.visitLdcInsn(d.getCommand());
        super.visitMethodInsn(Opcodes.INVOKESTATIC, "pac/config/Notify", "run_after_check",
                              "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;", false);
        super.visitTypeInsn(Opcodes.CHECKCAST, className);
        super.visitLabel(label);
        // methodNode.instructions.insert(methodInsnNode, insnList);
        // return label;
    }
    //#endif
}
