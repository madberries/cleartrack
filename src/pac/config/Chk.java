package pac.config;

import static pac.config.ConfigFileTokens.ARRAY_TKN;
import static pac.config.ConfigFileTokens.BLACK_ENCODINGS_ACTION_TKN;
import static pac.config.ConfigFileTokens.BLACK_ENCODINGS_TKN;
import static pac.config.ConfigFileTokens.BLACK_FAILURE_ACTION_TKN;
import static pac.config.ConfigFileTokens.CHECK_CRITERIA_TKN;
import static pac.config.ConfigFileTokens.CHECK_TKN;
import static pac.config.ConfigFileTokens.DESC_TKN;
import static pac.config.ConfigFileTokens.END_TKN;
import static pac.config.ConfigFileTokens.INTERNAL_BLACK_TKN;
import static pac.config.ConfigFileTokens.INTERNAL_WHITE_TKN;
import static pac.config.ConfigFileTokens.LOG_FILE_TKN;
import static pac.config.ConfigFileTokens.PREFIX_BLACK_TKN;
import static pac.config.ConfigFileTokens.PREFIX_WHITE_TKN;
import static pac.config.ConfigFileTokens.QSTRING_TKN;
import static pac.config.ConfigFileTokens.SHELL_LIST_TKN;
import static pac.config.ConfigFileTokens.SUFFIX_BLACK_TKN;
import static pac.config.ConfigFileTokens.SUFFIX_WHITE_TKN;
import static pac.config.ConfigFileTokens.TOKENIZER_TKN;
import static pac.config.ConfigFileTokens.TOKEN_MATCH_TKN;
import static pac.config.ConfigFileTokens.TOKEN_NOMATCH_TKN;
import static pac.config.ConfigFileTokens.TOKEN_TKN;
import static pac.config.ConfigFileTokens.TOKEN_WHITESPACE_TKN;
import static pac.config.ConfigFileTokens.TRAVERSAL_COUNT_TKN;
import static pac.config.ConfigFileTokens.VAR_VALUE_BLACK_TKN;
import static pac.config.ConfigFileTokens.VAR_VALUE_WHITE_TKN;
import static pac.config.ConfigFileTokens.WHITE_ENCODINGS_ACTION_TKN;
import static pac.config.ConfigFileTokens.WHITE_ENCODINGS_TKN;
import static pac.config.ConfigFileTokens.WHITE_FAILURE_ACTION_TKN;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import pac.config.Tokenizer.Tokenized;
import pac.util.StringRegionIterator;
import pac.util.StringRegionIterator.StringRegion;
import pac.util.TaintUtils;
import pac.util.TaintValues;

// buildChk_parseLine
// build_black_white_failure
// check_legal_array
// build_qstring
// compilePattern
// findEnvVarFromName
// build_env_var
// build_token_whitespace
// isWhitelisted2
// isWhitelisted
// isBlacklisted2
// isBlacklisted
// blacklistReplace2
// blacklistReplace
// isSectionPrefix
// isSectionSuffix
// tokenizeAndRunWhiteBlack
// runWhiteBlackSections
// tokenWhiteSpaceMsg
// runTokenWhiteSpace
// replace_nth_token
// runFromChk
// doQString
// whitePatternMatches
// blackPatternMatches
// runTrustedStreamFiles

// runEnvChk
// debug_verify
// dump

public class Chk {

    // The fields in this Chk class are filled from a single check entry when reading the config file

    final String check_name;
    final int cweNumber;

    private Pattern check_criteria;

    private int traversal_count_action = -1;
    private Constructor<?> traversal_count_exception_constructor;
    private String traversal_count_replace_char;

    private Pattern prefix_whitelist;
    private Pattern internal_whitelist;
    private Pattern suffix_whitelist;
    private int white_failure_action;

    private Pattern prefix_blacklist;
    private Pattern internal_blacklist;
    private Pattern suffix_blacklist;
    private int black_failure_action;
    private String black_replace_char;

    private Tokenizer tokenizer;
    int matchOffset, nomatchOffset;
    String matchRegex, nomatchRegex;

    private Constructor<?> white_exception_constructor;
    private Constructor<?> black_exception_constructor;

    private int encodings_whitelist;
    private int encodings_blacklist;
    private int encodings_whitelist_action;
    private int encodings_blacklist_action;
    private Constructor<?> encodings_whitelist_exception_constructor;
    private Constructor<?> encodings_blacklist_exception_constructor;

    private Constructor<?> token_whitespace_exception_constructor;
    private String token_whitespace_replace_char;
    private int token_whitespace_action;

    private Constructor<?> Q_exception_constructor;
    private int Q_action;

    // constructor
    Chk(final String name) {
        int idx = name.indexOf('{');
        if (idx > 0) {
            check_name = name.substring(0, idx);
            cweNumber = Integer.parseInt(name.substring(idx + 1, name.length() - 1).split("\\-")[1]);
        } else {
            check_name = name;
            cweNumber = 0;
        }
    }

    private class EnvVar {
        String varName;
        Pattern whitelist;
        Pattern blacklist;

        int white_failure_action;
        int black_failure_action;

        String black_replace_char;

        Constructor<?> env_white_exception_constructor;
        Constructor<?> env_black_exception_constructor;
    }

    private enum Encoding {
        XPATH_TYPE(TaintValues.XPATH_TYPE), CSS_TYPE(TaintValues.CSS_TYPE), DN_TYPE(TaintValues.DN_TYPE), HTML_TYPE(
                TaintValues.HTML_TYPE), JAVASCRIPT_TYPE(TaintValues.JAVASCRIPT_TYPE), LDAP_TYPE(
                        TaintValues.LDAP_TYPE), OS_TYPE(TaintValues.OS_TYPE), SQL_TYPE(TaintValues.SQL_TYPE), URL_TYPE(
                                TaintValues.URL_TYPE), VBSCRIPT_TYPE(
                                        TaintValues.VBSCRIPT_TYPE), XML_TYPE(TaintValues.XML_TYPE);

        private int value;

        private Encoding(int value) {
            this.value = value;
        }

        public int getMask() {
            return value;
        }
    }

    private Vector<EnvVar> envVec; // One EnvVar entry for each environment variable
    Vector<String> directivesList; // Elems are directive strings from config file between check <name>
                                   //                                                         and
                                   //                                                      end check

    /**
    * Called for each config file line that is between "check <name>" and "end check"
    * @param knownCheckNames - Contains every check name having a definition in the config file.
    *                          The caller has already read once through the config file. On reading a "check <check_name>"
    *                          line the <check_name> is added to knownCheckNames.
    */
    public void buildChk_parseLine(final String line, final Vector<String> knownCheckNames, AbstractConfig config)
            throws MsgException {

        if (directivesList == null) {
            directivesList = new Vector<String>();
        }

        final String[] lineRay = line.split("\\s+"); // PatternSyntaxException

        if (lineRay[0].equals(PREFIX_WHITE_TKN)) {
            prefix_whitelist = compilePattern(lineRay, 1); // MsgException
            directivesList.addElement(PREFIX_WHITE_TKN);
        } else if (lineRay[0].equals(INTERNAL_WHITE_TKN)) {
            internal_whitelist = compilePattern(lineRay, 1); // MsgException
            directivesList.addElement(INTERNAL_WHITE_TKN);
        } else if (lineRay[0].equals(SUFFIX_WHITE_TKN)) {
            suffix_whitelist = compilePattern(lineRay, 1); // MsgException
            directivesList.addElement(SUFFIX_WHITE_TKN);
        } else if (lineRay[0].equals(WHITE_FAILURE_ACTION_TKN)) {
            // line looks like "white_failure_action <action> <replace_char/exception_subtype>
            // o set white_failure_action
            // o Possibly set global white_exception_constructor
            // o              white_replace_char
            build_black_white_failure(true, lineRay); // MsgException
            directivesList.addElement(WHITE_FAILURE_ACTION_TKN);
        } else if (lineRay[0].equals(PREFIX_BLACK_TKN)) {
            prefix_blacklist = compilePattern(lineRay, 1); // MsgException
            directivesList.addElement(PREFIX_BLACK_TKN);
        } else if (lineRay[0].equals(INTERNAL_BLACK_TKN)) {
            internal_blacklist = compilePattern(lineRay, 1); // MsgException
            directivesList.addElement(INTERNAL_BLACK_TKN);
        } else if (lineRay[0].equals(SUFFIX_BLACK_TKN)) {
            suffix_blacklist = compilePattern(lineRay, 1); // MsgException
            directivesList.addElement(SUFFIX_BLACK_TKN);
        } else if (lineRay[0].equals(BLACK_FAILURE_ACTION_TKN)) {
            // line looks like "black_failure_action <action> [<replace_char/exception_subtype>]
            // o set black_failure_action
            // o Possibly set global black_exception_constructor
            // o              black_replace_char
            build_black_white_failure(false, lineRay); // MsgException
            directivesList.addElement(BLACK_FAILURE_ACTION_TKN);
            // looks like "var_value_white <env_name> <regExp> <action>"
        } else if (lineRay[0].equals(VAR_VALUE_WHITE_TKN)) {
            build_env_var(line); // MsgException
            directivesList.addElement(line);
        } else if (lineRay[0].equals(VAR_VALUE_BLACK_TKN)) {
            build_env_var(line); // MsgException
            directivesList.addElement(line);
        } else if (lineRay[0].equals(TOKEN_TKN)) {
            directivesList.addElement(line); // Token 2 names a check
        } else if (lineRay[0].equals(QSTRING_TKN)) {
            // line must look like: "qstring <char-to-be-replace> <replace_str> action
            build_qstring(lineRay); // MsgException
            directivesList.addElement(lineRay[0]);
        } else if (lineRay[0].equals(TRAVERSAL_COUNT_TKN)) {
            build_traversal_count(lineRay);
            directivesList.addElement(line);
        } else if (lineRay[0].equals(ARRAY_TKN)) {
            if (knownCheckNames != null)
                check_legal_array(line, knownCheckNames); // MsgException
            directivesList.addElement(line);
        } else if (lineRay[0].equals(DESC_TKN) || lineRay[0].equals(CHECK_TKN) || lineRay[0].equals(LOG_FILE_TKN)
                || lineRay[0].equals(SHELL_LIST_TKN)) {
            throw new MsgException(
                    "Illegal key word token read between \"" + CHECK_TKN + " " + check_name + "\"" + " and " + END_TKN);
        } else if (lineRay[0].equals(TOKEN_WHITESPACE_TKN)) {
            build_token_whitespace(lineRay); // MsgException
            directivesList.addElement(line);
        } else if (lineRay[0].equals(CHECK_CRITERIA_TKN)) {
            check_criteria = compilePattern(lineRay, 1); // MsgException
            //irectivesList.addElement(CHECK_CRITERIA_TKN);
        } else if (lineRay[0].equals(TOKENIZER_TKN)) {
            tokenizer = config.getTokenizer(lineRay[1]);
        } else if (lineRay[0].equals(TOKEN_MATCH_TKN)) {
            matchOffset = atoi(lineRay[1]);
            matchRegex = lineRay[2];
        } else if (lineRay[0].equals(TOKEN_NOMATCH_TKN)) {
            nomatchOffset = atoi(lineRay[1]);
            nomatchRegex = lineRay[2];
        } else if (lineRay[0].equals(WHITE_ENCODINGS_TKN)) {
            encodings_whitelist = compileEncodings(lineRay, 1);
            directivesList.addElement(line);
        } else if (lineRay[0].equals(BLACK_ENCODINGS_TKN)) {
            encodings_blacklist = compileEncodings(lineRay, 1);
            directivesList.addElement(line);
        } else if (lineRay[0].equals(WHITE_ENCODINGS_ACTION_TKN)) {
            build_encoding_action(true, lineRay); // MsgException
        } else if (lineRay[0].equals(BLACK_ENCODINGS_ACTION_TKN)) {
            build_encoding_action(false, lineRay); // MsgException
        } else {
            // First token is not a keyword.
            // Check that the first token is a known check name
            if (knownCheckNames != null && !knownCheckNames.contains(lineRay[0])) {
                throw new MsgException("Unknown token: " + lineRay[0]);
            }

            directivesList.addElement(line); // Token 0 names another check
        }
    }

    // FIXME this will only work on digits (or negative digits) 
    private int atoi(String string) {
        char first = string.charAt(0);
        if (first == '-') {
            return -(string.charAt(1) - '0');
        }
        return string.charAt(0) - '0';
    }

    /**
    * <pre>
    * parse action from line
    * set this.black_failure_action
    * if action is exception,
    *    o parse out the execption_subtype string
    *    o convert it to Constructor
    *    o set either white_exception_constructor or black_exception_constructor
    * if action is replace, can called_for_white is true, parse out the replace char
    *   set this.black_replace_char
    * @param called_for_white true if processing WHITE_FAILURE_ACTION_TKN
    *                         false if processing BLACK_FAILURE_ACTION_TKN
    * @param lineRay  lineRay[0] white/black_failure_action
    *                 lineRay[1] action
    *                 lineRay[2] replace_char or exception_subtype
    */
    private void build_black_white_failure(final boolean called_for_white, final String[] lineRay) throws MsgException {

        if (lineRay.length < 2) {
            throw new MsgException(lineRay[0] + " required an action to be specified.");
        }
        // Return a legal int action value
        final int action = RunChecks.actionStringToInt(lineRay[1]); // MsgException

        // Insure that action is a legal string value
        // Insure that action is legal for the white list or black list failure
        if (action == RunChecks.REPLACE_ACTION && called_for_white) {
            throw new MsgException(
                    "Is illegal to set action for " + WHITE_FAILURE_ACTION_TKN + " to " + RunChecks.REPLACE_ACTION_TKN);
        }

        if (called_for_white) {
            white_failure_action = action;
        } else {
            black_failure_action = action;
        }

        if ((action == RunChecks.EXCEPTION_ACTION) || (action == RunChecks.REPLACE_ACTION)) {
            if (lineRay.length < 3) {
                throw new MsgException(
                        lineRay[0] + " specifies action: " + RunChecks.actionIntToString(action) + ".\n" + // MsgException
                                "A third parameter specifying the "
                                + (action == RunChecks.EXCEPTION_ACTION ? "exception subclass" : "replace character")
                                + " is missing.");
            }

            if (action == RunChecks.EXCEPTION_ACTION) {
                final Constructor<?> constructor = Utils.buildExceptionConstructor(lineRay[2]); // MsgException
                if (called_for_white) {
                    white_exception_constructor = constructor;
                } else {
                    black_exception_constructor = constructor;
                }
            } else {
                final String replace_char = lineRay[2];
                if (replace_char.length() > 1) {
                    throw new MsgException("Illegal replace-character follows " + lineRay[0] + " " + lineRay[1] + "\n"
                            + "Must be a single character.");
                }

                black_replace_char = replace_char;
            }
            /*
              final String tkn = Utils.getTokenAtIndex(line, 2); // MsgException
              black_replace_char = tkn.trim();
              if (black_replace_char.length() > 1) {
                 throw new MsgException("Illegal character entered for " + RunChecks.BLACK_REPLACE_CHAR_TKN + " " + black_replace_char + "\n" +
                                        "Must be a single character.");
              }
            }
            */
        }
    }

    private void build_encoding_action(final boolean called_for_white, final String[] lineRay) throws MsgException {
        if (lineRay.length < 2) {
            throw new MsgException(lineRay[0] + " required an action to be specified.");
        }
        // Return a legal int action value
        final int action = RunChecks.actionStringToInt(lineRay[1]); // MsgException

        if (called_for_white) {
            encodings_whitelist_action = action;
        } else {
            encodings_blacklist_action = action;
        }

        if (action == RunChecks.EXCEPTION_ACTION) {
            if (lineRay.length < 3) {
                throw new MsgException(
                        lineRay[0] + " specifies action: " + RunChecks.actionIntToString(action) + ".\n" + // MsgException
                                "A third parameter specifying the exception subclass is missing.");
            }

            final Constructor<?> constructor = Utils.buildExceptionConstructor(lineRay[2]); // MsgException
            if (called_for_white) {
                encodings_whitelist_exception_constructor = constructor;
            } else {
                encodings_blacklist_exception_constructor = constructor;
            }
        } else if (action == RunChecks.REPLACE_ACTION) {
            /* TODO: would like to implement this check by undoing the improper
             * encoding and then properly encoding it.  It doesn't appear that
             * ESAPI supports decoding of all types.  So to properly implement
             * this I would have to keep a HashMap of all encoded strings to the
             * String before it was encoded.
             */
            throw new MsgException(lineRay[0] + " does not support replacement...yet!");
        }
    }

    // Check that line looks like "array <num> <check_name>"
    private static void check_legal_array(final String line, Vector<String> knownCheckNames) throws MsgException {

        final String MSG = "Illegal entry: Expected \"array <integer> <check_name>\".\n";
        String[] tokens = line.split("\\s+"); // PatternSyntaxException

        try {
            if (tokens.length != 3) {
                throw new MsgException(
                        MSG + line + "\nExpected three token.\n" + "There are " + tokens.length + " tokens.");
            }

            // call parseInt only to test that tokens[1] is a legal integer
            Integer.parseInt(tokens[1]); // NumberFormatException
            if (!knownCheckNames.contains(tokens[2])) {
                throw new MsgException(
                        MSG + "Where check_name must be a check name defined in the configuration file.\n" + tokens[2]
                                + " is not an existing check name.");
            }

        } catch (NumberFormatException ex) {
            throw new MsgException(MSG + line + "Illegal array number read: " + tokens[1] + "\n" + ex.getMessage());
        }
    }

    // Check if config file qstring line is legal
    // The tokenized line should have two or three tokens: "qstring <action> [<RuntimeException>]"
    // Set globals Q_action
    //             Q_exception_constructor
    private void build_qstring(final String[] lineRay) throws MsgException {

        try {
            if (tokenizer == null) {
                throw new MsgException("Check \"" + check_name + "\" must reference a tokenizer to perform a qstring.");
            }

            if (lineRay.length < 2) {
                throw new MsgException("Following \"" + lineRay[0] + "\" " + "expected one or two tokens.\n"
                        + "There are " + lineRay.length + " tokens.");
            }

            // Return a legal int action value
            Q_action = RunChecks.actionStringToInt(lineRay[1]); // MsgException

        } catch (MsgException ex) {
            throw new MsgException("Unable to determine action for " + lineRay[0]);
        }

        if (Q_action == RunChecks.EXCEPTION_ACTION) {
            if (lineRay.length < 3) {
                throw new MsgException("The action for " + lineRay[0] + " is " + RunChecks.actionIntToString(Q_action) + // MsgException
                        ".\nExpected a subclass of RuntimeException to follow "
                        + RunChecks.actionIntToString(Q_action));
            }

            Q_exception_constructor = Utils.buildExceptionConstructor(lineRay[2]); // MsgException
        }
    }

    private void build_traversal_count(final String[] lineRay) throws MsgException {

        try {
            if (lineRay.length < 2) {
                throw new MsgException("Following \"" + lineRay[0] + "\" " + "expected one or two tokens.\n"
                        + "There are " + lineRay.length + " tokens.");
            }

            // Return a legal int action value
            traversal_count_action = RunChecks.actionStringToInt(lineRay[1]); // MsgException

        } catch (MsgException ex) {
            throw new MsgException("Unable to determine action for " + lineRay[0]);
        }

        if (traversal_count_action == RunChecks.EXCEPTION_ACTION) {
            if (lineRay.length < 3) {
                throw new MsgException(
                        "The action for " + lineRay[0] + " is " + RunChecks.actionIntToString(traversal_count_action) + // MsgException
                                ".\nExpected a subclass of RuntimeException to follow "
                                + RunChecks.actionIntToString(traversal_count_action));
            }

            traversal_count_exception_constructor = Utils.buildExceptionConstructor(lineRay[2]); // MsgException
        } else if (traversal_count_action == RunChecks.REPLACE_ACTION) {
            if (lineRay.length < 3) {
                throw new MsgException("The action for " + lineRay[0] + " is " + lineRay[1] + ".\n"
                        + "This action requires that a replacement character be specified.");
            }
            traversal_count_replace_char = lineRay[2];
        }
    }

    // Note: For charTaint the returned pattern in compiled always with an unmapped regExp
    private Pattern compilePattern(final String[] lineRay, final int tokenIndex) throws MsgException {
        try {
            if (lineRay.length - 1 < tokenIndex) {
                throw new MsgException("Unable to access token " + tokenIndex + " from line in configuration file.");
            }

            final String regExp = lineRay[tokenIndex];
            final Pattern pattern = Pattern.compile(regExp); // PatternSyntaxException
            return pattern;

        } catch (PatternSyntaxException ex) {
            throw new MsgException("Bad regular expression read in configuration file.\n" + ex.getMessage());
        }
    }

    private int compileEncodings(final String[] lineRay, final int tokenIndex) throws MsgException {
        if (lineRay.length - 1 < tokenIndex) {
            throw new MsgException("Unable to access token " + tokenIndex + " from line in configuration file.");
        }

        List<String> badEncodingTypes = new LinkedList<String>();

        int encodings = 0;
        for (int i = tokenIndex; i < lineRay.length; i++) {
            try {
                // TODO this should be implemented without using the old enum
                encodings |= Enum.valueOf(Encoding.class, lineRay[i] + "_TYPE").getMask();
            } catch (IllegalArgumentException ex) {
                badEncodingTypes.add(lineRay[i]);
            }
        }

        if (!badEncodingTypes.isEmpty())
            throw new MsgException("No encodings exist for " + badEncodingTypes + ".");

        return encodings;
    }

    public EnvVar findEnvVarFromName(final String name) {
        EnvVar found = null;
        if (envVec != null) {
            for (int i = 0; found == null && i < envVec.size(); i++) {
                final EnvVar envVar = envVec.elementAt(i);
                if (name.equals(envVar.varName)) {
                    found = envVar;
                }
            }
        }

        return found;
    }

    private void build_env_var(final String line) throws MsgException {

        final String[] ray = line.split("\\s+"); // ray[0] var_value_white or var_value_black
                                                 // ray[1] env var name
                                                 // ray[2] regular expression
                                                 // ray[3] action
                                                 // ray[4] replacement-char or exception-subclass
                                                 // PatternSyntaxExcetpion

        final String MSG = "Illegal entry: Expected " + ray[0] + " <env_var_name> <reg_exp> <action>\n";

        try {
            if (ray.length < 4) {
                throw new MsgException(MSG + "Expected minimum of 4 tokens. There are " + ray.length + " tokens.");
            }

            if (envVec == null) {
                envVec = new Vector<EnvVar>();
            }

            boolean envVarIsNew;
            EnvVar envVar = findEnvVarFromName(ray[1]);
            if (envVar == null) {
                envVar = new EnvVar();
                envVar.varName = ray[1];
                envVarIsNew = true;
            } else {
                envVarIsNew = false;
            }

            final Pattern pattern = Pattern.compile(ray[2]);

            if (VAR_VALUE_WHITE_TKN.equals(ray[0])) {
                envVar.whitelist = pattern;
                envVar.white_failure_action = RunChecks.actionStringToInt(ray[3]);
                if (envVar.white_failure_action == RunChecks.REPLACE_ACTION) {
                    throw new MsgException(ray[3] + " is an illegal action for " + ray[0]);
                }

                if (envVar.white_failure_action == RunChecks.EXCEPTION_ACTION) {

                    if (ray.length < 5) {
                        throw new MsgException("The action for " + ray[0] + " is " + ray[3]
                                + ".\nThis action requires that a subclass of Excpetion be specified.");
                    }

                    envVar.env_white_exception_constructor = Utils.buildExceptionConstructor(ray[4]); // MsgExcpetion
                }

            } else if (VAR_VALUE_BLACK_TKN.equals(ray[0])) {
                envVar.blacklist = pattern;
                envVar.black_failure_action = RunChecks.actionStringToInt(ray[3]); // MsgException
                if ((envVar.black_failure_action == RunChecks.REPLACE_ACTION)
                        || (envVar.black_failure_action == RunChecks.EXCEPTION_ACTION)) {
                    if (ray.length < 5) {
                        throw new MsgException(
                                "The action for " + ray[0] + " is " + ray[3] + ".\n" + "This action requires that a "
                                        + (envVar.black_failure_action == RunChecks.REPLACE_ACTION
                                                ? "replacement character"
                                                : "RuntimeException subclass")
                                        + " be specified.\n");
                    }

                    if (envVar.black_failure_action == RunChecks.REPLACE_ACTION) {
                        envVar.black_replace_char = ray[4];
                        envVar.black_replace_char = TaintUtils.taint(envVar.black_replace_char);
                    } else { // action is exception
                        envVar.env_black_exception_constructor = Utils.buildExceptionConstructor(ray[4]); // MsgException
                    }
                }

            } else {
                throw new MsgException("Keyword error: expected " + VAR_VALUE_WHITE_TKN + " or " + VAR_VALUE_BLACK_TKN);
            }

            if (envVarIsNew) {
                envVec.addElement(envVar);
            }

        } catch (NumberFormatException ex) {
            throw new MsgException(MSG + "Illegal action read: " + ray[3] + ".");
        }
    }

    /**
     * Called when reading through config file in "check" section and have read a line that starts with "token_whitespace"
     *
     * This method sets token_whitespace_action
     *      sets token_whitespace_exeption_constructor - if action is exception
     *      sets token_whitespace_replace_char - if action is replace
     */
    private void build_token_whitespace(final String[] lineRay) throws MsgException {
        if (lineRay.length < 2) {
            throw new MsgException(lineRay[0] + " requires an action. No action specified.");
        }

        token_whitespace_action = RunChecks.actionStringToInt(lineRay[1]); // MsgException

        if (token_whitespace_action == RunChecks.EXCEPTION_ACTION) {
            if (lineRay.length < 3) {
                throw new MsgException("The action for " + lineRay[0] + " is " + lineRay[1] + ".\n"
                        + "This action requires that a subclass of RuntimeException be specified.");
            }
            token_whitespace_exception_constructor = Utils.buildExceptionConstructor(lineRay[2]);

        } else if (token_whitespace_action == RunChecks.REPLACE_ACTION) {
            if (lineRay.length < 3) {
                throw new MsgException("The action for " + lineRay[0] + " is " + lineRay[1] + ".\n"
                        + "This action requires that a replacement character be specified.");
            }
            token_whitespace_replace_char = lineRay[2];
        }
    }

    // Called to process a single env var entry.
    // processMe looks like: "<var_name>=<value>"
    private String runEnvChk(final String processMe, final NotifyMsg notifyMsg) throws MsgException, IOException {

        String newProcessMe = processMe;

        final String[] processMeTkns = TaintUtils.split(processMe, "="); // PatternSyntaxException
        final String envName = processMeTkns[0];

        final EnvVar envVar = findEnvVarFromName(envName);
        if (envVar != null) {
            boolean white_failure = false;

            // Process for whitelist failure
            final String envValue = processMeTkns[1];
            if (envVar.whitelist != null) {
                white_failure = whitePatternMatches(envVar.whitelist, envValue);
                if (white_failure) {
                    notifyMsg.append("check ");
                    notifyMsg.append(check_name);
                    notifyMsg.append(": Environment variable ");
                    notifyMsg.append(envName);
                    notifyMsg.append(" has value ");
                    notifyMsg.append(processMeTkns[1]);
                    notifyMsg.append(" which does not match whitelist ");
                    notifyMsg.append(envVar.whitelist.toString());
                    notifyMsg.append("\n");
                    notifyMsg.setAction(envVar.white_failure_action);

                    if (envVar.white_failure_action == RunChecks.EXCEPTION_ACTION
                            || envVar.white_failure_action == RunChecks.TERMINATE_ACTION) {
                        notifyMsg.prepareForExceptionOrTerminate(envVar.env_white_exception_constructor,
                                                                 envVar.white_failure_action);
                        Notify.notifyAndRespond(notifyMsg); // IOException
                    }
                }
            }

            // Process for blacklist failure
            if (!white_failure && envVar.blacklist != null) {
                final boolean black_failure = blackPatternMatches(envVar.blacklist, envValue);
                if (black_failure) {
                    notifyMsg.append("check ");
                    notifyMsg.append(check_name);
                    notifyMsg.append(": Environment variable ");
                    notifyMsg.append(envName);
                    notifyMsg.append(" has value ");
                    notifyMsg.append(processMeTkns[1]);
                    notifyMsg.append(" which matches blacklist ");
                    notifyMsg.append(envVar.blacklist.toString());
                    notifyMsg.append("\n");

                    if (envVar.black_failure_action == RunChecks.REPLACE_ACTION) {
                        // create "<name>=_________"
                        newProcessMe = processMeTkns[0] + "="
                                + TaintUtils.replaceAll(processMeTkns[1], ".", envVar.black_replace_char);

                        // Set the trust of the name be that processMeTkns[0]
                        // Set the = char and the replace chars that follow to untrusted
                        newProcessMe = TaintUtils.copyTaint(processMeTkns[0], newProcessMe, processMeTkns[0].length());
                        newProcessMe = TaintUtils.taint(newProcessMe, processMeTkns[0].length(),
                                                        processMeTkns[1].length() + 1); // Add + 1 for the equals sign
                    }

                    if (newProcessMe != processMe) {
                        notifyMsg.append("Altered environment entry to: ");
                        notifyMsg.append(newProcessMe);
                        notifyMsg.append("\n");
                    }

                    notifyMsg.setAction(envVar.black_failure_action);

                    if (envVar.black_failure_action == RunChecks.EXCEPTION_ACTION
                            || envVar.black_failure_action == RunChecks.TERMINATE_ACTION) {
                        notifyMsg.prepareForExceptionOrTerminate(envVar.env_black_exception_constructor,
                                                                 envVar.black_failure_action);
                        Notify.notifyAndRespond(notifyMsg); // IOException // envVar.black_failure_response;
                    }
                }
            }
        }

        return newProcessMe;
    }

    private boolean isWhitelisted2(final String section, final String checkName, final String directive,
                                   final int white_failure_action, final NotifyMsg notifyMsg, final Pattern pattern) {
        boolean retval = true;
        if (pattern != null) {
            retval = whitePatternMatches(pattern, section);
            if (!retval) {
                notifyMsg.append("check: ");
                notifyMsg.append(checkName);
                notifyMsg.append("  directive: ");
                notifyMsg.append(directive);
                notifyMsg.append("  regular expression: ");
                notifyMsg.append(pattern.toString());
                notifyMsg.append("  failed to match: \"");
                notifyMsg.append(section);
                notifyMsg.append("\"\n");

                if (white_failure_action != 0) {
                    notifyMsg.setAction(white_failure_action);
                }
            }
        }

        return retval;
    }

    private boolean isWhitelisted(final String section, final NotifyMsg notifyMsg, boolean isPrefix,
                                  boolean useInternal, boolean isSuffix) {
        boolean retval = true;

        if (isPrefix) {
            retval = isWhitelisted2(section, check_name, PREFIX_WHITE_TKN, white_failure_action, notifyMsg,
                                    prefix_whitelist);
        }
        if (retval && useInternal) {
            retval = isWhitelisted2(section, check_name, INTERNAL_WHITE_TKN, white_failure_action, notifyMsg,
                                    internal_whitelist);
        }
        if (retval && isSuffix) {
            retval = isWhitelisted2(section, check_name, SUFFIX_WHITE_TKN, white_failure_action, notifyMsg,
                                    suffix_whitelist);
        }

        return retval;
    }

    private boolean isBlacklisted2(final String section, final String checkName, final String directive,
                                   final int black_failure_action, final NotifyMsg notifyMsg, final Pattern pattern) {
        boolean retval = false;
        if (pattern != null) {
            retval = blackPatternMatches(pattern, section);
            if (retval) {
                notifyMsg.append("check: ");
                notifyMsg.append(checkName);
                notifyMsg.append("  directive: ");
                notifyMsg.append(directive);
                notifyMsg.append("  regular expression: ");
                notifyMsg.append(pattern.toString());
                notifyMsg.append("  matched: \"");
                notifyMsg.append(section);
                notifyMsg.append("\"\n");

                if (black_failure_action != 0) {
                    notifyMsg.setAction(black_failure_action);
                }
            }
        }

        return retval;
    }

    private boolean isBlacklisted(final String section, final NotifyMsg notifyMsg, boolean isPrefix,
                                  boolean useInternal, boolean isSuffix) {
        boolean retval = false;
        if (isPrefix) {
            retval = isBlacklisted2(section, check_name, PREFIX_BLACK_TKN, black_failure_action, notifyMsg,
                                    prefix_blacklist);
        }
        if (!retval && useInternal) {
            retval = isBlacklisted2(section, check_name, INTERNAL_BLACK_TKN, black_failure_action, notifyMsg,
                                    internal_blacklist);
        }
        if (!retval && isSuffix) {
            retval = isBlacklisted2(section, check_name, SUFFIX_BLACK_TKN, black_failure_action, notifyMsg,
                                    suffix_blacklist);
        }
        return retval;
    }

    /**
     * <pre>
     * Each bit of data that is checked because of a config file "desc" entry goes through this method.
     * This method uses the directives listed in global directivesList inspect String processMe
     *
     * @param processMe Look for dangerous untrusted data in this string
     * @param runningShell bit-or values of RUNNING_SHELL RUNNING_EXEC.
     * @return processMe, which may be altered if action is REPLACE_ACTION and dangerous untrusted
     *         data was replaced in processMe,
     */
    public String runFromChk(String processMe, Tokenized tokenized, final int runningShell, final NotifyMsg notifyMsg)
            throws MsgException, IOException {
        boolean didWhiteBlackLists = false;
        boolean didEnv = false;

        boolean processMeChanged = false;
        if (tokenizer != null && tokenized == null) {
            tokenized = tokenizer.tokenize(processMe, notifyMsg);
            String newProcessMe = tokenized.getString();
            processMeChanged = (processMe != newProcessMe);
            if (processMeChanged)
                processMe = newProcessMe;
        }
        for (int i = 0; i < directivesList.size(); i++) {
            final String directive = directivesList.elementAt(i);

            final String[] tokens = directive.split("\\s+"); // PatternSyntaxException
            final String cmd = tokens[0];

            if (cmd.equals(TOKEN_TKN)) {
                if (tokenizer != null && processMeChanged) {
                    tokenized = tokenizer.tokenize(processMe, notifyMsg);
                    String newProcessMe = tokenized.getString();
                    processMeChanged = (processMe != newProcessMe);
                    if (processMeChanged)
                        processMe = newProcessMe;
                }

                // directive looks like "token <tkn_num> <check_name>"
                final int tkn_num = Utils.getTokenIntAtIndex(directive, 1); // MsgException
                // TODO: may want to implement this using the Tokenizer class...
                final String nth_processMe_tkn = (tokenized == null)
                        ? Utils.getTokenAtIndexWithTaint(processMe, tkn_num)
                        : tokenized.getTokenAt(tkn_num); // MsgException
                final String checkName = Utils.getTokenAtIndex(directive, 2); // MsgException
                final Chk newChk = BaseConfig.getInstance().findChkFromName(checkName);
                if (newChk != null) {
                    final String new_nth = newChk.runFromChk(nth_processMe_tkn, tokenized, runningShell, notifyMsg); // MsgException IOException

                    // nth_processMe_tkn is token number n in processMe.
                    // Locate nth_processMe_tkn, replace it with new_nth, and return the resulting string
                    if (new_nth != nth_processMe_tkn) {
                        processMe = replace_nth_token(processMe, nth_processMe_tkn, new_nth, tkn_num);
                    }
                }

            } else if (cmd.equals(PREFIX_WHITE_TKN) || cmd.equals(INTERNAL_WHITE_TKN) || cmd.equals(SUFFIX_WHITE_TKN)
                    || cmd.equals(PREFIX_BLACK_TKN) || cmd.equals(INTERNAL_BLACK_TKN) || cmd.equals(SUFFIX_BLACK_TKN)
                    || cmd.equals(TRAVERSAL_COUNT_TKN)) {
                // Do these only once, and all at once. Because traveling through
                // alternating trusted/untrusted sections is expensive
                if (!didWhiteBlackLists) {
                    didWhiteBlackLists = true;

                    if ((runningShell & (RunChecks.RUNNING_SHELL | RunChecks.RUNNING_EXEC)) != 0) {
                        processMe = tokenizeAndRunWhiteBlack(processMe, notifyMsg); // IOException
                    } else {
                        processMe = runWhiteBlackSections(processMe, notifyMsg); // IOException
                    }
                }

            } else if (cmd.equals(BLACK_FAILURE_ACTION_TKN) || cmd.equals(WHITE_FAILURE_ACTION_TKN)
                    || cmd.equals(ARRAY_TKN)) {

            } else if (cmd.equals(VAR_VALUE_WHITE_TKN) || cmd.equals(VAR_VALUE_BLACK_TKN)) {
                if (!didEnv) {
                    didEnv = true;
                    processMe = runEnvChk(processMe, notifyMsg); // MsgException IOException
                }

            } else if (cmd.equals(QSTRING_TKN)) {
                processMe = doQString(processMe, notifyMsg, directive); // IOException

            } else if (cmd.equals(TOKEN_WHITESPACE_TKN)) {
                processMe = runTokenWhiteSpace(processMe, notifyMsg, directive); // IOException

            } else if (cmd.equals(BLACK_ENCODINGS_TKN) || cmd.equals(WHITE_ENCODINGS_TKN)) {
                runEncodingCheck(processMe, notifyMsg, directive);
            } else { // token 0 in directive is unknown. It must name another check.
                final Chk chk_2 = BaseConfig.getInstance().findChkFromName(cmd);
                if (chk_2 == null) {
                    // Error unknown token
                    final String file = BaseConfig.getInstance().getPath();
                    Notify.appendToLogFile("ERROR in configuration file " + file + "\nUnknown token read " + cmd, 0); // 0 means error has nothing to do with attack
                } else {
                    processMe = chk_2.runFromChk(processMe, tokenized, runningShell, notifyMsg); // MsgException IOException
                }
            }
        }

        return processMe;
    }

    /**
    * @param section Is presumably an untrusted section. Each sequence of chars in section
    *                that matches "pattern" is replaced with a string of "replaceChar"
    *          Note: For charTaint, "section" is unmapped and pattern was compiled from an
    *                unmapped Sting - so pattern.matcher(section) will work as expected. 
    * @param pattern Is a blacklist pattern
    * @param replaceChar Repeating sequences of this character replace substrings of "section"
    * @return Section with the substrings that match pattern replaced with "replaceChar"
    */
    private static String blacklistReplace2(final String section, final Pattern pattern, final String replaceChar) {

        String newSection = section;

        if (pattern != null) {
            final Matcher matcher = pattern.matcher(section);
            final StringBuffer strbuf = new StringBuffer();
            boolean changed = false;
            boolean cont = true;
            while (cont) {
                cont = matcher.find();
                if (cont) {
                    final int numChars = matcher.end() - matcher.start();
                    changed = true;
                    final StringBuffer charBuf = new StringBuffer();
                    for (int i = 0; i < numChars; i++) {
                        charBuf.append(replaceChar);
                    }
                    matcher.appendReplacement(strbuf, charBuf.toString()); // no need save taint
                }
            }

            if (changed) {
                matcher.appendTail(strbuf);
            }

            if (strbuf.length() != 0) {
                // Are altering newSection, so copy taint from section to newSection
                newSection = strbuf.toString();
                newSection = TaintUtils.copyTaint(section, newSection, section.length());
            }
        }

        return newSection;
    }

    private String blacklistReplace(final String section, final boolean isPrefix, final boolean useInternal,
                                    final boolean useSuffix) {
        String newSection = section;
        if (isPrefix) {
            newSection = blacklistReplace2(newSection, prefix_blacklist, black_replace_char);
        }
        if (useInternal) {
            newSection = blacklistReplace2(newSection, internal_blacklist, black_replace_char);
        }
        if (useSuffix) {
            newSection = blacklistReplace2(newSection, suffix_blacklist, black_replace_char);
        }

        return newSection;
    }

    /**
    * Ex:    /bin/ls ~/file
    *        ^       ^^
    *        |       ||____ would return false for this trStart
    *   true_|       |
    *                |__ would return true for this trStart
    *
    * Ex:    csh -c "~/file"
    *                ^
    *                |___ would return true for this index
    *
    * @param processMe trStart indexes the start of an untrusted section within processMe
    * @param trStart Want to know if the string starting here is a prefix of its parent token
    * @return true is trStart index a substring of processMe that is the prefix of a string
    */
    private boolean isSectionPrefix(final String processMe, int trStart) {
        trStart--;
        return (trStart < 0 ||
        // If (are on closing quote or char to left is closing quote) and (open/close quote is not empty),
        //     isPrefix is false
        // If on any closing quote or char to left is an any closing quote,
        //    move left thru open/close quotes
        //    if land on non-empty open close quote, isPrefix is false
        //    if land on white space, isPrefix is true
        //
                processMe.charAt(trStart) == '\"' || processMe.charAt(trStart) == '\''
                || Character.isWhitespace(processMe.charAt(trStart)));
    }

    /**
    * Ex        /bin/ls ~/file
    *                 ^     ^^
    *                 |     ||__ would return true if trEnd were here
    *                 |     |___ would return false if trEnd were here
    *                 |_________ would return true if trEnd were here
    *
    * Ex       /bin/ls -l "~/file"
    *                           ^
    *                           |__ would return true
    *
    * @param processMe
    * @param trEnd Indexes the last untrusted character in a contiguous string of untrusted
    *              characters.
    * @return
    */
    private boolean isSectionSuffix(final String processMe, int trEnd) {
        trEnd++;
        return (trEnd >= processMe.length() - 1 || processMe.charAt(trEnd) == '\"' || processMe.charAt(trEnd) == '\''
                || Character.isWhitespace(processMe.charAt(trEnd)));
    }

    private String tokenizeAndRunWhiteBlack(final String processMe, final NotifyMsg notifyMsg)
            throws IOException, MsgException {
        // First need to tokenize sections surrounded by trusted quotes
        // Each unescaped quote enclosed in this section is untrusted and program should escape each.
        // The tokenizing below should be applied that part of string not enclosed in trusted quotes.

        // Form a Vector of Tkn in class Quotes. Each Tkn element represents a string token as a shell would see it
        // and is the token that the regular expressions are to be matched against
        // Ex "ab"""../fname   - where all quotes are untrusted would be tokenized to -  ab../fname
        //    The quotes are never seen by shell
        final Quotes quotes = new Quotes(processMe);
        quotes.fillQuotePairs(); // Collect indexes of pairs of trusted and untrusted quotes
        quotes.tokenize(); // Tokenize processMe. Remove quotes that shell would remove. Do this so that
                           // regular expressions will work. ex """"../a.out is changed to ../a.out
                           //                                 ex: "a "bc  is changed to string  a bc

        Vector<String> tkns = new Vector<String>();
        boolean alteredStr = false;
        String str;

        // tknStrAtIndex retrieve string token. The return string has the outmost quotes,
        // if any, removed. Plus "do nothing" quotes also have been removed.
        for (int i = 0; (str = quotes.tknStrAtIndex(i)) != null; i++) {
            String newStr = runWhiteBlackSections(str, notifyMsg); // IOException
            if (newStr == str) {
                // Put back quotes that were removed
                newStr = quotes.reconstructTknStrAtIndex(i);
            } else {
                alteredStr = true;
                // Put back quotes that were remove
                newStr = quotes.reconstructTknStrAtIndex(newStr, i);
            }

            tkns.addElement(newStr);
        }

        String retval;

        if (alteredStr) {
            final StringBuilder buf = TaintUtils.newStringBuilder();
            for (int i = 0; i < tkns.size(); i++) {
                if (i != 0) {
                    TaintUtils.append(buf, " ");
                }
                TaintUtils.append(buf, tkns.elementAt(i));
            }

            retval = TaintUtils.toString(buf);

        } else {
            retval = processMe;
        }

        return retval;
    }

    /**
     * Helper method for determining whether the substring of the input up to the point of index
     * matches the the regular expression set for check_criteria.
     *
     * @param input StringBuffer containing the input in question.
     * @param index int of the prefix position.
     * @return True if this criteria check has passed, otherwise false.  If no check_critera has
     *          been set for this check, return true;
     */
    private boolean check_criteria_passed(String input, int index) {
        int len = input.length();
        if (check_criteria == null || index == 0)
            return true;

        String testInput = input.substring(0, (index >= len) ? (len) : (index));
        return check_criteria.matcher(testInput).matches();
    }

    private boolean token_rules_passed(Tokenized tokenized, String input, int start, int end) {
        if (tokenized == null)
            return true;

        int startTokenIdx = tokenized.getTokenIndex(start);
        int endTokenIdx = tokenized.getTokenIndex(end);

        if (matchRegex != null) {
            int idx = matchOffset > 0 ? endTokenIdx : startTokenIdx;
            String token = tokenized.getTokenAt(idx + matchOffset);
            if (token == null || !token.matches(matchRegex)) {
                return false;
            }
        }

        if (nomatchRegex != null) {
            int idx = nomatchOffset > 0 ? endTokenIdx : startTokenIdx;
            String token = tokenized.getTokenAt(idx + nomatchOffset);
            if (token == null || token.matches(nomatchRegex)) {
                return false;
            }
        }

        return true;
    }

    /* NOT NEEDED
    // 'start' indexes a whitespace character or single-quote/double-quote
    // char in processMe  
    // Starting at 'start' move thru characters in processMe until
    // start no longer indexes a whitespace or single/double quote character.
    // Return that index
    private int collect_whitespace(String processMe, int start) {
        start++;
        while(start < processMe.length() &&
             (TaintUtils.characterIsWhitespace(processMe.charAt(start)) ||
              TaintUtils.charEquals(processMe.charAt(start), '\"') ||
              TaintUtils.charEquals(processMe.charAt(start), '\''))) {
            start++;
        }
        return start;
    }
    
    // 'start' indexes a non-whitespace character in processMe.
    // Starting at 'start' move thru characters in processMe until
    // start indexes a whitespace character or move past end of string.
    // Return that index
    private int collect_non_whitespace(String processMe, int start) {
         start++;
         while((start < processMe.length()) &&
              !TaintUtils.characterIsWhitespace(processMe.charAt(start))) {
             start++;
         }
         return start;
    }
    
    private String traversal_count_on_tokens(String processMe, NotifyMsg notifyMsg) throws MsgException,
                                                                                           IOException {
        int start = 0;
        int stop = 0;  // indexes one past the last char in
                       // white-space non-white-space section
        StringBuffer buf = new StringBuffer();
        while(start < processMe.length()) {
            if (TaintUtils.characterIsWhitespace(processMe.charAt(start))|| 
                TaintUtils.charEquals(processMe.charAt(start), '\"') ||
                TaintUtils.charEquals(processMe.charAt(start), '\'')) {
    
                stop = collect_whitespace(processMe, start);
                buf.append(processMe.substring(start, stop));
            } else {
                stop = collect_non_whitespace(processMe, start);
                String str = doTraversalCount(processMe.substring(start, stop),
                                              notifyMsg); // MsgException
                                                          // IOException
                buf.append(str);
            }
            start = stop;
        }
        return buf.toString();
    }
    */

    /**
     * <pre>
     * @param processMe Can contain alternating trusted/untrusted sections
     *                  Discover dangerous untrusted data in this string.
     * @param For notify. The API signature of caller. no param names or ret val. ex. "Runtime.exec(String, String[])"
     * @return processMe. If white_failure_action or black_failure_action is REPLACE_ACTION and dangerous
     *                    untrusted data is discovered in processMe, then the returned String will differ from
     *                    processMe.
     * @throws MsgException 
     */
    private String runWhiteBlackSections(String processMe, final NotifyMsg notifyMsg) throws IOException, MsgException {
        if (traversal_count_action >= 0) {
            // NOT NEEDED
            // processMe can look like "/bin/ls ../file"
            // doTraversalCount splits the string on '/' and does not
            // expect the string it is processing to contain whitespace.
            // traversal_count_on_tokens does the following:
            // o Split processMe in white space delimited string
            // o send each to doTraversalCount
            // o reassemble the returned strings with the original white
            //   space between each return string.
            // processMe = traversal_count_on_tokens(processMe, notifyMsg); // MsgException

            // Replaced above.
            // if a traversal count should be enforced, then
            // check it against the string in question and
            // sanitize if necessary.
            processMe = doTraversalCount(processMe, notifyMsg);
        }
        final int whiteAction = white_failure_action;
        final int blackAction = black_failure_action;
        final StringBuilder alteredProcessMeBuf = TaintUtils.newStringBuilder();
        int startCpy = 0; // always points to next avail place in processMe where to start copying char from

        Tokenized tokenized = null;
        if (tokenizer != null) {
            tokenized = tokenizer.tokenize(processMe, notifyMsg);
            processMe = tokenized.getString();
        }

        StringRegionIterator iter = new StringRegionIterator(processMe, TaintValues.TRUST_MASK);
        // loop thru contiguous trusted/untrusted sections
        while (iter.hasNext()) {
            StringRegion sr = iter.next();
            if (sr.isUntrusted()) { // region is not trusted...
                int trStart = sr.getStart();
                if (trStart >= processMe.length())
                    break;
                int trEnd = Math.min(sr.getEnd(), processMe.length() - 1);

                if (!check_criteria_passed(processMe, trStart))
                    continue;
                if (!token_rules_passed(tokenized, processMe, trStart, trEnd))
                    continue;

                final String section = sr.getString();

                //                                                             ______________
                // ex. section can be the untrusted "~/illeagl_file : /bin/cat ~/illegal_file"
                final boolean isPrefix = isSectionPrefix(processMe, trStart);
                final boolean isSuffix = isSectionSuffix(processMe, trEnd);

                // ***
                // Do white list
                // use internal whitelist if section is neither prefix nor suffix
                //                     or if neither of these conditions exist:
                //                          o section is a prefix and prefix_whitelist exists
                //                          o section is a suffix and suffix_whitelist exists
                boolean useInternal = ((!isPrefix && !isSuffix) || internal_whitelist != null);

                // If white list does not match, notifyMsg will contain explanation
                final boolean whitelistFailure = !isWhitelisted(section, notifyMsg, isPrefix, useInternal, isSuffix);

                if (whitelistFailure) {
                    if (whiteAction == RunChecks.EXCEPTION_ACTION || whiteAction == RunChecks.TERMINATE_ACTION) {
                        notifyMsg.prepareForExceptionOrTerminate(white_exception_constructor, white_failure_action);
                        Notify.notifyAndRespond(notifyMsg); // IOException or terminate
                    }
                }

                // ***
                // DO black list
                // use internal blacklist if section is neither prefix nor suffix
                //                     or if neither of these conditions exist:
                //                        o section is a prefix and prefix_blacklist exists
                //                        o section is a suffix and suffix_blacklist exists
                useInternal = ((!isPrefix && !isSuffix) || internal_blacklist != null);

                final boolean blacklistFailure = isBlacklisted(section, notifyMsg, isPrefix, useInternal, isSuffix);

                // The case where need to keep looping - action is replace,
                if (blacklistFailure) {
                    if (blackAction == RunChecks.REPLACE_ACTION) {
                        final String newStr = blacklistReplace(section, isPrefix, useInternal, isSuffix);
                        if (!newStr.equals(section)) {
                            TaintUtils.append(alteredProcessMeBuf, TaintUtils.substring(processMe, startCpy, trStart));
                            TaintUtils.append(alteredProcessMeBuf, newStr);
                            startCpy = trStart + newStr.length();
                        }
                    } else if (blackAction == RunChecks.EXCEPTION_ACTION || blackAction == RunChecks.TERMINATE_ACTION) {
                        notifyMsg.prepareForExceptionOrTerminate(black_exception_constructor, black_failure_action);
                        Notify.notifyAndRespond(notifyMsg); // IOException or terminate
                    }
                }
            }
        }

        if (alteredProcessMeBuf.length() > 0 && startCpy < processMe.length()) {
            TaintUtils.append(alteredProcessMeBuf, TaintUtils.substring(processMe, startCpy));
        }

        String retval = processMe;

        if (alteredProcessMeBuf.length() > 0) { // an untrusted section matched a blacklist
            retval = TaintUtils.toString(alteredProcessMeBuf);
            notifyMsg.append("replacement: " + retval + "\n");
        }

        return retval;
    }

    /**
     * This method is used for testing if a data source such as file input streams, environment
     * variables, and properties should be tainted.  The data source is considered trusted if
     * and only if the underlying regular expression matches the supplied string.
     * 
     * Is like runWhiteBlackSections except the entire string is processed without regard to
     * trusted/untrusted sections
     * Return true if processMe is a trusted
     * 
     * @param processMe - for chartaint is mapped/unmapped
     * @param notifyMsg
     * @return
     */
    public boolean runTrustedDataSource(String processMe, final NotifyMsg notifyMsg) {
        final boolean whitelisted = isWhitelisted(processMe, notifyMsg, // Message will added, but never used
                                                  false, // isPrefix
                                                  true, // useInternal
                                                  false); // isSuffix

        return whitelisted;
    }

    private void tokenWhiteSpaceMsg(final String processMe, final NotifyMsg notifyMsg, final String directive) {

        notifyMsg.setAction(token_whitespace_action);

        notifyMsg.append("check ");
        notifyMsg.append(check_name);
        notifyMsg.append(": directive ");
        notifyMsg.append(directive);
        notifyMsg.append(".\nHave detected untrusted white space characters in the following line:\n");
        notifyMsg.append(processMe);
        notifyMsg.append("\n");
    }

    /**
    * Detect untrusted white space in processMe.
    *
    */
    private String runTokenWhiteSpace(final String processMe, NotifyMsg notifyMsg, final String directive)
            throws IOException {

        final Pattern pattern = Pattern.compile(TaintUtils.whiteSpaceRegExp() + "+");

        boolean keepLooking = true;

        final StringRegionIterator iter = new StringRegionIterator(processMe, TaintValues.TRUST_MASK);
        final String replace_str = (token_whitespace_replace_char == null ? "_" : token_whitespace_replace_char);
        final StringBuilder alteredProcessMeBuf = TaintUtils.newStringBuilder(); // Use when either whiteAction or
        // blackReponse is REPLACE_ACTION
        int startCpy = 0; // always points to next avail place in processMe where to start copying char from

        // loop thru contiguous trusted/untrusted sections
        while (iter.hasNext()) {
            StringRegion sr = iter.next();
            if (sr.isUntrusted()) { // region is not trusted...
                int trStart = sr.getStart();
                if (trStart >= processMe.length())
                    break;

                // Get the untrusted section
                final String section = sr.getString();

                final String alteredSection = blacklistReplace2(section, pattern, replace_str);
                if (alteredSection != section) {
                    if ((token_whitespace_action == RunChecks.EXCEPTION_ACTION
                            || token_whitespace_action == RunChecks.TERMINATE_ACTION)) {
                        notifyMsg.prepareForExceptionOrTerminate(token_whitespace_exception_constructor,
                                                                 token_whitespace_action);
                        tokenWhiteSpaceMsg(processMe, notifyMsg, directive);
                        Notify.notifyAndRespond(notifyMsg); // IOException of terminate

                    } else if (token_whitespace_action == RunChecks.REPLACE_ACTION) {
                        TaintUtils.append(alteredProcessMeBuf, TaintUtils.substring(processMe, startCpy, trStart));
                        TaintUtils.append(alteredProcessMeBuf, alteredSection);
                        startCpy = trStart + alteredSection.length();

                    } else { // whitespace action is LOG_ONLY - can log the info and exit this method
                        break;
                    }
                }
            }
        }

        if (alteredProcessMeBuf.length() > 0 && startCpy < processMe.length()) { // action is replace & replaced untrusted whitespace
            notifyMsg.setAction(token_whitespace_action);
            TaintUtils.append(alteredProcessMeBuf, TaintUtils.substring(processMe, startCpy));
        }

        String newProcessMe = processMe;

        if (alteredProcessMeBuf.length() > 0) { // action was replace and untrusted whitespace was replaced
            newProcessMe = TaintUtils.toString(alteredProcessMeBuf);
        }

        if (!keepLooking || alteredProcessMeBuf.length() > 0) { // action is log_only or replace and found untrusted white space
            tokenWhiteSpaceMsg(processMe, notifyMsg, directive);
            if (newProcessMe != processMe) {
                notifyMsg.append("altered line to: ");
                notifyMsg.append(newProcessMe);
            }

            notifyMsg.append("\n");
        }

        return newProcessMe;
    }

    /**
    * In String processMe locate in the 0-based nth token. Replace that string with new_nth
    *
    * @param processMe Token number n in this line of data is dangerous an untrusted.
    * @param orig_tkn This is token number n in processMe.
    * @param new_nth Replace orig_tkn with this string
    * @param tkn_num The 0-based number of the token in processMe that need replacing
    * @return processMe with token n replaced
    */
    private String replace_nth_token(final String processMe, final String orig_tkn, final String new_nth,
                                     final int tkn_num) {
        String new_processMe = null;
        if (tkn_num > 0) {
            final String[] ray = processMe.split(TaintUtils.whiteSpaceRegExp() + "+"); // PatternSyntaxException

            // Locate index of token tkn_num
            int index = 0;
            for (int i = 0; index != -1 && i < tkn_num; i++) {
                index = processMe.indexOf(ray[i], index);
                if (index != -1) {
                    index += ray[i].length();
                }
            }

            if (index != -1) {
                final String pre = TaintUtils.substring(processMe, 0, index + 1);
                final String post = TaintUtils.substring(processMe, index + orig_tkn.length() + 1);
                new_processMe = pre + new_nth + post;
            }

        } else {
            new_processMe = processMe.replaceFirst(orig_tkn, new_nth);
        }

        return new_processMe;
    }

    /**
     * Checks the path for any illegal path traversals as defined by MITRE
     * 
     * @param path
     * @param notifyMsg
     * @return
     * @throws MsgException
     * @throws IOException
     */
    private String doTraversalCount(String path, NotifyMsg notifyMsg) throws MsgException, IOException {

        if (path.equals(""))
            return path;

        int count = 0;
        boolean becameNeg = false;

        // Trust the prefix of the path that matches the CWD...
        path = TaintUtils.trustPrefixCWD(path);

        String[] pathComponents = TaintUtils.split(path, "/");
        StringBuilder newProcessMe = null;
        if (traversal_count_action == RunChecks.REPLACE_ACTION) {
            newProcessMe = TaintUtils.newStringBuilder(path);
            traversal_count_replace_char = TaintUtils.taint(traversal_count_replace_char);
        }

        int idx = 0;
        for (int i = 0; i < pathComponents.length; i++) {
            String pathComp = pathComponents[i];
            boolean isTainted = TaintUtils.isTainted(pathComp);
            // don't count path components that don't change levels...
            if (!TaintUtils.stringEquals(pathComp, ".") && !pathComp.equals("")) {
                // reset count between trust-taint regions...
                if (!isTainted) {
                    count = 0;
                } else {
                    if (TaintUtils.stringEquals(pathComp, "..")) {
                        count--;
                        if (traversal_count_action == RunChecks.REPLACE_ACTION) {
                            int start = idx;
                            int end = start + 1;
                            TaintUtils.replace(newProcessMe, start, end, traversal_count_replace_char);
                            start = end;
                            end = start + 1;
                            TaintUtils.replace(newProcessMe, start, end, traversal_count_replace_char);
                        }
                    } else {
                        count++;
                    }
                }

                if (count < 0) {
                    becameNeg = true;
                }
            }
            idx += pathComp.length() + 1;
        }

        if (becameNeg) {
            notifyMsg.setAction(traversal_count_action);
            notifyMsg.append("check ");
            notifyMsg.append(check_name);
            notifyMsg.append(": directive traversal_count");
            if (becameNeg) {
                notifyMsg.append(" detected illegal path traversal");
            } else {
                notifyMsg.append(" detected a path with a tainted prefix that is not within the CWD");
            }

            if (traversal_count_action == RunChecks.REPLACE_ACTION) {
                // replace all tainted .. with __
                notifyMsg.append(" \nAltered line to: \"");
                String alteredPath = TaintUtils.toString(newProcessMe);
                notifyMsg.append(alteredPath);
                notifyMsg.append("\"\n");
                return alteredPath;
            } else if (traversal_count_action == RunChecks.EXCEPTION_ACTION
                    || traversal_count_action == RunChecks.TERMINATE_ACTION) {
                notifyMsg.prepareForExceptionOrTerminate(traversal_count_exception_constructor, traversal_count_action);
                Notify.notifyAndRespond(notifyMsg); // IOException or terminate
            }
        }
        return path;
    }

    /**
    *
    * @param processMe  If Q_action is replace, replace untrusted quotes surrounded by trusted quotes with escaped quotes
    * @param directive  The first token of the line read in
    * @return processMe Apply the Qsting check to this line.
    */
    private String doQString(final String processMe, final NotifyMsg notifyMsg, final String directive)
            throws MsgException, IOException {
        // In processMe within each pair of trusted replaceMe, look for untrusted replaceMe
        // and replace each untrusted "replaceMe" with "replaceWith"
        // Note: replaceWith String should be be trusted - I think
        // Here were just go ahead and alter processMe, regardless if Q_action is REPLACE_ACTION.
        if (tokenizer == null)
            return processMe;

        Tokenized tokenized = tokenizer.tokenize(processMe, notifyMsg);
        String newProcessMe = tokenized.escapeQuotes();
        if (newProcessMe != processMe) {
            notifyMsg.setAction(Q_action);

            notifyMsg.append("check ");
            notifyMsg.append(check_name);
            notifyMsg.append(": directive ");
            notifyMsg.append(directive);
            notifyMsg.append(" detected one or more protected quotes");
            notifyMsg.append("\n");

            if (Q_action == RunChecks.REPLACE_ACTION) {
                notifyMsg.append("Altered line to: \"");
                notifyMsg.append(newProcessMe);
                notifyMsg.append("\"\n");
            } else { // action is log_only, exception,  terminate
                notifyMsg.setAction(Q_action);
                newProcessMe = processMe; // Throw away the altered line
            }

            if (Q_action == RunChecks.EXCEPTION_ACTION || Q_action == RunChecks.TERMINATE_ACTION) {
                notifyMsg.prepareForExceptionOrTerminate(Q_exception_constructor, Q_action);
                Notify.notifyAndRespond(notifyMsg); // IOException or terminate
            }
        }

        return newProcessMe;
    }

    private void runEncodingCheck(String processMe, NotifyMsg notifyMsg, String directive) throws IOException {
        final StringRegionIterator iter = new StringRegionIterator(processMe);

        while (iter.hasNext()) {
            StringRegion sr = iter.next();
            int taint = sr.getTaint();
            if (sr.isUntrusted()) { // not trusted
                if ((encodings_blacklist & TaintValues.ENCODING_MASK & taint) != 0) {
                    encodingCheckTakeAction(processMe, notifyMsg, directive, taint, true);
                } else if ((encodings_whitelist | (TaintValues.ENCODING_MASK & taint)) != (encodings_whitelist
                        & TaintValues.ENCODING_MASK)) {
                    encodingCheckTakeAction(processMe, notifyMsg, directive, taint, false);
                }
            }
        }
    }

    private String encodingCheckTakeAction(String processMe, NotifyMsg notifyMsg, String directive, int encoding,
                                           boolean isBlack)
            throws IOException {
        int action = (isBlack) ? (encodings_blacklist_action) : (encodings_whitelist_action);
        notifyMsg.setCweNumber(838);
        notifyMsg.setAction(action);

        notifyMsg.append("check ");
        notifyMsg.append(check_name);
        notifyMsg.append(": directive ");
        notifyMsg.append(directive);
        // TODO get the encoding object from the numeric encoding...
        notifyMsg.append(" detected improper encoding: " + encoding + "\n");

        if (action == RunChecks.REPLACE_ACTION) {
            // TODO: Add functionality to encode the input properly.
            //            notifyMsg.append("Altered line to: \"");
            //            notifyMsg.append(newProcessMe);
            //            notifyMsg.append("\"\n");
            throw new RuntimeException("Replace is not yet supported for directive: " + directive);
        }

        if (action == RunChecks.EXCEPTION_ACTION || action == RunChecks.TERMINATE_ACTION) {
            Constructor<?> exception = (isBlack) ? (encodings_blacklist_exception_constructor)
                    : (encodings_whitelist_exception_constructor);
            notifyMsg.prepareForExceptionOrTerminate(exception, action);
            Notify.notifyAndRespond(notifyMsg); // IOException or terminate
        }

        return processMe;
    }

    private boolean whitePatternMatches(final Pattern pattern, final String str_param) {
        String str = str_param;
        final Matcher matcher = pattern.matcher(str);
        return matcher.matches(); // Note: whitelist reg exp use Matcher.matches(). blacklist reg exp use Matcher.find()
    }

    private boolean blackPatternMatches(final Pattern pattern, final String str_param) {
        String str = str_param;
        final Matcher matcher = pattern.matcher(str);
        return matcher.find(); // blacklist regular expression must use find
    }

    // Called when are finishied building this single Chk.
    // Run consistancy check.
    void debug_verify() throws MsgException {
        StringBuffer strbuf;
        if (black_failure_action == RunChecks.REPLACE_ACTION && black_replace_char == null) {
            strbuf = new StringBuffer();
            strbuf.append("In check ");
            strbuf.append(check_name);
            strbuf.append(": ");
            strbuf.append(BLACK_FAILURE_ACTION_TKN);
            strbuf.append(" is set to ");
            strbuf.append(RunChecks.REPLACE_ACTION_TKN);
            strbuf.append(".\n");
            strbuf.append("But the black list replace character is not set.");

            throw new MsgException(strbuf.toString());
        }

        if (envVec != null) {
            for (int i = 0; i < envVec.size(); i++) {
                final EnvVar envVar = envVec.elementAt(i);
                if (envVar.black_failure_action == RunChecks.REPLACE_ACTION && envVar.black_replace_char == null) {
                    strbuf = new StringBuffer();
                    strbuf.append("In check ");
                    strbuf.append(check_name);
                    strbuf.append(": entry ");
                    strbuf.append(VAR_VALUE_BLACK_TKN);
                    strbuf.append(" for environment variable ");
                    strbuf.append(envVar.varName);
                    strbuf.append(".\n");
                    strbuf.append(BLACK_FAILURE_ACTION_TKN);
                    strbuf.append(" is set to ");
                    strbuf.append(RunChecks.REPLACE_ACTION_TKN);
                    strbuf.append(".\n");
                    strbuf.append("But the black list replace character is not set.");

                    throw new MsgException(strbuf.toString());
                }
            }
        }
    }

    void dump() {
        System.out.println("####\nname: " + check_name);
        System.out.println("  black & white");
        if (check_criteria != null) {
            System.out.println("    check_criteria: " + check_criteria.toString());
        }
        if (prefix_whitelist != null) {
            System.out.println("    prefix_whitelist: " + prefix_whitelist.toString());
        }
        if (internal_whitelist != null) {
            System.out.println("  internal_whitelist: " + internal_whitelist.toString());
        }
        if (suffix_whitelist != null) {
            System.out.println("    suffix_whitelist:  " + suffix_whitelist.toString());
        }
        if (white_failure_action != 0) {
            System.out.println("    white_failure_action:  " + white_failure_action);
        }
        if (prefix_blacklist != null) {
            System.out.println("    prefix_blacklist:  " + prefix_blacklist.toString());
        }
        if (internal_blacklist != null) {
            System.out.println("    internal_blacklist:  " + internal_blacklist.toString());
        }
        if (suffix_blacklist != null) {
            System.out.println("    suffix_blacklist:  " + suffix_blacklist.toString());
        }
        if (black_failure_action != 0) {
            System.out.println("    black_failure action:  " + black_failure_action);
        }
        if (black_replace_char != null) {
            System.out.println("    black_replace_char:  " + black_replace_char);
        }
        if (white_exception_constructor != null) {
            System.out.println("    white_exception_constructor: " + white_exception_constructor.getName());
        }
        if (black_exception_constructor != null) {
            System.out.println("    black_exception_constructor: " + black_exception_constructor.getName());
        }

        System.out.println("  token whitespace");
        System.out.println("    token_whitespace_replace_char: " + token_whitespace_replace_char);
        System.out.println("    token_whitespace_action: " + token_whitespace_action);
        System.out.println("    token_whitespace_replace_char: " + token_whitespace_replace_char);

        System.out.println("  EnvVec");
        for (int i = 0; i < envVec.size(); i++) {
            try {
                final EnvVar envVar = envVec.elementAt(i);
                System.out.println("  varName: " + envVar.varName);
                System.out.print("  whitelist: ");
                System.out.println(envVar.whitelist == null ? "null" : envVar.whitelist.toString());
                System.out.print("  white_failure_action: ");
                System.out.println(envVar.white_failure_action == 0 ? "nnon"
                        : (RunChecks.actionIntToString(envVar.white_failure_action))); // MsgException
                System.out.print("  env_white_exception_constructor: ");
                System.out.print(envVar.env_white_exception_constructor == null ? "null"
                        : envVar.env_white_exception_constructor);

                System.out.print("  blacklist: ");
                System.out.println(envVar.blacklist == null ? "null" : envVar.blacklist.toString());
                System.out.print("  black_failure_action: ");
                System.out.println(envVar.black_failure_action == 0 ? "none"
                        : (RunChecks.actionIntToString(envVar.black_failure_action))); // MsgException
                System.out.print("  replace_char: ");
                System.out.println(envVar.black_replace_char == null ? "null" : (envVar.black_replace_char));
                System.out.print("  env_black_exception_constructor: ");
                System.out.println(envVar.env_black_exception_constructor == null ? "null"
                        : envVar.env_black_exception_constructor);
            } catch (MsgException ex) {
                System.out.println("  Error: " + ex.getMessage());
            }
        }

        System.out.println("");
    }

} // class Chk
