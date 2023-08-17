package pac.agent.gui.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTextField;

import pac.org.objectweb.asm.tree.AbstractInsnNode;
import pac.org.objectweb.asm.tree.VarInsnNode;

/**
 * JTable component for manipulating VarInsnNode objects.
 * 
 * @author jeikenberry
 */
public class VarComponent extends JTextField implements ActionListener, InstructionComponent {
  private static final long serialVersionUID = -4676636642093360636L;

  private VarInsnNode instruction;

  public VarComponent() {
    super();
  }

  @Override
  public void setInstruction(AbstractInsnNode instruction) {
    this.instruction = (VarInsnNode) instruction;
    if (instruction != null) {
      setText("" + this.instruction.var);
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
        instruction.var = Integer.parseInt(t.trim());
      } catch (NumberFormatException ex) {

      }
    }
  }
}
