package pac.agent.gui.components;

import java.text.ParseException;

import javax.swing.JFormattedTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.MaskFormatter;

import pac.org.objectweb.asm.tree.AbstractInsnNode;
import pac.org.objectweb.asm.tree.IincInsnNode;

/**
 * JTable component for manipulating IincInsnNode objects.
 * 
 * @author jeikenberry
 */
public class IIncComponent extends JFormattedTextField
    implements DocumentListener, InstructionComponent {
  private static final long serialVersionUID = 7239718854213098562L;

  private static final int REG_DIGITS = 3, INC_DIGITS = 10;
  private static final String FORMAT_STR = "reg: %0" + REG_DIGITS + "d inc: %0" + INC_DIGITS + "d";
  private static final String FORMAT_MASK;

  private IincInsnNode instruction;

  static {
    StringBuilder buf = new StringBuilder("reg: ");
    for (int i = 0; i < REG_DIGITS; i++)
      buf.append('#');
    buf.append(" inc: ");
    for (int i = 0; i < INC_DIGITS; i++)
      buf.append('#');
    FORMAT_MASK = buf.toString();
  }

  public IIncComponent() throws ParseException {
    super(new MaskFormatter(FORMAT_MASK));
    getDocument().addDocumentListener(this);
  }

  @Override
  public void setInstruction(AbstractInsnNode instruction) {
    this.instruction = (IincInsnNode) instruction;
    if (instruction != null) {
      String formatted = String.format(FORMAT_STR, this.instruction.var, this.instruction.incr);
      setText(formatted);
    }
    invalidate();
    revalidate();
    repaint();
  }

  @Override
  public void insertUpdate(DocumentEvent e) {
    String t = getText();
    if (instruction != null && t != null) {
      if (t.length() < 11 + REG_DIGITS + INC_DIGITS)
        return;
      instruction.var = Integer.parseInt(t.substring(5, 5 + REG_DIGITS).trim());
      instruction.incr = Integer.parseInt(t.substring(11 + REG_DIGITS).trim());
    }
  }

  @Override
  public void removeUpdate(DocumentEvent e) {
    String t = getText();
    if (instruction != null && t != null) {
      if (t.length() < 11 + REG_DIGITS + INC_DIGITS)
        return;
      instruction.var = Integer.parseInt(t.substring(5, 5 + REG_DIGITS).trim());
      instruction.incr = Integer.parseInt(t.substring(11 + REG_DIGITS).trim());
    }
  }

  @Override
  public void changedUpdate(DocumentEvent e) {
    String t = getText();
    if (instruction != null && t != null) {
      if (t.length() < 11 + REG_DIGITS + INC_DIGITS)
        return;
      instruction.var = Integer.parseInt(t.substring(5, 5 + REG_DIGITS).trim());
      instruction.incr = Integer.parseInt(t.substring(11 + REG_DIGITS).trim());
    }
  }
}
