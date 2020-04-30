package pac.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import pac.org.objectweb.asm.ClassReader;
import pac.org.objectweb.asm.Opcodes;
import pac.org.objectweb.asm.Type;
import pac.org.objectweb.asm.tree.AnnotationNode;
import pac.org.objectweb.asm.tree.ClassNode;
import pac.org.objectweb.asm.tree.MethodInsnNode;
import pac.org.objectweb.asm.tree.MethodNode;

/**
 * Dynamic-agent used to inline wrapped methods annotated with the
 * InstrumentationMethod annotation (when inline is set to true).
 * This class is necessary for reflection to work properly without
 * having to change field/method modifiers.
 * 
 * @author jeikenberry
 */
public class CleartrackMethodInliner extends ClassNode {
    // These class nodes can be accessed concurrently
    private static final Map<String, MethodNode> methodsToInline =
            new ConcurrentHashMap<String, MethodNode>();

    public CleartrackMethodInliner() {
        super(Opcodes.ASM5);
    }

    @Override
    public void visitEnd() {
        String instOwner = null;

        if (this.visibleAnnotations != null) {
            outer: for (AnnotationNode annotation : this.visibleAnnotations) {
                if (annotation.desc.equals("Lpac/inst/InstrumentationClass;")) {
                    if (annotation.values == null)
                        continue;
                    for (int i = 0; i < annotation.values.size(); i++) {
                        String fieldName = (String) annotation.values.get(i++);
                        // Find the the owner of the method that is wrapped
                        if (fieldName.equals("value")) {
                            instOwner = annotation.values.get(i).toString();
                            break outer;
                        }
                    }
                }
            }
        }

        if (instOwner == null) {
            return;
        }

        for (MethodNode methodNode : this.methods) {
            if (methodNode.visibleAnnotations == null)
                continue;
            for (AnnotationNode annotation : methodNode.visibleAnnotations) {
                if (annotation.values == null)
                    continue;
                if (annotation.desc.equals("Lpac/inst/InstrumentationMethod;")) {
                    boolean inlining = false;
                    boolean explicitDesc = false;
                    boolean isStatic = false;
                    boolean isConstructor = false;
                    String methodName = methodNode.name;
                    String methodDesc = methodNode.desc;
                    for (int i = 0; i < annotation.values.size(); i++) {
                        String fieldName = (String) annotation.values.get(i++);
                        // Only add this method node if we are actually inlining it.
                        if (fieldName.equals("inline")) {
                            inlining = (Boolean) annotation.values.get(i);
                        } else if (fieldName.equals("name")) {
                            methodName = annotation.values.get(i).toString();
                        } else if (fieldName.equals("descriptor")) {
                            methodDesc = annotation.values.get(i).toString();
                            explicitDesc = true;
                        } else if (fieldName.equals("invocationType")) {
                            String[] enumVal = (String[]) annotation.values.get(i);
                            isStatic = enumVal[1].equals("STATIC");
                            isConstructor = enumVal[1].equals("CONSTRUCTOR");
                        }
                    }

                    if (inlining) {
                        if (!explicitDesc && (!isStatic || isConstructor)) {
                            StringBuilder buf = new StringBuilder("(");
                            Type[] argTypes = Type.getArgumentTypes(methodDesc);
                            for (int i = isStatic ? 0 : 1; i < argTypes.length; i++) {
                                buf.append(argTypes[i].getDescriptor());
                            }
                            buf.append(")");
                            if (isConstructor)
                                buf.append("V");
                            else
                                buf.append(Type.getReturnType(methodDesc).getDescriptor());
                            methodDesc = buf.toString();
                        }

                        methodsToInline.put(instOwner + "." + methodName + methodDesc, methodNode);
                    }
                    break;
                }
            }
        }
    }

    /**
     * Clones the MethodNode represented by methodCall and returns the cloned method.
     * We need to operate on the clone, since there may be many invocations of a single
     * MethodNode.
     * 
     * @param actualOwner String of the internal name of the owner.
     * @param methodCall MethodInsnNode of the method invocation.
     * @return MethodNode of the inlined method copy.
     */
    public static MethodNode createInlinedMethod(String actualOwner, MethodInsnNode methodCall) {
        String key = actualOwner + "." + methodCall.name + methodCall.desc;
        MethodNode methodToInline = methodsToInline.get(key);
        if (methodToInline == null)
            return null;

        // Copy the method to inline and insert it into the method iterator.
        MethodNode inlinedMethod = new MethodNode(Opcodes.ASM5, methodToInline.access,
                getInlinedMethodName(actualOwner, methodCall), methodToInline.desc, methodToInline.signature,
                methodToInline.exceptions == null ? null : methodToInline.exceptions.toArray(new String[0]));
        synchronized (methodToInline) {
            // Since multiple copies can be made, make sure we reset the labels
            // such that stackmaps can be correctly calculated.
            methodToInline.instructions.resetLabels();
            methodToInline.accept(inlinedMethod);
        }

        return inlinedMethod;
    }

    /**
     * Obtain the name of the inline method.
     * 
     * @param actualOwner String of the internal name of the owner.
     * @param methodCall MethodInsnNode of the method invocation.
     * @return String of the inlined method name.
     */
    public static String getInlinedMethodName(String actualOwner, MethodInsnNode methodCall) {
        return actualOwner.replace('/', '_').replaceAll("\\$", "__") + "__" + methodCall.name + "_inlined";
    }

    public static void premain(String args, Instrumentation inst) {
        inst.addTransformer(new MethodInlineTransformer(), false);
    }

    /**
     * Transformer for processing inlined methods.
     * 
     * @author jeikenberry
     */
    private static class MethodInlineTransformer implements ClassFileTransformer {

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain, byte[] classfileBuffer)
                throws IllegalClassFormatException {
            if (className.startsWith("pac/inst/")) {
                ClassReader cr = new ClassReader(classfileBuffer);
                CleartrackMethodInliner visitor = new CleartrackMethodInliner();
                cr.accept(visitor, 0);
            }
            return classfileBuffer;
        }

    }
}
