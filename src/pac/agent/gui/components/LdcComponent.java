package pac.agent.gui.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTextField;

import pac.org.objectweb.asm.tree.AbstractInsnNode;
import pac.org.objectweb.asm.tree.LdcInsnNode;

/**
 * JTable component for manipulating LdcInsnNode objects.
 * 
 * @author jeikenberry
 */
public class LdcComponent extends JTextField implements ActionListener, InstructionComponent {
  private static final long serialVersionUID = 6228230705481431790L;

  private LdcInsnNode instruction;

  public LdcComponent() {
    super();
  }

  @Override
  public void setInstruction(AbstractInsnNode instruction) {
    this.instruction = (LdcInsnNode) instruction;
    if (instruction != null) {
      setText(this.instruction.cst.toString());
    }
    invalidate();
    revalidate();
    repaint();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    String t = getText();
    if (instruction != null && t != null) {
      // Infer the constant type by the size of the number or default the object as String if the
      // value is not a parseable number.
      try {
        long longVal = Long.parseLong(t.trim());
        if (longVal <= Byte.MAX_VALUE && longVal >= Byte.MIN_VALUE) {
          instruction.cst = (byte) longVal;
        } else if (longVal <= Short.MAX_VALUE && longVal >= Short.MIN_VALUE) {
          instruction.cst = (short) longVal;
        } else if (longVal <= Integer.MAX_VALUE && longVal >= Integer.MIN_VALUE) {
          instruction.cst = (int) longVal;
        } else {
          instruction.cst = longVal;
        }
      } catch (NumberFormatException ex) {
        try {
          double doubleVal = Double.parseDouble(t.trim());
          if (doubleVal <= Float.MAX_VALUE && doubleVal >= Float.MIN_VALUE) {
            instruction.cst = (float) doubleVal;
          } else {
            instruction.cst = doubleVal;
          }
        } catch (NumberFormatException ex2) {
          instruction.cst = t.trim();
        }
      }
    }
  }
}
