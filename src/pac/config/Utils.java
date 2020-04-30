package pac.config;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pac.util.TaintUtils;

// getUnescapedSingleQuotes
// getUnescapedDoubleQuotes
// getQuotedSubStrs
// getTokenAtIndex
// getTokenIntAtIndex
// getLineNumberReader
// treatUnknownCharData
// compareDescriptions
// arrayToString
// buildExceptionConstructor
// testConstructor
// newException
public class Utils {
    // SupertypeANalyzer builds this file.
    // The file contains class name of parent classes and class names of that classes immediate superclasses.
    // THIS class read the file to build hashtable in ParentTree
    final static String CLASS_RELATIONS_FILE_LEAFNAME = "class_relations";

    final static String DOUBLE_QUOTE_REG_EXP = "(?<!\\\\)\"";
    final static String SINGLE_QUOTE_REG_EXP = "(?<!\\\\)\'";

    // Get list of indexes of unescaped pairs of quotes within processMe.
    // Each pair of quotes is represented as a point:
    //   point.x indexes the open quote
    //   point.y indexes the closed quote
    public static Vector<Point> getUnescapedSingleQuotes(final String str) {
        final Vector<Point> quotes = getQuotedSubStrs(str, SINGLE_QUOTE_REG_EXP);
        return quotes;
    }

    public static Vector<Point> getUnescapedDoubleQuotes(final String str) {
        final Vector<Point> quotes = getQuotedSubStrs(str, DOUBLE_QUOTE_REG_EXP);
        return quotes;
    }

    public static int nextTrustedQuote(final String str, final char quoteChar, final char escapeChar, final int start) {

        int quoteIndex = TaintUtils.stringIndexOf(str, quoteChar, start);
        boolean selfEscaped = TaintUtils.charEquals(quoteChar, escapeChar);
        /* rjk
        int quoteIndex = str.indexOf(quoteChar, start);
        boolean selfEscaped = quoteChar==escapeChar;
        */
        while (true) {
            if (quoteIndex < 0)
                break;

            if (selfEscaped) {
                // RULES for quotes:
                // 1) trusted-trusted are escaped
                // 2) trusted-untrusted are escaped
                // 3) untrusted-trusted... if the following character is tainted, then treat as escaped
                //                      if the following character is trusted and is a space, then treat as closed quote.
                if ((quoteIndex + 1) >= str.length())
                    break;
                boolean firstTrusted = TaintUtils.isTrusted(str, quoteIndex, quoteIndex);
                char secondChar = str.charAt(quoteIndex + 1);
                if (firstTrusted && !TaintUtils.charEquals(secondChar, escapeChar)) // rjk
                    // rjk          if (firstTrusted && secondChar != escapeChar)
                    break;
                boolean secondTrusted = TaintUtils.isTrusted(str, quoteIndex + 1, quoteIndex + 1);
                if (firstTrusted && TaintUtils.charEquals(secondChar, escapeChar)) { // rjk
                    // rjk          if (firstTrusted && secondChar == escapeChar) {
                    //Rule 1 and 2 apply...
                    quoteIndex++;
                } else if (!firstTrusted && secondTrusted && TaintUtils.charEquals(secondChar, escapeChar)) {
                    // rjk          } else if (!firstTrusted && secondTrusted && secondChar == escapeChar) {
                    if (quoteIndex + 2 >= str.length())
                        return quoteIndex + 1;
                    boolean thirdTrusted = TaintUtils.isTrusted(str, quoteIndex + 2, quoteIndex + 2);
                    char thirdChar = str.charAt(quoteIndex + 2);
                    if (thirdTrusted && TaintUtils.characterIsWhitespace(thirdChar)) // Apply Rule 3...
                        return quoteIndex + 1;
                }
            } else if (escapeChar == 0 && TaintUtils.isTrusted(str, quoteIndex, quoteIndex)) {
                // no escape character and the quote is trusted...
                break;
            } else if (escapeChar != 0 && TaintUtils.isTrusted(str, quoteIndex, quoteIndex)
                    && (!TaintUtils.charEquals(str.charAt(quoteIndex - 1), escapeChar)
                            || !TaintUtils.isTrusted(str, quoteIndex - 1, quoteIndex - 1))) {
                // rjk          (str.charAt(quoteIndex-1) != escapeChar || !TaintUtils.isTrusted(str, quoteIndex-1, quoteIndex-1))) {
                // escape character and the quote is trusted, but the previous character 
                // is not a trusted escape character...
                break;
            }
            quoteIndex = TaintUtils.stringIndexOf(str, quoteChar, quoteIndex + 1);
            // rjk      quoteIndex = str.indexOf(quoteChar, quoteIndex + 1);
        }

        return quoteIndex;
    }

    /**
    *
    * @param processMe Locate each pair of un-escaped quotes in this string
    * @return Elements are Points. Point.x indexes the opening quote..
    *         Point.y indexes closing quote. processMe.substring(point.x, point.y)
    *         produces a quoted string contained in processMe without the quotes.
    */
    static Vector<Point> getQuotedSubStrs(final String processMe_param, final String regExp) {

        final String processMe = processMe_param;

        // This pattern matches quote char that is not preceded with backslash char
        final Pattern pattern = Pattern.compile(regExp);
        final Matcher matcher = pattern.matcher(processMe);
        boolean cont = true;
        boolean even = true;
        int openQuote = 0; // Indexes the character to right of first quote
        Vector<Point> retval = new Vector<Point>();

        while (cont) { // Each time through loop an un-escaped quote will be located
            cont = matcher.find();
            if (cont) {
                if (!even) { // matcher is on the closing quote
                    retval.addElement(new Point(openQuote - 1, matcher.start()));
                }

                even = !even;
                openQuote = matcher.end();
            }
        }

        return retval;
    }

    /**
     * Retrieves the token at index tknIndex from line (without taint propagation).
     * 
     * @param line
     * @param tknIndex
     * @return
     * @throws MsgException
     */
    public static String getTokenAtIndex(final String line, final int tknIndex) throws MsgException {
        final String[] ray = line.split(TaintUtils.whiteSpaceRegExp() + "+");
        if (ray.length < tknIndex + 1) {
            throw new MsgException("token at index " + tknIndex + " out of bounds for line: " + line);
        }

        final String retval = ray[tknIndex];
        return retval;
    }

    /**
     * Retrieves the token at index tknIndex from line (with taint propagation).
     * 
     * @param line
     * @param tknIndex
     * @return
     * @throws MsgException
     */
    public static String getTokenAtIndexWithTaint(final String line, final int tknIndex) throws MsgException {
        final String[] ray = TaintUtils.split(line, TaintUtils.trust(TaintUtils.whiteSpaceRegExp() + "+"));
        if (ray.length < tknIndex + 1) {
            throw new MsgException("token at index " + tknIndex + " out of bounds for line: " + line);
        }

        final String retval = ray[tknIndex];
        return retval;
    }

    public static int getTokenIntAtIndex(final String line, final int tknIndex) throws MsgException {

        final String MSG = "Failed to convert token " + tknIndex + " in \"" + line + "\" to an int.\n";

        try {
            final String tkn = getTokenAtIndex(line, tknIndex); // MsgException
            final int retval = Integer.parseInt(tkn); // NumberFormatException
            return retval;

        } catch (NumberFormatException ex) {
            throw new MsgException(MSG + ex.getMessage());
        }
    }

    public static LineNumberReader getLineNumberReader(final String filePath) throws MsgException {
        try {
            final FileReader fileReader = new FileReader(filePath); // FileNotFoundException
            final BufferedReader buffReader = new BufferedReader(fileReader);
            final LineNumberReader reader = new LineNumberReader(buffReader);

            return reader;

        } catch (FileNotFoundException ex) {
            throw new MsgException(ex.getMessage());
        }
    }

    public static LineNumberReader getLineNumberReader(final File file) throws MsgException {
        try {
            final FileReader fileReader = new FileReader(file); // FileNotFoundException
            final BufferedReader buffReader = new BufferedReader(fileReader);
            final LineNumberReader reader = new LineNumberReader(buffReader);

            return reader;

        } catch (FileNotFoundException ex) {
            throw new MsgException(ex.getMessage());
        }
    }

    // Use this method to determine if two descriptions are equivalent.
    // This method returns true for descriptionA: "Runtime.exe(String [], String[], File)"
    //                              descriptionB: "Runtime.exe(String[],  String [],File)"
    // Do not use this method on descriptions that come from config file
    public static boolean compareDescriptions(final String descriptionA, final String descriptionB) {
        final String A = descriptionA.replaceAll("[\\s+]", "");
        final String B = descriptionB.replaceAll("[\\s+]", "");
        return A.equals(B);
    }

    public static String arrayToString(final String[] ray) {
        final StringBuilder buf = new StringBuilder();
        int i;
        for (i = 0; i < ray.length && i < 3; i++) {
            buf.append("[");
            buf.append(i);
            buf.append("]");
            buf.append(ray[i]);
            if (i < 2) {
                buf.append("  ");
            }
        }

        if (i == 3 && ray.length > 3) {
            buf.append("...");
        }

        return buf.toString();
    }

    /**
    * From an Exception package spec build a constructor.
    * Test the constructor before returning the constructor
    *
    * @param spec Looks like "java.lang.annotation.AnnotationTypeMismatcheException"
    * @return The constructor for the class
    * @throws MsgException if error building the constructor or testing the constructor failed
    */
    public static Constructor<?> buildExceptionConstructor(final String spec) throws MsgException {

        final String ERR_MSG = "Error building an exception constructor from: ";

        try {
            final Class<?> lass = Class.forName(spec); // ClassNotFoundException
                                                       // LinkageError
                                                       // ExceptionInInitializerError
            final Constructor<?> c = lass.getConstructor(new Class[] { String.class }); // NoSuchMethodException
                                                                                        // SecurityException
            testConstructor(c); // MsgException

            return c;

        } catch (LinkageError ex) {
            throw new MsgException(ERR_MSG + spec + "\n" + ex.getMessage());
        } catch (ClassNotFoundException ex) {
            throw new MsgException(ERR_MSG + spec + "\n" + ex.getMessage());
        } catch (NoSuchMethodException ex) {
            throw new MsgException(ERR_MSG + spec + "\n" + ex.getMessage());
        }
    }

    // Test the constructor that was just built:
    //    o test building an exception from the constructor
    //    o test throwing the exception instance
    private static void testConstructor(final Constructor<?> constructor) throws MsgException {

        final String MSG = "testing";
        boolean caught = false;

        try {
            final RuntimeException runEx = Utils.newException(constructor, MSG); // MsgException
            throw runEx;

        } catch (RuntimeException ex) {
            if (MSG.equals(ex.getMessage())) {
                caught = true;
            }
        } catch (MsgException ex) {
            throw new MsgException("Unable to create a RuntimeException for: " + constructor.getName());
        }

        if (!caught) {
            throw new MsgException("Unable to create a working RuntimeException: " + constructor.getName());
        }
    }

    @SuppressWarnings("rawtypes")
    static RuntimeException newException(final Constructor c, final String msg) throws MsgException {
        try {
            final Object obj = c.newInstance(new Object[] { msg }); // InvocationTargetException
                                                                    // IllegalAccessException
                                                                    // InstantiationException
            final RuntimeException ex = (RuntimeException) obj; // ClassCasteException

            return ex;

        } catch (ClassCastException ex) {
            throw new MsgException(ex.getMessage());
        } catch (InstantiationException ex) {
            throw new MsgException("Instantion access\n" + ex.getMessage());
        } catch (IllegalAccessException ex) {
            throw new MsgException("Illegal access\n" + ex.getMessage());
        } catch (InvocationTargetException ex) {
            throw new MsgException("no invocation\n" + ex.getMessage());
        }
    }

    public static File getClassRelationsFile() {
        final File file = new File(".", CLASS_RELATIONS_FILE_LEAFNAME);
        return file;
    }

    // For debugging only
    public static void debug_print() {
        System.out.println("===================================== ++++++++++++++++++++++");
    }

    // For debugging only
    // Call debug_print_stack using:
    // AbstractInsnNode new_node = new MethodInsnNode(pac.org.objectweb.asm.Opcodes.INVOKESTATIC,
    //                                                "pac/config/Utils",
    //                                                "debug_print_stack",
    //                                                "()V");
    //  list.insertBefore(insn, new_node);
    public static void debug_print_stack() {
        debug_print_stack("found antlr/Token.<init> XXXXXXXXXXXXXXXX ^^^^^^^^^^^^^^ YYYYYYYYYYYYYY");
    }

    /**
    * For stack output while debugging
    */
    public static void debug_print_stack(String debug_str) {
        StringBuffer strbuf = new StringBuffer();
        StackTraceElement trace[] = Thread.currentThread().getStackTrace();
        // elem 0 is Thread.getStackTrace
        // elem 1 is this method: debug_print_stack
        for (int i = 2; i < trace.length; i++) {
            final StackTraceElement s = trace[i];
            final String className = s.getClassName();
            strbuf.append("  ");
            strbuf.append(className);
            strbuf.append(".");
            strbuf.append(s.getMethodName());
            strbuf.append("  line:");
            strbuf.append(s.getLineNumber());
            strbuf.append("\n");
        }
        System.out.println(debug_str);
        System.out.println(strbuf.toString());
    }

    public static char debug_print_param(char ch) {
        System.out.println("ch:" + ch);
        return ch;
    }

    public static int debug_print_param(int i) {
        System.out.println("i:" + i);
        return i;
    }

    public static void debug_print_param(int i, String str) {
        System.out.println("i:" + i + "  str:" + str);
    }
} // class Utils
