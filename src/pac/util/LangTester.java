package pac.util;

import java.util.HashSet;
import java.util.Set;

import pac.org.antlr.runtime.CommonTokenStream;

import pac.util.parsers.ExtendedANTLRStringStream;
import pac.util.parsers.LangConstants;
import pac.util.parsers.TestLang;
import pac.util.parsers.TestLangLexer;

public class LangTester implements LangConstants {
    /**
     * Names of some commonly used executables.
     */
    static Set<String> progs;
    static {
        progs = new HashSet<String>();
        progs.add("csh");
        progs.add("sh");
        progs.add("bash");
        progs.add("ls");
    }

    public static LangTester theInstance = new LangTester();

    /**
     * Given a string, tests whether the string is a command, an LDAP expression,
     * an SQL statement, an XQuery expression, or unknown.  Returns an integer code
     * for the resulting language or unknown.
     * 
     * @param input the string to be tested
     * @return the integer code that represents the resulting language or unknown
     */
    public int testLang(String input) {
        ExtendedANTLRStringStream cs = new ExtendedANTLRStringStream(input);
        TestLangLexer lexer = new TestLangLexer(cs);
        CommonTokenStream tokens = new CommonTokenStream();
        tokens.setTokenSource(lexer);
        int result;
        try {
            TestLang parser = new TestLang(tokens, input);
            result = parser.testLang();
        } catch (IllegalArgumentException e) {
            result = UNKNOWN;
        }
        return result;
    }

    private static String printLang(int lang) {
        switch (lang) {
        case COMMAND:
            return "COMMAND";
        case LDAP:
            return "LDAP";
        case SQL:
            return "SQL";
        case XQUERY:
            return "XQUERY";
        default:
            return "UNKNOWN";
        }
    }

    /**
     * Given a string, tests whether the string represents a command, an LDAP expression,
     * an SQL statement, an XQuery expression, or unknown.  Prints the test result to the
     * console.
     */
    public static void main(String[] args) {
        String input = args[0];
        int result = theInstance.testLang(input);
        System.out.println("result: " + printLang(result));
    }
}
