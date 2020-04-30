package pac.agent.gui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FileDialog;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import pac.agent.gui.models.BytecodeTableModel;
import pac.agent.gui.tables.BytecodeTable;
import pac.org.objectweb.asm.ClassReader;
import pac.org.objectweb.asm.ClassVisitor;
import pac.org.objectweb.asm.ClassWriter;
import pac.org.objectweb.asm.Opcodes;
import pac.org.objectweb.asm.tree.ClassNode;
import pac.org.objectweb.asm.tree.MethodNode;
import pac.org.objectweb.asm.tree.analysis.BasicValue;
import pac.org.objectweb.asm.tree.analysis.Frame;
import pac.util.OS;

//#if issubstr("Mac", os_type)
import com.apple.eawt.AppEvent.QuitEvent;
import com.apple.eawt.Application;
import com.apple.eawt.QuitHandler;
import com.apple.eawt.QuitResponse;
//#endif

/**
 * This class contains both the main entry point for manipulating/editing
 * class files, and also responsible for creating the Frame for the editor.
 * 
 * @author jeikenberry
 */
public class ClassEditor extends JFrame implements ListSelectionListener, ActionListener {
    private static final long serialVersionUID = -3586256018528152012L;

    // Lock to prevent the ClassWriter from writing the bytes, before
    // we even have a chance to make modifications.
    private static Object lock = new Object();
    private static boolean shouldSave = false;

    private ClassNode classNode;
    private boolean save;

    // Menu items
    private JMenuItem deleteMenuItem, insertMenuItem, printMenuItem, closeMenuItem, previewMenuItem, exportMenuItem;
    private ButtonGroup methodGroup;

    // Preivew dialog
    private PrintPreview printPreview;

    // Main view
    private BytecodeTable bytecodeTable;

    // Tabbed views
    private StackFrames stackFrames;
    private Scoping scoping;
    private Analysis analysis;

    static {
        // Set OS specific UI properties...
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (OS.get().isMac()) {
            System.setProperty("apple.awt.graphics.EnableQ2DX", "true");
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Class Editor");
        }
    }

    /**
     * Create a new ClassEditor with the first MethodNode selected (by
     * default).
     * 
     * @param classNode ClassNode to edit
     */
    public ClassEditor(ClassNode classNode) {
        this(classNode, classNode.methods.get(0));
    }

    /**
     * Create a new ClassEditor, loading defaultMethod into the editor 
     * view (by default).
     * 
     * @param classNode ClassNode to edit
     * @param defaultMethod MethodNode belonging to classNode
     */
    public ClassEditor(ClassNode classNode, MethodNode defaultMethod) {
        super();

        // Add callback for Mac OSX specific "Quit" menu item.
        //#if issubstr("Mac", os_type)
        if (OS.get().isMac()) {
            Application macApplication = Application.getApplication();
            macApplication.setDockIconBadge(null);
            macApplication.setQuitHandler(new QuitHandler() {
                @Override
                public void handleQuitRequestWith(QuitEvent event, QuitResponse response) {
                    // Do not invoke response.performQuit() otherwise the
                    // application will close prematurely, before writing
                    // out the bytecode.
                    response.cancelQuit();
                    close();
                }
            });
        }
        //#endif

        this.classNode = classNode;

        setSize(800, 1000);

        // Setup window listener to close application properly.
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                close();
            }
        });

        bytecodeTable = new BytecodeTable(classNode);
        stackFrames = new StackFrames();
        scoping = new Scoping((BytecodeTableModel) bytecodeTable.getModel());

        // Add key binding for deleting selected rows with backspace key...
        InputMap inputMap = bytecodeTable.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = bytecodeTable.getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "deleteRow");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteRow");
        actionMap.put("deleteRow", new AbstractAction() {
            private static final long serialVersionUID = 6443690178352560065L;

            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedRows();
            }
        });
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                     "nextMethod");
        actionMap.put("nextMethod", new AbstractAction() {
            private static final long serialVersionUID = 6443690178352560065L;

            @Override
            public void actionPerformed(ActionEvent e) {
                nextMethod();
            }
        });
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
                     "prevMethod");
        actionMap.put("prevMethod", new AbstractAction() {
            private static final long serialVersionUID = 6443690178352560065L;

            @Override
            public void actionPerformed(ActionEvent e) {
                previousMethod();
            }
        });

        // Setup a tabbed pane for any method extras (i.e. stacks, locals,
        // etc...)
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Stack Frames", stackFrames);
        tabbedPane.add("Scopes", scoping);
        try {
            analysis = new Analysis(classNode);
            tabbedPane.add("Analysis", new JScrollPane(analysis));
            if (!analysis.foundAnalysis()) {
                tabbedPane.setEnabledAt(2, false);
                tabbedPane.setToolTipTextAt(2, "No analysis file found");
            }
        } catch (ParserConfigurationException | SAXException | IOException e1) {
            tabbedPane.add("Analysis", new JScrollPane(analysis));
            tabbedPane.setEnabledAt(2, false);
            tabbedPane.setToolTipTextAt(2, "Error parsing analysis file");
        }

        // If there was analysis for this class, let's make this the
        // default selected tab...
        if (analysis.foundAnalysis()) {
            tabbedPane.setSelectedIndex(2);
        }

        // Add the views to this frame's content pane
        getContentPane().add(new JScrollPane(bytecodeTable), BorderLayout.CENTER);
        getContentPane().add(tabbedPane, BorderLayout.SOUTH);

        // Initialize menubar
        initMenus(defaultMethod);

        bytecodeTable.getSelectionModel().addListSelectionListener(this);
    }

    /**
     * Initialize the menubar.  On Mac OSX, the menus will be displayed in the
     * OS menubar and not the default Swing menubar.
     * 
     * @param defaultMethod MethodNode of the method to be selected by default
     */
    private void initMenus(MethodNode defaultMethod) {
        JMenuBar menubar = new JMenuBar();

        // ****************************************
        // Setup the File menu
        // ****************************************
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');
        exportMenuItem = new JMenuItem("Export as HTML...");
        exportMenuItem.setMnemonic('x');
        exportMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E,
                                                             Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        exportMenuItem.addActionListener(this);
        fileMenu.add(exportMenuItem);
        fileMenu.add(new JSeparator());
        printMenuItem = new JMenuItem("Print...");
        printMenuItem.setMnemonic('P');
        printMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P,
                                                            Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        printMenuItem.addActionListener(this);
        fileMenu.add(printMenuItem);
        previewMenuItem = new JMenuItem("Print Preview...");
        previewMenuItem.setMnemonic('v');
        previewMenuItem.addActionListener(this);
        fileMenu.add(previewMenuItem);
        if (!OS.get().isMac()) { // mac has it's own "Quit"
            fileMenu.add(new JSeparator());
            closeMenuItem = new JMenuItem("Quit");
            closeMenuItem.setMnemonic('Q');
            closeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
                                                                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
            closeMenuItem.addActionListener(this);
            fileMenu.add(closeMenuItem);
        }
        menubar.add(fileMenu);

        // ****************************************
        // Setup the Edit menu
        // ****************************************
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic('E');
        insertMenuItem = new JMenuItem("Insert");
        insertMenuItem.setMnemonic('I');
        insertMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I,
                                                             Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        insertMenuItem.addActionListener(this);
        insertMenuItem.setEnabled(false);
        editMenu.add(insertMenuItem);
        deleteMenuItem = new JMenuItem("Delete");
        deleteMenuItem.setMnemonic('D');
        deleteMenuItem.addActionListener(this);
        deleteMenuItem.setEnabled(false);
        editMenu.add(deleteMenuItem);
        menubar.add(editMenu);

        // ****************************************
        // Setup the Methods menu
        // ****************************************
        JMenu methodsMenu = new JMenu("Methods");
        methodGroup = new ButtonGroup();
        methodsMenu.setMnemonic('M');

        // Sort the methods alphabetically, for easier navigation
        Set<MethodNode> sortedMethods = new TreeSet<MethodNode>(new Comparator<MethodNode>() {

            @Override
            public int compare(MethodNode method1, MethodNode method2) {
                int tmp = method1.name.compareTo(method2.name);
                if (tmp != 0)
                    return tmp;
                pac.org.objectweb.asm.Type[] args1 = pac.org.objectweb.asm.Type.getArgumentTypes(method1.desc);
                pac.org.objectweb.asm.Type[] args2 = pac.org.objectweb.asm.Type.getArgumentTypes(method2.desc);
                if (args1.length != args2.length)
                    return args1.length < args2.length ? -1 : 1;
                for (int i = 0; i < args1.length; i++) {
                    tmp = args1[i].toString().compareTo(args2[i].toString());
                    if (tmp != 0)
                        return tmp;
                }
                pac.org.objectweb.asm.Type returnType1 = pac.org.objectweb.asm.Type.getReturnType(method1.desc);
                pac.org.objectweb.asm.Type returnType2 = pac.org.objectweb.asm.Type.getReturnType(method2.desc);
                return returnType1.toString().compareTo(returnType2.toString());
            }
        });
        sortedMethods.addAll(classNode.methods);

        for (MethodNode methodNode : sortedMethods) {
            MethodMenuItem methodMenuItem = new MethodMenuItem(methodNode);

            // Select the default method as the selection method
            if (methodNode == defaultMethod) {
                methodMenuItem.setSelected(true);
                setMethod(methodNode);
            }

            methodMenuItem.addActionListener(this);

            methodGroup.add(methodMenuItem);
            methodsMenu.add(methodMenuItem);
        }

        // If not on mac, then we need to scroll this menu since the class
        // may have a lot of methods
        if (!OS.get().isMac()) {
            MenuScroller.setScrollerFor(methodsMenu, 40, 75, 0, 0);
        }

        menubar.add(methodsMenu);

        setJMenuBar(menubar);
    }

    /**
     * Sets the current methodNode to be edited. Calls to this will update all
     * dependent editors/components.
     * 
     * @param methodNode MethodNode
     */
    public void setMethod(MethodNode methodNode) {
        setTitle(classNode.name + "." + methodNode.name + methodNode.desc);

        // update all views...
        bytecodeTable.setMethod(methodNode);
        stackFrames.setMethod(methodNode);
        scoping.setMethod(methodNode);
        analysis.setMethod(methodNode);

        // check the correct method menu item
        Enumeration<AbstractButton> methodEnum = methodGroup.getElements();
        while (methodEnum.hasMoreElements()) {
            MethodMenuItem methodMenuItem = (MethodMenuItem) methodEnum.nextElement();
            if (methodMenuItem.getMethodNode() == methodNode) {
                methodMenuItem.setSelected(true);
                break;
            }
        }
    }

    /**
     * Load the next method from the current method to be the newly
     * edited method.
     */
    public void nextMethod() {
        String methodStr = methodGroup.getSelection().getActionCommand();
        Enumeration<AbstractButton> methodEnum = methodGroup.getElements();
        boolean foundCurrent = false;
        while (methodEnum.hasMoreElements()) {
            MethodMenuItem methodMenuItem = (MethodMenuItem) methodEnum.nextElement();
            MethodNode methodNode = methodMenuItem.getMethodNode();
            if (foundCurrent) {
                setMethod(methodNode);
                return;
            }
            if (methodStr.equals(methodNode.name + methodNode.desc)) {
                foundCurrent = true;
            }
        }
    }

    /**
     * Load the previous method from the current method to be the newly
     * edited method.
     */
    public void previousMethod() {
        String methodStr = methodGroup.getSelection().getActionCommand();
        Enumeration<AbstractButton> methodEnum = methodGroup.getElements();
        MethodNode prevMethodNode = null;
        while (methodEnum.hasMoreElements()) {
            MethodMenuItem methodMenuItem = (MethodMenuItem) methodEnum.nextElement();
            MethodNode methodNode = methodMenuItem.getMethodNode();
            if (methodStr.equals(methodNode.name + methodNode.desc)) {
                if (prevMethodNode != null)
                    setMethod(prevMethodNode);
                return;
            }
            prevMethodNode = methodNode;
        }
    }

    /**
     * Deletes the selected instructions from the current MethodNode object.
     */
    public void deleteSelectedRows() {
        if (bytecodeTable.getSelectedColumn() != 0 || bytecodeTable.getSelectedColumnCount() > 1)
            return;
        int[] selected = bytecodeTable.getSelectedRows();
        if (selected == null || selected.length == 0)
            return;
        Arrays.sort(selected);
        int first = selected[0];
        ((BytecodeTableModel) bytecodeTable.getModel()).removeRows(selected);
        ListSelectionModel selectionModel = bytecodeTable.getSelectionModel();
        selectionModel.clearSelection();
        selectionModel.addSelectionInterval(first, first);
        bytecodeTable.getColumnModel().getSelectionModel().addSelectionInterval(0, 0);
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        // Enable/Disable insert and edit menu items, depending on
        // whether at least one row is selected or not.
        int row = bytecodeTable.getSelectedRow();
        Frame<BasicValue> frame = null;
        if (row > 0)
            frame = ((BytecodeTableModel) bytecodeTable.getModel()).getFrameAt(row);
        stackFrames.setFrame(frame);
        boolean rowSelected = bytecodeTable.getSelectedRowCount() > 0;
        deleteMenuItem.setEnabled(rowSelected);
        insertMenuItem.setEnabled(rowSelected);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == null)
            return;

        if (src == insertMenuItem) {
            int row = bytecodeTable.getSelectedRow();
            ((BytecodeTableModel) bytecodeTable.getModel()).insertNewInstruction(row);
            ListSelectionModel selectionModel = bytecodeTable.getSelectionModel();
            selectionModel.clearSelection();
            selectionModel.addSelectionInterval(row, row);
            bytecodeTable.getColumnModel().getSelectionModel().addSelectionInterval(0, 0);
        } else if (src == deleteMenuItem) {
            deleteSelectedRows();
        } else if (src == exportMenuItem) {
            FileDialog fileDialog = new FileDialog(ClassEditor.this, "Save as HTML", FileDialog.SAVE);
            fileDialog.setMultipleMode(false);
            fileDialog.setDirectory(System.getProperty("user.dir"));
            fileDialog.setVisible(true);
            String filename = fileDialog.getFile();
            if (filename != null) {
                if (!filename.endsWith(".html"))
                    filename += ".html";
                try (PrintWriter out = new PrintWriter(new File(filename))) {
                    out.println(((BytecodeTableModel) bytecodeTable.getModel()).toHtml());
                } catch (FileNotFoundException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
            fileDialog.dispose();
        } else if (src == printMenuItem) {
            try {
                PrinterJob prnJob = PrinterJob.getPrinterJob();
                // Have to recreate the printable, after preview has made
                // a pass over the pages...
                prnJob.setPrintable(PrintPreview.getPrintable(bytecodeTable, classNode.name), prnJob.defaultPage());
                if (prnJob.printDialog()) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    prnJob.print();
                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            } catch (PrinterException printEx) {
                // TODO Auto-generated catch block
                printEx.printStackTrace();
            }
        } else if (src == previewMenuItem) {
            if (printPreview == null)
                printPreview = new PrintPreview(bytecodeTable, classNode.name);
            printPreview.preview();
            printPreview.setVisible(true);
        } else if (src == closeMenuItem) {
            close();
        } else if (src instanceof MethodMenuItem) {
            MethodMenuItem methodMenuItem = (MethodMenuItem) src;
            setMethod(methodMenuItem.getMethodNode());
        }
    }

    /**
     * Closes an instance of this editor and subsequently releases block
     * on shouldSave() call.
     * 
     * @return true if the editor was actually close, o.w. false.
     */
    public boolean close() {
        if (bytecodeTable.isModified() || scoping.isModified()) {
            switch (JOptionPane.showConfirmDialog(this, "Bytecode has been modified, do you wish to save?", "Save?",
                                                  JOptionPane.YES_NO_CANCEL_OPTION)) {
            case JOptionPane.CANCEL_OPTION:
                return false;
            case JOptionPane.YES_OPTION:
                save = true;
                break;
            default:
                save = false;
            }
        }
        dispose();
        synchronized (lock) {
            lock.notify();
        }
        return true;
    }

    @Override
    public void dispose() {
        // This clears out the labels, so they can be reused on the next run.
        ((BytecodeTableModel) bytecodeTable.getModel()).setMethod(null);
        super.dispose();
    }

    /**
     * Block on this call until the frame has been disposed of.
     * 
     * @return true if and only if a method's instructions have been
     *   altered.
     */
    public boolean shouldSave() {
        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return save;
    }

    /**
     * @param args
     * @throws IOException
     * @throws FileNotFoundException
     * @throws ClassNotFoundException
     */
    public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {
        if (args.length < 0 || !args[0].endsWith(".class")) {
            System.err.println("USAGE: method_editor <java class file>");
            System.exit(1);
        }
        ClassReader classReader = new ClassReader(new FileInputStream(args[0]));
        ClassWriter classWriter = new ClassWriter(0);
        ClassNode classNode = new MethodEditorClassNode(classWriter);
        classReader.accept(classNode, 0);
        if (shouldSave) {
            byte[] bytes = classWriter.toByteArray();
            FileOutputStream out = new FileOutputStream(args[0]);
            System.out.println("WRITING BYTES");
            out.write(bytes);
            out.close();
        }
    }

    /**
     * ClassNode visitor for editting ClassNode objects graphically
     * 
     * @author jeikenberry
     */
    private static class MethodEditorClassNode extends ClassNode {
        private ClassVisitor cv;

        public MethodEditorClassNode(ClassVisitor cv) {
            super(Opcodes.ASM5);
            this.cv = cv;
        }

        @Override
        public void visitEnd() {
            ClassEditor editor = new ClassEditor(this);
            editor.setVisible(true);
            shouldSave = editor.shouldSave();
            accept(cv);
        }
    }
}
