// $ANTLR 3.4 TestLang.g 2011-11-20 22:47:16

package pac.util.parsers;

import pac.org.antlr.runtime.*;
import pac.util.Ret;

import java.util.Stack;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@SuppressWarnings({ "all", "warnings", "unchecked" })
public class TestLangLexer extends Lexer {
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

    private int LDAP = LangConstants.LDAP;
    private int SQL = LangConstants.SQL;
    private int XQUERY = LangConstants.XQUERY;
    private int LDAP_SQL_XQUERY = LDAP | SQL | XQUERY;
    private int LDAP_SQL = LDAP | SQL;
    private int SQL_XQUERY = SQL | XQUERY;
    private int targets = LDAP | SQL | XQUERY;

    @Override
    public Token emit() {
        return emit(new Ret());
    }

    public Token emit(Ret ret) {
        TargetedToken t = new TargetedToken(input, state.type, state.channel, state.tokenStartCharIndex,
                getCharIndex() - 1);
        t.setLine(state.tokenStartLine);
        t.setText(state.text);
        t.setCharPositionInLine(state.tokenStartCharPositionInLine);
        t.setTargets(targets);
        emit(t);
        return t;
    }

    @Override
    public void reportError(RecognitionException e) {
        throw new IllegalArgumentException(e);
    }

    public void reportError(RecognitionException e, Ret ret) {
        throw new IllegalArgumentException(e);
    }

    // delegates
    // delegators
    public Lexer[] getDelegates() {
        return new Lexer[] {};
    }

    public TestLangLexer() {
    }

    public TestLangLexer(CharStream input) {
        this(input, new RecognizerSharedState());
    }

    public TestLangLexer(CharStream input, RecognizerSharedState state) {
        super(input, state);
    }

    public String getGrammarFileName() {
        return "TestLang.g";
    }

    // $ANTLR start "KW_SQL_ALL"
    public final void mKW_SQL_ALL() throws RecognitionException {
        try {
            int _type = KW_SQL_ALL;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:58:35: ( 'all' )
            // TestLang.g:58:39: 'all'
            {
                match("all");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_SQL_ALL"

    // $ANTLR start "KW_SQL_ALTER"
    public final void mKW_SQL_ALTER() throws RecognitionException {
        try {
            int _type = KW_SQL_ALTER;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:59:35: ( 'alter' )
            // TestLang.g:59:39: 'alter'
            {
                match("alter");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_SQL_ALTER"

    // $ANTLR start "KW_SQL_BY"
    public final void mKW_SQL_BY() throws RecognitionException {
        try {
            int _type = KW_SQL_BY;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:60:35: ( 'by' )
            // TestLang.g:60:39: 'by'
            {
                match("by");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_SQL_BY"

    // $ANTLR start "KW_SQL_CREATE"
    public final void mKW_SQL_CREATE() throws RecognitionException {
        try {
            int _type = KW_SQL_CREATE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:61:35: ( 'create' )
            // TestLang.g:61:39: 'create'
            {
                match("create");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_SQL_CREATE"

    // $ANTLR start "KW_SQL_DATABASE"
    public final void mKW_SQL_DATABASE() throws RecognitionException {
        try {
            int _type = KW_SQL_DATABASE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:62:35: ( 'database' )
            // TestLang.g:62:39: 'database'
            {
                match("database");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_SQL_DATABASE"

    // $ANTLR start "KW_SQL_DELETE"
    public final void mKW_SQL_DELETE() throws RecognitionException {
        try {
            int _type = KW_SQL_DELETE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:63:35: ( 'delete' )
            // TestLang.g:63:39: 'delete'
            {
                match("delete");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_SQL_DELETE"

    // $ANTLR start "KW_SQL_DISTINCT"
    public final void mKW_SQL_DISTINCT() throws RecognitionException {
        try {
            int _type = KW_SQL_DISTINCT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:64:35: ( 'distinct' )
            // TestLang.g:64:39: 'distinct'
            {
                match("distinct");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_SQL_DISTINCT"

    // $ANTLR start "KW_SQL_DROP"
    public final void mKW_SQL_DROP() throws RecognitionException {
        try {
            int _type = KW_SQL_DROP;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:65:35: ( 'drop' )
            // TestLang.g:65:39: 'drop'
            {
                match("drop");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_SQL_DROP"

    // $ANTLR start "KW_SQL_FROM"
    public final void mKW_SQL_FROM() throws RecognitionException {
        try {
            int _type = KW_SQL_FROM;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:66:35: ( 'from' )
            // TestLang.g:66:39: 'from'
            {
                match("from");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_SQL_FROM"

    // $ANTLR start "KW_SQL_INDEX"
    public final void mKW_SQL_INDEX() throws RecognitionException {
        try {
            int _type = KW_SQL_INDEX;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:67:35: ( 'index' )
            // TestLang.g:67:39: 'index'
            {
                match("index");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_SQL_INDEX"

    // $ANTLR start "KW_SQL_INSERT"
    public final void mKW_SQL_INSERT() throws RecognitionException {
        try {
            int _type = KW_SQL_INSERT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:68:35: ( 'insert' )
            // TestLang.g:68:39: 'insert'
            {
                match("insert");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_SQL_INSERT"

    // $ANTLR start "KW_SQL_INTO"
    public final void mKW_SQL_INTO() throws RecognitionException {
        try {
            int _type = KW_SQL_INTO;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:69:35: ( 'into' )
            // TestLang.g:69:39: 'into'
            {
                match("into");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_SQL_INTO"

    // $ANTLR start "KW_SQL_ORDER"
    public final void mKW_SQL_ORDER() throws RecognitionException {
        try {
            int _type = KW_SQL_ORDER;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:70:35: ( 'order' )
            // TestLang.g:70:39: 'order'
            {
                match("order");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_SQL_ORDER"

    // $ANTLR start "KW_SQL_SELECT"
    public final void mKW_SQL_SELECT() throws RecognitionException {
        try {
            int _type = KW_SQL_SELECT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:71:35: ( 'select' )
            // TestLang.g:71:39: 'select'
            {
                match("select");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_SQL_SELECT"

    // $ANTLR start "KW_SQL_SET"
    public final void mKW_SQL_SET() throws RecognitionException {
        try {
            int _type = KW_SQL_SET;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:72:35: ( 'set' )
            // TestLang.g:72:39: 'set'
            {
                match("set");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_SQL_SET"

    // $ANTLR start "KW_SQL_TABLE"
    public final void mKW_SQL_TABLE() throws RecognitionException {
        try {
            int _type = KW_SQL_TABLE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:73:35: ( 'table' )
            // TestLang.g:73:39: 'table'
            {
                match("table");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_SQL_TABLE"

    // $ANTLR start "KW_SQL_TRUNCATE"
    public final void mKW_SQL_TRUNCATE() throws RecognitionException {
        try {
            int _type = KW_SQL_TRUNCATE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:74:35: ( 'truncate' )
            // TestLang.g:74:39: 'truncate'
            {
                match("truncate");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_SQL_TRUNCATE"

    // $ANTLR start "KW_SQL_UNIQUE"
    public final void mKW_SQL_UNIQUE() throws RecognitionException {
        try {
            int _type = KW_SQL_UNIQUE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:75:35: ( 'unique' )
            // TestLang.g:75:39: 'unique'
            {
                match("unique");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_SQL_UNIQUE"

    // $ANTLR start "KW_SQL_UPDATE"
    public final void mKW_SQL_UPDATE() throws RecognitionException {
        try {
            int _type = KW_SQL_UPDATE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:76:35: ( 'update' )
            // TestLang.g:76:39: 'update'
            {
                match("update");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_SQL_UPDATE"

    // $ANTLR start "KW_SQL_VALUES"
    public final void mKW_SQL_VALUES() throws RecognitionException {
        try {
            int _type = KW_SQL_VALUES;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:77:35: ( 'values' )
            // TestLang.g:77:39: 'values'
            {
                match("values");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_SQL_VALUES"

    // $ANTLR start "KW_SQL_VIEW"
    public final void mKW_SQL_VIEW() throws RecognitionException {
        try {
            int _type = KW_SQL_VIEW;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:78:35: ( 'view' )
            // TestLang.g:78:39: 'view'
            {
                match("view");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_SQL_VIEW"

    // $ANTLR start "KW_SQL_WHERE"
    public final void mKW_SQL_WHERE() throws RecognitionException {
        try {
            int _type = KW_SQL_WHERE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:79:35: ( 'where' )
            // TestLang.g:79:39: 'where'
            {
                match("where");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_SQL_WHERE"

    // $ANTLR start "KW_XQUERY_AND"
    public final void mKW_XQUERY_AND() throws RecognitionException {
        try {
            int _type = KW_XQUERY_AND;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:82:38: ( 'and' )
            // TestLang.g:82:42: 'and'
            {
                match("and");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_AND"

    // $ANTLR start "KW_XQUERY_AS"
    public final void mKW_XQUERY_AS() throws RecognitionException {
        try {
            int _type = KW_XQUERY_AS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:83:38: ( 'as' )
            // TestLang.g:83:42: 'as'
            {
                match("as");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_AS"

    // $ANTLR start "KW_XQUERY_BASE_URI"
    public final void mKW_XQUERY_BASE_URI() throws RecognitionException {
        try {
            int _type = KW_XQUERY_BASE_URI;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:84:38: ( 'base-uri' )
            // TestLang.g:84:42: 'base-uri'
            {
                match("base-uri");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_BASE_URI"

    // $ANTLR start "KW_XQUERY_BOUNDARY_SPACE"
    public final void mKW_XQUERY_BOUNDARY_SPACE() throws RecognitionException {
        try {
            int _type = KW_XQUERY_BOUNDARY_SPACE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:85:38: ( 'boundary-space' )
            // TestLang.g:85:42: 'boundary-space'
            {
                match("boundary-space");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_BOUNDARY_SPACE"

    // $ANTLR start "KW_XQUERY_CASE"
    public final void mKW_XQUERY_CASE() throws RecognitionException {
        try {
            int _type = KW_XQUERY_CASE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:86:38: ( 'case' )
            // TestLang.g:86:42: 'case'
            {
                match("case");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_CASE"

    // $ANTLR start "KW_XQUERY_CAST"
    public final void mKW_XQUERY_CAST() throws RecognitionException {
        try {
            int _type = KW_XQUERY_CAST;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:87:38: ( 'cast' )
            // TestLang.g:87:42: 'cast'
            {
                match("cast");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_CAST"

    // $ANTLR start "KW_XQUERY_CASTABLE"
    public final void mKW_XQUERY_CASTABLE() throws RecognitionException {
        try {
            int _type = KW_XQUERY_CASTABLE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:88:38: ( 'castable' )
            // TestLang.g:88:42: 'castable'
            {
                match("castable");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_CASTABLE"

    // $ANTLR start "KW_XQUERY_COMMENT"
    public final void mKW_XQUERY_COMMENT() throws RecognitionException {
        try {
            int _type = KW_XQUERY_COMMENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:89:38: ( 'comment' )
            // TestLang.g:89:42: 'comment'
            {
                match("comment");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_COMMENT"

    // $ANTLR start "KW_XQUERY_CONSTRUCTION"
    public final void mKW_XQUERY_CONSTRUCTION() throws RecognitionException {
        try {
            int _type = KW_XQUERY_CONSTRUCTION;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:90:38: ( 'construction' )
            // TestLang.g:90:42: 'construction'
            {
                match("construction");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_CONSTRUCTION"

    // $ANTLR start "KW_XQUERY_COPY_NAMESPACES"
    public final void mKW_XQUERY_COPY_NAMESPACES() throws RecognitionException {
        try {
            int _type = KW_XQUERY_COPY_NAMESPACES;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:91:38: ( 'copy-namespaces' )
            // TestLang.g:91:42: 'copy-namespaces'
            {
                match("copy-namespaces");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_COPY_NAMESPACES"

    // $ANTLR start "KW_XQUERY_DECLARE"
    public final void mKW_XQUERY_DECLARE() throws RecognitionException {
        try {
            int _type = KW_XQUERY_DECLARE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:92:38: ( 'declare' )
            // TestLang.g:92:42: 'declare'
            {
                match("declare");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_DECLARE"

    // $ANTLR start "KW_XQUERY_DEFAULT"
    public final void mKW_XQUERY_DEFAULT() throws RecognitionException {
        try {
            int _type = KW_XQUERY_DEFAULT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:93:38: ( 'default' )
            // TestLang.g:93:42: 'default'
            {
                match("default");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_DEFAULT"

    // $ANTLR start "KW_XQUERY_DIV"
    public final void mKW_XQUERY_DIV() throws RecognitionException {
        try {
            int _type = KW_XQUERY_DIV;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:94:38: ( 'div' )
            // TestLang.g:94:42: 'div'
            {
                match("div");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_DIV"

    // $ANTLR start "KW_XQUERY_DOCNODE"
    public final void mKW_XQUERY_DOCNODE() throws RecognitionException {
        try {
            int _type = KW_XQUERY_DOCNODE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:95:38: ( 'document-node' )
            // TestLang.g:95:42: 'document-node'
            {
                match("document-node");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_DOCNODE"

    // $ANTLR start "KW_XQUERY_ELEMENT"
    public final void mKW_XQUERY_ELEMENT() throws RecognitionException {
        try {
            int _type = KW_XQUERY_ELEMENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:96:38: ( 'element' )
            // TestLang.g:96:42: 'element'
            {
                match("element");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_ELEMENT"

    // $ANTLR start "KW_XQUERY_ELSE"
    public final void mKW_XQUERY_ELSE() throws RecognitionException {
        try {
            int _type = KW_XQUERY_ELSE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:97:38: ( 'else' )
            // TestLang.g:97:42: 'else'
            {
                match("else");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_ELSE"

    // $ANTLR start "KW_XQUERY_EMPTY_SEQUENCE"
    public final void mKW_XQUERY_EMPTY_SEQUENCE() throws RecognitionException {
        try {
            int _type = KW_XQUERY_EMPTY_SEQUENCE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:98:38: ( 'empty-sequence' )
            // TestLang.g:98:42: 'empty-sequence'
            {
                match("empty-sequence");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_EMPTY_SEQUENCE"

    // $ANTLR start "KW_XQUERY_EQ"
    public final void mKW_XQUERY_EQ() throws RecognitionException {
        try {
            int _type = KW_XQUERY_EQ;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:99:38: ( 'eq' )
            // TestLang.g:99:42: 'eq'
            {
                match("eq");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_EQ"

    // $ANTLR start "KW_XQUERY_EVERY"
    public final void mKW_XQUERY_EVERY() throws RecognitionException {
        try {
            int _type = KW_XQUERY_EVERY;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:100:38: ( 'every' )
            // TestLang.g:100:42: 'every'
            {
                match("every");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_EVERY"

    // $ANTLR start "KW_XQUERY_EXCEPT"
    public final void mKW_XQUERY_EXCEPT() throws RecognitionException {
        try {
            int _type = KW_XQUERY_EXCEPT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:101:38: ( 'except' )
            // TestLang.g:101:42: 'except'
            {
                match("except");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_EXCEPT"

    // $ANTLR start "KW_XQUERY_FOR"
    public final void mKW_XQUERY_FOR() throws RecognitionException {
        try {
            int _type = KW_XQUERY_FOR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:102:38: ( 'for' )
            // TestLang.g:102:42: 'for'
            {
                match("for");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_FOR"

    // $ANTLR start "KW_XQUERY_FUNCTION"
    public final void mKW_XQUERY_FUNCTION() throws RecognitionException {
        try {
            int _type = KW_XQUERY_FUNCTION;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:103:38: ( 'function' )
            // TestLang.g:103:42: 'function'
            {
                match("function");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_FUNCTION"

    // $ANTLR start "KW_XQUERY_GE"
    public final void mKW_XQUERY_GE() throws RecognitionException {
        try {
            int _type = KW_XQUERY_GE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:104:38: ( 'ge' )
            // TestLang.g:104:42: 'ge'
            {
                match("ge");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_GE"

    // $ANTLR start "KW_XQUERY_GT"
    public final void mKW_XQUERY_GT() throws RecognitionException {
        try {
            int _type = KW_XQUERY_GT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:105:38: ( 'gt' )
            // TestLang.g:105:42: 'gt'
            {
                match("gt");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_GT"

    // $ANTLR start "KW_XQUERY_IDIV"
    public final void mKW_XQUERY_IDIV() throws RecognitionException {
        try {
            int _type = KW_XQUERY_IDIV;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:106:38: ( 'idiv' )
            // TestLang.g:106:42: 'idiv'
            {
                match("idiv");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_IDIV"

    // $ANTLR start "KW_XQUERY_IF"
    public final void mKW_XQUERY_IF() throws RecognitionException {
        try {
            int _type = KW_XQUERY_IF;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:107:38: ( 'if' )
            // TestLang.g:107:42: 'if'
            {
                match("if");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_IF"

    // $ANTLR start "KW_XQUERY_IMPORT"
    public final void mKW_XQUERY_IMPORT() throws RecognitionException {
        try {
            int _type = KW_XQUERY_IMPORT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:108:38: ( 'import' )
            // TestLang.g:108:42: 'import'
            {
                match("import");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_IMPORT"

    // $ANTLR start "KW_XQUERY_IN"
    public final void mKW_XQUERY_IN() throws RecognitionException {
        try {
            int _type = KW_XQUERY_IN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:109:38: ( 'in' )
            // TestLang.g:109:42: 'in'
            {
                match("in");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_IN"

    // $ANTLR start "KW_XQUERY_INSTANCE"
    public final void mKW_XQUERY_INSTANCE() throws RecognitionException {
        try {
            int _type = KW_XQUERY_INSTANCE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:110:38: ( 'instance' )
            // TestLang.g:110:42: 'instance'
            {
                match("instance");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_INSTANCE"

    // $ANTLR start "KW_XQUERY_INTERSECT"
    public final void mKW_XQUERY_INTERSECT() throws RecognitionException {
        try {
            int _type = KW_XQUERY_INTERSECT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:111:38: ( 'intersect' )
            // TestLang.g:111:42: 'intersect'
            {
                match("intersect");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_INTERSECT"

    // $ANTLR start "KW_XQUERY_IS"
    public final void mKW_XQUERY_IS() throws RecognitionException {
        try {
            int _type = KW_XQUERY_IS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:112:38: ( 'is' )
            // TestLang.g:112:42: 'is'
            {
                match("is");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_IS"

    // $ANTLR start "KW_XQUERY_ITEM"
    public final void mKW_XQUERY_ITEM() throws RecognitionException {
        try {
            int _type = KW_XQUERY_ITEM;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:113:38: ( 'item' )
            // TestLang.g:113:42: 'item'
            {
                match("item");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_ITEM"

    // $ANTLR start "KW_XQUERY_LE"
    public final void mKW_XQUERY_LE() throws RecognitionException {
        try {
            int _type = KW_XQUERY_LE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:114:38: ( 'le' )
            // TestLang.g:114:42: 'le'
            {
                match("le");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_LE"

    // $ANTLR start "KW_XQUERY_LET"
    public final void mKW_XQUERY_LET() throws RecognitionException {
        try {
            int _type = KW_XQUERY_LET;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:115:38: ( 'let' )
            // TestLang.g:115:42: 'let'
            {
                match("let");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_LET"

    // $ANTLR start "KW_XQUERY_LT"
    public final void mKW_XQUERY_LT() throws RecognitionException {
        try {
            int _type = KW_XQUERY_LT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:116:38: ( 'lt' )
            // TestLang.g:116:42: 'lt'
            {
                match("lt");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_LT"

    // $ANTLR start "KW_XQUERY_MOD"
    public final void mKW_XQUERY_MOD() throws RecognitionException {
        try {
            int _type = KW_XQUERY_MOD;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:117:38: ( 'mod' )
            // TestLang.g:117:42: 'mod'
            {
                match("mod");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_MOD"

    // $ANTLR start "KW_XQUERY_MODULE"
    public final void mKW_XQUERY_MODULE() throws RecognitionException {
        try {
            int _type = KW_XQUERY_MODULE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:118:38: ( 'module' )
            // TestLang.g:118:42: 'module'
            {
                match("module");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_MODULE"

    // $ANTLR start "KW_XQUERY_NAMESPACE"
    public final void mKW_XQUERY_NAMESPACE() throws RecognitionException {
        try {
            int _type = KW_XQUERY_NAMESPACE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:119:38: ( 'namespace' )
            // TestLang.g:119:42: 'namespace'
            {
                match("namespace");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_NAMESPACE"

    // $ANTLR start "KW_XQUERY_NE"
    public final void mKW_XQUERY_NE() throws RecognitionException {
        try {
            int _type = KW_XQUERY_NE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:120:38: ( 'ne' )
            // TestLang.g:120:42: 'ne'
            {
                match("ne");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_NE"

    // $ANTLR start "KW_XQUERY_NODE"
    public final void mKW_XQUERY_NODE() throws RecognitionException {
        try {
            int _type = KW_XQUERY_NODE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:121:38: ( 'node' )
            // TestLang.g:121:42: 'node'
            {
                match("node");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_NODE"

    // $ANTLR start "KW_XQUERY_OF"
    public final void mKW_XQUERY_OF() throws RecognitionException {
        try {
            int _type = KW_XQUERY_OF;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:122:38: ( 'of' )
            // TestLang.g:122:42: 'of'
            {
                match("of");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_OF"

    // $ANTLR start "KW_XQUERY_OPTION"
    public final void mKW_XQUERY_OPTION() throws RecognitionException {
        try {
            int _type = KW_XQUERY_OPTION;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:123:38: ( 'option' )
            // TestLang.g:123:42: 'option'
            {
                match("option");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_OPTION"

    // $ANTLR start "KW_XQUERY_OR"
    public final void mKW_XQUERY_OR() throws RecognitionException {
        try {
            int _type = KW_XQUERY_OR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:124:38: ( 'or' )
            // TestLang.g:124:42: 'or'
            {
                match("or");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_OR"

    // $ANTLR start "KW_XQUERY_ORDERING"
    public final void mKW_XQUERY_ORDERING() throws RecognitionException {
        try {
            int _type = KW_XQUERY_ORDERING;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:125:38: ( 'ordering' )
            // TestLang.g:125:42: 'ordering'
            {
                match("ordering");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_ORDERING"

    // $ANTLR start "KW_XQUERY_PROCESSING_INSTRUCTION"
    public final void mKW_XQUERY_PROCESSING_INSTRUCTION() throws RecognitionException {
        try {
            int _type = KW_XQUERY_PROCESSING_INSTRUCTION;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:126:38: ( 'processing-instruction' )
            // TestLang.g:126:42: 'processing-instruction'
            {
                match("processing-instruction");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_PROCESSING_INSTRUCTION"

    // $ANTLR start "KW_XQUERY_RETURN"
    public final void mKW_XQUERY_RETURN() throws RecognitionException {
        try {
            int _type = KW_XQUERY_RETURN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:127:38: ( 'return' )
            // TestLang.g:127:42: 'return'
            {
                match("return");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_RETURN"

    // $ANTLR start "KW_XQUERY_SATISFIES"
    public final void mKW_XQUERY_SATISFIES() throws RecognitionException {
        try {
            int _type = KW_XQUERY_SATISFIES;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:128:38: ( 'satisfies' )
            // TestLang.g:128:42: 'satisfies'
            {
                match("satisfies");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_SATISFIES"

    // $ANTLR start "KW_XQUERY_SCHEMA"
    public final void mKW_XQUERY_SCHEMA() throws RecognitionException {
        try {
            int _type = KW_XQUERY_SCHEMA;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:129:38: ( 'schema' )
            // TestLang.g:129:42: 'schema'
            {
                match("schema");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_SCHEMA"

    // $ANTLR start "KW_XQUERY_SCHEMA_ATTR"
    public final void mKW_XQUERY_SCHEMA_ATTR() throws RecognitionException {
        try {
            int _type = KW_XQUERY_SCHEMA_ATTR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:130:38: ( 'schema-attribute' )
            // TestLang.g:130:42: 'schema-attribute'
            {
                match("schema-attribute");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_SCHEMA_ATTR"

    // $ANTLR start "KW_XQUERY_SCHEMA_ELEM"
    public final void mKW_XQUERY_SCHEMA_ELEM() throws RecognitionException {
        try {
            int _type = KW_XQUERY_SCHEMA_ELEM;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:131:38: ( 'schema-element' )
            // TestLang.g:131:42: 'schema-element'
            {
                match("schema-element");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_SCHEMA_ELEM"

    // $ANTLR start "KW_XQUERY_SOME"
    public final void mKW_XQUERY_SOME() throws RecognitionException {
        try {
            int _type = KW_XQUERY_SOME;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:132:38: ( 'some' )
            // TestLang.g:132:42: 'some'
            {
                match("some");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_SOME"

    // $ANTLR start "KW_XQUERY_TEXT"
    public final void mKW_XQUERY_TEXT() throws RecognitionException {
        try {
            int _type = KW_XQUERY_TEXT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:133:38: ( 'text' )
            // TestLang.g:133:42: 'text'
            {
                match("text");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_TEXT"

    // $ANTLR start "KW_XQUERY_THEN"
    public final void mKW_XQUERY_THEN() throws RecognitionException {
        try {
            int _type = KW_XQUERY_THEN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:134:38: ( 'then' )
            // TestLang.g:134:42: 'then'
            {
                match("then");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_THEN"

    // $ANTLR start "KW_XQUERY_TO"
    public final void mKW_XQUERY_TO() throws RecognitionException {
        try {
            int _type = KW_XQUERY_TO;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:135:38: ( 'to' )
            // TestLang.g:135:42: 'to'
            {
                match("to");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_TO"

    // $ANTLR start "KW_XQUERY_TREAT"
    public final void mKW_XQUERY_TREAT() throws RecognitionException {
        try {
            int _type = KW_XQUERY_TREAT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:136:38: ( 'treat' )
            // TestLang.g:136:42: 'treat'
            {
                match("treat");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_TREAT"

    // $ANTLR start "KW_XQUERY_TYPESWITCH"
    public final void mKW_XQUERY_TYPESWITCH() throws RecognitionException {
        try {
            int _type = KW_XQUERY_TYPESWITCH;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:137:38: ( 'typeswitch' )
            // TestLang.g:137:42: 'typeswitch'
            {
                match("typeswitch");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_TYPESWITCH"

    // $ANTLR start "KW_XQUERY_UNION"
    public final void mKW_XQUERY_UNION() throws RecognitionException {
        try {
            int _type = KW_XQUERY_UNION;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:138:38: ( 'union' )
            // TestLang.g:138:42: 'union'
            {
                match("union");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_UNION"

    // $ANTLR start "KW_XQUERY_VARIABLE"
    public final void mKW_XQUERY_VARIABLE() throws RecognitionException {
        try {
            int _type = KW_XQUERY_VARIABLE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:139:38: ( 'variable' )
            // TestLang.g:139:42: 'variable'
            {
                match("variable");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_VARIABLE"

    // $ANTLR start "KW_XQUERY_VALIDATE"
    public final void mKW_XQUERY_VALIDATE() throws RecognitionException {
        try {
            int _type = KW_XQUERY_VALIDATE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:140:38: ( 'validate' )
            // TestLang.g:140:42: 'validate'
            {
                match("validate");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_VALIDATE"

    // $ANTLR start "KW_XQUERY_VERSION"
    public final void mKW_XQUERY_VERSION() throws RecognitionException {
        try {
            int _type = KW_XQUERY_VERSION;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:141:38: ( 'version' )
            // TestLang.g:141:42: 'version'
            {
                match("version");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_VERSION"

    // $ANTLR start "KW_XQUERY_XQUERY"
    public final void mKW_XQUERY_XQUERY() throws RecognitionException {
        try {
            int _type = KW_XQUERY_XQUERY;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:142:38: ( 'xquery' )
            // TestLang.g:142:42: 'xquery'
            {
                match("xquery");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_XQUERY"

    // $ANTLR start "KW_XQUERY_ANCESTOR_COLON"
    public final void mKW_XQUERY_ANCESTOR_COLON() throws RecognitionException {
        try {
            int _type = KW_XQUERY_ANCESTOR_COLON;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:143:38: ( 'ancestor::' )
            // TestLang.g:143:42: 'ancestor::'
            {
                match("ancestor::");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_ANCESTOR_COLON"

    // $ANTLR start "KW_XQUERY_ANCESTOR_OR_SELF_COLON"
    public final void mKW_XQUERY_ANCESTOR_OR_SELF_COLON() throws RecognitionException {
        try {
            int _type = KW_XQUERY_ANCESTOR_OR_SELF_COLON;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:144:38: ( 'ancestor-or-self::' )
            // TestLang.g:144:42: 'ancestor-or-self::'
            {
                match("ancestor-or-self::");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_ANCESTOR_OR_SELF_COLON"

    // $ANTLR start "KW_XQUERY_ATTRIBUTE_COLON"
    public final void mKW_XQUERY_ATTRIBUTE_COLON() throws RecognitionException {
        try {
            int _type = KW_XQUERY_ATTRIBUTE_COLON;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:145:38: ( 'attribute::' )
            // TestLang.g:145:42: 'attribute::'
            {
                match("attribute::");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_ATTRIBUTE_COLON"

    // $ANTLR start "KW_XQUERY_CHILD_COLON"
    public final void mKW_XQUERY_CHILD_COLON() throws RecognitionException {
        try {
            int _type = KW_XQUERY_CHILD_COLON;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:146:38: ( 'child::' )
            // TestLang.g:146:42: 'child::'
            {
                match("child::");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_CHILD_COLON"

    // $ANTLR start "KW_XQUERY_DESCENDANT_COLON"
    public final void mKW_XQUERY_DESCENDANT_COLON() throws RecognitionException {
        try {
            int _type = KW_XQUERY_DESCENDANT_COLON;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:147:38: ( 'descendant::' )
            // TestLang.g:147:42: 'descendant::'
            {
                match("descendant::");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_DESCENDANT_COLON"

    // $ANTLR start "KW_XQUERY_DESCENDANT_OR_SELF_COLON"
    public final void mKW_XQUERY_DESCENDANT_OR_SELF_COLON() throws RecognitionException {
        try {
            int _type = KW_XQUERY_DESCENDANT_OR_SELF_COLON;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:148:38: ( 'descendant-or-self::' )
            // TestLang.g:148:42: 'descendant-or-self::'
            {
                match("descendant-or-self::");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_DESCENDANT_OR_SELF_COLON"

    // $ANTLR start "KW_XQUERY_FOLLOWING_COLON"
    public final void mKW_XQUERY_FOLLOWING_COLON() throws RecognitionException {
        try {
            int _type = KW_XQUERY_FOLLOWING_COLON;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:149:38: ( 'following::' )
            // TestLang.g:149:42: 'following::'
            {
                match("following::");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_FOLLOWING_COLON"

    // $ANTLR start "KW_XQUERY_FOLLOWING_SIBLING_COLON"
    public final void mKW_XQUERY_FOLLOWING_SIBLING_COLON() throws RecognitionException {
        try {
            int _type = KW_XQUERY_FOLLOWING_SIBLING_COLON;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:150:38: ( 'following-sibling::' )
            // TestLang.g:150:42: 'following-sibling::'
            {
                match("following-sibling::");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_FOLLOWING_SIBLING_COLON"

    // $ANTLR start "KW_XQUERY_NAMESPACE_COLON"
    public final void mKW_XQUERY_NAMESPACE_COLON() throws RecognitionException {
        try {
            int _type = KW_XQUERY_NAMESPACE_COLON;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:151:38: ( 'namespace::' )
            // TestLang.g:151:42: 'namespace::'
            {
                match("namespace::");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_NAMESPACE_COLON"

    // $ANTLR start "KW_XQUERY_PARENT_COLON"
    public final void mKW_XQUERY_PARENT_COLON() throws RecognitionException {
        try {
            int _type = KW_XQUERY_PARENT_COLON;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:152:38: ( 'parent::' )
            // TestLang.g:152:42: 'parent::'
            {
                match("parent::");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_PARENT_COLON"

    // $ANTLR start "KW_XQUERY_PRECEDING_COLON"
    public final void mKW_XQUERY_PRECEDING_COLON() throws RecognitionException {
        try {
            int _type = KW_XQUERY_PRECEDING_COLON;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:153:38: ( 'preceding::' )
            // TestLang.g:153:42: 'preceding::'
            {
                match("preceding::");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_PRECEDING_COLON"

    // $ANTLR start "KW_XQUERY_PRECEDING_SIBLING_COLON"
    public final void mKW_XQUERY_PRECEDING_SIBLING_COLON() throws RecognitionException {
        try {
            int _type = KW_XQUERY_PRECEDING_SIBLING_COLON;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:154:38: ( 'preceding-sibling::' )
            // TestLang.g:154:42: 'preceding-sibling::'
            {
                match("preceding-sibling::");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_PRECEDING_SIBLING_COLON"

    // $ANTLR start "KW_XQUERY_SELF_COLON"
    public final void mKW_XQUERY_SELF_COLON() throws RecognitionException {
        try {
            int _type = KW_XQUERY_SELF_COLON;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:155:38: ( 'self::' )
            // TestLang.g:155:42: 'self::'
            {
                match("self::");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "KW_XQUERY_SELF_COLON"

    // $ANTLR start "LEFT_PAREN"
    public final void mLEFT_PAREN() throws RecognitionException {
        try {
            int _type = LEFT_PAREN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:162:28: ( '(' )
            // TestLang.g:162:32: '('
            {
                match('(');
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "LEFT_PAREN"

    // $ANTLR start "RIGHT_PAREN"
    public final void mRIGHT_PAREN() throws RecognitionException {
        try {
            int _type = RIGHT_PAREN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:163:28: ( ')' )
            // TestLang.g:163:32: ')'
            {
                match(')');
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "RIGHT_PAREN"

    // $ANTLR start "STAR"
    public final void mSTAR() throws RecognitionException {
        try {
            int _type = STAR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:164:28: ( '*' )
            // TestLang.g:164:32: '*'
            {
                match('*');
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "STAR"

    // $ANTLR start "EQ"
    public final void mEQ() throws RecognitionException {
        try {
            int _type = EQ;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:165:28: ( '=' )
            // TestLang.g:165:32: '='
            {
                match('=');
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "EQ"

    // $ANTLR start "LE"
    public final void mLE() throws RecognitionException {
        try {
            int _type = LE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:166:28: ( '<=' )
            // TestLang.g:166:32: '<='
            {
                match("<=");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "LE"

    // $ANTLR start "GE"
    public final void mGE() throws RecognitionException {
        try {
            int _type = GE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:167:28: ( '>=' )
            // TestLang.g:167:32: '>='
            {
                match(">=");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "GE"

    // $ANTLR start "VERTICAL_BAR"
    public final void mVERTICAL_BAR() throws RecognitionException {
        try {
            int _type = VERTICAL_BAR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:168:28: ( '|' )
            // TestLang.g:168:32: '|'
            {
                match('|');
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "VERTICAL_BAR"

    // $ANTLR start "AMPERSAND"
    public final void mAMPERSAND() throws RecognitionException {
        try {
            int _type = AMPERSAND;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:171:28: ( '&' )
            // TestLang.g:171:32: '&'
            {
                match('&');
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "AMPERSAND"

    // $ANTLR start "LT"
    public final void mLT() throws RecognitionException {
        try {
            int _type = LT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:174:28: ( '<' )
            // TestLang.g:174:32: '<'
            {
                match('<');
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "LT"

    // $ANTLR start "GT"
    public final void mGT() throws RecognitionException {
        try {
            int _type = GT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:175:28: ( '>' )
            // TestLang.g:175:32: '>'
            {
                match('>');
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "GT"

    // $ANTLR start "SEMICOLON"
    public final void mSEMICOLON() throws RecognitionException {
        try {
            int _type = SEMICOLON;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:176:28: ( ';' )
            // TestLang.g:176:32: ';'
            {
                match(';');
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "SEMICOLON"

    // $ANTLR start "COLON"
    public final void mCOLON() throws RecognitionException {
        try {
            int _type = COLON;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:177:28: ( ':' )
            // TestLang.g:177:32: ':'
            {
                match(':');
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "COLON"

    // $ANTLR start "PLUS"
    public final void mPLUS() throws RecognitionException {
        try {
            int _type = PLUS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:178:28: ( '+' )
            // TestLang.g:178:32: '+'
            {
                match('+');
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "PLUS"

    // $ANTLR start "MINUS"
    public final void mMINUS() throws RecognitionException {
        try {
            int _type = MINUS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:179:28: ( '-' )
            // TestLang.g:179:32: '-'
            {
                match('-');
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "MINUS"

    // $ANTLR start "LEFT_BRACKET"
    public final void mLEFT_BRACKET() throws RecognitionException {
        try {
            int _type = LEFT_BRACKET;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:180:28: ( '[' )
            // TestLang.g:180:32: '['
            {
                match('[');
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "LEFT_BRACKET"

    // $ANTLR start "RIGHT_BRACKET"
    public final void mRIGHT_BRACKET() throws RecognitionException {
        try {
            int _type = RIGHT_BRACKET;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:181:28: ( ']' )
            // TestLang.g:181:32: ']'
            {
                match(']');
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "RIGHT_BRACKET"

    // $ANTLR start "LEFT_BRACE"
    public final void mLEFT_BRACE() throws RecognitionException {
        try {
            int _type = LEFT_BRACE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:182:28: ( '{' )
            // TestLang.g:182:32: '{'
            {
                match('{');
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "LEFT_BRACE"

    // $ANTLR start "RIGHT_BRACE"
    public final void mRIGHT_BRACE() throws RecognitionException {
        try {
            int _type = RIGHT_BRACE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:183:28: ( '}' )
            // TestLang.g:183:32: '}'
            {
                match('}');
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "RIGHT_BRACE"

    // $ANTLR start "COMMA"
    public final void mCOMMA() throws RecognitionException {
        try {
            int _type = COMMA;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:184:28: ( ',' )
            // TestLang.g:184:32: ','
            {
                match(',');
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "COMMA"

    // $ANTLR start "DOT"
    public final void mDOT() throws RecognitionException {
        try {
            int _type = DOT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:185:28: ( '.' )
            // TestLang.g:185:32: '.'
            {
                match('.');
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "DOT"

    // $ANTLR start "SLASH"
    public final void mSLASH() throws RecognitionException {
        try {
            int _type = SLASH;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:186:28: ( '/' )
            // TestLang.g:186:32: '/'
            {
                match('/');
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "SLASH"

    // $ANTLR start "QUESTION_MARK"
    public final void mQUESTION_MARK() throws RecognitionException {
        try {
            int _type = QUESTION_MARK;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:187:28: ( '?' )
            // TestLang.g:187:32: '?'
            {
                match('?');
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "QUESTION_MARK"

    // $ANTLR start "QUOTE"
    public final void mQUOTE() throws RecognitionException {
        try {
            // TestLang.g:188:28: ( '\\'' )
            // TestLang.g:188:32: '\\''
            {
                match('\'');
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = SQL_XQUERY;
                }

            }

        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "QUOTE"

    // $ANTLR start "DOUBLE_QUOTE"
    public final void mDOUBLE_QUOTE() throws RecognitionException {
        try {
            // TestLang.g:189:28: ( '\"' )
            // TestLang.g:189:32: '\"'
            {
                match('\"');
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = SQL_XQUERY;
                }

            }

        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "DOUBLE_QUOTE"

    // $ANTLR start "EXCLAMATION"
    public final void mEXCLAMATION() throws RecognitionException {
        try {
            int _type = EXCLAMATION;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:192:28: ( '!' )
            // TestLang.g:192:32: '!'
            {
                match('!');
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "EXCLAMATION"

    // $ANTLR start "APPROX"
    public final void mAPPROX() throws RecognitionException {
        try {
            int _type = APPROX;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:193:28: ( '~=' )
            // TestLang.g:193:32: '~='
            {
                match("~=");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = LDAP;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "APPROX"

    // $ANTLR start "LT_GT"
    public final void mLT_GT() throws RecognitionException {
        try {
            int _type = LT_GT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:196:28: ( '<>' )
            // TestLang.g:196:32: '<>'
            {
                match("<>");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = SQL;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "LT_GT"

    // $ANTLR start "PERCENT"
    public final void mPERCENT() throws RecognitionException {
        try {
            int _type = PERCENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:197:28: ( '\\%' )
            // TestLang.g:197:32: '\\%'
            {
                match('%');
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = SQL;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "PERCENT"

    // $ANTLR start "CIRCUMFLEX"
    public final void mCIRCUMFLEX() throws RecognitionException {
        try {
            int _type = CIRCUMFLEX;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:198:28: ( '^' )
            // TestLang.g:198:32: '^'
            {
                match('^');
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = SQL;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "CIRCUMFLEX"

    // $ANTLR start "UNDERSCORE"
    public final void mUNDERSCORE() throws RecognitionException {
        try {
            // TestLang.g:199:28: ( '_' )
            // TestLang.g:199:32: '_'
            {
                match('_');
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = SQL;
                }

            }

        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "UNDERSCORE"

    // $ANTLR start "POUND_SIGN"
    public final void mPOUND_SIGN() throws RecognitionException {
        try {
            int _type = POUND_SIGN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:202:28: ( '#' )
            // TestLang.g:202:32: '#'
            {
                match('#');
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "POUND_SIGN"

    // $ANTLR start "DOUBLE_SLASH"
    public final void mDOUBLE_SLASH() throws RecognitionException {
        try {
            int _type = DOUBLE_SLASH;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:203:28: ( '//' )
            // TestLang.g:203:32: '//'
            {
                match("//");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "DOUBLE_SLASH"

    // $ANTLR start "DOUBLE_DOT"
    public final void mDOUBLE_DOT() throws RecognitionException {
        try {
            int _type = DOUBLE_DOT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:204:28: ( '..' )
            // TestLang.g:204:32: '..'
            {
                match("..");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "DOUBLE_DOT"

    // $ANTLR start "DOUBLE_LT"
    public final void mDOUBLE_LT() throws RecognitionException {
        try {
            int _type = DOUBLE_LT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:205:28: ( '<<' )
            // TestLang.g:205:32: '<<'
            {
                match("<<");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "DOUBLE_LT"

    // $ANTLR start "DOUBLE_GT"
    public final void mDOUBLE_GT() throws RecognitionException {
        try {
            int _type = DOUBLE_GT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:206:28: ( '>>' )
            // TestLang.g:206:32: '>>'
            {
                match(">>");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "DOUBLE_GT"

    // $ANTLR start "EXCLAMATION_EQ"
    public final void mEXCLAMATION_EQ() throws RecognitionException {
        try {
            int _type = EXCLAMATION_EQ;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:207:28: ( '!=' )
            // TestLang.g:207:32: '!='
            {
                match("!=");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "EXCLAMATION_EQ"

    // $ANTLR start "COLON_EQ"
    public final void mCOLON_EQ() throws RecognitionException {
        try {
            int _type = COLON_EQ;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:208:28: ( ':=' )
            // TestLang.g:208:32: ':='
            {
                match(":=");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "COLON_EQ"

    // $ANTLR start "LT_EXCLAMATION"
    public final void mLT_EXCLAMATION() throws RecognitionException {
        try {
            int _type = LT_EXCLAMATION;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:209:28: ( '<!' )
            // TestLang.g:209:32: '<!'
            {
                match("<!");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "LT_EXCLAMATION"

    // $ANTLR start "LEFT_PAREN_POUND_SIGN"
    public final void mLEFT_PAREN_POUND_SIGN() throws RecognitionException {
        try {
            int _type = LEFT_PAREN_POUND_SIGN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:210:28: ( '(#' )
            // TestLang.g:210:32: '(#'
            {
                match("(#");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "LEFT_PAREN_POUND_SIGN"

    // $ANTLR start "RIGHT_PAREN_POUND_SIGN"
    public final void mRIGHT_PAREN_POUND_SIGN() throws RecognitionException {
        try {
            int _type = RIGHT_PAREN_POUND_SIGN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:211:28: ( '#)' )
            // TestLang.g:211:32: '#)'
            {
                match("#)");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "RIGHT_PAREN_POUND_SIGN"

    // $ANTLR start "AT"
    public final void mAT() throws RecognitionException {
        try {
            int _type = AT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:212:28: ( '@' )
            // TestLang.g:212:32: '@'
            {
                match('@');
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "AT"

    // $ANTLR start "DOLLAR"
    public final void mDOLLAR() throws RecognitionException {
        try {
            int _type = DOLLAR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:213:28: ( '$' )
            // TestLang.g:213:32: '$'
            {
                match('$');
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "DOLLAR"

    // $ANTLR start "STRING_LITERAL"
    public final void mSTRING_LITERAL() throws RecognitionException {
        try {
            int _type = STRING_LITERAL;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:235:5: ( QUOTE ( '\\\\' QUOTE | QUOTE QUOTE | CHAR_NO_QUOTE )* QUOTE | DOUBLE_QUOTE ( DOUBLE_QUOTE DOUBLE_QUOTE | CHAR_NO_DOUBLE_QUOTE )* DOUBLE_QUOTE )
            int alt3 = 2;
            int LA3_0 = input.LA(1);

            if ((LA3_0 == '\'')) {
                alt3 = 1;
            } else if ((LA3_0 == '\"')) {
                alt3 = 2;
            } else {
                if (state.backtracking > 0) {
                    state.failed = true;
                    return;
                }
                NoViableAltException nvae = new NoViableAltException("", 3, 0, input);

                throw nvae;

            }
            switch (alt3) {
            case 1:
            // TestLang.g:235:7: QUOTE ( '\\\\' QUOTE | QUOTE QUOTE | CHAR_NO_QUOTE )* QUOTE
            {
                mQUOTE();
                if (state.failed)
                    return;

                // TestLang.g:235:13: ( '\\\\' QUOTE | QUOTE QUOTE | CHAR_NO_QUOTE )*
                loop1: do {
                    int alt1 = 4;
                    alt1 = dfa1.predict(input);
                    switch (alt1) {
                    case 1:
                    // TestLang.g:235:15: '\\\\' QUOTE
                    {
                        match('\\');
                        if (state.failed)
                            return;

                        mQUOTE();
                        if (state.failed)
                            return;

                    }
                        break;
                    case 2:
                    // TestLang.g:235:28: QUOTE QUOTE
                    {
                        mQUOTE();
                        if (state.failed)
                            return;

                        mQUOTE();
                        if (state.failed)
                            return;

                    }
                        break;
                    case 3:
                    // TestLang.g:235:43: CHAR_NO_QUOTE
                    {
                        mCHAR_NO_QUOTE();
                        if (state.failed)
                            return;

                    }
                        break;

                    default:
                        break loop1;
                    }
                } while (true);

                mQUOTE();
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = SQL_XQUERY;
                }

            }
                break;
            case 2:
            // TestLang.g:237:4: DOUBLE_QUOTE ( DOUBLE_QUOTE DOUBLE_QUOTE | CHAR_NO_DOUBLE_QUOTE )* DOUBLE_QUOTE
            {
                mDOUBLE_QUOTE();
                if (state.failed)
                    return;

                // TestLang.g:237:17: ( DOUBLE_QUOTE DOUBLE_QUOTE | CHAR_NO_DOUBLE_QUOTE )*
                loop2: do {
                    int alt2 = 3;
                    int LA2_0 = input.LA(1);

                    if ((LA2_0 == '\"')) {
                        int LA2_1 = input.LA(2);

                        if ((LA2_1 == '\"')) {
                            alt2 = 1;
                        }

                    } else if (((LA2_0 >= '\t' && LA2_0 <= '\n') || LA2_0 == '\r' || (LA2_0 >= ' ' && LA2_0 <= '!')
                            || (LA2_0 >= '#' && LA2_0 <= '\uD7FF') || (LA2_0 >= '\uE000' && LA2_0 <= '\uFFFD'))) {
                        alt2 = 2;
                    }

                    switch (alt2) {
                    case 1:
                    // TestLang.g:237:18: DOUBLE_QUOTE DOUBLE_QUOTE
                    {
                        mDOUBLE_QUOTE();
                        if (state.failed)
                            return;

                        mDOUBLE_QUOTE();
                        if (state.failed)
                            return;

                    }
                        break;
                    case 2:
                    // TestLang.g:237:46: CHAR_NO_DOUBLE_QUOTE
                    {
                        mCHAR_NO_DOUBLE_QUOTE();
                        if (state.failed)
                            return;

                    }
                        break;

                    default:
                        break loop2;
                    }
                } while (true);

                mDOUBLE_QUOTE();
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    targets = XQUERY;
                }

            }
                break;

            }
            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "STRING_LITERAL"

    // $ANTLR start "CHAR_NO_QUOTE"
    public final void mCHAR_NO_QUOTE() throws RecognitionException {
        try {
            // TestLang.g:242:2: ( ( '\\U0009' | '\\u000A' | '\\u000D' | '\\u0020' .. '\\u0026' | '\\u0028' .. '\\uD7FF' | '\\uE000' .. '\\uFFFD' ) )
            // TestLang.g:
            {
                if ((input.LA(1) >= '\t' && input.LA(1) <= '\n') || input.LA(1) == '\r'
                        || (input.LA(1) >= ' ' && input.LA(1) <= '&') || (input.LA(1) >= '(' && input.LA(1) <= '\uD7FF')
                        || (input.LA(1) >= '\uE000' && input.LA(1) <= '\uFFFD')) {
                    input.consume();
                    state.failed = false;
                } else {
                    if (state.backtracking > 0) {
                        state.failed = true;
                        return;
                    }
                    MismatchedSetException mse = new MismatchedSetException(null, input);
                    recover(mse);
                    throw mse;
                }

            }

        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "CHAR_NO_QUOTE"

    // $ANTLR start "CHAR_NO_DOUBLE_QUOTE"
    public final void mCHAR_NO_DOUBLE_QUOTE() throws RecognitionException {
        try {
            // TestLang.g:246:2: ( ( '\\U0009' | '\\u000A' | '\\u000D' | '\\u0020' | '\\u0021' | '\\u0023' .. '\\uD7FF' | '\\uE000' .. '\\uFFFD' ) )
            // TestLang.g:
            {
                if ((input.LA(1) >= '\t' && input.LA(1) <= '\n') || input.LA(1) == '\r'
                        || (input.LA(1) >= ' ' && input.LA(1) <= '!') || (input.LA(1) >= '#' && input.LA(1) <= '\uD7FF')
                        || (input.LA(1) >= '\uE000' && input.LA(1) <= '\uFFFD')) {
                    input.consume();
                    state.failed = false;
                } else {
                    if (state.backtracking > 0) {
                        state.failed = true;
                        return;
                    }
                    MismatchedSetException mse = new MismatchedSetException(null, input);
                    recover(mse);
                    throw mse;
                }

            }

        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "CHAR_NO_DOUBLE_QUOTE"

    // $ANTLR start "DECIMAL_LITERAL"
    public final void mDECIMAL_LITERAL() throws RecognitionException {
        try {
            int _type = DECIMAL_LITERAL;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:255:5: ( ( ( ( DOT ( DIGIT )+ ) | ( DIGIT )+ ( DOT ( DIGIT )* )? ) ( ( 'f' | 'F' ) | ( 'd' | 'D' ) | ( 'e' | 'E' ) ( PLUS | MINUS )? ( DIGIT )+ )? ) )
            // TestLang.g:255:9: ( ( ( DOT ( DIGIT )+ ) | ( DIGIT )+ ( DOT ( DIGIT )* )? ) ( ( 'f' | 'F' ) | ( 'd' | 'D' ) | ( 'e' | 'E' ) ( PLUS | MINUS )? ( DIGIT )+ )? )
            {
                // TestLang.g:255:9: ( ( ( DOT ( DIGIT )+ ) | ( DIGIT )+ ( DOT ( DIGIT )* )? ) ( ( 'f' | 'F' ) | ( 'd' | 'D' ) | ( 'e' | 'E' ) ( PLUS | MINUS )? ( DIGIT )+ )? )
                // TestLang.g:255:10: ( ( DOT ( DIGIT )+ ) | ( DIGIT )+ ( DOT ( DIGIT )* )? ) ( ( 'f' | 'F' ) | ( 'd' | 'D' ) | ( 'e' | 'E' ) ( PLUS | MINUS )? ( DIGIT )+ )?
                {
                    // TestLang.g:255:10: ( ( DOT ( DIGIT )+ ) | ( DIGIT )+ ( DOT ( DIGIT )* )? )
                    int alt8 = 2;
                    int LA8_0 = input.LA(1);

                    if ((LA8_0 == '.')) {
                        alt8 = 1;
                    } else if (((LA8_0 >= '0' && LA8_0 <= '9'))) {
                        alt8 = 2;
                    } else {
                        if (state.backtracking > 0) {
                            state.failed = true;
                            return;
                        }
                        NoViableAltException nvae = new NoViableAltException("", 8, 0, input);

                        throw nvae;

                    }
                    switch (alt8) {
                    case 1:
                    // TestLang.g:255:12: ( DOT ( DIGIT )+ )
                    {
                        // TestLang.g:255:12: ( DOT ( DIGIT )+ )
                        // TestLang.g:255:13: DOT ( DIGIT )+
                        {
                            mDOT();
                            if (state.failed)
                                return;

                            // TestLang.g:255:17: ( DIGIT )+
                            int cnt4 = 0;
                            loop4: do {
                                int alt4 = 2;
                                int LA4_0 = input.LA(1);

                                if (((LA4_0 >= '0' && LA4_0 <= '9'))) {
                                    alt4 = 1;
                                }

                                switch (alt4) {
                                case 1:
                                // TestLang.g:
                                {
                                    if ((input.LA(1) >= '0' && input.LA(1) <= '9')) {
                                        input.consume();
                                        state.failed = false;
                                    } else {
                                        if (state.backtracking > 0) {
                                            state.failed = true;
                                            return;
                                        }
                                        MismatchedSetException mse = new MismatchedSetException(null, input);
                                        recover(mse);
                                        throw mse;
                                    }

                                }
                                    break;

                                default:
                                    if (cnt4 >= 1)
                                        break loop4;
                                    if (state.backtracking > 0) {
                                        state.failed = true;
                                        return;
                                    }
                                    EarlyExitException eee = new EarlyExitException(4, input);
                                    throw eee;
                                }
                                cnt4++;
                            } while (true);

                        }

                    }
                        break;
                    case 2:
                    // TestLang.g:256:11: ( DIGIT )+ ( DOT ( DIGIT )* )?
                    {
                        // TestLang.g:256:11: ( DIGIT )+
                        int cnt5 = 0;
                        loop5: do {
                            int alt5 = 2;
                            int LA5_0 = input.LA(1);

                            if (((LA5_0 >= '0' && LA5_0 <= '9'))) {
                                alt5 = 1;
                            }

                            switch (alt5) {
                            case 1:
                            // TestLang.g:
                            {
                                if ((input.LA(1) >= '0' && input.LA(1) <= '9')) {
                                    input.consume();
                                    state.failed = false;
                                } else {
                                    if (state.backtracking > 0) {
                                        state.failed = true;
                                        return;
                                    }
                                    MismatchedSetException mse = new MismatchedSetException(null, input);
                                    recover(mse);
                                    throw mse;
                                }

                            }
                                break;

                            default:
                                if (cnt5 >= 1)
                                    break loop5;
                                if (state.backtracking > 0) {
                                    state.failed = true;
                                    return;
                                }
                                EarlyExitException eee = new EarlyExitException(5, input);
                                throw eee;
                            }
                            cnt5++;
                        } while (true);

                        // TestLang.g:256:22: ( DOT ( DIGIT )* )?
                        int alt7 = 2;
                        int LA7_0 = input.LA(1);

                        if ((LA7_0 == '.')) {
                            alt7 = 1;
                        }
                        switch (alt7) {
                        case 1:
                        // TestLang.g:256:24: DOT ( DIGIT )*
                        {
                            mDOT();
                            if (state.failed)
                                return;

                            // TestLang.g:256:28: ( DIGIT )*
                            loop6: do {
                                int alt6 = 2;
                                int LA6_0 = input.LA(1);

                                if (((LA6_0 >= '0' && LA6_0 <= '9'))) {
                                    alt6 = 1;
                                }

                                switch (alt6) {
                                case 1:
                                // TestLang.g:
                                {
                                    if ((input.LA(1) >= '0' && input.LA(1) <= '9')) {
                                        input.consume();
                                        state.failed = false;
                                    } else {
                                        if (state.backtracking > 0) {
                                            state.failed = true;
                                            return;
                                        }
                                        MismatchedSetException mse = new MismatchedSetException(null, input);
                                        recover(mse);
                                        throw mse;
                                    }

                                }
                                    break;

                                default:
                                    break loop6;
                                }
                            } while (true);

                        }
                            break;

                        }

                    }
                        break;

                    }

                    // TestLang.g:257:9: ( ( 'f' | 'F' ) | ( 'd' | 'D' ) | ( 'e' | 'E' ) ( PLUS | MINUS )? ( DIGIT )+ )?
                    int alt11 = 4;
                    switch (input.LA(1)) {
                    case 'F':
                    case 'f': {
                        alt11 = 1;
                    }
                        break;
                    case 'D':
                    case 'd': {
                        alt11 = 2;
                    }
                        break;
                    case 'E':
                    case 'e': {
                        alt11 = 3;
                    }
                        break;
                    }

                    switch (alt11) {
                    case 1:
                    // TestLang.g:257:11: ( 'f' | 'F' )
                    {
                        if (input.LA(1) == 'F' || input.LA(1) == 'f') {
                            input.consume();
                            state.failed = false;
                        } else {
                            if (state.backtracking > 0) {
                                state.failed = true;
                                return;
                            }
                            MismatchedSetException mse = new MismatchedSetException(null, input);
                            recover(mse);
                            throw mse;
                        }

                    }
                        break;
                    case 2:
                    // TestLang.g:258:11: ( 'd' | 'D' )
                    {
                        if (input.LA(1) == 'D' || input.LA(1) == 'd') {
                            input.consume();
                            state.failed = false;
                        } else {
                            if (state.backtracking > 0) {
                                state.failed = true;
                                return;
                            }
                            MismatchedSetException mse = new MismatchedSetException(null, input);
                            recover(mse);
                            throw mse;
                        }

                    }
                        break;
                    case 3:
                    // TestLang.g:259:11: ( 'e' | 'E' ) ( PLUS | MINUS )? ( DIGIT )+
                    {
                        if (input.LA(1) == 'E' || input.LA(1) == 'e') {
                            input.consume();
                            state.failed = false;
                        } else {
                            if (state.backtracking > 0) {
                                state.failed = true;
                                return;
                            }
                            MismatchedSetException mse = new MismatchedSetException(null, input);
                            recover(mse);
                            throw mse;
                        }

                        // TestLang.g:259:23: ( PLUS | MINUS )?
                        int alt9 = 3;
                        int LA9_0 = input.LA(1);

                        if ((LA9_0 == '+')) {
                            alt9 = 1;
                        } else if ((LA9_0 == '-')) {
                            alt9 = 2;
                        }
                        switch (alt9) {
                        case 1:
                        // TestLang.g:259:25: PLUS
                        {
                            mPLUS();
                            if (state.failed)
                                return;

                        }
                            break;
                        case 2:
                        // TestLang.g:259:32: MINUS
                        {
                            mMINUS();
                            if (state.failed)
                                return;

                        }
                            break;

                        }

                        // TestLang.g:259:41: ( DIGIT )+
                        int cnt10 = 0;
                        loop10: do {
                            int alt10 = 2;
                            int LA10_0 = input.LA(1);

                            if (((LA10_0 >= '0' && LA10_0 <= '9'))) {
                                alt10 = 1;
                            }

                            switch (alt10) {
                            case 1:
                            // TestLang.g:
                            {
                                if ((input.LA(1) >= '0' && input.LA(1) <= '9')) {
                                    input.consume();
                                    state.failed = false;
                                } else {
                                    if (state.backtracking > 0) {
                                        state.failed = true;
                                        return;
                                    }
                                    MismatchedSetException mse = new MismatchedSetException(null, input);
                                    recover(mse);
                                    throw mse;
                                }

                            }
                                break;

                            default:
                                if (cnt10 >= 1)
                                    break loop10;
                                if (state.backtracking > 0) {
                                    state.failed = true;
                                    return;
                                }
                                EarlyExitException eee = new EarlyExitException(10, input);
                                throw eee;
                            }
                            cnt10++;
                        } while (true);

                    }
                        break;

                    }

                }

                if (state.backtracking == 0) {
                    targets = SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "DECIMAL_LITERAL"

    // $ANTLR start "DIGIT"
    public final void mDIGIT() throws RecognitionException {
        try {
            // TestLang.g:263:22: ( '0' .. '9' )
            // TestLang.g:
            {
                if ((input.LA(1) >= '0' && input.LA(1) <= '9')) {
                    input.consume();
                    state.failed = false;
                } else {
                    if (state.backtracking > 0) {
                        state.failed = true;
                        return;
                    }
                    MismatchedSetException mse = new MismatchedSetException(null, input);
                    recover(mse);
                    throw mse;
                }

            }

        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "DIGIT"

    // $ANTLR start "OCTAL_OR_HEX_LITERAL"
    public final void mOCTAL_OR_HEX_LITERAL() throws RecognitionException {
        try {
            int _type = OCTAL_OR_HEX_LITERAL;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:267:5: ( ( '0' ( 'X' ( HEX_DIGIT )+ | ( OCTAL_DIGIT )+ |{...}? ( '0' )* ) ) )
            // TestLang.g:267:9: ( '0' ( 'X' ( HEX_DIGIT )+ | ( OCTAL_DIGIT )+ |{...}? ( '0' )* ) )
            {
                // TestLang.g:267:9: ( '0' ( 'X' ( HEX_DIGIT )+ | ( OCTAL_DIGIT )+ |{...}? ( '0' )* ) )
                // TestLang.g:267:11: '0' ( 'X' ( HEX_DIGIT )+ | ( OCTAL_DIGIT )+ |{...}? ( '0' )* )
                {
                    match('0');
                    if (state.failed)
                        return;

                    // TestLang.g:268:13: ( 'X' ( HEX_DIGIT )+ | ( OCTAL_DIGIT )+ |{...}? ( '0' )* )
                    int alt15 = 3;
                    switch (input.LA(1)) {
                    case 'X': {
                        alt15 = 1;
                    }
                        break;
                    case '0': {
                        int LA15_2 = input.LA(2);

                        if ((!(((true))))) {
                            alt15 = 2;
                        } else if (((true))) {
                            alt15 = 3;
                        } else {
                            if (state.backtracking > 0) {
                                state.failed = true;
                                return;
                            }
                            NoViableAltException nvae = new NoViableAltException("", 15, 2, input);

                            throw nvae;

                        }
                    }
                        break;
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7': {
                        alt15 = 2;
                    }
                        break;
                    default:
                        alt15 = 3;
                    }

                    switch (alt15) {
                    case 1:
                    // TestLang.g:268:15: 'X' ( HEX_DIGIT )+
                    {
                        match('X');
                        if (state.failed)
                            return;

                        // TestLang.g:268:19: ( HEX_DIGIT )+
                        int cnt12 = 0;
                        loop12: do {
                            int alt12 = 2;
                            int LA12_0 = input.LA(1);

                            if (((LA12_0 >= '0' && LA12_0 <= '9') || (LA12_0 >= 'A' && LA12_0 <= 'F'))) {
                                alt12 = 1;
                            }

                            switch (alt12) {
                            case 1:
                            // TestLang.g:
                            {
                                if ((input.LA(1) >= '0' && input.LA(1) <= '9')
                                        || (input.LA(1) >= 'A' && input.LA(1) <= 'F')) {
                                    input.consume();
                                    state.failed = false;
                                } else {
                                    if (state.backtracking > 0) {
                                        state.failed = true;
                                        return;
                                    }
                                    MismatchedSetException mse = new MismatchedSetException(null, input);
                                    recover(mse);
                                    throw mse;
                                }

                            }
                                break;

                            default:
                                if (cnt12 >= 1)
                                    break loop12;
                                if (state.backtracking > 0) {
                                    state.failed = true;
                                    return;
                                }
                                EarlyExitException eee = new EarlyExitException(12, input);
                                throw eee;
                            }
                            cnt12++;
                        } while (true);

                    }
                        break;
                    case 2:
                    // TestLang.g:269:15: ( OCTAL_DIGIT )+
                    {
                        // TestLang.g:269:15: ( OCTAL_DIGIT )+
                        int cnt13 = 0;
                        loop13: do {
                            int alt13 = 2;
                            int LA13_0 = input.LA(1);

                            if (((LA13_0 >= '0' && LA13_0 <= '7'))) {
                                alt13 = 1;
                            }

                            switch (alt13) {
                            case 1:
                            // TestLang.g:
                            {
                                if ((input.LA(1) >= '0' && input.LA(1) <= '7')) {
                                    input.consume();
                                    state.failed = false;
                                } else {
                                    if (state.backtracking > 0) {
                                        state.failed = true;
                                        return;
                                    }
                                    MismatchedSetException mse = new MismatchedSetException(null, input);
                                    recover(mse);
                                    throw mse;
                                }

                            }
                                break;

                            default:
                                if (cnt13 >= 1)
                                    break loop13;
                                if (state.backtracking > 0) {
                                    state.failed = true;
                                    return;
                                }
                                EarlyExitException eee = new EarlyExitException(13, input);
                                throw eee;
                            }
                            cnt13++;
                        } while (true);

                    }
                        break;
                    case 3:
                    // TestLang.g:270:15: {...}? ( '0' )*
                    {
                        if (!((true))) {
                            if (state.backtracking > 0) {
                                state.failed = true;
                                return;
                            }
                            throw new FailedPredicateException(input, "OCTAL_OR_HEX_LITERAL", "true");
                        }

                        // TestLang.g:271:17: ( '0' )*
                        loop14: do {
                            int alt14 = 2;
                            int LA14_0 = input.LA(1);

                            if ((LA14_0 == '0')) {
                                alt14 = 1;
                            }

                            switch (alt14) {
                            case 1:
                            // TestLang.g:271:19: '0'
                            {
                                match('0');
                                if (state.failed)
                                    return;

                            }
                                break;

                            default:
                                break loop14;
                            }
                        } while (true);

                    }
                        break;

                    }

                }

                if (state.backtracking == 0) {
                    targets = SQL;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "OCTAL_OR_HEX_LITERAL"

    // $ANTLR start "OCTAL_DIGIT"
    public final void mOCTAL_DIGIT() throws RecognitionException {
        try {
            // TestLang.g:277:23: ( '0' .. '7' )
            // TestLang.g:
            {
                if ((input.LA(1) >= '0' && input.LA(1) <= '7')) {
                    input.consume();
                    state.failed = false;
                } else {
                    if (state.backtracking > 0) {
                        state.failed = true;
                        return;
                    }
                    MismatchedSetException mse = new MismatchedSetException(null, input);
                    recover(mse);
                    throw mse;
                }

            }

        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "OCTAL_DIGIT"

    // $ANTLR start "HEX_DIGIT"
    public final void mHEX_DIGIT() throws RecognitionException {
        try {
            // TestLang.g:278:23: ( ( DIGIT | 'A' .. 'F' ) )
            // TestLang.g:
            {
                if ((input.LA(1) >= '0' && input.LA(1) <= '9') || (input.LA(1) >= 'A' && input.LA(1) <= 'F')) {
                    input.consume();
                    state.failed = false;
                } else {
                    if (state.backtracking > 0) {
                        state.failed = true;
                        return;
                    }
                    MismatchedSetException mse = new MismatchedSetException(null, input);
                    recover(mse);
                    throw mse;
                }

            }

        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "HEX_DIGIT"

    // $ANTLR start "IDENTIFIER"
    public final void mIDENTIFIER() throws RecognitionException {
        try {
            int _type = IDENTIFIER;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:290:2: ( IDENTIFIER_START ( IDENTIFIER_CONTINUE )* )
            // TestLang.g:290:5: IDENTIFIER_START ( IDENTIFIER_CONTINUE )*
            {
                mIDENTIFIER_START();
                if (state.failed)
                    return;

                // TestLang.g:290:22: ( IDENTIFIER_CONTINUE )*
                loop16: do {
                    int alt16 = 2;
                    int LA16_0 = input.LA(1);

                    if ((LA16_0 == '.' || (LA16_0 >= '0' && LA16_0 <= '9') || (LA16_0 >= 'A' && LA16_0 <= 'Z')
                            || LA16_0 == '_' || (LA16_0 >= 'a' && LA16_0 <= 'z') || LA16_0 == '\u00B7'
                            || (LA16_0 >= '\u00C0' && LA16_0 <= '\u00D6') || (LA16_0 >= '\u00D8' && LA16_0 <= '\u00F6')
                            || (LA16_0 >= '\u00F8' && LA16_0 <= '\u037D') || (LA16_0 >= '\u037F' && LA16_0 <= '\u1FFF')
                            || (LA16_0 >= '\u200C' && LA16_0 <= '\u200D') || (LA16_0 >= '\u203F' && LA16_0 <= '\u2040')
                            || (LA16_0 >= '\u2070' && LA16_0 <= '\u218F') || (LA16_0 >= '\u2C00' && LA16_0 <= '\u2FEF')
                            || (LA16_0 >= '\u3001' && LA16_0 <= '\uD7FF') || (LA16_0 >= '\uF900' && LA16_0 <= '\uFDCF')
                            || (LA16_0 >= '\uFDF0' && LA16_0 <= '\uFFFD'))) {
                        alt16 = 1;
                    }

                    switch (alt16) {
                    case 1:
                    // TestLang.g:290:22: IDENTIFIER_CONTINUE
                    {
                        mIDENTIFIER_CONTINUE();
                        if (state.failed)
                            return;

                    }
                        break;

                    default:
                        break loop16;
                    }
                } while (true);

                if (state.backtracking == 0) {
                    targets = LDAP_SQL_XQUERY;
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "IDENTIFIER"

    // $ANTLR start "IDENTIFIER_START"
    public final void mIDENTIFIER_START() throws RecognitionException {
        try {
            // TestLang.g:295:2: ( LETTER | UNDERSCORE | ( '\\u00C0' .. '\\u00D6' ) | ( '\\u00D8' .. '\\u00F6' ) | ( '\\u00F8' .. '\\u02FF' ) | ( '\\u0370' .. '\\u037D' ) | ( '\\u037F' .. '\\u1FFF' ) | ( '\\u200C' .. '\\u200D' ) | ( '\\u2070' .. '\\u218F' ) | ( '\\u2C00' .. '\\u2FEF' ) | ( '\\u3001' .. '\\uD7FF' ) | ( '\\uF900' .. '\\uFDCF' ) | ( '\\uFDF0' .. '\\uFFFD' ) )
            int alt17 = 13;
            int LA17_0 = input.LA(1);

            if (((LA17_0 >= 'A' && LA17_0 <= 'Z') || (LA17_0 >= 'a' && LA17_0 <= 'z'))) {
                alt17 = 1;
            } else if ((LA17_0 == '_')) {
                alt17 = 2;
            } else if (((LA17_0 >= '\u00C0' && LA17_0 <= '\u00D6'))) {
                alt17 = 3;
            } else if (((LA17_0 >= '\u00D8' && LA17_0 <= '\u00F6'))) {
                alt17 = 4;
            } else if (((LA17_0 >= '\u00F8' && LA17_0 <= '\u02FF'))) {
                alt17 = 5;
            } else if (((LA17_0 >= '\u0370' && LA17_0 <= '\u037D'))) {
                alt17 = 6;
            } else if (((LA17_0 >= '\u037F' && LA17_0 <= '\u1FFF'))) {
                alt17 = 7;
            } else if (((LA17_0 >= '\u200C' && LA17_0 <= '\u200D'))) {
                alt17 = 8;
            } else if (((LA17_0 >= '\u2070' && LA17_0 <= '\u218F'))) {
                alt17 = 9;
            } else if (((LA17_0 >= '\u2C00' && LA17_0 <= '\u2FEF'))) {
                alt17 = 10;
            } else if (((LA17_0 >= '\u3001' && LA17_0 <= '\uD7FF'))) {
                alt17 = 11;
            } else if (((LA17_0 >= '\uF900' && LA17_0 <= '\uFDCF'))) {
                alt17 = 12;
            } else if (((LA17_0 >= '\uFDF0' && LA17_0 <= '\uFFFD'))) {
                alt17 = 13;
            } else {
                if (state.backtracking > 0) {
                    state.failed = true;
                    return;
                }
                NoViableAltException nvae = new NoViableAltException("", 17, 0, input);

                throw nvae;

            }
            switch (alt17) {
            case 1:
            // TestLang.g:295:4: LETTER
            {
                mLETTER();
                if (state.failed)
                    return;

            }
                break;
            case 2:
            // TestLang.g:295:13: UNDERSCORE
            {
                mUNDERSCORE();
                if (state.failed)
                    return;

            }
                break;
            case 3:
            // TestLang.g:295:26: ( '\\u00C0' .. '\\u00D6' )
            {
                if ((input.LA(1) >= '\u00C0' && input.LA(1) <= '\u00D6')) {
                    input.consume();
                    state.failed = false;
                } else {
                    if (state.backtracking > 0) {
                        state.failed = true;
                        return;
                    }
                    MismatchedSetException mse = new MismatchedSetException(null, input);
                    recover(mse);
                    throw mse;
                }

            }
                break;
            case 4:
            // TestLang.g:295:49: ( '\\u00D8' .. '\\u00F6' )
            {
                if ((input.LA(1) >= '\u00D8' && input.LA(1) <= '\u00F6')) {
                    input.consume();
                    state.failed = false;
                } else {
                    if (state.backtracking > 0) {
                        state.failed = true;
                        return;
                    }
                    MismatchedSetException mse = new MismatchedSetException(null, input);
                    recover(mse);
                    throw mse;
                }

            }
                break;
            case 5:
            // TestLang.g:295:72: ( '\\u00F8' .. '\\u02FF' )
            {
                if ((input.LA(1) >= '\u00F8' && input.LA(1) <= '\u02FF')) {
                    input.consume();
                    state.failed = false;
                } else {
                    if (state.backtracking > 0) {
                        state.failed = true;
                        return;
                    }
                    MismatchedSetException mse = new MismatchedSetException(null, input);
                    recover(mse);
                    throw mse;
                }

            }
                break;
            case 6:
            // TestLang.g:295:95: ( '\\u0370' .. '\\u037D' )
            {
                if ((input.LA(1) >= '\u0370' && input.LA(1) <= '\u037D')) {
                    input.consume();
                    state.failed = false;
                } else {
                    if (state.backtracking > 0) {
                        state.failed = true;
                        return;
                    }
                    MismatchedSetException mse = new MismatchedSetException(null, input);
                    recover(mse);
                    throw mse;
                }

            }
                break;
            case 7:
            // TestLang.g:295:118: ( '\\u037F' .. '\\u1FFF' )
            {
                if ((input.LA(1) >= '\u037F' && input.LA(1) <= '\u1FFF')) {
                    input.consume();
                    state.failed = false;
                } else {
                    if (state.backtracking > 0) {
                        state.failed = true;
                        return;
                    }
                    MismatchedSetException mse = new MismatchedSetException(null, input);
                    recover(mse);
                    throw mse;
                }

            }
                break;
            case 8:
            // TestLang.g:295:141: ( '\\u200C' .. '\\u200D' )
            {
                if ((input.LA(1) >= '\u200C' && input.LA(1) <= '\u200D')) {
                    input.consume();
                    state.failed = false;
                } else {
                    if (state.backtracking > 0) {
                        state.failed = true;
                        return;
                    }
                    MismatchedSetException mse = new MismatchedSetException(null, input);
                    recover(mse);
                    throw mse;
                }

            }
                break;
            case 9:
            // TestLang.g:295:164: ( '\\u2070' .. '\\u218F' )
            {
                if ((input.LA(1) >= '\u2070' && input.LA(1) <= '\u218F')) {
                    input.consume();
                    state.failed = false;
                } else {
                    if (state.backtracking > 0) {
                        state.failed = true;
                        return;
                    }
                    MismatchedSetException mse = new MismatchedSetException(null, input);
                    recover(mse);
                    throw mse;
                }

            }
                break;
            case 10:
            // TestLang.g:295:187: ( '\\u2C00' .. '\\u2FEF' )
            {
                if ((input.LA(1) >= '\u2C00' && input.LA(1) <= '\u2FEF')) {
                    input.consume();
                    state.failed = false;
                } else {
                    if (state.backtracking > 0) {
                        state.failed = true;
                        return;
                    }
                    MismatchedSetException mse = new MismatchedSetException(null, input);
                    recover(mse);
                    throw mse;
                }

            }
                break;
            case 11:
            // TestLang.g:295:210: ( '\\u3001' .. '\\uD7FF' )
            {
                if ((input.LA(1) >= '\u3001' && input.LA(1) <= '\uD7FF')) {
                    input.consume();
                    state.failed = false;
                } else {
                    if (state.backtracking > 0) {
                        state.failed = true;
                        return;
                    }
                    MismatchedSetException mse = new MismatchedSetException(null, input);
                    recover(mse);
                    throw mse;
                }

            }
                break;
            case 12:
            // TestLang.g:295:233: ( '\\uF900' .. '\\uFDCF' )
            {
                if ((input.LA(1) >= '\uF900' && input.LA(1) <= '\uFDCF')) {
                    input.consume();
                    state.failed = false;
                } else {
                    if (state.backtracking > 0) {
                        state.failed = true;
                        return;
                    }
                    MismatchedSetException mse = new MismatchedSetException(null, input);
                    recover(mse);
                    throw mse;
                }

            }
                break;
            case 13:
            // TestLang.g:295:256: ( '\\uFDF0' .. '\\uFFFD' )
            {
                if ((input.LA(1) >= '\uFDF0' && input.LA(1) <= '\uFFFD')) {
                    input.consume();
                    state.failed = false;
                } else {
                    if (state.backtracking > 0) {
                        state.failed = true;
                        return;
                    }
                    MismatchedSetException mse = new MismatchedSetException(null, input);
                    recover(mse);
                    throw mse;
                }

            }
                break;

            }

        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "IDENTIFIER_START"

    // $ANTLR start "IDENTIFIER_CONTINUE"
    public final void mIDENTIFIER_CONTINUE() throws RecognitionException {
        try {
            // TestLang.g:299:2: ( IDENTIFIER_START | DOT | DIGIT | '\\u00B7' | '\\u0300' .. '\\u036F' | '\\u203F' .. '\\u2040' )
            int alt18 = 6;
            int LA18_0 = input.LA(1);

            if (((LA18_0 >= 'A' && LA18_0 <= 'Z') || LA18_0 == '_' || (LA18_0 >= 'a' && LA18_0 <= 'z')
                    || (LA18_0 >= '\u00C0' && LA18_0 <= '\u00D6') || (LA18_0 >= '\u00D8' && LA18_0 <= '\u00F6')
                    || (LA18_0 >= '\u00F8' && LA18_0 <= '\u02FF') || (LA18_0 >= '\u0370' && LA18_0 <= '\u037D')
                    || (LA18_0 >= '\u037F' && LA18_0 <= '\u1FFF') || (LA18_0 >= '\u200C' && LA18_0 <= '\u200D')
                    || (LA18_0 >= '\u2070' && LA18_0 <= '\u218F') || (LA18_0 >= '\u2C00' && LA18_0 <= '\u2FEF')
                    || (LA18_0 >= '\u3001' && LA18_0 <= '\uD7FF') || (LA18_0 >= '\uF900' && LA18_0 <= '\uFDCF')
                    || (LA18_0 >= '\uFDF0' && LA18_0 <= '\uFFFD'))) {
                alt18 = 1;
            } else if ((LA18_0 == '.')) {
                alt18 = 2;
            } else if (((LA18_0 >= '0' && LA18_0 <= '9'))) {
                alt18 = 3;
            } else if ((LA18_0 == '\u00B7')) {
                alt18 = 4;
            } else if (((LA18_0 >= '\u0300' && LA18_0 <= '\u036F'))) {
                alt18 = 5;
            } else if (((LA18_0 >= '\u203F' && LA18_0 <= '\u2040'))) {
                alt18 = 6;
            } else {
                if (state.backtracking > 0) {
                    state.failed = true;
                    return;
                }
                NoViableAltException nvae = new NoViableAltException("", 18, 0, input);

                throw nvae;

            }
            switch (alt18) {
            case 1:
            // TestLang.g:299:4: IDENTIFIER_START
            {
                mIDENTIFIER_START();
                if (state.failed)
                    return;

            }
                break;
            case 2:
            // TestLang.g:299:23: DOT
            {
                mDOT();
                if (state.failed)
                    return;

            }
                break;
            case 3:
            // TestLang.g:299:29: DIGIT
            {
                mDIGIT();
                if (state.failed)
                    return;

            }
                break;
            case 4:
            // TestLang.g:299:37: '\\u00B7'
            {
                match('\u00B7');
                if (state.failed)
                    return;

            }
                break;
            case 5:
            // TestLang.g:299:48: '\\u0300' .. '\\u036F'
            {
                matchRange('\u0300', '\u036F');
                if (state.failed)
                    return;

            }
                break;
            case 6:
            // TestLang.g:299:69: '\\u203F' .. '\\u2040'
            {
                matchRange('\u203F', '\u2040');
                if (state.failed)
                    return;

            }
                break;

            }

        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "IDENTIFIER_CONTINUE"

    // $ANTLR start "LETTER"
    public final void mLETTER() throws RecognitionException {
        try {
            // TestLang.g:303:5: ( 'a' .. 'z' | 'A' .. 'Z' )
            // TestLang.g:
            {
                if ((input.LA(1) >= 'A' && input.LA(1) <= 'Z') || (input.LA(1) >= 'a' && input.LA(1) <= 'z')) {
                    input.consume();
                    state.failed = false;
                } else {
                    if (state.backtracking > 0) {
                        state.failed = true;
                        return;
                    }
                    MismatchedSetException mse = new MismatchedSetException(null, input);
                    recover(mse);
                    throw mse;
                }

            }

        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "LETTER"

    // $ANTLR start "COMMENT_SQL"
    public final void mCOMMENT_SQL() throws RecognitionException {
        try {
            int _type = COMMENT_SQL;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:312:5: ( ( '--' (~ ( '\\n' | '\\r' ) )* ( '\\r' )? ( '\\n' | EOF ) | '/*' ( options {greedy=false; } : . )* '*/' ) )
            // TestLang.g:312:9: ( '--' (~ ( '\\n' | '\\r' ) )* ( '\\r' )? ( '\\n' | EOF ) | '/*' ( options {greedy=false; } : . )* '*/' )
            {
                // TestLang.g:312:9: ( '--' (~ ( '\\n' | '\\r' ) )* ( '\\r' )? ( '\\n' | EOF ) | '/*' ( options {greedy=false; } : . )* '*/' )
                int alt23 = 2;
                int LA23_0 = input.LA(1);

                if ((LA23_0 == '-')) {
                    alt23 = 1;
                } else if ((LA23_0 == '/')) {
                    alt23 = 2;
                } else {
                    if (state.backtracking > 0) {
                        state.failed = true;
                        return;
                    }
                    NoViableAltException nvae = new NoViableAltException("", 23, 0, input);

                    throw nvae;

                }
                switch (alt23) {
                case 1:
                // TestLang.g:312:10: '--' (~ ( '\\n' | '\\r' ) )* ( '\\r' )? ( '\\n' | EOF )
                {
                    match("--");
                    if (state.failed)
                        return;

                    // TestLang.g:312:15: (~ ( '\\n' | '\\r' ) )*
                    loop19: do {
                        int alt19 = 2;
                        int LA19_0 = input.LA(1);

                        if (((LA19_0 >= '\u0000' && LA19_0 <= '\t') || (LA19_0 >= '\u000B' && LA19_0 <= '\f')
                                || (LA19_0 >= '\u000E' && LA19_0 <= '\uFFFF'))) {
                            alt19 = 1;
                        }

                        switch (alt19) {
                        case 1:
                        // TestLang.g:
                        {
                            if ((input.LA(1) >= '\u0000' && input.LA(1) <= '\t')
                                    || (input.LA(1) >= '\u000B' && input.LA(1) <= '\f')
                                    || (input.LA(1) >= '\u000E' && input.LA(1) <= '\uFFFF')) {
                                input.consume();
                                state.failed = false;
                            } else {
                                if (state.backtracking > 0) {
                                    state.failed = true;
                                    return;
                                }
                                MismatchedSetException mse = new MismatchedSetException(null, input);
                                recover(mse);
                                throw mse;
                            }

                        }
                            break;

                        default:
                            break loop19;
                        }
                    } while (true);

                    // TestLang.g:312:29: ( '\\r' )?
                    int alt20 = 2;
                    int LA20_0 = input.LA(1);

                    if ((LA20_0 == '\r')) {
                        alt20 = 1;
                    }
                    switch (alt20) {
                    case 1:
                    // TestLang.g:312:29: '\\r'
                    {
                        match('\r');
                        if (state.failed)
                            return;

                    }
                        break;

                    }

                    // TestLang.g:312:35: ( '\\n' | EOF )
                    int alt21 = 2;
                    int LA21_0 = input.LA(1);

                    if ((LA21_0 == '\n')) {
                        alt21 = 1;
                    } else {
                        alt21 = 2;
                    }
                    switch (alt21) {
                    case 1:
                    // TestLang.g:312:36: '\\n'
                    {
                        match('\n');
                        if (state.failed)
                            return;

                    }
                        break;
                    case 2:
                    // TestLang.g:312:43: EOF
                    {
                        match(EOF);
                        if (state.failed)
                            return;

                    }
                        break;

                    }

                }
                    break;
                case 2:
                // TestLang.g:313:13: '/*' ( options {greedy=false; } : . )* '*/'
                {
                    match("/*");
                    if (state.failed)
                        return;

                    // TestLang.g:313:18: ( options {greedy=false; } : . )*
                    loop22: do {
                        int alt22 = 2;
                        int LA22_0 = input.LA(1);

                        if ((LA22_0 == '*')) {
                            int LA22_1 = input.LA(2);

                            if ((LA22_1 == '/')) {
                                alt22 = 2;
                            } else if (((LA22_1 >= '\u0000' && LA22_1 <= '.')
                                    || (LA22_1 >= '0' && LA22_1 <= '\uFFFF'))) {
                                alt22 = 1;
                            }

                        } else if (((LA22_0 >= '\u0000' && LA22_0 <= ')') || (LA22_0 >= '+' && LA22_0 <= '\uFFFF'))) {
                            alt22 = 1;
                        }

                        switch (alt22) {
                        case 1:
                        // TestLang.g:313:46: .
                        {
                            matchAny();
                            if (state.failed)
                                return;

                        }
                            break;

                        default:
                            break loop22;
                        }
                    } while (true);

                    match("*/");
                    if (state.failed)
                        return;

                }
                    break;

                }

                if (state.backtracking == 0) {
                    //_channel=HIDDEN;
                    skip();
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "COMMENT_SQL"

    // $ANTLR start "COMMENT_XQUERY"
    public final void mCOMMENT_XQUERY() throws RecognitionException {
        try {
            int _type = COMMENT_XQUERY;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:323:9: ( '(:' (~ ( '(' | ':' ) | ( '(' ~ ':' )=> '(' | ( ':' ~ ')' )=> ':' | COMMENT_XQUERY )* ':)' )
            // TestLang.g:323:17: '(:' (~ ( '(' | ':' ) | ( '(' ~ ':' )=> '(' | ( ':' ~ ')' )=> ':' | COMMENT_XQUERY )* ':)'
            {
                match("(:");
                if (state.failed)
                    return;

                // TestLang.g:324:17: (~ ( '(' | ':' ) | ( '(' ~ ':' )=> '(' | ( ':' ~ ')' )=> ':' | COMMENT_XQUERY )*
                loop24: do {
                    int alt24 = 5;
                    int LA24_0 = input.LA(1);

                    if ((LA24_0 == ':')) {
                        int LA24_1 = input.LA(2);

                        if ((LA24_1 == ')')) {
                            int LA24_4 = input.LA(3);

                            if ((LA24_4 == ':') && (synpred2_TestLang())) {
                                alt24 = 3;
                            } else if (((LA24_4 >= '\u0000' && LA24_4 <= '\'') || (LA24_4 >= ')' && LA24_4 <= '9')
                                    || (LA24_4 >= ';' && LA24_4 <= '\uFFFF')) && (synpred2_TestLang())) {
                                alt24 = 3;
                            } else if ((LA24_4 == '(') && (synpred2_TestLang())) {
                                alt24 = 3;
                            }

                        } else if ((LA24_1 == ':') && (synpred2_TestLang())) {
                            alt24 = 3;
                        } else if (((LA24_1 >= '\u0000' && LA24_1 <= '\'') || (LA24_1 >= '*' && LA24_1 <= '9')
                                || (LA24_1 >= ';' && LA24_1 <= '\uFFFF')) && (synpred2_TestLang())) {
                            alt24 = 3;
                        } else if ((LA24_1 == '(') && (synpred2_TestLang())) {
                            alt24 = 3;
                        }

                    } else if (((LA24_0 >= '\u0000' && LA24_0 <= '\'') || (LA24_0 >= ')' && LA24_0 <= '9')
                            || (LA24_0 >= ';' && LA24_0 <= '\uFFFF'))) {
                        alt24 = 1;
                    } else if ((LA24_0 == '(')) {
                        int LA24_3 = input.LA(2);

                        if ((LA24_3 == ':')) {
                            int LA24_8 = input.LA(3);

                            if ((synpred1_TestLang())) {
                                alt24 = 2;
                            } else if ((true)) {
                                alt24 = 4;
                            }

                        } else if (((LA24_3 >= '\u0000' && LA24_3 <= '\'') || (LA24_3 >= ')' && LA24_3 <= '9')
                                || (LA24_3 >= ';' && LA24_3 <= '\uFFFF')) && (synpred1_TestLang())) {
                            alt24 = 2;
                        } else if ((LA24_3 == '(') && (synpred1_TestLang())) {
                            alt24 = 2;
                        }

                    }

                    switch (alt24) {
                    case 1:
                    // TestLang.g:324:25: ~ ( '(' | ':' )
                    {
                        if ((input.LA(1) >= '\u0000' && input.LA(1) <= '\'')
                                || (input.LA(1) >= ')' && input.LA(1) <= '9')
                                || (input.LA(1) >= ';' && input.LA(1) <= '\uFFFF')) {
                            input.consume();
                            state.failed = false;
                        } else {
                            if (state.backtracking > 0) {
                                state.failed = true;
                                return;
                            }
                            MismatchedSetException mse = new MismatchedSetException(null, input);
                            recover(mse);
                            throw mse;
                        }

                    }
                        break;
                    case 2:
                    // TestLang.g:325:33: ( '(' ~ ':' )=> '('
                    {
                        match('(');
                        if (state.failed)
                            return;

                    }
                        break;
                    case 3:
                    // TestLang.g:326:33: ( ':' ~ ')' )=> ':'
                    {
                        match(':');
                        if (state.failed)
                            return;

                    }
                        break;
                    case 4:
                    // TestLang.g:327:33: COMMENT_XQUERY
                    {
                        mCOMMENT_XQUERY();
                        if (state.failed)
                            return;

                    }
                        break;

                    default:
                        break loop24;
                    }
                } while (true);

                match(":)");
                if (state.failed)
                    return;

                if (state.backtracking == 0) {
                    skip();
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "COMMENT_XQUERY"

    // $ANTLR start "WS"
    public final void mWS() throws RecognitionException {
        try {
            int _type = WS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestLang.g:348:5: ( ( ' ' | '\\t' | '\\r' | '\\n' | '\\u000C' )+ )
            // TestLang.g:348:9: ( ' ' | '\\t' | '\\r' | '\\n' | '\\u000C' )+
            {
                // TestLang.g:348:9: ( ' ' | '\\t' | '\\r' | '\\n' | '\\u000C' )+
                int cnt25 = 0;
                loop25: do {
                    int alt25 = 2;
                    int LA25_0 = input.LA(1);

                    if (((LA25_0 >= '\t' && LA25_0 <= '\n') || (LA25_0 >= '\f' && LA25_0 <= '\r') || LA25_0 == ' ')) {
                        alt25 = 1;
                    }

                    switch (alt25) {
                    case 1:
                    // TestLang.g:
                    {
                        if ((input.LA(1) >= '\t' && input.LA(1) <= '\n') || (input.LA(1) >= '\f' && input.LA(1) <= '\r')
                                || input.LA(1) == ' ') {
                            input.consume();
                            state.failed = false;
                        } else {
                            if (state.backtracking > 0) {
                                state.failed = true;
                                return;
                            }
                            MismatchedSetException mse = new MismatchedSetException(null, input);
                            recover(mse);
                            throw mse;
                        }

                    }
                        break;

                    default:
                        if (cnt25 >= 1)
                            break loop25;
                        if (state.backtracking > 0) {
                            state.failed = true;
                            return;
                        }
                        EarlyExitException eee = new EarlyExitException(25, input);
                        throw eee;
                    }
                    cnt25++;
                } while (true);

                if (state.backtracking == 0) {
                    //_channel=HIDDEN;
                    skip();
                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "WS"

    public void mTokens() throws RecognitionException {
        // TestLang.g:1:8: ( KW_SQL_ALL | KW_SQL_ALTER | KW_SQL_BY | KW_SQL_CREATE | KW_SQL_DATABASE | KW_SQL_DELETE | KW_SQL_DISTINCT | KW_SQL_DROP | KW_SQL_FROM | KW_SQL_INDEX | KW_SQL_INSERT | KW_SQL_INTO | KW_SQL_ORDER | KW_SQL_SELECT | KW_SQL_SET | KW_SQL_TABLE | KW_SQL_TRUNCATE | KW_SQL_UNIQUE | KW_SQL_UPDATE | KW_SQL_VALUES | KW_SQL_VIEW | KW_SQL_WHERE | KW_XQUERY_AND | KW_XQUERY_AS | KW_XQUERY_BASE_URI | KW_XQUERY_BOUNDARY_SPACE | KW_XQUERY_CASE | KW_XQUERY_CAST | KW_XQUERY_CASTABLE | KW_XQUERY_COMMENT | KW_XQUERY_CONSTRUCTION | KW_XQUERY_COPY_NAMESPACES | KW_XQUERY_DECLARE | KW_XQUERY_DEFAULT | KW_XQUERY_DIV | KW_XQUERY_DOCNODE | KW_XQUERY_ELEMENT | KW_XQUERY_ELSE | KW_XQUERY_EMPTY_SEQUENCE | KW_XQUERY_EQ | KW_XQUERY_EVERY | KW_XQUERY_EXCEPT | KW_XQUERY_FOR | KW_XQUERY_FUNCTION | KW_XQUERY_GE | KW_XQUERY_GT | KW_XQUERY_IDIV | KW_XQUERY_IF | KW_XQUERY_IMPORT | KW_XQUERY_IN | KW_XQUERY_INSTANCE | KW_XQUERY_INTERSECT | KW_XQUERY_IS | KW_XQUERY_ITEM | KW_XQUERY_LE | KW_XQUERY_LET | KW_XQUERY_LT | KW_XQUERY_MOD | KW_XQUERY_MODULE | KW_XQUERY_NAMESPACE | KW_XQUERY_NE | KW_XQUERY_NODE | KW_XQUERY_OF | KW_XQUERY_OPTION | KW_XQUERY_OR | KW_XQUERY_ORDERING | KW_XQUERY_PROCESSING_INSTRUCTION | KW_XQUERY_RETURN | KW_XQUERY_SATISFIES | KW_XQUERY_SCHEMA | KW_XQUERY_SCHEMA_ATTR | KW_XQUERY_SCHEMA_ELEM | KW_XQUERY_SOME | KW_XQUERY_TEXT | KW_XQUERY_THEN | KW_XQUERY_TO | KW_XQUERY_TREAT | KW_XQUERY_TYPESWITCH | KW_XQUERY_UNION | KW_XQUERY_VARIABLE | KW_XQUERY_VALIDATE | KW_XQUERY_VERSION | KW_XQUERY_XQUERY | KW_XQUERY_ANCESTOR_COLON | KW_XQUERY_ANCESTOR_OR_SELF_COLON | KW_XQUERY_ATTRIBUTE_COLON | KW_XQUERY_CHILD_COLON | KW_XQUERY_DESCENDANT_COLON | KW_XQUERY_DESCENDANT_OR_SELF_COLON | KW_XQUERY_FOLLOWING_COLON | KW_XQUERY_FOLLOWING_SIBLING_COLON | KW_XQUERY_NAMESPACE_COLON | KW_XQUERY_PARENT_COLON | KW_XQUERY_PRECEDING_COLON | KW_XQUERY_PRECEDING_SIBLING_COLON | KW_XQUERY_SELF_COLON | LEFT_PAREN | RIGHT_PAREN | STAR | EQ | LE | GE | VERTICAL_BAR | AMPERSAND | LT | GT | SEMICOLON | COLON | PLUS | MINUS | LEFT_BRACKET | RIGHT_BRACKET | LEFT_BRACE | RIGHT_BRACE | COMMA | DOT | SLASH | QUESTION_MARK | EXCLAMATION | APPROX | LT_GT | PERCENT | CIRCUMFLEX | POUND_SIGN | DOUBLE_SLASH | DOUBLE_DOT | DOUBLE_LT | DOUBLE_GT | EXCLAMATION_EQ | COLON_EQ | LT_EXCLAMATION | LEFT_PAREN_POUND_SIGN | RIGHT_PAREN_POUND_SIGN | AT | DOLLAR | STRING_LITERAL | DECIMAL_LITERAL | OCTAL_OR_HEX_LITERAL | IDENTIFIER | COMMENT_SQL | COMMENT_XQUERY | WS )
        int alt26 = 142;
        alt26 = dfa26.predict(input);
        switch (alt26) {
        case 1:
        // TestLang.g:1:10: KW_SQL_ALL
        {
            mKW_SQL_ALL();
            if (state.failed)
                return;

        }
            break;
        case 2:
        // TestLang.g:1:21: KW_SQL_ALTER
        {
            mKW_SQL_ALTER();
            if (state.failed)
                return;

        }
            break;
        case 3:
        // TestLang.g:1:34: KW_SQL_BY
        {
            mKW_SQL_BY();
            if (state.failed)
                return;

        }
            break;
        case 4:
        // TestLang.g:1:44: KW_SQL_CREATE
        {
            mKW_SQL_CREATE();
            if (state.failed)
                return;

        }
            break;
        case 5:
        // TestLang.g:1:58: KW_SQL_DATABASE
        {
            mKW_SQL_DATABASE();
            if (state.failed)
                return;

        }
            break;
        case 6:
        // TestLang.g:1:74: KW_SQL_DELETE
        {
            mKW_SQL_DELETE();
            if (state.failed)
                return;

        }
            break;
        case 7:
        // TestLang.g:1:88: KW_SQL_DISTINCT
        {
            mKW_SQL_DISTINCT();
            if (state.failed)
                return;

        }
            break;
        case 8:
        // TestLang.g:1:104: KW_SQL_DROP
        {
            mKW_SQL_DROP();
            if (state.failed)
                return;

        }
            break;
        case 9:
        // TestLang.g:1:116: KW_SQL_FROM
        {
            mKW_SQL_FROM();
            if (state.failed)
                return;

        }
            break;
        case 10:
        // TestLang.g:1:128: KW_SQL_INDEX
        {
            mKW_SQL_INDEX();
            if (state.failed)
                return;

        }
            break;
        case 11:
        // TestLang.g:1:141: KW_SQL_INSERT
        {
            mKW_SQL_INSERT();
            if (state.failed)
                return;

        }
            break;
        case 12:
        // TestLang.g:1:155: KW_SQL_INTO
        {
            mKW_SQL_INTO();
            if (state.failed)
                return;

        }
            break;
        case 13:
        // TestLang.g:1:167: KW_SQL_ORDER
        {
            mKW_SQL_ORDER();
            if (state.failed)
                return;

        }
            break;
        case 14:
        // TestLang.g:1:180: KW_SQL_SELECT
        {
            mKW_SQL_SELECT();
            if (state.failed)
                return;

        }
            break;
        case 15:
        // TestLang.g:1:194: KW_SQL_SET
        {
            mKW_SQL_SET();
            if (state.failed)
                return;

        }
            break;
        case 16:
        // TestLang.g:1:205: KW_SQL_TABLE
        {
            mKW_SQL_TABLE();
            if (state.failed)
                return;

        }
            break;
        case 17:
        // TestLang.g:1:218: KW_SQL_TRUNCATE
        {
            mKW_SQL_TRUNCATE();
            if (state.failed)
                return;

        }
            break;
        case 18:
        // TestLang.g:1:234: KW_SQL_UNIQUE
        {
            mKW_SQL_UNIQUE();
            if (state.failed)
                return;

        }
            break;
        case 19:
        // TestLang.g:1:248: KW_SQL_UPDATE
        {
            mKW_SQL_UPDATE();
            if (state.failed)
                return;

        }
            break;
        case 20:
        // TestLang.g:1:262: KW_SQL_VALUES
        {
            mKW_SQL_VALUES();
            if (state.failed)
                return;

        }
            break;
        case 21:
        // TestLang.g:1:276: KW_SQL_VIEW
        {
            mKW_SQL_VIEW();
            if (state.failed)
                return;

        }
            break;
        case 22:
        // TestLang.g:1:288: KW_SQL_WHERE
        {
            mKW_SQL_WHERE();
            if (state.failed)
                return;

        }
            break;
        case 23:
        // TestLang.g:1:301: KW_XQUERY_AND
        {
            mKW_XQUERY_AND();
            if (state.failed)
                return;

        }
            break;
        case 24:
        // TestLang.g:1:315: KW_XQUERY_AS
        {
            mKW_XQUERY_AS();
            if (state.failed)
                return;

        }
            break;
        case 25:
        // TestLang.g:1:328: KW_XQUERY_BASE_URI
        {
            mKW_XQUERY_BASE_URI();
            if (state.failed)
                return;

        }
            break;
        case 26:
        // TestLang.g:1:347: KW_XQUERY_BOUNDARY_SPACE
        {
            mKW_XQUERY_BOUNDARY_SPACE();
            if (state.failed)
                return;

        }
            break;
        case 27:
        // TestLang.g:1:372: KW_XQUERY_CASE
        {
            mKW_XQUERY_CASE();
            if (state.failed)
                return;

        }
            break;
        case 28:
        // TestLang.g:1:387: KW_XQUERY_CAST
        {
            mKW_XQUERY_CAST();
            if (state.failed)
                return;

        }
            break;
        case 29:
        // TestLang.g:1:402: KW_XQUERY_CASTABLE
        {
            mKW_XQUERY_CASTABLE();
            if (state.failed)
                return;

        }
            break;
        case 30:
        // TestLang.g:1:421: KW_XQUERY_COMMENT
        {
            mKW_XQUERY_COMMENT();
            if (state.failed)
                return;

        }
            break;
        case 31:
        // TestLang.g:1:439: KW_XQUERY_CONSTRUCTION
        {
            mKW_XQUERY_CONSTRUCTION();
            if (state.failed)
                return;

        }
            break;
        case 32:
        // TestLang.g:1:462: KW_XQUERY_COPY_NAMESPACES
        {
            mKW_XQUERY_COPY_NAMESPACES();
            if (state.failed)
                return;

        }
            break;
        case 33:
        // TestLang.g:1:488: KW_XQUERY_DECLARE
        {
            mKW_XQUERY_DECLARE();
            if (state.failed)
                return;

        }
            break;
        case 34:
        // TestLang.g:1:506: KW_XQUERY_DEFAULT
        {
            mKW_XQUERY_DEFAULT();
            if (state.failed)
                return;

        }
            break;
        case 35:
        // TestLang.g:1:524: KW_XQUERY_DIV
        {
            mKW_XQUERY_DIV();
            if (state.failed)
                return;

        }
            break;
        case 36:
        // TestLang.g:1:538: KW_XQUERY_DOCNODE
        {
            mKW_XQUERY_DOCNODE();
            if (state.failed)
                return;

        }
            break;
        case 37:
        // TestLang.g:1:556: KW_XQUERY_ELEMENT
        {
            mKW_XQUERY_ELEMENT();
            if (state.failed)
                return;

        }
            break;
        case 38:
        // TestLang.g:1:574: KW_XQUERY_ELSE
        {
            mKW_XQUERY_ELSE();
            if (state.failed)
                return;

        }
            break;
        case 39:
        // TestLang.g:1:589: KW_XQUERY_EMPTY_SEQUENCE
        {
            mKW_XQUERY_EMPTY_SEQUENCE();
            if (state.failed)
                return;

        }
            break;
        case 40:
        // TestLang.g:1:614: KW_XQUERY_EQ
        {
            mKW_XQUERY_EQ();
            if (state.failed)
                return;

        }
            break;
        case 41:
        // TestLang.g:1:627: KW_XQUERY_EVERY
        {
            mKW_XQUERY_EVERY();
            if (state.failed)
                return;

        }
            break;
        case 42:
        // TestLang.g:1:643: KW_XQUERY_EXCEPT
        {
            mKW_XQUERY_EXCEPT();
            if (state.failed)
                return;

        }
            break;
        case 43:
        // TestLang.g:1:660: KW_XQUERY_FOR
        {
            mKW_XQUERY_FOR();
            if (state.failed)
                return;

        }
            break;
        case 44:
        // TestLang.g:1:674: KW_XQUERY_FUNCTION
        {
            mKW_XQUERY_FUNCTION();
            if (state.failed)
                return;

        }
            break;
        case 45:
        // TestLang.g:1:693: KW_XQUERY_GE
        {
            mKW_XQUERY_GE();
            if (state.failed)
                return;

        }
            break;
        case 46:
        // TestLang.g:1:706: KW_XQUERY_GT
        {
            mKW_XQUERY_GT();
            if (state.failed)
                return;

        }
            break;
        case 47:
        // TestLang.g:1:719: KW_XQUERY_IDIV
        {
            mKW_XQUERY_IDIV();
            if (state.failed)
                return;

        }
            break;
        case 48:
        // TestLang.g:1:734: KW_XQUERY_IF
        {
            mKW_XQUERY_IF();
            if (state.failed)
                return;

        }
            break;
        case 49:
        // TestLang.g:1:747: KW_XQUERY_IMPORT
        {
            mKW_XQUERY_IMPORT();
            if (state.failed)
                return;

        }
            break;
        case 50:
        // TestLang.g:1:764: KW_XQUERY_IN
        {
            mKW_XQUERY_IN();
            if (state.failed)
                return;

        }
            break;
        case 51:
        // TestLang.g:1:777: KW_XQUERY_INSTANCE
        {
            mKW_XQUERY_INSTANCE();
            if (state.failed)
                return;

        }
            break;
        case 52:
        // TestLang.g:1:796: KW_XQUERY_INTERSECT
        {
            mKW_XQUERY_INTERSECT();
            if (state.failed)
                return;

        }
            break;
        case 53:
        // TestLang.g:1:816: KW_XQUERY_IS
        {
            mKW_XQUERY_IS();
            if (state.failed)
                return;

        }
            break;
        case 54:
        // TestLang.g:1:829: KW_XQUERY_ITEM
        {
            mKW_XQUERY_ITEM();
            if (state.failed)
                return;

        }
            break;
        case 55:
        // TestLang.g:1:844: KW_XQUERY_LE
        {
            mKW_XQUERY_LE();
            if (state.failed)
                return;

        }
            break;
        case 56:
        // TestLang.g:1:857: KW_XQUERY_LET
        {
            mKW_XQUERY_LET();
            if (state.failed)
                return;

        }
            break;
        case 57:
        // TestLang.g:1:871: KW_XQUERY_LT
        {
            mKW_XQUERY_LT();
            if (state.failed)
                return;

        }
            break;
        case 58:
        // TestLang.g:1:884: KW_XQUERY_MOD
        {
            mKW_XQUERY_MOD();
            if (state.failed)
                return;

        }
            break;
        case 59:
        // TestLang.g:1:898: KW_XQUERY_MODULE
        {
            mKW_XQUERY_MODULE();
            if (state.failed)
                return;

        }
            break;
        case 60:
        // TestLang.g:1:915: KW_XQUERY_NAMESPACE
        {
            mKW_XQUERY_NAMESPACE();
            if (state.failed)
                return;

        }
            break;
        case 61:
        // TestLang.g:1:935: KW_XQUERY_NE
        {
            mKW_XQUERY_NE();
            if (state.failed)
                return;

        }
            break;
        case 62:
        // TestLang.g:1:948: KW_XQUERY_NODE
        {
            mKW_XQUERY_NODE();
            if (state.failed)
                return;

        }
            break;
        case 63:
        // TestLang.g:1:963: KW_XQUERY_OF
        {
            mKW_XQUERY_OF();
            if (state.failed)
                return;

        }
            break;
        case 64:
        // TestLang.g:1:976: KW_XQUERY_OPTION
        {
            mKW_XQUERY_OPTION();
            if (state.failed)
                return;

        }
            break;
        case 65:
        // TestLang.g:1:993: KW_XQUERY_OR
        {
            mKW_XQUERY_OR();
            if (state.failed)
                return;

        }
            break;
        case 66:
        // TestLang.g:1:1006: KW_XQUERY_ORDERING
        {
            mKW_XQUERY_ORDERING();
            if (state.failed)
                return;

        }
            break;
        case 67:
        // TestLang.g:1:1025: KW_XQUERY_PROCESSING_INSTRUCTION
        {
            mKW_XQUERY_PROCESSING_INSTRUCTION();
            if (state.failed)
                return;

        }
            break;
        case 68:
        // TestLang.g:1:1058: KW_XQUERY_RETURN
        {
            mKW_XQUERY_RETURN();
            if (state.failed)
                return;

        }
            break;
        case 69:
        // TestLang.g:1:1075: KW_XQUERY_SATISFIES
        {
            mKW_XQUERY_SATISFIES();
            if (state.failed)
                return;

        }
            break;
        case 70:
        // TestLang.g:1:1095: KW_XQUERY_SCHEMA
        {
            mKW_XQUERY_SCHEMA();
            if (state.failed)
                return;

        }
            break;
        case 71:
        // TestLang.g:1:1112: KW_XQUERY_SCHEMA_ATTR
        {
            mKW_XQUERY_SCHEMA_ATTR();
            if (state.failed)
                return;

        }
            break;
        case 72:
        // TestLang.g:1:1134: KW_XQUERY_SCHEMA_ELEM
        {
            mKW_XQUERY_SCHEMA_ELEM();
            if (state.failed)
                return;

        }
            break;
        case 73:
        // TestLang.g:1:1156: KW_XQUERY_SOME
        {
            mKW_XQUERY_SOME();
            if (state.failed)
                return;

        }
            break;
        case 74:
        // TestLang.g:1:1171: KW_XQUERY_TEXT
        {
            mKW_XQUERY_TEXT();
            if (state.failed)
                return;

        }
            break;
        case 75:
        // TestLang.g:1:1186: KW_XQUERY_THEN
        {
            mKW_XQUERY_THEN();
            if (state.failed)
                return;

        }
            break;
        case 76:
        // TestLang.g:1:1201: KW_XQUERY_TO
        {
            mKW_XQUERY_TO();
            if (state.failed)
                return;

        }
            break;
        case 77:
        // TestLang.g:1:1214: KW_XQUERY_TREAT
        {
            mKW_XQUERY_TREAT();
            if (state.failed)
                return;

        }
            break;
        case 78:
        // TestLang.g:1:1230: KW_XQUERY_TYPESWITCH
        {
            mKW_XQUERY_TYPESWITCH();
            if (state.failed)
                return;

        }
            break;
        case 79:
        // TestLang.g:1:1251: KW_XQUERY_UNION
        {
            mKW_XQUERY_UNION();
            if (state.failed)
                return;

        }
            break;
        case 80:
        // TestLang.g:1:1267: KW_XQUERY_VARIABLE
        {
            mKW_XQUERY_VARIABLE();
            if (state.failed)
                return;

        }
            break;
        case 81:
        // TestLang.g:1:1286: KW_XQUERY_VALIDATE
        {
            mKW_XQUERY_VALIDATE();
            if (state.failed)
                return;

        }
            break;
        case 82:
        // TestLang.g:1:1305: KW_XQUERY_VERSION
        {
            mKW_XQUERY_VERSION();
            if (state.failed)
                return;

        }
            break;
        case 83:
        // TestLang.g:1:1323: KW_XQUERY_XQUERY
        {
            mKW_XQUERY_XQUERY();
            if (state.failed)
                return;

        }
            break;
        case 84:
        // TestLang.g:1:1340: KW_XQUERY_ANCESTOR_COLON
        {
            mKW_XQUERY_ANCESTOR_COLON();
            if (state.failed)
                return;

        }
            break;
        case 85:
        // TestLang.g:1:1365: KW_XQUERY_ANCESTOR_OR_SELF_COLON
        {
            mKW_XQUERY_ANCESTOR_OR_SELF_COLON();
            if (state.failed)
                return;

        }
            break;
        case 86:
        // TestLang.g:1:1398: KW_XQUERY_ATTRIBUTE_COLON
        {
            mKW_XQUERY_ATTRIBUTE_COLON();
            if (state.failed)
                return;

        }
            break;
        case 87:
        // TestLang.g:1:1424: KW_XQUERY_CHILD_COLON
        {
            mKW_XQUERY_CHILD_COLON();
            if (state.failed)
                return;

        }
            break;
        case 88:
        // TestLang.g:1:1446: KW_XQUERY_DESCENDANT_COLON
        {
            mKW_XQUERY_DESCENDANT_COLON();
            if (state.failed)
                return;

        }
            break;
        case 89:
        // TestLang.g:1:1473: KW_XQUERY_DESCENDANT_OR_SELF_COLON
        {
            mKW_XQUERY_DESCENDANT_OR_SELF_COLON();
            if (state.failed)
                return;

        }
            break;
        case 90:
        // TestLang.g:1:1508: KW_XQUERY_FOLLOWING_COLON
        {
            mKW_XQUERY_FOLLOWING_COLON();
            if (state.failed)
                return;

        }
            break;
        case 91:
        // TestLang.g:1:1534: KW_XQUERY_FOLLOWING_SIBLING_COLON
        {
            mKW_XQUERY_FOLLOWING_SIBLING_COLON();
            if (state.failed)
                return;

        }
            break;
        case 92:
        // TestLang.g:1:1568: KW_XQUERY_NAMESPACE_COLON
        {
            mKW_XQUERY_NAMESPACE_COLON();
            if (state.failed)
                return;

        }
            break;
        case 93:
        // TestLang.g:1:1594: KW_XQUERY_PARENT_COLON
        {
            mKW_XQUERY_PARENT_COLON();
            if (state.failed)
                return;

        }
            break;
        case 94:
        // TestLang.g:1:1617: KW_XQUERY_PRECEDING_COLON
        {
            mKW_XQUERY_PRECEDING_COLON();
            if (state.failed)
                return;

        }
            break;
        case 95:
        // TestLang.g:1:1643: KW_XQUERY_PRECEDING_SIBLING_COLON
        {
            mKW_XQUERY_PRECEDING_SIBLING_COLON();
            if (state.failed)
                return;

        }
            break;
        case 96:
        // TestLang.g:1:1677: KW_XQUERY_SELF_COLON
        {
            mKW_XQUERY_SELF_COLON();
            if (state.failed)
                return;

        }
            break;
        case 97:
        // TestLang.g:1:1698: LEFT_PAREN
        {
            mLEFT_PAREN();
            if (state.failed)
                return;

        }
            break;
        case 98:
        // TestLang.g:1:1709: RIGHT_PAREN
        {
            mRIGHT_PAREN();
            if (state.failed)
                return;

        }
            break;
        case 99:
        // TestLang.g:1:1721: STAR
        {
            mSTAR();
            if (state.failed)
                return;

        }
            break;
        case 100:
        // TestLang.g:1:1726: EQ
        {
            mEQ();
            if (state.failed)
                return;

        }
            break;
        case 101:
        // TestLang.g:1:1729: LE
        {
            mLE();
            if (state.failed)
                return;

        }
            break;
        case 102:
        // TestLang.g:1:1732: GE
        {
            mGE();
            if (state.failed)
                return;

        }
            break;
        case 103:
        // TestLang.g:1:1735: VERTICAL_BAR
        {
            mVERTICAL_BAR();
            if (state.failed)
                return;

        }
            break;
        case 104:
        // TestLang.g:1:1748: AMPERSAND
        {
            mAMPERSAND();
            if (state.failed)
                return;

        }
            break;
        case 105:
        // TestLang.g:1:1758: LT
        {
            mLT();
            if (state.failed)
                return;

        }
            break;
        case 106:
        // TestLang.g:1:1761: GT
        {
            mGT();
            if (state.failed)
                return;

        }
            break;
        case 107:
        // TestLang.g:1:1764: SEMICOLON
        {
            mSEMICOLON();
            if (state.failed)
                return;

        }
            break;
        case 108:
        // TestLang.g:1:1774: COLON
        {
            mCOLON();
            if (state.failed)
                return;

        }
            break;
        case 109:
        // TestLang.g:1:1780: PLUS
        {
            mPLUS();
            if (state.failed)
                return;

        }
            break;
        case 110:
        // TestLang.g:1:1785: MINUS
        {
            mMINUS();
            if (state.failed)
                return;

        }
            break;
        case 111:
        // TestLang.g:1:1791: LEFT_BRACKET
        {
            mLEFT_BRACKET();
            if (state.failed)
                return;

        }
            break;
        case 112:
        // TestLang.g:1:1804: RIGHT_BRACKET
        {
            mRIGHT_BRACKET();
            if (state.failed)
                return;

        }
            break;
        case 113:
        // TestLang.g:1:1818: LEFT_BRACE
        {
            mLEFT_BRACE();
            if (state.failed)
                return;

        }
            break;
        case 114:
        // TestLang.g:1:1829: RIGHT_BRACE
        {
            mRIGHT_BRACE();
            if (state.failed)
                return;

        }
            break;
        case 115:
        // TestLang.g:1:1841: COMMA
        {
            mCOMMA();
            if (state.failed)
                return;

        }
            break;
        case 116:
        // TestLang.g:1:1847: DOT
        {
            mDOT();
            if (state.failed)
                return;

        }
            break;
        case 117:
        // TestLang.g:1:1851: SLASH
        {
            mSLASH();
            if (state.failed)
                return;

        }
            break;
        case 118:
        // TestLang.g:1:1857: QUESTION_MARK
        {
            mQUESTION_MARK();
            if (state.failed)
                return;

        }
            break;
        case 119:
        // TestLang.g:1:1871: EXCLAMATION
        {
            mEXCLAMATION();
            if (state.failed)
                return;

        }
            break;
        case 120:
        // TestLang.g:1:1883: APPROX
        {
            mAPPROX();
            if (state.failed)
                return;

        }
            break;
        case 121:
        // TestLang.g:1:1890: LT_GT
        {
            mLT_GT();
            if (state.failed)
                return;

        }
            break;
        case 122:
        // TestLang.g:1:1896: PERCENT
        {
            mPERCENT();
            if (state.failed)
                return;

        }
            break;
        case 123:
        // TestLang.g:1:1904: CIRCUMFLEX
        {
            mCIRCUMFLEX();
            if (state.failed)
                return;

        }
            break;
        case 124:
        // TestLang.g:1:1915: POUND_SIGN
        {
            mPOUND_SIGN();
            if (state.failed)
                return;

        }
            break;
        case 125:
        // TestLang.g:1:1926: DOUBLE_SLASH
        {
            mDOUBLE_SLASH();
            if (state.failed)
                return;

        }
            break;
        case 126:
        // TestLang.g:1:1939: DOUBLE_DOT
        {
            mDOUBLE_DOT();
            if (state.failed)
                return;

        }
            break;
        case 127:
        // TestLang.g:1:1950: DOUBLE_LT
        {
            mDOUBLE_LT();
            if (state.failed)
                return;

        }
            break;
        case 128:
        // TestLang.g:1:1960: DOUBLE_GT
        {
            mDOUBLE_GT();
            if (state.failed)
                return;

        }
            break;
        case 129:
        // TestLang.g:1:1970: EXCLAMATION_EQ
        {
            mEXCLAMATION_EQ();
            if (state.failed)
                return;

        }
            break;
        case 130:
        // TestLang.g:1:1985: COLON_EQ
        {
            mCOLON_EQ();
            if (state.failed)
                return;

        }
            break;
        case 131:
        // TestLang.g:1:1994: LT_EXCLAMATION
        {
            mLT_EXCLAMATION();
            if (state.failed)
                return;

        }
            break;
        case 132:
        // TestLang.g:1:2009: LEFT_PAREN_POUND_SIGN
        {
            mLEFT_PAREN_POUND_SIGN();
            if (state.failed)
                return;

        }
            break;
        case 133:
        // TestLang.g:1:2031: RIGHT_PAREN_POUND_SIGN
        {
            mRIGHT_PAREN_POUND_SIGN();
            if (state.failed)
                return;

        }
            break;
        case 134:
        // TestLang.g:1:2054: AT
        {
            mAT();
            if (state.failed)
                return;

        }
            break;
        case 135:
        // TestLang.g:1:2057: DOLLAR
        {
            mDOLLAR();
            if (state.failed)
                return;

        }
            break;
        case 136:
        // TestLang.g:1:2064: STRING_LITERAL
        {
            mSTRING_LITERAL();
            if (state.failed)
                return;

        }
            break;
        case 137:
        // TestLang.g:1:2079: DECIMAL_LITERAL
        {
            mDECIMAL_LITERAL();
            if (state.failed)
                return;

        }
            break;
        case 138:
        // TestLang.g:1:2095: OCTAL_OR_HEX_LITERAL
        {
            mOCTAL_OR_HEX_LITERAL();
            if (state.failed)
                return;

        }
            break;
        case 139:
        // TestLang.g:1:2116: IDENTIFIER
        {
            mIDENTIFIER();
            if (state.failed)
                return;

        }
            break;
        case 140:
        // TestLang.g:1:2127: COMMENT_SQL
        {
            mCOMMENT_SQL();
            if (state.failed)
                return;

        }
            break;
        case 141:
        // TestLang.g:1:2139: COMMENT_XQUERY
        {
            mCOMMENT_XQUERY();
            if (state.failed)
                return;

        }
            break;
        case 142:
        // TestLang.g:1:2154: WS
        {
            mWS();
            if (state.failed)
                return;

        }
            break;

        }

    }

    // $ANTLR start synpred1_TestLang
    public final void synpred1_TestLang_fragment() throws RecognitionException {
        // TestLang.g:325:33: ( '(' ~ ':' )
        // TestLang.g:325:34: '(' ~ ':'
        {
            match('(');
            if (state.failed)
                return;

            if ((input.LA(1) >= '\u0000' && input.LA(1) <= '9') || (input.LA(1) >= ';' && input.LA(1) <= '\uFFFF')) {
                input.consume();
                state.failed = false;
            } else {
                if (state.backtracking > 0) {
                    state.failed = true;
                    return;
                }
                MismatchedSetException mse = new MismatchedSetException(null, input);
                recover(mse);
                throw mse;
            }

        }

    }
    // $ANTLR end synpred1_TestLang

    // $ANTLR start synpred2_TestLang
    public final void synpred2_TestLang_fragment() throws RecognitionException {
        // TestLang.g:326:33: ( ':' ~ ')' )
        // TestLang.g:326:34: ':' ~ ')'
        {
            match(':');
            if (state.failed)
                return;

            if ((input.LA(1) >= '\u0000' && input.LA(1) <= '(') || (input.LA(1) >= '*' && input.LA(1) <= '\uFFFF')) {
                input.consume();
                state.failed = false;
            } else {
                if (state.backtracking > 0) {
                    state.failed = true;
                    return;
                }
                MismatchedSetException mse = new MismatchedSetException(null, input);
                recover(mse);
                throw mse;
            }

        }

    }
    // $ANTLR end synpred2_TestLang

    public final boolean synpred1_TestLang() {
        state.backtracking++;
        int start = input.mark();
        try {
            synpred1_TestLang_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: " + re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        state.backtracking--;
        state.failed = false;
        return success;
    }

    public final boolean synpred2_TestLang() {
        state.backtracking++;
        int start = input.mark();
        try {
            synpred2_TestLang_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: " + re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        state.backtracking--;
        state.failed = false;
        return success;
    }

    protected DFA1 dfa1 = new DFA1(this);
    protected DFA26 dfa26 = new DFA26(this);
    static final String DFA1_eotS = "\1\uffff\1\4\4\uffff\1\3\1\10\1\uffff\1\3";
    static final String DFA1_eofS = "\12\uffff";
    static final String DFA1_minS = "\1\11\1\47\1\11\3\uffff\2\11\1\uffff\1\11";
    static final String DFA1_maxS = "\1\ufffd\1\47\1\ufffd\3\uffff\2\ufffd\1\uffff\1\ufffd";
    static final String DFA1_acceptS = "\3\uffff\1\3\1\4\1\2\2\uffff\1\1\1\uffff";
    static final String DFA1_specialS = "\12\uffff}>";
    static final String[] DFA1_transitionS = {
            "\2\3\2\uffff\1\3\22\uffff\7\3\1\1\64\3\1\2\ud7a3\3\u0800\uffff" + "\u1ffe\3", "\1\5",
            "\2\3\2\uffff\1\3\22\uffff\7\3\1\6\ud7d8\3\u0800\uffff\u1ffe" + "\3", "", "", "",
            "\2\10\2\uffff\1\10\22\uffff\7\10\1\7\ud7d8\10\u0800\uffff\u1ffe" + "\10",
            "\2\3\2\uffff\1\3\22\uffff\7\3\1\11\ud7d8\3\u0800\uffff\u1ffe" + "\3", "",
            "\2\10\2\uffff\1\10\22\uffff\7\10\1\7\ud7d8\10\u0800\uffff\u1ffe" + "\10" };

    static final short[] DFA1_eot = DFA.unpackEncodedString(DFA1_eotS);
    static final short[] DFA1_eof = DFA.unpackEncodedString(DFA1_eofS);
    static final char[] DFA1_min = DFA.unpackEncodedStringToUnsignedChars(DFA1_minS);
    static final char[] DFA1_max = DFA.unpackEncodedStringToUnsignedChars(DFA1_maxS);
    static final short[] DFA1_accept = DFA.unpackEncodedString(DFA1_acceptS);
    static final short[] DFA1_special = DFA.unpackEncodedString(DFA1_specialS);
    static final short[][] DFA1_transition;

    static {
        int numStates = DFA1_transitionS.length;
        DFA1_transition = new short[numStates][];
        for (int i = 0; i < numStates; i++) {
            DFA1_transition[i] = DFA.unpackEncodedString(DFA1_transitionS[i]);
        }
    }

    class DFA1 extends DFA {

        public DFA1(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 1;
            this.eot = DFA1_eot;
            this.eof = DFA1_eof;
            this.min = DFA1_min;
            this.max = DFA1_max;
            this.accept = DFA1_accept;
            this.special = DFA1_special;
            this.transition = DFA1_transition;
        }

        public String getDescription() {
            return "()* loopback of 235:13: ( '\\\\' QUOTE | QUOTE QUOTE | CHAR_NO_QUOTE )*";
        }
    }

    static final String DFA26_eotS = "\1\uffff\24\63\1\164\3\uffff\1\171\1\174\3\uffff\1\176\1\uffff\1"
            + "\u0080\5\uffff\1\u0082\1\u0084\1\uffff\1\u0086\3\uffff\1\u0088\3"
            + "\uffff\1\62\3\uffff\2\63\1\u0090\1\63\1\u0092\16\63\1\u00ab\1\63"
            + "\1\u00ad\1\63\1\u00af\1\63\1\u00b2\1\u00b3\11\63\1\u00bf\11\63\1"
            + "\u00cb\2\63\1\u00ce\1\u00cf\1\u00d1\1\u00d2\2\63\1\u00d5\5\63\27"
            + "\uffff\1\62\1\uffff\1\62\1\u00dc\1\63\1\u00de\1\63\1\uffff\1\63"
            + "\1\uffff\16\63\1\u00f0\3\63\1\u00f4\5\63\1\uffff\1\63\1\uffff\1"
            + "\63\1\uffff\2\63\2\uffff\2\63\1\u0103\10\63\1\uffff\13\63\1\uffff"
            + "\2\63\2\uffff\1\u011b\2\uffff\1\u011d\1\63\1\uffff\6\63\1\uffff"
            + "\1\63\1\uffff\5\63\1\u012b\1\u012d\12\63\1\uffff\1\u0138\1\63\1"
            + "\u013a\1\uffff\5\63\1\u0140\1\63\1\u0142\1\63\1\u0144\4\63\1\uffff"
            + "\2\63\1\u014b\3\63\1\u014f\1\u0150\7\63\1\u0158\3\63\1\u015c\3\63"
            + "\1\uffff\1\63\1\uffff\1\63\1\u0162\5\63\1\u0168\2\63\1\uffff\2\63"
            + "\1\uffff\1\63\1\uffff\2\63\1\uffff\7\63\1\uffff\1\63\1\uffff\2\63"
            + "\1\u017a\2\63\1\uffff\1\63\1\uffff\1\63\1\uffff\1\u0180\2\63\1\uffff"
            + "\2\63\1\uffff\1\u0185\1\63\1\u0187\2\uffff\2\63\1\u018a\4\63\1\uffff"
            + "\1\63\1\u0190\1\63\1\uffff\1\63\1\u0193\3\63\1\uffff\5\63\1\uffff"
            + "\3\63\1\u019f\3\63\1\uffff\1\63\1\u01a4\7\63\1\uffff\1\u01ac\2\63"
            + "\1\u01af\1\63\1\uffff\1\u01b1\1\u01b2\1\63\1\u01b5\1\uffff\1\63"
            + "\1\uffff\1\63\1\u01b8\1\uffff\1\u01b9\1\u01ba\3\63\1\uffff\1\63"
            + "\2\uffff\1\u01bf\1\u01c0\4\63\1\u01c5\1\u01c6\3\63\1\uffff\1\63"
            + "\1\u01cb\2\63\1\uffff\1\u01ce\1\u01cf\5\63\1\uffff\2\63\1\uffff"
            + "\1\63\2\uffff\1\63\2\uffff\2\63\3\uffff\2\63\1\u01df\1\u01e0\2\uffff"
            + "\3\63\3\uffff\3\63\1\u01e8\1\uffff\1\63\1\u01ea\2\uffff\1\63\1\u01ec"
            + "\2\63\1\u01ef\1\u01f0\1\63\1\u01f2\1\63\2\uffff\1\u01f4\1\63\1\u01f6"
            + "\1\u01f7\2\uffff\3\63\2\uffff\1\63\2\uffff\1\63\1\uffff\1\63\2\uffff"
            + "\1\63\2\uffff\1\u0200\1\uffff\1\u0201\1\uffff\1\63\2\uffff\1\u0204"
            + "\2\63\1\uffff\2\63\4\uffff\1\u020b\2\uffff\1\63\2\uffff\1\63\4\uffff" + "\1\u020e\1\uffff";
    static final String DFA26_eofS = "\u020f\uffff";
    static final String DFA26_minS = "\1\11\1\154\3\141\1\157\1\144\1\146\2\141\1\156\1\141\1\150\1\154"
            + "\2\145\1\157\2\141\1\145\1\161\1\43\3\uffff\1\41\1\75\3\uffff\1"
            + "\75\1\uffff\1\55\5\uffff\1\56\1\52\1\uffff\1\75\3\uffff\1\51\3\uffff"
            + "\1\60\3\uffff\1\154\1\143\1\56\1\164\1\56\1\163\1\165\1\145\1\163"
            + "\1\155\1\151\1\164\1\143\1\163\1\157\1\143\1\157\1\154\1\156\1\56"
            + "\1\151\1\56\1\160\1\56\1\145\2\56\1\164\1\154\1\164\1\150\1\155"
            + "\1\142\1\145\1\170\1\145\1\56\1\160\1\151\1\144\1\154\1\145\1\162"
            + "\2\145\1\160\1\56\1\145\1\143\4\56\1\144\1\155\1\56\1\144\1\145"
            + "\1\162\1\164\1\165\27\uffff\1\60\1\uffff\1\60\1\56\1\145\1\56\1"
            + "\145\1\uffff\1\162\1\uffff\1\145\1\156\1\141\1\145\1\155\1\163\1"
            + "\171\1\154\1\141\1\145\1\154\1\141\1\143\1\164\1\56\1\160\1\165"
            + "\1\155\1\56\1\154\1\143\3\145\1\uffff\1\166\1\uffff\1\157\1\uffff"
            + "\1\155\1\145\2\uffff\1\151\1\145\1\56\1\151\2\145\1\154\1\156\1"
            + "\141\1\164\1\156\1\uffff\1\145\1\157\1\141\2\151\1\167\1\163\1\162"
            + "\1\155\1\145\1\164\1\uffff\1\162\1\145\2\uffff\1\56\2\uffff\1\56"
            + "\1\145\1\uffff\1\145\2\143\1\145\1\165\1\145\1\uffff\1\162\1\uffff"
            + "\1\163\1\151\1\55\1\144\1\164\2\56\1\145\1\164\1\55\1\144\1\142"
            + "\1\164\1\141\1\165\1\145\1\151\1\uffff\1\56\1\155\1\56\1\uffff\1"
            + "\157\1\164\1\170\1\162\1\141\1\56\1\162\1\56\1\162\1\56\1\162\1"
            + "\157\1\143\1\72\1\uffff\1\163\1\155\1\56\1\145\1\143\1\164\2\56"
            + "\1\163\1\165\1\156\1\164\1\145\1\144\1\141\1\56\1\151\2\145\1\56"
            + "\2\171\1\160\1\uffff\1\154\1\uffff\1\163\1\56\2\145\1\156\2\162"
            + "\1\56\1\164\1\142\1\uffff\1\141\1\145\1\uffff\1\142\1\uffff\1\156"
            + "\1\162\1\uffff\1\72\1\141\1\145\1\162\1\154\2\156\1\uffff\1\145"
            + "\1\uffff\1\167\1\151\1\56\1\164\1\156\1\uffff\1\163\1\uffff\1\164"
            + "\1\uffff\1\56\1\156\1\164\1\uffff\1\146\1\141\1\uffff\1\56\1\141"
            + "\1\56\2\uffff\1\167\1\145\1\56\1\145\1\163\1\141\1\142\1\uffff\1"
            + "\157\1\56\1\156\1\uffff\1\55\1\56\1\164\1\145\1\160\1\uffff\1\163"
            + "\1\144\1\164\1\156\1\171\1\uffff\1\157\1\165\1\162\1\56\1\154\1"
            + "\164\1\165\1\uffff\1\163\1\56\1\145\1\164\1\144\1\143\1\156\1\151"
            + "\1\157\1\uffff\1\56\1\143\1\145\1\56\1\156\1\uffff\2\56\1\151\1"
            + "\55\1\uffff\1\164\1\uffff\1\151\1\56\1\uffff\2\56\1\164\1\154\1"
            + "\156\1\uffff\1\164\2\uffff\2\56\1\141\1\163\1\151\1\72\2\56\1\162"
            + "\1\164\1\171\1\uffff\1\145\1\56\1\143\1\145\1\uffff\2\56\1\141\2"
            + "\164\2\156\1\uffff\1\145\1\143\1\uffff\1\147\2\uffff\1\145\1\141"
            + "\1\uffff\1\145\1\164\3\uffff\2\145\2\56\2\uffff\1\143\1\151\1\156"
            + "\3\uffff\1\55\1\145\1\55\1\56\1\uffff\1\164\1\56\2\uffff\1\156\1"
            + "\56\1\55\1\147\2\56\1\164\1\56\1\163\2\uffff\1\56\1\143\2\56\2\uffff"
            + "\1\145\1\156\1\147\2\uffff\1\72\2\uffff\1\151\1\uffff\1\164\2\uffff"
            + "\1\55\2\uffff\1\56\1\uffff\1\56\1\uffff\1\150\2\uffff\1\56\1\147"
            + "\1\55\1\uffff\1\157\1\55\4\uffff\1\56\2\uffff\1\55\2\uffff\1\156" + "\4\uffff\1\56\1\uffff";
    static final String DFA26_maxS = "\1\ufffd\1\164\1\171\2\162\1\165\1\164\1\162\1\157\1\171\1\160\1"
            + "\151\1\150\1\170\2\164\2\157\1\162\1\145\1\161\1\72\3\uffff\2\76"
            + "\3\uffff\1\75\1\uffff\1\55\5\uffff\1\71\1\57\1\uffff\1\75\3\uffff"
            + "\1\51\3\uffff\1\130\3\uffff\1\164\1\144\1\ufffd\1\164\1\ufffd\1"
            + "\163\1\165\1\145\1\163\1\160\1\151\1\164\1\163\1\166\1\157\1\143"
            + "\1\157\1\162\1\156\1\ufffd\1\151\1\ufffd\1\160\1\ufffd\1\145\2\ufffd"
            + "\3\164\1\150\1\155\1\142\1\165\1\170\1\145\1\ufffd\1\160\1\151\1"
            + "\144\1\162\1\145\1\162\1\145\1\163\1\160\1\ufffd\1\145\1\143\4\ufffd"
            + "\1\144\1\155\1\ufffd\1\144\1\157\1\162\1\164\1\165\27\uffff\1\67"
            + "\1\uffff\1\67\1\ufffd\1\145\1\ufffd\1\145\1\uffff\1\162\1\uffff"
            + "\1\145\1\156\1\141\1\164\1\155\1\163\1\171\1\154\1\141\1\145\1\154"
            + "\1\141\1\143\1\164\1\ufffd\1\160\1\165\1\155\1\ufffd\1\154\1\143"
            + "\1\145\1\164\1\157\1\uffff\1\166\1\uffff\1\157\1\uffff\1\155\1\145"
            + "\2\uffff\1\151\1\146\1\ufffd\1\151\2\145\1\154\1\156\1\141\1\164"
            + "\1\156\1\uffff\1\145\1\161\1\141\1\165\1\151\1\167\1\163\1\162\1"
            + "\155\1\145\1\164\1\uffff\1\162\1\145\2\uffff\1\ufffd\2\uffff\1\ufffd"
            + "\1\145\1\uffff\1\145\2\143\1\145\1\165\1\145\1\uffff\1\162\1\uffff"
            + "\1\163\1\151\1\55\1\144\1\164\2\ufffd\1\145\1\164\1\55\1\144\1\142"
            + "\1\164\1\141\1\165\1\145\1\151\1\uffff\1\ufffd\1\155\1\ufffd\1\uffff"
            + "\1\157\1\164\1\170\1\162\1\141\1\ufffd\1\162\1\ufffd\1\162\1\ufffd"
            + "\1\162\1\157\1\143\1\72\1\uffff\1\163\1\155\1\ufffd\1\145\1\143"
            + "\1\164\2\ufffd\1\163\1\165\1\156\1\164\1\145\1\144\1\141\1\ufffd"
            + "\1\151\2\145\1\ufffd\2\171\1\160\1\uffff\1\154\1\uffff\1\163\1\ufffd"
            + "\2\145\1\156\2\162\1\ufffd\1\164\1\142\1\uffff\1\141\1\145\1\uffff"
            + "\1\142\1\uffff\1\156\1\162\1\uffff\1\72\1\141\1\145\1\162\1\154"
            + "\2\156\1\uffff\1\145\1\uffff\1\167\1\151\1\ufffd\1\164\1\156\1\uffff"
            + "\1\163\1\uffff\1\164\1\uffff\1\ufffd\1\156\1\164\1\uffff\1\146\1"
            + "\141\1\uffff\1\ufffd\1\141\1\ufffd\2\uffff\1\167\1\145\1\ufffd\1"
            + "\145\1\163\1\141\1\142\1\uffff\1\157\1\ufffd\1\156\1\uffff\1\55"
            + "\1\ufffd\1\164\1\145\1\160\1\uffff\1\163\1\144\1\164\1\156\1\171"
            + "\1\uffff\1\157\1\165\1\162\1\ufffd\1\154\1\164\1\165\1\uffff\1\163"
            + "\1\ufffd\1\145\1\164\1\144\1\143\1\156\1\151\1\157\1\uffff\1\ufffd"
            + "\1\143\1\145\1\ufffd\1\156\1\uffff\2\ufffd\1\151\1\ufffd\1\uffff"
            + "\1\164\1\uffff\1\151\1\ufffd\1\uffff\2\ufffd\1\164\1\154\1\156\1"
            + "\uffff\1\164\2\uffff\2\ufffd\1\141\1\163\1\151\1\72\2\ufffd\1\162"
            + "\1\164\1\171\1\uffff\1\145\1\ufffd\1\143\1\145\1\uffff\2\ufffd\1"
            + "\141\2\164\2\156\1\uffff\1\145\1\143\1\uffff\1\147\2\uffff\2\145"
            + "\1\uffff\1\145\1\164\3\uffff\2\145\2\ufffd\2\uffff\1\143\1\151\1"
            + "\156\3\uffff\1\72\1\145\1\55\1\ufffd\1\uffff\1\164\1\ufffd\2\uffff"
            + "\1\156\1\ufffd\1\55\1\147\2\ufffd\1\164\1\ufffd\1\163\2\uffff\1"
            + "\ufffd\1\143\2\ufffd\2\uffff\1\145\1\156\1\147\2\uffff\1\72\2\uffff"
            + "\1\151\1\uffff\1\164\2\uffff\1\72\2\uffff\1\ufffd\1\uffff\1\ufffd"
            + "\1\uffff\1\150\2\uffff\1\ufffd\1\147\1\72\1\uffff\1\157\1\72\4\uffff"
            + "\1\ufffd\2\uffff\1\55\2\uffff\1\156\4\uffff\1\ufffd\1\uffff";
    static final String DFA26_acceptS = "\26\uffff\1\142\1\143\1\144\2\uffff\1\147\1\150\1\153\1\uffff\1"
            + "\155\1\uffff\1\157\1\160\1\161\1\162\1\163\2\uffff\1\166\1\uffff"
            + "\1\170\1\172\1\173\1\uffff\1\u0086\1\u0087\1\u0088\1\uffff\1\u0089"
            + "\1\u008b\1\u008e\75\uffff\1\u0084\1\u008d\1\141\1\145\1\171\1\177"
            + "\1\u0083\1\151\1\146\1\u0080\1\152\1\u0082\1\154\1\u008c\1\156\1"
            + "\176\1\164\1\175\1\165\1\u0081\1\167\1\u0085\1\174\1\uffff\1\u008a"
            + "\5\uffff\1\30\1\uffff\1\3\30\uffff\1\62\1\uffff\1\60\1\uffff\1\65"
            + "\2\uffff\1\101\1\77\13\uffff\1\114\13\uffff\1\50\2\uffff\1\55\1"
            + "\56\1\uffff\1\67\1\71\2\uffff\1\75\6\uffff\1\1\1\uffff\1\27\21\uffff"
            + "\1\43\3\uffff\1\53\16\uffff\1\17\27\uffff\1\70\1\uffff\1\72\12\uffff"
            + "\1\31\2\uffff\1\33\1\uffff\1\34\2\uffff\1\40\7\uffff\1\10\1\uffff"
            + "\1\11\5\uffff\1\14\1\uffff\1\57\1\uffff\1\66\3\uffff\1\140\2\uffff"
            + "\1\111\3\uffff\1\112\1\113\7\uffff\1\25\3\uffff\1\46\5\uffff\1\76"
            + "\5\uffff\1\2\7\uffff\1\127\11\uffff\1\12\5\uffff\1\15\4\uffff\1"
            + "\20\1\uffff\1\115\2\uffff\1\117\5\uffff\1\26\1\uffff\1\47\1\51\13"
            + "\uffff\1\4\4\uffff\1\6\7\uffff\1\13\2\uffff\1\61\1\uffff\1\100\1"
            + "\16\2\uffff\1\106\2\uffff\1\22\1\23\1\24\4\uffff\1\52\1\73\3\uffff"
            + "\1\135\1\104\1\123\4\uffff\1\36\2\uffff\1\41\1\42\11\uffff\1\107"
            + "\1\110\4\uffff\1\122\1\45\3\uffff\1\124\1\125\1\uffff\1\32\1\35"
            + "\1\uffff\1\5\1\uffff\1\7\1\44\1\uffff\1\54\1\63\1\uffff\1\102\1"
            + "\uffff\1\21\1\uffff\1\121\1\120\3\uffff\1\126\2\uffff\1\132\1\133"
            + "\1\64\1\105\1\uffff\1\134\1\74\1\uffff\1\136\1\137\1\uffff\1\130" + "\1\131\1\116\1\103\1\uffff\1\37";
    static final String DFA26_specialS = "\u020f\uffff}>";
    static final String[] DFA26_transitionS = {
            "\2\64\1\uffff\2\64\22\uffff\1\64\1\51\1\60\1\55\1\57\1\53\1"
                    + "\34\1\60\1\25\1\26\1\27\1\37\1\45\1\40\1\46\1\47\1\61\11\62"
                    + "\1\36\1\35\1\31\1\30\1\32\1\50\1\56\32\63\1\41\1\uffff\1\42"
                    + "\1\54\1\63\1\uffff\1\1\1\2\1\3\1\4\1\15\1\5\1\16\1\63\1\6\2"
                    + "\63\1\17\1\20\1\21\1\7\1\22\1\63\1\23\1\10\1\11\1\12\1\13\1"
                    + "\14\1\24\2\63\1\43\1\33\1\44\1\52\101\uffff\27\63\1\uffff\37"
                    + "\63\1\uffff\u0208\63\160\uffff\16\63\1\uffff\u1c81\63\14\uffff"
                    + "\2\63\142\uffff\u0120\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff"
                    + "\63\u2100\uffff\u04d0\63\40\uffff\u020e\63",
            "\1\65\1\uffff\1\66\4\uffff\1\67\1\70", "\1\72\15\uffff\1\73\11\uffff\1\71",
            "\1\75\6\uffff\1\77\6\uffff\1\76\2\uffff\1\74",
            "\1\100\3\uffff\1\101\3\uffff\1\102\5\uffff\1\104\2\uffff\1" + "\103", "\1\106\2\uffff\1\105\2\uffff\1\107",
            "\1\111\1\uffff\1\112\6\uffff\1\113\1\110\4\uffff\1\114\1\115", "\1\117\11\uffff\1\120\1\uffff\1\116",
            "\1\122\1\uffff\1\123\1\uffff\1\121\11\uffff\1\124",
            "\1\125\3\uffff\1\127\2\uffff\1\130\6\uffff\1\131\2\uffff\1" + "\126\6\uffff\1\132", "\1\133\1\uffff\1\134",
            "\1\135\3\uffff\1\137\3\uffff\1\136", "\1\140", "\1\141\1\142\3\uffff\1\143\4\uffff\1\144\1\uffff\1\145",
            "\1\146\16\uffff\1\147", "\1\150\16\uffff\1\151", "\1\152", "\1\153\3\uffff\1\154\11\uffff\1\155",
            "\1\157\20\uffff\1\156", "\1\160", "\1\161", "\1\162\26\uffff\1\163", "", "", "",
            "\1\170\32\uffff\1\167\1\165\1\166", "\1\172\1\173", "", "", "", "\1\175", "", "\1\177", "", "", "", "", "",
            "\1\u0081\1\uffff\12\62", "\1\177\4\uffff\1\u0083", "", "\1\u0085", "", "", "", "\1\u0087", "", "", "",
            "\1\u0089\7\u008b\40\uffff\1\u008a", "", "", "", "\1\u008c\7\uffff\1\u008d", "\1\u008f\1\u008e",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u0091",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u0093", "\1\u0094", "\1\u0095", "\1\u0096", "\1\u0097\1\u0098\1\uffff\1\u0099", "\1\u009a", "\1\u009b",
            "\1\u009d\2\uffff\1\u009e\5\uffff\1\u009c\6\uffff\1\u009f", "\1\u00a0\2\uffff\1\u00a1", "\1\u00a2",
            "\1\u00a3", "\1\u00a4", "\1\u00a6\5\uffff\1\u00a5", "\1\u00a7",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\3\63"
                    + "\1\u00a8\16\63\1\u00a9\1\u00aa\6\63\74\uffff\1\63\10\uffff\27"
                    + "\63\1\uffff\37\63\1\uffff\u0286\63\1\uffff\u1c81\63\14\uffff"
                    + "\2\63\61\uffff\2\63\57\uffff\u0120\63\u0a70\uffff\u03f0\63\21"
                    + "\uffff\ua7ff\63\u2100\uffff\u04d0\63\40\uffff\u020e\63",
            "\1\u00ac",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u00ae",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u00b0",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\3\63"
                    + "\1\u00b1\26\63\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1"
                    + "\uffff\u0286\63\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63"
                    + "\57\uffff\u0120\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100"
                    + "\uffff\u04d0\63\40\uffff\u020e\63",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u00b4", "\1\u00b5\7\uffff\1\u00b6", "\1\u00b7", "\1\u00b8", "\1\u00b9", "\1\u00ba",
            "\1\u00bc\17\uffff\1\u00bb", "\1\u00bd", "\1\u00be",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u00c0", "\1\u00c1", "\1\u00c2", "\1\u00c3\5\uffff\1\u00c4", "\1\u00c5", "\1\u00c6", "\1\u00c7",
            "\1\u00c8\15\uffff\1\u00c9", "\1\u00ca",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u00cc", "\1\u00cd",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\23\63"
                    + "\1\u00d0\6\63\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff"
                    + "\u0286\63\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff"
                    + "\u0120\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff" + "\u04d0\63\40\uffff\u020e\63",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u00d3", "\1\u00d4",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u00d6", "\1\u00d8\11\uffff\1\u00d7", "\1\u00d9", "\1\u00da", "\1\u00db", "", "", "", "", "", "", "", "",
            "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "\1\u0089\7\u008b", "", "\10\u008b",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u00dd",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u00df", "", "\1\u00e0", "", "\1\u00e1", "\1\u00e2", "\1\u00e3", "\1\u00e4\16\uffff\1\u00e5", "\1\u00e6",
            "\1\u00e7", "\1\u00e8", "\1\u00e9", "\1\u00ea", "\1\u00eb", "\1\u00ec", "\1\u00ed", "\1\u00ee", "\1\u00ef",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u00f1", "\1\u00f2", "\1\u00f3",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u00f5", "\1\u00f6", "\1\u00f7", "\1\u00f8\16\uffff\1\u00f9", "\1\u00fb\11\uffff\1\u00fa", "",
            "\1\u00fc", "", "\1\u00fd", "", "\1\u00fe", "\1\u00ff", "", "", "\1\u0100", "\1\u0101\1\u0102",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u0104", "\1\u0105", "\1\u0106", "\1\u0107", "\1\u0108", "\1\u0109", "\1\u010a", "\1\u010b", "",
            "\1\u010c", "\1\u010e\1\uffff\1\u010d", "\1\u010f", "\1\u0111\13\uffff\1\u0110", "\1\u0112", "\1\u0113",
            "\1\u0114", "\1\u0115", "\1\u0116", "\1\u0117", "\1\u0118", "", "\1\u0119", "\1\u011a", "", "",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "", "",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\24\63"
                    + "\1\u011c\5\63\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff"
                    + "\u0286\63\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff"
                    + "\u0120\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff" + "\u04d0\63\40\uffff\u020e\63",
            "\1\u011e", "", "\1\u011f", "\1\u0120", "\1\u0121", "\1\u0122", "\1\u0123", "\1\u0124", "", "\1\u0125", "",
            "\1\u0126", "\1\u0127", "\1\u0128", "\1\u0129", "\1\u012a",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\1\u012c"
                    + "\31\63\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286"
                    + "\63\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u012e", "\1\u012f", "\1\u0130", "\1\u0131", "\1\u0132", "\1\u0133", "\1\u0134", "\1\u0135", "\1\u0136",
            "\1\u0137", "",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u0139",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "", "\1\u013b", "\1\u013c", "\1\u013d", "\1\u013e", "\1\u013f",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u0141",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u0143",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u0145", "\1\u0146", "\1\u0147", "\1\u0148", "", "\1\u0149", "\1\u014a",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u014c", "\1\u014d", "\1\u014e",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u0151", "\1\u0152", "\1\u0153", "\1\u0154", "\1\u0155", "\1\u0156", "\1\u0157",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u0159", "\1\u015a", "\1\u015b",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u015d", "\1\u015e", "\1\u015f", "", "\1\u0160", "", "\1\u0161",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u0163", "\1\u0164", "\1\u0165", "\1\u0166", "\1\u0167",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u0169", "\1\u016a", "", "\1\u016b", "\1\u016c", "", "\1\u016d", "", "\1\u016e", "\1\u016f", "",
            "\1\u0170", "\1\u0171", "\1\u0172", "\1\u0173", "\1\u0174", "\1\u0175", "\1\u0176", "", "\1\u0177", "",
            "\1\u0178", "\1\u0179",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u017b", "\1\u017c", "", "\1\u017d", "", "\1\u017e", "",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\10\63"
                    + "\1\u017f\21\63\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1"
                    + "\uffff\u0286\63\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63"
                    + "\57\uffff\u0120\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100"
                    + "\uffff\u04d0\63\40\uffff\u020e\63",
            "\1\u0181", "\1\u0182", "", "\1\u0183", "\1\u0184", "",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u0186",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "", "", "\1\u0188", "\1\u0189",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u018b", "\1\u018c", "\1\u018d", "\1\u018e", "", "\1\u018f",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u0191", "", "\1\u0192",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u0194", "\1\u0195", "\1\u0196", "", "\1\u0197", "\1\u0198", "\1\u0199", "\1\u019a", "\1\u019b", "",
            "\1\u019c", "\1\u019d", "\1\u019e",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u01a0", "\1\u01a1", "\1\u01a2", "", "\1\u01a3",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u01a5", "\1\u01a6", "\1\u01a7", "\1\u01a8", "\1\u01a9", "\1\u01aa", "\1\u01ab", "",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u01ad", "\1\u01ae",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u01b0", "",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u01b3",
            "\1\u01b4\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff"
                    + "\32\63\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286"
                    + "\63\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "", "\1\u01b6", "", "\1\u01b7",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u01bb", "\1\u01bc", "\1\u01bd", "", "\1\u01be", "", "",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u01c1", "\1\u01c2", "\1\u01c3", "\1\u01c4",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u01c7", "\1\u01c8", "\1\u01c9", "", "\1\u01ca",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u01cc", "\1\u01cd", "",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u01d0", "\1\u01d1", "\1\u01d2", "\1\u01d3", "\1\u01d4", "", "\1\u01d5", "\1\u01d6", "", "\1\u01d7", "",
            "", "\1\u01d8", "\1\u01d9\3\uffff\1\u01da", "", "\1\u01db", "\1\u01dc", "", "", "", "\1\u01dd", "\1\u01de",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "", "", "\1\u01e1", "\1\u01e2", "\1\u01e3", "", "", "", "\1\u01e5\14\uffff\1\u01e4", "\1\u01e6", "\1\u01e7",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "", "\1\u01e9",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "", "", "\1\u01eb",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u01ed", "\1\u01ee",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u01f1",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u01f3", "", "",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u01f5",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "", "", "\1\u01f8", "\1\u01f9", "\1\u01fa", "", "", "\1\u01fb", "", "", "\1\u01fc", "", "\1\u01fd", "", "",
            "\1\u01ff\14\uffff\1\u01fe", "", "",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "", "\1\u0202", "", "",
            "\1\63\1\uffff\12\63\1\u0203\6\uffff\32\63\4\uffff\1\63\1\uffff"
                    + "\32\63\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286"
                    + "\63\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "\1\u0205", "\1\u0207\14\uffff\1\u0206", "", "\1\u0208", "\1\u020a\14\uffff\1\u0209", "", "", "", "",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "", "", "\1\u020c", "", "", "\1\u020d", "", "", "", "",
            "\1\63\1\uffff\12\63\7\uffff\32\63\4\uffff\1\63\1\uffff\32\63"
                    + "\74\uffff\1\63\10\uffff\27\63\1\uffff\37\63\1\uffff\u0286\63"
                    + "\1\uffff\u1c81\63\14\uffff\2\63\61\uffff\2\63\57\uffff\u0120"
                    + "\63\u0a70\uffff\u03f0\63\21\uffff\ua7ff\63\u2100\uffff\u04d0" + "\63\40\uffff\u020e\63",
            "" };

    static final short[] DFA26_eot = DFA.unpackEncodedString(DFA26_eotS);
    static final short[] DFA26_eof = DFA.unpackEncodedString(DFA26_eofS);
    static final char[] DFA26_min = DFA.unpackEncodedStringToUnsignedChars(DFA26_minS);
    static final char[] DFA26_max = DFA.unpackEncodedStringToUnsignedChars(DFA26_maxS);
    static final short[] DFA26_accept = DFA.unpackEncodedString(DFA26_acceptS);
    static final short[] DFA26_special = DFA.unpackEncodedString(DFA26_specialS);
    static final short[][] DFA26_transition;

    static {
        int numStates = DFA26_transitionS.length;
        DFA26_transition = new short[numStates][];
        for (int i = 0; i < numStates; i++) {
            DFA26_transition[i] = DFA.unpackEncodedString(DFA26_transitionS[i]);
        }
    }

    class DFA26 extends DFA {
        public DFA26(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 26;
            this.eot = DFA26_eot;
            this.eof = DFA26_eof;
            this.min = DFA26_min;
            this.max = DFA26_max;
            this.accept = DFA26_accept;
            this.special = DFA26_special;
            this.transition = DFA26_transition;
        }

        public String getDescription() {
            return "1:1: Tokens : ( KW_SQL_ALL | KW_SQL_ALTER | KW_SQL_BY | KW_SQL_CREATE | KW_SQL_DATABASE | KW_SQL_DELETE | KW_SQL_DISTINCT | KW_SQL_DROP | KW_SQL_FROM | KW_SQL_INDEX | KW_SQL_INSERT | KW_SQL_INTO | KW_SQL_ORDER | KW_SQL_SELECT | KW_SQL_SET | KW_SQL_TABLE | KW_SQL_TRUNCATE | KW_SQL_UNIQUE | KW_SQL_UPDATE | KW_SQL_VALUES | KW_SQL_VIEW | KW_SQL_WHERE | KW_XQUERY_AND | KW_XQUERY_AS | KW_XQUERY_BASE_URI | KW_XQUERY_BOUNDARY_SPACE | KW_XQUERY_CASE | KW_XQUERY_CAST | KW_XQUERY_CASTABLE | KW_XQUERY_COMMENT | KW_XQUERY_CONSTRUCTION | KW_XQUERY_COPY_NAMESPACES | KW_XQUERY_DECLARE | KW_XQUERY_DEFAULT | KW_XQUERY_DIV | KW_XQUERY_DOCNODE | KW_XQUERY_ELEMENT | KW_XQUERY_ELSE | KW_XQUERY_EMPTY_SEQUENCE | KW_XQUERY_EQ | KW_XQUERY_EVERY | KW_XQUERY_EXCEPT | KW_XQUERY_FOR | KW_XQUERY_FUNCTION | KW_XQUERY_GE | KW_XQUERY_GT | KW_XQUERY_IDIV | KW_XQUERY_IF | KW_XQUERY_IMPORT | KW_XQUERY_IN | KW_XQUERY_INSTANCE | KW_XQUERY_INTERSECT | KW_XQUERY_IS | KW_XQUERY_ITEM | KW_XQUERY_LE | KW_XQUERY_LET | KW_XQUERY_LT | KW_XQUERY_MOD | KW_XQUERY_MODULE | KW_XQUERY_NAMESPACE | KW_XQUERY_NE | KW_XQUERY_NODE | KW_XQUERY_OF | KW_XQUERY_OPTION | KW_XQUERY_OR | KW_XQUERY_ORDERING | KW_XQUERY_PROCESSING_INSTRUCTION | KW_XQUERY_RETURN | KW_XQUERY_SATISFIES | KW_XQUERY_SCHEMA | KW_XQUERY_SCHEMA_ATTR | KW_XQUERY_SCHEMA_ELEM | KW_XQUERY_SOME | KW_XQUERY_TEXT | KW_XQUERY_THEN | KW_XQUERY_TO | KW_XQUERY_TREAT | KW_XQUERY_TYPESWITCH | KW_XQUERY_UNION | KW_XQUERY_VARIABLE | KW_XQUERY_VALIDATE | KW_XQUERY_VERSION | KW_XQUERY_XQUERY | KW_XQUERY_ANCESTOR_COLON | KW_XQUERY_ANCESTOR_OR_SELF_COLON | KW_XQUERY_ATTRIBUTE_COLON | KW_XQUERY_CHILD_COLON | KW_XQUERY_DESCENDANT_COLON | KW_XQUERY_DESCENDANT_OR_SELF_COLON | KW_XQUERY_FOLLOWING_COLON | KW_XQUERY_FOLLOWING_SIBLING_COLON | KW_XQUERY_NAMESPACE_COLON | KW_XQUERY_PARENT_COLON | KW_XQUERY_PRECEDING_COLON | KW_XQUERY_PRECEDING_SIBLING_COLON | KW_XQUERY_SELF_COLON | LEFT_PAREN | RIGHT_PAREN | STAR | EQ | LE | GE | VERTICAL_BAR | AMPERSAND | LT | GT | SEMICOLON | COLON | PLUS | MINUS | LEFT_BRACKET | RIGHT_BRACKET | LEFT_BRACE | RIGHT_BRACE | COMMA | DOT | SLASH | QUESTION_MARK | EXCLAMATION | APPROX | LT_GT | PERCENT | CIRCUMFLEX | POUND_SIGN | DOUBLE_SLASH | DOUBLE_DOT | DOUBLE_LT | DOUBLE_GT | EXCLAMATION_EQ | COLON_EQ | LT_EXCLAMATION | LEFT_PAREN_POUND_SIGN | RIGHT_PAREN_POUND_SIGN | AT | DOLLAR | STRING_LITERAL | DECIMAL_LITERAL | OCTAL_OR_HEX_LITERAL | IDENTIFIER | COMMENT_SQL | COMMENT_XQUERY | WS );";
        }
    }
}
