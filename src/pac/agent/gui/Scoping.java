package pac.agent.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import pac.agent.gui.models.BytecodeTableModel;
import pac.agent.gui.models.ExceptionHandlerTableModel;
import pac.agent.gui.models.LocalVariableTableModel;
import pac.agent.gui.tables.ExceptionHandlerTable;
import pac.agent.gui.tables.LocalVariableTable;
import pac.org.objectweb.asm.tree.MethodNode;

/**
 * Represents the view for manipulating the scoped types behind the current MethodNode (i.e
 * exceptions, locals, etc...).
 * 
 * @author jeikenberry
 */
public class Scoping extends JPanel {
  private static final long serialVersionUID = -3969596806802934090L;

  private LocalVariableTable localsTable;
  private ExceptionHandlerTable exceptionsTable;

  public Scoping(BytecodeTableModel bytecodeTableModel) {
    super(new GridLayout(2, 1));

    JPanel panel1 = new JPanel(new BorderLayout());
    localsTable = new LocalVariableTable(bytecodeTableModel);
    JScrollPane localsScrollPane = new JScrollPane(localsTable);
    panel1.setBorder(BorderFactory.createTitledBorder("Local Variables"));
    panel1.add(localsScrollPane, BorderLayout.CENTER);

    JPanel panel2 = new JPanel(new BorderLayout());
    exceptionsTable = new ExceptionHandlerTable(bytecodeTableModel);
    JScrollPane exceptionsScrollPane = new JScrollPane(exceptionsTable);
    panel2.setBorder(BorderFactory.createTitledBorder("Exception Handlers"));
    panel2.add(exceptionsScrollPane, BorderLayout.CENTER);

    add(panel1);
    add(panel2);
  }

  public void setMethod(MethodNode methodNode) {
    ((LocalVariableTableModel) localsTable.getModel()).setLocalVariables(methodNode.localVariables);
    ((ExceptionHandlerTableModel) exceptionsTable.getModel())
        .setExceptionHandlers(methodNode.tryCatchBlocks);
  }

  public boolean isModified() {
    return ((LocalVariableTableModel) localsTable.getModel()).isModified()
        || ((ExceptionHandlerTableModel) exceptionsTable.getModel()).isModified();
  }
}
