package pac.util.parsers;

import pac.org.antlr.runtime.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

@SuppressWarnings({ "all", "warnings", "unchecked" })
public class TestCommand extends Parser {
    public static final String[] tokenNames = new String[] { "<invalid>", "<EOR>", "<DOWN>", "<UP>", "COMMENT",
            "DECIMAL_LITERAL", "DIGIT", "DOLLAR", "DOT", "DOUBLE_DOT", "DOUBLE_QUOTE", "GT", "LEFT_PAREN", "LETTER",
            "MINUS", "NAME", "PATH", "PLUS", "QUOTE", "RIGHT_PAREN", "STRING_LITERAL", "Semicolon", "TILDA",
            "UNDERSCORE", "VERTICAL_BAR", "WS" };

    public static final int EOF = -1;
    public static final int COMMENT = 4;
    public static final int DECIMAL_LITERAL = 5;
    public static final int DIGIT = 6;
    public static final int DOLLAR = 7;
    public static final int DOT = 8;
    public static final int DOUBLE_DOT = 9;
    public static final int DOUBLE_QUOTE = 10;
    public static final int GT = 11;
    public static final int LEFT_PAREN = 12;
    public static final int LETTER = 13;
    public static final int MINUS = 14;
    public static final int NAME = 15;
    public static final int PATH = 16;
    public static final int PLUS = 17;
    public static final int QUOTE = 18;
    public static final int RIGHT_PAREN = 19;
    public static final int STRING_LITERAL = 20;
    public static final int Semicolon = 21;
    public static final int TILDA = 22;
    public static final int UNDERSCORE = 23;
    public static final int VERTICAL_BAR = 24;
    public static final int WS = 25;

    String[] systemPaths = System.getenv("PATH").split(":");
    static Set<String> progs;
    static {
        progs = new HashSet<String>();
        progs.add("csh");
        progs.add("sh");
        progs.add("ksh");
        progs.add("kshell");
        progs.add("tsh");
        progs.add("zsh");
        progs.add("bash");
        progs.add("dash");
        progs.add("ls");
    }

    public TestCommand(TokenStream input) {
        super(input);
    }

    public final boolean testCommand() {
        int ttype1 = input.LA(1);
        int ttype2 = input.LA(2);
        if ((ttype1 == PATH || ttype1 == NAME) && (ttype2 == WS || ttype2 == EOF)) {
            String cmd = input.LT(1).getText();
            if (ttype1 == PATH) {
                if (cmd.startsWith("~/")) {
                    cmd = System.getProperty("user.home") + cmd.substring(1);
                } else if (cmd.startsWith("./")) {
                    cmd = cmd.substring(2);
                } else if (cmd.startsWith("../")) {
                    String curDir = System.getProperty("user.dir");
                    int pos = curDir.lastIndexOf('/');
                    if (pos > 0) {
                        cmd = curDir.substring(0, pos) + cmd.substring(2);
                    }
                }
            }
            return testCommand(cmd);
        }
        return false;
    }

    private boolean testCommand(String cmd) {
        boolean isPath = (cmd.indexOf('/') >= 0);
        if (isPath) {
            if (cmd.startsWith("/bin") && progs.contains(cmd.substring(5)))
                return true;
        } else {
            if (progs.contains(cmd))
                return true;
        }
        File file = new File(cmd);
        if (file.exists() && file.isFile() && file.canExecute())
            return true;
        for (String path : systemPaths) {
            file = new File(path, cmd);
            if (file.exists() && file.isFile() && file.canExecute())
                return true;
        }
        //		if (!isPath) {
        //			ProcessBuilder processBuilder = new ProcessBuilder("which", cmd);
        //			Process process;
        //			try {
        //				process = processBuilder.start();
        //				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        //				int exitValue = process.waitFor();
        //				if (exitValue == 0 && reader.readLine() != null)
        //					return true;
        //				reader.close();
        //			} catch (Exception e) {
        //				e.printStackTrace();
        //			}
        //		}
        return false;
    }
}
