package pac.agent.hierarchy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import pac.agent.CleartrackTaintMethodAdapter;
import pac.agent.hierarchy.SkippedClassesParser.SkipRule;
import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationLocation;
import pac.inst.InstrumentationMethod;
import pac.org.apache.commons.io.output.ByteArrayOutputStream;
import pac.org.objectweb.asm.Opcodes;
import pac.org.objectweb.asm.Type;
import pac.org.objectweb.asm.tree.AbstractInsnNode;
import pac.org.objectweb.asm.tree.AnnotationNode;
import pac.org.objectweb.asm.tree.ClassNode;
import pac.org.objectweb.asm.tree.FieldInsnNode;
import pac.org.objectweb.asm.tree.FieldNode;
import pac.org.objectweb.asm.tree.MethodInsnNode;
import pac.org.objectweb.asm.tree.MethodNode;
import pac.org.reflections.Reflections;
import pac.util.Ansi;
import pac.util.AsmUtils;

/**
 * Internal structure of the entire class hierarchy that has been processed by
 * the instrumenter. This is used to determine many things about how to
 * instrument or (in many cases) not instrument classes, fields, and methods.
 * This class is thread-safe.
 * 
 * @author jeikenberry
 */
public class ClassHierarchy implements Serializable {
    private static final long serialVersionUID = 5533101038583963373L;

    public static final String CLASS_PREFIX = "Cleartrack";

    public static final int INST_MODIFIERS = Modifier.PUBLIC | Modifier.STATIC;

    private static ClassHierarchy instance = new ClassHierarchy();

    /**
     * This cannot be serialized because of the parser generator that generates
     * the parser defining these rules.
     */
    private transient Map<String, SkipRule> skippedClasses;
    private transient AtomicInteger nativeMethodsJdk, nativeMethodsApp;

    private Map<String, Set<String>> skippedMethods;
    private Map<String, ClassType> types;
    private Map<Float, Integer> jdkVersionCounts, appVersionCounts;

    private ClassHierarchy() {
        types = new TreeMap<String, ClassType>();

        jdkVersionCounts = new TreeMap<Float, Integer>(Collections.reverseOrder());
        appVersionCounts = new TreeMap<Float, Integer>(Collections.reverseOrder());

        nativeMethodsJdk = new AtomicInteger(0);
        nativeMethodsApp = new AtomicInteger(0);

        // Read in all methods that should be left uninstrumented and
        // all of the class rules for instrumentation
        skippedMethods = new TreeMap<String, Set<String>>();
        loadSkippedMethods(skippedMethods, "META-INF/methods.skip");
        loadSkippedClasses();
    }

    /**
     * Populates methodMap by parsing the file skipFileName, and loading it into
     * the map mapping the class names of each skipped method to the set of all
     * method names method descriptors that are skipped for that class.
     * 
     * @param methodMap
     *            Map&lt;String, Set&lt;String&gt;&gt;
     * @param skipFileName
     *            path to the method skip file to load.
     */
    public static void loadSkippedMethods(Map<String, Set<String>> methodMap, String skipFileName) {
        // read in all methods that will be skipped
        ClassLoader cl = ClassHierarchy.class.getClassLoader();
        boolean asAgent = cl == null; // If cl is null then we must be running
                                      // with dynamic agent

        // Acquire the input stream of the skip file from the jar.
        InputStream inStream = asAgent ? ClassLoader.getSystemResourceAsStream(skipFileName)
                : cl.getResourceAsStream(skipFileName);

        try (BufferedReader in = new BufferedReader(new InputStreamReader(inStream));) {
            String method;
            while ((method = in.readLine()) != null) {
                int commentIdx = method.indexOf('#');
                // strip away the comment and trailing whitespace
                if (commentIdx >= 0)
                    method = method.substring(0, commentIdx);
                method = method.trim();
                if (method.equals(""))
                    continue;
                String[] strs = method.split("\\.");
                Set<String> methodSet = methodMap.get(strs[0]);
                if (methodSet == null) {
                    methodSet = new TreeSet<String>();
                    methodMap.put(strs[0], methodSet);
                }
                methodSet.add(strs[1]);
            }
        } catch (IOException e) {
            // this file should always be in the jar
        }
    }

    /**
     * Parses the logic used to determine whether a class should be skipped or
     * not.
     */
    private void loadSkippedClasses() {
        ClassLoader cl = ClassHierarchy.class.getClassLoader();
        boolean asAgent = cl == null; // If cl is null then we must be running
                                      // with dynamic agent

        // Parse the rules for instrumenting/uninstrumenting classes.
        String fileName = "META-INF/classes.skip";
        SkippedClassesParser parser = new SkippedClassesParser(this,
                asAgent ? ClassLoader.getSystemResourceAsStream(fileName) : cl.getResourceAsStream(fileName));
        try {
            skippedClasses = parser.parse();
        } catch (ParseException e) {
            System.err.println("Parse exception in '" + fileName + "' file: " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Obtains the ClassType object of the typeName in a thread-safe manner.
     * 
     * @param typeName
     *            String of the class name to lookup
     * @return ClassType of the existing/new type.
     */
    private ClassType getType(String typeName, String jar) {
        if (typeName == null)
            return null;
        ClassType type;
        synchronized (types) {
            type = types.get(typeName);
            if (type == null) {
                type = new ClassType(typeName, jar);
                types.put(typeName, type);
            }
        }
        return type;
    }

    /**
     * Functionally equivalent to Class.getSuperClass()
     */
    public Type getSuperClass(Type type) {
        if (type == null)
            return null;
        switch (type.getSort()) {
        case Type.OBJECT:
            if (type.getInternalName().equals("java/lang/Object"))
                return null;
            ClassType classType = types.get(type.getInternalName());
            if (classType == null)
                return Type.getType(Object.class); // assume it's an object
            if (!classType.isClass)
                return null;
            for (ClassType parentType : classType.parents) {
                if (parentType.isClass)
                    return Type.getObjectType(parentType.name);
            }
            return Type.getType(Object.class);
        case Type.ARRAY:
            return Type.getType(Object.class);
        default:
            return null;
        }
    }

    /**
     * Gets the exact match of MethodType object from the given MethodInsnNode.
     * 
     * @param methodInsnNode
     *            MethodInsnNode
     * @return MethodType
     */
    public MethodType getMethod(MethodInsnNode methodInsnNode) {
        ClassType type = types.get(methodInsnNode.owner);
        if (type == null)
            return null;

        for (MethodType method : type.methods) {
            if (method.name.equals(methodInsnNode.name) && method.desc.equals(methodInsnNode.desc)) {
                return method;
            }
        }

        return null;
    }

    /**
     * Returns the fully-qualified (internal name or descriptor) of a dangerous
     * classes subtype. For example,
     * 
     * o java/lang/Integer -> java/lang/CleartrackInteger
     * o [Ljava/lang/Integer -> [Ljava/lang/CleartrackInteger
     * 
     * @param className
     *            String of the classname (internal or descriptor)
     * @return String of the tracked subtype name
     */
    public static String getDangerousSublassName(String className) {
        int idx = className.lastIndexOf('/') + 1;
        StringBuilder buf = new StringBuilder(className.substring(0, idx));
        buf.append(CLASS_PREFIX);
        buf.append(className.substring(idx));
        return buf.toString();
    }

    /**
     * Functionally equivalent to t.isAssignableFrom(u), where t and u are Class
     * objects.
     */
    public boolean isAssignableFrom(Type t, Type u) {
        if (t.equals(u))
            return true;
        // ClassType tClassType = getType(t.getInternalName());
        switch (t.getSort()) {
        case Type.OBJECT:
            switch (u.getSort()) {
            case Type.OBJECT:
                return isA(u.getInternalName(), t.getInternalName());
            case Type.ARRAY:
                return t.getInternalName().equals("java/lang/Object");
            default:
                return false;
            }
        case Type.ARRAY:
            switch (u.getSort()) {
            case Type.OBJECT:
                return false;
            case Type.ARRAY:
                Type tEle = t.getElementType();
                Type uEle = u.getElementType();
                switch (tEle.getSort()) {
                case Type.OBJECT:
                    switch (uEle.getSort()) {
                    case Type.OBJECT:
                        if (t.getDimensions() > u.getDimensions())
                            return false;
                        return isA(uEle.getInternalName(), tEle.getInternalName());
                    default:
                        if (!tEle.getInternalName().equals("java/lang/Object"))
                            return false;
                        return t.getDimensions() < u.getDimensions();
                    }
                default:
                    return false;
                }
            default:
                return false;
            }
        default:
            return t.getSort() == u.getSort();
        }
    }

    /**
     * Functionally equivalent to Class.isInterface()
     */
    public boolean isInterface(Type type) {
        if (type == null || type.getSort() != Type.OBJECT)
            return false;
        ClassType classType = types.get(type.getInternalName());
        if (classType == null)
            return false;
        return !classType.isClass;
    }

    /**
     * Determines whether a given type should be considered dangerous (i.e. if
     * the JVM has hardcoded field offsets for these types).
     * 
     * @param type
     *            Type in question
     * @return boolean
     */
    public boolean isDangerousClass(Type type) {
        switch (type.getSort()) {
        case Type.OBJECT:
            return isDangerousClass(type.getInternalName());
        case Type.ARRAY:
            Type elemType = type.getElementType();
            if (elemType.getSort() != Type.OBJECT)
                return false;
            return isDangerousClass(elemType.getInternalName());
        }
        return false;
    }

    /**
     * Determines whether a given type should be considered dangerous (i.e. if
     * the JVM has hardcoded field offsets for these types).
     * 
     * @param type
     *            String of the classname in question (internal name or
     *            descriptor).
     * @return boolean
     */
    public boolean isDangerousClass(String className) {
        if (className.startsWith("[") || className.endsWith(";"))
            return isDangerousClass(Type.getType(className));
        return skippedClasses.get("dangerous").isSkipped(className);
    }

    /**
     * Determines whether we should not instrument a class, as specified by the
     * set of rules in the classes.skip file. Also note, that all array based
     * Object types are left uninstrumented, regardless of whether the
     * underlying type is instrumented or not.
     * 
     * NOTE: This method should only be used internally.
     * 
     * @param className
     *            String of the classname in question (internal name only).
     * @return boolean
     */
    private boolean isSkippedClass(String className) {
        if (className.startsWith("["))
            return true;
        else if (isJdkClass(className))
            return
            // false
            // !isDangerousClass(className)
            // && !className.equals("java/lang/String")
            // && !className.equals("java/lang/System")
            // ;
            skippedClasses.get("jdk").isSkipped(className);
        return skippedClasses.get("app").isSkipped(className);
    }

    /**
     * Determines whether we should not instrument a class, as specified by the
     * set of rules in the classes.skip file. Also note, that all array based
     * Object types are left uninstrumented, regardless of whether the
     * underlying type is instrumented or not.
     * 
     * @param classNode
     *            ClassNode in question.
     * @return boolean
     */
    public boolean isSkipped(ClassNode classNode) {
        ClassType type = types.get(classNode.name);
        if (type == null)
            return false;
        return type.isSkipped;
    }

    /**
     * Determines whether we should not instrument a field, as determined by a
     * hierarchical search of the declared field. The fieldNode is not
     * instrumented if and only if the declared field representing this node is
     * not instrumented.
     * 
     * @param classNode
     *            ClassNode of the owner.
     * @param fieldNode
     *            FieldNode of the field.
     * @return boolean
     */
    public boolean isSkipped(ClassNode classNode, FieldNode fieldNode) {
        ClassType type = types.get(classNode.name);
        if (type == null)
            return false;
        FieldType field = type.findField(fieldNode.name);
        if (field == null)
            return false;
        return field.isSkipped;
    }

    /**
     * Determines whether we should not instrument a field, as determined by a
     * hierarchical search of the declared field. The fieldNode is not
     * instrumented if and only if the declared field representing this node is
     * not instrumented.
     * 
     * @param fieldInsnNode
     *            FieldInsnNode
     * @return boolean
     */
    public boolean isSkipped(FieldInsnNode fieldInsnNode) {
        ClassType type = types.get(fieldInsnNode.owner);
        if (type == null)
            return false;
        FieldType field = type.findField(fieldInsnNode.name);
        if (field == null)
            return false;
        return field.isSkipped;
    }

    /**
     * Presume that all of the methods in methods.skip file are there because
     * the method was too large.
     * 
     * @param classNode
     * @param methodNode
     * @return
     */
    public boolean isMethodTooLarge(ClassNode classNode, MethodNode methodNode) {
        if (classNode.name.startsWith("["))
            return false;
        Set<String> methodSet = skippedMethods.get(classNode.name);
        if (methodSet == null)
            return false;
        return methodSet.contains(methodNode.name + methodNode.desc);
    }

    /**
     * Determines whether we should not instrument a method, as determined by a
     * hierarchical search of the declared method. The methodNode is not
     * instrumented if and only if the declared method representing this node is
     * not instrumented. All methods whose owner is an array type is essentially
     * of type Object and should not be instrumented.
     * 
     * @param classNode
     *            ClassNode
     * @param methodNode
     *            MethodNode
     * @return boolean
     */
    public boolean isSkipped(ClassNode classNode, MethodNode methodNode) {
        if (classNode.name.startsWith("["))
            return true;
        ClassType type = types.get(classNode.name);
        if (type == null)
            return false;
        MethodType method = type.findMethod(methodNode.name, methodNode.desc);
        if (method == null)
            return false;
        return method.isSkipped;
    }

    /**
     * Determines whether we should not instrument a method, as determined by a
     * hierarchical search of the declared method. The methodNode is not
     * instrumented if and only if the declared method representing this node is
     * not instrumented. All methods whose owner is an array type is essentially
     * of type Object and should not be instrumented.
     * 
     * @param methodInsnNode
     *            methodInsnNode
     * @return boolean
     */
    public boolean isSkipped(MethodInsnNode methodInsnNode, boolean inJdk) {
        if (methodInsnNode.owner.startsWith("["))
            return true;
        ClassType type = types.get(methodInsnNode.owner);
        MethodType method = type == null ? null : type.findMethod(methodInsnNode.name, methodInsnNode.desc);
        if (method == null) {
            // It's possible that we could have a method invocation that was
            // never preprocessed (if the method body was not analyzed)...
            Set<String> methodSet = skippedMethods.get(methodInsnNode.owner);
            if (methodSet == null)
                return false;
            return methodSet.contains(methodInsnNode.name + methodInsnNode.desc);
        }
        if (method.isSkipped) {
            method = type.findMethod(methodInsnNode.name,
                                     CleartrackTaintMethodAdapter.toPrimitiveDesc(methodInsnNode.desc));
            // Check to see if we added an instrumentation wrapper to a method
            // this is not instrumented. We will still say this is instrumented,
            // since the wrapper will be replaced on this method.
            if (method != null) {
                // We do not perform method replacements on INVOKESPECIAL (other
                // than constructors)
                if (AsmUtils.hasModifiers(methodInsnNode.getOpcode(), Opcodes.INVOKESPECIAL)
                        && !methodInsnNode.name.equals("<init>"))
                    return true;
                InstrumentationMethod instMethod = method.getTargetAnnotation(methodInsnNode);
                if (instMethod == null)
                    return true;

                // If this method invocation is in the JDK and the wrapper will
                // only be replaced in the application, then this should still be
                // skipped.
                if (inJdk && instMethod.instrumentationLocation() == InstrumentationLocation.APP) {
                    return true;
                }
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * @param classNode ClassNode of the owner
     * @param methodNode MethodNode of the method in question
     * @return true if and only if methodNode is recursive
     */
    public boolean isRecursive(ClassNode classNode, MethodNode methodNode) {
        ClassType type = types.get(classNode.name);
        if (type == null)
            return false;
        MethodType method = type.findMethod(methodNode.name, methodNode.desc);
        if (method == null)
            return false;
        return method.isRecursive && method.isUnique();
    }

    /**
     * @param fieldNode
     *            FieldInsnNode
     * @return true if and only if the given field contains a constant value
     */
    public boolean isConstant(FieldInsnNode fieldNode) {
        ClassType type = types.get(fieldNode.owner);
        if (type == null)
            return false;
        FieldType field = type.findField(fieldNode.name);
        if (field == null)
            return false;
        return field.isConstant;
    }

    public boolean isStaticField(String owner, String fieldName) {
        ClassType type = types.get(owner);
        if (type == null)
            return false;
        FieldType field = type.findField(fieldName);
        if (field == null)
            return false;
        return field.isStatic;
    }

    /**
     * @param ownerClass
     *            ClassNode
     * @param fieldNode
     *            FieldInsnNode
     * @return true if and only if the given field contains a constant value
     */
    public boolean isConstant(ClassNode ownerClass, FieldNode fieldNode) {
        ClassType type = types.get(ownerClass.name);
        if (type == null)
            return false;
        FieldType field = type.findField(fieldNode.name);
        if (field == null)
            return false;
        return field.isConstant;
    }

    /**
     * @param child
     *            String of the internal name of the child.
     * @param parent
     *            String of the internal name of the parent.
     * @return true if and only if child is a direct or indirect subclass of
     *         parent
     */
    public boolean isA(String child, String parent) {
        if (child.equals(parent))
            return true;
        ClassType childType = types.get(child);
        ClassType parentType = types.get(parent);
        if (childType == null) {
            if (parentType == null)
                return false;
            // We may not know the exact type, but at least we know
            // it is of type Object.
            return parentType.name.equals("java/lang/Object");
        } else if (parentType == null) {
            return false;
        }
        Stack<ClassType> stack = new Stack<ClassType>();
        stack.push(childType);
        while (!stack.isEmpty()) {
            ClassType subtype = stack.pop();
            if (subtype == null)
                continue;
            Set<ClassType> inherited = subtype.parents;
            if (inherited == null || inherited.isEmpty())
                continue;
            if (inherited.contains(parentType))
                return true;
            for (ClassType next : inherited)
                stack.push(next);
        }
        return false;
    }

    /**
     * @param className
     *            String of the internal name
     * @return true if and only if className is defined in a JDK jar
     */
    public boolean isJdkClass(String className) {
        ClassType type = types.get(className);
        if (type == null)
            return false;
        return type.inJdk | type.isCompatibility;
    }

    /**
     * @param className
     *            String of the internal name
     * @return true if and only if className is defined in a compatibility jar
     */
    public boolean isCompatibilityClass(String className) {
        ClassType type = types.get(className);
        if (type == null)
            return false;
        return type.isCompatibility;
    }

    /**
     * It is entirely possible for a classfile to contain duplicate fields of
     * different types. This is problematic for us, since we are changing the
     * underlying type in the instrumented field.
     * 
     * @param className
     *            String of the classname in question (in internal form).
     * @return true if and only if classNode has duplicate field names.
     */
    public boolean hasDuplicatePrimField(String className) {
        ClassType type = types.get(className);
        if (type == null)
            return false;
        return type.hasDuplicatePrimField;
    }

    /**
     * Determines whether owner overrides an instrumented abstract method
     * compatible with the method descriptor desc and matching the same name as
     * the given method name.
     * 
     * @param owner
     *            String of the owner classname
     * @param name
     *            String of the method name
     * @param desc
     *            String of the method descriptor
     * @return boolean
     */
    private boolean overridesInstrumentedMethod(String owner, String name, String desc) {
        ClassType type = types.get(owner);
        if (type == null)
            return false;
        Stack<ClassType> stack = new Stack<ClassType>();
        stack.push(type);
        while (!stack.isEmpty()) {
            ClassType subtype = stack.pop();
            if (subtype == null)
                continue;
            Set<ClassType> inherited = subtype.parents;
            if (inherited == null || inherited.isEmpty())
                continue;
            for (ClassType next : inherited) {
                MethodType matchingMethod = next.getMethod(name, desc);
                if (matchingMethod != null && !matchingMethod.isSkipped
                        && (AsmUtils.hasModifiers(matchingMethod.access, Opcodes.ACC_ABSTRACT)
                        // || next.name.equals("java/lang/Object")
                        ))
                    return true;
                stack.push(next);
            }
        }
        return false;
    }

    /**
     * Determines whether classNode overrides an instrumented abstract method
     * compatible with methodNode.
     * 
     * @param classNode
     *            ClassNode
     * @param methodNode
     *            MethodNode
     * @return boolean
     */
    public boolean overridesInstrumentedMethod(ClassNode classNode, MethodNode methodNode) {
        return overridesInstrumentedMethod(classNode.name, methodNode.name, methodNode.desc);
    }

    /**
     * Determines whether methodInsnNode's owner overrides an instrumented
     * abstract method compatible with methodInsnNode.
     * 
     * @param methodInsnNode
     *            MethodInsnNode
     * @return boolean
     */
    public boolean overridesInstrumentedMethod(MethodInsnNode methodInsnNode) {
        return overridesInstrumentedMethod(methodInsnNode.owner, methodInsnNode.name, methodInsnNode.desc);
    }

    /**
     * Obtains all inherited MethodType objects for classNode that inherit an
     * instrumented abstract method, whose method is not defined in classNode.
     * This can happen when a class implements an interface with foo(), and
     * extends a class defining foo().
     * 
     * @param classNode
     *            ClassNode
     * @return Set&lt;MethodType&gt;
     */
    public Set<MethodType> getAllInheritedMethodsNotDefined(ClassNode classNode) {
        Set<MethodType> results = new TreeSet<MethodType>();
        ClassType type = types.get(classNode.name);
        if (type == null)
            return results;

        Stack<ClassType> stack = new Stack<ClassType>();
        stack.push(type);
        while (!stack.isEmpty()) {
            ClassType subtype = stack.pop();
            if (subtype == null)
                continue;
            Set<ClassType> inherited = subtype.parents;
            if (inherited == null || inherited.isEmpty())
                continue;
            for (ClassType next : inherited) {
                for (MethodType method : next.methods) {
                    if (method.name.equals("<init>") || method.name.equals("<clinit>")) {
                        continue;
                    } else {
                        // also skip over instrumented methods
                        Type[] argTypes = Type.getArgumentTypes(method.desc);
                        if (argTypes.length > 0) {
                            Type lastArg = argTypes[argTypes.length - 1];
                            if (lastArg.getSort() == Type.OBJECT && lastArg.getInternalName().equals("pac/util/Ret")) {
                                continue;
                            }
                        }
                    }

                    // Do only if the original type does not have said method
                    if (type.getMethod(method.name, method.desc) == null) {
                        // Get only the first unique method up the chain, for
                        // any other method can't be abstract.
                        boolean shouldAdd = true;
                        Iterator<MethodType> iter = results.iterator();
                        while (iter.hasNext()) {
                            MethodType resultMethod = iter.next();
                            if (resultMethod.name.equals(method.name) && resultMethod.desc.equals(method.desc)) {
                                // Always write over the method that is not skipped and 
                                // not abstract. we will remove them in the end.
                                if ((!method.isSkipped || method.owner.isCompatibility)
                                        && !AsmUtils.hasModifiers(method.access, Opcodes.ACC_ABSTRACT)) {
                                    shouldAdd = true;
                                    iter.remove();
                                } else {
                                    shouldAdd = false;
                                }
                                break;
                            }
                        }
                        if (shouldAdd)
                            results.add(method);
                    }
                }
                stack.push(next);
            }
        }

        // Only keep the methods that are both instrumented and abstract
        Iterator<MethodType> iter = results.iterator();
        while (iter.hasNext()) {
            MethodType methodType = iter.next();
            if (methodType.isSkipped || !AsmUtils.hasModifiers(methodType.access, Opcodes.ACC_ABSTRACT))
                iter.remove();
        }
        return results;
    }

    /**
     * @param classNode
     *            ClassNode
     * @return String of the internal form name of the first class found that
     *         has a default constructor (starting from classNode and walking
     *         up).
     */
    public String getNearestDefaultConstructorClass(ClassNode classNode) {
        ClassType type = types.get(classNode.name);
        if (type == null)
            return "java/lang/Object";
        return type.getNearestDefaultConstructorClass();
    }

    /**
     * This needs to be called once in CleartrackMain prior to the instrumenter
     * running, such that we don't introduce a data race.
     * 
     * @return ClassHierarchy
     */
    public static ClassHierarchy getInstance() {
        return instance;
    }

    /**
     * Loads an instance from a serialized object. This should only be called if
     * being used as a dynamic agent (and called only once).
     * 
     * @return ClassHierarchy of the deserialized object.
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public static ClassHierarchy loadInstance() throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(ClassLoader.getSystemResourceAsStream("hierarchy.obj"))) {
            instance = (ClassHierarchy) in.readObject();
            instance.loadSkippedClasses(); // need to reload this since it's not
                                           // serialized
            return instance;
        }
    }

    /**
     * Serializes an instance of the ClassHierarchy. This should only be called
     * once the class hierarchy has been constructed in it's entirety (i.e after
     * postProcess() has been called).
     * 
     * @return byte[] of the class hierarchy
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static byte[] saveInstance() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream os = new ObjectOutputStream(baos)) {
            os.writeObject(instance);
            return baos.toByteArray();
        }
    }

    /**
     * Processes a single classNode into the ClassHierarchy instance. This needs
     * to be thread-safe, since we are writing/reading to fields and this method
     * can be called by many threads.
     * 
     * @param classNode
     *            ClassNode
     * @param inJdk
     *            - true if classNode is defined in a JDK jar.
     * @param isCompatibility
     *            - true if classNode came from a compatibility jar file (i.e.
     *            an application library jar that we are entirely skipping).
     * @return true if and only if classNode contains a JSR or RET instruction.
     */
    public boolean process(ClassNode classNode, boolean inJdk, boolean isCompatibility, String checksum,
                           String jarName) {
        // Update version counts
        int majorNum = classNode.version & 0xffff;
        float version = majorNum == Opcodes.V1_1 ? 1.1f : (majorNum - 34.0f) / 10.0f;
        Map<Float, Integer> versionCountMap = inJdk ? jdkVersionCounts : appVersionCounts;
        synchronized (versionCountMap) {
            Integer versionCount = versionCountMap.get(version);
            if (versionCount == null)
                versionCountMap.put(version, 1);
            else
                versionCountMap.put(version, versionCount + 1);
        }

        // Strategy: keep the ClassType whose checksum is alphabetically first,
        // except keep JDK ClassTypes over application ClassTypes
        ClassType classType, oldClassType = null;
        boolean existingType = false;
        synchronized (types) {
            classType = types.get(classNode.name);
            if (classType == null) {
                classType = new ClassType(classNode.name, jarName);
                types.put(classNode.name, classType);
            } else {
                existingType = true;
                // FIXME This could cause a race condition on classType (though
                // it would be very unlikely)... Off the top of my head, I'm not
                // sure how to make this the way it ought to be without
                // introducing a deadlock scenario.
            }
        }

        synchronized (classType) {
            if (existingType) {
                // If the existing type is a JDK class and the type we are
                // processing is not a JDK class, then keep the existing
                // JDK type...
                if (classType.inJdk && !inJdk) { // prefer JDK over app
                    return processMethods(classNode);
                } else if (classType.checksum != null) {
                    // Replace the existing ClassType if either the currently
                    // processed type is a JDK class and the current type is
                    // not or the checksum of this currently processed class
                    // comes before the existing types checksum...
                    if ((!classType.inJdk && inJdk) || checksum.compareTo(classType.checksum) < 0) {
                        oldClassType = classType;
                        classType = new ClassType(classNode.name, jarName);
                        synchronized (types) {
                            types.put(classNode.name, classType);
                        }
                    } else {
                        return processMethods(classNode);
                    }
                }
            }

            classType.isClass = (classNode.access & Opcodes.ACC_INTERFACE) != Opcodes.ACC_INTERFACE;
            classType.isCompatibility = isCompatibility;
            classType.inJdk = inJdk;
            classType.hasDuplicatePrimField = false;
            classType.checksum = checksum;

            if (oldClassType != null) {
                synchronized (oldClassType) {
                    // We need to first invalidate this child with both
                    // it's child types and parent types, so that they
                    // may be added again with this new ClassType object.
                    for (ClassType parent : oldClassType.parents) {
                        synchronized (parent) {
                            parent.children.remove(oldClassType);
                        }
                    }
                    for (ClassType child : oldClassType.children) {
                        synchronized (child) {
                            child.parents.remove(oldClassType);
                            child.parents.add(classType);
                        }
                        classType.children.add(child);
                    }

                    // Ensure no memory leaks.
                    oldClassType.parents.clear();
                    oldClassType.children.clear();
                }
            }

            // Add parent class and interface types...
            if (classNode.superName != null) {
                ClassType parent = getType(classNode.superName, jarName);
                synchronized (parent) {
                    parent.isClass = true;
                    parent.children.add(classType);
                }
                classType.parents.add(parent);
            }
            for (String inter : classNode.interfaces) {
                ClassType parent = getType(inter, jarName);
                synchronized (parent) {
                    parent.isClass = false;
                    parent.children.add(classType);
                }
                classType.parents.add(parent);
            }

            processFields(classNode, classType);
            boolean hasJsrOrRetInsn = processMethods(classNode, classType, inJdk);

            if (classNode.visibleAnnotations != null) {
                for (AnnotationNode annotation : classNode.visibleAnnotations) {
                    // We need to skip Entity classes for hibernate to work properly.
                    if ("Ljavax/persistence/Entity;".equals(annotation.desc)) {
                        classType.skip();
                        break;
                    }
                }
            }

            return hasJsrOrRetInsn;
        }
    }

    /**
     * Processes a Class object that is loaded into the JVM, but has not been
     * pre-processed into the hierarchy (i.e. instrumentation wrappers that refer
     * to cleartrack classes).
     * 
     * @param c Class object to process.
     */
    private void process(Class<?> c) {
        String name = Type.getInternalName(c);
        ClassType ct = new ClassType(name, "cleartrack.jar");
        for (Field field : c.getDeclaredFields())
            ct.process(field);
        for (Method method : c.getDeclaredMethods())
            ct.process(method);
        for (Constructor<?> constructor : c.getDeclaredConstructors())
            ct.process(constructor);
        synchronized (types) {
            // TODO we need to update the children of each parent class along the way
            types.put(name, ct);
            Class<?> superClass = c.getSuperclass();
            String superName = superClass.getName().replace('.', '/');
            if (!types.containsKey(superName))
                process(superClass);
            for (Class<?> interfaceClass : c.getInterfaces()) {
                superName = interfaceClass.getName().replace('.', '/');
                if (!types.containsKey(superName))
                    process(interfaceClass);
            }
        }
    }

    private void processFields(ClassNode classNode, ClassType classType) {
        // Process all of the field types and mark any class that contains
        // duplicate field names of different primitive types...
        Map<String, String> primFieldMap = new HashMap<String, String>();
        for (FieldNode fieldNode : classNode.fields) {
            classType.process(fieldNode);
            Type fieldType = Type.getType(fieldNode.desc);
            switch (fieldType.getSort()) {
            case Type.OBJECT:
            case Type.ARRAY:
                break;
            default:
                if (primFieldMap.containsKey(fieldNode.name))
                    classType.hasDuplicatePrimField = true;
                primFieldMap.put(fieldNode.name, fieldNode.desc);
            }
        }
    }

    private boolean processMethods(ClassNode classNode) {
        // If the class file version is less than Java 1.5, then
        // this class will not have stack frames and so let's not
        // recompute them.
        if ((classNode.version & 0xffff) < Opcodes.V1_5) {
            return true;
        }

        for (MethodNode methodNode : classNode.methods) {
            for (AbstractInsnNode curNode = methodNode.instructions.getFirst(); curNode != null; curNode = curNode
                    .getNext()) {
                switch (curNode.getOpcode()) {
                case Opcodes.RET:
                case Opcodes.JSR:
                    return true;
                }
            }
        }
        return false;
    }

    private boolean processMethods(ClassNode classNode, ClassType classType, boolean inJdk) {
        // If the class file version is less than Java 1.5, then
        // this class will not have stack frames and so let's not
        // recompute them.
        boolean hasJsrOrRetInsn = false;
        if ((classNode.version & 0xffff) < Opcodes.V1_5) {
            hasJsrOrRetInsn = true;
        }

        // Process all of the methods, marking all JUnit test classes and
        // the ClassType if there is a method with a JSR or RET
        // instructions...
        for (MethodNode methodNode : classNode.methods) {
            if ((methodNode.access & Opcodes.ACC_NATIVE) == Opcodes.ACC_NATIVE) {
                if (inJdk)
                    nativeMethodsJdk.incrementAndGet();
                else
                    nativeMethodsApp.incrementAndGet();
            }

            MethodType methodType = classType.process(methodNode);

            // No need to go through the instructions, since we presume that the
            // application has been compiled with Java 1.7.  Therefore, there
            // should be no JSR or RET instructions.
            if (hasJsrOrRetInsn)
                continue;

            for (AbstractInsnNode curNode = methodNode.instructions.getFirst(); curNode != null; curNode = curNode
                    .getNext()) {
                switch (curNode.getOpcode()) {
                case Opcodes.INVOKESTATIC:
                case Opcodes.INVOKEVIRTUAL:
                    // Only mark methods as recursive if they are virtual/static
                    // methods that share the same name and descriptor.
                    MethodInsnNode methodInsnNode = (MethodInsnNode) curNode;
                    if (!methodNode.name.equals("<init>") && !methodNode.name.equals("<clinit>")
                            && methodNode.name.equals(methodInsnNode.name)
                            && methodNode.desc.equals(methodInsnNode.desc)) {
                        methodType.isRecursive = true;
                    }
                    break;
                case Opcodes.RET:
                case Opcodes.JSR:
                    hasJsrOrRetInsn = true;
                    break;
                }
            }
        }
        return hasJsrOrRetInsn;
    }

    /**
     * Only call this method once all class types have been processed. This will
     * make a second pass over all class types and finalize the
     * methods/fields/classes that we are not instrumenting. We can only know
     * this once the entire heirarchy has been built (hence the second pass). We
     * also need add instrumented methods to all ClassType objects in our
     * hierarchy, so that we can quickly lookup the instrumentation wrappers for
     * these methods.
     * 
     * This does not need to be thread safe, since this is called only once
     * after everything has already been processed by ASM.
     * 
     * @param outputDirFile
     * 
     * @param saveInstance
     *            true if we should serialize the heirarchy once we are done
     *            post-processing.
     */
    public void postProcess(File outputDirFile, String instPackage) {
        Map<String, String> dangerousTypeMap = new TreeMap<String, String>();
        int skippedJdk = 0, skippedApp = 0;
        int instrumentedJdk = 0, instrumentedApp = 0;

        ClassType objType = types.get("java/lang/Object");
        Set<String> mainClasses = new TreeSet<String>();
        Map<String, Set<String>> jarDependencies = new TreeMap<String, Set<String>>();
        Map<String, String> jarNodeMap = new TreeMap<String, String>();
        for (ClassType type : types.values()) {
            // if the type has no parent, then the class definition must be
            // missing.  let's just safely assume the parent is java/lang/Object.
            if (type != objType && type.parents.isEmpty())
                type.parents.add(objType);

            if (type.isCompatibility || isSkippedClass(type.name)) {
                type.skip();
                if (type.inJdk)
                    skippedJdk++;
                else
                    skippedApp++;
                type.addInstrumentedMethods(type);
            } else {
                // mark all skipped methods of this type, if there are any...
                Set<String> methodSet = skippedMethods.get(type.name);
                if (methodSet != null) {
                    for (String method : methodSet) {
                        int idx = method.indexOf('(');
                        String methodName = method.substring(0, idx);
                        String methodDesc = method.substring(idx);
                        MethodType methodObj = type.getMethod(methodName, methodDesc);
                        if (methodObj != null)
                            methodObj.isSkipped = true;
                    }
                }

                if (isDangerousClass(type.name))
                    dangerousTypeMap.put(type.name, getDangerousSublassName(type.name));
                else
                    type.addInstrumentedMethods(type);

                if (type.inJdk)
                    instrumentedJdk++;
                else
                    instrumentedApp++;
            }

            if (!type.inJdk) {
                if (type.hasMain)
                    mainClasses.add(type.name);
                if (type.jar != null) {
                    for (ClassType parent : type.parents) {
                        if (parent.inJdk || parent.jar == null || type.jar.equals(parent.jar))
                            continue;
                        String childClassName = getNextJarNode(jarNodeMap, type);
                        String parentClassName = getNextJarNode(jarNodeMap, parent);
                        Set<String> outEdges = jarDependencies.get(childClassName);
                        if (outEdges == null) {
                            outEdges = new HashSet<String>();
                            jarDependencies.put(childClassName, outEdges);
                        }
                        outEdges.add(parentClassName);
                    }
                }
            }
        }

        // Process all dangerous types into the heirarchy, so that the
        // verifier can work correctly.
        for (Entry<String, String> entry : dangerousTypeMap.entrySet()) {
            ClassType origType = types.get(entry.getKey());
            ClassType subType = getType(entry.getValue(), origType.jar);
            subType.inJdk = origType.inJdk;
            subType.parents.add(origType);
            subType.addInstrumentedMethods(origType);
        }

        // Output final results
        System.out.printf("Application classes: instumented = %7d  uninstrumented = %7d\n", instrumentedApp,
                          skippedApp);
        System.out.printf("JDK classes:         instumented = %7d  uninstrumented = %7d\n", instrumentedJdk,
                          skippedJdk);

        Set<Float> allVersions = new TreeSet<Float>(Collections.reverseOrder());
        allVersions.addAll(jdkVersionCounts.keySet());
        allVersions.addAll(appVersionCounts.keySet());
        for (Float version : allVersions) {
            Integer jdkCount = jdkVersionCounts.get(version);
            Integer appCount = appVersionCounts.get(version);
            System.out.printf("Java %.1f classes:            jdk = %7d     application = %7d\n", version,
                              jdkCount == null ? 0 : jdkCount, appCount == null ? 0 : appCount);
        }

        System.out.printf("Native methods:              jdk = %7d     application = %7d\n", nativeMethodsJdk.intValue(),
                          nativeMethodsApp.intValue());
        // System.out.println("Main classes:");
        // for (String mainClass : mainClasses)
        // System.out.printf(" * %s\n", mainClass);

        // Dynamically load all instrumented classes from
        // their annotation...
        Reflections reflections = new Reflections(instPackage);
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(InstrumentationClass.class);
        for (Class<?> annotation : annotated) {
            addInstrumentationClass(annotation);
        }

        // Write out the application jar dependency graph
        try (PrintWriter jarDepsOut = new PrintWriter(new File(outputDirFile, "jardeps.dot"));) {
            jarDepsOut.println("strict digraph JARDEPS {");
            for (String nodeDef : jarNodeMap.values()) {
                jarDepsOut.println("  " + nodeDef);
            }
            for (Entry<String, Set<String>> entry : jarDependencies.entrySet()) {
                for (String jarDep : entry.getValue())
                    jarDepsOut.printf("  \"%s\" -> \"%s\"\n", entry.getKey(), jarDep);
            }
            jarDepsOut.println("}");
        } catch (FileNotFoundException e1) {
            Ansi.warn("unable to write jar dependency graph: %s", null, e1);
        }

        // Serialize this class hierarchy object as an xml file, for
        // later viewing.
        try (PrintWriter out = new PrintWriter(new File(outputDirFile, "hierarchy.xml"))) {
            out.print(toXML());
            out.close();
        } catch (IOException e) {
            Ansi.warn("unable to write ClassHierarchy object to disk: %s", null, e);
        }
    }

    private static String nextJarNode = "a";

    private String getNextJarNode(Map<String, String> jarNodeMap, ClassType type) {
        String nodeName = jarNodeMap.get(type.jar);
        if (nodeName == null) {
            nodeName = nextJarNode;
            if (nextJarNode.endsWith("z")) {
                nextJarNode += "a";
            } else {
                char nextChar = (char) (((int) nextJarNode.charAt(nextJarNode.length() - 1)) + 1);
                nextJarNode = nextJarNode.substring(0, nextJarNode.length() - 1) + nextChar;
            }
            if (type.isCompatibility) {
                nodeName += " [shape=box,label=\"" + type.jar + "\",color=red,style=filled];";
            } else {
                nodeName += " [shape=box,label=\"" + type.jar + "\",color=lightblue,style=filled];";
            }
            jarNodeMap.put(type.jar, nodeName);
        }
        return nodeName.substring(0, nodeName.indexOf("[") - 1);
    }

    /**
     * Takes a classNode annotated with pac.inst.InstrumentationClass, and
     * processes all methods annotated by pac.inst.InstrumentationMethod.
     * 
     * @param classObj
     *            Class
     */
    private void addInstrumentationClass(Class<?> classObj) {
        InstrumentationClass instrumentationFor = classObj.getAnnotation(InstrumentationClass.class);
        ClassType type = types.get(instrumentationFor.value());
        if (type == null) {
            try {
                // It's possible that this instrumentation class refers to a cleartrack
                // class, which will not have been processed by cleartrack.  So in this
                // case, we load this type and it's parent types into the hierarchy
                // (presuming it's on the classpath).
                Class<?> klass = Class.forName(instrumentationFor.value().replace('/', '.'));
                process(klass);
                type = types.get(instrumentationFor.value());
                if (type == null)
                    return; // should not happen
            } catch (ClassNotFoundException e) {
                return;
            }
        }
        type.target = instrumentationFor;

        for (Method method : classObj.getMethods()) {
            InstrumentationMethod target = method.getAnnotation(InstrumentationMethod.class);
            if (null != target) {
                if (INST_MODIFIERS == (method.getModifiers() & INST_MODIFIERS)) {
                    for (MethodType methodType : type.getMethods(method, target)) {
                        methodType.addTarget(method, target);
                    }
                } else {
                    Ansi.warn("instrumentation method '%s' is either not declared public or not static.", null, method);
                }
            }
        }
    }

    /**
     * Convert the hierarchy into XML form (useful for debugging).
     * 
     * @return String
     */
    public String toXML() {
        StringBuilder classes = new StringBuilder();
        classes.append("<classes>\n");
        for (ClassType classType : types.values())
            classes.append(classType.toXML());
        classes.append("</classes>");
        return classes.toString();
    }

}
