package pac.agent.gui.renderers;

import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * The cell renderer used for the Analysis tree viewer.
 * 
 * @author jeikenberry
 */
public class AnalysisCellRenderer extends DefaultTreeCellRenderer {
  private static final long serialVersionUID = 6878870499141748515L;

  @Override
  public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
      boolean expanded, boolean leaf, int row, boolean hasFocus) {
    Component component =
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
    Node node = (Node) value;

    // Output the XML node as HTML.
    StringBuilder buf = new StringBuilder();
    buf.append("<html><body>");
    buf.append("<b>");
    buf.append("&lt;");
    buf.append(node.getNodeName());
    buf.append("</b>");
    NamedNodeMap attributes = node.getAttributes();
    if (attributes != null) {
      for (int i = 0; i < attributes.getLength(); i++) {
        buf.append(' ');
        Node attribute = attributes.item(i);
        buf.append(attribute.getNodeName());
        buf.append('=');
        buf.append("<font color='" + (sel ? "#FF00FF" : "#00FF00") + // Invert the color, if
                                                                     // selected
            "'>");
        buf.append('\"');
        buf.append(attribute.getNodeValue());
        buf.append('\"');
        buf.append("</font>");
      }
    }
    buf.append("<b>");
    if (leaf)
      buf.append("/&gt;");
    else
      buf.append("&gt;");
    buf.append("</b>");
    buf.append("</body></html>");

    setText(buf.toString());
    setIcon(null); // Turn off icons.

    return component;
  }
}
