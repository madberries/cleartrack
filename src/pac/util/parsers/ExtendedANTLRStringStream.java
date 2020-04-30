package pac.util.parsers;

import pac.org.antlr.runtime.ANTLRStringStream;
import pac.org.antlr.runtime.CharStream;

public class ExtendedANTLRStringStream extends ANTLRStringStream {
    boolean debug = false;
    boolean caseInsensitive = true;

    public ExtendedANTLRStringStream(String input) {
        super(input);
    }

    public ExtendedANTLRStringStream(String input, boolean caseInsensitive) {
        super(input);
        this.caseInsensitive = caseInsensitive;
    }

    /** Reset the stream so that it's in the same state it was
     *  when the object was created *except* the data array is not
     *  touched.
     */
    public void reset(boolean caseInsensitive) {
        super.reset();
        this.caseInsensitive = caseInsensitive;
    }

    @Override
    public int LA(int i) {
        if (i == 0) {
            return 0; // undefined
        }

        if (i < 0) {
            i++; // e.g., translate LA(-1) to use offset i=0; then data[p+0-1]
            if ((p + i - 1) < 0) {
                return CharStream.EOF; // invalid; no char before first char
            }
        }

        if ((p + i - 1) >= n) {
            //System.out.println("char LA("+i+")=EOF; p="+p);
            return CharStream.EOF;
        }

        //System.out.println("char LA("+i+")="+(char)data[p+i-1]+"; p="+p);
        //System.out.println("LA("+i+"); p="+p+" n="+n+" data.length="+data.length);
        return (caseInsensitive) ? Character.toLowerCase(data[p + i - 1]) : data[p + i - 1];
    }

    @Override
    public void consume() {
        int old_p = p;
        super.consume();
        if (debug && p != old_p) {
            System.out.println("consume(): prev p=" + old_p + ", c=" + (char) data[old_p]);
            System.out.println("p moves to " + p + ((p < n) ? " (c='" + (char) data[p] + "')" : ""));
        }
    }
}
