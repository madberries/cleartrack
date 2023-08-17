package pac.agent.gui.renderers;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import pac.agent.gui.editors.InstructionCellEditor;
import pac.org.objectweb.asm.tree.LabelNode;

/**
 * Cell renderer for LabelNodes inside of a Combo box.
 * 
 * @author jeikenberry
 */
public class LabelCellRenderer implements ListCellRenderer<LabelNode> {
  private JLabel label = new JLabel();

  @Override
  public Component getListCellRendererComponent(JList<? extends LabelNode> list, LabelNode value,
      int index, boolean isSelected, boolean cellHasFocus) {
    label.setText(InstructionCellEditor.getLabelName(value));
    label.setForeground(InstructionCellEditor.getLabelColor(value));
    return label;
  }
}
