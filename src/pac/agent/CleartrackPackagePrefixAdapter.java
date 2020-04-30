package pac.agent;

import java.util.LinkedList;
import java.util.List;

import pac.agent.hierarchy.ClassHierarchy;
import pac.org.objectweb.asm.ClassVisitor;
import pac.org.objectweb.asm.Opcodes;
import pac.org.objectweb.asm.Type;
import pac.org.objectweb.asm.signature.SignatureReader;
import pac.org.objectweb.asm.signature.SignatureWriter;
import pac.org.objectweb.asm.tree.AbstractInsnNode;
import pac.org.objectweb.asm.tree.AnnotationNode;
import pac.org.objectweb.asm.tree.ClassNode;
import pac.org.objectweb.asm.tree.FieldInsnNode;
import pac.org.objectweb.asm.tree.FieldNode;
import pac.org.objectweb.asm.tree.FrameNode;
import pac.org.objectweb.asm.tree.InnerClassNode;
import pac.org.objectweb.asm.tree.LdcInsnNode;
import pac.org.objectweb.asm.tree.LocalVariableNode;
import pac.org.objectweb.asm.tree.MethodInsnNode;
import pac.org.objectweb.asm.tree.MethodNode;
import pac.org.objectweb.asm.tree.MultiANewArrayInsnNode;
import pac.org.objectweb.asm.tree.TryCatchBlockNode;
import pac.org.objectweb.asm.tree.TypeInsnNode;

/**
 * Use this class to generate a new class with the "pac" added to the package
 * prefix of each class.  This is needed to ensure that application libraries
 * don't conflict with instrumentation libraries.
 * 
 * @author jeikenberry
 */
public class CleartrackPackagePrefixAdapter extends ClassNode {
    public static final ClassHierarchy hierarchy = ClassHierarchy.getInstance();
    public static final String PACKAGE_PREFIX = "pac/";

    private ClassVisitor cv;

    public CleartrackPackagePrefixAdapter(ClassVisitor cv) {
        super(Opcodes.ASM5);
        this.cv = cv;
    }

    @Override
    public void visitEnd() {
        if (!hierarchy.isJdkClass(this.name)) {
            this.name = prefixType(this.name);
            this.outerClass = prefixType(this.outerClass);
            this.superName = prefixType(this.superName);
            this.signature = prefixSignatureType(this.signature);
            for (InnerClassNode innerClass : this.innerClasses) {
                innerClass.name = prefixType(innerClass.name);
                innerClass.outerName = prefixType(innerClass.outerName);
            }

            List<String> newInterfaces = new LinkedList<String>();
            for (String iface : this.interfaces) {
                newInterfaces.add(prefixType(iface));
            }
            this.interfaces = newInterfaces;

            if (this.invisibleAnnotations != null) {
                for (AnnotationNode annotation : this.invisibleAnnotations) {
                    annotation.desc = prefixType(annotation.desc);
                }
            }

            if (this.visibleAnnotations != null) {
                for (AnnotationNode annotation : this.visibleAnnotations) {
                    annotation.desc = prefixType(annotation.desc);
                }
            }

            this.outerMethodDesc = prefixMethod(this.outerMethodDesc);

            for (FieldNode fieldNode : this.fields) {
                prefix(fieldNode);
            }

            for (MethodNode methodNode : this.methods) {
                prefix(methodNode);
            }
        }
        accept(cv);
    }

    public void prefix(FieldNode fieldNode) {
        fieldNode.desc = prefixType(fieldNode.desc);
        fieldNode.signature = prefixSignatureType(fieldNode.signature);

        if (fieldNode.invisibleAnnotations != null) {
            for (AnnotationNode annotation : fieldNode.invisibleAnnotations) {
                annotation.desc = prefixType(annotation.desc);
            }
        }

        if (fieldNode.visibleAnnotations != null) {
            for (AnnotationNode annotation : fieldNode.visibleAnnotations) {
                annotation.desc = prefixType(annotation.desc);
            }
        }
    }

    public void prefix(MethodNode methodNode) {
        methodNode.desc = prefixMethod(methodNode.desc);
        methodNode.signature = prefixSignatureType(methodNode.signature);

        List<String> newExceptions = new LinkedList<String>();
        for (String className : methodNode.exceptions) {
            newExceptions.add(prefixType(className));
        }
        methodNode.exceptions = newExceptions;

        if (methodNode.invisibleParameterAnnotations != null) {
            for (List<AnnotationNode> annotations : methodNode.invisibleParameterAnnotations) {
                if (annotations == null)
                    continue;
                for (AnnotationNode annotation : annotations) {
                    annotation.desc = prefixType(annotation.desc);
                }
            }
        }

        if (methodNode.visibleParameterAnnotations != null) {
            for (List<AnnotationNode> annotations : methodNode.visibleParameterAnnotations) {
                if (annotations == null)
                    continue;
                for (AnnotationNode annotation : annotations) {
                    annotation.desc = prefixType(annotation.desc);
                }
            }
        }

        if (methodNode.invisibleAnnotations != null) {
            for (AnnotationNode annotation : methodNode.invisibleAnnotations) {
                annotation.desc = prefixType(annotation.desc);
            }
        }

        if (methodNode.visibleAnnotations != null) {
            for (AnnotationNode annotation : methodNode.visibleAnnotations) {
                annotation.desc = prefixType(annotation.desc);
            }
        }

        if (methodNode.localVariables != null) {
            for (LocalVariableNode localVar : methodNode.localVariables) {
                localVar.desc = prefixType(localVar.desc);
                localVar.signature = prefixType(localVar.signature);
            }
        }

        if (methodNode.tryCatchBlocks != null) {
            for (TryCatchBlockNode tryCatch : methodNode.tryCatchBlocks) {
                tryCatch.type = prefixType(tryCatch.type);
            }
        }

        AbstractInsnNode currentNode = methodNode.instructions.getFirst();
        while (currentNode != null) {
            if (currentNode instanceof FieldInsnNode) {
                FieldInsnNode fieldInsnNode = (FieldInsnNode) currentNode;
                fieldInsnNode.owner = prefixType(fieldInsnNode.owner);
                fieldInsnNode.desc = prefixType(fieldInsnNode.desc);
            } else if (currentNode instanceof MethodInsnNode) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) currentNode;
                methodInsnNode.owner = prefixType(methodInsnNode.owner);
                methodInsnNode.desc = prefixMethod(methodInsnNode.desc);
            } else if (currentNode instanceof TypeInsnNode) {
                TypeInsnNode typeInsnNode = (TypeInsnNode) currentNode;
                typeInsnNode.desc = prefixType(typeInsnNode.desc);
            } else if (currentNode instanceof MultiANewArrayInsnNode) {
                MultiANewArrayInsnNode multiANewArrayInsnNode = (MultiANewArrayInsnNode) currentNode;
                multiANewArrayInsnNode.desc = prefixType(multiANewArrayInsnNode.desc);
            } else if (currentNode instanceof LdcInsnNode) {
                LdcInsnNode ldcInsnNode = (LdcInsnNode) currentNode;
                if (ldcInsnNode.cst instanceof Type) {
                    ldcInsnNode.cst = Type.getType(prefixType(ldcInsnNode.cst.toString()));
                }
            } else if (currentNode instanceof FrameNode) {
                FrameNode frameNode = (FrameNode) currentNode;
                if (frameNode.stack != null) {
                    List<Object> newStack = new LinkedList<Object>();
                    for (Object obj : frameNode.stack) {
                        if (obj instanceof String) {
                            newStack.add(prefixType((String) obj));
                        } else {
                            newStack.add(obj);
                        }
                    }
                    frameNode.stack = newStack;
                }
                if (frameNode.local != null) {
                    List<Object> newStack = new LinkedList<Object>();
                    for (Object obj : frameNode.local) {
                        if (obj instanceof String) {
                            newStack.add(prefixType((String) obj));
                        } else {
                            newStack.add(obj);
                        }
                    }
                    frameNode.local = newStack;
                }
            }
            currentNode = currentNode.getNext();
        }
    }

    public String prefixSignatureType(String signature) {
        if (signature == null)
            return null;
        SignatureReader sigReader = new SignatureReader(signature);
        PrefixSignatureWriter sigWriter = new PrefixSignatureWriter();
        sigReader.accept(sigWriter);
        return sigWriter.toString();
    }

    public String prefixMethod(String methodDesc) {
        if (methodDesc == null)
            return null;
        Type args[] = Type.getArgumentTypes(methodDesc);
        StringBuilder newDesc = new StringBuilder("(");
        for (int i = 0; i < args.length; i++) {
            newDesc.append(prefixType(args[i].toString()));
        }
        newDesc.append(')');
        newDesc.append(prefixType(Type.getReturnType(methodDesc).toString()));
        return newDesc.toString();
    }

    public String prefixType(String desc) {
        if (desc == null)
            return null;
        if (desc.length() <= 1)
            return desc;
        if (desc.endsWith(";") || desc.startsWith("[")) {
            Type type = Type.getType(desc);
            String internalName;
            int arrayDim = 0;
            if (type.getSort() == Type.ARRAY) {
                Type elemType = type.getElementType();
                if (elemType.getSort() != Type.OBJECT)
                    return desc;
                arrayDim = type.getDimensions();
                internalName = elemType.getInternalName();
            } else if (type.getSort() == Type.OBJECT) {
                internalName = type.getInternalName();
            } else
                return desc;

            if (hierarchy.isJdkClass(internalName))
                return desc;

            StringBuilder newDesc = new StringBuilder();
            for (int i = 0; i < arrayDim; i++)
                newDesc.append('[');
            newDesc.append('L');
            newDesc.append(PACKAGE_PREFIX);
            newDesc.append(internalName);
            newDesc.append(';');
            return newDesc.toString();
        } else {
            if (!hierarchy.isJdkClass(desc))
                return PACKAGE_PREFIX + desc;
            return desc;
        }
    }

    class PrefixSignatureWriter extends SignatureWriter {

        @Override
        public void visitClassType(final String name) {
            super.visitClassType(prefixType(name));
        }
        
    }
}
