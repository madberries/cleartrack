package pac.agent.gui.editors;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import pac.agent.gui.models.BytecodeTableModel;
import pac.agent.gui.tables.BytecodeTable;
import pac.org.objectweb.asm.tree.LabelNode;

/**
 * CellEditor for editing LabelNode objects (not instructions that happen to contain LabelNode
 * objects, i.e. JumpInsnNode's). The component behind this editor is simply a checkbox. By checking
 * the box, a new label is either added or removed. This will trigger a LabelChangeEvent, which will
 * generate a new label name for the newly added LabelNode.
 * 
 * @author jeikenberry
 */
public class LabelCellEditor extends AbstractCellEditor implements TableCellEditor {
  private static final long serialVersionUID = -2859582614245952670L;

  /** The core component for this CellEditor. */
  private JCheckBox checkBox;

  /** The current LabelNode. */
  private LabelNode label;

  public LabelCellEditor() {
    checkBox = new JCheckBox();
    checkBox.setSelected(false);
    checkBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (checkBox.isSelected()) {
          label = new LabelNode();
        } else {
          label = null;
        }
      }
    });
  }

  @Override
  public Object getCellEditorValue() {
    return label;
  }

  @Override
  public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
      int row, int column) {
    if (value instanceof LabelNode) {
      label = (LabelNode) value;
      checkBox.setSelected(true);
      boolean enabled =
          !((BytecodeTableModel) ((BytecodeTable) table).getModel()).isReferenced(label);
      checkBox.setEnabled(enabled);
    } else {
      label = null;
      checkBox.setSelected(false);
      checkBox.setEnabled(true);
    }
    return checkBox;
  }
}
