package pac.agent.gui.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;

import pac.agent.gui.editors.InstructionCellEditor;
import pac.agent.gui.renderers.LabelCellRenderer;
import pac.org.objectweb.asm.tree.AbstractInsnNode;
import pac.org.objectweb.asm.tree.JumpInsnNode;
import pac.org.objectweb.asm.tree.LabelNode;

/**
 * JTable component for manipulating JumpInsnNode objects.
 * 
 * @author jeikenberry
 */
public class JumpComponent extends JComboBox<LabelNode>
    implements ActionListener, InstructionComponent {
  private static final long serialVersionUID = -7500449196156103449L;

  private JumpInsnNode instruction;

  public JumpComponent() {
    super();
    setRenderer(new LabelCellRenderer());
    setMaximumRowCount(15);
  }

  @Override
  public void setInstruction(AbstractInsnNode instruction) {
    this.instruction = (JumpInsnNode) instruction;
    if (instruction != null) {
      String labelName = InstructionCellEditor.getLabelName(this.instruction.label);
      if (labelName != null) {
        setSelectedItem(this.instruction.label);
      }
    }
    invalidate();
    revalidate();
    repaint();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    LabelNode selectedLabel = (LabelNode) getSelectedItem();
    if (instruction != null && selectedLabel != null) {
      instruction.label = selectedLabel;
    }
  }

  @Override
  public void addItem(LabelNode item) {
    // Skip over duplicate LabelNode objects.
    for (int i = 0; i < getItemCount(); i++) {
      if (getItemAt(i) == item)
        return;
    }
    super.addItem(item);
  }
}
