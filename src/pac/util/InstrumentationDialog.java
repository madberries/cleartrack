package pac.util;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import pac.util.StringRegionIterator.StringRegion;

public class InstrumentationDialog implements Runnable {
    private static boolean showPopUps = true;

    public static void setShowPopUps(boolean showPopUps) {
        InstrumentationDialog.showPopUps = showPopUps;
    }

    final String message;
    final String title;

    // INSTANCE AREA

    private InstrumentationDialog(String message, String title) {
        this.message = message;
        this.title = title;
    }

    public void run() {
        try {
            //System.err.println(title + "\n" + message);
            int maximum_line_width = 100;
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            // Split a long message into lines at spaces.
            StringBuilder m = new StringBuilder();
            for (int i = 0; i < message.length();) {
                int j;
                for (j = i + maximum_line_width; j < message.length() && message.charAt(j) != ' '; j++) {
                }
                if (j > message.length()) {
                    j = message.length();
                }
                m.append(message.substring(i, j));
                m.append("<br>");
                i = j;
            }
            JOptionPane.showMessageDialog(null, m.toString(), title, JOptionPane.WARNING_MESSAGE);
        } catch (Exception e) {
            // do nothing in case of errors
        }
    }

    // STATIC AREA

    public static void showRandomizationDialog(String sql) {

        if (showPopUps) {
            // start the popup in a new thread so we are not beholden to Swing's event loop
            StringBuilder message = new StringBuilder();
            message.append("<html><center><font size=\"+1\"");
            message.append("The SQL string<br>");
            message.append(sql);
            message.append("<br>could not be parsed, possibly because it has unrandomized keywords.<br>");
            message.append("</font></center></html>");
            InstrumentationDialog id = new InstrumentationDialog(message.toString(), "Parse Randomized SQL");
            Thread t = new Thread(id);
            t.start();
        }
    }

    public static void showSanitizationDialog(String sql, String sanitizedSql) {
        if (showPopUps) {
            // start the popup in a new thread so we are not beholden to Swing's event loop
            StringBuilder message = new StringBuilder();
            message.append("<html><center><font size=\"+1\"");
            message.append("The tainted SQL string<br>");
            message.append(htmlFormatTrackedString(sql));
            message.append("<br>was sanitized to<br>");
            message.append(simpleHTMLEscape(sanitizedSql));
            message.append("</font></center></html>");
            InstrumentationDialog id = new InstrumentationDialog(message.toString(), "Sanitized Tainted SQL");
            Thread t = new Thread(id);
            t.start();
        }
    }

    public static String htmlFormatTrackedString(String sourceString) {
        StringBuilder output = new StringBuilder();

        if (!TaintUtils.isTracked(sourceString)) {
            return sourceString;
        }

        int length = sourceString.length();
        int currentIndex = 0;

        StringRegionIterator iter = new StringRegionIterator(sourceString, TaintValues.TRUST_MASK);
        while (iter.hasNext()) {
            StringRegion reg = iter.next();
            int startIndex = reg.getStart();
            int endIndex = reg.getEnd(); // region end index is inclusive

            // output characters in this region in special font

            if (endIndex >= length)
                endIndex = length - 1;
            if (endIndex < startIndex)
                break; // invalid region, bail

            String color = "black";
            if ((reg.getTaint() & TaintValues.TRUST_MASK) == TaintValues.TRUSTED)
                color = "blue";
            else if ((reg.getTaint() & TaintValues.TRUST_MASK) == TaintValues.TAINTED)
                color = "red";

            output.append("<font color=\"" + color + "\">");
            output.append(simpleHTMLEscape(sourceString.substring(startIndex, endIndex + 1))); // end-index exclusive operation
            output.append("</font>");

            currentIndex = endIndex + 1;
        }

        // output any remaining characters normally
        if (currentIndex < length) {
            output.append(simpleHTMLEscape(sourceString.substring(currentIndex, length)));
        }

        return output.toString();
    }

    /**
     * Because our Dialog uses HTML formatting, we need to escape HTML in our output message.
     * @param s raw message
     * @return message with simple HTML escape characters
     */
    public static String simpleHTMLEscape(String s) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
            case ' ':
                sb.append("&nbsp;");
                break;
            case '&':
                sb.append("&amp;");
                break;
            case '<':
                sb.append("&lt;");
                break;
            case '>':
                sb.append("&gt;");
                break;
            default:
                sb.append(c);
                break;
            }
        }

        return sb.toString();
    }

    /**
     * This main is purely for testing the dialog.
     * @param args
     */
    public static void main(String[] args) {
        String message = "<html>default text <font color=\"red\">red text</font></html>";
        String title = "some title";
        InstrumentationDialog id = new InstrumentationDialog(message, title);
        id.run();
    }
}
