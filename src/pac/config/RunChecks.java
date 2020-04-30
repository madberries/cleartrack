package pac.config;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import pac.config.Tokenizer.Tokenized;
import pac.inst.taint.SystemInstrumentation;
import pac.util.Ret;
import pac.util.TaintUtils;
import pac.util.TaintValues;

// actionStringToInt
// actionIntToString
// runFromChkArray
// divideOnTrustedCmdSeparatorStrings
// isBashMultiCharOption
// handlePossibleShellAndParams
// expandDollarVar
// expandDollarVars
// runFromDescArray
// runFromDesc
// checkLegalFileName
//

public class RunChecks {
    public static final int RUNNING_SHELL = 1; // Indicates that a shell is running an executable
    public static final int RUNNING_EXEC = 2; // Indicates that either Runtime.exe or ProcessBuilder are running an executable

    // This is the name of a check in the config file.
    // It has the directives to be used when Runtime.exec runs a shell and the shell runs a command.
    // This directive is used on the command
    // ex: Runtime.exec(String[]cmd, ...) - if cmd[0] is a shell, and cmd[1] is "-c",  then this check
    //     will be used on cmd[2]
    // ex: Runtime.exec(String cmd, ...) cmd is tokenized and if token[0] is a shell and token[1] is "-c" then
    //     this check will be used on token[2]
    private static final String SHELL_RUNS_THIS_COMMAND_CHK = "shell_runs_this_command";

    static final String LOG_ONLY_ACTION_TKN = "log_only";
    static final String EXCEPTION_ACTION_TKN = "exception";
    static final String TERMINATE_ACTION_TKN = "terminate";
    static final String REPLACE_ACTION_TKN = "replace";
    static final String REMOVE_ACTION_TKN = "remove";
    static final String BREAK_ACTION_TKN = "break";
    static final String JAIL_ACTION_TKN = "jail";
    static final String IGNORE_ACTION_TKN = "ignore";

    public static final int LOG_ONLY_ACTION = 1;
    public static final int EXCEPTION_ACTION = 2;
    public static final int TERMINATE_ACTION = 3;
    public static final int REPLACE_ACTION = 4;
    public static final int BREAK_ACTION = 5;
    public static final int JAIL_ACTION = 6;
    public static final int REMOVE_ACTION = 7;
    public static final int IGNORE_ACTION = 8;

    private static HashSet<String> bashMultiCharOptions = null; // bash multi-character option ex: "--protected"

    private static final int IS_SHELL_CMD_WITH_NO_PARAMS = -100;

    static int actionStringToInt(final String actionStr) throws MsgException {
        int retval;
        if (LOG_ONLY_ACTION_TKN.equals(actionStr)) {
            retval = LOG_ONLY_ACTION;
        } else if (EXCEPTION_ACTION_TKN.equals(actionStr)) {
            retval = EXCEPTION_ACTION;
        } else if (TERMINATE_ACTION_TKN.equals(actionStr)) {
            retval = TERMINATE_ACTION;
        } else if (REPLACE_ACTION_TKN.equals(actionStr)) {
            retval = REPLACE_ACTION;
        } else if (BREAK_ACTION_TKN.equals(actionStr)) {
            retval = BREAK_ACTION;
        } else if (JAIL_ACTION_TKN.equals(actionStr)) {
            retval = JAIL_ACTION;
        } else if (REMOVE_ACTION_TKN.equals(actionStr)) {
            retval = REMOVE_ACTION;
        } else if (IGNORE_ACTION_TKN.equals(actionStr)) {
            retval = IGNORE_ACTION;
        } else {
            throw new MsgException("Unable to convert " + actionStr + " to its integer representation.");
        }

        return retval;
    }

    static String actionIntToString(final int actionInt) throws MsgException {
        String retval;
        switch (actionInt) {
        case 0:
            retval = "[Action is not set]";
            break;
        case LOG_ONLY_ACTION:
            retval = LOG_ONLY_ACTION_TKN;
            break;
        case EXCEPTION_ACTION:
            retval = EXCEPTION_ACTION_TKN;
            break;
        case TERMINATE_ACTION:
            retval = TERMINATE_ACTION_TKN;
            break;
        case REPLACE_ACTION:
            retval = REPLACE_ACTION_TKN;
            break;
        case BREAK_ACTION:
            retval = BREAK_ACTION_TKN;
            break;
        case JAIL_ACTION:
            retval = JAIL_ACTION_TKN;
            break;
        case REMOVE_ACTION:
            retval = REMOVE_ACTION_TKN;
            break;
        case IGNORE_ACTION:
            retval = IGNORE_ACTION_TKN;
            break;
        default:
            throw new MsgException("Failed to convert int action to String action.\n"
                    + "actionIntToString called with bad action integer: " + actionInt);
        }

        return retval;
    }

    // Loop thru each element in processMe
    //     Apply each directives of chk to each line.
    //     EXCEPT directive "array <num> check_name"
    //            This directive applies only once.
    //            It is applied to a specific processMe element
    //     ex. "array 0 must_trust" this directive says to apply must_trust to processMe[0]
    //
    // param runningShell RUNNING_EXEC bit is set if called from Runtime.exec or ProcessBuilder
    //
    private static void runFromChkArray(final String[] processMe, final Chk chk, final int runningShell,
                                        final NotifyMsg notifyMsg)
            throws MsgException, IOException {
        // First, do any ARRAY_TKN commands
        for (int i = 0; i < chk.directivesList.size(); i++) {
            final String directive = chk.directivesList.elementAt(i);
            if (directive.startsWith(ConfigFileTokens.ARRAY_TKN)) {
                // directive looks is: "array <num> <check_name>"
                final int arrayIndex = Utils.getTokenIntAtIndex(directive, 1); // MsgException
                final String check_name = Utils.getTokenAtIndex(directive, 2); // MsgException
                final Chk newChk = BaseConfig.getInstance().findChkFromName(check_name);
                if (newChk == null) {
                    throw new MsgException("Configfile check " + chk.check_name + ": directive " + directive + "\n"
                            + "Unable to locate check " + check_name);
                }
                if (arrayIndex > processMe.length - 1) {
                    throw new MsgException("Configfile check " + chk.check_name + ": directive " + directive + "\n"
                            + arrayIndex + " indexes out of bounds.\n" + "The array size is " + processMe.length);
                }

                final String new_processMe = newChk.runFromChk(processMe[arrayIndex], null, runningShell, notifyMsg); // MsgException IOException
                if (new_processMe != processMe[i]) {
                    processMe[i] = new_processMe;
                }
            }
        }

        // Loop thru each element in processMe
        // runFromChk will NOT perform ARRAY_TKN directives
        for (int i = 0; i < processMe.length; i++) {
            final String newProcessMe = chk.runFromChk(processMe[i], null, runningShell, notifyMsg); // MsgException IOException
            if (newProcessMe != processMe[i]) {
                processMe[i] = newProcessMe;
            }
        }
    }

    /**
    * Called when Runtime.exec runs a shell. Is called to possibly divide the string submitted to the shell
    *        into multiple commands, each separated by the special shell strings:   ;   |   |&   &&   ||
    *        Each of the multiple commands must be processed separately, possibly altered, and then
    *        recombined.
    *
    * @param processMe is the command that the shell is running
    *                  ie for exec(String[]cmd, ...)  processMe is cmd[2]
    *                     for exec(String cmd, ...) processMe is the token[2] when cmd is tokenized
    * @param notifyMsg It's chk has been set. runFromDesc uses the Chk sent in NotifyMsg, rather than
    *                  use the description sent in NotifyMsg to locate the required check
    */
    private static String divideOnTrustedCmdSeparatorStrings(final String processMe, final int runningShell,
                                                             final NotifyMsg notifyMsg)
            throws MsgException, IOException {
        String newProcessMe = null;
        final CmdSplitter cmdSplitter = new CmdSplitter(processMe);
        final String processMeA = cmdSplitter.getA(); // Get String in front of ";" (or one of the other divider strings)
        if (processMeA != null) { // null means processMe did not contain a trusted ";" (or some other trusted divider string)
            final String newProcessMeA = runFromDesc(processMeA, notifyMsg, runningShell, 0, null); // Any env vars in processMeA for shell already have been expanded
                                                                                                    // IOException

            final String processMeB = cmdSplitter.getB(); // Get String on other side of ";"

            // Next few lines for situation where processMeB looks like:
            //              csh -c <command> [; <command - perhaps shell cmd)>]
            // In this case it is as if runFromDesc is being run from api description "Runtime.exe(String...".
            // So change  "Runtime.exec(String[]..."  to  "Runtime.exec(String..."
            // runFromDesc won't test for shell command unless the api descrription is "Runtime.exe(String..."
            //
            final String description = notifyMsg.getDescription();
            if (description.startsWith("Runtime.exec(String[]")) {
                final String description2 = description.replaceFirst("\\[\\]", "");
                notifyMsg.setDescription(description2);
            }

            // If processMeB contains ";", then processMeB will be sent here and be further divided etc
            final String newProcessMeB = runFromDesc(processMeB, notifyMsg, runningShell, 0, null); // Any env vars in processMeB for shell already have been expanded
                                                                                                    // IOException

            // recombine the two commands
            newProcessMe = newProcessMeA + cmdSplitter.getDividerStr() + newProcessMeB;

            notifyMsg.setDescription(description); // put the original description back
        }

        return newProcessMe;
    }

    private static boolean isBashMultiCharOption(final String processMe_param) {

        if (bashMultiCharOptions == null) {
            bashMultiCharOptions = new HashSet<String>();
            bashMultiCharOptions.add("debug");
            bashMultiCharOptions.add("debugger");
            bashMultiCharOptions.add("dump-po-strings");
            bashMultiCharOptions.add("dump-strings");
            bashMultiCharOptions.add("help");
            bashMultiCharOptions.add("init-file");
            bashMultiCharOptions.add("login");
            bashMultiCharOptions.add("noediting");
            bashMultiCharOptions.add("noprofile");
            bashMultiCharOptions.add("norc");
            bashMultiCharOptions.add("posix");
            bashMultiCharOptions.add("protected");
            bashMultiCharOptions.add("rcfile");
            bashMultiCharOptions.add("restricted");
            bashMultiCharOptions.add("verbose");
            bashMultiCharOptions.add("version");
            bashMultiCharOptions.add("wordexp");
        }

        String processMe = processMe_param;
        int index = 0;
        if (processMe.startsWith("--")) {
            index = 2;
        } else if (processMe.startsWith("-")) {
            index = 1;
        }

        final boolean retval = (index > 0 && bashMultiCharOptions.contains(processMe.substring(index)));

        return retval;
    }

    /**
    * Examples of perverse shell input involving inbuilt commands (command, exec),
    * circular calls of shell calling a shell
    *   /bin/bash command exec /bin/ls -l file
    *   /bin/bash command exec csh command /bin/ls -l file
    *   /bin/bash command exec csh -c /bin/ls -l file
    *   /bin/bash command exec csh script
    *   /bin/bash command bash command exec /bin/ls -l
    *   /bin/bash exec /bin/csh -l file
    *   /bin/bash command csh -noprorfile -c -f /bin/ls -l file
    *
    * This method attempts, if are RUNNING_EXEC and SHELL_RUNS_THIS_COMMAND_CHK exists and processMe[0] is a shell,
    * to loop through cycles of shells calling shells plus this method loops through various possible shell
    * parameters to eventually identify the script or executable that the shell will run.
    * The index in processMe of that script or executable is returned to caller.
    * This methods may change any number of shell executable calls in processMe from
    *   ex: bash to /bin/bash
    * @param processMe[i] can each be mapped/unmapped - must unmap processMe[i when performing various tests
    * @return o -1 if the caller should not treat processMe as a shell executing something.
    *            Otherwise return the index in processMe of the script or non-shell executable eventually
    *            executed by the processMe[0]
    *         o  IS_SHELL_CMD_WITH_NO_PARAMS if processMe[0] is a shell and there are not parameters ie processMe.length is 1
    */
    private static int handlePossibleShellAndParams(final String[] processMe, final NotifyMsg notifyMsg,
                                                    final int runningShell, final int descIndex)
            throws MsgException {
        Chk chk; // "shell_runs_this_command"
        String last_shell_altered = null;
        String shell = null; // null means Runtime.exec is not execing a shell
        int i;

        if (descIndex == 0 && ((runningShell & RUNNING_EXEC) != 0)
                && ((chk = BaseConfig.getInstance().findChkFromName(SHELL_RUNS_THIS_COMMAND_CHK)) != null) // MsgException
                && ((shell = BaseConfig.getInstance().isShell(processMe[0])) != null)) { // null if not shell
                                                                                         // MsgException
            boolean alteredShell = false; // set true when "csh" is altered to "/bin/csh"
            String origShell = null;

            notifyMsg.setChk(chk);

            i = 0;
            while ((shell = BaseConfig.getInstance().isShell(processMe[i])) != null) {
                boolean equality = shell == processMe[i];
                if (!equality) { // if (shell != TaintUtils.taint(processMe[i])) {
                    alteredShell = true;
                    origShell = new String(processMe[i]);
                    ;

                    // Note shell was tainted above iff processMe[i] was tainted
                    processMe[i] = shell;

                    last_shell_altered = shell; // if alter > 1 shell, remember only the last one altered
                }

                // Case where processMe contains only shell command with not parameters
                if (processMe.length == 1) {
                    i = IS_SHELL_CMD_WITH_NO_PARAMS;
                    break;
                }

                i++;

                boolean cont = true;
                while (cont) {
                    cont = false;
                    final boolean equality_2 = (TaintUtils.stringEquals(processMe[i], "command")
                            || TaintUtils.stringEquals(processMe[i], "exec"));
                    if (equality_2) {
                        // Check if processMe[i] is untrusted?
                        i++;
                        cont = true;
                    } else if (isBashMultiCharOption(processMe[i])) { // bash-only: multi-character shell options
                        i++;
                        cont = true;
                    } else {
                        String pMe = processMe[i];
                        while (pMe.startsWith("-")) {
                            // Note: If -c is present, then the shell expects command to follow the -x param section
                            //       If no -c is found, a shell script can be expected
                            i++;
                            cont = true;
                            pMe = processMe[i];
                        }
                    }
                }

                // If cont is false here, it means processMe[i] is either a script (found_c is false)
                // or an executable (found_c is true)
            }

            if (alteredShell) {
                // Tell user of only the last shell altered
                notifyMsg.append("Altered a shell from ");
                notifyMsg.append(origShell);
                notifyMsg.append(" to ");
                notifyMsg.append(last_shell_altered);
                notifyMsg.append(".\n");

                notifyMsg.setAlteredShell(); // note for caller that shell was altered
            }

        } else { // processMe[0] is not a shell - or SHELL_RUNS_THIS_COMMAND_CHK is not defined in config file
            i = -1; // -1 signals caller not to assume processMe[0] is a shell
        }

        return i;
    }

    /**
    * This method replaces each untrusted $var found in processMe with its value.
    * @param rocessMe is a String may be surrounded by double quote chars. If so, processMe may have white space.
    * @param envMap A Map containing env vars
    *
    * @return processMe if no alteration were performed.
    *                   Else return processMe with each $var converted to its value
    */
    private static String expandDollarVar(final String processMe, final Map<String, String> envMap) {
        String processMe_unmapped = processMe;

        String newProcessMe = processMe;
        if (envMap != null) {
            final Set<String> keys = envMap.keySet();
            StringBuilder strBuf = TaintUtils.newStringBuilder();
            int processMe_copyFrom = 0;
            int dollar_index;
            int fromIndex = 0;
            while ((dollar_index = processMe_unmapped.indexOf('$', fromIndex)) != -1) {
                final String END_OF_TKN_CHARS_STR = " \t:,;()[]=<>&|?\'\""; // ":,;()[]=<>%&|?";
                int tkn_end;
                // Set tkn_end to end of $var
                for (tkn_end = dollar_index + 1; tkn_end < processMe.length()
                        && END_OF_TKN_CHARS_STR.indexOf(processMe_unmapped.charAt(tkn_end)) == -1; tkn_end++) {
                    ;
                }
                tkn_end--;

                // tkn_end indexes last char in $var
                if (tkn_end > dollar_index) {
                    // Replace $var with it value, regardless if $var is trusted/untrusted
                    final String key = TaintUtils.substring(processMe, dollar_index + 1, tkn_end + 1);
                    if (TaintUtils.stringSetContains(keys, key)) { // if (keys.contains(key)
                        final String val = TaintUtils.stringMapGet(envMap, key); // val = envMap.get(key);
                        if (val != null) {
                            // ORIG FIXED
                            // make String copy of val. taint it according as key is tainted.
                            //                          String valCopy = StringInstrumentation.init(val);
                            String valCopy = val;
                            if (TaintUtils.isTrusted(key)) {
                                valCopy = TaintUtils.trust(valCopy);
                            } else {
                                valCopy = TaintUtils.taint(valCopy);
                            }
                            // ORIG FIXED

                            TaintUtils.append(strBuf,
                                              TaintUtils.substring(processMe, processMe_copyFrom, dollar_index));
                            TaintUtils.append(strBuf, valCopy); // GOLD
                            processMe_copyFrom = tkn_end + 1;
                        }
                    }
                }

                // Set fromIndex to the next char after $var
                fromIndex = tkn_end + 1;
            } // while

            if (strBuf.length() > 0 && processMe_copyFrom < processMe.length()) {
                TaintUtils.append(strBuf, TaintUtils.substring(processMe, processMe_copyFrom, processMe.length()));
            }

            if (strBuf.length() > 0) {
                newProcessMe = TaintUtils.toString(strBuf);
            }
        }

        return newProcessMe;
    }

    /**
    * @param processMe is the arguments to shell -c. Separate this sting into tokens that
    *                  the shell will see. Some of the tokens will be single or double quote
    *                  enclosed strings. For each token that is not single quote enclosed,
    *                  search that token for $vars (either trusted or untrusted) and expand such $vars
    * @param envp If not null is the list of env vars the user application has sent to Runtime.exec(...)
    */
    private static String expandDollarVars(String processMe, final String[] envp) {
        String newProcessMe = processMe;
        final Map<String, String> envMap;
        if (envp == null) {
            envMap = SystemInstrumentation.getenv(new Ret());
        } else { // envp contains the env vars to be used by Runtime.exec running shell
            envMap = new HashMap<String, String>();
            for (int i = 0; i < envp.length; i++) {
                // each env entry looks like "HOME=/Users/dir"
                final String[] line = envp[i].split("=");
                if (line.length == 2) {
                    envMap.put(line[0], line[1]);
                } else {
                    Notify.log("odd environment variable %s in expandDollar\n", envp[i]);
                }
            }
        }

        String pMe = processMe;
        if (envMap != null && pMe.contains("$")) {
            boolean found_dollar = false; // Set true if any element in tokens is of form $var
                                          // and var is an environment variable

            final char[] TKN_CHARS_RAY = ":,;()[]=<>&|?".toCharArray(); // ":,;()[]=<>%&|?";
            final Tokenizer tokenizer = new Tokenizer();
            for (char t : TKN_CHARS_RAY) {
                tokenizer.addToken(t);
            }
            tokenizer.addQuotedBlock('\"', '\\');
            tokenizer.addQuotedBlock('\'', '\\');
            Tokenized tokenized = null;
            try {
                // Method 1:
                // Change all single quotes to trusted - So tokenize.getQuoteChar 
                // will return single quote char and $VAR will NOT be expanded
                //
                boolean altered = false;
                StringBuilder sb = new StringBuilder(processMe);
                Vector<Integer> untrusted_quote_positions = new Vector<Integer>();
                for (int k = 0; k < processMe.length(); k++) {
                    // If sb[k] is '\'  and   sb[k] is not trusted
                    if (sb.charAt(k) == '\'' && !TaintUtils.isTrusted(sb, k, k)) {

                        TaintUtils.trust(sb, 0, // inputType ?
                                         k, k);
                        untrusted_quote_positions.add(k);
                        altered = true;
                    }
                }

                if (altered) {
                    // processMe now will have its untrusted single quotes
                    // replaced with trusted single quotes
                    processMe = sb.toString();
                }

                // token.getQuoteChar() will return a quote char for any
                // token returned that is enclosed in trusted single quotes
                tokenized = tokenizer.tokenize(processMe, null); // IOException

                // Method 2:
                // Need to use the no-tracking tokenizer so that when a token is enclosed in either trusted
                // or tainted quotes, that token.getQuoteChar() below, will return the single quote char.
                // tokenizer.tokenize(processMe, null) will use taint, and so token.getQuoteChar() will
                // return single quote only if the token was enclosed in TRUSTED single quotes.
                // returned String will be totally unmapped, loosing taint
                // tokenized = tokenizer.tokenizeNoTracking(processMe, null); // IOException

                processMe = tokenized.getString();

                if (altered) {
                    // Each untrusted single quote chars in processMe that
                    // was replaced by trusted single quote chars, now needs
                    // to be re-converted back to untrusted single quote char.
                    sb = new StringBuilder(processMe);
                    for (int j = 0; j < untrusted_quote_positions.size(); j++) {
                        int sb_index = untrusted_quote_positions.elementAt(j);
                        char ch = sb.charAt(sb_index);
                        // ch is a trusted single quote.
                        // Replace ch it with untrusted single quote
                        if (TaintUtils.charEquals(ch, '\'')) {
                            TaintUtils.taint(sb, sb_index, sb_index);
                        } else {
                            System.out.println("Error: did not find a single quote in expected index:" + j
                                    + " in string:" + processMe);
                        }
                    }
                    processMe = sb.toString();
                }

            } catch (IOException e) {
                Notify.error("Exception: " + e + "\n");
                return processMe;
            }

            Tokenizer.Token token;

            // Each token in processMe will have an entry in newStrs. If a $var expansion has occurred in the
            // tokens[n], then newStrs[n] will be a string. If token[n] was not expanded, newStrs[n] will be null.
            final Vector<String> newStrs = new Vector<String>();
            int num_tokens;
            for (num_tokens = 0; (token = tokenized.getTokenClassAt(num_tokens)) != null; num_tokens++) {
                final String str = TaintUtils.substring(processMe, token.getStart(), token.getEnd() + 1);
                // If str contains $, and the $variable can be expanded (ie is enclosed in single quotes),
                // then expand the $variable, and put the result in token
                if (str.contains("$") &&
                // If str is enclosed in single quotes, getQuoteChar returns single quote char
                        !TaintUtils.charEquals(token.getQuoteChar(), '\'')) {
                    // If string is enclosed in single quotes, $var is not expanded
                    final String newStr = expandDollarVar(str, envMap);
                    if (newStr == str) {
                        newStrs.addElement(null);
                    } else {
                        newStrs.addElement(newStr);
                        found_dollar = true;
                    }
                } else {
                    newStrs.addElement(null);
                }
            }

            // If found a $var in any token of processMe, need to rebuild an alter processMe
            if (found_dollar) {
                final StringBuilder strBuf = TaintUtils.newStringBuilder();
                for (int i = 0; i < num_tokens; i++) {
                    String newStr = newStrs.elementAt(i);
                    token = tokenized.getTokenClassAt(i);

                    String blanks;
                    if (i == 0 && token.getStart() > 0) {
                        blanks = TaintUtils.substring(processMe, 0, token.getStart());
                        TaintUtils.append(strBuf, blanks);
                    }

                    if (newStr == null) { // token[i] was not altered - get the unchanged token
                        newStr = TaintUtils.substring(processMe, token.getStart(), token.getEnd() + 1);
                    }

                    TaintUtils.append(strBuf, newStr);

                    if (i < num_tokens - 1) {
                        // Copy any white space that exists between current token and next token
                        final Tokenizer.Token next_token = tokenized.getTokenClassAt(i + 1);
                        blanks = TaintUtils.substring(processMe, token.getEnd() + 1, next_token.getStart());
                        TaintUtils.append(strBuf, blanks);
                    } else if (token.getEnd() < processMe.length() - 1) { // are on the last token
                        // processMe ended with white space. Copy in that white space
                        blanks = TaintUtils.substring(processMe, token.getEnd(), processMe.length());
                        TaintUtils.append(strBuf, blanks);
                    }
                }

                if (strBuf.length() > 0) {
                    newProcessMe = TaintUtils.toString(strBuf);
                }
            }
        }

        return newProcessMe;
    }

    // Called from "Runtime.exec(String[])" and ProcessBuilder. In either case RUNNING_EXEC should be set in runningShell
    public static void runFromDescArray(final String[] processMe, final NotifyMsg notifyMsg, final int descIndex,
                                        final String[] envp)
            throws IOException {
        try {
            Chk chk; // "shell_runs_this_command"
            String shell = null; // null means Runtime.exec is not execing a shell
            String alteredProcessMe;

            final String description = notifyMsg.getDescription();

            // Return index in processMe of executable that shell runs
            final int processMeIndex = handlePossibleShellAndParams(processMe, notifyMsg, RUNNING_EXEC, descIndex); // MssgException

            if (processMeIndex > -1) {
                // Replace each untrusted $var with it value.
                alteredProcessMe = expandDollarVars(processMe[processMeIndex], envp);
                alteredProcessMe = runFromDesc(alteredProcessMe, notifyMsg, RUNNING_EXEC | RUNNING_SHELL, // RUNNING_EXEC is set because are being called
                                               //              exclusively from only Runtime.exe or ProcessBuilder
                                               0, envp); // IOException
                if (alteredProcessMe != processMe[processMeIndex]) {
                    processMe[processMeIndex] = alteredProcessMe;
                }

                // TODO processMe[0] is a shell and are using check "shell_runs_this_command on processMe[2]
                // What about if processMe[0] is not trusted

                /* ITWORKS - THE ORIGINAL WAY
                alteredProcessMe = chk.runFromChk(processMe[2], notifyMsg); // MsgException
                if (alteredProcessMe != processMe[2]) {
                    processMe[2] = alteredProcessMe;
                }
                */

            } else if (processMeIndex != IS_SHELL_CMD_WITH_NO_PARAMS) {
                // a) Are doing Runtime.exec(...) but:
                //      are not running shell
                //      or this call is not processing the param 0
                //      or are running shell and even processing param 0 but check shell_runs_this_command
                //         is not defined in config file
                // b) Are not doing Runtime.exec(...) and not doing ProcessBuilder
                //   Locate the Chk that matches "description".
                chk = BaseConfig.getInstance().findChkFromDescription(description, descIndex);
                if (chk != null) {
                    if (descIndex == 0) {
                        BaseConfig.getInstance().exeIsLeafInLegalDir(processMe[0], notifyMsg, envp); // MsgException if error reading config file
                    }
                    // Set RUNNING_EXEC because this method (runFromDescArray) is called only from
                    // Runtime.exec or ProcessBuilder.
                    // Set RUNNING_SHELL if shell is the program being executed.
                    final int runningShell = (RUNNING_EXEC | (shell == null ? 0 : RUNNING_SHELL));
                    runFromChkArray(processMe, chk, runningShell, notifyMsg); // MsgException IOException
                }
            }

        } catch (MsgException ex) {
            Notify.appendToLogFile(ex.getMessage(), 0); // cwe 0 means do not write msg to test harness
        }
    }

    /**
    *
    * @param processMe Two special cases:
    *                  can be cmd[2] in Runtime.exec(String[]cmd,...)
    *                  can be token[2] when cmd is tokenized in Runtime.exec(String cmd,...)
    * @param notifyMsg - contain the api description, used to locate the correct Chk to use
    *                    If being called from Runtime.exec(String[]cmd, ...), where cmd[0] is shell
    *                    and processMe is cmd[2], then should use the Chk stored in notifyMsg.
    * @param runningShell RUNNING_SHELL is set if caller knows Runtime.exec is execing a shell.
    *                     RUNNING_EXEC is set if are ultimately called from Runimte.exec or ProcessBuilder
    * @param descIndex  Identifies which parameter in description "run" is to process
    * @return The possibly altered (if action is REPLACE_ACTION) processMe String
    */
    public static String runFromDesc(final String processMe, final NotifyMsg notifyMsg, final int runningShell,
                                     final int descIndex, final String[] envp)
            throws IOException {

        String newProcessMe = processMe;

        try {
            Chk chk;
            final String description = notifyMsg.getDescription();
            // tokens will contain only contiguous non-white space
            // If processMe starts with white space, tokens[0] will be ""
            final String[] tokens = TaintUtils.split(processMe, TaintUtils.whiteSpaceRegExp() + "+");

            // Check that if tokens[0] is a leaf that it lives in a legal directory.
            // An trusted command having a full path is always considered existing in a legal directory.
            // An executable leaf file name, trusted or untrusted, must exist in one of the legal
            // directories specified in config file AND the first directory in PATH where the leaf is found
            // must be that legal directory.
            if ((runningShell & RUNNING_EXEC) != 0) {
                BaseConfig.getInstance().exeIsLeafInLegalDir(tokens[0], notifyMsg, envp); // MsgException if error reading config file
            }

            // Return index in processMe of executable that shell runs
            final int tokensIndex = handlePossibleShellAndParams(tokens, notifyMsg, RUNNING_EXEC, descIndex); // MssgException
            if (tokensIndex > -1) {
                if ((runningShell & RUNNING_EXEC) != 0) {
                    BaseConfig.getInstance().exeIsLeafInLegalDir(tokens[tokensIndex], notifyMsg, envp); // MsgException if error reading config file
                }

                // Starting from tokensIndex need to reassemble tokens back to a string.
                // So can send a string to expandDollarVars and runFromDesc
                // Each white_tokens elem will contain processMe white space
                // If processMe begins with non-white space, white_tokens[0]
                // will contain a zero-length entry.
                final String[] white_tokens = TaintUtils.split(processMe, "[^\\s]+");

                // re-assemble tokens[] and white_tokens[] into strBuf - for sending to runFromDesc
                StringBuilder strBuf = TaintUtils.newStringBuilder();

                // ray_1 and ray_2 - Use for reassembling the processMe from tokens and white_tokens.
                // One will hold non-white space tokens, the other will hold the white space chars that are between these tokens.
                // Set ray_1 to whichever begins with a non-zero length token. ray_2 will hold the other tokens.
                final String[] ray_1 = ((tokens.length == 0 || tokens[0].length() == 0) ? white_tokens : tokens);
                final String[] ray_2 = (ray_1 == tokens ? white_tokens : tokens);

                // ray_1_is_real_token is true if ray_1 contains the non-white space tokens
                final boolean ray_1_is_real_token = (ray_1[0].length() > 0);

                // Reassemble the tokens and white space starting at the executable or script the shell will be executing to end
                for (int i = tokensIndex; i < ray_1.length; i++) {
                    if (i < ray_1.length)
                        TaintUtils.append(strBuf, ray_1_is_real_token ? ray_1[i] : ray_2[i]);
                    if (i + 1 < ray_2.length) // ray_2[0] is "".  Skip the first token of ray_2
                        TaintUtils.append(strBuf, ray_1_is_real_token ? ray_2[i + 1] : ray_1[i + 1]);
                }
                String shellArgs = TaintUtils.toString(strBuf);

                // Replace each untrusted $var with it value.
                shellArgs = expandDollarVars(shellArgs, envp);

                final String shellArgs2 = runFromDesc(shellArgs, notifyMsg, runningShell | RUNNING_SHELL, descIndex,
                                                      envp); // IOException

                // If handlePossibleShellAndParam altered any shell call
                // Or if shellArgs2 differs from shellArgs, then must reassemble a new newProcessMe
                // Otherwise can leave newProcessMe as it is - set to processMe
                if (notifyMsg.getAlteredShell() || shellArgs != shellArgs2) {
                    strBuf = TaintUtils.newStringBuilder();
                    for (int i = 0; i < tokensIndex; i++) {
                        // Reassemble the tokens and white space from 0 until get to tokensIndex - which is
                        // the executable and args that the shell will be running
                        if (i < ray_1.length)
                            TaintUtils.append(strBuf, ray_1[i]);
                        if (i + 1 < ray_2.length) // ray_2[0] is "".  Skip the first token of ray_2
                            TaintUtils.append(strBuf, ray_2[i + 1]);
                    }
                    TaintUtils.append(strBuf, shellArgs2);
                    newProcessMe = TaintUtils.toString(strBuf);
                }

            } else if (tokensIndex != IS_SHELL_CMD_WITH_NO_PARAMS) {

                // If notifyMsg.chk is set - and it's name is "shell_runs_this_command", it means that
                // are being called to process argument to a shell
                // ie processMe is cmd[2] in Runtime.exec(String[]cmd, ....)
                //              cmd[0]==/bin/csh    cmd[1]==-c     cmd[2]==<argument>
                // argument can itself be divided into multiple shell commands separated from each other
                // by ";" or separated from each other by one of the other divider chars.
                chk = notifyMsg.getChk();
                if (chk != null && chk.check_name.equals(SHELL_RUNS_THIS_COMMAND_CHK)) {
                    final String newProcessMe2 = divideOnTrustedCmdSeparatorStrings(processMe, runningShell, notifyMsg);
                    newProcessMe = (newProcessMe2 == null ? // newProcessMe2 == null means were no dividers
                            chk.runFromChk(processMe, null, runningShell, notifyMsg) : // Was no divider.
                            newProcessMe2); // There were dividers. newProcessMe2 is processMe
                                            // with dividers processed
                } else { // processMe is not the cmd[2] where cmd[0] is a shell
                    chk = BaseConfig.getInstance().findChkFromDescription(description, descIndex);
                    if (chk != null) {
                        newProcessMe = chk.runFromChk(processMe, null, runningShell, notifyMsg); // MssgException  IOException
                    }
                }

                /* ITWORKS - THE ORIG WAY
                chk = findChkFromDescription(description, descIndex);
                if (chk != null) {
                    newProcessMe = chk.runFromChk(processMe, notifyMsg);
                }
                */
            }

        } catch (MsgException ex) {
            Notify.appendToLogFile(ex.getMessage(), 0); // cwe 0 means the error has nothing to do with any attack
        }

        return newProcessMe;
    }

    public static boolean checkLegalFileName(File file, NotifyMsg notifyMsg) {
        boolean retval;
        try {
            final String canonicalPath = file.getCanonicalPath(); // IOException
            retval = checkLegalFileName(canonicalPath, notifyMsg);
        } catch (IOException ex) {
            Notify.log("exception occured when performing legal filename check: %s\n", ex.toString());
            retval = false;
        }

        return retval;
    }

    private static boolean checkLegalFileName(String canonicalPath, NotifyMsg notifyMsg) {
        boolean legalFile = false;
        final Chk chk = BaseConfig.getInstance().findChkFromName("trusted_stream_files");

        if (chk != null) {
            legalFile = chk.runTrustedDataSource(canonicalPath, notifyMsg);
        }

        return legalFile;
    }

    // Return true if config file lists param path as trusted.
    public static boolean checkLegalFileName(final FileDescriptor fd, final String path, NotifyMsg notifyMsg) {
        return checkLegalFileName(fd, new File(path), notifyMsg);
    }

    /**
    * Test if the canonical path of File is listed as a trusted location in config file
    * If the path not not trusted, mark obj as untrusted
    *
    * @param obj  Is a FileInputStream or a FileReader.
    *             Mark this object as tainted if the canonical path of file is NOT
    *             listed in configfile as a legal file
    *             obj is File, or FileInputStream, or FileWriter etc whose path is
    *             specifiec by the file parameter
    * @param file Represents the path fo the inputstream/filereader object that is being created
    * @param notifyMsg Append message here
    * @returns true if config_file lists the canonical path of File as trusted
    */
    public static boolean checkLegalFileName(final FileDescriptor fd, final File file, final NotifyMsg notifyMsg) {
        try {
            boolean legalFile = false;
            final Chk chk = BaseConfig.getInstance().findChkFromName("trusted_stream_files"); // MsgException

            int trust;
            if (chk != null) {
                final String canonicalPath = file.getCanonicalPath(); // IOException
                legalFile = chk.runTrustedDataSource(canonicalPath, notifyMsg);
                if (legalFile) {
                    trust = TaintValues.FILE | TaintValues.TRUSTED;
                } else {
                    trust = TaintValues.FILE | TaintValues.TAINTED;
                }
            } else {
                trust = TaintValues.FILE | TaintValues.TAINTED;
            }

            fd.fd_t = trust;
            return legalFile;

        } catch (IOException ex) {
            // To be safe let's make this unknown and assume it's not a legal file
            int trust = TaintValues.FILE | TaintValues.UNKNOWN;

            fd.fd_t = trust;
            return false;
        }
    }

    /**
     * Test if the property is listed as a trusted property in the config file
     * If the this property is trusted, then trust the value.  Otherwise taint
     * the value.
     *
     * @param property String of the property name. For char taint must be mapped/unmapped
     * @param value String of the property value.
     * @param notifyMsg Append message here
     * @returns the possibly new value String mapped/unmapped
     * @throws MsgException if error opening/reading config file
     */
    public static String checkLegalProperty(String property, String value, NotifyMsg notifymsg) {
        boolean legalProp = false;
        Chk chk = null;
        try {
            chk = BaseConfig.getInstance().findChkFromName("trusted_properties"); // MsgException
        } catch (Exception e) {
        }
        if (chk != null) {
            legalProp = chk.runTrustedDataSource(property, notifymsg);
            if (legalProp) {
                value = TaintUtils.trust(value, TaintValues.PROPERTY);
                /* Can cause recursion when called from getproperty. Appending to log file calls getproperty
                notifymsg.append("Setting property value " + value + " to trusted.\n");
                */
            } else if (!TaintUtils.isTracked(value)) {
                value = TaintUtils.taint(value, TaintValues.PROPERTY);
                /* Can cause recursion
                notifymsg.append("Setting property value " + value + " to tainted.\n");
                */
            }
        } else {
            value = TaintUtils.taint(value, TaintValues.PROPERTY);
            /* Can cause recursion
            notifymsg.append("\"trusted_properties\" not found. Setting property value " + value + " to tainted.\n");
            */
        }

        return value;
    }

    public static void main(String[] args) {
        //                  ____       ______         _________
        String str = "$one \"two $three\" $four \"$HOME\"   ' $HOME;ace ' \"$HOME\"";

        TaintUtils.trust(str);

        str = TaintUtils.taint(str, 0, 3);
        str = TaintUtils.taint(str, 10, 15);
        str = TaintUtils.taint(str, 24, 30);

        final String ret = expandDollarVars(str, null);
        System.out.println(str);
        System.out.println(ret);
        /*
        try {
            String[]processMe = new String[16];
            String cmd = Utils.arrayToString(processMe);
            NotifyMsg notifyMsg = new NotifyMsg("Runtime.exec(String, String[])", cmd);
        
            // /bin/bash command csh -noprofile -c -f /bin/ls -l file
        
            // /bin/bash command exec /bin/ls -l file
            processMe[0] = "bash";
            processMe[1] = "command";
            processMe[2] = "exec";
            processMe[3] = "/bin/ls";
            processMe[4] = "-l";
            processMe[5] = "/bin/pwd";
        
            int index = handlePossibleShellAndParams(processMe, notifyMsg, RUNNING_EXEC, 0); // MsgException
            System.out.println(index);
        } catch (MsgException ex) {
            System.out.println(ex.getMessage());
        }
        */
    }
}
