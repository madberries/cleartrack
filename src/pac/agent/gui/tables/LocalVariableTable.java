package pac.agent.gui.tables;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import pac.agent.gui.editors.InstructionCellEditor;
import pac.agent.gui.models.BytecodeTableModel;
import pac.agent.gui.models.LocalVariableTableModel;
import pac.agent.gui.renderers.InstructionCellRenderer;

/**
 * The JTable for editing and viewing the local variable table.
 * 
 * @author jeikenberry
 */
public class LocalVariableTable extends JTable {
    private static final long serialVersionUID = 8525229137696556317L;

    private TableCellEditor[] cellEditors = new TableCellEditor[] { new DefaultCellEditor(new JTextField()),
            new InstructionCellEditor() };
    private TableCellRenderer[] cellRenderers = new TableCellRenderer[] { new DefaultTableCellRenderer() {
        private static final long serialVersionUID = -3660562607353788207L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value == null)
                setText("<null>");
            return comp;
        }
    }, new InstructionCellRenderer() };

    public LocalVariableTable(BytecodeTableModel bytecodeTableModel) {
        super(new LocalVariableTableModel());

        TableColumnModel cm = getColumnModel();
        cm.getColumn(0).setMaxWidth(50);
        cm.getColumn(1).setMinWidth(100);
        cm.getColumn(2).setMinWidth(200);
        cm.getColumn(3).setMinWidth(50);
        cm.getColumn(3).setMaxWidth(75);
        cm.getColumn(4).setMinWidth(50);
        cm.getColumn(4).setMaxWidth(75);

        setGridColor(new Color(224, 224, 224));
        getTableHeader().setReorderingAllowed(false);

        LocalVariableTableModel tableModel = (LocalVariableTableModel) getModel();
        tableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                invalidate();
                revalidate();
                repaint();
            }
        });
        bytecodeTableModel.addLabelChangeListener((InstructionCellEditor) cellEditors[1]);

        setVisibleRowCount(8);
    }

    /**
     * Adjust the size of this table to fit a specified number of rows.
     * 
     * @param rows int of the rows to fit.
     */
    public void setVisibleRowCount(int rows) {
        int height = 0;
        for (int row = 0; row < rows; row++)
            height += getRowHeight(row);

        setPreferredScrollableViewportSize(new Dimension(getPreferredScrollableViewportSize().width, height));
    }

    @Override
    public TableCellEditor getCellEditor(int row, int column) {
        if (column == 3 || column == 4)
            return cellEditors[1];
        return cellEditors[0];
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        if (column == 3 || column == 4)
            return cellRenderers[1];
        return cellRenderers[0];
    }
}
