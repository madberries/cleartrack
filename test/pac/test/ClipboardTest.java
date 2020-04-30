package pac.test;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import pac.util.TaintUtils;

public class ClipboardTest implements ClipboardOwner {

    @Test
    public void clipboardTest() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

            // Initially the clipboard contents should have been set from some
            // outside source, so it should be tainted.
            Transferable contents = clipboard.getContents(null);
            boolean hasTransferableText = (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
            if (hasTransferableText) {
                try {
                    String result = (String) contents.getTransferData(DataFlavor.stringFlavor);
                    Assert.assertTrue("clipboard contents coming from some outside source should be tainted",
                                      TaintUtils.isAllTainted(result, 0, result.length() - 1));
                } catch (UnsupportedFlavorException ex) {

                    System.out.println(ex);
                    ex.printStackTrace();
                } catch (IOException ex) {
                    System.out.println(ex);
                    ex.printStackTrace();
                }
            }

            // Set the clipboard to some tracked String value.
            String partiallyTainted = TaintUtils.trust("this is a partially tainted string");
            partiallyTainted = TaintUtils.taint(partiallyTainted, 10, 19);
            StringSelection stringSelection = new StringSelection(partiallyTainted);
            clipboard.setContents(stringSelection, (ClipboardOwner) this);

            // Acquire the clipboard contents and check to make sure that
            // the trust metadata matches that of the original string...
            contents = clipboard.getContents(null);
            hasTransferableText = (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
            if (hasTransferableText) {
                try {
                    String result = (String) contents.getTransferData(DataFlavor.stringFlavor);
                    Assert.assertTrue("trust metadata from clipboard should be tainted", TaintUtils.isTainted(result));
                } catch (UnsupportedFlavorException ex) {
                    System.out.println(ex);
                    ex.printStackTrace();
                } catch (IOException ex) {
                    System.out.println(ex);
                    ex.printStackTrace();
                }
            }
        } catch (HeadlessException e) {
            System.out.println("Skipping test: " + e.getMessage());
        }
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {

    }

}
