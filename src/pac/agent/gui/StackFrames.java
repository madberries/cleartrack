package pac.agent.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.beans.Transient;

import javax.swing.JComponent;

import pac.org.objectweb.asm.Type;
import pac.org.objectweb.asm.tree.MethodNode;
import pac.org.objectweb.asm.tree.analysis.BasicValue;
import pac.org.objectweb.asm.tree.analysis.Frame;

/**
 * Component for viewing initialized local variable/register types, and also for viewing the stack
 * of the current selected instruction.
 * 
 * @author jeikenberry
 */
public class StackFrames extends JComponent {
  private static final long serialVersionUID = -6882584233694438534L;

  private static final Color REG_COLOR = Color.BLUE;
  private static final Color LOCAL_COLOR = Color.GRAY;
  private static final Color SPECIAL_COLOR = Color.MAGENTA;

  /** margin size between columns (in pixels). */
  private static final int INSET = 10;

  /** number of columns (for local variables). **/
  // TODO may want to consider making this variable (based on maxLocals).
  private static final int NUM_OF_LOCAL_COLS = 5;

  /** The current stack frame. */
  private Frame<BasicValue> frame;

  /** The current MethodNode. */
  private MethodNode methodNode;

  /**
   * Sets the MethodNode that is currently being edited.
   * 
   * @param methodNode MethodNode
   */
  public void setMethod(MethodNode methodNode) {
    this.methodNode = methodNode;
  }

  /**
   * Sets the stack frame to be shown in this component.
   * 
   * @param frame Frame&lt;BasicValue&gt;
   */
  public void setFrame(Frame<BasicValue> frame) {
    this.frame = frame;
    invalidate();
    revalidate();
    repaint();
  }

  /**
   * Acquire the type from the local variable.
   * 
   * @param fm FontMetrics of the current graphics context.
   * @param local BasicValue of the local variable.
   * @param maxWidth int of the maximum width String to return (in pixels).
   * @return String of the ASM type name for local (truncated to fit within the bounds of maxWidth).
   */
  private String getType(FontMetrics fm, BasicValue local, int maxWidth) {
    if (local == null)
      return "<empty>";
    return getType(fm, local.getType(), maxWidth);
  }

  /**
   * Acquire the type from the local variable.
   * 
   * @param fm FontMetrics of the current graphics context.
   * @param local Type of the local variable.
   * @param maxWidth int of the maximum width String to return (in pixels).
   * @return String of the ASM type name for local (truncated to fit within the bounds of maxWidth).
   */
  private String getType(FontMetrics fm, Type type, int maxWidth) {
    if (type == null)
      return "<empty>";
    StringBuilder buf = new StringBuilder(type.toString());
    if (fm.stringWidth(buf.toString()) > maxWidth) {
      int deleteIdx = buf.length();
      buf.append("...");
      while (fm.stringWidth(buf.toString()) > maxWidth) {
        if (--deleteIdx < 0)
          break;
        buf.delete(deleteIdx, deleteIdx + 1);
      }
    }
    return buf.toString();
  }

  @Override
  @Transient
  public Dimension getPreferredSize() {
    int prefHeight = 400;
    if (methodNode != null) {
      int rem = methodNode.maxLocals % NUM_OF_LOCAL_COLS > 0 ? 1 : 0;
      prefHeight = ((methodNode.maxLocals / NUM_OF_LOCAL_COLS) + rem + methodNode.maxStack + 1) * 20
          + INSET * 7;
      if (prefHeight > 400) // Let's not get too crazy with the height.
        prefHeight = 400;
    }
    return new Dimension(500, prefHeight);
  }

  @Override
  @Transient
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  public void paintComponent(Graphics g) {
    super.paintComponent(g);

    // Calculate local variable column widths.
    FontMetrics fm = g.getFontMetrics();
    int numOfLocals = frame == null ? 0 : frame.getLocals();
    int[] widths = new int[NUM_OF_LOCAL_COLS];
    int localWidth = getWidth() - 2 * NUM_OF_LOCAL_COLS * INSET;
    for (int i = 0; i < numOfLocals; i++) {
      int k = i % NUM_OF_LOCAL_COLS;
      widths[k] = Math.max(widths[k], fm.stringWidth("" + i));
    }
    for (int i = 0; i < NUM_OF_LOCAL_COLS; i++)
      localWidth -= widths[i];
    localWidth = localWidth / NUM_OF_LOCAL_COLS;

    // Draw locals...
    BasicValue type = numOfLocals == 0 ? null : frame.getLocal(0);
    String typeStr = getType(fm, type, localWidth);
    for (int i = 0; i < numOfLocals;) {
      int x = INSET;
      int y = ((i / NUM_OF_LOCAL_COLS) + 1) * 20;

      for (int k = 0; k < NUM_OF_LOCAL_COLS; k++) {
        String varNum = "" + i;
        g.setColor(REG_COLOR);
        g.drawString(varNum, x, y);

        x += INSET + widths[k];
        int size = 0;
        if (type != null) {
          size = type.getSize();
        }
        g.setColor(typeStr.equals("<empty>") ? SPECIAL_COLOR : LOCAL_COLOR);
        g.drawString(typeStr, x, y);
        if (size > 1) {
          g.setColor(SPECIAL_COLOR);
          g.drawString(" <word 1>", x + fm.stringWidth(typeStr), y);
        } else if (size == 0) {
          g.setColor(SPECIAL_COLOR);
          g.drawString(" <word 2>", x + fm.stringWidth(typeStr), y);
        }

        if (++i >= numOfLocals)
          break;

        if (size == 2) {
          type = null;
        } else {
          type = frame.getLocal(i);
          typeStr = getType(fm, type, localWidth);
        }

        x += INSET + localWidth;
      }
    }

    g.setColor(LOCAL_COLOR);

    // Draw locals header.
    int y = numOfLocals % NUM_OF_LOCAL_COLS != 0 ? ((numOfLocals / NUM_OF_LOCAL_COLS) + 1) * 20 + 10
        : (numOfLocals / NUM_OF_LOCAL_COLS) * 20 + 10;
    g.drawLine(0, y, getWidth(), y);
    g.drawString("Locals", (getWidth() - fm.stringWidth("Locals")) / 2, y + fm.getAscent() + 3);
    g.drawLine(0, y + fm.getHeight() + 6, getWidth(), y + fm.getHeight() + 6);

    // Draw stack header.
    y = getHeight() - fm.getDescent() - INSET;
    g.drawLine(0, y, getWidth(), y);
    g.drawString("Stack", (getWidth() - fm.stringWidth("Stack")) / 2, y - fm.getDescent() - 3);
    y -= fm.getHeight() + 6;
    g.drawLine(0, y, getWidth(), y);

    // Draw stack.
    y -= fm.getDescent() + 3;
    int stackSize = frame == null ? 1 : frame.getStackSize() + 1;
    for (int i = 0; i < stackSize; i++) {
      typeStr = i == (stackSize - 1) ? "\u2193" : getType(fm, frame.getStack(i), getWidth());
      g.drawString(typeStr, (getWidth() - fm.stringWidth(typeStr)) / 2, y);
      // y -= fm.getHeight();
      y -= 20;
    }
  }
}
