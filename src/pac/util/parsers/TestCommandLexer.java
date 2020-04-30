// $ANTLR 3.4 TestCommand.g 2011-09-26 23:24:52

package pac.util.parsers;

import pac.org.antlr.runtime.*;
import pac.util.Ret;

import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

@SuppressWarnings({ "all", "warnings", "unchecked" })
public class TestCommandLexer extends Lexer {
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

    public TestCommandLexer() {
    }

    public TestCommandLexer(CharStream input) {
        this(input, new RecognizerSharedState());
    }

    public TestCommandLexer(CharStream input, RecognizerSharedState state) {
        super(input, state);
    }

    public String getGrammarFileName() {
        return "TestCommand.g";
    }

    // $ANTLR start "PATH"
    public final void mPATH() throws RecognitionException {
        try {
            int _type = PATH;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestCommand.g:35:5: ( ( TILDA | DOT | DOUBLE_DOT )? ( '/' NAME )+ )
            // TestCommand.g:35:7: ( TILDA | DOT | DOUBLE_DOT )? ( '/' NAME )+
            {
                // TestCommand.g:35:7: ( TILDA | DOT | DOUBLE_DOT )?
                int alt1 = 4;
                int LA1_0 = input.LA(1);

                if ((LA1_0 == '~')) {
                    alt1 = 1;
                } else if ((LA1_0 == '.')) {
                    int LA1_2 = input.LA(2);

                    if ((LA1_2 == '.')) {
                        alt1 = 3;
                    } else if ((LA1_2 == '/')) {
                        alt1 = 2;
                    }
                }
                switch (alt1) {
                case 1:
                // TestCommand.g:35:8: TILDA
                {
                    mTILDA();

                }
                    break;
                case 2:
                // TestCommand.g:35:16: DOT
                {
                    mDOT();

                }
                    break;
                case 3:
                // TestCommand.g:35:22: DOUBLE_DOT
                {
                    mDOUBLE_DOT();

                }
                    break;

                }

                // TestCommand.g:35:35: ( '/' NAME )+
                int cnt2 = 0;
                loop2: do {
                    int alt2 = 2;
                    int LA2_0 = input.LA(1);

                    if ((LA2_0 == '/')) {
                        alt2 = 1;
                    }

                    switch (alt2) {
                    case 1:
                    // TestCommand.g:35:36: '/' NAME
                    {
                        match('/');

                        mNAME();

                    }
                        break;

                    default:
                        if (cnt2 >= 1)
                            break loop2;
                        EarlyExitException eee = new EarlyExitException(2, input);
                        throw eee;
                    }
                    cnt2++;
                } while (true);

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "PATH"

    // $ANTLR start "NAME"
    public final void mNAME() throws RecognitionException {
        try {
            int _type = NAME;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestCommand.g:39:5: ( ( LETTER | DIGIT | UNDERSCORE | MINUS | DOT | DOLLAR )+ )
            // TestCommand.g:39:7: ( LETTER | DIGIT | UNDERSCORE | MINUS | DOT | DOLLAR )+
            {
                // TestCommand.g:39:7: ( LETTER | DIGIT | UNDERSCORE | MINUS | DOT | DOLLAR )+
                int cnt3 = 0;
                loop3: do {
                    int alt3 = 2;
                    int LA3_0 = input.LA(1);

                    if ((LA3_0 == '$' || (LA3_0 >= '-' && LA3_0 <= '.') || (LA3_0 >= '0' && LA3_0 <= '9')
                            || (LA3_0 >= 'A' && LA3_0 <= 'Z') || LA3_0 == '_' || (LA3_0 >= 'a' && LA3_0 <= 'z'))) {
                        alt3 = 1;
                    }

                    switch (alt3) {
                    case 1:
                    // TestCommand.g:
                    {
                        if (input.LA(1) == '$' || (input.LA(1) >= '-' && input.LA(1) <= '.')
                                || (input.LA(1) >= '0' && input.LA(1) <= '9')
                                || (input.LA(1) >= 'A' && input.LA(1) <= 'Z') || input.LA(1) == '_'
                                || (input.LA(1) >= 'a' && input.LA(1) <= 'z')) {
                            input.consume();
                        } else {
                            MismatchedSetException mse = new MismatchedSetException(null, input);
                            recover(mse);
                            throw mse;
                        }

                    }
                        break;

                    default:
                        if (cnt3 >= 1)
                            break loop3;
                        EarlyExitException eee = new EarlyExitException(3, input);
                        throw eee;
                    }
                    cnt3++;
                } while (true);

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "NAME"

    // $ANTLR start "LETTER"
    public final void mLETTER() throws RecognitionException {
        try {
            // TestCommand.g:43:5: ( 'a' .. 'z' | 'A' .. 'Z' )
            // TestCommand.g:
            {
                if ((input.LA(1) >= 'A' && input.LA(1) <= 'Z') || (input.LA(1) >= 'a' && input.LA(1) <= 'z')) {
                    input.consume();
                } else {
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

    // $ANTLR start "DIGIT"
    public final void mDIGIT() throws RecognitionException {
        try {
            // TestCommand.g:46:22: ( '0' .. '9' )
            // TestCommand.g:
            {
                if ((input.LA(1) >= '0' && input.LA(1) <= '9')) {
                    input.consume();
                } else {
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

    // $ANTLR start "Semicolon"
    public final void mSemicolon() throws RecognitionException {
        try {
            int _type = Semicolon;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestCommand.g:52:28: ( ';' )
            // TestCommand.g:52:32: ';'
            {
                match(';');

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "Semicolon"

    // $ANTLR start "GT"
    public final void mGT() throws RecognitionException {
        try {
            int _type = GT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestCommand.g:53:28: ( '>' )
            // TestCommand.g:53:32: '>'
            {
                match('>');

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "GT"

    // $ANTLR start "VERTICAL_BAR"
    public final void mVERTICAL_BAR() throws RecognitionException {
        try {
            int _type = VERTICAL_BAR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestCommand.g:54:28: ( '|' )
            // TestCommand.g:54:32: '|'
            {
                match('|');

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "VERTICAL_BAR"

    // $ANTLR start "LEFT_PAREN"
    public final void mLEFT_PAREN() throws RecognitionException {
        try {
            int _type = LEFT_PAREN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestCommand.g:55:28: ( '(' )
            // TestCommand.g:55:32: '('
            {
                match('(');

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
            // TestCommand.g:56:28: ( ')' )
            // TestCommand.g:56:32: ')'
            {
                match(')');

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "RIGHT_PAREN"

    // $ANTLR start "DOT"
    public final void mDOT() throws RecognitionException {
        try {
            // TestCommand.g:57:28: ( '.' )
            // TestCommand.g:57:32: '.'
            {
                match('.');

            }

        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "DOT"

    // $ANTLR start "DOUBLE_DOT"
    public final void mDOUBLE_DOT() throws RecognitionException {
        try {
            // TestCommand.g:58:28: ( '..' )
            // TestCommand.g:58:32: '..'
            {
                match("..");

            }

        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "DOUBLE_DOT"

    // $ANTLR start "DOLLAR"
    public final void mDOLLAR() throws RecognitionException {
        try {
            // TestCommand.g:59:28: ( '$' )
            // TestCommand.g:59:32: '$'
            {
                match('$');

            }

        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "DOLLAR"

    // $ANTLR start "PLUS"
    public final void mPLUS() throws RecognitionException {
        try {
            // TestCommand.g:60:28: ( '+' )
            // TestCommand.g:60:32: '+'
            {
                match('+');

            }

        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "PLUS"

    // $ANTLR start "MINUS"
    public final void mMINUS() throws RecognitionException {
        try {
            // TestCommand.g:61:28: ( '-' )
            // TestCommand.g:61:32: '-'
            {
                match('-');

            }

        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "MINUS"

    // $ANTLR start "TILDA"
    public final void mTILDA() throws RecognitionException {
        try {
            // TestCommand.g:62:28: ( '~' )
            // TestCommand.g:62:32: '~'
            {
                match('~');

            }

        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "TILDA"

    // $ANTLR start "UNDERSCORE"
    public final void mUNDERSCORE() throws RecognitionException {
        try {
            // TestCommand.g:63:28: ( '_' )
            // TestCommand.g:63:32: '_'
            {
                match('_');

            }

        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "UNDERSCORE"

    // $ANTLR start "QUOTE"
    public final void mQUOTE() throws RecognitionException {
        try {
            // TestCommand.g:64:28: ( '\\'' )
            // TestCommand.g:64:32: '\\''
            {
                match('\'');

            }

        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "QUOTE"

    // $ANTLR start "DOUBLE_QUOTE"
    public final void mDOUBLE_QUOTE() throws RecognitionException {
        try {
            // TestCommand.g:65:28: ( '\"' )
            // TestCommand.g:65:32: '\"'
            {
                match('\"');

            }

        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "DOUBLE_QUOTE"

    // $ANTLR start "STRING_LITERAL"
    public final void mSTRING_LITERAL() throws RecognitionException {
        try {
            int _type = STRING_LITERAL;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestCommand.g:72:5: ( QUOTE ( '\\\\' QUOTE |~ QUOTE )* QUOTE | DOUBLE_QUOTE ( '\\\\' DOUBLE_QUOTE |~ DOUBLE_QUOTE )* DOUBLE_QUOTE )
            int alt6 = 2;
            int LA6_0 = input.LA(1);

            if ((LA6_0 == '\'')) {
                alt6 = 1;
            } else if ((LA6_0 == '\"')) {
                alt6 = 2;
            } else {
                NoViableAltException nvae = new NoViableAltException("", 6, 0, input);

                throw nvae;

            }
            switch (alt6) {
            case 1:
            // TestCommand.g:72:7: QUOTE ( '\\\\' QUOTE |~ QUOTE )* QUOTE
            {
                mQUOTE();

                // TestCommand.g:72:13: ( '\\\\' QUOTE |~ QUOTE )*
                loop4: do {
                    int alt4 = 3;
                    int LA4_0 = input.LA(1);

                    if ((LA4_0 == '\\')) {
                        int LA4_2 = input.LA(2);

                        if ((LA4_2 == '\'')) {
                            int LA4_4 = input.LA(3);

                            if (((LA4_4 >= '\u0000' && LA4_4 <= '\uFFFF'))) {
                                alt4 = 1;
                            }

                            else {
                                alt4 = 2;
                            }

                        } else if (((LA4_2 >= '\u0000' && LA4_2 <= '&') || (LA4_2 >= '(' && LA4_2 <= '\uFFFF'))) {
                            alt4 = 2;
                        }

                    } else if (((LA4_0 >= '\u0000' && LA4_0 <= '&') || (LA4_0 >= '(' && LA4_0 <= '[')
                            || (LA4_0 >= ']' && LA4_0 <= '\uFFFF'))) {
                        alt4 = 2;
                    }

                    switch (alt4) {
                    case 1:
                    // TestCommand.g:72:15: '\\\\' QUOTE
                    {
                        match('\\');

                        mQUOTE();

                    }
                        break;
                    case 2:
                    // TestCommand.g:72:29: ~ QUOTE
                    {
                        if ((input.LA(1) >= '\u0000' && input.LA(1) <= '\u0011')
                                || (input.LA(1) >= '\u0013' && input.LA(1) <= '\uFFFF')) {
                            input.consume();
                        } else {
                            MismatchedSetException mse = new MismatchedSetException(null, input);
                            recover(mse);
                            throw mse;
                        }

                    }
                        break;

                    default:
                        break loop4;
                    }
                } while (true);

                mQUOTE();

            }
                break;
            case 2:
            // TestCommand.g:73:4: DOUBLE_QUOTE ( '\\\\' DOUBLE_QUOTE |~ DOUBLE_QUOTE )* DOUBLE_QUOTE
            {
                mDOUBLE_QUOTE();

                // TestCommand.g:73:17: ( '\\\\' DOUBLE_QUOTE |~ DOUBLE_QUOTE )*
                loop5: do {
                    int alt5 = 3;
                    int LA5_0 = input.LA(1);

                    if ((LA5_0 == '\\')) {
                        int LA5_2 = input.LA(2);

                        if ((LA5_2 == '\"')) {
                            int LA5_4 = input.LA(3);

                            if (((LA5_4 >= '\u0000' && LA5_4 <= '\uFFFF'))) {
                                alt5 = 1;
                            }

                            else {
                                alt5 = 2;
                            }

                        } else if (((LA5_2 >= '\u0000' && LA5_2 <= '!') || (LA5_2 >= '#' && LA5_2 <= '\uFFFF'))) {
                            alt5 = 2;
                        }

                    } else if (((LA5_0 >= '\u0000' && LA5_0 <= '!') || (LA5_0 >= '#' && LA5_0 <= '[')
                            || (LA5_0 >= ']' && LA5_0 <= '\uFFFF'))) {
                        alt5 = 2;
                    }

                    switch (alt5) {
                    case 1:
                    // TestCommand.g:73:19: '\\\\' DOUBLE_QUOTE
                    {
                        match('\\');

                        mDOUBLE_QUOTE();

                    }
                        break;
                    case 2:
                    // TestCommand.g:73:39: ~ DOUBLE_QUOTE
                    {
                        if ((input.LA(1) >= '\u0000' && input.LA(1) <= '\t')
                                || (input.LA(1) >= '\u000B' && input.LA(1) <= '\uFFFF')) {
                            input.consume();
                        } else {
                            MismatchedSetException mse = new MismatchedSetException(null, input);
                            recover(mse);
                            throw mse;
                        }

                    }
                        break;

                    default:
                        break loop5;
                    }
                } while (true);

                mDOUBLE_QUOTE();

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

    // $ANTLR start "DECIMAL_LITERAL"
    public final void mDECIMAL_LITERAL() throws RecognitionException {
        try {
            int _type = DECIMAL_LITERAL;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestCommand.g:81:5: ( ( ( DOT ( DIGIT )+ ) | ( DIGIT )+ ( DOT ( DIGIT )* )? ) ( ( 'e' | 'E' ) ( PLUS | MINUS )? ( DIGIT )+ )? )
            // TestCommand.g:81:9: ( ( DOT ( DIGIT )+ ) | ( DIGIT )+ ( DOT ( DIGIT )* )? ) ( ( 'e' | 'E' ) ( PLUS | MINUS )? ( DIGIT )+ )?
            {
                // TestCommand.g:81:9: ( ( DOT ( DIGIT )+ ) | ( DIGIT )+ ( DOT ( DIGIT )* )? )
                int alt11 = 2;
                int LA11_0 = input.LA(1);

                if ((LA11_0 == '.')) {
                    alt11 = 1;
                } else if (((LA11_0 >= '0' && LA11_0 <= '9'))) {
                    alt11 = 2;
                } else {
                    NoViableAltException nvae = new NoViableAltException("", 11, 0, input);

                    throw nvae;

                }
                switch (alt11) {
                case 1:
                // TestCommand.g:81:11: ( DOT ( DIGIT )+ )
                {
                    // TestCommand.g:81:11: ( DOT ( DIGIT )+ )
                    // TestCommand.g:81:12: DOT ( DIGIT )+
                    {
                        mDOT();

                        // TestCommand.g:81:16: ( DIGIT )+
                        int cnt7 = 0;
                        loop7: do {
                            int alt7 = 2;
                            int LA7_0 = input.LA(1);

                            if (((LA7_0 >= '0' && LA7_0 <= '9'))) {
                                alt7 = 1;
                            }

                            switch (alt7) {
                            case 1:
                            // TestCommand.g:
                            {
                                if ((input.LA(1) >= '0' && input.LA(1) <= '9')) {
                                    input.consume();
                                } else {
                                    MismatchedSetException mse = new MismatchedSetException(null, input);
                                    recover(mse);
                                    throw mse;
                                }

                            }
                                break;

                            default:
                                if (cnt7 >= 1)
                                    break loop7;
                                EarlyExitException eee = new EarlyExitException(7, input);
                                throw eee;
                            }
                            cnt7++;
                        } while (true);

                    }

                }
                    break;
                case 2:
                // TestCommand.g:82:11: ( DIGIT )+ ( DOT ( DIGIT )* )?
                {
                    // TestCommand.g:82:11: ( DIGIT )+
                    int cnt8 = 0;
                    loop8: do {
                        int alt8 = 2;
                        int LA8_0 = input.LA(1);

                        if (((LA8_0 >= '0' && LA8_0 <= '9'))) {
                            alt8 = 1;
                        }

                        switch (alt8) {
                        case 1:
                        // TestCommand.g:
                        {
                            if ((input.LA(1) >= '0' && input.LA(1) <= '9')) {
                                input.consume();
                            } else {
                                MismatchedSetException mse = new MismatchedSetException(null, input);
                                recover(mse);
                                throw mse;
                            }

                        }
                            break;

                        default:
                            if (cnt8 >= 1)
                                break loop8;
                            EarlyExitException eee = new EarlyExitException(8, input);
                            throw eee;
                        }
                        cnt8++;
                    } while (true);

                    // TestCommand.g:82:22: ( DOT ( DIGIT )* )?
                    int alt10 = 2;
                    int LA10_0 = input.LA(1);

                    if ((LA10_0 == '.')) {
                        alt10 = 1;
                    }
                    switch (alt10) {
                    case 1:
                    // TestCommand.g:82:24: DOT ( DIGIT )*
                    {
                        mDOT();

                        // TestCommand.g:82:28: ( DIGIT )*
                        loop9: do {
                            int alt9 = 2;
                            int LA9_0 = input.LA(1);

                            if (((LA9_0 >= '0' && LA9_0 <= '9'))) {
                                alt9 = 1;
                            }

                            switch (alt9) {
                            case 1:
                            // TestCommand.g:
                            {
                                if ((input.LA(1) >= '0' && input.LA(1) <= '9')) {
                                    input.consume();
                                } else {
                                    MismatchedSetException mse = new MismatchedSetException(null, input);
                                    recover(mse);
                                    throw mse;
                                }

                            }
                                break;

                            default:
                                break loop9;
                            }
                        } while (true);

                    }
                        break;

                    }

                }
                    break;

                }

                // TestCommand.g:83:9: ( ( 'e' | 'E' ) ( PLUS | MINUS )? ( DIGIT )+ )?
                int alt14 = 2;
                int LA14_0 = input.LA(1);

                if ((LA14_0 == 'E' || LA14_0 == 'e')) {
                    alt14 = 1;
                }
                switch (alt14) {
                case 1:
                // TestCommand.g:83:11: ( 'e' | 'E' ) ( PLUS | MINUS )? ( DIGIT )+
                {
                    if (input.LA(1) == 'E' || input.LA(1) == 'e') {
                        input.consume();
                    } else {
                        MismatchedSetException mse = new MismatchedSetException(null, input);
                        recover(mse);
                        throw mse;
                    }

                    // TestCommand.g:83:23: ( PLUS | MINUS )?
                    int alt12 = 2;
                    int LA12_0 = input.LA(1);

                    if ((LA12_0 == '+' || LA12_0 == '-')) {
                        alt12 = 1;
                    }
                    switch (alt12) {
                    case 1:
                    // TestCommand.g:
                    {
                        if (input.LA(1) == '+' || input.LA(1) == '-') {
                            input.consume();
                        } else {
                            MismatchedSetException mse = new MismatchedSetException(null, input);
                            recover(mse);
                            throw mse;
                        }

                    }
                        break;

                    }

                    // TestCommand.g:83:41: ( DIGIT )+
                    int cnt13 = 0;
                    loop13: do {
                        int alt13 = 2;
                        int LA13_0 = input.LA(1);

                        if (((LA13_0 >= '0' && LA13_0 <= '9'))) {
                            alt13 = 1;
                        }

                        switch (alt13) {
                        case 1:
                        // TestCommand.g:
                        {
                            if ((input.LA(1) >= '0' && input.LA(1) <= '9')) {
                                input.consume();
                            } else {
                                MismatchedSetException mse = new MismatchedSetException(null, input);
                                recover(mse);
                                throw mse;
                            }

                        }
                            break;

                        default:
                            if (cnt13 >= 1)
                                break loop13;
                            EarlyExitException eee = new EarlyExitException(13, input);
                            throw eee;
                        }
                        cnt13++;
                    } while (true);

                }
                    break;

                }

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "DECIMAL_LITERAL"

    // $ANTLR start "COMMENT"
    public final void mCOMMENT() throws RecognitionException {
        try {
            int _type = COMMENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestCommand.g:91:5: ( '#' (~ ( '\\n' | '\\r' ) )* ( '\\r' )? ( '\\n' | EOF ) )
            // TestCommand.g:91:9: '#' (~ ( '\\n' | '\\r' ) )* ( '\\r' )? ( '\\n' | EOF )
            {
                match('#');

                // TestCommand.g:91:13: (~ ( '\\n' | '\\r' ) )*
                loop15: do {
                    int alt15 = 2;
                    int LA15_0 = input.LA(1);

                    if (((LA15_0 >= '\u0000' && LA15_0 <= '\t') || (LA15_0 >= '\u000B' && LA15_0 <= '\f')
                            || (LA15_0 >= '\u000E' && LA15_0 <= '\uFFFF'))) {
                        alt15 = 1;
                    }

                    switch (alt15) {
                    case 1:
                    // TestCommand.g:
                    {
                        if ((input.LA(1) >= '\u0000' && input.LA(1) <= '\t')
                                || (input.LA(1) >= '\u000B' && input.LA(1) <= '\f')
                                || (input.LA(1) >= '\u000E' && input.LA(1) <= '\uFFFF')) {
                            input.consume();
                        } else {
                            MismatchedSetException mse = new MismatchedSetException(null, input);
                            recover(mse);
                            throw mse;
                        }

                    }
                        break;

                    default:
                        break loop15;
                    }
                } while (true);

                // TestCommand.g:91:27: ( '\\r' )?
                int alt16 = 2;
                int LA16_0 = input.LA(1);

                if ((LA16_0 == '\r')) {
                    alt16 = 1;
                }
                switch (alt16) {
                case 1:
                // TestCommand.g:91:27: '\\r'
                {
                    match('\r');

                }
                    break;

                }

                // TestCommand.g:91:33: ( '\\n' | EOF )
                int alt17 = 2;
                int LA17_0 = input.LA(1);

                if ((LA17_0 == '\n')) {
                    alt17 = 1;
                } else {
                    alt17 = 2;
                }
                switch (alt17) {
                case 1:
                // TestCommand.g:91:34: '\\n'
                {
                    match('\n');

                }
                    break;
                case 2:
                // TestCommand.g:91:41: EOF
                {
                    match(EOF);

                }
                    break;

                }

                //_channel=HIDDEN;
                skip();

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "COMMENT"

    // $ANTLR start "WS"
    public final void mWS() throws RecognitionException {
        try {
            int _type = WS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // TestCommand.g:103:2: ( ( ' ' | '\\t' | '\\\\' '\\r' | '\\\\' '\\n' )+ )
            // TestCommand.g:103:4: ( ' ' | '\\t' | '\\\\' '\\r' | '\\\\' '\\n' )+
            {
                // TestCommand.g:103:4: ( ' ' | '\\t' | '\\\\' '\\r' | '\\\\' '\\n' )+
                int cnt18 = 0;
                loop18: do {
                    int alt18 = 5;
                    switch (input.LA(1)) {
                    case ' ': {
                        alt18 = 1;
                    }
                        break;
                    case '\t': {
                        alt18 = 2;
                    }
                        break;
                    case '\\': {
                        int LA18_4 = input.LA(2);

                        if ((LA18_4 == '\r')) {
                            alt18 = 3;
                        } else if ((LA18_4 == '\n')) {
                            alt18 = 4;
                        }

                    }
                        break;

                    }

                    switch (alt18) {
                    case 1:
                    // TestCommand.g:103:5: ' '
                    {
                        match(' ');

                    }
                        break;
                    case 2:
                    // TestCommand.g:103:9: '\\t'
                    {
                        match('\t');

                    }
                        break;
                    case 3:
                    // TestCommand.g:103:14: '\\\\' '\\r'
                    {
                        match('\\');

                        match('\r');

                    }
                        break;
                    case 4:
                    // TestCommand.g:103:24: '\\\\' '\\n'
                    {
                        match('\\');

                        match('\n');

                    }
                        break;

                    default:
                        if (cnt18 >= 1)
                            break loop18;
                        EarlyExitException eee = new EarlyExitException(18, input);
                        throw eee;
                    }
                    cnt18++;
                } while (true);

            }

            state.type = _type;
            state.channel = _channel;
        } finally {
            // do for sure before leaving
        }
    }
    // $ANTLR end "WS"

    public void mTokens() throws RecognitionException {
        // TestCommand.g:1:8: ( PATH | NAME | Semicolon | GT | VERTICAL_BAR | LEFT_PAREN | RIGHT_PAREN | STRING_LITERAL | DECIMAL_LITERAL | COMMENT | WS )
        int alt19 = 11;
        alt19 = dfa19.predict(input);
        switch (alt19) {
        case 1:
        // TestCommand.g:1:10: PATH
        {
            mPATH();

        }
            break;
        case 2:
        // TestCommand.g:1:15: NAME
        {
            mNAME();

        }
            break;
        case 3:
        // TestCommand.g:1:20: Semicolon
        {
            mSemicolon();

        }
            break;
        case 4:
        // TestCommand.g:1:30: GT
        {
            mGT();

        }
            break;
        case 5:
        // TestCommand.g:1:33: VERTICAL_BAR
        {
            mVERTICAL_BAR();

        }
            break;
        case 6:
        // TestCommand.g:1:46: LEFT_PAREN
        {
            mLEFT_PAREN();

        }
            break;
        case 7:
        // TestCommand.g:1:57: RIGHT_PAREN
        {
            mRIGHT_PAREN();

        }
            break;
        case 8:
        // TestCommand.g:1:69: STRING_LITERAL
        {
            mSTRING_LITERAL();

        }
            break;
        case 9:
        // TestCommand.g:1:84: DECIMAL_LITERAL
        {
            mDECIMAL_LITERAL();

        }
            break;
        case 10:
        // TestCommand.g:1:100: COMMENT
        {
            mCOMMENT();

        }
            break;
        case 11:
        // TestCommand.g:1:108: WS
        {
            mWS();

        }
            break;

        }

    }

    protected DFA19 dfa19 = new DFA19(this);
    static final String DFA19_eotS = "\2\uffff\2\12\11\uffff\7\12\1\uffff";
    static final String DFA19_eofS = "\25\uffff";
    static final String DFA19_minS = "\1\11\1\uffff\2\56\11\uffff\1\57\2\60\1\53\3\60\1\uffff";
    static final String DFA19_maxS = "\1\176\1\uffff\1\71\1\145\11\uffff\1\57\2\145\1\71\1\145\2\71\1" + "\uffff";
    static final String DFA19_acceptS = "\1\uffff\1\1\2\uffff\1\3\1\4\1\5\1\6\1\7\1\10\1\2\1\12\1\13\7\uffff" + "\1\11";
    static final String DFA19_specialS = "\25\uffff}>";
    static final String[] DFA19_transitionS = {
            "\1\14\26\uffff\1\14\1\uffff\1\11\1\13\1\12\2\uffff\1\11\1\7"
                    + "\1\10\3\uffff\1\12\1\2\1\1\12\3\1\uffff\1\4\2\uffff\1\5\2\uffff"
                    + "\32\12\1\uffff\1\14\2\uffff\1\12\1\uffff\32\12\1\uffff\1\6\1" + "\uffff\1\1",
            "", "\1\15\1\1\12\16", "\1\17\1\uffff\12\3\13\uffff\1\20\37\uffff\1\20", "", "", "", "", "", "", "", "", "",
            "\1\1", "\12\16\13\uffff\1\20\37\uffff\1\20", "\12\21\13\uffff\1\20\37\uffff\1\20",
            "\1\24\1\uffff\1\22\2\uffff\12\23", "\12\21\13\uffff\1\20\37\uffff\1\20", "\12\23", "\12\23", "" };

    static final short[] DFA19_eot = DFA.unpackEncodedString(DFA19_eotS);
    static final short[] DFA19_eof = DFA.unpackEncodedString(DFA19_eofS);
    static final char[] DFA19_min = DFA.unpackEncodedStringToUnsignedChars(DFA19_minS);
    static final char[] DFA19_max = DFA.unpackEncodedStringToUnsignedChars(DFA19_maxS);
    static final short[] DFA19_accept = DFA.unpackEncodedString(DFA19_acceptS);
    static final short[] DFA19_special = DFA.unpackEncodedString(DFA19_specialS);
    static final short[][] DFA19_transition;

    static {
        int numStates = DFA19_transitionS.length;
        DFA19_transition = new short[numStates][];
        for (int i = 0; i < numStates; i++) {
            DFA19_transition[i] = DFA.unpackEncodedString(DFA19_transitionS[i]);
        }
    }

    class DFA19 extends DFA {
        public DFA19(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 19;
            this.eot = DFA19_eot;
            this.eof = DFA19_eof;
            this.min = DFA19_min;
            this.max = DFA19_max;
            this.accept = DFA19_accept;
            this.special = DFA19_special;
            this.transition = DFA19_transition;
        }

        public String getDescription() {
            return "1:1: Tokens : ( PATH | NAME | Semicolon | GT | VERTICAL_BAR | LEFT_PAREN | RIGHT_PAREN | STRING_LITERAL | DECIMAL_LITERAL | COMMENT | WS );";
        }
    }
}
