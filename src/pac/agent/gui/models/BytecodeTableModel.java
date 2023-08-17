package pac.agent.gui.models;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

import pac.agent.CleartrackVerifier;
import pac.agent.gui.LabelChangeEvent;
import pac.agent.gui.LabelChangeListener;
import pac.agent.gui.editors.OpcodeCellEditor.Opcode;
import pac.agent.gui.renderers.InstructionCellRenderer;
import pac.org.apache.commons.lang.StringEscapeUtils;
import pac.org.objectweb.asm.Opcodes;
import pac.org.objectweb.asm.Type;
import pac.org.objectweb.asm.tree.AbstractInsnNode;
import pac.org.objectweb.asm.tree.ClassNode;
import pac.org.objectweb.asm.tree.FieldInsnNode;
import pac.org.objectweb.asm.tree.IincInsnNode;
import pac.org.objectweb.asm.tree.InsnNode;
import pac.org.objectweb.asm.tree.IntInsnNode;
import pac.org.objectweb.asm.tree.JumpInsnNode;
import pac.org.objectweb.asm.tree.LabelNode;
import pac.org.objectweb.asm.tree.LdcInsnNode;
import pac.org.objectweb.asm.tree.LocalVariableNode;
import pac.org.objectweb.asm.tree.LookupSwitchInsnNode;
import pac.org.objectweb.asm.tree.MethodInsnNode;
import pac.org.objectweb.asm.tree.MethodNode;
import pac.org.objectweb.asm.tree.MultiANewArrayInsnNode;
import pac.org.objectweb.asm.tree.TableSwitchInsnNode;
import pac.org.objectweb.asm.tree.TryCatchBlockNode;
import pac.org.objectweb.asm.tree.TypeInsnNode;
import pac.org.objectweb.asm.tree.VarInsnNode;
import pac.org.objectweb.asm.tree.analysis.Analyzer;
import pac.org.objectweb.asm.tree.analysis.AnalyzerException;
import pac.org.objectweb.asm.tree.analysis.BasicValue;
import pac.org.objectweb.asm.tree.analysis.Frame;
import pac.util.AsmUtils;
import pac.util.OS;

//#if issubstr("Mac", os_type)
import com.apple.eawt.Application;
//#endif

/**
 * The main model for manipulating MethodNode objects. This maintains information about the
 * instructions (excluding instructions whose opcode is < 0). It will also verify on each update to
 * the model, as well as maintain an ordering of the branches contained within the method.
 * 
 * @author jeikenberry
 */
public class BytecodeTableModel extends AbstractTableModel {
  private static final long serialVersionUID = -2624096337145843822L;

  /**
   * The analyzer object for both verifying and producing stack frame information.
   */
  private Analyzer<BasicValue> analyzer;

  /** The current MethodNode object. */
  private MethodNode methodNode;

  /**
   * Mapping of all LabelNode objects to the first "real" instruction (i.e. instruction whose opcode
   * is > 0).
   */
  private Map<LabelNode, AbstractInsnNode> labelToInsnMap;

  /**
   * If there are multiple labels in a row without actual instructions, they will be hidden from the
   * user. But, if there happens to be a jump to this hidden label, then we must obtain the one that
   * is visible. This hashmap maps hidden nodes to the one that is visible.
   */
  private Map<LabelNode, LabelNode> hiddenLabels;

  /**
   * Set of all LabelNode objects in the current MethodNode.
   */
  private Set<LabelNode> labels;

  /** Set of all jumps (and subsequently sub-jumps). */
  private Set<Jump> jumps;

  /**
   * List of row indices where index i refers to the row of the first instruction at the i+1-th
   * LabelNode in labelToInsnMap.
   */
  private List<Integer> numOfInstructions;

  /**
   * The class name of the ClassNode containing the current MethodNode object.
   */
  private String className;

  /** The number of rows (non-negative opcode instructions). */
  private int rows;

  /**
   * The row of the verify error. If we were unable to determine a row number this value will be -2.
   * If there was no verify error, then this number will be -1.
   */
  private int errorRow;

  /**
   * The verify error message, or null if there was no verify error.
   */
  private String errorMsg;

  /**
   * boolean for determining whether changes have been made to the underlying MethodNode object.
   */
  private boolean modified;

  /**
   * Stack frames of the current method node (where the frames of non-negative instruction opcodes
   * have been stripped away).
   */
  private Frame<BasicValue>[] frames;

  /**
   * Create a new ByteCodeTableModel with the supplied ClassNode object. This will not load a
   * MethodNode into the model but initialize the model to verify over classNode.
   * 
   * @param classNode ClassNode
   */
  public BytecodeTableModel(ClassNode classNode) {
    labels = new HashSet<LabelNode>();
    labelToInsnMap = new LinkedHashMap<LabelNode, AbstractInsnNode>();
    numOfInstructions = new LinkedList<Integer>();
    jumps = new HashSet<Jump>();
    hiddenLabels = new LinkedHashMap<LabelNode, LabelNode>();
    modified = false;

    List<Type> interfaces = null;
    if (classNode.interfaces != null) {
      interfaces = new LinkedList<Type>();
      for (String interfaceName : classNode.interfaces) {
        interfaces.add(Type.getObjectType(interfaceName));
      }
    }

    analyzer = new Analyzer<BasicValue>(new CleartrackVerifier(Type.getObjectType(classNode.name),
        classNode.superName == null ? null : Type.getObjectType(classNode.superName), interfaces,
        (classNode.access & Opcodes.ACC_INTERFACE) == Opcodes.ACC_INTERFACE));
    className = classNode.name;
  }

  /**
   * Loads methodNode into the model and triggers a TableChanged event. This is like reloadMethods,
   * but will fire a label removed LabelChangeEvent on all labels loaded by the preceeding method.
   * 
   * @param methodNode MethodNode
   */
  public void setMethod(MethodNode methodNode) {
    this.methodNode = methodNode;
    for (LabelNode label : labels) {
      fireLabelRemovedEvent(new LabelChangeEvent(label));
    }
    labels.clear();
    if (methodNode != null)
      reloadMethod(modified);
    fireTableDataChanged();
  }

  public MethodNode getMethod() {
    return methodNode;
  }

  /**
   * Reloads the current underlying MethodNode object into the model and sets the modified flag.
   * 
   * @param modified boolean
   */
  @SuppressWarnings("unchecked")
  protected void reloadMethod(boolean modified) {
    // Reset all state left behind from the preceeding method node.
    this.modified = modified;
    rows = 0;
    labelToInsnMap.clear();
    numOfInstructions.clear();
    hiddenLabels.clear();

    // Verify methodNode and produce the stack frame information.
    errorRow = -2;
    AbstractInsnNode errorInsn = null;
    try {
      // We haven't computed stack and local size, so let's assume some really large value.
      methodNode.maxLocals = 9999;
      methodNode.maxStack = 9999;
      frames = analyzer.analyze(className, methodNode);
      errorRow = -1;
      errorMsg = null;
      //#if issubstr("Mac", os_type)
      if (OS.get().isMac()) {
        Application macApplication = Application.getApplication();
        macApplication.setDockIconBadge(null);
      }
      //#endif
    } catch (AnalyzerException e) {
      errorMsg = e.getMessage();
      if (errorMsg != null) {
        int start = errorMsg.indexOf("Error at instruction ");
        if (start >= 0) {
          start += "Error at instruction ".length();
          int end = errorMsg.indexOf(':', start);
          if (end >= 0) {
            int errorInsnOffset = Integer.parseInt(errorMsg.substring(start, end));
            errorInsn = methodNode.instructions.get(errorInsnOffset);
          }
        }
        frames = analyzer.getFrames();
      }
    } catch (Throwable e) {
      errorMsg = e.getMessage();
      e.printStackTrace();
      frames = analyzer.getFrames();
    }

    int k = 0;

    // We need this to filter out frames of instructions that we do not show.
    ArrayList<Frame<BasicValue>> newFrames = new ArrayList<Frame<BasicValue>>();

    // Some methods don't have a label before the first set of instructions.
    AbstractInsnNode curNode = methodNode.instructions.getFirst();
    if (!(curNode instanceof LabelNode)) {
      while (curNode != null && curNode.getOpcode() < 0) {
        curNode = curNode.getNext();
      }
      if (curNode != null)
        labelToInsnMap.put(null, curNode);
    }

    for (; curNode != null; curNode = curNode.getNext()) {
      if (curNode instanceof LabelNode) {
        if (labelToInsnMap.size() > 0)
          numOfInstructions.add(rows);
        LabelNode label = (LabelNode) curNode;
        if (!labels.contains(label)) {
          // We've encountered a new label, so add this to the model and fire a LabelChangeEvent.
          labels.add(label);
          fireLabelAddedEvent(new LabelChangeEvent(label));
        }
        while (curNode != null && curNode.getOpcode() < 0) {
          curNode = curNode.getNext();
          if (curNode instanceof LabelNode) {
            hiddenLabels.put((LabelNode) curNode, label);
          }
          k++;
        }
        labelToInsnMap.put(label, curNode);
        if (curNode == null)
          break;
      }

      if (curNode instanceof JumpInsnNode) {
        JumpInsnNode jumpNode = (JumpInsnNode) curNode;
        if (!labels.contains(jumpNode.label)) {
          // We've encountered a new label, so add this to the model and fire a LabelChangeEvent.
          labels.add(jumpNode.label);
          fireLabelAddedEvent(new LabelChangeEvent(jumpNode.label));
        }
      } else if (curNode instanceof TableSwitchInsnNode) {
        TableSwitchInsnNode tableSwitch = (TableSwitchInsnNode) curNode;
        for (LabelNode switchLabel : tableSwitch.labels) {
          if (!labels.contains(switchLabel)) {
            // We've encountered a new label, so add this to the model and fire a LabelChangeEvent.
            labels.add(switchLabel);
            fireLabelAddedEvent(new LabelChangeEvent(switchLabel));
          }
        }
        if (tableSwitch.dflt != null && !labels.contains(tableSwitch.dflt)) {
          // We've encountered a new label, so add this to the model and fire a LabelChangeEvent.
          labels.add(tableSwitch.dflt);
          fireLabelAddedEvent(new LabelChangeEvent(tableSwitch.dflt));
        }
      } else if (curNode instanceof LookupSwitchInsnNode) {
        LookupSwitchInsnNode lookupSwitch = (LookupSwitchInsnNode) curNode;
        for (LabelNode switchLabel : lookupSwitch.labels) {
          if (!labels.contains(switchLabel)) {
            // We've encountered a new label, so add this to the model and fire a LabelChangeEvent.
            labels.add(switchLabel);
            fireLabelAddedEvent(new LabelChangeEvent(switchLabel));
          }
        }
        if (lookupSwitch.dflt != null && !labels.contains(lookupSwitch.dflt)) {
          // We've encountered a new label, so add this to the model and fire a LabelChangeEvent.
          labels.add(lookupSwitch.dflt);
          fireLabelAddedEvent(new LabelChangeEvent(lookupSwitch.dflt));
        }
      }

      if (curNode.getOpcode() >= 0) {
        newFrames.add(frames[k]);
        if (errorInsn == curNode) {
          // We found the instruction causing the verify error.
          errorRow = rows;
        }
        rows++;
      }
      k++;
    }

    // Report a verify error as a DockIconBadge (specifically for Mac OSX).
    //#if issubstr("Mac", os_type)
    if (errorRow != -1 && OS.get().isMac()) {
      Application macApplication = Application.getApplication();
      macApplication.setDockIconBadge("" + errorRow);
    }
    //#endif

    frames = newFrames.toArray(new Frame[0]);
    numOfInstructions.add(rows);

    // Reconstruct all of the branches into an ordered data structure.
    jumps.clear();
    for (curNode = methodNode.instructions.getFirst(); curNode != null; curNode =
        curNode.getNext()) {
      if (curNode instanceof JumpInsnNode) {
        int fromRow = getRowOf(curNode);
        LabelNode jumpToLabel = ((JumpInsnNode) curNode).label;
        int gotoRow = getRowOf(jumpToLabel);
        if (fromRow < 0 || gotoRow < 0) {
          /*
           * TODO: We should investigate why one of these offsets would be negative. Perhaps we
           * encounter multiple labels in a row or something??
           */
          System.out.println("INVALID JUMP INSTRUCTION!!");
          continue;
        }
        Jump newJump = new Jump(fromRow, gotoRow);

        // It's possible that the jump already exists, and we can simply add the offset to the
        // existing branch, in this case.
        boolean mergedExisting = false;
        for (Jump jump : jumps) {
          if (jump.mergeWithExistingJump(newJump)) {
            mergedExisting = true;
            // System.out.println("merged " + jump + " with " + newJump);
            break;
          }
        }

        if (!mergedExisting) {
          // We were unable to find an existing jump, so join the new jump with each root jump,
          // until we are able to find an intersecting jump.
          boolean disjoint = true;
          for (Jump jump : jumps) {
            disjoint = disjoint && !jump.join(newJump);
            if (!disjoint)
              break;
          }

          if (disjoint)
            jumps.add(newJump);
        }
        // System.out.println("jumps: " + jumps);
      }
    }
  }

  /**
   * Get the set of all branches of this model's underlying MethodNode.
   * 
   * @return Set&lt;Jump&gt;
   */
  public Set<Jump> getJumps() {
    return jumps;
  }

  /**
   * @return The row number containing the verify error, -1 if there was no verify error, or -2 if
   *         there was a verify error but the verifier was unable to determine the line number.
   */
  public int getErrorRow() {
    return errorRow;
  }

  public String getErrorMessage() {
    return errorMsg;
  }

  @Override
  public int getRowCount() {
    return rows;
  }

  @Override
  public int getColumnCount() {
    return 4;
  }

  @Override
  public String getColumnName(int columnIndex) {
    switch (columnIndex) {
      case 0:
        return "Offset";
      case 1:
        return "Label";
      case 2:
        return "Opcode";
      default:
        return "Operands";
    }
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    switch (columnIndex) {
      case 0:
        return String.class;
      case 1:
        return LabelNode.class;
      default:
        return AbstractInsnNode.class;
    }
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return columnIndex > 0;
  }

  public int getRowOf(AbstractInsnNode insnNode) {
    if (insnNode == null)
      return -1;

    // If it's a label, it could possible be an alias and so we need to ensure we grab the actual
    // label node.
    boolean isLabel = insnNode instanceof LabelNode;
    if (isLabel) {
      LabelNode actualLabel = hiddenLabels.get(insnNode);
      if (actualLabel != null)
        insnNode = actualLabel;
    }

    Iterator<Integer> numIter = numOfInstructions.iterator();
    if (!numIter.hasNext()) // there are no instructions, return -1.
      return -1;
    int lastNum = 0;
    // Iterate through each entry in the labelToInsnMap.
    for (Entry<LabelNode, AbstractInsnNode> entry : labelToInsnMap.entrySet()) {
      int curNum = numIter.next();
      if (insnNode == entry.getKey())
        return lastNum; // The instruction node was found (and it happens to be a label), so return
                        // the value that was pulled from the numOfInstructions set. It's a label,
                        // no need to check the instructions that come after
      if (!isLabel) {
        AbstractInsnNode curNode = entry.getValue();
        do {
          if (curNode == null)
            return -1;
          if (insnNode == curNode) // We found the node, so return the computed row.
            return lastNum;
          curNode = curNode.getNext();
          lastNum++;
        } while (curNode.getOpcode() >= 0);
      }
      lastNum = curNum;
    }
    return -1;
  }

  public List<LabelNode> getLabelAliases(LabelNode label) {
    List<LabelNode> aliases = new LinkedList<LabelNode>();
    for (Entry<LabelNode, LabelNode> entry : hiddenLabels.entrySet()) {
      if (entry.getValue() == label)
        aliases.add(entry.getKey());
    }
    return aliases;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    if (columnIndex == 0) // The column is the row/instruction number.
      return "" + rowIndex;
    if (rowIndex >= rows) // rowIndex is too large
      return null;
    Iterator<Integer> numIter = numOfInstructions.iterator();
    if (!numIter.hasNext())
      return null;
    int lastNum = 0;
    for (Entry<LabelNode, AbstractInsnNode> entry : labelToInsnMap.entrySet()) {
      int curNum = numIter.next();

      if (lastNum == rowIndex) {
        // The instruction is the first one indicated by the labelToInsnMap. So, if the column is
        // the label column, then chose the key (i.e. LabelNode). Otherwise, choose the first
        // instruction immediately following this LabelNode.
        if (columnIndex == 1)
          return entry.getKey();
        else
          return entry.getValue();
      } else if (curNum > rowIndex) {
        // The instruction lies somewhere in the current LabelNode block.
        if (columnIndex == 1)
          return null; // We only need to report LabeNode's here.
        int total = lastNum;
        AbstractInsnNode curNode = entry.getValue();
        while (total++ < rowIndex)
          curNode = curNode.getNext();
        return curNode; // We found the node.
      }

      lastNum = curNum;
    }
    return null;
  }

  @Override
  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    switch (columnIndex) {
      case 1: { // LabelNode instructions.
        Object oldValue = getValueAt(rowIndex, columnIndex);
        if (oldValue == null) {
          // There was no label before, so add a new label at rowIndex presuming that the value we
          // are adding is not null.
          if (aValue != null && !methodNode.instructions.contains((AbstractInsnNode) aValue))
            methodNode.instructions
                .insertBefore(
                (AbstractInsnNode) getValueAt(rowIndex, columnIndex + 1),
                (AbstractInsnNode) aValue);
          else
            return;
        } else if (aValue == null) {
          // remove the existing label...
          AbstractInsnNode curNode =
              ((AbstractInsnNode) getValueAt(rowIndex, columnIndex + 1)).getPrevious();
          while (curNode.getOpcode() < 0) {
            AbstractInsnNode prev = curNode.getPrevious();
            methodNode.instructions.remove(curNode);
            curNode = prev;
          }
        } else {
          return;
        }
        // If we made it to here then a label has been altered so mark this as modified and reload
        // the method to recalculate flush out the old labels and recompute the labels/branches for
        // methodNode.
        modified = true;
        setMethod(methodNode);
        return;
      }
      case 2: { // Opcode >= 0 instructions.
        if (aValue == null) // Ignore null values, since an instruction can't be null.
          return;
        AbstractInsnNode insnNode = (AbstractInsnNode) getValueAt(rowIndex, columnIndex);
        int opcode = ((Opcode) aValue).getOpcode();
        // If the opcode did not change, then we don't need to do anything.
        if (insnNode.getOpcode() == opcode)
          return;
        // Insert the new instruction relative to the instruction we will delete.
        switch (opcode) {
          case Opcodes.GETSTATIC:
          case Opcodes.PUTSTATIC:
          case Opcodes.GETFIELD:
          case Opcodes.PUTFIELD:
            methodNode.instructions.insert(insnNode, new FieldInsnNode(opcode, "XXX", "XXX", "I"));
            break;
          case Opcodes.IINC:
            methodNode.instructions.insert(insnNode, new IincInsnNode(1, 1));
            break;
          case Opcodes.BIPUSH:
          case Opcodes.SIPUSH:
            methodNode.instructions.insert(insnNode, new IntInsnNode(opcode, 0));
            break;
          case Opcodes.NEWARRAY:
            methodNode.instructions.insert(insnNode, new IntInsnNode(opcode, Opcodes.T_INT));
            break;
          case Opcodes.INVOKEDYNAMIC:
            return;
          case Opcodes.IFEQ:
          case Opcodes.IFNE:
          case Opcodes.IFLT:
          case Opcodes.IFGE:
          case Opcodes.IFGT:
          case Opcodes.IFLE:
          case Opcodes.IF_ICMPEQ:
          case Opcodes.IF_ICMPNE:
          case Opcodes.IF_ICMPLT:
          case Opcodes.IF_ICMPGE:
          case Opcodes.IF_ICMPGT:
          case Opcodes.IF_ICMPLE:
          case Opcodes.IF_ACMPEQ:
          case Opcodes.IF_ACMPNE:
          case Opcodes.GOTO:
          case Opcodes.JSR:
          case Opcodes.IFNULL:
          case Opcodes.IFNONNULL:
            Iterator<Entry<LabelNode, AbstractInsnNode>> entries =
                labelToInsnMap.entrySet().iterator();
            LabelNode label = entries.hasNext() ? entries.next().getKey() : new LabelNode();
            methodNode.instructions.insert(insnNode, new JumpInsnNode(opcode, label));
            break;
          case Opcodes.LDC:
            methodNode.instructions.insert(insnNode, new LdcInsnNode(""));
            break;
          case Opcodes.LOOKUPSWITCH:
          case Opcodes.TABLESWITCH:
            return;
          case Opcodes.INVOKEINTERFACE:
          case Opcodes.INVOKESPECIAL:
          case Opcodes.INVOKESTATIC:
          case Opcodes.INVOKEVIRTUAL:
            methodNode.instructions.insert(insnNode,
                new MethodInsnNode(opcode, "XXX", "XXX", "()V", opcode == Opcodes.INVOKEINTERFACE));
            break;
          case Opcodes.MULTIANEWARRAY:
            methodNode.instructions.insert(insnNode, new MultiANewArrayInsnNode("[I", 1));
            break;
          case Opcodes.NEW:
          case Opcodes.ANEWARRAY:
          case Opcodes.CHECKCAST:
          case Opcodes.INSTANCEOF:
            methodNode.instructions.insert(insnNode, new TypeInsnNode(opcode, "java/lang/Object"));
            break;
          case Opcodes.ILOAD:
          case Opcodes.LLOAD:
          case Opcodes.FLOAD:
          case Opcodes.DLOAD:
          case Opcodes.ALOAD:
          case Opcodes.ISTORE:
          case Opcodes.LSTORE:
          case Opcodes.FSTORE:
          case Opcodes.DSTORE:
          case Opcodes.ASTORE:
          case Opcodes.RET:
            methodNode.instructions.insert(insnNode, new VarInsnNode(opcode, 1));
            break;
          default:
            methodNode.instructions.insert(insnNode, new InsnNode(opcode));
        }
        // Delete the old instruction.
        methodNode.instructions.remove(insnNode);
        break;
      }
      case 3: { // The operand of the instruction.
        AbstractInsnNode insnNode = (AbstractInsnNode) getValueAt(rowIndex, columnIndex);
        if (insnNode == null) // Ignore null values
          return;
        // Non-jumps are clone so we must insert the new (cloned) instruction, and remove the old
        // one.
        if (!(insnNode instanceof JumpInsnNode)) {
          methodNode.instructions.insert(insnNode, (AbstractInsnNode) aValue);
          methodNode.instructions.remove(insnNode);
        }
        break;
      }
      default:
        return;
    }

    // If we get here then the model has been altered, so reload the MethodNode (setting modified to
    // true), and issue a TableChangeEvent.
    reloadMethod(true);
    fireTableDataChanged();
  }

  /**
   * Adds a LabelChangeListener to this model.
   * 
   * @param listener LabelChangeListener
   */
  public void addLabelChangeListener(LabelChangeListener listener) {
    listenerList.add(LabelChangeListener.class, listener);
  }

  /**
   * Removes a LabelChangeListener to this model.
   * 
   * @param listener LabelChangeListener
   */
  public void removeLabelChangeListener(LabelChangeListener listener) {
    listenerList.remove(LabelChangeListener.class, listener);
  }

  private void fireLabelRemovedEvent(LabelChangeEvent event) {
    // Guaranteed to return a non-null array.
    Object[] listeners = listenerList.getListenerList();
    // Process the listeners last to first, notifying those that are interested in this event.
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == LabelChangeListener.class) {
        ((LabelChangeListener) listeners[i + 1]).labelRemoved(event);
      }
    }
  }

  private void fireLabelAddedEvent(LabelChangeEvent event) {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    // Process the listeners last to first, notifying those that are interested in this event.
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == LabelChangeListener.class) {
        ((LabelChangeListener) listeners[i + 1]).labelAdded(event);
      }
    }
  }

  /**
   * Adds a new (NOP) instruction at rowIndex and fires a TableChangeEvent.
   * 
   * @param rowIndex int
   */
  public void insertNewInstruction(int rowIndex) {
    AbstractInsnNode insnNode = (AbstractInsnNode) getValueAt(rowIndex, 2);
    methodNode.instructions.insertBefore(insnNode, new InsnNode(Opcodes.NOP));
    reloadMethod(true);
    fireTableDataChanged();
  }

  /**
   * Removes the instructions at the rows specified by rows.
   * 
   * @param rows int[]
   */
  public void removeRows(int[] rows) {
    if (rows == null)
      return;

    List<AbstractInsnNode> toRemove = new LinkedList<AbstractInsnNode>();
    for (int i = 0; i < rows.length; i++)
      toRemove.add((AbstractInsnNode) getValueAt(rows[i], 2));
    for (AbstractInsnNode insnNode : toRemove) {
      if (insnNode != null) {
        methodNode.instructions.remove(insnNode);
      }
    }
    reloadMethod(true);
    fireTableDataChanged();
  }

  /**
   * @return true iff the model has been altered.
   */
  public boolean isModified() {
    return modified;
  }

  /**
   * Acquire the Frame at the specified row.
   * 
   * @param row int
   * @return Frame&lt;BasicValue&gt;
   */
  public Frame<BasicValue> getFrameAt(int row) {
    if (frames == null || row < 0 || row >= frames.length)
      return null;
    return frames[row];
  }

  /**
   * Determines if a LabelNode has been referenced in either a jump, switch, local variable table,
   * or exception table.
   * 
   * @param label LabelNode
   * @return true if label is referenced.
   */
  public boolean isReferenced(LabelNode label) {
    if (label == null)
      return false;
    if (methodNode.localVariables != null) {
      for (LocalVariableNode localVar : methodNode.localVariables) {
        if (localVar.start == label || localVar.end == label)
          return true;
      }
    }
    if (methodNode.exceptions != null) {
      for (TryCatchBlockNode exceptions : methodNode.tryCatchBlocks) {
        if (exceptions.start == label || exceptions.end == label || exceptions.handler == label)
          return true;
      }
    }
    for (AbstractInsnNode curNode = methodNode.instructions.getFirst(); curNode != null; curNode =
        curNode.getNext()) {
      if (curNode instanceof JumpInsnNode) {
        if (((JumpInsnNode) curNode).label == label)
          return true;
      } else if (curNode instanceof TableSwitchInsnNode) {
        if (((TableSwitchInsnNode) curNode).labels.contains(label))
          return true;
      } else if (curNode instanceof LookupSwitchInsnNode) {
        if (((LookupSwitchInsnNode) curNode).labels.contains(label))
          return true;
      }
    }

    // It's not directly referenced, but it could be referenced from
    // an aliased LabelNode.
    for (LabelNode hiddenLabel : getLabelAliases(label)) {
      if (isReferenced(hiddenLabel))
        return true;
    }
    return false;
  }

  public String toHtml() {
    String header =
        StringEscapeUtils.escapeHtml(className + "." + methodNode.name + methodNode.desc);

    // Determine the jump calls that should be made in the javascript.
    StringBuilder jumpsHtml = new StringBuilder();
    int maxLevel = 0;
    for (Jump jump : jumps)
      maxLevel = Math.max(maxLevel, jump.toHtml(jumpsHtml, 1));

    // Add all of the table row data.
    StringBuilder rowData = new StringBuilder();
    int offsetWidth = 10 * maxLevel + 60;
    int colCount = getColumnCount();
    String[] classStr = new String[] {"offset", "label", "opcode", "operand"};
    for (int i = 0; i < getRowCount(); i++) {
      rowData.append("      <tr>\n");
      for (int j = 0; j < colCount; j++) {
        // Determine the HTML encoded value.
        boolean isConstant = false;
        Object value = getValueAt(i, j);
        if (value instanceof LabelNode) {
          LabelNode label = (LabelNode) value;
          StringBuilder buf = new StringBuilder();
          buf.append(InstructionCellRenderer.getLabelString(label, true));
          for (LabelNode alias : getLabelAliases(label)) {
            String aliasStr = InstructionCellRenderer.getLabelString(alias, true);
            if (aliasStr != null) {
              buf.append("&nbsp;");
              buf.append(aliasStr);
            }
          }
          value = buf.toString();
        } else if (value instanceof AbstractInsnNode) {
          String str = AsmUtils.toString((AbstractInsnNode) value);
          int idx = str.indexOf(' ');
          if (idx < 0) {
            if (j == 2)
              value = str;
            else
              value = "&nbsp;";
          } else {
            if (j == 2) {
              value = str.substring(0, idx);
            } else if (value instanceof JumpInsnNode) {
              value = InstructionCellRenderer.getLabelString(((JumpInsnNode) value).label, false);
            } else if (value instanceof TableSwitchInsnNode) {
              value = InstructionCellRenderer.getSwitchString((TableSwitchInsnNode) value);
            } else if (value instanceof LookupSwitchInsnNode) {
              value = InstructionCellRenderer.getSwitchString((LookupSwitchInsnNode) value);
            } else {
              isConstant = value instanceof LdcInsnNode;
              value = StringEscapeUtils.escapeHtml(str.substring(idx + 1));
            }
          }
        } else if (value == null) {
          value = "&nbsp;";
        }

        String attributes;
        if (isConstant) {
          attributes = " class='constant'";
        } else {
          attributes = " class='" + classStr[j] + "'";
        }

        rowData.append("         <td" + attributes + ">" + value + "</td>\n");
      }
      rowData.append("      </tr>\n");
    }

    // Return the entire HTML content.
    return "<html>\n" + "<head>\n" + "<style>\n" + "* {\n" + "   margin: 0;\n" + "   padding: 0;\n"
        + "   font-family: courier;\n" + "   font-weight: bold;\n" + "}\n" + "#bytecode {\n"
        + "   width: 100%;\n" + "}\n" + "#header {\n" + "   font-size: x-large;\n"
        + "   font-weight: bolder;\n" + "   text-decoration: underline;\n"
        + "   margin-bottom: 20px;\n" + "   word-wrap: break-word;\n" + "}\n" + ".constant {\n"
        + "   font-style: italic;\n" + "   font-weight: lighter;\n" + "   max-width: 400px;\n"
        + "   word-wrap: break-word;\n" + "}\n" + ".offset {\n" + "   font-weight: lighter;\n"
        + "   width: " + offsetWidth + "px;\n" + "}\n" + ".label {\n" + "   width: 75px;\n" + "}\n"
        + ".opcode {\n" + "   width: 175px;\n" + "}\n" + ".operand {\n" + "   max-width: 400px;\n"
        + "   word-wrap: break-word;\n" + "}\n" + "</style>\n" + "<script>\n"
        + "   window.onresize = drawJumps;\n" + "\n" + "   function drawJumps() {\n"
        + "      var divs = document.getElementsByClassName('jump');\n"
        + "      for(var i = 0; i < divs.length; i++) {\n"
        + "         divs[i].parentNode.removeChild(divs[i]);\n" + "      }\n" + jumpsHtml + "   }\n"
        + "\n" + "   function drawJump(from, to, level) {\n"
        + "       var table = document.getElementById('bytecode');\n"
        + "       var cellFrom = table.rows[from].cells[0];\n"
        + "       var cellTo = table.rows[to].cells[0];\n"
        + "       var topFrom = cellFrom.offsetTop + (cellFrom.offsetHeight / 2) + table.offsetTop;\n"
        + "       var topTo = cellTo.offsetTop + (cellTo.offsetHeight / 2) + table.offsetTop;\n"
        + "       var height = topTo - topFrom;\n" + "       var leftCoord = level * 10 + 25;\n"
        + "       var width = (cellFrom.offsetWidth - leftCoord);\n"
        + "       var arrowTop = topTo - 5;\n"
        + "       var arrowLeft = cellTo.offsetLeft + cellTo.offsetWidth - 10;\n" + "\n"
        + "       if (height < 0) {\n"
        + "          addVerticalLine(topFrom + height, leftCoord, -height);\n" + "       } else {\n"
        + "          addVerticalLine(topFrom, leftCoord, height);\n" + "       }\n"
        + "       addHorizontalLine(topFrom, leftCoord, width + table.rows[from].cells[1].offsetWidth);\n"
        + "       addHorizontalLine(topTo, leftCoord, width);\n"
        + "       addArrow(arrowTop, arrowLeft);\n" + "   }\n" + "\n"
        + "   function addVerticalLine(topFrom, leftCoord, height) {\n"
        + "       addDiv('height: ' + height\n"
        + "               + 'px; border-left: 1px solid black; position: absolute; top: '\n"
        + "               + topFrom + 'px; left: ' + leftCoord + 'px;');\n" + "   }\n" + "\n"
        + "   function addHorizontalLine(topCoord, leftCoord, width) {\n"
        + "       addDiv('width: ' + width\n"
        + "               + 'px; border-top: 1px solid black; position: absolute; top: '\n"
        + "               + topCoord + 'px; left: ' + leftCoord + 'px;');\n" + "   }\n" + "\n"
        + "   function addArrow(topCoord, leftCoord) {\n"
        + "       addDiv('top: ' + topCoord + 'px; left: ' + leftCoord\n"
        + "               + 'px; position: absolute; border-top: 5px solid transparent; '\n"
        + "               + 'border-left: 10px solid black; border-bottom: 5px solid transparent;');\n"
        + "   }\n" + "\n" + "   function addDiv(style) {\n"
        + "       var divEle = document.createElement('div');\n"
        + "       divEle.setAttribute('style', style);\n"
        + "       divEle.setAttribute('class', 'jump');\n"
        + "       document.body.insertBefore(divEle, document.getElementById('bytecode'));\n"
        + "   }\n" + "</script>\n" + "</head>\n" + "<body onload='drawJumps()'>\n"
        + "   <h1 id='header'>" + header + "</h1>\n" + "   <table id='bytecode'>\n" + rowData
        + "   </table>\n" + "</body>\n" + "</html>\n";

  }
}
