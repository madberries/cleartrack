package pac.agent.gui.tables;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import pac.agent.gui.LabelChangeListener;
import pac.agent.gui.editors.InstructionCellEditor;
import pac.agent.gui.editors.LabelCellEditor;
import pac.agent.gui.editors.OpcodeCellEditor;
import pac.agent.gui.models.BytecodeTableModel;
import pac.agent.gui.models.Jump;
import pac.agent.gui.renderers.InstructionCellRenderer;
import pac.agent.gui.renderers.OffsetCellRenderer;
import pac.org.objectweb.asm.tree.ClassNode;
import pac.org.objectweb.asm.tree.JumpInsnNode;
import pac.org.objectweb.asm.tree.LabelNode;
import pac.org.objectweb.asm.tree.MethodNode;

/**
 * The JTable for viewing and editing the bytecode of a given MethodNode.
 * 
 * @author jeikenberry
 */
public class BytecodeTable extends JTable implements MouseListener {
    private static final long serialVersionUID = -4976246421543219590L;

    private TableCellEditor[] cellEditors = new TableCellEditor[] { null, new LabelCellEditor(), new OpcodeCellEditor(),
            new InstructionCellEditor() };

    private TableCellRenderer[] cellRenderers = new TableCellRenderer[] { new OffsetCellRenderer(),
            new InstructionCellRenderer() };

    /**
     * Constructs a new BytecodeTable.
     * 
     * @param classNode ClassNode of the class loaded into the editor
     */
    public BytecodeTable(ClassNode classNode) {
        super(new BytecodeTableModel(classNode));
        setAutoResizeMode(AUTO_RESIZE_OFF);

        setShowGrid(false);
        getTableHeader().setReorderingAllowed(false);

        BytecodeTableModel tableModel = (BytecodeTableModel) getModel();
        tableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                BytecodeTableModel btm = (BytecodeTableModel) e.getSource();

                // Determine the maximum jump height...
                int height = 0;
                for (Jump jump : btm.getJumps()) {
                    height = Math.max(height, jump.getHeight());
                }

                resize(height);

                invalidate();
                revalidate();
                repaint();
                setVisibleRow(btm.getErrorRow());
            }
        });
        tableModel.addLabelChangeListener((LabelChangeListener) cellEditors[3]);
        getSelectionModel().addListSelectionListener(this);
        addMouseListener(this);
    }

    private void resize(int maxJumpLevels) {
        for (int i = 0; i < getColumnCount(); i++) {
            DefaultTableColumnModel colModel = (DefaultTableColumnModel) getColumnModel();
            TableColumn col = colModel.getColumn(i);
            int width = 0;

            TableCellRenderer renderer = col.getHeaderRenderer();
            for (int r = 0; r < getRowCount(); r++) {
                renderer = getCellRenderer(r, i);
                Component comp = renderer.getTableCellRendererComponent(BytecodeTable.this, getValueAt(r, i), false,
                                                                        false, r, i);
                width = Math.max(width, comp.getPreferredSize().width);
            }
            if (i == 0)
                col.setPreferredWidth(width + 22 + maxJumpLevels * 10);
            else
                col.setPreferredWidth(width + 22);
        }
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        if (column == 0) {
            return cellRenderers[0];
        }
        return cellRenderers[1];
    }

    @Override
    public TableCellEditor getCellEditor(int row, int column) {
        if (column >= cellEditors.length || cellEditors[column] == null) {
            return super.getCellEditor(row, column);
        } else {
            return cellEditors[column];
        }
    }

    /**
     * @return true iff changes have been made to the underlying 
     * 	MethodNode's bytecode.
     */
    public boolean isModified() {
        return ((BytecodeTableModel) getModel()).isModified();
    }

    /**
     * Load's a new method to edit from the loaded class.
     * 
     * @param methodNode MethodNode
     */
    public void setMethod(MethodNode methodNode) {
        ((BytecodeTableModel) getModel()).setMethod(methodNode);
    }

    /**
     * @return MethodNode that we are editing.
     */
    public MethodNode getMethod() {
        return ((BytecodeTableModel) getModel()).getMethod();
    }

    /**
     * Makes the row at the specified index visible, by adjusting
     * the scroll's viewport.  The viewport is left unmodified if
     * either row is negative or it is already visible in the
     * viewport.
     * 
     * @param row int of the row to make visible.
     */
    private void setVisibleRow(int row) {
        if (row < 0)
            return;
        // Get the visible region, and the region of the newly
        // selected row.
        JViewport viewport = (JViewport) this.getParent();
        Rectangle rect = this.getCellRect(row, 0, true);
        Rectangle r2 = viewport.getVisibleRect();
        Point pt = viewport.getViewPosition();
        Rectangle rectCopy = new Rectangle(rect);
        rectCopy.setLocation(rect.x - pt.x, rect.y - pt.y);

        // Scroll to the selected region, only if it's not visible...
        if (!new Rectangle(viewport.getExtentSize()).contains(rectCopy)) {
            this.scrollRectToVisible(new Rectangle(rect.x, rect.y, (int) r2.getWidth(), (int) r2.getHeight()));
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // On double clicks, jump to the selected label (for jump instructions).
        int selected = getSelectedRow();
        if (e.getClickCount() == 2 && selected >= 0) {
            BytecodeTableModel btm = (BytecodeTableModel) getModel();
            Object value = btm.getValueAt(selected, 2);
            if (!(value instanceof JumpInsnNode))
                return;

            // Acquire the row for the label to jump to, and select
            // that row.
            LabelNode gotoLabel = ((JumpInsnNode) value).label;
            int labelRow = btm.getRowOf(gotoLabel);
            ListSelectionModel selectionModel = getSelectionModel();
            selectionModel.clearSelection();
            selectionModel.setSelectionInterval(labelRow, labelRow);

            setVisibleRow(this.getSelectedRow());
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        super.paintComponent(g2);

        // Find the start offset (based on the text length of the last row).
        OffsetCellRenderer renderer = (OffsetCellRenderer) getCellRenderer(getModel().getRowCount() - 1, 0);
        int start = renderer.getFontMetrics(renderer.getFont()).stringWidth("" + (getModel().getRowCount() - 1));

        // Draw all of the branches over the already rendered table.
        BytecodeTableModel btm = (BytecodeTableModel) getModel();
        for (Jump jump : btm.getJumps()) {
            jump.draw(g2, this, 1, start);
        }
    }
}
