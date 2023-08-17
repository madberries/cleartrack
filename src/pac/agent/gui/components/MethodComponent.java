package pac.agent.gui.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTextField;

import pac.org.objectweb.asm.tree.AbstractInsnNode;
import pac.org.objectweb.asm.tree.MethodInsnNode;

/**
 * JTable component for manipulating MethodInsnNode objects.
 * 
 * @author jeikenberry
 */
public class MethodComponent extends JTextField implements ActionListener, InstructionComponent {
  private static final long serialVersionUID = 7788900653671787171L;

  private MethodInsnNode instruction;

  public MethodComponent() {
    super();
  }

  @Override
  public void setInstruction(AbstractInsnNode instruction) {
    this.instruction = (MethodInsnNode) instruction;
    if (instruction != null) {
      setText(this.instruction.owner + "." + this.instruction.name + this.instruction.desc);
    }
    invalidate();
    revalidate();
    repaint();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    String t = getText();
    if (instruction != null && t != null) {
      int methodStart = t.indexOf('.') + 1;
      if (methodStart <= 0)
        return;
      int methodEnd = t.indexOf('(', methodStart);
      if (methodEnd < 0)
        return;
      instruction.owner = t.substring(0, methodStart - 1).trim();
      instruction.name = t.substring(methodStart, methodEnd).trim();
      instruction.desc = t.substring(methodEnd).trim();
    }
  }
}
