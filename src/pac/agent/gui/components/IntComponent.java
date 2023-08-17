package pac.agent.gui.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTextField;

import pac.org.objectweb.asm.tree.AbstractInsnNode;
import pac.org.objectweb.asm.tree.IntInsnNode;

/**
 * JTable component for manipulating IntInsnNode objects (excluding NEWARRAY instructions).
 * 
 * @author jeikenberry
 */
public class IntComponent extends JTextField implements ActionListener, InstructionComponent {
  private static final long serialVersionUID = -8038458897260628058L;

  private IntInsnNode instruction;

  public IntComponent() {
    super();
  }

  @Override
  public void setInstruction(AbstractInsnNode instruction) {
    this.instruction = (IntInsnNode) instruction;
    if (instruction != null) {
      setText("" + this.instruction.operand);
    }
    invalidate();
    revalidate();
    repaint();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    String t = getText();
    if (instruction != null && t != null) {
      try {
        instruction.operand = Integer.parseInt(t.trim());
      } catch (NumberFormatException ex) {

      }
    }
  }
}
