package pac.agent.gui.renderers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import pac.agent.gui.editors.InstructionCellEditor;
import pac.agent.gui.models.BytecodeTableModel;
import pac.agent.gui.tables.BytecodeTable;
import pac.org.objectweb.asm.tree.AbstractInsnNode;
import pac.org.objectweb.asm.tree.IincInsnNode;
import pac.org.objectweb.asm.tree.JumpInsnNode;
import pac.org.objectweb.asm.tree.LabelNode;
import pac.org.objectweb.asm.tree.LdcInsnNode;
import pac.org.objectweb.asm.tree.LookupSwitchInsnNode;
import pac.org.objectweb.asm.tree.MethodInsnNode;
import pac.org.objectweb.asm.tree.TableSwitchInsnNode;
import pac.util.AsmUtils;

/**
 * This cell render operates on AbstractInsnNode's (with non-negative 
 * opcodes).  LabeNode's are color-coded, and assigned names in a
 * lexicologically first fashion.
 * 
 * @author jeikenberry
 */
public class InstructionCellRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = -3594036403655051846L;

    private static Color SELECTED_COLOR = new Color(0xb8, 0xcf, 0xe5, 128);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        AbstractInsnNode insnNode = (AbstractInsnNode) value;
        setBackground(isSelected ? SELECTED_COLOR : Color.white);
        setBorder(hasFocus ? BorderFactory.createLineBorder(Color.BLACK) : null);
        if (insnNode == null) {
            setText("");
        } else {
            String str = AsmUtils.toString(insnNode);
            if (column == 3 && insnNode instanceof JumpInsnNode) {
                insnNode = ((JumpInsnNode) insnNode).label;
            }
            setForeground(Color.black);
            setFont(getFont().deriveFont(insnNode instanceof LdcInsnNode && column > 2 ? Font.ITALIC : Font.BOLD));
            if (insnNode instanceof LabelNode) {
                LabelNode label = (LabelNode) insnNode;
                StringBuilder buf = new StringBuilder("<html><body>");
                boolean isLabelDef = (table instanceof BytecodeTable) && column == 1;
                buf.append(getLabelString(label, isLabelDef));
                if (isLabelDef) {
                    for (LabelNode alias : ((BytecodeTableModel) ((BytecodeTable) table).getModel())
                            .getLabelAliases(label)) {
                        String aliasStr = getLabelString(alias, true);
                        if (aliasStr != null) {
                            buf.append("&nbsp;");
                            buf.append(aliasStr);
                        }
                    }
                }
                buf.append("</body></html>");
                setText(buf.toString());
                return this;
            } else if (column == 3) {
                if (insnNode instanceof LookupSwitchInsnNode) {
                    setText(getSwitchString((LookupSwitchInsnNode) insnNode));
                    return this;
                } else if (insnNode instanceof TableSwitchInsnNode) {
                    setText(getSwitchString((TableSwitchInsnNode) insnNode));
                    return this;
                }
            }

            if (str == null) {
                setText("<null>");
                return this;
            }

            int idx = str.indexOf(' ');
            if (column == 3) {
                if (idx < 0) {
                    setText("");
                } else {
                    str = str.substring(idx + 1);
                    if (insnNode instanceof IincInsnNode) {
                        String[] params = str.trim().split("\\s+");
                        str = "reg: " + params[0] + "  inc: " + params[1];
                    } else if (insnNode instanceof MethodInsnNode) {
                        str = str.replaceAll("\\s+", "");
                    }
                    setText(str);
                }
            } else {
                if (idx < 0)
                    setText(str);
                else
                    setText(str.substring(0, idx));
            }
        }
        return this;
    }

    /**
     * Convert's a LookupSwitchInsnNode into an HTML-formatted string.
     * 
     * @param switchNode LookupSwitchInsnNode
     * @return String
     */
    public static String getSwitchString(LookupSwitchInsnNode switchNode) {
        return getSwitchString(switchNode.keys, switchNode.labels, switchNode.dflt);
    }

    /**
     * Convert's a TableSwitchInsnNode into an HTML-formatted string.
     * 
     * @param switchNode TableSwitchInsnNode
     * @return String
     */
    public static String getSwitchString(TableSwitchInsnNode switchNode) {
        List<Integer> keys = new LinkedList<Integer>();
        for (int i = switchNode.min; i <= switchNode.max; i++)
            keys.add(i);
        return getSwitchString(keys, switchNode.labels, switchNode.dflt);
    }

    /**
     * Convert's a switch instruction into an HTML-formatted string.
     * 
     * @param keys List&lt;Integer&gt;
     * @param labels List&lt;LabelNode&gt;
     * @param defaultLabel LabelNode
     * @return String
     */
    private static String getSwitchString(List<Integer> keys, List<LabelNode> labels, LabelNode defaultLabel) {
        StringBuilder str = new StringBuilder("<html>");
        Iterator<LabelNode> labelIter = labels.iterator();
        for (int key : keys) {
            LabelNode nextLabel = labelIter.next();
            str.append(key);
            str.append(": ");
            str.append(getLabelString(nextLabel, false));
            str.append(" ");
        }
        if (defaultLabel != null) {
            str.append("default: ");
            str.append(getLabelString(defaultLabel, false));
        }
        str.append("</html>");
        return str.toString();
    }

    /**
     * Converts a LabelNode into an HTML-formatted string.
     * 
     * @param label LabelNode
     * @param standalone indicates that this LabelNode is by itself
     *  and not part of a jump or switch.
     * @return String
     */
    public static String getLabelString(LabelNode label, boolean standalone) {
        StringBuilder str = new StringBuilder();
        Color labelColor = InstructionCellEditor.getLabelColor(label);
        if (labelColor == null) {
            String labelName = InstructionCellEditor.getLabelName(label);
            if (labelName != null)
                str.append(labelName);
            else
                return null;
        } else {
            str.append("<cite style='color: rgb(");
            str.append(labelColor.getRed());
            str.append(",");
            str.append(labelColor.getGreen());
            str.append(",");
            str.append(labelColor.getBlue());
            str.append(")'>");
            str.append(InstructionCellEditor.getLabelName(label));
            if (standalone)
                str.append(':');
            str.append("</cite>");
        }
        return str.toString();
    }
}
