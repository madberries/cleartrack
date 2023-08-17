package pac.agent.gui.editors;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;

import pac.agent.gui.LabelChangeEvent;
import pac.agent.gui.LabelChangeListener;
import pac.agent.gui.components.FieldComponent;
import pac.agent.gui.components.IIncComponent;
import pac.agent.gui.components.InstructionComponent;
import pac.agent.gui.components.IntComponent;
import pac.agent.gui.components.JumpComponent;
import pac.agent.gui.components.LabelComponent;
import pac.agent.gui.components.LdcComponent;
import pac.agent.gui.components.MethodComponent;
import pac.agent.gui.components.NewarrayComponent;
import pac.agent.gui.components.TypeComponent;
import pac.agent.gui.components.VarComponent;
import pac.org.objectweb.asm.Opcodes;
import pac.org.objectweb.asm.tree.AbstractInsnNode;
import pac.org.objectweb.asm.tree.FieldInsnNode;
import pac.org.objectweb.asm.tree.IincInsnNode;
import pac.org.objectweb.asm.tree.IntInsnNode;
import pac.org.objectweb.asm.tree.JumpInsnNode;
import pac.org.objectweb.asm.tree.LabelNode;
import pac.org.objectweb.asm.tree.LdcInsnNode;
import pac.org.objectweb.asm.tree.MethodInsnNode;
import pac.org.objectweb.asm.tree.TypeInsnNode;
import pac.org.objectweb.asm.tree.VarInsnNode;

/**
 * CellEditor for editing AbstractInsnNode objects that have opcodes >= 0. So, for example, this
 * editor does not support LabelNode. For LabelNode, you should use LabelCellEditor.
 * 
 * @author jeikenberry
 */
public class InstructionCellEditor extends AbstractCellEditor
    implements TableCellEditor, LabelChangeListener {
  private static final long serialVersionUID = -4570070919228750319L;

  /** Random number generator to produce random colors for labels. */
  private static final Random rand = new Random();

  /**
   * maintains mapping of LabelNode objects to new LabelNode objects (for cloning instructions).
   */
  private static final Map<LabelNode, LabelNode> labelMap = new HashMap<LabelNode, LabelNode>();

  /**
   * maintains mapping of LabelNode objects to it's label name (names are assign lexicologically).
   */
  private static final Map<LabelNode, String> labelNames = new HashMap<LabelNode, String>();
  /**
   * maintains mapping of LabelNode objects to their corresponding color.
   */
  private static final Map<LabelNode, Color> labelColors = new HashMap<LabelNode, Color>();

  /** The currently edited instruction node. */
  private AbstractInsnNode curNode;

  /**
   * Cache of all the types of editor components supported by this cell editor.
   */
  private InstructionComponent[] components;

  public InstructionCellEditor() {
    try {
      components = new InstructionComponent[] {new IIncComponent(), new VarComponent(),
          new TypeComponent(), new NewarrayComponent(), new IntComponent(), new LdcComponent(),
          new FieldComponent(), new MethodComponent(), new JumpComponent(), new LabelComponent()};
    } catch (ParseException e) {
      // I don't believe this exception is ever thrown, but let's exit just in case.
      e.printStackTrace();
      System.exit(1);
    }

    // Bind each component with their respective ActionListener implementation.
    for (int i = 0; i < components.length; i++) {
      if (components[i] instanceof JTextField) {
        JTextField textField = (JTextField) components[i];
        textField.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            fireEditingStopped();
          }
        });
        if (textField instanceof ActionListener)
          textField.addActionListener((ActionListener) textField);
      } else if (components[i] instanceof LabelComponent) {
        final JComboBox<?> comboBox = (JComboBox<?>) components[i];
        comboBox.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            curNode = (AbstractInsnNode) comboBox.getSelectedItem();
            fireEditingStopped();
          }
        });
        if (comboBox instanceof ActionListener)
          comboBox.addActionListener((ActionListener) comboBox);
      } else if (components[i] instanceof JComboBox<?>) {
        JComboBox<?> comboBox = (JComboBox<?>) components[i];
        comboBox.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            fireEditingStopped();
          }
        });
        if (comboBox instanceof ActionListener)
          comboBox.addActionListener((ActionListener) comboBox);
      }
    }
  }

  @Override
  public Object getCellEditorValue() {
    return curNode;
  }

  @Override
  public void fireEditingStopped() {
    super.fireEditingStopped();
  }

  @Override
  public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
      int row, int column) {
    if (!(value instanceof AbstractInsnNode))
      return null;

    curNode = (AbstractInsnNode) value;
    if (curNode instanceof LabelNode) {
      // do nothing
    } else if (curNode.getOpcode() < 0) {
      return null;
    } else if (!(curNode instanceof JumpInsnNode)) {
      curNode = (AbstractInsnNode) ((AbstractInsnNode) value).clone(labelMap);
    }

    if (curNode instanceof IincInsnNode) {
      components[0].setInstruction(curNode);
      return (Component) components[0];
    } else if (curNode instanceof VarInsnNode) {
      components[1].setInstruction(curNode);
      return (Component) components[1];
    } else if (curNode instanceof TypeInsnNode) {
      components[2].setInstruction(curNode);
      return (Component) components[2];
    } else if (curNode instanceof IntInsnNode) {
      switch (curNode.getOpcode()) {
        case Opcodes.NEWARRAY:
          components[3].setInstruction(curNode);
          return (Component) components[3];
        default:
          components[4].setInstruction(curNode);
          return (Component) components[4];
      }
    } else if (curNode instanceof LdcInsnNode) {
      components[5].setInstruction(curNode);
      return (Component) components[5];
    } else if (curNode instanceof FieldInsnNode) {
      components[6].setInstruction(curNode);
      return (Component) components[6];
    } else if (curNode instanceof MethodInsnNode) {
      components[7].setInstruction(curNode);
      return (Component) components[7];
    } else if (curNode instanceof JumpInsnNode) {
      components[8].setInstruction(curNode);
      return (Component) components[8];
    } else if (curNode instanceof LabelNode) {
      components[9].setInstruction(curNode);
      return (Component) components[9];
    }
    return null;
  }

  /**
   * Acquires the label name of labelNode.
   * 
   * @param labelNode LabelNode.
   * @return String of the labelNode's name.
   */
  public static String getLabelName(LabelNode labelNode) {
    return labelNames.get(labelNode);
  }

  /**
   * Acquires the label color of labelNode.
   * 
   * @param labelNode LabelNode.
   * @return Color of labelNode.
   */
  public static Color getLabelColor(LabelNode labelNode) {
    return labelColors.get(labelNode);
  }

  @Override
  public void labelAdded(LabelChangeEvent e) {
    // Update the label maps and the components that depend on LabelNode objects.
    LabelNode label = e.getLabel();
    synchronized (labelMap) {
      if (!labelMap.containsKey(label)) {
        labelMap.put(label, new LabelNode());
      }
      String labelName = labelNames.get(label);
      if (labelName == null) {
        labelName = "L" + labelNames.size();
        Color labelColor = new Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
        labelNames.put(label, labelName);
        labelColors.put(label, labelColor);
      } ;
      ((JumpComponent) components[8]).addItem(label);
      ((LabelComponent) components[9]).addItem(label);
    }
  }

  @Override
  public void labelRemoved(LabelChangeEvent e) {
    // Update the label maps and the components that depend on LabelNode objects.
    LabelNode label = e.getLabel();
    synchronized (labelMap) {
      labelMap.remove(label);
      labelNames.remove(label);
      labelColors.remove(label);
      ((JumpComponent) components[8]).removeItem(label);
      ((LabelComponent) components[9]).addItem(label);
    }
  }
}
