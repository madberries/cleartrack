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

import pac.agent.gui.LabelChangeListener;
import pac.agent.gui.editors.InstructionCellEditor;
import pac.agent.gui.models.BytecodeTableModel;
import pac.agent.gui.models.ExceptionHandlerTableModel;
import pac.agent.gui.renderers.InstructionCellRenderer;

/**
 * The JTable for editing and viewing the exception handler table.
 * 
 * @author jeikenberry
 */
public class ExceptionHandlerTable extends JTable {
  private static final long serialVersionUID = 8780677064590432047L;

  private TableCellEditor[] cellEditors =
      new TableCellEditor[] {new DefaultCellEditor(new JTextField()), new InstructionCellEditor()};
  private TableCellRenderer[] cellRenderers =
      new TableCellRenderer[] {new DefaultTableCellRenderer() {
        private static final long serialVersionUID = -4851871070647180961L;

        public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
          Component comp =
              super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
          if (value == null)
            setText("<finally>");
          return comp;
        }
      }, new InstructionCellRenderer()};

  public ExceptionHandlerTable(BytecodeTableModel bytecodeTableModel) {
    super(new ExceptionHandlerTableModel());

    TableColumnModel cm = getColumnModel();
    cm.getColumn(0).setMinWidth(275);

    setGridColor(new Color(224, 224, 224));
    getTableHeader().setReorderingAllowed(false);

    ExceptionHandlerTableModel tableModel = (ExceptionHandlerTableModel) getModel();
    tableModel.addTableModelListener(new TableModelListener() {
      @Override
      public void tableChanged(TableModelEvent e) {
        invalidate();
        revalidate();
        repaint();
      }
    });
    bytecodeTableModel.addLabelChangeListener((LabelChangeListener) cellEditors[1]);

    setVisibleRowCount(5);
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

    setPreferredScrollableViewportSize(
        new Dimension(getPreferredScrollableViewportSize().width, height));
  }

  @Override
  public TableCellEditor getCellEditor(int row, int column) {
    if (column == 0)
      return cellEditors[0];
    return cellEditors[1];
  }

  @Override
  public TableCellRenderer getCellRenderer(int row, int column) {
    if (column == 0)
      return cellRenderers[0];
    return cellRenderers[1];
  }
}
