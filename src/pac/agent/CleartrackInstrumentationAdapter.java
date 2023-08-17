package pac.agent;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import pac.agent.hierarchy.ClassHierarchy;
import pac.agent.hierarchy.MethodType;
import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.inst.InstrumentationType;
import pac.org.objectweb.asm.ClassVisitor;
import pac.org.objectweb.asm.Opcodes;
import pac.org.objectweb.asm.Type;
import pac.org.objectweb.asm.tree.AbstractInsnNode;
import pac.org.objectweb.asm.tree.AnnotationNode;
import pac.org.objectweb.asm.tree.ClassNode;
import pac.org.objectweb.asm.tree.IincInsnNode;
import pac.org.objectweb.asm.tree.InsnList;
import pac.org.objectweb.asm.tree.InsnNode;
import pac.org.objectweb.asm.tree.JumpInsnNode;
import pac.org.objectweb.asm.tree.LabelNode;
import pac.org.objectweb.asm.tree.MethodInsnNode;
import pac.org.objectweb.asm.tree.MethodNode;
import pac.org.objectweb.asm.tree.TypeInsnNode;
import pac.org.objectweb.asm.tree.VarInsnNode;
import pac.util.Ansi;
import pac.util.AsmUtils;

public class CleartrackInstrumentationAdapter extends ClassNode {
  public static final ClassHierarchy hierarchy = ClassHierarchy.getInstance();

  /** The next ClassVisitor in the class transformation pipeline. */
  private ClassVisitor cv;

  public CleartrackInstrumentationAdapter(ClassVisitor cv) {
    super(Opcodes.ASM5);
    this.cv = cv;
  }

  /**
   * This is the last method called by ClassReader when directing a ClassVisitor (of which this is a
   * subclass) through the visitation of some bytecode. It can be taken as a signal that the tree
   * model of the class in question has been constructed and is ready for processing.
   */
  @Override
  public void visitEnd() {
    // Do not reinstrument if the class has already been instrumented.
    if (this.invisibleAnnotations != null) {
      for (AnnotationNode annotation : this.invisibleAnnotations) {
        if (annotation.desc.equals("Lpac/inst/InstrumentedClass;")) {
          @SuppressWarnings("unchecked")
          List<String> options = (List<String>) annotation.values.get(3);
          for (String option : options) {
            if (option.equals("CONFINEMENT")) {
              accept(cv);
              return;
            }
          }
        }
      }
    }

    boolean inJdk = hierarchy.isJdkClass(this.name);
    boolean isCompatibility = hierarchy.isCompatibilityClass(this.name);

    // We use this set to mark which methods that we have inlined into the class, but not inlined
    // directly into the method. In other words, for the cases where we directly copy the inlined
    // method into the class. We need to mark these to ensure that we do not have duplicate methods.
    Set<String> inlinedMethods = new HashSet<String>();

    // Perform our instrumentation processing each method in this class.
    ListIterator<MethodNode> methodIter = this.methods.listIterator();
    while (methodIter.hasNext()) {
      MethodNode methodNode = methodIter.next();

      // We should method swap on all methods in compatibility classes.
      boolean isInstrumented = isInstrumentedMethod(methodNode);

      // Scan through all the opcodes in this method and perform modifications.
      AbstractInsnNode currentNode = methodNode.instructions.getFirst();

      while (null != currentNode) {
        if (currentNode instanceof MethodInsnNode) {

          // Do not insert or replace instrumented calls on super calls.
          MethodInsnNode methodInsnNode = (MethodInsnNode) currentNode;
          if (methodInsnNode.getOpcode() == Opcodes.INVOKESPECIAL
              && !methodInsnNode.name.equals("<init>")) {
            currentNode = currentNode.getNext();
            continue;
          }

          // Perform different behaviors depending on the type of the invocation.
          MethodType method = hierarchy.getMethod(methodInsnNode);

          if (null != method) {
            Map<Method, InstrumentationMethod> targets = method.getTargets();
            if (null != targets) {
              for (Entry<Method, InstrumentationMethod> entry : targets.entrySet()) {
                InstrumentationMethod target = entry.getValue();
                Method wrapper = entry.getKey();
                if (target == null || wrapper == null)
                  continue;

                // Determine whether we should skip targets depending on the instrumentation
                // level.
                switch (target.instrumentationLocation()) {
                  case JDK:
                    if (!inJdk || !isInstrumented)
                      continue;
                    break;
                  case APP:
                    if (inJdk || !isInstrumented)
                      continue;
                    break;
                  case ALL:
                    if (!isInstrumented)
                      continue;
                    break;
                  case TRANS:
                    if (inJdk && !isCompatibility)
                      continue;
                    break;
                  case COMPAT:
                }

                Class<?> declaringClass = wrapper.getDeclaringClass();
                String owner = Type.getInternalName(declaringClass);
                String name = wrapper.getName();
                String desc = Type.getMethodDescriptor(wrapper);

                boolean inline = target.inline();
                if (inline) {
                  // We are inlining this method, so we need to determine what the owner class is by
                  // looking at the InstrumentationClass annotation of the declaring type.
                  String key = owner + "." + name + desc;
                  InstrumentationClass instClass =
                      declaringClass.getAnnotation(InstrumentationClass.class);
                  if (instClass == null) {
                    Ansi.warn("failed to inline the wrapper method %s.%s%s, since an "
                        + "@InstrumentationClass annotation could not be found on the "
                        + "declaring wrapper class.", this.name, owner, name, desc);
                  } else {
                    // Check to see if we've already added an inlined method to this class.
                    if (!inlinedMethods.contains(key)) {
                      // Create an inlined method for this method invocation.
                      MethodNode inlinedMethod = CleartrackMethodInliner
                          .createInlinedMethod(instClass.value(), methodInsnNode);

                      // Check to see if an inlined method could be created or not.
                      if (inlinedMethod == null) {
                        Ansi.warn("failed to inline the wrapper method %s.%s%s", this.name, owner,
                            name, desc);
                      } else {
                        // We cannot actually inline a method directly into some calling method, if
                        // the method to inline has try-catch blocks. We do not really know where to
                        // add the inlined catch block for these cases.
                        if (!inlinedMethod.exceptions.isEmpty()) {
                          methodIter.add(inlinedMethod);
                          inlinedMethods.add(key);
                          owner = this.name;
                          name = CleartrackMethodInliner.getInlinedMethodName(instClass.value(),
                              methodInsnNode);
                        } else {
                          // Inline the method directly into the calling method.
                          currentNode = inlineMethod(methodNode, currentNode, inlinedMethod,
                              target.instrumentationType());
                          continue;
                        }
                      }
                    } else {
                      // There is already a method definition for the inlined method in this class
                      // node, so just update the MethodInsnNode instruction to invoke this method.
                      owner = this.name;
                      name = CleartrackMethodInliner.getInlinedMethodName(instClass.value(),
                          methodInsnNode);
                    }
                  }
                }

                switch (target.instrumentationType()) {
                  case REPLACE:
                    // If we are replacing this constructor call with our instrumented version, we
                    // have to remove any potential DUP instructions.
                    boolean shouldInstrument = true;
                    if (methodInsnNode.name.equals("<init>")) {
                      shouldInstrument = removeRelevantDups(methodNode, methodInsnNode);
                      /*
                       * TODO: We should address what we should do with super() calls.
                       */
                    }

                    if (shouldInstrument) {
                      methodInsnNode.setOpcode(Opcodes.INVOKESTATIC);
                      methodInsnNode.owner = owner;
                      methodInsnNode.name = name;
                      methodInsnNode.desc = desc;
                      methodInsnNode.itf = false;
                    }
                    break;
                  case INSERT_BEFORE:
                    MethodInsnNode insertedCallInsn =
                        new MethodInsnNode(Opcodes.INVOKESTATIC, owner, name, desc, false);
                    methodNode.instructions.insertBefore(methodInsnNode, insertedCallInsn);
                    break;
                  case INSERT_AFTER:
                    insertedCallInsn =
                        new MethodInsnNode(Opcodes.INVOKESTATIC, owner, name, desc, false);
                    methodNode.instructions.insert(methodInsnNode, insertedCallInsn);
                    break;
                }
              }
            }
          }
        }

        currentNode = currentNode.getNext();
      }
    }

    accept(cv);
  }

  /**
   * Called to inline inlinedMethod into methodNode, replacing methodInsnNode with these
   * instructions. All returns are essentially converted into GOTO instructions that branch to the
   * end of the inlinedMethod. Note, that this should not be called if the method you are inlining
   * contains try-catch blocks.
   * 
   * @param methodNode MethodNode to inline into.
   * @param methodInsnNode AbstractInsnNode of the method invocation to be inlined.
   * @param inlinedMethod MethodNode that we are inlining.
   * @param instType InstrumentationType of this inline operation. This will determine what to do
   *        with methodInsnNode, and how the instructions will be inlined.
   * @return LabelNode marking the end of the inlined method.
   */
  private LabelNode inlineMethod(MethodNode methodNode, AbstractInsnNode methodInsnNode,
      MethodNode inlinedMethod, InstrumentationType instType) {
    int startReg = findStartReg(methodNode);
    int maxLocals = startReg;
    InsnList instructions = methodNode.instructions;
    LabelNode endInline = new LabelNode();

    // Construct a label map mapping inlined method labels to new labels. This will be needed when
    // we clone the inlined instructions.
    Map<LabelNode, LabelNode> labelMap = new HashMap<LabelNode, LabelNode>();
    for (AbstractInsnNode curNode =
        inlinedMethod.instructions.getFirst(); curNode != null; curNode = curNode.getNext()) {
      if (curNode instanceof LabelNode)
        labelMap.put((LabelNode) curNode, new LabelNode());
    }

    // Process the method body of the inlined instructions (these need to be cloned). We also need
    // to offset all variables by maxLocals so that there are no collisions. Returns will turn into
    // a GOTO that jumps to a location after the method body.
    InsnList methodBody = new InsnList();
    for (AbstractInsnNode curNode =
        inlinedMethod.instructions.getFirst(); curNode != null; curNode = curNode.getNext()) {
      switch (curNode.getOpcode()) {
        case Opcodes.RETURN:
        case Opcodes.ARETURN:
        case Opcodes.IRETURN:
        case Opcodes.FRETURN:
        case Opcodes.LRETURN:
        case Opcodes.DRETURN:
          methodBody.add(new JumpInsnNode(Opcodes.GOTO, endInline));
          break;
        default:
          AbstractInsnNode clonedNode =
              curNode instanceof LabelNode ? labelMap.get((LabelNode) curNode)
                  : curNode.clone(labelMap);
          if (clonedNode instanceof VarInsnNode) {
            VarInsnNode varInsnNode = (VarInsnNode) clonedNode;
            varInsnNode.var += startReg;
            maxLocals = Math.max(varInsnNode.var, maxLocals);
          } else if (clonedNode instanceof IincInsnNode) {
            IincInsnNode varInsnNode = (IincInsnNode) clonedNode;
            varInsnNode.var += startReg;
            maxLocals = Math.max(varInsnNode.var, maxLocals);
          }
          methodBody.add(clonedNode);
          break;
      }
    }

    // Append the instructions, remove the method invocation, and update maxLocals.
    instructions.insert(methodInsnNode, endInline);
    instructions.insert(methodInsnNode, methodBody);

    // Add store instructions storing all of the parameter types into new local registers.
    Type[] argTypes = Type.getArgumentTypes(inlinedMethod.desc);
    int maxParams = startReg;
    for (int i = 0; i < argTypes.length; i++) {
      switch (argTypes[i].getSort()) {
        case Type.OBJECT:
        case Type.ARRAY:
          instructions.insert(methodInsnNode, new VarInsnNode(Opcodes.ASTORE, maxParams));
          break;
        case Type.FLOAT:
          instructions.insert(methodInsnNode, new VarInsnNode(Opcodes.FSTORE, maxParams));
          break;
        case Type.LONG:
          instructions.insert(methodInsnNode, new VarInsnNode(Opcodes.LSTORE, maxParams));
          break;
        case Type.DOUBLE:
          instructions.insert(methodInsnNode, new VarInsnNode(Opcodes.DSTORE, maxParams));
          break;
        default:
          instructions.insert(methodInsnNode, new VarInsnNode(Opcodes.ISTORE, maxParams));
          break;
      }
      maxParams += argTypes[i].getSize();
    }

    instructions.remove(methodInsnNode);
    methodNode.maxLocals = Math.max(maxParams, maxLocals) + 1;
    return endInline;
  }

  /**
   * Determines the first free register of methodNode. We can't rely on maxLocals being up to date,
   * so we have to calculate this each time.
   * 
   * @param methodNode MethodNode
   * @return int of the first free register of methodNode.
   */
  private int findStartReg(MethodNode methodNode) {
    int startReg = 0;
    if (!AsmUtils.hasModifiers(methodNode.access, Opcodes.ACC_STATIC))
      startReg = 1;

    // Account for all of the method parameters.
    Type[] argTypes = Type.getArgumentTypes(methodNode.desc);
    for (int i = 0; i < argTypes.length; i++) {
      startReg += argTypes[i].getSize();
    }

    // Read in all of the instructions, and ensure that we update startReg such that it's big enough
    // to not overlap with any existing VarInsnNode or IincInsnNode instruction.
    for (AbstractInsnNode curNode = methodNode.instructions.getFirst(); curNode != null; curNode =
        curNode.getNext()) {
      if (curNode instanceof VarInsnNode) {
        VarInsnNode varInsnNode = (VarInsnNode) curNode;
        switch (curNode.getOpcode()) {
          case Opcodes.DLOAD:
          case Opcodes.LLOAD:
          case Opcodes.DSTORE:
          case Opcodes.LSTORE:
            startReg = Math.max(startReg, varInsnNode.var + 2);
            break;
          default:
            startReg = Math.max(startReg, varInsnNode.var + 1);
            break;
        }
      } else if (curNode instanceof IincInsnNode) {
        IincInsnNode varInsnNode = (IincInsnNode) curNode;
        startReg = Math.max(startReg, varInsnNode.var + 1);
      }
    }

    return startReg;
  }

  private boolean isInstrumentedMethod(MethodNode methodNode) {
    if (methodNode.name.equals("<clinit>"))
      return !hierarchy.isSkipped(this, methodNode);
    Type[] argTypes = Type.getArgumentTypes(methodNode.desc);
    if (argTypes.length == 0)
      return false;
    Type lastType = argTypes[argTypes.length - 1];
    if (lastType.getSort() == Type.OBJECT && lastType.getInternalName().equals("pac/util/Ret"))
      return true;
    // Check if it's an instrumented recursive method. These methods will have an additional count
    // parameter at the end, after the Ret parameter.
    if (argTypes.length <= 1)
      return false;
    Type secondToLastType = argTypes[argTypes.length - 2];
    if (secondToLastType.getSort() != Type.OBJECT)
      return false;
    return secondToLastType.getInternalName().equals("pac/util/Ret");
  }

  /*
   * FIXME: this is an imperfect way of handling DUP instructions. Ideally a verifier would correct
   * for these DUP instructions by analyzing the stack.
   */
  private boolean removeRelevantDups(MethodNode methodNode, MethodInsnNode methodInsnNode) {
    AbstractInsnNode startNode = methodInsnNode;
    while (startNode != null) {
      if ((startNode instanceof TypeInsnNode) && startNode.getOpcode() == Opcodes.NEW) {
        if (methodInsnNode.owner.equals(((TypeInsnNode) startNode).desc))
          break;
      }
      startNode = startNode.getPrevious();
    }

    if (startNode == null) // Should only happen on super() or this() calls.
      return false;

    // Find the label node associated with the new type.
    AbstractInsnNode startLabelNode = startNode;
    while (startLabelNode != null && !(startLabelNode instanceof LabelNode)) {
      startLabelNode = startLabelNode.getPrevious();
    }

    /*
     * Remove instructions of the form: NEW class DUP
     * 
     * If there is a gap between DUP and NEW instructions, then insert pop after instrumented
     * constructor.
     */
    AbstractInsnNode nextNode = startNode.getNext();
    methodNode.instructions.remove(startNode);
    if (nextNode.getOpcode() == Opcodes.DUP) {
      methodNode.instructions.remove(nextNode);
    } else { // Assume that the NEW type does not get DUPed for now this should be fixed.
      methodNode.instructions.insert(methodInsnNode, new InsnNode(Opcodes.POP));
    }

    return true;
  }
}
