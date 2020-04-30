package pac.agent;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import pac.agent.gui.ClassEditor;
import pac.agent.hierarchy.ClassHierarchy;
import pac.agent.hierarchy.MethodType;
import pac.org.objectweb.asm.ClassVisitor;
import pac.org.objectweb.asm.MethodVisitor;
import pac.org.objectweb.asm.Opcodes;
import pac.org.objectweb.asm.Type;
import pac.org.objectweb.asm.tree.AbstractInsnNode;
import pac.org.objectweb.asm.tree.AnnotationNode;
import pac.org.objectweb.asm.tree.ClassNode;
import pac.org.objectweb.asm.tree.FieldNode;
import pac.org.objectweb.asm.tree.InsnList;
import pac.org.objectweb.asm.tree.InsnNode;
import pac.org.objectweb.asm.tree.LdcInsnNode;
import pac.org.objectweb.asm.tree.MethodInsnNode;
import pac.org.objectweb.asm.tree.MethodNode;
import pac.org.objectweb.asm.tree.TypeInsnNode;
import pac.org.objectweb.asm.tree.VarInsnNode;
import pac.org.objectweb.asm.tree.analysis.Analyzer;
import pac.org.objectweb.asm.tree.analysis.AnalyzerException;
import pac.org.objectweb.asm.tree.analysis.BasicValue;
import pac.org.objectweb.asm.tree.analysis.Frame;
import pac.util.AsmUtils;

/**
 * This provides the basic framework for instrumenting an application, with no
 * actual transformations in place.
 * 
 * @author jeikenberry
 */
public class CleartrackBaseInstrumentationAdapter extends ClassNode {

    public static final ClassHierarchy hierarchy = ClassHierarchy.getInstance();

    /** The next ClassVisitor in the class transformation pipeline. */
    protected ClassVisitor cv;

    /**
     * Instrumented sub-class of the underlying ClassNode, or null if this class
     * should not be sub-classed.
     */
    protected ClassNode subClassNode;

    /**
     * The analyzer for this class, or null if we are not verifying.
     */
    private Analyzer<BasicValue> analyzer;

    /** Set to true if this ClassNode is a JDK class, otherwise false. */
    protected boolean inJdk;

    /**
     * Set to true if this ClassNode has a default constructor (i.e.
     * constructor with no arguments).
     */
    private boolean hasDefaultConstructor;

    private boolean editMode;

    /**
     * Set to true if the bytecode we are instrumenting has already been
     * instrumented and therefore already has the necessary calls to setup
     * the instrumentation inside of main().
     */
    protected boolean alreadyHasMainSetup;

    /**
     * Mapping of all instrumentation arguments to their argument values.
     */
    private Map<String, Object> optionMap;

    /**
     * Constructor for creating a CleartrackBaseInstrumentationAdatper.
     * 
     * @param cv ClassVisitor of the next in the chain of ClassVisitors
     * @param analysisOptions EnumSet&lt;StaticOption&gt; of the analysis 
     *  options to employ.
     */
    public CleartrackBaseInstrumentationAdapter(ClassVisitor cv, boolean editMode) {
        super(Opcodes.ASM5);
        this.cv = cv;
        this.editMode = editMode;
        this.alreadyHasMainSetup = false;
        this.optionMap = new TreeMap<String, Object>();
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        inJdk = hierarchy.isJdkClass(name);

        // if this class has a dangerous subtype, then create it now so
        // we can add methods and field to it after this class has been
        // processed.
        if (shouldSubclass(name)) {
            subClassNode = new ClassNode(Opcodes.ASM5);
            subClassNode.name = ClassHierarchy.getDangerousSublassName(name);
            subClassNode.superName = name;
            subClassNode.access = access;
            if (AsmUtils.hasModifiers(access, Opcodes.ACC_FINAL))
                access = access - Opcodes.ACC_FINAL;
            subClassNode.version = Opcodes.V1_7;
        }

        super.visit(version, access, name, signature, superName, interfaces);

        if (shouldVerify()) {
            // setup cleartrack verifier and analyzer that is responsible for
            // constructing the stack frames for each method in this class.
            List<Type> interfaceList = null;
            if (this.interfaces != null) {
                interfaceList = new LinkedList<Type>();
                for (String interfaceName : this.interfaces) {
                    interfaceList.add(Type.getObjectType(interfaceName));
                }
            }
            analyzer = new Analyzer<BasicValue>(new CleartrackVerifier(Type.getObjectType(this.name),
                    this.superName == null ? null : Type.getObjectType(this.superName), interfaceList,
                    AsmUtils.hasModifiers(this.access, Opcodes.ACC_INTERFACE)));
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (name.equals("<init>") && desc.equals("()V"))
            hasDefaultConstructor = true;
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    public final void visitEnd() {
        // Do not reinstrument if the class has already been instrumented with
        // any of the instrumentation options...
        if (this.invisibleAnnotations != null) {
            for (AnnotationNode annotation : this.invisibleAnnotations) {
                if (annotation.desc.equals("Lpac/inst/InstrumentedClass;")) {
                    alreadyHasMainSetup = true;
                    Map<String, Object> newOptions = getInstrumentationOptions();
                    @SuppressWarnings("unchecked")
                    List<String> options = (List<String>) annotation.values.get(3);
                    for (String option : options) {
                        if (newOptions.containsKey(option)) {
                            accept(cv);
                            return;
                        }
                    }
                }
            }
        }

        instrumentClass();

        accept(cv);
    }

    /**
     * All of the transformations for the instrumentation should happen in this
     * call. The default implementation of this method will instrument the
     * main() methods by inserting the instructions to invoke Notify.setup().
     * Override this class to perform more specialized instrumentation.
     */
    protected void instrumentClass() {
        // fields must be added at the end because they fields are indexed
        // natively through a hardcoded offset.
        for (FieldNode fieldNode : getInstrumentedFields()) {
            if (shouldSubclass())
                subClassNode.fields.add(fieldNode);
            else
                this.fields.add(fieldNode);
        }

        // add all primitive taint methods
        ListIterator<MethodNode> methodIter = this.methods.listIterator();
        while (methodIter.hasNext()) {
            MethodNode methodNode = methodIter.next();
            instrumentMethod(methodIter, methodNode);
        }

        if (shouldCopyMethods() && !AsmUtils.hasModifiers(this.access, Opcodes.ACC_INTERFACE)) {
            // We also need to add an instrumented method that invokes the
            // original method for any method not defined in this class but
            // has an instrumented abstract method in a parent class or
            // interface.
            for (MethodType method : hierarchy.getAllInheritedMethodsNotDefined(this)) {
                // Create a new method node with our instrumentation...
                MethodNode newMethodNode = callUninstrumented(method);
                if (shouldSubclass())
                    subClassNode.methods.add(newMethodNode);
                else
                    methodIter.add(newMethodNode);
            }
        }

        if (shouldSubclass()) {
            instrumentSubclass(subClassNode);
        }

        addExtraFields();
    }

    /**
     * Called only if shouldSubclass() returns true for this ClassNode. This
     * should be implemented if your subclass should provide an additional
     * instrumentation.  By default, this call will add a default constructor
     * calling the super-type of subClassNode
     * 
     * @param subClassNode ClassNode to sub-class
     */
    protected void instrumentSubclass(ClassNode subClassNode) {
        // we need to add a default constructor if the dangerous type
        // does not already have one. this will simply just call the
        // super <init> constructor.
        if (!hasDefaultConstructor) {
            MethodNode defConst = new MethodNode(Opcodes.ASM5, Opcodes.ACC_PUBLIC, "<init>", "()V", null,
                    new String[0]);
            defConst.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            defConst.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                    hierarchy.getNearestDefaultConstructorClass(this), "<init>", "()V", false));
            defConst.instructions.add(new InsnNode(Opcodes.RETURN));
            subClassNode.methods.add(defConst);
        }
    }

    /**
     * Override this method to provide instrumentation for each uninstrumented
     * MethodNode.
     * 
     * @param methodNode MethodNode
     */
    protected void instrumentMethod(ListIterator<MethodNode> methodIter, MethodNode methodNode) {
        if (!shouldCopyMethods()) {
            if (hierarchy.isSkipped(this, methodNode))
                return;
            if (isMainMethod(methodNode)) {
                if (alreadyHasMainSetup)
                    removeMainSetup(methodNode);
                // Setup the application in the main() entry by trusting or
                // tainting the arguments, input streams, properties, etc...
                setupMainApplication(methodNode);
            }
            instrumentMethodInline(methodNode);
        } else {
            boolean isSkippedMethod = hierarchy.isSkipped(this, methodNode);
            if (isSkippedMethod) {
                // first trust the constants, and set taint fields to unknown on
                // putfield/putstatic instructions, that way they will exist in
                // the "skipped" method as well.
                if (!hierarchy.isMethodTooLarge(this, methodNode)) {
                    // we can only add code to a method that is not too large
                    instrumentMethodInline(methodNode);
                }
                if (!AsmUtils.hasModifiers(this.access, Opcodes.ACC_INTERFACE) && !methodNode.name.equals("<init>")
                        && !methodNode.name.equals("<clinit>")
                        && hierarchy.overridesInstrumentedMethod(this, methodNode)) {
                    // the class is skipped but needs to have an instrumented
                    // method that simply performs the original operations.
                    CleartrackSkippedMethodAdapter skippedMethodNode = new CleartrackSkippedMethodAdapter(this.name,
                            methodNode);
                    methodNode.accept(skippedMethodNode);
                    methodIter.add(skippedMethodNode);
                }
            } else {
                while (true) {
                    try {
                        // acquire the stack frames of the original method.
                        Frame<BasicValue>[] frames = null;
                        if (shouldVerify()) {
                            analyzer.analyze(this.name, methodNode);
                            frames = analyzer.getFrames();
                        }

                        // create a new method node with our instrumentation...
                        MethodNode newMethodNode = instrumentMethodCopy(methodNode, frames);

                        if (shouldSubclass()) {
                            // first trust the constants, and set taint fields to unknown on
                            // putfield/putstatic instructions, that way they will exist in
                            // the "skipped" method as well.
                            instrumentMethodInline(methodNode);

                            if (!methodNode.name.equals("<init>") && !methodNode.name.equals("<clinit>")) {
                                // the "skipped" method is needed for compatibility reasons, and since both
                                // parent and subclass define the instrumented methods we can simply refer to
                                // the parent type everywhere.  polymorphically, the correct method will be
                                // invoked at runtime, and no conversion is needed when going from parent
                                // type to subtype, for instance.
                                CleartrackSkippedMethodAdapter skippedMethodNode = new CleartrackSkippedMethodAdapter(
                                        this.name, methodNode);
                                methodNode.accept(skippedMethodNode);
                                if (AsmUtils.hasModifiers(skippedMethodNode.access, Opcodes.ACC_FINAL)) {
                                    skippedMethodNode.access = skippedMethodNode.access - Opcodes.ACC_FINAL;
                                }
                                if (AsmUtils.hasModifiers(skippedMethodNode.access, Opcodes.ACC_PRIVATE)) {
                                    skippedMethodNode.access = skippedMethodNode.access - Opcodes.ACC_PRIVATE;
                                    skippedMethodNode.access = skippedMethodNode.access | Opcodes.ACC_PROTECTED;
                                }
                                methodIter.add(skippedMethodNode);
                            }
                            subClassNode.methods.add(newMethodNode);
                        } else if (methodNode.name.equals("<clinit>")) {
                            methodIter.set(newMethodNode);
                        } else {
                            methodIter.add(newMethodNode);

                            // have the original method call the tainted method
                            // (only in the application)
                            if ((!inJdk || (!isSkippedMethod && isRunMethod(methodNode)))
                                    && !AsmUtils.hasModifiers(methodNode.access, Opcodes.ACC_NATIVE)
                                    && !AsmUtils.hasModifiers(methodNode.access, Opcodes.ACC_ABSTRACT)
                                    && !AsmUtils.hasModifiers(this.access, Opcodes.ACC_INTERFACE)) {
                                callInstrumented(methodNode, newMethodNode);
                            } else {
                                instrumentMethodInline(methodNode);
                            }
                        }
                    } catch (AnalyzerException e) {
                        System.err.println("error analyzing " + this.name + "." + methodNode.name + methodNode.desc
                                + ": " + e);
                        e.printStackTrace();
                        if (editMode) {
                            ClassEditor editor = new ClassEditor(this);
                            editor.setMethod(methodNode);
                            editor.setVisible(true);
                            editor.shouldSave(); // block on gui-exit
                            continue; // continue to re-verify
                        } else {
                            // TODO for the actual T&E we may want to proceed with
                            // partial stack frame information
                            System.exit(1);
                        }
                    }
                    break;
                }
            }
        }
    }

    /**
     * Override this method to define how to create an instrumented copy of
     * methodNode with the specified stack frames of that MethodNode (frames will
     * be null if shouldVerify() is false.
     * 
     * @param methodNode
     * @param frames
     * @return
     */
    protected MethodNode instrumentMethodCopy(MethodNode methodNode, Frame<BasicValue>[] frames) {
        return methodNode;
    }

    /**
     * Override this method if the original method node needs to be altered.  If
     * shouldCopyMethods() returns false, then the main instrumentation should go
     * here.
     */
    protected void instrumentMethodInline(MethodNode methodNode) {

    }

    /**
     * Override this method to define how methods will be copied. By default,
     * methods are not copied.
     * 
     * @param methodNode
     * @param instMethodNode
     */
    protected void callInstrumented(MethodNode methodNode, MethodNode instMethodNode) {

    }

    /**
     * Override this method to define how an instrumented method will call
     * an unisntrumented one (in the event that an instrumented method
     * overrides an uninstrumented method).
     * 
     * @param methodType MethodType of the uninstrumented call
     * @return MethodNode of the newly instrumented method
     */
    protected MethodNode callUninstrumented(MethodType methodType) {
        return null;
    }

    /**
     * Override me if the instrumentation should define any extra fields.
     */
    protected void addExtraFields() {

    }

    /**
     * Fields are not instrumented by default. Override this method if you need
     * to instrument fields.
     * 
     * @return List&lt;FieldNode&gt;
     */
    protected List<FieldNode> getInstrumentedFields() {
        return new LinkedList<FieldNode>();
    }

    /**
     * @return the ClassNode object of the subclassed dangerous type, if this
     *         ClassNode has a subclass (i.e. is dangerous). Otherwise, this
     *         will return null.
     */
    public ClassNode getInstrumentedSubclass() {
        return subClassNode;
    }

    /**
     * Acquire a mapping of all instrumented options (key/value pairs).  Any
     * option that does not have an argument will be mapped as the empty
     * String.
     * 
     * @return Map&lt;String, String&gt;
     */
    public Map<String, Object> getInstrumentationOptions() {
        return optionMap;
    }

    /**
     * Methods are not copied by default. Override this method if you would like
     * to create copies for instrumented methods.
     * 
     * @return boolean
     */
    public boolean shouldCopyMethods() {
        return false;
    }

    /**
     * @return boolean return true if the underlying ClassNode should be
     *         sub-classed, o.w. false.
     */
    public boolean shouldSubclass(String className) {
        return false;
    }

    /**
     * @return boolean return true if the underlying ClassNode should be
     *         sub-classed, o.w. false.
     */
    public boolean shouldSubclass() {
        return subClassNode != null;
    }

    /**
     * @return boolean return true iff this ClassNode should be verified.
     */
    public boolean shouldVerify() {
        return false;
    }

    /**
     * @param methodNode MethodNode
     * @return true iff methodNode represents a main() method.
     */
    public static boolean isMainMethod(MethodNode methodNode) {
        return methodNode.name.equals("main") && methodNode.desc.equals("([Ljava/lang/String;)V")
                && AsmUtils.hasModifiers(methodNode.access, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
    }

    public static boolean isMainMethod(Method method) {
        return method.getName().equals("main") && Type.getMethodDescriptor(method).equals("([Ljava/lang/String;)V")
                && (method.getModifiers() & Modifier.STATIC) == Modifier.STATIC;
    }

    /**
     * @param methodNode MethodNode
     * @return true iff methodNode represents a Thread.run() method.
     */
    private boolean isRunMethod(MethodNode methodNode) {
        if (!hierarchy.isA(this.name, "java/lang/Runnable") && !hierarchy.isA(this.name, "java/lang/Thread")
                && !hierarchy.isA(this.name, "java/security/PrivilegedAction"))
            return false;
        return methodNode.name.equals("run")
                && (methodNode.desc.equals("()V") || methodNode.desc.equals("()Ljava/lang/Object;"));
    }

    /**
     * Called to remove any main() setup that was added to classes
     * that have already been instrumented.  This method is required
     * to allow multiple passes of instrumentation.
     * 
     * @param methodNode MethodNode
     */
    protected void removeMainSetup(MethodNode methodNode) {
        ListIterator<AbstractInsnNode> iter = methodNode.instructions.iterator();
        while (iter.hasNext()) {
            AbstractInsnNode next = iter.next();
            iter.remove(); // remove everything up till the Notify.setup() invocation.
            if (next instanceof MethodInsnNode) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) next;
                if (methodInsnNode.owner.equals("pac/config/Notify") && methodInsnNode.name.equals("setup")
                        && methodInsnNode.desc.equals("(Ljava/util/Set;)V")) {
                    break;
                }
            } else if (next instanceof LdcInsnNode) {
                // readd the exist options back to the option map
                LdcInsnNode ldcInsnNode = (LdcInsnNode) next;
                if (ldcInsnNode.cst instanceof String) {
                    String[] optionPair = ((String) ldcInsnNode.cst).split("=");
                    if (optionPair.length == 1) {
                        optionMap.put(optionPair[0], "");
                    } else {
                        optionMap.put(optionPair[0], optionPair[1]);
                    }
                }
            }
        }
    }

    /**
     * Add instrumentation setup for the specified main() method.
     * 
     * @param methodNode MethodNode
     */
    protected void setupMainApplication(MethodNode methodNode) {
        // construct a list of all the flags that were used when statically
        // instrumenting the application.
        InsnList insnList = new InsnList();
        insnList.add(new TypeInsnNode(Opcodes.NEW, "java/util/TreeMap"));
        insnList.add(new InsnNode(Opcodes.DUP));
        insnList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/TreeMap", "<init>", "()V", false));
        for (Entry<String, Object> entry : getInstrumentationOptions().entrySet()) {
            insnList.add(new InsnNode(Opcodes.DUP));
            insnList.add(new LdcInsnNode(entry.getKey()));
            addInstrumentationOption(insnList, entry.getValue());
            insnList.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/Map", "put",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true));
            insnList.add(new InsnNode(Opcodes.POP));
        }

        // add instructions to invoke cleartrack setup..
        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "pac/config/Notify", "setup", "(Ljava/util/Map;)V",
                false));
        methodNode.instructions.insert(insnList);
        methodNode.maxStack += 6;
    }

    /**
     * Adds the instructions to initialize an instrumentation options
     * value.  These options are mapped and sent off to Notify.setup().
     * Currently, only Boolean and EnumSet value types are supported.
     */
    private void addInstrumentationOption(InsnList insnList, Object value) {
        if (value instanceof Boolean) {
            insnList.add(new InsnNode((Boolean) value ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf",
                    "(Z)Ljava/lang/Boolean;", false));
        } else {
            insnList.add(new LdcInsnNode("<< type of '" + value.getClass().getName() + "' is not supported >>"));
        }
    }
}
