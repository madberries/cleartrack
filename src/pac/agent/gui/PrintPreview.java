package pac.agent.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.text.MessageFormat;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.border.MatteBorder;

import pac.agent.gui.tables.BytecodeTable;
import pac.org.objectweb.asm.tree.MethodNode;

public class PrintPreview extends JDialog implements Runnable {
    private static final long serialVersionUID = -114490467345174267L;

    //	private static final int INCH = 72;
    private static boolean bScallToFitOnePage = false;

    protected JScrollPane displayArea;
    protected int m_wPage;
    protected int m_hPage;
    protected int width;
    protected int height;
    protected JComboBox<String> m_cbScale;
    protected PreviewContainer m_preview;
    protected PageFormat pp_pf = null;
    protected JButton formatButton;
    protected JButton shrinkButton;
    private BytecodeTable byteTable;
    private String className;

    public PrintPreview(BytecodeTable byteTable, String className) {
        this(byteTable, className, "Print Preview");
    }

    private PrintPreview(BytecodeTable byteTable, String className, String title) {
        this(byteTable, className, title, false);
    }

    private PrintPreview(BytecodeTable byteTable, String className, String title, boolean shrink) {
        super(getParentFrame(byteTable), title, true);

        this.byteTable = byteTable;
        this.className = className;

        bScallToFitOnePage = false; // reset to default
        PrinterJob prnJob = PrinterJob.getPrinterJob();
        pp_pf = prnJob.defaultPage();
        if (pp_pf.getHeight() == 0 || pp_pf.getWidth() == 0) {
            System.err.println("Unable to determine default page size");
            return;
        }
        setSize(600, 400);

        displayArea = null;
        m_preview = null;

        JToolBar tb = new JToolBar();
        createButtons(tb, shrink);

        String[] scales = { "10 %", "25 %", "50 %", "100 %" };
        m_cbScale = new JComboBox<String>(scales);
        m_cbScale.setSelectedIndex(1);
        ActionListener lst = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Thread runner = new Thread(PrintPreview.this);
                runner.start();
            }
        };
        m_cbScale.addActionListener(lst);
        m_cbScale.setMaximumSize(m_cbScale.getPreferredSize());
        m_cbScale.setEditable(true);
        tb.addSeparator();
        tb.add(m_cbScale);
        getContentPane().add(tb, BorderLayout.NORTH);
    }

    private static Frame getParentFrame(BytecodeTable byteTable) {
        Component comp;
        for (comp = byteTable.getParent(); comp != null && !(comp instanceof JFrame); comp = comp.getParent())
            ;
        return (JFrame) comp;
    }

    static Printable getPrintable(BytecodeTable byteTable, String className) {
        MethodNode methodNode = byteTable.getMethod();
        return byteTable.getPrintable(javax.swing.JTable.PrintMode.FIT_WIDTH,
                                      new MessageFormat(className + "." + methodNode.name + methodNode.desc),
                                      new MessageFormat("page {0}"));
    }

    protected void getThePreviewPages() {
        m_wPage = (int) (pp_pf.getWidth());
        m_hPage = (int) (pp_pf.getHeight());
        int scale = getDisplayScale();
        width = (int) Math.ceil(m_wPage * scale / 100);
        height = (int) Math.ceil(m_hPage * scale / 100);

        int pageIndex = 0;
        try {
            while (true) {
                BufferedImage img = new BufferedImage(m_wPage, m_hPage, BufferedImage.TYPE_INT_RGB);
                Graphics g = img.getGraphics();
                g.setColor(Color.white);
                g.fillRect(0, 0, m_wPage, m_hPage);
                Printable target = getPrintable(byteTable, className);
                if (bScallToFitOnePage) {
                    target.print(g, pp_pf, -1);
                    PagePreview pp = new PagePreview(width, height, img);
                    m_preview.add(pp);
                    break;
                } else if (target.print(g, pp_pf, pageIndex) != Printable.PAGE_EXISTS)
                    break;
                PagePreview pp = new PagePreview(width, height, img);
                m_preview.add(pp);
                pageIndex++;
            }
        } catch (OutOfMemoryError om) {
            JOptionPane.showMessageDialog(this, "image is too big that run out of memory.", "Print Preview",
                                          JOptionPane.INFORMATION_MESSAGE);
        } catch (PrinterException e) {
            e.printStackTrace();
            System.err.println("Printing error: " + e.toString());
        }
    }

    protected void previewThePages(int orientation) {
        if (displayArea != null)
            displayArea.setVisible(false);

        m_preview = new PreviewContainer();

        getThePreviewPages();

        displayArea = new JScrollPane(m_preview);
        getContentPane().add(displayArea, BorderLayout.CENTER);
        System.gc();
    }

    public void preview() {
        previewThePages(pp_pf.getOrientation());
    }

    protected void createButtons(JToolBar tb, boolean shrink) {
        JButton bt = new JButton("Print");
        ActionListener lst = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    PrinterJob prnJob = PrinterJob.getPrinterJob();
                    // Have to recreate the printable, after preview has made
                    // a pass over the pages...
                    prnJob.setPrintable(getPrintable(byteTable, className), pp_pf);
                    if (prnJob.printDialog()) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        prnJob.print();
                        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                    dispose();
                } catch (PrinterException ex) {
                    ex.printStackTrace();
                    System.err.println("Printing error: " + ex.toString());
                }
            }
        };
        bt.addActionListener(lst);
        bt.setAlignmentY(0.5f);
        bt.setMargin(new Insets(4, 6, 4, 6));
        tb.add(bt);

        if (pp_pf.getOrientation() == PageFormat.PORTRAIT)
            formatButton = new JButton("Landscape");
        else
            formatButton = new JButton("Portrait");

        lst = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (pp_pf.getOrientation() == PageFormat.PORTRAIT) {
                    pp_pf.setOrientation(PageFormat.LANDSCAPE);
                    previewThePages(PageFormat.LANDSCAPE);
                    formatButton.setText("Portrait");
                } else {
                    pp_pf.setOrientation(PageFormat.PORTRAIT);
                    previewThePages(PageFormat.PORTRAIT);
                    formatButton.setText("Landscape");
                }
            }
        };
        formatButton.addActionListener(lst);
        formatButton.setAlignmentY(0.5f);
        formatButton.setMargin(new Insets(4, 6, 4, 6));
        tb.add(formatButton);

        if (shrink) {
            shrinkButton = new JButton("Shrink to fit");

            lst = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    bScallToFitOnePage = !bScallToFitOnePage;
                    previewThePages(pp_pf.getOrientation());
                }
            };
            shrinkButton.addActionListener(lst);
            shrinkButton.setAlignmentY(0.5f);
            shrinkButton.setMargin(new Insets(4, 6, 4, 6));
            tb.add(shrinkButton);
        }

        bt = new JButton("Close");
        lst = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        };
        bt.addActionListener(lst);
        bt.setAlignmentY(0.5f);
        bt.setMargin(new Insets(4, 6, 4, 6));
        tb.add(bt);
    }

    public int getDisplayScale() {
        String str = m_cbScale.getSelectedItem().toString();
        if (str.endsWith("%"))
            str = str.substring(0, str.length() - 1);
        str = str.trim();
        int scale = 0;
        try {
            scale = Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            return 25;
        }
        return scale;
    }

    public void run() {
        int scale = getDisplayScale();
        width = (int) (m_wPage * scale / 100);
        height = (int) (m_hPage * scale / 100);

        Component[] comps = m_preview.getComponents();
        for (int k = 0; k < comps.length; k++) {
            if (!(comps[k] instanceof PagePreview))
                continue;
            PagePreview pp = (PagePreview) comps[k];
            pp.setScaledSize(width, height);
        }
        m_preview.doLayout();
        m_preview.getParent().getParent().validate();
    }

    class PreviewContainer extends JPanel {

        private static final long serialVersionUID = -7232082848001434641L;

        protected int H_GAP = 16;
        protected int V_GAP = 10;

        public Dimension getPreferredSize() {
            int n = getComponentCount();
            if (n == 0)
                return new Dimension(H_GAP, V_GAP);
            Component comp = getComponent(0);
            Dimension dc = comp.getPreferredSize();
            int w = dc.width;
            int h = dc.height;

            Dimension dp = getParent().getSize();
            int nCol = Math.max((dp.width - H_GAP) / (w + H_GAP), 1);
            int nRow = n / nCol;
            if (nRow * nCol < n)
                nRow++;

            int ww = nCol * (w + H_GAP) + H_GAP;
            int hh = nRow * (h + V_GAP) + V_GAP;
            Insets ins = getInsets();
            return new Dimension(ww + ins.left + ins.right, hh + ins.top + ins.bottom);
        }

        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        public void doLayout() {
            Insets ins = getInsets();
            int x = ins.left + H_GAP;
            int y = ins.top + V_GAP;

            int n = getComponentCount();
            if (n == 0)
                return;
            Component comp = getComponent(0);
            Dimension dc = comp.getPreferredSize();
            int w = dc.width;
            int h = dc.height;

            Dimension dp = getParent().getSize();
            int nCol = Math.max((dp.width - H_GAP) / (w + H_GAP), 1);
            int nRow = n / nCol;
            if (nRow * nCol < n)
                nRow++;

            int index = 0;
            for (int k = 0; k < nRow; k++) {
                for (int m = 0; m < nCol; m++) {
                    if (index >= n)
                        return;
                    comp = getComponent(index++);
                    comp.setBounds(x, y, w, h);
                    x += w + H_GAP;
                }
                y += h + V_GAP;
                x = ins.left + H_GAP;
            }
        }
    }

    class PagePreview extends JPanel {

        private static final long serialVersionUID = 4323600921743689772L;

        protected int m_w;
        protected int m_h;
        protected Image m_source;
        protected Image m_img;

        public PagePreview(int w, int h, Image source) {
            m_w = w;
            m_h = h;
            m_source = source;
            m_img = m_source.getScaledInstance(m_w, m_h, Image.SCALE_SMOOTH);
            m_img.flush();
            setBackground(Color.white);
            setBorder(new MatteBorder(1, 1, 2, 2, Color.black));
        }

        public void setScaledSize(int w, int h) {
            m_w = w;
            m_h = h;
            m_img = m_source.getScaledInstance(m_w, m_h, Image.SCALE_SMOOTH);
            repaint();
        }

        public Dimension getPreferredSize() {
            Insets ins = getInsets();
            return new Dimension(m_w + ins.left + ins.right, m_h + ins.top + ins.bottom);
        }

        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        public void paint(Graphics g) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            g.drawImage(m_img, 0, 0, this);
            paintBorder(g);
        }
    }
}
