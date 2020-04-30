package pac.agent.gui.models;

import java.util.List;

import javax.swing.table.AbstractTableModel;

import pac.org.objectweb.asm.tree.LabelNode;
import pac.org.objectweb.asm.tree.LocalVariableNode;

/**
 * The model represent the local variable table of a MethodNode.
 * 
 * @author jeikenberry
 */
public class LocalVariableTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 7242772088177974796L;

    /**
     * List of LocalVariableNode objects associated with this model.
     */
    private List<LocalVariableNode> localVars;

    /**
     * true iff at least one exception has been altered.
     */
    private boolean modified;

    @Override
    public int getRowCount() {
        return localVars == null ? 0 : localVars.size();
    }

    @Override
    public int getColumnCount() {
        return 6;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    @Override
    public String getColumnName(int column) {
        switch (column) {
        case 0:
            return "Index";
        case 1:
            return "Name";
        case 2:
            return "Type";
        case 3:
            return "Start";
        case 4:
            return "End";
        case 5:
            return "Signature";
        }
        return null;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
        case 3:
        case 4:
            return LabelNode.class;
        default:
            return String.class;
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
        case 0:
            return localVars.get(rowIndex).index;
        case 1:
            return localVars.get(rowIndex).name;
        case 2:
            return localVars.get(rowIndex).desc;
        case 3:
            return localVars.get(rowIndex).start;
        case 4:
            return localVars.get(rowIndex).end;
        case 5:
            return localVars.get(rowIndex).signature;
        }
        return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        LocalVariableNode localVar = localVars.get(rowIndex);
        switch (columnIndex) {
        case 0:
            int tmp = Integer.parseInt((String) aValue);
            if (localVar.index == tmp)
                return;
            localVar.index = tmp;
            break;
        case 1:
            if (localVar.name.equals(aValue))
                return;
            localVar.name = (String) aValue;
            break;
        case 2:
            if (localVar.desc.equals(aValue))
                return;
            localVar.desc = (String) aValue;
            break;
        case 3:
            if (localVar.start == aValue)
                return;
            localVar.start = (LabelNode) aValue;
            break;
        case 4:
            if (localVar.end == aValue)
                return;
            localVar.end = (LabelNode) aValue;
            break;
        case 5:
            if (aValue != null && aValue.toString().equals(""))
                aValue = null;
            if (localVar.signature == aValue || (localVar.signature != null && localVar.signature.equals(aValue)))
                return;
            localVar.signature = (String) aValue;
            break;
        }
        modified = true;
        fireTableDataChanged();
    }

    /**
     * Load the local variable table into the model.
     * 
     * @param exceptionHandlers List&lt;LocalVariableNode&gt;
     */
    public void setLocalVariables(List<LocalVariableNode> localVars) {
        this.localVars = localVars;
        fireTableDataChanged();
    }

    /**
     * @return true iff the model has been altered
     */
    public boolean isModified() {
        return modified;
    }
}
