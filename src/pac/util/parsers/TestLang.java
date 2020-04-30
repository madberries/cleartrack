package pac.util.parsers;

import pac.util.LangTester;

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
public class TestLang extends Parser implements LangConstants {
    public static final String[] tokenNames = new String[] { "<invalid>", "<EOR>", "<DOWN>", "<UP>", "AMPERSAND",
            "APPROX", "AT", "CHAR_NO_DOUBLE_QUOTE", "CHAR_NO_QUOTE", "CIRCUMFLEX", "COLON", "COLON_EQ", "COMMA",
            "COMMENT_SQL", "COMMENT_XQUERY", "DECIMAL_LITERAL", "DIGIT", "DOLLAR", "DOT", "DOUBLE_DOT", "DOUBLE_GT",
            "DOUBLE_LT", "DOUBLE_QUOTE", "DOUBLE_SLASH", "EQ", "EXCLAMATION", "EXCLAMATION_EQ", "GE", "GT", "HEX_DIGIT",
            "IDENTIFIER", "IDENTIFIER_CONTINUE", "IDENTIFIER_START", "KW_SQL_ALL", "KW_SQL_ALTER", "KW_SQL_BY",
            "KW_SQL_CREATE", "KW_SQL_DATABASE", "KW_SQL_DELETE", "KW_SQL_DISTINCT", "KW_SQL_DROP", "KW_SQL_FROM",
            "KW_SQL_INDEX", "KW_SQL_INSERT", "KW_SQL_INTO", "KW_SQL_ORDER", "KW_SQL_SELECT", "KW_SQL_SET",
            "KW_SQL_TABLE", "KW_SQL_TRUNCATE", "KW_SQL_UNIQUE", "KW_SQL_UPDATE", "KW_SQL_VALUES", "KW_SQL_VIEW",
            "KW_SQL_WHERE", "KW_XQUERY_ANCESTOR_COLON", "KW_XQUERY_ANCESTOR_OR_SELF_COLON", "KW_XQUERY_AND",
            "KW_XQUERY_AS", "KW_XQUERY_ATTRIBUTE_COLON", "KW_XQUERY_BASE_URI", "KW_XQUERY_BOUNDARY_SPACE",
            "KW_XQUERY_CASE", "KW_XQUERY_CAST", "KW_XQUERY_CASTABLE", "KW_XQUERY_CHILD_COLON", "KW_XQUERY_COMMENT",
            "KW_XQUERY_CONSTRUCTION", "KW_XQUERY_COPY_NAMESPACES", "KW_XQUERY_DECLARE", "KW_XQUERY_DEFAULT",
            "KW_XQUERY_DESCENDANT_COLON", "KW_XQUERY_DESCENDANT_OR_SELF_COLON", "KW_XQUERY_DIV", "KW_XQUERY_DOCNODE",
            "KW_XQUERY_ELEMENT", "KW_XQUERY_ELSE", "KW_XQUERY_EMPTY_SEQUENCE", "KW_XQUERY_EQ", "KW_XQUERY_EVERY",
            "KW_XQUERY_EXCEPT", "KW_XQUERY_FOLLOWING_COLON", "KW_XQUERY_FOLLOWING_SIBLING_COLON", "KW_XQUERY_FOR",
            "KW_XQUERY_FUNCTION", "KW_XQUERY_GE", "KW_XQUERY_GT", "KW_XQUERY_IDIV", "KW_XQUERY_IF", "KW_XQUERY_IMPORT",
            "KW_XQUERY_IN", "KW_XQUERY_INSTANCE", "KW_XQUERY_INTERSECT", "KW_XQUERY_IS", "KW_XQUERY_ITEM",
            "KW_XQUERY_LE", "KW_XQUERY_LET", "KW_XQUERY_LT", "KW_XQUERY_MOD", "KW_XQUERY_MODULE", "KW_XQUERY_NAMESPACE",
            "KW_XQUERY_NAMESPACE_COLON", "KW_XQUERY_NE", "KW_XQUERY_NODE", "KW_XQUERY_OF", "KW_XQUERY_OPTION",
            "KW_XQUERY_OR", "KW_XQUERY_ORDERING", "KW_XQUERY_PARENT_COLON", "KW_XQUERY_PRECEDING_COLON",
            "KW_XQUERY_PRECEDING_SIBLING_COLON", "KW_XQUERY_PROCESSING_INSTRUCTION", "KW_XQUERY_RETURN",
            "KW_XQUERY_SATISFIES", "KW_XQUERY_SCHEMA", "KW_XQUERY_SCHEMA_ATTR", "KW_XQUERY_SCHEMA_ELEM",
            "KW_XQUERY_SELF_COLON", "KW_XQUERY_SOME", "KW_XQUERY_TEXT", "KW_XQUERY_THEN", "KW_XQUERY_TO",
            "KW_XQUERY_TREAT", "KW_XQUERY_TYPESWITCH", "KW_XQUERY_UNION", "KW_XQUERY_VALIDATE", "KW_XQUERY_VARIABLE",
            "KW_XQUERY_VERSION", "KW_XQUERY_XQUERY", "LE", "LEFT_BRACE", "LEFT_BRACKET", "LEFT_PAREN",
            "LEFT_PAREN_POUND_SIGN", "LETTER", "LT", "LT_EXCLAMATION", "LT_GT", "MINUS", "OCTAL_DIGIT",
            "OCTAL_OR_HEX_LITERAL", "PERCENT", "PLUS", "POUND_SIGN", "QUESTION_MARK", "QUOTE", "RIGHT_BRACE",
            "RIGHT_BRACKET", "RIGHT_PAREN", "RIGHT_PAREN_POUND_SIGN", "SEMICOLON", "SLASH", "STAR", "STRING_LITERAL",
            "UNDERSCORE", "VERTICAL_BAR", "WS" };

    public static final int EOF = -1;
    public static final int AMPERSAND = 4;
    public static final int APPROX = 5;
    public static final int AT = 6;
    public static final int CHAR_NO_DOUBLE_QUOTE = 7;
    public static final int CHAR_NO_QUOTE = 8;
    public static final int CIRCUMFLEX = 9;
    public static final int COLON = 10;
    public static final int COLON_EQ = 11;
    public static final int COMMA = 12;
    public static final int COMMENT_SQL = 13;
    public static final int COMMENT_XQUERY = 14;
    public static final int DECIMAL_LITERAL = 15;
    public static final int DIGIT = 16;
    public static final int DOLLAR = 17;
    public static final int DOT = 18;
    public static final int DOUBLE_DOT = 19;
    public static final int DOUBLE_GT = 20;
    public static final int DOUBLE_LT = 21;
    public static final int DOUBLE_QUOTE = 22;
    public static final int DOUBLE_SLASH = 23;
    public static final int EQ = 24;
    public static final int EXCLAMATION = 25;
    public static final int EXCLAMATION_EQ = 26;
    public static final int GE = 27;
    public static final int GT = 28;
    public static final int HEX_DIGIT = 29;
    public static final int IDENTIFIER = 30;
    public static final int IDENTIFIER_CONTINUE = 31;
    public static final int IDENTIFIER_START = 32;
    public static final int KW_SQL_ALL = 33;
    public static final int KW_SQL_ALTER = 34;
    public static final int KW_SQL_BY = 35;
    public static final int KW_SQL_CREATE = 36;
    public static final int KW_SQL_DATABASE = 37;
    public static final int KW_SQL_DELETE = 38;
    public static final int KW_SQL_DISTINCT = 39;
    public static final int KW_SQL_DROP = 40;
    public static final int KW_SQL_FROM = 41;
    public static final int KW_SQL_INDEX = 42;
    public static final int KW_SQL_INSERT = 43;
    public static final int KW_SQL_INTO = 44;
    public static final int KW_SQL_ORDER = 45;
    public static final int KW_SQL_SELECT = 46;
    public static final int KW_SQL_SET = 47;
    public static final int KW_SQL_TABLE = 48;
    public static final int KW_SQL_TRUNCATE = 49;
    public static final int KW_SQL_UNIQUE = 50;
    public static final int KW_SQL_UPDATE = 51;
    public static final int KW_SQL_VALUES = 52;
    public static final int KW_SQL_VIEW = 53;
    public static final int KW_SQL_WHERE = 54;
    public static final int KW_XQUERY_ANCESTOR_COLON = 55;
    public static final int KW_XQUERY_ANCESTOR_OR_SELF_COLON = 56;
    public static final int KW_XQUERY_AND = 57;
    public static final int KW_XQUERY_AS = 58;
    public static final int KW_XQUERY_ATTRIBUTE_COLON = 59;
    public static final int KW_XQUERY_BASE_URI = 60;
    public static final int KW_XQUERY_BOUNDARY_SPACE = 61;
    public static final int KW_XQUERY_CASE = 62;
    public static final int KW_XQUERY_CAST = 63;
    public static final int KW_XQUERY_CASTABLE = 64;
    public static final int KW_XQUERY_CHILD_COLON = 65;
    public static final int KW_XQUERY_COMMENT = 66;
    public static final int KW_XQUERY_CONSTRUCTION = 67;
    public static final int KW_XQUERY_COPY_NAMESPACES = 68;
    public static final int KW_XQUERY_DECLARE = 69;
    public static final int KW_XQUERY_DEFAULT = 70;
    public static final int KW_XQUERY_DESCENDANT_COLON = 71;
    public static final int KW_XQUERY_DESCENDANT_OR_SELF_COLON = 72;
    public static final int KW_XQUERY_DIV = 73;
    public static final int KW_XQUERY_DOCNODE = 74;
    public static final int KW_XQUERY_ELEMENT = 75;
    public static final int KW_XQUERY_ELSE = 76;
    public static final int KW_XQUERY_EMPTY_SEQUENCE = 77;
    public static final int KW_XQUERY_EQ = 78;
    public static final int KW_XQUERY_EVERY = 79;
    public static final int KW_XQUERY_EXCEPT = 80;
    public static final int KW_XQUERY_FOLLOWING_COLON = 81;
    public static final int KW_XQUERY_FOLLOWING_SIBLING_COLON = 82;
    public static final int KW_XQUERY_FOR = 83;
    public static final int KW_XQUERY_FUNCTION = 84;
    public static final int KW_XQUERY_GE = 85;
    public static final int KW_XQUERY_GT = 86;
    public static final int KW_XQUERY_IDIV = 87;
    public static final int KW_XQUERY_IF = 88;
    public static final int KW_XQUERY_IMPORT = 89;
    public static final int KW_XQUERY_IN = 90;
    public static final int KW_XQUERY_INSTANCE = 91;
    public static final int KW_XQUERY_INTERSECT = 92;
    public static final int KW_XQUERY_IS = 93;
    public static final int KW_XQUERY_ITEM = 94;
    public static final int KW_XQUERY_LE = 95;
    public static final int KW_XQUERY_LET = 96;
    public static final int KW_XQUERY_LT = 97;
    public static final int KW_XQUERY_MOD = 98;
    public static final int KW_XQUERY_MODULE = 99;
    public static final int KW_XQUERY_NAMESPACE = 100;
    public static final int KW_XQUERY_NAMESPACE_COLON = 101;
    public static final int KW_XQUERY_NE = 102;
    public static final int KW_XQUERY_NODE = 103;
    public static final int KW_XQUERY_OF = 104;
    public static final int KW_XQUERY_OPTION = 105;
    public static final int KW_XQUERY_OR = 106;
    public static final int KW_XQUERY_ORDERING = 107;
    public static final int KW_XQUERY_PARENT_COLON = 108;
    public static final int KW_XQUERY_PRECEDING_COLON = 109;
    public static final int KW_XQUERY_PRECEDING_SIBLING_COLON = 110;
    public static final int KW_XQUERY_PROCESSING_INSTRUCTION = 111;
    public static final int KW_XQUERY_RETURN = 112;
    public static final int KW_XQUERY_SATISFIES = 113;
    public static final int KW_XQUERY_SCHEMA = 114;
    public static final int KW_XQUERY_SCHEMA_ATTR = 115;
    public static final int KW_XQUERY_SCHEMA_ELEM = 116;
    public static final int KW_XQUERY_SELF_COLON = 117;
    public static final int KW_XQUERY_SOME = 118;
    public static final int KW_XQUERY_TEXT = 119;
    public static final int KW_XQUERY_THEN = 120;
    public static final int KW_XQUERY_TO = 121;
    public static final int KW_XQUERY_TREAT = 122;
    public static final int KW_XQUERY_TYPESWITCH = 123;
    public static final int KW_XQUERY_UNION = 124;
    public static final int KW_XQUERY_VALIDATE = 125;
    public static final int KW_XQUERY_VARIABLE = 126;
    public static final int KW_XQUERY_VERSION = 127;
    public static final int KW_XQUERY_XQUERY = 128;
    public static final int LE = 129;
    public static final int LEFT_BRACE = 130;
    public static final int LEFT_BRACKET = 131;
    public static final int LEFT_PAREN = 132;
    public static final int LEFT_PAREN_POUND_SIGN = 133;
    public static final int LETTER = 134;
    public static final int LT = 135;
    public static final int LT_EXCLAMATION = 136;
    public static final int LT_GT = 137;
    public static final int MINUS = 138;
    public static final int OCTAL_DIGIT = 139;
    public static final int OCTAL_OR_HEX_LITERAL = 140;
    public static final int PERCENT = 141;
    public static final int PLUS = 142;
    public static final int POUND_SIGN = 143;
    public static final int QUESTION_MARK = 144;
    public static final int QUOTE = 145;
    public static final int RIGHT_BRACE = 146;
    public static final int RIGHT_BRACKET = 147;
    public static final int RIGHT_PAREN = 148;
    public static final int RIGHT_PAREN_POUND_SIGN = 149;
    public static final int SEMICOLON = 150;
    public static final int SLASH = 151;
    public static final int STAR = 152;
    public static final int STRING_LITERAL = 153;
    public static final int UNDERSCORE = 154;
    public static final int VERTICAL_BAR = 155;
    public static final int WS = 156;

    private String inputStr;

    public TestLang(TokenStream input, String inputStr) {
        this(input, new RecognizerSharedState());
        this.inputStr = inputStr;
    }

    public TestLang(TokenStream input, RecognizerSharedState state) {
        super(input, state);
    }

    public String[] getTokenNames() {
        return TestLang.tokenNames;
    }

    public String getGrammarFileName() {
        return "TestLang.g";
    }

    private int targets;
    private int result = UNKNOWN;

    public final int testLang() {
        switch (input.LA(1)) {
        case EOF:
            return UNKNOWN;
        case KW_SQL_SELECT:
            testSQLSelect(SQL_XQUERY);
            break;
        case KW_SQL_DELETE:
            testSQLDelete(SQL_XQUERY);
            break;
        case KW_SQL_INSERT:
            testSQLInsert(SQL_XQUERY);
            break;
        case KW_SQL_UPDATE:
            testSQLUpdate(SQL_XQUERY);
            break;
        case KW_SQL_CREATE:
            testSQLCreate(SQL_XQUERY);
            break;
        case KW_SQL_ALTER:
            testSQLAlter(SQL_XQUERY);
            break;
        case KW_SQL_DROP:
            testSQLDrop(SQL_XQUERY);
            break;
        case KW_SQL_TRUNCATE:
            testSQLTruncate(SQL_XQUERY);
            break;
        case LEFT_PAREN:
            testLDAP(LDAP_XQUERY);
            break;
        case KW_XQUERY_XQUERY:
            testXQueryVersion(XQUERY);
            break;
        case KW_XQUERY_MODULE:
            testXQueryModule(XQUERY);
            break;
        case KW_XQUERY_DECLARE:
            testXQueryDeclare(XQUERY);
            break;
        case KW_XQUERY_IMPORT:
            testXQueryImport(XQUERY);
            break;
        case KW_XQUERY_FOR:
            testXQueryFor(XQUERY);
            break;
        case KW_XQUERY_LET:
            testXQueryLet(XQUERY);
            break;
        case KW_XQUERY_SOME:
        case KW_XQUERY_EVERY:
            testXQueryQuantified(XQUERY);
            break;
        case KW_XQUERY_TYPESWITCH:
            testXQueryTypeSwitch(XQUERY);
            break;
        case KW_XQUERY_IF:
            testXQueryIf(XQUERY);
            break;
        case KW_XQUERY_VALIDATE:
            testXQueryValidate(XQUERY);
            break;
        case LEFT_PAREN_POUND_SIGN:
            testXQueryExtension(XQUERY);
            break;
        default:
            testCommand();
        }
        if (result == UNKNOWN) {
            reset();
            testXQuery(XQUERY);
        }
        return result;
    }

    public final void testSQLSelect(int targets) {
        this.targets = targets;
        if (findMatch(KW_SQL_FROM, SQL)) {
            result = SQL;
        }
    }

    public final void testSQLDelete(int targets) {
        this.targets = targets;
        if (findMatch(KW_SQL_FROM, SQL)) {
            result = SQL;
        }
    }

    public final void testSQLInsert(int targets) {
        this.targets = targets;
        if (findMatch(KW_SQL_INTO, SQL) && findMatch(KW_SQL_VALUES, SQL)) {
            result = SQL;
        }
    }

    public final void testSQLUpdate(int targets) {
        this.targets = targets;
        if (findMatch(KW_SQL_SET, SQL)) {
            result = SQL;
        }
    }

    public final void testSQLCreate(int targets) {
        this.targets = targets;
        if (input.LA(2) == KW_SQL_TABLE || input.LA(2) == KW_SQL_VIEW
                || (input.LA(2) == KW_SQL_UNIQUE && input.LA(3) == KW_SQL_INDEX) || input.LA(2) == KW_SQL_INDEX
                || input.LA(2) == KW_SQL_DATABASE) {
            result = SQL;
        }
    }

    public final void testSQLAlter(int targets) {
        this.targets = targets;
        if (input.LA(2) == KW_SQL_TABLE) {
            result = SQL;
        }
    }

    public final void testSQLDrop(int targets) {
        this.targets = targets;
        if (input.LA(2) == KW_SQL_TABLE || input.LA(2) == KW_SQL_VIEW || input.LA(2) == KW_SQL_INDEX
                || input.LA(2) == KW_SQL_ALL) {
            result = SQL;
        }
    }

    public final void testSQLTruncate(int targets) {
        this.targets = targets;
        if (input.LA(2) == KW_SQL_TABLE) {
            result = SQL;
        }
    }

    public final void testLDAP(int targets) {
        this.targets = targets;
        if (checkTarget(LDAP))
            result = LDAP;
    }

    public void testCommand() {
        CharStream cs = new ANTLRStringStream(inputStr);
        TestCommandLexer lexer = new TestCommandLexer(cs);
        CommonTokenStream tokens = new CommonTokenStream();
        tokens.setTokenSource(lexer);
        try {
            TestCommand parser = new TestCommand(tokens);
            if (parser.testCommand())
                result = COMMAND;
        } catch (IllegalArgumentException e) {
        }
    }

    public final void testXQueryVersion(int targets) {
        this.targets = targets;
        if (input.LA(2) == KW_XQUERY_VERSION) {
            result = XQUERY;
        }
    }

    public final void testXQueryModule(int targets) {
        this.targets = targets;
        if (input.LA(2) == KW_XQUERY_NAMESPACE) {
            result = XQUERY;
        }
    }

    public final void testXQueryDeclare(int targets) {
        this.targets = targets;
        if (input.LA(2) == KW_XQUERY_DEFAULT || input.LA(2) == KW_XQUERY_BOUNDARY_SPACE
                || input.LA(2) == KW_XQUERY_BASE_URI || input.LA(2) == KW_XQUERY_CONSTRUCTION
                || input.LA(2) == KW_XQUERY_ORDERING || input.LA(2) == KW_XQUERY_COPY_NAMESPACES
                || input.LA(2) == KW_XQUERY_NAMESPACE || input.LA(2) == KW_XQUERY_VARIABLE
                || input.LA(2) == KW_XQUERY_FUNCTION || input.LA(2) == KW_XQUERY_OPTION) {
            result = XQUERY;
        }
    }

    public final void testXQueryImport(int targets) {
        this.targets = targets;
        if (input.LA(2) == KW_XQUERY_SCHEMA || input.LA(2) == KW_XQUERY_MODULE) {
            result = XQUERY;
        }
    }

    public final void testXQueryFor(int targets) {
        this.targets = targets;
        if (findMatch(KW_XQUERY_IN, XQUERY) && findMatch(KW_XQUERY_RETURN, XQUERY)) {
            result = XQUERY;
        }
    }

    public final void testXQueryLet(int targets) {
        this.targets = targets;
        if (findMatch(COLON_EQ, XQUERY) && findMatch(KW_XQUERY_RETURN, XQUERY)) {
            result = XQUERY;
        }
    }

    public final void testXQueryQuantified(int targets) {
        this.targets = targets;
        if (findMatch(KW_XQUERY_IN, XQUERY) && findMatch(KW_XQUERY_SATISFIES, XQUERY)) {
            result = XQUERY;
        }
    }

    public final void testXQueryTypeSwitch(int targets) {
        this.targets = targets;
        if (findMatch(KW_XQUERY_CASE, XQUERY) && findMatch(KW_XQUERY_RETURN, XQUERY)) {
            result = XQUERY;
        }
    }

    public final void testXQueryIf(int targets) {
        this.targets = targets;
        if (findMatch(KW_XQUERY_THEN, XQUERY) && findMatch(KW_XQUERY_ELSE, XQUERY)) {
            result = XQUERY;
        }
    }

    public final void testXQueryValidate(int targets) {
        this.targets = targets;
        if (findMatch(LEFT_BRACE, XQUERY) && findMatch(RIGHT_BRACE, XQUERY)) {
            result = XQUERY;
        }
    }

    public final void testXQueryExtension(int targets) {
        this.targets = targets;
        if (findMatch(RIGHT_PAREN_POUND_SIGN, XQUERY) && findMatch(LEFT_BRACE, XQUERY)
                && findMatch(RIGHT_BRACE, XQUERY)) {
            result = XQUERY;
        }
    }

    public final void testXQuery(int targets) {
        this.targets = targets;
        if (checkTarget(XQUERY)) {
            result = XQUERY;
        }
    }

    public final boolean findMatch(int ttype, int target) {
        input.consume();
        while ((input.LA(1)) != EOF) {
            TargetedToken token = (TargetedToken) input.LT(1);
            targets &= token.getTargets();
            if ((targets & target) == 0)
                return false;
            if (token.getType() == ttype) {
                return true;
            }
            input.consume();
        }
        return false;
    }

    public final boolean checkTarget(int target) {
        int ttype;
        while ((ttype = input.LA(1)) != EOF) {
            TargetedToken token = (TargetedToken) input.LT(1);
            input.consume();
            int tokenTarget = token.getTargets();
            if (target == SQL && KW_SQL_ALL <= ttype && ttype <= KW_SQL_WHERE)
                tokenTarget = SQL;
            else if (target == XQUERY && KW_XQUERY_ANCESTOR_COLON <= ttype && ttype <= KW_XQUERY_XQUERY)
                tokenTarget = XQUERY;
            targets &= tokenTarget;
            if ((targets & target) == 0)
                return false;
        }
        return true;
    }
}
