package pac.agent.gui.models;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import pac.org.objectweb.asm.tree.LabelNode;
import pac.org.objectweb.asm.tree.TryCatchBlockNode;

/**
 * The model representing all of the try-catch blocks of a MethodNode.
 * 
 * @author jeikenberry
 */
public class ExceptionHandlerTableModel extends AbstractTableModel {
  private static final long serialVersionUID = 900763085819100353L;

  /**
   * List of TryCatchBlockNode objects associated with this model.
   */
  private List<TryCatchBlockNode> exceptionHandlers;

  /**
   * true iff at least one exception has been altered.
   */
  private boolean modified;

  @Override
  public int getRowCount() {
    return exceptionHandlers == null ? 0 : exceptionHandlers.size();
  }

  @Override
  public int getColumnCount() {
    return 4;
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return true;
  }

  @Override
  public String getColumnName(int column) {
    switch (column) {
      case 0:
        return "Type";
      case 1:
        return "Start";
      case 2:
        return "End";
      case 3:
        return "Handler";
    }
    return null;
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    switch (columnIndex) {
      case 0:
        return String.class;
      default:
        return LabelNode.class;
    }
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    switch (columnIndex) {
      case 0:
        return exceptionHandlers.get(rowIndex).type;
      case 1:
        return exceptionHandlers.get(rowIndex).start;
      case 2:
        return exceptionHandlers.get(rowIndex).end;
      case 3:
        return exceptionHandlers.get(rowIndex).handler;
    }
    return null;
  }

  @Override
  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    TryCatchBlockNode catchBlock = exceptionHandlers.get(rowIndex);
    switch (columnIndex) {
      case 0:
        if (aValue.equals(catchBlock.type))
          return;
        catchBlock.type = aValue.equals("") ? null : (String) aValue;
        break;
      case 1:
        if (catchBlock.start == aValue)
          return;
        catchBlock.start = (LabelNode) aValue;
        break;
      case 2:
        if (catchBlock.end == aValue)
          return;
        catchBlock.end = (LabelNode) aValue;
        break;
      case 3:
        if (catchBlock.handler == aValue)
          return;
        catchBlock.handler = (LabelNode) aValue;
        break;
    }
    modified = true;
    fireTableDataChanged();
  }

  /**
   * Load the exception handlers into the model.
   * 
   * @param exceptionHandlers List&lt;TryCatchBlockNode&gt;
   */
  public void setExceptionHandlers(List<TryCatchBlockNode> exceptionHandlers) {
    this.exceptionHandlers = exceptionHandlers;
    fireTableDataChanged();
  }

  /**
   * @return true iff the model has been altered
   */
  public boolean isModified() {
    return modified;
  }
}
