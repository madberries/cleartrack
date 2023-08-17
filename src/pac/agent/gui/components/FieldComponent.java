package pac.agent.gui.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTextField;

import pac.org.objectweb.asm.tree.AbstractInsnNode;
import pac.org.objectweb.asm.tree.FieldInsnNode;

/**
 * JTable component for manipulating FieldInsnNode objects.
 * 
 * @author jeikenberry
 */
public class FieldComponent extends JTextField implements ActionListener, InstructionComponent {
  private static final long serialVersionUID = -2979170909873486081L;

  private FieldInsnNode instruction;

  public FieldComponent() {
    super();
  }

  @Override
  public void setInstruction(AbstractInsnNode instruction) {
    this.instruction = (FieldInsnNode) instruction;
    if (instruction != null) {
      setText(this.instruction.owner + "." + this.instruction.name + ":" + this.instruction.desc);
    }
    invalidate();
    revalidate();
    repaint();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    String t = getText();
    if (instruction != null && t != null) {
      int fieldStart = t.indexOf('.') + 1;
      if (fieldStart <= 0)
        return;
      int fieldEnd = t.indexOf(':', fieldStart);
      if (fieldEnd < 0)
        return;
      instruction.owner = t.substring(0, fieldStart - 1).trim();
      instruction.name = t.substring(fieldStart, fieldEnd).trim();
      instruction.desc = t.substring(fieldEnd + 1).trim();
    }
  }
}
