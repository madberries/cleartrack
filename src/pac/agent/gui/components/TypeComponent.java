package pac.agent.gui.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTextField;

import pac.org.objectweb.asm.tree.AbstractInsnNode;
import pac.org.objectweb.asm.tree.TypeInsnNode;

/**
 * JTable component for manipulating TypeInsnNode objects.
 * 
 * @author jeikenberry
 */
public class TypeComponent extends JTextField implements ActionListener, InstructionComponent {
  private static final long serialVersionUID = 7788900653671787171L;

  private TypeInsnNode instruction;

  public TypeComponent() {
    super();
  }

  @Override
  public void setInstruction(AbstractInsnNode instruction) {
    this.instruction = (TypeInsnNode) instruction;
    if (instruction != null) {
      setText("" + this.instruction.desc);
    }
    invalidate();
    revalidate();
    repaint();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    String t = getText();
    if (instruction != null && t != null) {
      instruction.desc = t.trim();
    }
  }
}
