package pac.agent.hierarchy;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import pac.agent.CleartrackTaintClassAdapter;
import pac.agent.CleartrackTaintMethodAdapter;
import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.inst.InvocationType;
import pac.org.apache.commons.lang.StringEscapeUtils;
import pac.org.objectweb.asm.Opcodes;
import pac.org.objectweb.asm.Type;
import pac.org.objectweb.asm.tree.FieldNode;
import pac.org.objectweb.asm.tree.MethodNode;

/**
 * This class represents a class in our hierarchy.  This class is not thread-safe,
 * but the only place it really needs synchronization is in ClassHierarchy.process().
 * 
 * @author jeikenberry
 */
public class ClassType implements Serializable, Comparable<ClassType> {
    public static final ClassHierarchy hierarchy = ClassHierarchy.getInstance();

    private static final long serialVersionUID = -2228878796743109037L;

    /*
     * Be careful to synchronize over the ClassType object before
     * modifying these fields in methods that can be invoked by
     * multiple threads.
     */
    protected InstrumentationClass target;
    protected String name, checksum;
    protected boolean isClass, isCompatibility, inJdk, isSkipped, hasDefaultConstructor, hasDuplicatePrimField, hasMain;

    /*
     * These data structures need to be thread-safe, since they can be
     * modified by multiple threads.
     */
    protected Set<ClassType> parents;
    protected Set<ClassType> children;
    protected Set<MethodType> methods;
    protected Set<FieldType> fields;
    protected String jar;

    ClassType(String name, String jar) {
        this.name = name;
        this.jar = jar;
        methods = new TreeSet<MethodType>();
        fields = new TreeSet<FieldType>();
        parents = new TreeSet<ClassType>();
        children = new TreeSet<ClassType>();
    }

    public String getName() {
        return name;
    }

    /**
     * Add instrumentation MethodType objects to this ClassType for all
     * methods in the type ClassType that are instrumented.
     * 
     * @param type ClassType that is either the same as this ClassType, or
     *   is the dangerous classType (and type is the subclass).
     */
    public void addInstrumentedMethods(ClassType type) {
        for (MethodType method : type.methods.toArray(new MethodType[0])) {
            if (method.isSkipped)
                continue;
            MethodType newMethod = new MethodType(type, method.access, method.name,
                    CleartrackTaintMethodAdapter.toPrimitiveDesc(method.desc), method.signature, method.exceptions);
            methods.add(newMethod);
        }
    }

    /**
     * @param classNode ClassNode
     * @return String of the internal form name of the first class found
     *   that has a default constructor (starting from classNode and walking
     *   up).
     */
    public String getNearestDefaultConstructorClass() {
        if (hasDefaultConstructor || name.equals("java/lang/Object"))
            return name;
        for (ClassType parent : parents) {
            if (parent.isClass) {
                return parent.getNearestDefaultConstructorClass();
            }
        }
        return null; // should never get here!
    }

    /**
     * Processes the field node and adds the field to this type.
     * 
     * @param fieldNode FieldNode
     * @return FieldType of field that was added.
     */
    protected FieldType process(FieldNode fieldNode) {
        FieldType fieldType = new FieldType(this, fieldNode.name, fieldNode.desc, fieldNode.value != null || !isClass,
                (fieldNode.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC);
        fields.add(fieldType);
        return fieldType;
    }

    /**
     * Processes the java Field into this class, for ClassTypes that are
     * loaded late into the class hierarchy.
     * 
     * @param field Field
     * @return FieldType of field that was added.
     */
    protected FieldType process(Field field) {
        FieldType fieldType = new FieldType(this, field.getName(), Type.getDescriptor(field.getType()), false,
                (field.getModifiers() & Modifier.STATIC) == Modifier.STATIC);
        fields.add(fieldType);
        return fieldType;
    }

    /**
     * Processes the method node and adds the method to this type.
     * 
     * @param methodNode MethodNode
     * @return MethodType of method that was added.
     */
    protected MethodType process(MethodNode methodNode) {
        if (methodNode.name.equals("<init>") && methodNode.desc.equals("()V"))
            hasDefaultConstructor = true;
        else if (CleartrackTaintClassAdapter.isMainMethod(methodNode))
            hasMain = true;
        MethodType methodType = new MethodType(this, methodNode.access, methodNode.name, methodNode.desc,
                methodNode.signature, methodNode.exceptions);
        methods.add(methodType);
        return methodType;
    }

    /**
     * Processes the java Method into this class, for ClassTypes that
     * are loaded late into the class hierarchy.
     * 
     * @param method Method
     * @return MethodType of method that was added.
     */
    protected MethodType process(Method method) {
        if (CleartrackTaintClassAdapter.isMainMethod(method))
            hasMain = true;
        MethodType methodType = new MethodType(this, method.getModifiers(), method.getName(),
                Type.getMethodDescriptor(method), null, new ArrayList<String>(0));
        methods.add(methodType);
        return methodType;
    }

    /**
     * Processes the java Constructor into this class, for ClassTypes that
     * are loaded late into the class hierarchy.
     * 
     * @param constructor Constructor
     * @return MethodType of method that was added.
     */
    protected MethodType process(Constructor<?> constructor) {
        String constructorDesc = Type.getConstructorDescriptor(constructor);
        if (constructorDesc.equals("()V"))
            hasDefaultConstructor = true;
        MethodType methodType = new MethodType(this, constructor.getModifiers(), "<init>", constructorDesc, null,
                new ArrayList<String>(0));
        methods.add(methodType);
        return methodType;
    }

    /**
     * Finds the first declared field matching the given name.
     * 
     * @param name String of the field name to match.
     * @return FieldType
     */
    protected FieldType findField(String name) {
        for (FieldType field : fields) {
            if (field.name.equals(name))
                return field;
        }

        if (parents.isEmpty())
            return null;
        for (ClassType parent : parents) {
            FieldType field = parent.findField(name);
            if (field != null)
                return field;
        }
        return null;
    }

    /**
     * Finds the first declared method matching the given name and
     * compatible with the given method descriptor.
     * 
     * @param name String of the method name to match.
     * @param desc String of the method descriptor to match.
     * @return MethodType
     */
    protected MethodType findMethod(String name, String desc) {
        MethodType result = getMethod(name, desc);
        if (result != null)
            return result;

        if (parents.isEmpty())
            return null;
        for (ClassType parent : parents) {
            MethodType method = parent.findMethod(name, desc);
            if (method != null)
                return method;
        }
        return null;
    }

    /**
     * Gets the method defined in this ClassType matching the given name
     * and compatible with the given method descriptor.
     * 
     * @param name String of the method name to match.
     * @param desc String of the method descriptor to match.
     * @return MethodType
     */
    protected MethodType getMethod(String name, String desc) {
        for (MethodType method : methods) {
            if (method.name.equals(name) && areCompatible(method.desc, desc))
                return method;
        }
        return null;
    }

    /**
     * Compares two descriptors and determines whether one is compatible with
     * the other (i.e. they can both be called using the same arguments).
     * 
     * @param desc1 String of the first method descriptor
     * @param desc2 String of the second method descriptor
     * @return true if and only if desc1 is compatible with desc2
     */
    protected boolean areCompatible(String desc1, String desc2) {
        Type[] args1 = Type.getArgumentTypes(desc1);
        Type[] args2 = Type.getArgumentTypes(desc2);
        if (args1.length != args2.length)
            return false;
        for (int i = 0; i < args1.length; i++) {
            if (args1[i].getSort() != args2[i].getSort())
                return false;
            switch (args1[i].getSort()) {
            case Type.OBJECT:
                if (!hierarchy.isA(args1[i].getInternalName(), args2[i].getInternalName()))
                    return false;
                break;
            case Type.ARRAY:
                if (args1[i].getDimensions() != args2[i].getDimensions())
                    return false;
                Type elemType1 = args1[i];
                Type elemType2 = args2[i];
                if (elemType1.getSort() != elemType2.getSort())
                    return false;
                if (elemType1.getSort() == Type.OBJECT) {
                    if (!hierarchy.isA(elemType1.getInternalName(), elemType2.getInternalName()))
                        return false;
                }
            }
        }
        return true;
    }

    /**
     * Mark this class (including all fields and methods) as not instrumented.
     */
    protected void skip() {
        isSkipped = true;
        for (FieldType field : fields) {
            field.isSkipped = true;
        }
        for (MethodType method : methods) {
            method.isSkipped = true;
        }
    }

    /**
     * @return true if this ClassType is a class and false if it is an interface.
     */
    boolean isClass() {
        return isClass;
    }

    /**
     * @return the name of this ClassType
     */
    public String toString() {
        return name;
    }

    /**
     * Finds all MethodType objects that should be replaced by the wrapped method
     * refered to by instMethod and matching the InstrumentationMethod target.
     * 
     * @param instMethod Method
     * @param target InstrumentationMethod
     * @return Setlt;MethodTypegt; of all types matching this target.
     */
    protected Set<MethodType> getMethods(Method instMethod, InstrumentationMethod target) {
        Set<MethodType> results = new TreeSet<MethodType>();

        // acquire the original descriptor from target, or construct it
        // automically from instMethod if a descriptor was not supplied.
        String desc = target.descriptor();
        if (desc == null || desc.length() == 0) {
            desc = Type.getMethodDescriptor(instMethod);
            if (target.invocationType() == InvocationType.CONSTRUCTOR) {
                desc = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getArgumentTypes(desc));
            } else if (target.invocationType() != InvocationType.STATIC) {
                Type[] args = Type.getArgumentTypes(desc);
                Type[] newArgs = new Type[args.length - 1];
                System.arraycopy(args, 1, newArgs, 0, newArgs.length);
                desc = Type.getMethodDescriptor(Type.getReturnType(desc), newArgs);
            }
        }
        String origDesc = target.skippedDescriptor();

        // acquire the method name to match from the target, or construct
        // it from instMethod if one was not supplied.
        String name = target.name();
        if (name == null || name.length() == 0) {
            if (target.invocationType() == InvocationType.CONSTRUCTOR) {
                name = "<init>";
            } else {
                name = instMethod.getName();
            }
        }

        // grab the first method that matches under the following rules...
        //   1) Methods matching the target method annotation will be added
        //      as a target all way up the chain of overriding methods.
        //      This is because it is possible that this wrapper refers to
        //      a method that has been overridden in the respective class.
        //   2) If the current class is skipped, but we have a method wrapper
        //      that is the instrumented version of this method.  Then add this
        //      method as a target, and add a new method type for this.  We do
        //      this in the event that we wish to have an instrumented wrapper
        //      for some class that is skipped.
        boolean isSkipped = true;
        boolean methodFound = false;
        Stack<ClassType> parentStack = new Stack<ClassType>();
        parentStack.push(this);
        while (!methodFound && !parentStack.isEmpty()) {
            ClassType current = parentStack.pop();
            for (MethodType method : current.methods) {
                if (method.name.equals(name) && method.desc.equals(desc)) {
                    if (current != this) {
                        // create a dummy method so that instrumentation
                        // call lookup is easy.
                        MethodType dummyMethod = new MethodType(this, method.access, name, desc, method.signature,
                                method.exceptions);
                        isSkipped = dummyMethod.isSkipped = current.isSkipped;
                        results.add(dummyMethod);
                        methods.add(dummyMethod);
                    } else {
                        isSkipped = method.isSkipped;
                        results.add(method);
                    }
                    methodFound = true;
                    break;
                } else if (!origDesc.equals("") && current.isSkipped && current == this && method.name.equals(name)
                        && method.desc.equals(origDesc)) {
                    // add a psuedo-method to this type, so that it may be properly replaced
                    MethodType dummyMethod = new MethodType(this, method.access, name, desc, method.signature,
                            method.exceptions);
                    isSkipped = dummyMethod.isSkipped = current.isSkipped;
                    results.add(dummyMethod);
                    methods.add(dummyMethod);
                    methodFound = true;
                    break;
                }
            }
            for (ClassType parent : current.parents)
                parentStack.push(parent);
        }

        // if this is only a shallow search we are done.
        if (!target.canExtend())
            return results;

        if (children.isEmpty())
            return results;

        // continue by recursively call this on all child types...
        for (ClassType child : children) {
            for (MethodType method : child.getMethods(instMethod, target)) {
                method.isSkipped = isSkipped;
                results.add(method);
            }
        }
        return results;
    }
    
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.writeObject(target);
        stream.writeUTF(name);
        if (jar != null) {
            stream.writeBoolean(true);
            stream.writeUTF(jar);
        } else {
            stream.writeBoolean(false);
        }
        if (checksum != null) {
            stream.writeBoolean(true);
            stream.writeUTF(checksum);
        } else {
            stream.writeBoolean(false);
        }
        stream.writeBoolean(isClass);
        stream.writeBoolean(isCompatibility);
        stream.writeBoolean(inJdk);
        stream.writeBoolean(isSkipped);
        stream.writeBoolean(hasDefaultConstructor);
        stream.writeBoolean(hasDuplicatePrimField);
        stream.writeBoolean(hasMain);
        stream.writeInt(parents.size());
        for (ClassType ct : parents) {
            stream.writeObject(ct);
        }
        stream.writeInt(children.size());
        for (ClassType ct : children) {
            stream.writeObject(ct);
        }
        stream.writeInt(methods.size());
        for (MethodType mt : methods) {
            stream.writeObject(mt);
        }
        stream.writeInt(fields.size());
        for (FieldType ft : fields) {
            stream.writeObject(ft);
        }
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        target = (InstrumentationClass) stream.readObject();
        name = stream.readUTF();
        if (stream.readBoolean()) {
            jar = stream.readUTF();
        } else {
            jar = null;
        }
        if (stream.readBoolean()) {
            checksum = stream.readUTF();
        } else {
            checksum = null;
        }
        isClass = stream.readBoolean();
        isCompatibility = stream.readBoolean();
        inJdk = stream.readBoolean();
        isSkipped = stream.readBoolean();
        hasDefaultConstructor = stream.readBoolean();
        hasDuplicatePrimField = stream.readBoolean();
        hasMain = stream.readBoolean();
        parents = new TreeSet<ClassType>();
        int pSize = stream.readInt();
        for (int i = 0; i < pSize; i++) {
            parents.add((ClassType) stream.readObject());
        }
        children = new TreeSet<ClassType>();
        int cSize = stream.readInt();
        for (int i = 0; i < cSize; i++) {
            children.add((ClassType) stream.readObject());
        }
        methods = new TreeSet<MethodType>();
        int mSize = stream.readInt();
        for (int i = 0; i < mSize; i++) {
            methods.add((MethodType) stream.readObject());
        }
        fields = new TreeSet<FieldType>();
        int fSize = stream.readInt();
        for (int i = 0; i < fSize; i++) {
            fields.add((FieldType) stream.readObject());
        }
    }

    /**
     * Make sure classes come before interfaces, but this is otherwise sorted
     * alphabetically.
     */
    @Override
    public int compareTo(ClassType classType) {
        // classes should come before all interfaces.
        if (this.isClass && !classType.isClass)
            return -1;
        if (!this.isClass && classType.isClass)
            return 1;
        return name.compareTo(classType.name);
    }

    /**
     * Convert the class into XML form (useful for debugging).
     * 
     * @return String
     */
    public String toXML() {
        StringBuilder xml = new StringBuilder();
        xml.append("  <class");
        addXMLAttribute(xml, "name", name);
        if (!isClass)
            addXMLAttribute(xml, "interface", "true");
        if (isCompatibility)
            addXMLAttribute(xml, "compatibility", "true");
        if (inJdk)
            addXMLAttribute(xml, "jdk", "true");
        if (!isSkipped)
            addXMLAttribute(xml, "instrumented", "true");
        if (!hasDefaultConstructor)
            addXMLAttribute(xml, "default", "false");
        if (hasDuplicatePrimField)
            addXMLAttribute(xml, "duplicate", "true");
        if (hasMain)
            addXMLAttribute(xml, "main", "true");
        addXMLAttribute(xml, "checksum", checksum);
        xml.append(">\n");

        if (target != null) {
            xml.append("    <target");
            addXMLAttribute(xml, "class", target.value());
            addXMLAttribute(xml, "interface", "" + target.isInterface());
            xml.append("/>\n");
        }

        addXMLClasses(xml, "parents", parents);
        addXMLClasses(xml, "children", children);

        if (fields.isEmpty()) {
            xml.append("    <fields/>\n");
        } else {
            xml.append("    <fields>\n");
            for (FieldType fieldType : fields)
                xml.append(fieldType.toXML());
            xml.append("    </fields>\n");
        }

        if (methods.isEmpty()) {
            xml.append("    <methods/>\n");
        } else {
            xml.append("    <methods>\n");
            for (MethodType methodType : methods)
                xml.append(methodType.toXML());
            xml.append("    </methods>\n");
        }

        xml.append("  </class>\n");
        return xml.toString();
    }

    private void addXMLClasses(StringBuilder xml, String tagName, Set<ClassType> classes) {
        if (classes.isEmpty()) {
            xml.append("    <");
            xml.append(tagName);
            xml.append("/>\n");
            return;
        }

        xml.append("    <");
        xml.append(tagName);
        xml.append(">\n");
        for (ClassType classType : classes) {
            xml.append("      <class");
            addXMLAttribute(xml, "name", classType.name);
            xml.append("/>\n");
        }
        xml.append("    <");
        xml.append(tagName);
        xml.append("/>\n");
    }

    protected static void addXMLAttribute(StringBuilder xml, String name, String value) {
        xml.append(" ");
        xml.append(name);
        xml.append("=\"");
        xml.append(StringEscapeUtils.escapeXml(value));
        xml.append("\"");
    }
}
