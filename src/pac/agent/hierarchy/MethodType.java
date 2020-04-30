package pac.agent.hierarchy;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import pac.agent.CleartrackTaintMethodAdapter;
import pac.inst.InstrumentationMethod;
import pac.org.objectweb.asm.Type;
import pac.org.objectweb.asm.tree.MethodInsnNode;

/**
 * This class represents a method in our hierarchy.  This class is not thread-safe,
 * but the only place it really needs synchronization is in ClassHierarchy.process().
 * 
 * @author jeikenberry
 *
 */
public class MethodType implements Serializable, Comparable<MethodType> {
    private static final long serialVersionUID = 8043193816090796729L;

    // TODO Method objects are not serializable, so we cannot serialize this
    // field, which means that CleartrackInstrumentationAdapter is essentially
    // broken if used dynamically (i.e. as a java agent).
    private transient Map<Method, InstrumentationMethod> targets;

    public int access;
    public ClassType owner;
    public String name;
    public String desc;
    public String signature;
    public List<String> exceptions;
    protected boolean isSkipped, isRecursive;

    MethodType(ClassType owner, int access, String name, String desc, String signature, List<String> exceptions) {
        this.owner = owner;
        this.name = name;
        this.desc = desc;
        this.access = access;
        this.signature = signature;
        this.exceptions = exceptions;
    }

    public Map<Method, InstrumentationMethod> getTargets() {
        return targets;
    }

    /**
     * Checks whether this method overrides some parent method, or is
     * overridden by some child method.
     * 
     * @return boolean
     */
    public boolean isUnique() {
        for (ClassType parent : owner.parents) {
            if (parent.findMethod(name, desc) != null)
                return false;
        }
        for (ClassType child : owner.children) {
            if (child.findMethod(name, desc) != this)
                return false;
        }
        return true;
    }

    public String toString() {
        return owner + "." + name + desc;
    }

    @Override
    public int compareTo(MethodType mt) {
        return toString().compareTo(mt.toString());
    }

    public void addTarget(Method method, InstrumentationMethod target) {
        //	    Ansi.warn("map method '%s' to target '%s'", method, target);
        Method remove = null;
        // If the wrapper is already set, pick the leaf (i.e. child of the other)
        if (targets != null) {
            for (Method wrapperMethod : targets.keySet()) {
                if (overrides(wrapperMethod, method)) {
                    //				    Ansi.warn("1) method '%s' overrides '%s'", wrapperMethod, method);
                    // We already have the leaf, so do not add this target...
                    return;
                } else if (overrides(method, wrapperMethod)) {
                    //				    Ansi.warn("2) method '%s' overrides '%s'", method, wrapperMethod);
                    remove = wrapperMethod;
                    break;
                }
            }
        } else {
            // I'm not really sure if these will come in some order, since we are
            // at the whim of the reflections API.  So, to be safe, let's use an
            // ordered tree.
            targets = new TreeMap<Method, InstrumentationMethod>(new Comparator<Method>() {
                @Override
                public int compare(Method method1, Method method2) {
                    if (method1 == method2)
                        return 0;
                    if (method1 == null)
                        return -1;
                    if (method2 == null)
                        return 1;
                    if (method1.equals(method2))
                        return 0;
                    return method1.toString().compareTo(method2.toString());
                }
            });
        }
        if (remove != null)
            targets.remove(remove);
        targets.put(method, target);
    }

    public Method getTarget(MethodInsnNode method) {
        Entry<Method, InstrumentationMethod> entry = getTargetEntry(method);
        if (entry == null)
            return null;
        return entry.getKey();
    }

    public InstrumentationMethod getTargetAnnotation(MethodInsnNode method) {
        Entry<Method, InstrumentationMethod> entry = getTargetEntry(method);
        if (entry == null)
            return null;
        return entry.getValue();
    }

    private Entry<Method, InstrumentationMethod> getTargetEntry(MethodInsnNode method) {
        if (targets == null)
            return null;
        outerloop: for (Entry<Method, InstrumentationMethod> entry : targets.entrySet()) {
            // String owner =
            // Type.getType(target.getDeclaringClass()).getInternalName();
            // if (!owner.equals(method.owner))
            // continue;

            // skip over methods that have been instrumented...
            Type[] origTypes = Type.getArgumentTypes(method.desc);
            if (origTypes.length > 0) {
                Type lastArg = origTypes[origTypes.length - 1];
                if (lastArg.getSort() == Type.OBJECT && lastArg.getInternalName().equals("pac/util/Ret"))
                    continue;
            }

            Method target = entry.getKey();

            String newDesc = CleartrackTaintMethodAdapter.toPrimitiveDesc(method.desc);
            Type[] params = Type.getArgumentTypes(newDesc);
            Type[] testParams = Type.getArgumentTypes(target);
            if (params.length != testParams.length - 1)
                continue;
            for (int i = 0; i < params.length; i++) {
                if (!params[i].equals(testParams[i + 1]))
                    continue outerloop;
            }
            Type retType = Type.getReturnType(newDesc);
            Type testRetType = Type.getReturnType(target);
            if (!retType.equals(testRetType))
                continue;

            return entry;
        }
        return null;
    }

    private boolean overrides(Method childMethod, Method overriddenMethod) {
        if (!overriddenMethod.getName().equals(childMethod.getName()))
            return false; // names don't match...

        Class<?> parentClass = overriddenMethod.getDeclaringClass();
        Class<?> childClass = childMethod.getDeclaringClass();
        if (!parentClass.isAssignableFrom(childClass))
            return false; // is not overridden since the classes don't inherit

        Class<?>[] parentParams = overriddenMethod.getParameterTypes();
        Class<?>[] childParams = childMethod.getParameterTypes();
        if (parentParams.length != childParams.length)
            return false;

        // Start at the second index since the first represents the "this" object.
        for (int i = 1; i < parentParams.length; i++) {
            if (parentParams[i] != childParams[i])
                return false;
        }

        return true;
    }

    /**
     * Convert the method into XML form (useful for debugging).
     * 
     * @return String
     */
    public String toXML() {
        StringBuilder xml = new StringBuilder();
        xml.append("      <method");
        ClassType.addXMLAttribute(xml, "name", name);
        ClassType.addXMLAttribute(xml, "desc", desc);
        ClassType.addXMLAttribute(xml, "owner", owner.name);
        ClassType.addXMLAttribute(xml, "signature", signature);
        ClassType.addXMLAttribute(xml, "access", "" + access);
        if (exceptions != null && !exceptions.isEmpty()) {
            StringBuilder exStr = new StringBuilder();
            Iterator<String> exIter = exceptions.iterator();
            exStr.append(exIter.next());
            while (exIter.hasNext()) {
                exStr.append(",");
                exStr.append(exIter.next());
            }
            ClassType.addXMLAttribute(xml, "exceptions", exStr.toString());
        }
        if (!isSkipped)
            ClassType.addXMLAttribute(xml, "instrumented", "true");
        if (isRecursive)
            ClassType.addXMLAttribute(xml, "recursive", "true");
        if (targets == null || targets.isEmpty()) {
            xml.append("/>\n");
        } else {
            xml.append(">\n");
            xml.append("        <targets>\n");
            for (Entry<Method, InstrumentationMethod> target : targets.entrySet()) {
                xml.append("          <target");
                ClassType.addXMLAttribute(xml, "key", target.getKey().toString());
                xml.append(">\n");

                InstrumentationMethod instMethod = target.getValue();
                xml.append("            <name");
                ClassType.addXMLAttribute(xml, "value", instMethod.name());
                xml.append("/>\n");
                xml.append("            <descriptor");
                ClassType.addXMLAttribute(xml, "value", instMethod.descriptor());
                xml.append("/>\n");
                xml.append("            <type");
                ClassType.addXMLAttribute(xml, "value", instMethod.instrumentationType().toString());
                xml.append("/>\n");
                xml.append("            <extends");
                ClassType.addXMLAttribute(xml, "value", "" + instMethod.canExtend());
                xml.append("/>\n");
                xml.append("            <location");
                ClassType.addXMLAttribute(xml, "value", instMethod.instrumentationLocation().toString());
                xml.append("/>\n");
                xml.append("            <invocation");
                ClassType.addXMLAttribute(xml, "value", instMethod.invocationType().toString());
                xml.append("/>\n");

                xml.append("          </target>\n");
            }
            xml.append("        </targets>\n");
            xml.append("      </methods>\n");
        }
        return xml.toString();
    }
}
