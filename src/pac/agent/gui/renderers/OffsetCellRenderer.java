package pac.agent.gui.renderers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import pac.agent.gui.models.BytecodeTableModel;
import pac.org.objectweb.asm.tree.analysis.BasicValue;
import pac.org.objectweb.asm.tree.analysis.Frame;

public class OffsetCellRenderer extends DefaultTableCellRenderer {
  private static final long serialVersionUID = -4700104415356342972L;
  private static final Color STACK_COLOR = new Color(224, 224, 224);

  /** The stack frame at the current offset */
  private Frame<BasicValue> frame;

  /** True if there is a verify error at the current offset */
  private boolean hasError;

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
      boolean hasFocus, int row, int column) {
    Component comp =
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    Font font = this.getFont();
    BytecodeTableModel btm = (BytecodeTableModel) table.getModel();
    int errorRow = btm.getErrorRow();
    hasError = errorRow == row || errorRow < -1;

    if (hasError) {
      setFont(font.deriveFont(Font.BOLD));
      setForeground(Color.RED);
    } else {
      setFont(font.deriveFont(Font.PLAIN));
      setForeground(Color.BLACK);
    }

    frame = btm.getFrameAt(row);

    // If there was a verify error on this row, then set the tool-tip text to be the error message.
    String errorMsg = null;
    if (hasError) {
      errorMsg = btm.getErrorMessage();
    }
    setToolTipText(errorMsg);

    return comp;
  }

  /**
   * Construct the indicator shape for a verify error in the given graphics context.
   * 
   * @param g2 Graphics2D
   * @return Shape
   */
  private Shape getErrorShape(Graphics2D g2) {
    float rectSize = 540f; // make the math easy for java :)
    float third = rectSize / 3.0f;

    // Error indicator in the form of a basic stop sign shape.
    GeneralPath error = new GeneralPath();
    error.moveTo(third, 0);
    error.lineTo(2.0f * third, 0);
    error.lineTo(rectSize, third);
    error.lineTo(rectSize, 2.0f * third);
    error.lineTo(2.0f * third, rectSize);
    error.lineTo(third, rectSize);
    error.lineTo(0, 2.0f * third);
    error.lineTo(0, third);
    error.closePath();

    Rectangle bounds = error.getBounds();
    FontRenderContext frc = g2.getFontRenderContext();
    Font f = new Font("Courier", Font.BOLD, Math.max(10, (int) (rectSize / 1.15f)));
    String s = new String("!");
    TextLayout textTl = new TextLayout(s, f, frc);
    Rectangle2D r = textTl.getBounds();
    FontMetrics fm = g2.getFontMetrics(f);
    Shape outline = textTl.getOutline(
        AffineTransform.getTranslateInstance((bounds.getWidth() - fm.stringWidth("!")) / 2.0d,
            (bounds.getHeight() + r.getHeight()) / 2.0d));

    Area a = new Area(error);
    a.subtract(new Area(outline));
    return a;
  }

  @Override
  public void paintComponent(Graphics g) {
    Graphics2D g2 = (Graphics2D) g;
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    if (frame != null) {
      double len = (((double) frame.getStackSize()) / frame.getMaxStackSize()) * getWidth();
      if (len > 0) {
        Rectangle2D rect = new Rectangle2D.Double(0, 0, len, getHeight());
        Color old = g2.getColor();
        g2.setColor(STACK_COLOR);
        g2.fill(rect);
        g2.setColor(old);
      }
    }

    super.paintComponent(g2);

    if (hasError) {
      Shape errorShape = getErrorShape(g2);
      double inset = 2d;
      double size = Math.min(getWidth(), getHeight()) - 2d * inset;
      double scale = size / errorShape.getBounds2D().getWidth();
      // We need to scale down the shape to fit within the bounds of this label component (and after
      // the label itself)... Order of affine transformation matters, of course.
      g2.transform(AffineTransform.getTranslateInstance(
          inset + g2.getFontMetrics(getFont()).stringWidth(getText()), inset));
      g2.transform(AffineTransform.getScaleInstance(scale, scale));
      g2.setColor(Color.RED);
      g2.fill(errorShape);
    }
  }
}
