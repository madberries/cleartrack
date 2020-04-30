package pac.config;

import java.awt.Point;
import java.util.Vector;

import pac.util.TaintUtils;

// Quotes             // First: call Quote constructor
// fillQuotePairs     // Second: call fillQuotePairs
// tokenize           // Third:  call tokenize
//
// findSplitterStr
// combineHoldingArea
// findQuotePairFromOpeningQuoteIndex
// tknAtIndex
// tknStrAtIndex
//
// reconstruct
// dump

// A class to keep track of quotes, single quotes and double quotes that can be in a single line

public class Quotes {

    final int UNQUOTED_TKN = 1;
    final int SINGLE_QUOTE_TKN = 2;
    final int DOUBLE_QUOTE_TKN = 3;

    final private String DEBUG_INDENT = "     ";

    // This class exists to make it easier to tokenize string and quote in a way that resembles a shell
    // For example quoted strings that are contiguous to other quoted or non quoted strings should be
    // combined with those string as follows:
    //     a" b"/cd  => "a b/cd"
    //      """"/bc  => /bc
    //       "~"/cd  => ~/cd
    //       ""/abc  =>  /aabc
    //      ""~/abc  =>  ~/abc
    //    a"  "/abd  => "a  /abc"
    //     "  "/abd  => "  /abc"
    //    "  z"/abd" => "  z/abc   "
    //    "b"  "c"   =>
    //
    // The point of this class is to form this Vector.
    // Each element is a string. The strings are the tokens in the line being processed.
    // The strings are either single quoted strings, double quoted strings, or strings delimited either by white space or
    // a single quoted string token or double quoted string token.
    // The string can be trusted, untrusted or mixed.
    // Quotes have been removed.
    final private Vector<Tkn> tknVector = new Vector<Tkn>();

    final private String processMe;

    final private Vector<Point> trustedQuotes;
    final private Vector<Point> untrustedQuotes;

    // TODO What if these special chars are escaped
    final private String[] splitterChars = { "&", "&&", // Already processed
            "||", // Already processed
            "<<", ">>", "|&", // Already processed
            "|", ";", // Already processed
            "<", ">", "(", ")" };

    // Class is to remember location of an open and closed pair of either single quotes or double quotes
    // When Tkn's are compressed into a single token and contiguous quotes areas are compressed, the locations
    // of each pair of quotes is stored in a RemQuote
    class RemQuote {

        final int open, close;
        final int howQuoted; // One of: UNQUOTED_TKN  SINGLE_QUOTE_TKN  DOUBLE_QUOTE_TKN
        boolean openIsTrusted, closedIsTrusted;

        RemQuote(final int open, final int close, final int howQuoted) {
            this.open = open;
            this.close = close;
            this.howQuoted = howQuoted;
        }
    }

    class Tkn {
        final String seg; // The string. without it's single or double quotes
        final int howQuoted; // One of: UNQUOTED_TKN  SINGLE_QUOTE_TKN  DOUBLE_QUOTE_TKN
        boolean openIsTrusted, closedIsTrusted;
        Vector<RemQuote> remQuotes; // Not used until one or more contiguous Tkns are combined to
                                    // to a single Tkn.

        Tkn(final String seg, final int howQuoted) {
            this.seg = seg;
            this.howQuoted = howQuoted;
        }

        String getStringNoQuotes() {
            return seg;
        }

        /**
        * Call this method after a group of contiguous Tkns have been combined into a single Tkn.
        * The caller has collected the indeces of all quote
        *
        * @param remQuotes
        */
        void addRemQuotes(final Vector<RemQuote> remQuotes) {
            this.remQuotes = remQuotes;
        }
    }

    // Constructor
    Quotes(final String processMe) {
        this.processMe = processMe;
        trustedQuotes = new Vector<Point>();
        untrustedQuotes = new Vector<Point>();
    }

    /**
    * Call here immediately after calling the constructor
    *
    * Fill globals: trustedQuotes   Each point.x is an index in processMe of an opening trusted quote
    *                               Each point.y is an index in processMe of the matching closing trusted quote
    *               untrustedQuotes Each Point is an untrusted quote. Not point.x or point.y in this collection
    *                               is between the point.x and point.y of any point in the trustedQuotes collection
    */
    void fillQuotePairs() {

        boolean ESCAPE_STATE = false;
        boolean FOUND_TRUSTED_OPEN_QUOTE = false; // | Only one of these can be true
        boolean FOUND_UNTRUSTED_OPEN_QUOTE = false; // | at any given time

        char lookingForThisCloseQuote = 'a'; // one of: 'a'  '\'\   '\"'
        int openQuoteIndex = -1;

        for (int ch_index = 0; ch_index < processMe.length(); ch_index++) {
            char ch = processMe.charAt(ch_index);

            if (ESCAPE_STATE) {
                ESCAPE_STATE = false;
            } else {
                if (ch == '\\') {
                    ESCAPE_STATE = true;
                } else if (ch == '\"' || ch == '\'') {
                    if (TaintUtils.isTrusted(processMe, ch_index, ch_index)) {
                        if (FOUND_TRUSTED_OPEN_QUOTE) {
                            if (TaintUtils.charEquals(ch, lookingForThisCloseQuote)) {
                                final Point point = new Point(openQuoteIndex, ch_index);
                                trustedQuotes.addElement(point);
                                lookingForThisCloseQuote = 'a';
                                FOUND_TRUSTED_OPEN_QUOTE = false;
                            }
                        } else {
                            FOUND_TRUSTED_OPEN_QUOTE = true;
                            lookingForThisCloseQuote = ch;
                            openQuoteIndex = ch_index;
                        }

                    } else { // ch is an untrusted single or double quote
                        // If ch is enclosed between trusted single or double quotes,
                        // ie if FOUND_TRUSTED_OPEN_QUOTE is true,
                        // then skip any/all untrusted quotes
                        if (FOUND_TRUSTED_OPEN_QUOTE) {
                            // skip - do nothing

                        } else { // Are on an untrusted single or double quote not surrounded by trusted quotes
                            if (FOUND_UNTRUSTED_OPEN_QUOTE) { // Are on a closing untrusted quote
                                if (TaintUtils.charEquals(ch, lookingForThisCloseQuote)) {
                                    final Point point = new Point(openQuoteIndex, ch_index);
                                    untrustedQuotes.addElement(point);
                                    lookingForThisCloseQuote = 'a';
                                    FOUND_UNTRUSTED_OPEN_QUOTE = false;
                                }
                            } else { // Are on an opening untrusted single/double quote
                                FOUND_UNTRUSTED_OPEN_QUOTE = true;
                                lookingForThisCloseQuote = ch;
                                openQuoteIndex = ch_index;
                            }
                        }
                    }
                }
            }
        }
    }

    private int findSplitterStr(final String processMe, final int i) {

        final String lookFor = processMe.substring(i);
        int found = 0;
        for (int j = 0; found == 0 && j < splitterChars.length; j++) {
            if (lookFor.startsWith(splitterChars[j])) {
                found = splitterChars[j].length();
            }
        }

        return found;
    }

    // Each Tkn in holdingArea contains a string. Combine these string to form single string.
    // Combine all the Tkns in holdingArea into a single Tkn.
    // Append that TKn to global tknVector
    private void combineHoldingArea(final Vector<Tkn> holdingArea) {
        Vector<RemQuote> remQuotes = new Vector<RemQuote>();

        if (holdingArea.size() == 1) {
            final Tkn tkn = holdingArea.elementAt(0);

            final String str = tkn.getStringNoQuotes();
            final Tkn newTkn = new Tkn(str, tkn.howQuoted);
            if (tkn.howQuoted == SINGLE_QUOTE_TKN || tkn.howQuoted == DOUBLE_QUOTE_TKN) {
                final RemQuote remQuote = new RemQuote(0, str.length(), tkn.howQuoted);
                remQuote.openIsTrusted = tkn.openIsTrusted;
                remQuote.closedIsTrusted = tkn.closedIsTrusted;
                remQuotes.addElement(remQuote);
                newTkn.addRemQuotes(remQuotes);
            }

            tknVector.addElement(newTkn);

        } else if (holdingArea.size() > 1) {
            final StringBuilder buf = new StringBuilder();
            for (int q = 0; q < holdingArea.size(); q++) {
                final Tkn tkn = holdingArea.elementAt(q);
                final int quoteLocation = buf.length();
                TaintUtils.append(buf, tkn.getStringNoQuotes());
                if (tkn.howQuoted == SINGLE_QUOTE_TKN || tkn.howQuoted == DOUBLE_QUOTE_TKN) {
                    final RemQuote remQuote = new RemQuote(quoteLocation, buf.length(), tkn.howQuoted);
                    remQuote.openIsTrusted = tkn.openIsTrusted;
                    remQuote.closedIsTrusted = tkn.closedIsTrusted;
                    remQuotes.addElement(remQuote);
                }
            }
            final Tkn tkn = new Tkn(TaintUtils.toString(buf), UNQUOTED_TKN);
            tkn.addRemQuotes(remQuotes);

            tknVector.addElement(tkn);
        }

        holdingArea.removeAllElements();
    }

    /**
    * Call constructor
    * Call fillQuotePairs
    * Then call this method.
    *
    * This method tokenizes string processMe.
    * Each token is put in a Tkn. The Tkns are kept in global tknVector.
    * Each token can be; a string surrounded by trusted quotes
    *                    a string surrounded by untrusted quotes
    *                    a string delimited by white space, a single quoted string or a double quoted string
    * When a complete token is read in, it is put in holdingArea.
    * The holding area contains a group of tokens that are contiguous.
    * When a white space char is read, or when a splitter char is read, all the tokens in the holding area
    * are combined into a single Tkn.
    * That single token is ready for a regular expression check to be applied.
    *
    * @throws MsgException
    */
    void tokenize() {

        final Vector<Tkn> holdingArea = new Vector<Tkn>(); // As tokens are gathered, store them here.
                                                           // Upon reading a whitespace char or a splitter char combine all tokens
                                                           // in the holding area into a single token have no quotes
        int normalTknStart = 0;
        int normalTknEnd = 0;
        boolean readingNormalTkn = false;

        int advance;
        int i = 0;
        while (i < processMe.length()) {
            final char ch = processMe.charAt(i);
            Point point;

            // If are on a opening quote that is (single or double)  and  (trusted or untrusted)
            if ((ch == '\"' || ch == '\'') && (((point = findQuotePairFromOpeningQuoteIndex(trustedQuotes, i)) != null)
                    || ((point = findQuotePairFromOpeningQuoteIndex(untrustedQuotes, i)) != null))) {

                if (readingNormalTkn) {
                    final Tkn tkn = new Tkn(TaintUtils.substring(processMe, normalTknStart, normalTknEnd + 1),
                            UNQUOTED_TKN);
                    holdingArea.addElement(tkn);
                    readingNormalTkn = false;

                }

                boolean ch_is_quote = ch == '\"';
                final Tkn tkn = new Tkn(TaintUtils.substring(processMe, i + 1, point.y),
                        (ch_is_quote ? DOUBLE_QUOTE_TKN : SINGLE_QUOTE_TKN));
                tkn.openIsTrusted = TaintUtils.isTrusted(processMe, i, i);
                tkn.closedIsTrusted = TaintUtils.isTrusted(processMe, point.y, point.y);
                holdingArea.addElement(tkn);
                i = point.y + 1;
                readingNormalTkn = false;

            } else if (TaintUtils.characterIsWhitespace(ch)) { // jeik memorial find-of-the-day
                //          } else if (Character.isWhitespace(ch)){
                if (readingNormalTkn) {
                    final Tkn tkn = new Tkn(TaintUtils.substring(processMe, normalTknStart, normalTknEnd + 1),
                            UNQUOTED_TKN);
                    holdingArea.addElement(tkn);
                    readingNormalTkn = false;
                }
                i++;
                combineHoldingArea(holdingArea);
                readingNormalTkn = false;

            } else if ((advance = findSplitterStr(processMe, i)) != 0) {
                if (readingNormalTkn) {
                    final Tkn tkn = new Tkn(TaintUtils.substring(processMe, normalTknStart, normalTknEnd + 1),
                            UNQUOTED_TKN);
                    holdingArea.addElement(tkn);
                    readingNormalTkn = false;
                }
                combineHoldingArea(holdingArea);
                final Tkn tkn = new Tkn(TaintUtils.substring(processMe, i, i + advance), UNQUOTED_TKN);
                tknVector.addElement(tkn);
                i += advance;
                readingNormalTkn = false;

            } else {
                if (!readingNormalTkn) {
                    normalTknStart = i;
                    readingNormalTkn = true;
                }
                normalTknEnd = i;
                i++;
            }
        }

        if (readingNormalTkn) {
            final Tkn tkn = new Tkn(TaintUtils.substring(processMe, normalTknStart, normalTknEnd + 1), UNQUOTED_TKN);
            holdingArea.addElement(tkn);
            readingNormalTkn = false;
        }

        if (holdingArea.size() > 0) {
            combineHoldingArea(holdingArea);
        }
    }

    private Point findQuotePairFromOpeningQuoteIndex(final Vector<Point> vec, final int x) {

        boolean cont = true;
        Point found = null;

        for (int i = 0; cont && i < vec.size(); i++) {
            final Point point = vec.elementAt(i);
            if (point.x == x) {
                cont = false;
                found = point;
            } else if (point.x >= x) {
                cont = false;
            }
        }

        return found;
    }

    Tkn tknAtIndex(final int index) {
        final Tkn tkn = (index >= tknVector.size() ? null : tknVector.elementAt(index));
        return tkn;
    }

    String tknStrAtIndex(final int index) {
        final Tkn tkn = (index >= tknVector.size() ? null : tknVector.elementAt(index));
        final String str = (tkn == null ? null : tkn.getStringNoQuotes());
        return str;
    }

    private String reconstructStrCommon(final String str, final Tkn tkn) {
        final StringBuilder buf = TaintUtils.newStringBuilder(str);

        // Go from right to left inserting quote
        for (int j = tkn.remQuotes.size() - 1; j >= 0; j--) {
            final RemQuote remQuote = tkn.remQuotes.elementAt(j);
            // insert close first THEN open (are going rt to lf)
            String quote = (remQuote.howQuoted == DOUBLE_QUOTE_TKN ? "\"" : "\'");
            if (remQuote.openIsTrusted) {
                quote = TaintUtils.trust(quote);
            } else {
                quote = TaintUtils.taint(quote);
            }
            TaintUtils.insert(buf, remQuote.close, quote);

            if (remQuote.openIsTrusted) {
                quote = TaintUtils.trust(quote);
            } else {
                quote = TaintUtils.taint(quote);
            }
            TaintUtils.insert(buf, remQuote.open, quote);
        }

        return TaintUtils.toString(buf);
    }

    String reconstructTknStrAtIndex(final String str, final int index) {
        final Tkn tkn = tknVector.elementAt(index);
        String retval;
        if (tkn.remQuotes == null) {
            retval = str;
        } else {
            retval = reconstructStrCommon(str, tkn);
        }

        return retval;
    }

    String reconstructTknStrAtIndex(final int index) {
        final Tkn tkn = tknVector.elementAt(index);
        String retval;
        if (tkn.remQuotes == null) {
            retval = tkn.getStringNoQuotes();

        } else {
            retval = reconstructStrCommon(tkn.seg, tkn);
        }

        return retval;
    }

    private void reconstructAllTkns() {
        System.out.println("---");
        System.out.println("Reconstructed tokens:");
        StringBuilder buf = TaintUtils.newStringBuilder();
        for (int i = 0; i < tknVector.size(); i++) {
            final String rebuiltStr = reconstructTknStrAtIndex(i);

            if (i != 0) {
                TaintUtils.append(buf, " ");
            }
            TaintUtils.append(buf, rebuiltStr);
        }

        System.out.println(DEBUG_INDENT + TaintUtils.toString(buf));
    }

    private void dump() {
        System.out.println("---");
        System.out.println("Original processMe:");
        System.out.println(DEBUG_INDENT + processMe);

        System.out.println("Trusted quoted strings:");
        for (int a = 0; a < trustedQuotes.size(); a++) {
            final Point point = trustedQuotes.elementAt(a);
            System.out.println(DEBUG_INDENT + processMe.substring(point.x, point.y + 1));
        }

        System.out.println("---");
        System.out.println("List of tokens:");
        for (int i = 0; i < tknVector.size(); i++) {
            final Tkn tkn = tknVector.elementAt(i);
            System.out.println(DEBUG_INDENT + tkn.seg + "    "
                    + (tkn.howQuoted == UNQUOTED_TKN ? "not quoted" : "quoted"));

            if (tkn.remQuotes != null) {
                System.out.println(DEBUG_INDENT + DEBUG_INDENT + "Quote Locations:");
                for (int z = 0; z < tkn.remQuotes.size(); z++) {
                    final RemQuote remQuote = tkn.remQuotes.elementAt(z);
                    System.out.println(DEBUG_INDENT + DEBUG_INDENT
                            + (remQuote.howQuoted == DOUBLE_QUOTE_TKN ? "is double " : "is single ") +
                            //    (remQuote.isDoubleQuote ? "is double" : "is single") +
                            remQuote.open + "  " + remQuote.close);
                }
            }
        }
    }

    public static void main(String[] argv) {
        String processMe = "the \"big \'bay bridge\' gray\" fox \"took a\" jump";
        processMe = TaintUtils.taint(processMe);
        /*
        final String processMe = "ls -a \'a.out\";\"rm *\'";
        TaintValues.trust(processMe);
        TaintValues.getMetadata(processMe).markRegion(TaintValues.TrustLevel.TAINTED, 7, 18);
        */

        /*
        final String processMe = "\'this\\\"\"is the\" g\'uarded life.";
        TaintValues.trust(processMe);
        TaintValues.isTainted(processMe, 1, 16);
        TaintValues.getMetadata(processMe).markRegion(TaintValues.TrustLevel.TAINTED, 1, 16);
        */
        final Quotes quotes = new Quotes(processMe);
        quotes.fillQuotePairs(); // Fill trustedQuotes and untrustedQuotes
        quotes.tokenize();
        quotes.dump();
        quotes.reconstructAllTkns();

        /*
        System.out.println(processMe);
        for (int i = 0; i < points.size(); i++) {
            final Point point = points.elementAt(i);
            System.out.println(point);
        }
        */

        /*
        final Quotes quotes = new Quotes("bin/ls ;_/bin/ls /etc/hosts");
        //      final Quotes quotes = new Quotes("abc \"def\"");
        //      final Quotes quotes = new Quotes("a\"  \"../this \"\"\"\"\"\' my little dog\'\";\'a b\'\\\'is\\\"a string");
        quotes.tokenize();
        quotes.dump();
        quotes.reconstructAllTkns();
        */
    }
}
