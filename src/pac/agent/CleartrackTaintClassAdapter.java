package pac.agent;

import static pac.util.AsmUtils.TAINTED_OPCODE;
import static pac.util.AsmUtils.TRUSTED_OPCODE;
import static pac.util.AsmUtils.UNKNOWN_OPCODE;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import pac.agent.hierarchy.MethodType;
import pac.org.objectweb.asm.ClassVisitor;
import pac.org.objectweb.asm.Opcodes;
import pac.org.objectweb.asm.Type;
import pac.org.objectweb.asm.tree.AbstractInsnNode;
import pac.org.objectweb.asm.tree.FieldInsnNode;
import pac.org.objectweb.asm.tree.FieldNode;
import pac.org.objectweb.asm.tree.FrameNode;
import pac.org.objectweb.asm.tree.IincInsnNode;
import pac.org.objectweb.asm.tree.InsnList;
import pac.org.objectweb.asm.tree.InsnNode;
import pac.org.objectweb.asm.tree.JumpInsnNode;
import pac.org.objectweb.asm.tree.LabelNode;
import pac.org.objectweb.asm.tree.LdcInsnNode;
import pac.org.objectweb.asm.tree.LocalVariableNode;
import pac.org.objectweb.asm.tree.MethodInsnNode;
import pac.org.objectweb.asm.tree.MethodNode;
import pac.org.objectweb.asm.tree.TryCatchBlockNode;
import pac.org.objectweb.asm.tree.TypeInsnNode;
import pac.org.objectweb.asm.tree.VarInsnNode;
import pac.org.objectweb.asm.tree.analysis.BasicValue;
import pac.org.objectweb.asm.tree.analysis.Frame;
import pac.util.Ansi;
import pac.util.AsmUtils;
import pac.util.TaintValues;

/**
 * This is used to generate a semantically equivalent {@code ClassNode} into
 * one that contains instrumented fields/methods (presuming that the class
 * representing this {@code ClassNode} should be instrumented (according to
 * the rules provided in the *.skip files). Some methods will need to be
 * instrumented (or even added) in certain cases, to prevent a runtime
 * AbstractMethodError. This is determined by making queries to the
 * {@code ClassHierarchy}.
 * 
 * @author jeikenberry
 */
public class CleartrackTaintClassAdapter extends CleartrackBaseInstrumentationAdapter {
    private Set<MethodNode> recursiveMethods;
    private boolean dynamicMode;

    public CleartrackTaintClassAdapter(ClassVisitor cv, boolean editMode, boolean dynamicMode) {
        super(cv, editMode);
        this.recursiveMethods = new HashSet<MethodNode>();
        this.dynamicMode = dynamicMode;
    }

    /**
     * 
     * @param access
     *            int of the access modifier
     * @return access with the public bit set, and the private and protected bit
     *         unset.
     */
    public final static int makePublic(int access) {
        if (AsmUtils.hasModifiers(access, Opcodes.ACC_PRIVATE)) {
            access = access - Opcodes.ACC_PRIVATE;
        } else if (AsmUtils.hasModifiers(access, Opcodes.ACC_PROTECTED)) {
            access = access - Opcodes.ACC_PROTECTED;
        }
        return access | Opcodes.ACC_PUBLIC;
    }

    /**
     * It is possible that Java a class can contain fields all with the same
     * name but each with a different type. This is apparently legal with
     * respect to the JavaVM, though I can't see how anything like this would
     * ever be compiled. To counteract this problem we must make any duplicate
     * primitive field it's own unique taint field (by appending the descriptor
     * onto the field name).
     * 
     * @param origFieldName
     *            String of the original field name that we should instrument.
     * @param desc
     *            String of the descriptor of the original field.
     * @return String of the name for the instrumented field.
     */
    public String getFieldInstName(String origFieldName, String desc) {
        return AsmUtils.getFieldInstName(this.name, origFieldName, desc);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        //		access = makePublic(access); // this is necessary because of the
        // subtyping for dangerous types.
        if (AsmUtils.hasModifiers(access, Opcodes.ACC_INTERFACE))
            access = access | Opcodes.ACC_ABSTRACT;

        // Method splitting requires all classes to be >= JDK 1.6
        if ((version & 0xffff) < Opcodes.V1_6)
            version = Opcodes.V1_6;

        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    protected void addExtraFields() {
        // Add any extraneous fields that are required for the
        // instrumentation...
        if (this.name.equals("java/lang/String")) {
            super.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_TRANSIENT, "ss_interned", "Ljava/lang/String;", null,
                             null).visitEnd();
        } else if (this.name.equals("java/lang/Thread")) {
            super.visitField(Opcodes.ACC_PUBLIC, "ss_estack", "Ljava/util/Stack;",
                             "Ljava/util/Stack<Lpac/config/NotifyMsg;>;", null).visitEnd();
            super.visitField(Opcodes.ACC_PUBLIC, "ss_lobjs", "Ljava/util/Set;",
                             "Ljava/util/Set<Ljava/util/concurrent/locks/Lock;>;", null).visitEnd();
            super.visitField(Opcodes.ACC_PUBLIC, "ss_openfiles", "Ljava/util/Set;",
                             "Ljava/util/Set<Ljava/io/Closeable;>;", null).visitEnd();
            super.visitField(Opcodes.ACC_PUBLIC, "ss_join", "Ljava/lang/Thread;", null, null).visitEnd();
        } else if (this.name.equals("java/net/Socket")) {
            super.visitField(Opcodes.ACC_PUBLIC, "ss_server", "Z", null, null);
        } else if (this.name.equals("java/io/FileDescriptor")) {
            super.visitField(Opcodes.ACC_PUBLIC, "ss_file", "Ljava/io/File;", null, null).visitEnd();
            super.visitField(Opcodes.ACC_PUBLIC, "ss_shell", "Z", null, null).visitEnd();
        } else if (this.name.matches("java/io/(InputStream|OutputStream|Writer|Reader)")) {
            super.visitField(Opcodes.ACC_PUBLIC, "ss_taint", "I", null, null).visitEnd();
            super.visitField(Opcodes.ACC_PUBLIC, "ss_hasUniformTaint", "Z", null, null).visitEnd();
            // super.visitField(Opcodes.ACC_PUBLIC, "has_taint", "Z", null,
            // null).visitEnd();
            super.visitField(Opcodes.ACC_PUBLIC, "ss_socktype", "Lpac/inst/taint/SocketInstrumentation$SocketType;",
                             null, null).visitEnd();
        }
    }

    @Override
    protected void instrumentClass() {
        super.instrumentClass();

        // Add all of the modified recursive methods, if any...
        for (MethodNode recursiveMethodNode : recursiveMethods)
            this.methods.add(recursiveMethodNode);
    }

    @Override
    protected void instrumentMethodInline(MethodNode methodNode) {
        // Instrument get/set fields in original method so that both
        // fields properly update in the event a method is called
        // natively...
        AbstractInsnNode currentNode = methodNode.instructions.getFirst();
        while (currentNode != null) {
            if (currentNode instanceof FieldInsnNode) {
                instrumentFields(methodNode, (FieldInsnNode) currentNode);
            } else if (currentNode instanceof LdcInsnNode) {
                LdcInsnNode ldcNode = (LdcInsnNode) currentNode;
                if (ldcNode.cst instanceof String) {
                    methodNode.instructions.insert(currentNode, new MethodInsnNode(Opcodes.INVOKESTATIC,
                            "pac/util/TaintUtils", "trustConstant", "(Ljava/lang/String;)Ljava/lang/String;", false));
                    currentNode = currentNode.getNext();
                }
            }
            currentNode = currentNode.getNext();
        }
    }

    //#if bootstrap==false
    private pac.config.AbstractConfig getConfigInstance() {
        return dynamicMode ? pac.config.BaseConfig.getInstance() : pac.config.ConfigFile.getInstance();
    }
    //#endif

    @Override
    protected MethodNode instrumentMethodCopy(MethodNode methodNode, Frame<BasicValue>[] frames) {
        MethodNode newMethodNode = new CleartrackTaintMethodAdapter(this.name, this.access, frames, methodNode,
                dynamicMode);
        methodNode.accept(newMethodNode);

        // If the method we just instrumented is recursive, than have the instrumented
        // copy simply invoke the recursive method with an additional count parameter.
        // The count parameter will keep track of the relative stack size, and initially
        // will be zero.
        //#if bootstrap==false
        if (!inJdk && getConfigInstance().getMaxStackAction() != pac.config.RunChecks.IGNORE_ACTION
                && hierarchy.isRecursive(this, methodNode)) {
            String[] exceptions = newMethodNode.exceptions == null ? new String[0]
                    : newMethodNode.exceptions.toArray(new String[0]);
            MethodNode recursiveMethodNode = new MethodNode(Opcodes.ASM5, newMethodNode.access, newMethodNode.name,
                    getRecursiveDesc(newMethodNode.desc), getRecursiveDesc(newMethodNode.signature), exceptions);
            newMethodNode.accept(recursiveMethodNode);
            callRecursive(newMethodNode, recursiveMethodNode);
            recursiveMethods.add(recursiveMethodNode);
        }
        //#endif

        if (newMethodNode.name.equals("toString") && newMethodNode.desc.equals("(Lpac/util/Ret;)Ljava/lang/String;")) {
            if (shouldSubclass()) {
                if (subClassNode.interfaces == null)
                    subClassNode.interfaces = new LinkedList<String>();
                subClassNode.interfaces.add("pac/inst/Instrumentable");
            } else {
                if (this.interfaces == null)
                    this.interfaces = new LinkedList<String>();
                this.interfaces.add("pac/inst/Instrumentable");
            }
        }

        return newMethodNode;
    }

    /**
     * This should be invoked on the original (non taint-tracked) method node on
     * a field node instruction contained in this method. If the field is not
     * instrumented (i.e. the class is not instrumented), then we simply return.
     * Otherwise instructions are added prior to fieldNode to ensure that both
     * fields are properly set.
     * 
     * @param methodNode
     *            original method node
     * @param fieldNode
     *            field instruction node obtained from methodNode
     */
    protected void instrumentFields(MethodNode methodNode, FieldInsnNode fieldNode) {
        if (hierarchy.isSkipped(fieldNode)) // skipped fields are not altered,
                                            // so leave these alone...
            return;
        Type fieldType = Type.getType(fieldNode.desc);

        // if the field type is dangerous, then we do nothing extra, since the taint
        // will be handled in the psuedo-instrumented call.  also, we presume that
        // dangerous fields cannot be set outside it's own dangerous class, so do
        // nothing extra in this case as well.
        if (hierarchy.isDangerousClass(fieldType) || hierarchy.isDangerousClass(fieldNode.owner))
            return;

        boolean expectingObject = false;
        switch (fieldNode.getOpcode()) {
        case Opcodes.PUTFIELD:
            expectingObject = true;
        case Opcodes.PUTSTATIC:
            // this field is not dangerous, so the only type of fields left
            // that we need to check are primitive fields, primitive array
            // fields, and fields that could possible be a primitive array
            // (i.e. Object, Object[], Object[][], ...).
            InsnList insnList = new InsnList();
            Type primType = AsmUtils.toPrimitiveType(fieldType);
            if (primType != null) { // the type is either a primitive, or
                                    // primitive array.
                                    // we need to set both the original field, and also convert
                                    // the type on the stack to an unknown taint tracked type,
                                    // and set the shadowed field to this new object (or int
                                    // taint value in the case of primitives).
                switch (primType.getSort()) {
                case Type.INT:
                    if (expectingObject) {
                        // the object whose field we are accessing needs to be
                        // copied on the stack, such that we can also access
                        // the taint field.
                        if (fieldType.getSize() == 2) {
                            insnList.add(new InsnNode(Opcodes.DUP2_X1));
                            insnList.add(new InsnNode(Opcodes.POP2));
                            insnList.add(new InsnNode(Opcodes.DUP_X2));
                        } else {
                            insnList.add(new InsnNode(Opcodes.SWAP));
                            insnList.add(new InsnNode(Opcodes.DUP_X1));
                        }
                    }

                    // add instructions to push taint and set the shadowed
                    // taint field.
                    insnList.add(new InsnNode(UNKNOWN_OPCODE));
                    insnList.add(new FieldInsnNode(fieldNode.getOpcode(), fieldNode.owner,
                            getFieldInstName(fieldNode.name, fieldNode.desc), primType.getDescriptor()));
                    break;
                default:
                    Type arrayTaintType = AsmUtils.toWrappedArrayType(fieldType.getElementType());
                    if (expectingObject) // dup both the object and the
                                         // value being set.
                        insnList.add(new InsnNode(Opcodes.DUP2));
                    else
                        insnList.add(new InsnNode(Opcodes.DUP));

                    // add instructions to push the converted primitive taint array
                    // object onto the stack and set the shadowed taint field.
                    insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, arrayTaintType.getInternalName(),
                            "toTaintArray", "(" + fieldNode.desc + ")" + primType.getDescriptor(), false));
                    insnList.add(new FieldInsnNode(fieldNode.getOpcode(), fieldNode.owner,
                            getFieldInstName(fieldNode.name, fieldNode.desc), primType.getDescriptor()));
                }
            } else if (AsmUtils.canBePrimitiveArrayType(fieldType)) { // Object,
                // Object[],
                // ...
                if (expectingObject) // dup both the object and the value being set.
                    insnList.add(new InsnNode(Opcodes.DUP2));
                else
                    insnList.add(new InsnNode(Opcodes.DUP));

                // add instructions that will either push the original object (if
                // not a primitive array) or the primitive taint wrapped object
                // onto the stack. we then cast the object back to the expected
                // type on the stack and set the shadowed taint field.
                insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "pac/wrap/ArrayTaint", "toTaintArray",
                        "(Ljava/lang/Object;)Ljava/lang/Object;", false));
                if (fieldType.getSort() == Type.ARRAY) // need to cast back
                                                       // to array of
                                                       // objects
                    insnList.add(new TypeInsnNode(Opcodes.CHECKCAST, fieldType.getInternalName()));
                insnList.add(new FieldInsnNode(fieldNode.getOpcode(), fieldNode.owner,
                        getFieldInstName(fieldNode.name, fieldNode.desc), fieldNode.desc));
            }

            methodNode.instructions.insertBefore(fieldNode, insnList);
            break;
        }
    }

    @Override
    public boolean shouldSubclass(String className) {
        return hierarchy.isDangerousClass(className);
    }

    @Override
    public boolean shouldCopyMethods() {
        return true;
    }

    @Override
    public boolean shouldVerify() {
        return true;
    }

    private boolean shouldMakeFieldPublic(FieldNode origFieldNode, boolean isLockClass) {
        if (isLockClass && origFieldNode.name.equals("sync")) {
            // Make the lock synchronizer private, so we can access them later
            // to find more complicated deadlocks.
            Type type = Type.getType(origFieldNode.desc);
            if (type.getSort() == Type.OBJECT && hierarchy
                    .isA(type.getInternalName(), "java/util/concurrent/locks/AbstractOwnableSynchronizer")) {
                return true;
            }
        }
        // Subclassed fields must be accessed through their parent class, and hence need to be at least
        // protected (asside from static fields, since these are copied down).  However, we will make
        // these public so we may access them in the Field wrappers.
        return (shouldSubclass() && !AsmUtils.hasModifiers(origFieldNode.access, Opcodes.ACC_STATIC))
                || (origFieldNode.name.equals("value") && (this.name.equals("java/lang/String")
                        || this.name.equals("java/lang/AbstractStringBuilder")))
                || (this.name.equals("java/net/URL")
                        && origFieldNode.name.matches("protocol|port|file|authority|path|host|ref|userInfo|query"))
                || (origFieldNode.name.equals("fd")
                        && (this.name.equals("java/io/FileInputStream") || this.name.equals("java/io/FileDescriptor")
                                || this.name.equals("sun/nio/ch/FileChannelImpl"))
                        || (origFieldNode.name.equals("in") && this.name.equals("java/io/FilterInputStream"))
                        || (origFieldNode.name.equals("out") && this.name.equals("java/io/FilterOutputStream")))
                || (origFieldNode.name.equals("buf") && (this.name.equals("java/io/BufferedInputStream")
                        || this.name.equals("java/util/zip/Deflater") || this.name.equals("java/util/zip/Inflater")))
                || (origFieldNode.name.equals("mag") && this.name.equals("java/math/BigInteger"))
                || (origFieldNode.name.equals("hb") && this.name.equals("java/nio/ByteBuffer"));
    }

    /**
     * @return a list of all the instrumented fields (i.e. primitive taint,
     *         primitive array taint, java.lang.Object (or array of), and
     *         dangerous fields within a dangerous type).
     */
    @Override
    protected List<FieldNode> getInstrumentedFields() {
        // Acquire a list of all the taint fields...
        List<FieldNode> newFields = new LinkedList<FieldNode>();
        ListIterator<FieldNode> fieldIter = this.fields.listIterator();
        boolean isLock = hierarchy.isA(this.name, "java/util/concurrent/locks/Lock");
        while (fieldIter.hasNext()) {
            FieldNode fieldNode = fieldIter.next();

            // We need to make a select few fields public so that our
            // instrumentation has access to these fields.
            if (shouldMakeFieldPublic(fieldNode, isLock)) {
                fieldNode.access = makePublic(fieldNode.access);
            }

            if (hierarchy.isSkipped(this, fieldNode))
                continue; // the field is skipped, therefore no new type will be
                          // added.

            // If we are sub-classing, then ensure the following:
            //   a) Static fields are copied down to the sub-type, since they are local
            //      to the class and not the object
            //   b) Final fields are made non-final, since the parent field can be
            //      set from the sub-type.  However, since static fields will be copied,
            //      the original field does not need to be made non-final.
            if (shouldSubclass()) {
                if (AsmUtils.hasModifiers(fieldNode.access, Opcodes.ACC_STATIC)) {
                    FieldNode copiedField = new FieldNode(fieldNode.access, fieldNode.name, fieldNode.desc,
                            fieldNode.signature, fieldNode.value);
                    newFields.add(copiedField);
                } else if (AsmUtils.hasModifiers(fieldNode.access, Opcodes.ACC_FINAL)) {
                    fieldNode.access = fieldNode.access - Opcodes.ACC_FINAL;
                }
            }

            Type fieldType = Type.getType(fieldNode.desc);
            Type primFieldType = AsmUtils.toPrimitiveType(fieldType);

            // The access of the original field will be the same as the
            // taint field except the taint field will be made non-final.
            // This is because we may decide to reset the taint to some
            // other value at some point.  Also, we need to ensure that
            // any taint field in an interface is both static and final,
            // since only these fields are allowed in interfaces.
            int acc = fieldNode.access;
            if (AsmUtils.hasModifiers(this.access, Opcodes.ACC_INTERFACE))
                acc |= Opcodes.ACC_STATIC | Opcodes.ACC_FINAL;
            else if (AsmUtils.hasModifiers(fieldNode.access, Opcodes.ACC_FINAL))
                acc = acc - Opcodes.ACC_FINAL;

            FieldNode taintFieldNode;
            if (primFieldType != null) { // primitive or primitive array
                taintFieldNode = new FieldNode(acc, getFieldInstName(fieldNode.name, fieldNode.desc),
                        primFieldType.getDescriptor(), null, null);
            } else if (AsmUtils.canBePrimitiveArrayType(fieldType)) { // Object,
                // Object[],
                // Object[][],
                // etc...
                // In this case, the descriptor of the taint field will
                // be exactly the same as the descriptor of the original
                // field.
                taintFieldNode = new FieldNode(acc, getFieldInstName(fieldNode.name, fieldNode.desc), fieldNode.desc,
                        fieldNode.signature, fieldNode.value);
            } else {
                continue;
            }

            newFields.add(taintFieldNode);
        }
        return newFields;
    }

    /**
     * This method should only be called if methodNode represents a main()
     * method. Instructions are added to this method to initialize cleartrack
     * with the application and also trust/taint any command line arguments
     * (based on the configuration file settings).
     * 
     * @param methodNode
     *            MethodNode
     */
    @Override
    protected void setupMainApplication(MethodNode methodNode) {
        InsnList insnList = new InsnList();
        LabelNode startLabel = new LabelNode();
        LabelNode insideLoopLabel = new LabelNode();
        LabelNode endLabel = new LabelNode();
        LabelNode compareLabel = new LabelNode();

        // add instructions to iterate over all String arguments and taint/trust
        // depending on config file settings.
        insnList.add(startLabel);
        insnList.add(new InsnNode(Opcodes.ICONST_0));
        insnList.add(new VarInsnNode(Opcodes.ISTORE, 1));
        insnList.add(new LabelNode());
        insnList.add(new JumpInsnNode(Opcodes.GOTO, compareLabel));
        insnList.add(insideLoopLabel);
        insnList.add(new FrameNode(Opcodes.F_APPEND, 1, new Object[] { 1 }, 0, null));
        insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
        insnList.add(new VarInsnNode(Opcodes.ILOAD, 1));
        insnList.add(new InsnNode(Opcodes.AALOAD));
        insnList.add(new InsnNode(Opcodes.DUP));
        insnList.add(new FieldInsnNode(Opcodes.GETFIELD, "java/lang/String", "value", "[C"));

        // push the correct taint on the path depending on whether arguments
        // should be tainted...
        insnList.add(AsmUtils.numToInstruction(TaintValues.PROGRAM_ARG));
        LabelNode trusted = new LabelNode();
        LabelNode tainted = new LabelNode();
        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "pac/config/BaseConfig", "getInstance",
                "()Lpac/config/BaseConfig;", false));
        insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "pac/config/BaseConfig", "taintCommandArgs", "()Z",
                false));
        insnList.add(new JumpInsnNode(Opcodes.IFEQ, trusted));
        insnList.add(new InsnNode(TAINTED_OPCODE));
        insnList.add(new JumpInsnNode(Opcodes.GOTO, tainted));
        insnList.add(trusted);
        insnList.add(new InsnNode(TRUSTED_OPCODE));
        insnList.add(tainted);
        insnList.add(new InsnNode(Opcodes.IOR));

        // record the taint the String.value's taint field...
        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "pac/wrap/CharArrayTaint", "toTaintArray",
                "([CI)Lpac/wrap/CharArrayTaint;", false));
        insnList.add(new FieldInsnNode(Opcodes.PUTFIELD, "java/lang/String",
                getFieldInstName("value", "Lpac/wrap/CharArrayTaint;"), "Lpac/wrap/CharArrayTaint;"));

        insnList.add(new IincInsnNode(1, 1));
        insnList.add(compareLabel);
        insnList.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        insnList.add(new VarInsnNode(Opcodes.ILOAD, 1));
        insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
        insnList.add(new InsnNode(Opcodes.ARRAYLENGTH));
        insnList.add(new JumpInsnNode(Opcodes.IF_ICMPLT, insideLoopLabel));
        insnList.add(endLabel);
        methodNode.instructions.insert(insnList);
        methodNode.localVariables.add(new LocalVariableNode("i", "I", null, startLabel, endLabel, 1));
        methodNode.maxLocals++;
        methodNode.maxStack = Math.max(methodNode.maxStack, 2);

        super.setupMainApplication(methodNode);
    }

    @Override
    public Map<String, Object> getInstrumentationOptions() {
        Map<String, Object> optionMap = super.getInstrumentationOptions();
        optionMap.put("CONFINEMENT", true);
        return optionMap;
    }

    /**
     * Returns the modified descriptor of the new recursive methods that
     * will be added.  The new descriptor will merely have an integer added
     * as the last parameter.  This will represent the relative stack count.
     * 
     * @param desc String desc of the recursive method
     * @return String the new desc
     */
    private String getRecursiveDesc(String desc) {
        if (desc == null)
            return null;
        int idx = desc.indexOf(")");
        if (idx < 0)
            return desc;
        return desc.substring(0, idx) + "I" + desc.substring(idx);
    }

    /**
     * Replaces the instructions in the instrumented method instMethodNode with a call
     * to recursiveMethodNode (with count intialized to zero).  This should only be
     * called on recursive methods.  The instructions in recursiveMethodNode will be
     * modified to take the extra count parameter, and increment this value in each
     * subsequent recursive call.  A check is also inserted at the beginning of this
     * method that checks whether the count has gone beyond the limit imposed by the
     * config file.  If so, some action will be performed (as set by the config file).
     * 
     * @param instMethodNode MethodNode of the instrumented method copy
     * @param recursiveMethodNode MethodNode of the new recursive method copy
     */
    protected void callRecursive(MethodNode instMethodNode, MethodNode recursiveMethodNode) {
        if (instMethodNode.localVariables != null)
            instMethodNode.localVariables.clear();
        if (instMethodNode.tryCatchBlocks != null)
            instMethodNode.tryCatchBlocks.clear();
        if (instMethodNode.instructions != null)
            instMethodNode.instructions.clear();
        else
            instMethodNode.instructions = new InsnList();

        int reg = 0;
        boolean isStatic = AsmUtils.hasModifiers(instMethodNode.access, Opcodes.ACC_STATIC);
        if (!isStatic)
            instMethodNode.instructions.add(new VarInsnNode(Opcodes.ALOAD, reg++));

        Type[] argTypes = Type.getArgumentTypes(instMethodNode.desc);
        for (int i = 0; i < argTypes.length; i++) {
            switch (argTypes[i].getSort()) {
            case Type.ARRAY:
            case Type.OBJECT:
                instMethodNode.instructions.add(new VarInsnNode(Opcodes.ALOAD, reg++));
                break;
            case Type.FLOAT:
                instMethodNode.instructions.add(new VarInsnNode(Opcodes.FLOAD, reg++));
                break;
            case Type.LONG:
                instMethodNode.instructions.add(new VarInsnNode(Opcodes.LLOAD, reg++));
                reg++;
                break;
            case Type.DOUBLE:
                instMethodNode.instructions.add(new VarInsnNode(Opcodes.DLOAD, reg++));
                reg++;
                break;
            default:
                instMethodNode.instructions.add(new VarInsnNode(Opcodes.ILOAD, reg++));
                break;
            }
        }
        instMethodNode.instructions.add(new InsnNode(Opcodes.ICONST_0)); // initial recursive count

        instMethodNode.instructions.add(new MethodInsnNode(isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL,
                this.name, recursiveMethodNode.name, recursiveMethodNode.desc, false));
        // We need to reset breakFromRecursion flag for recursive
        // methods only on the initial call (not the subsequent
        // recursive calls).
        instMethodNode.instructions.add(new VarInsnNode(Opcodes.ALOAD, reg - 1));
        instMethodNode.instructions.add(new InsnNode(Opcodes.ICONST_0));
        instMethodNode.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, "pac/util/Ret", "breakFromRecursion", "Z"));

        int returnOpcode;
        Type returnType = Type.getReturnType(instMethodNode.desc);
        switch (returnType.getSort()) {
        case Type.ARRAY:
        case Type.OBJECT:
            returnOpcode = Opcodes.ARETURN;
            break;
        case Type.VOID:
            returnOpcode = Opcodes.RETURN;
            break;
        case Type.FLOAT:
            returnOpcode = Opcodes.FRETURN;
            break;
        case Type.LONG:
            returnOpcode = Opcodes.LRETURN;
            break;
        case Type.DOUBLE:
            returnOpcode = Opcodes.DRETURN;
            break;
        default:
            returnOpcode = Opcodes.IRETURN;
            break;
        }
        instMethodNode.instructions.add(new InsnNode(returnOpcode));

        // Find all of the recursive calls and insert instructions to track
        // the stack count, along with checks after the recursive call to
        // determine whether we should be breaking from this recurisve method.
        for (AbstractInsnNode curNode = recursiveMethodNode.instructions.getFirst(); curNode != null; curNode = curNode
                .getNext()) {
            if (curNode instanceof MethodInsnNode) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) curNode;
                int opcode = methodInsnNode.getOpcode();
                // Check to see if this call is recursive...
                if ((opcode == Opcodes.INVOKESTATIC || opcode == Opcodes.INVOKEVIRTUAL)
                        && this.name.equals(methodInsnNode.owner) && instMethodNode.name.equals(methodInsnNode.name)
                        && instMethodNode.desc.equals(methodInsnNode.desc)) {
                    // We are in a recursive call, so update the signature and
                    // add the incremented count parameter.
                    methodInsnNode.desc = recursiveMethodNode.desc;
                    InsnList insnList = new InsnList();
                    insnList.add(new VarInsnNode(Opcodes.ILOAD, reg)); // reg refers to count
                    insnList.add(new InsnNode(Opcodes.ICONST_1));
                    insnList.add(new InsnNode(Opcodes.IADD));
                    recursiveMethodNode.instructions.insertBefore(curNode, insnList);

                    // Add instructions to break from the recursive call if we
                    // exceeded the stack bounds...
                    LabelNode skipLabel = new LabelNode();
                    insnList = new InsnList();
                    insnList.add(new VarInsnNode(Opcodes.ALOAD, reg - 1));
                    insnList.add(new FieldInsnNode(Opcodes.GETFIELD, "pac/util/Ret", "breakFromRecursion", "Z"));
                    insnList.add(new JumpInsnNode(Opcodes.IFEQ, skipLabel));
                    insnList.add(new InsnNode(returnOpcode));
                    insnList.add(skipLabel);
                    recursiveMethodNode.instructions.insert(curNode, insnList);
                }
            } else if (curNode instanceof VarInsnNode) {
                VarInsnNode varInsnNode = (VarInsnNode) curNode;
                if (varInsnNode.var >= reg)
                    varInsnNode.var++;
            } else if (curNode instanceof IincInsnNode) {
                IincInsnNode iincInsnNode = (IincInsnNode) curNode;
                if (iincInsnNode.var >= reg)
                    iincInsnNode.var++;
            }
        }

        // Add the recursive check to the beginning of the method...
        LabelNode skipLabel = new LabelNode();
        InsnList insnList = new InsnList();
        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "pac/config/BaseConfig", "getInstance",
                "()Lpac/config/BaseConfig;", false));
        insnList.add(new VarInsnNode(Opcodes.ILOAD, reg));
        insnList.add(new LdcInsnNode(this.name + "." + instMethodNode.name + instMethodNode.desc));
        insnList.add(new VarInsnNode(Opcodes.ALOAD, reg - 1));
        insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "pac/config/BaseConfig", "handleRecursiveCheck",
                "(ILjava/lang/String;Lpac/util/Ret;)Z", false));
        insnList.add(new JumpInsnNode(Opcodes.IFEQ, skipLabel));
        if (returnOpcode != Opcodes.RETURN) {
            if (returnOpcode == Opcodes.ARETURN) {
                insnList.add(new InsnNode(Opcodes.ACONST_NULL));
            } else {
                // Trust the return value since we are setting it.
                insnList.add(new VarInsnNode(Opcodes.ALOAD, reg - 1));
                insnList.add(AsmUtils.numToInstruction(TaintValues.TRUSTED));
                insnList.add(new FieldInsnNode(Opcodes.PUTFIELD, "pac/util/Ret", "taint", "I"));
                switch (returnOpcode) {
                case Opcodes.FRETURN:
                    insnList.add(new InsnNode(Opcodes.FCONST_0));
                    break;
                case Opcodes.LRETURN:
                    insnList.add(new InsnNode(Opcodes.LCONST_0));
                    break;
                case Opcodes.DRETURN:
                    insnList.add(new InsnNode(Opcodes.DCONST_0));
                    break;
                default:
                    insnList.add(new InsnNode(Opcodes.ICONST_0));
                    break;
                }
            }
        }
        insnList.add(new InsnNode(returnOpcode));
        insnList.add(skipLabel);
        recursiveMethodNode.instructions.insert(insnList);

        Ansi.debug("added stack size check for recursive method %s%s", this.name, instMethodNode.name,
                   instMethodNode.desc);
    }

    /**
     * Replaces all instructions in methodNode, with instructions that will
     * essentially invoke instMethodNode with unknown taint values on arguments.
     * 
     * @param methodNode
     *            original method
     * @param instMethodNode
     *            method with taint propagation
     */
    @Override
    protected void callInstrumented(MethodNode methodNode, MethodNode instMethodNode) {
        if (methodNode.localVariables != null)
            methodNode.localVariables.clear();
        if (methodNode.tryCatchBlocks != null)
            methodNode.tryCatchBlocks.clear();
        if (methodNode.instructions != null)
            methodNode.instructions.clear();
        else
            methodNode.instructions = new InsnList();

        LabelNode startTry = new LabelNode();
        methodNode.instructions.add(startTry);

        if (isMainMethod(methodNode)) {
            if (alreadyHasMainSetup)
                removeMainSetup(instMethodNode);
            // Setup the application in the main() entry by trusting or
            // tainting the arguments, input streams, properties, etc...
            setupMainApplication(methodNode);
        }

        int reg = 0;
        boolean isStatic = AsmUtils.hasModifiers(methodNode.access, Opcodes.ACC_STATIC);
        if (!isStatic)
            methodNode.instructions.add(new VarInsnNode(Opcodes.ALOAD, reg++));

        Type[] argTypes = Type.getArgumentTypes(methodNode.desc);
        for (int i = 0; i < argTypes.length; i++) {
            Type primType = AsmUtils.toPrimitiveType(argTypes[i]);
            if (primType == null) {
                methodNode.instructions.add(new VarInsnNode(Opcodes.ALOAD, reg++));
                if (AsmUtils.canBePrimitiveArrayType(argTypes[i])) {
                    methodNode.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "pac/wrap/ArrayTaint",
                            "toTaintArray", "(Ljava/lang/Object;)Ljava/lang/Object;", false));
                    if (argTypes[i].getSort() == Type.ARRAY)
                        methodNode.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, argTypes[i].getInternalName()));
                }
            } else {
                switch (primType.getSort()) {
                case Type.INT:
                    methodNode.instructions
                            .add(new VarInsnNode(CleartrackTaintMethodAdapter.getLoadOpcode(argTypes[i]), reg++));
                    if (argTypes[i].getSize() == 2)
                        reg++;
                    methodNode.instructions.add(new InsnNode(UNKNOWN_OPCODE));
                    break;
                default:
                    methodNode.instructions.add(new VarInsnNode(Opcodes.ALOAD, reg++));
                    Type arrayTaintType = AsmUtils.toWrappedArrayType(argTypes[i].getElementType());
                    methodNode.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                            arrayTaintType.getInternalName(), "toTaintArray",
                            "(" + argTypes[i].getDescriptor() + ")" + primType.getDescriptor(), false));
                }
            }
        }

        // create the Ret object we pass to the instrumented method
        methodNode.instructions.add(new TypeInsnNode(Opcodes.NEW, "pac/util/Ret"));
        methodNode.instructions.add(new InsnNode(Opcodes.DUP));
        methodNode.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "pac/util/Ret", "<init>", "()V", false));

        // invoke the instrumented method
        methodNode.instructions.add(new MethodInsnNode(
                methodNode.name.equals("<init>") ? Opcodes.INVOKESPECIAL
                        : (isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL),
                this.name, instMethodNode.name, instMethodNode.desc, false));

        // acquire the value from the instrumented return type, to be returned
        // from this uninstrumented method.
        Type returnType = Type.getReturnType(methodNode.desc);
        Type primReturnType = AsmUtils.toPrimitiveArrayType(returnType);
        if (primReturnType != null) {
            Type arrayTaintType = AsmUtils.toWrappedArrayType(returnType.getElementType());
            methodNode.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, arrayTaintType.getInternalName(),
                    "toValueArray", "(" + primReturnType.getDescriptor() + ")" + returnType.getDescriptor(), false));
        } else if (AsmUtils.canBePrimitiveArrayType(returnType)) {
            methodNode.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "pac/wrap/ArrayTaint", "toValueArray",
                    "(Ljava/lang/Object;)Ljava/lang/Object;", false));
            if (returnType.getSort() == Type.ARRAY)
                methodNode.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, returnType.getInternalName()));
        }

        // Insert a finally block around all code, in the case of Thread.run()
        // or some overridden method of this. This will properly unlock any
        // lock that had not been previously unlocked in this Thread.
        if (!inJdk && methodNode.name.equals("run") && methodNode.desc.equals("()V")
                && hierarchy.isA(this.name, "java/lang/Runnable")) {
            LabelNode endTry = new LabelNode();
            LabelNode skipFinally = new LabelNode();

            methodNode.instructions.add(new JumpInsnNode(Opcodes.GOTO, skipFinally));
            methodNode.instructions.add(endTry);
            methodNode.instructions.add(new VarInsnNode(Opcodes.ASTORE, 1));
            methodNode.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "pac/config/BaseConfig", "getInstance",
                    "()Lpac/config/BaseConfig;", false));
            methodNode.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "pac/config/BaseConfig",
                    "handleThreadLocks", "()V", false));
            methodNode.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            methodNode.instructions.add(new InsnNode(Opcodes.ATHROW));
            methodNode.instructions.add(skipFinally);
            methodNode.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "pac/config/BaseConfig", "getInstance",
                    "()Lpac/config/BaseConfig;", false));
            methodNode.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "pac/config/BaseConfig",
                    "handleThreadLocks", "()V", false));

            methodNode.tryCatchBlocks.add(new TryCatchBlockNode(startTry, endTry, endTry, null));
        }

        // add the appropriate return instruction depending on the return type
        // of the method.
        switch (returnType.getSort()) {
        case Type.ARRAY:
        case Type.OBJECT:
            methodNode.instructions.add(new InsnNode(Opcodes.ARETURN));
            break;
        case Type.VOID:
            methodNode.instructions.add(new InsnNode(Opcodes.RETURN));
            break;
        case Type.FLOAT:
            methodNode.instructions.add(new InsnNode(Opcodes.FRETURN));
            break;
        case Type.LONG:
            methodNode.instructions.add(new InsnNode(Opcodes.LRETURN));
            break;
        case Type.DOUBLE:
            methodNode.instructions.add(new InsnNode(Opcodes.DRETURN));
            break;
        default:
            methodNode.instructions.add(new InsnNode(Opcodes.IRETURN));
            break;
        }
    }

    @Override
    protected MethodNode callUninstrumented(MethodType methodType) {
        MethodNode methodNode = new MethodNode(Opcodes.ASM5, methodType.access, methodType.name,
                CleartrackTaintMethodAdapter.toPrimitiveDesc(methodType.desc),
                CleartrackSignatureWriter.instrumentSignature(methodType.signature),
                (String[]) methodType.exceptions.toArray(new String[0]));
        CleartrackTaintMethodAdapter.callUninstrumented(this.name, methodNode, methodType.desc);
        return methodNode;
    }
}
