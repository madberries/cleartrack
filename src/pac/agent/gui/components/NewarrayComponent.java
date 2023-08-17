package pac.agent.gui.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;

import pac.org.objectweb.asm.Opcodes;
import pac.org.objectweb.asm.tree.AbstractInsnNode;
import pac.org.objectweb.asm.tree.IntInsnNode;

/**
 * JTable component for manipulating NEWARRAY instructions.
 * 
 * @author jeikenberry
 */
public class NewarrayComponent extends JComboBox<String>
    implements ActionListener, InstructionComponent {
  private static final long serialVersionUID = -6396379557314357327L;

  private IntInsnNode instruction;

  /*
   * FROM ASM API:
   * -------------
   * 
   * // Field descriptor #13 I
   * public static final int T_BOOLEAN = 4;
   * 
   * // Field descriptor #13 I
   * public static final int T_CHAR = 5;
   * 
   * // Field descriptor #13 I
   * public static final int T_FLOAT = 6;
   * 
   * // Field descriptor #13 I
   * public static final int T_DOUBLE = 7;
   * 
   * // Field descriptor #13 I
   * public static final int T_BYTE = 8;
   * 
   * // Field descriptor #13 I
   * public static final int T_SHORT = 9;
   * 
   * // Field descriptor #13 I
   * public static final int T_INT = 10;
   * 
   * // Field descriptor #13 I
   * public static final int T_LONG = 11;
   */
  public NewarrayComponent() {
    super(new String[] {"T_BOOLEAN", "T_CHAR", "T_FLOAT", "T_DOUBLE", "T_BYTE", "T_SHORT", "T_INT",
        "T_LONG"});
  }

  @Override
  public void setInstruction(AbstractInsnNode instruction) {
    this.instruction = (IntInsnNode) instruction;
    if (instruction != null) {
      int index = this.instruction.operand - Opcodes.T_BOOLEAN;
      setSelectedIndex(index);
    }
    invalidate();
    revalidate();
    repaint();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    int index = getSelectedIndex() + Opcodes.T_BOOLEAN;
    if (instruction != null && index >= Opcodes.T_BOOLEAN && index <= Opcodes.T_LONG) {
      instruction.operand = index;
    }
  }
}
