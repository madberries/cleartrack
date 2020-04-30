package pac.agent;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import pac.org.objectweb.asm.Label;
import pac.org.objectweb.asm.Opcodes;
import pac.org.objectweb.asm.Type;
import pac.org.objectweb.asm.tree.MethodNode;
import pac.util.Ansi;
import pac.util.AsmUtils;
import static pac.util.AsmUtils.UNKNOWN_OPCODE;

/**
 * <P>This is essentially an instrumented method with the original bytecode for the
 * method body.  In otherwords, it's the original method but with the instrumented
 * method descriptor.  Taint is stripped from the parameters, and unknown taint is
 * added to return values that expect taint.</P>
 * 
 * <P>This class is used for both dangerous classes, and also situations with abstract
 * methods and interfaces (i.e. when instrumented definitions need to exist in non-
 * instrumented classes).</P>
 * 
 * @author jeikenberry
 */
public class CleartrackSkippedMethodAdapter extends MethodNode {

    /** old param reg to new param reg map */
    private Map<Integer, Integer> paramMap;

    /** new param local to shadow taint map */
    private Map<Integer, Integer> localTaintMap;

    /** maps the original array register to a new register of 
        where the value array will be extracted */
    private Map<Integer, Integer> primArrayMap;

    private MethodNode originalMethod;

    private String owner;

    private int varOffset, retObjReg, nextFreeReg;
    private boolean isStatic;

    private static int removeVarargs(int access) {
        if ((access & Opcodes.ACC_VARARGS) == Opcodes.ACC_VARARGS)
            return access - Opcodes.ACC_VARARGS;
        return access;
    }

    public CleartrackSkippedMethodAdapter(String owner, MethodNode methodNode) {
        super(Opcodes.ASM5, removeVarargs(methodNode.access), methodNode.name, toPrimitiveDesc(methodNode.desc),
                CleartrackSignatureWriter.instrumentSignature(methodNode.signature),
                methodNode.exceptions.toArray(new String[0]));

        this.originalMethod = methodNode;
        this.owner = owner;

        varOffset = 0;
        paramMap = new LinkedHashMap<Integer, Integer>();
        localTaintMap = new LinkedHashMap<Integer, Integer>();
        primArrayMap = new LinkedHashMap<Integer, Integer>();

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
            reg += argTypes[i].getSize();
        }
        retObjReg = reg + varOffset;
        varOffset++;
        nextFreeReg = methodNode.maxLocals + varOffset;

        reg = isStatic ? 0 : 1;
        for (int i = 0; i < argTypes.length; i++) {
            if (AsmUtils.isPrimitiveArrayType(argTypes[i]) || AsmUtils.canBePrimitiveArrayType(argTypes[i])) {
                primArrayMap.put(reg, nextFreeReg++);
            }
            reg++;
            if (argTypes[i].getSize() == 2)
                reg++;
        }
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

    }

    @Override
    public void visitVarInsn(int opcode, int var) {
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
                var = primArrayMap.containsKey(var) ? primArrayMap.get(var) : paramMap.get(var);
            }
        } else {
            var += varOffset;
            if (!localTaintMap.containsKey(var))
                localTaintMap.put(var, nextFreeReg++);
        }

        super.visitVarInsn(opcode, var);
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

    @Override
    public void visitCode() {
        int reg = isStatic ? 0 : 1;
        Type[] argTypes = Type.getArgumentTypes(originalMethod.desc);
        for (int i = 0; i < argTypes.length; i++) {
            Type primArrType = AsmUtils.toPrimitiveArrayType(argTypes[i]);
            if (primArrType != null) {
                super.visitVarInsn(Opcodes.ALOAD, paramMap.get(reg));
                String arrayTaintClass = primArrType.getSort() == Type.OBJECT ? primArrType.getInternalName()
                        : primArrType.getElementType().getInternalName();
                super.visitMethodInsn(Opcodes.INVOKESTATIC, arrayTaintClass, "toValueArray",
                                      "(" + primArrType.getDescriptor() + ")" + argTypes[i].getDescriptor(), false);
                super.visitVarInsn(Opcodes.ASTORE, primArrayMap.get(reg));
            } else if (AsmUtils.canBePrimitiveArrayType(argTypes[i])) {
                super.visitVarInsn(Opcodes.ALOAD, paramMap.get(reg));
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "pac/wrap/ArrayTaint", "toValueArray",
                                      "(Ljava/lang/Object;)Ljava/lang/Object;", false);
                if (argTypes[i].getSort() == Type.ARRAY) // need to cast
                                                         // back to array
                                                         // of objects
                    super.visitTypeInsn(Opcodes.CHECKCAST, argTypes[i].getInternalName());
                super.visitVarInsn(Opcodes.ASTORE, primArrayMap.get(reg));
            }
            reg++;
            if (argTypes[i].getSize() == 2)
                reg++;
        }
        super.visitCode();
    }

    @Override
    public void visitInsn(int opcode) {
        switch (opcode) {
        case Opcodes.IRETURN:
        case Opcodes.FRETURN:
        case Opcodes.LRETURN:
        case Opcodes.DRETURN:
            super.visitVarInsn(Opcodes.ALOAD, retObjReg);
            super.visitInsn(UNKNOWN_OPCODE);
            super.visitFieldInsn(Opcodes.PUTFIELD, "pac/util/Ret", "taint", "I");
            break;
        case Opcodes.ARETURN:
            Type returnType = Type.getReturnType(originalMethod.desc);
            Type primArrType = AsmUtils.toPrimitiveArrayType(returnType);
            if (primArrType != null) {
                String arrayTaintClass = primArrType.getSort() == Type.OBJECT ? primArrType.getInternalName()
                        : primArrType.getElementType().getInternalName();
                super.visitMethodInsn(Opcodes.INVOKESTATIC, arrayTaintClass, "toTaintArray",
                                      "(" + returnType.getDescriptor() + ")" + primArrType.getDescriptor(), false);
            } else if (AsmUtils.canBePrimitiveArrayType(returnType)) {
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "pac/wrap/ArrayTaint", "toTaintArray",
                                      "(Ljava/lang/Object;)Ljava/lang/Object;", false);
                if (returnType.getSort() == Type.ARRAY) // need to cast
                                                        // back to array
                                                        // of objects
                    super.visitTypeInsn(Opcodes.CHECKCAST, returnType.getInternalName());
            }
            break;
        }
        super.visitInsn(opcode);
    }
    
}
