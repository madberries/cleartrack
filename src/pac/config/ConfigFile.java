package pac.config;

import static pac.config.ConfigFileTokens.ALLOWED_MIMETYPES_TKN;
import static pac.config.ConfigFileTokens.CAN_SET_CATALOG_WITH_UNTRUSTED_DATA_TKN;
import static pac.config.ConfigFileTokens.CAN_SET_CATALOG_WITH_UNTRUSTED_NAME_OR_VALUE_TKN;
import static pac.config.ConfigFileTokens.CAN_SET_CATALOG_WITH_UNTRUSTED_PROPERTIES_TKN;
import static pac.config.ConfigFileTokens.CAN_SET_SAVEPOINT_WITH_UNTRUSTED_NAME_TKN;
import static pac.config.ConfigFileTokens.CAN_SET_TYPEMAP_WITH_UNTRUSTED_MAP_TKN;
import static pac.config.ConfigFileTokens.CHECK_TKN;
import static pac.config.ConfigFileTokens.CLIENT_SOCKET_TIMEOUT_TKN;
import static pac.config.ConfigFileTokens.DEADLOCKS_ENABLED_TKN;
import static pac.config.ConfigFileTokens.DESC_TKN;
import static pac.config.ConfigFileTokens.END_TKN;
import static pac.config.ConfigFileTokens.INFINITY_PRINT_TKN;
import static pac.config.ConfigFileTokens.INFINITY_TKN;
import static pac.config.ConfigFileTokens.LOG_FILE_TKN;
import static pac.config.ConfigFileTokens.LOOP_CHECK_DEF_TKN;
import static pac.config.ConfigFileTokens.LOOP_CHECK_TKN;
import static pac.config.ConfigFileTokens.MALFORMED_HTTP_REQUEST_ACTION_TKN;
import static pac.config.ConfigFileTokens.MAX_ARRAY_ALLOC_TKN;
import static pac.config.ConfigFileTokens.MAX_FILES_TKN;
import static pac.config.ConfigFileTokens.MAX_MEMORY_TKN;
import static pac.config.ConfigFileTokens.MAX_STACK_SIZE_TKN;
import static pac.config.ConfigFileTokens.NULL_BYTE_ACTION_TKN;
import static pac.config.ConfigFileTokens.OVERFLOW_PRINT_TKN;
import static pac.config.ConfigFileTokens.OVERFLOW_TKN;
import static pac.config.ConfigFileTokens.SERVER_SOCKET_TIMEOUT_TKN;
import static pac.config.ConfigFileTokens.SHELL_LIST_TKN;
import static pac.config.ConfigFileTokens.TAINTED_REDIRECT_ACTION_TKN;
import static pac.config.ConfigFileTokens.TAINT_ARGUMENTS_TKN;
import static pac.config.ConfigFileTokens.TAINT_DB_READS_TKN;
import static pac.config.ConfigFileTokens.TAINT_ENV_VARS_TKN;
import static pac.config.ConfigFileTokens.TEST_HARNESS_ENABLED_TKN;
import static pac.config.ConfigFileTokens.TOCTOU_ACTION_TKN;
import static pac.config.ConfigFileTokens.TOKENIZER_TKN;
import static pac.config.ConfigFileTokens.TRUSTED_EXCEPTION_OUTPUT_TKN;
import static pac.config.ConfigFileTokens.TRUSTED_EXEC_PATHS_ACTION_TKN;
import static pac.config.ConfigFileTokens.TRUSTED_EXEC_PATHS_TKN;
import static pac.config.ConfigFileTokens.TRUSTED_PORTS_TKN;
import static pac.config.ConfigFileTokens.UNALLOWED_MIMETYPE_ACTION_TKN;
import static pac.config.ConfigFileTokens.UNDERFLOW_PRINT_TKN;
import static pac.config.ConfigFileTokens.UNDERFLOW_TKN;
import static pac.config.ConfigFileTokens.UNTRUSTED_KEY_CAN_ACCESS_PROPERTY_TKN;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import pac.org.objectweb.asm.Opcodes;
import pac.org.objectweb.asm.Type;
import pac.org.objectweb.asm.tree.ClassNode;
import pac.org.objectweb.asm.tree.FieldInsnNode;
import pac.org.objectweb.asm.tree.InsnList;
import pac.org.objectweb.asm.tree.InsnNode;
import pac.org.objectweb.asm.tree.IntInsnNode;
import pac.org.objectweb.asm.tree.LdcInsnNode;
import pac.org.objectweb.asm.tree.MethodInsnNode;
import pac.org.objectweb.asm.tree.MethodNode;
import pac.org.objectweb.asm.tree.TypeInsnNode;
import pac.org.objectweb.asm.tree.VarInsnNode;
import pac.util.Ansi;
import pac.util.AsmUtils;

/**
 * Represents the configuration file for a particular run of the instrumenter.  This
 * class is responsible for parsing the appropriate configuration file and translating
 * it into Java bytecode, which will then be loaded at application runtime.
 * 
 * @author jeikenberry
 */
public class ConfigFile extends AbstractConfig {
    protected static ConfigFile INSTANCE;

    /** contains a mapping of strings (values) that should be parsed by the particular object (key)*/
    protected HashMap<Object, String> buildStrMap;

    private ConfigFile() {
        untrusted_key_can_access_property_value = false;
        taint_env_vars = true;
        taint_arguments = true;
        taint_db_reads = true;
        trustAllPorts = false;
        server_socket_timeout = -1;
        client_socket_timeout = -1;

        // Default action when encountering null
        // bytes is to remove them...
        null_byte_action = RunChecks.REMOVE_ACTION;
        null_byte_replace_char = 0;

        // Default resource consumption values
        toctou_action = RunChecks.LOG_ONLY_ACTION;
        max_memory = 0.75f;
        max_files = 1024;

        LineNumberReader reader = null;

        final String configFile = getPath(); // Note: Avoid calling new File when reading config file, because the instrumented File
                                             //       constructor calls readConfigFile at start of program causing infinite loop

        buildStrMap = new HashMap<Object, String>();
        methodDescMap = new HashMap<String, Desc>();
        configChksMap = new HashMap<String, Chk>();
        tokenizers = new HashMap<String, Tokenizer>();
        trustedExecPaths = new Vector<String>();
        trustedPortsSet = new HashSet<Integer>();

        try {
            // Pass one thru config file
            // Read thru config file collecting check names
            reader = Utils.getLineNumberReader(configFile); // MsgException
            Vector<String> knownCheckNames = readConfig_collectCheckNames(reader); // MsgException
            reader.close(); // IOException

            // Pass two thru config file
            // Reopen line reader and read lines of config file
            reader = Utils.getLineNumberReader(configFile); // MsgException
            String line;
            Chk doingCheck = null;
            Tokenizer doingTokenizer = null;
            while ((line = reader.readLine()) != null) { // IOException
                                                         // IllegalArgumentException
                try {
                    line = line.trim();

                    if (!line.startsWith("#") && line.length() > 0) {
                        final String firstTkn = Utils.getTokenAtIndex(line, 0); // MsgException

                        if (firstTkn.equals(END_TKN)) {
                            String tkn = Utils.getTokenAtIndex(line, 1);
                            if (tkn.equals(CHECK_TKN)) {
                                doingCheck.debug_verify(); // MsgException
                                doingCheck = null;
                            } else if (tkn.equals(TOKENIZER_TKN)) {
                                doingTokenizer = null;
                            }
                        } else if (doingCheck != null) {
                            doingCheck.buildChk_parseLine(line, knownCheckNames, this); // MsgException
                            String str = buildStrMap.get(doingCheck);
                            if (str == null)
                                str = "";
                            str = str + line + "\n";
                            buildStrMap.put(doingCheck, str);
                        } else if (doingTokenizer != null) {
                            doingTokenizer.buildTkn_parseLine(line);
                            String str = buildStrMap.get(doingTokenizer);
                            if (str == null)
                                str = "";
                            str = str + line + "\n";
                            buildStrMap.put(doingTokenizer, str);
                        } else if (firstTkn.matches(DESC_TKN + "(\\{\\w+\\})?")) {
                            String descStr = line.substring(DESC_TKN.length()).trim();
                            final Desc desc = Desc.createDesc(descStr, this);
                            methodDescMap.put(desc.command, desc);
                            buildStrMap.put(desc, descStr);
                        } else if (firstTkn.equals(TRUSTED_EXEC_PATHS_TKN)) { // line looks like "trusted_exec_paths path1:path2:path3"
                            buildTrustedPaths(line); // fill global trustedExecPaths
                        } else if (firstTkn.equals(TRUSTED_EXEC_PATHS_ACTION_TKN)) { // line looks like: "trusted_exec_paths_action log_only"
                            read_trusted_exec_paths_action_line(line); //                  "trusted_exec_paths_action exception <RuntimeException"
                                                                       //                  "trusted_exec_paths_action terminate"
                        } else if (firstTkn.equals(CHECK_TKN)) {
                            final String checkName = Utils.getTokenAtIndex(line, 1); // MsgException
                            doingCheck = getCheck(checkName);
                        } else if (firstTkn.equals(TOKENIZER_TKN)) {
                            final String tokenizerName = Utils.getTokenAtIndex(line, 1);
                            doingTokenizer = new Tokenizer();
                            tokenizers.put(tokenizerName, doingTokenizer);
                        } else if (firstTkn.equals(LOG_FILE_TKN)) {
                            log_file = Utils.getTokenAtIndex(line, 1); // MsgException
                        } else if (firstTkn.equals(SHELL_LIST_TKN)) {
                            shells = readConfig_createShellList(line); // Create Vector of shell names
                        } else if (firstTkn.equals(LOOP_CHECK_TKN)) {
                            buildLoopCheck(line, false);
                        } else if (firstTkn.equals(LOOP_CHECK_DEF_TKN)) {
                            buildLoopCheck(line, true);
                        } else if (firstTkn.equals(MAX_STACK_SIZE_TKN)) {
                            buildRecursiveCheck(line);
                        } else if (firstTkn.equals(ALLOWED_MIMETYPES_TKN)) {
                            buildMimeTypes(reader);
                        } else if (firstTkn.equals(UNALLOWED_MIMETYPE_ACTION_TKN)) {
                            read_unallowed_mimetype_action(line);
                        } else if (firstTkn.equals(NULL_BYTE_ACTION_TKN)) {
                            read_null_byte_action(line);
                        } else if (firstTkn.equals(TRUSTED_EXCEPTION_OUTPUT_TKN)) {
                            read_trusted_exception_output_action(line);
                        } else if (firstTkn.equals(TAINTED_REDIRECT_ACTION_TKN)) {
                            read_tainted_redirect_action(line);
                        } else if (firstTkn.equals(MALFORMED_HTTP_REQUEST_ACTION_TKN)) {
                            read_malformed_http_request_action(line);
                        } else if (firstTkn.equals(UNTRUSTED_KEY_CAN_ACCESS_PROPERTY_TKN)) {
                            String booleanVal = Utils.getTokenAtIndex(line, 1);
                            untrusted_key_can_access_property_value = Boolean.parseBoolean(booleanVal);
                        } else if (firstTkn.equals(TAINT_ARGUMENTS_TKN)) {
                            String booleanVal = Utils.getTokenAtIndex(line, 1);
                            taint_arguments = Boolean.parseBoolean(booleanVal);
                        } else if (firstTkn.equals(TAINT_ENV_VARS_TKN)) {
                            String booleanVal = Utils.getTokenAtIndex(line, 1);
                            taint_env_vars = Boolean.parseBoolean(booleanVal);
                        } else if (firstTkn.equals(TAINT_DB_READS_TKN)) {
                            String booleanVal = Utils.getTokenAtIndex(line, 1);
                            taint_db_reads = Boolean.parseBoolean(booleanVal);
                        } else if (firstTkn.equals(TRUSTED_PORTS_TKN)) {
                            String[] params = line.split("\\s+");
                            for (int i = 1; i < params.length; i++) {
                                if (params[i].equals("*")) {
                                    trustAllPorts = true;
                                    break;
                                }

                                try {
                                    int port = Integer.parseInt(params[i]);
                                    trustedPortsSet.add(port);
                                } catch (NumberFormatException e) {

                                }
                            }
                        } else if (firstTkn.equals(TEST_HARNESS_ENABLED_TKN)) {
                            String boolStr = Utils.getTokenAtIndex(line, 1); // MssgException
                            test_harness_enabled = Boolean.parseBoolean(boolStr);
                        } else if (firstTkn.equals(CAN_SET_CATALOG_WITH_UNTRUSTED_DATA_TKN)) {
                            String boolStr = Utils.getTokenAtIndex(line, 1);
                            can_set_catalog_with_untrusted_data = Boolean.parseBoolean(boolStr);
                        } else if (firstTkn.equals(CAN_SET_CATALOG_WITH_UNTRUSTED_PROPERTIES_TKN)) {
                            String boolStr = Utils.getTokenAtIndex(line, 1);
                            can_set_clientinfo_with_untrusted_properties = Boolean.parseBoolean(boolStr);
                        } else if (firstTkn.equals(CAN_SET_CATALOG_WITH_UNTRUSTED_NAME_OR_VALUE_TKN)) {
                            String boolStr = Utils.getTokenAtIndex(line, 1);
                            can_set_clientinfo_with_untrusted_name_or_value = Boolean.parseBoolean(boolStr);
                        } else if (firstTkn.equals(CAN_SET_SAVEPOINT_WITH_UNTRUSTED_NAME_TKN)) {
                            String boolStr = Utils.getTokenAtIndex(line, 1);
                            can_set_savepoint_with_untrusted_name = Boolean.parseBoolean(boolStr);
                        } else if (firstTkn.equals(CAN_SET_TYPEMAP_WITH_UNTRUSTED_MAP_TKN)) {
                            String boolStr = Utils.getTokenAtIndex(line, 1);
                            can_set_typemap_with_untrusted_map = Boolean.parseBoolean(boolStr);
                        } else if (firstTkn.equals(SERVER_SOCKET_TIMEOUT_TKN)) {
                            String intValue = Utils.getTokenAtIndex(line, 1);
                            server_socket_timeout = Integer.parseInt(intValue);
                        } else if (firstTkn.equals(CLIENT_SOCKET_TIMEOUT_TKN)) {
                            String intValue = Utils.getTokenAtIndex(line, 1);
                            client_socket_timeout = Integer.parseInt(intValue);
                        } else if (firstTkn.equals(OVERFLOW_TKN)) {
                            String action = Utils.getTokenAtIndex(line, 1);
                            overflow_action = RunChecks.actionStringToInt(action);
                            if (overflow_action == RunChecks.EXCEPTION_ACTION) {
                                overflow_exception = Utils.buildExceptionConstructor(Utils.getTokenAtIndex(line, 2));
                            }
                        } else if (firstTkn.equals(UNDERFLOW_TKN)) {
                            String action = Utils.getTokenAtIndex(line, 1);
                            underflow_action = RunChecks.actionStringToInt(action);
                            if (underflow_action == RunChecks.EXCEPTION_ACTION) {
                                underflow_exception = Utils.buildExceptionConstructor(Utils.getTokenAtIndex(line, 2));
                            }
                        } else if (firstTkn.equals(INFINITY_TKN)) {
                            String action = Utils.getTokenAtIndex(line, 1);
                            infinity_action = RunChecks.actionStringToInt(action);
                            if (infinity_action == RunChecks.EXCEPTION_ACTION) {
                                infinity_exception = Utils.buildExceptionConstructor(Utils.getTokenAtIndex(line, 2));
                            }
                        } else if (firstTkn.equals(OVERFLOW_PRINT_TKN)) {
                            String action = Utils.getTokenAtIndex(line, 1);
                            overflow_print_action = RunChecks.actionStringToInt(action);
                            if (overflow_print_action == RunChecks.REPLACE_ACTION) {
                                overflow_print_replace = Utils.getTokenAtIndex(line, 2);
                            }
                        } else if (firstTkn.equals(UNDERFLOW_PRINT_TKN)) {
                            String action = Utils.getTokenAtIndex(line, 1);
                            underflow_print_action = RunChecks.actionStringToInt(action);
                            if (underflow_print_action == RunChecks.REPLACE_ACTION) {
                                underflow_print_replace = Utils.getTokenAtIndex(line, 2);
                            }
                        } else if (firstTkn.equals(INFINITY_PRINT_TKN)) {
                            String action = Utils.getTokenAtIndex(line, 1);
                            infinity_print_action = RunChecks.actionStringToInt(action);
                            if (infinity_print_action == RunChecks.REPLACE_ACTION) {
                                infinity_print_replace = Utils.getTokenAtIndex(line, 2);
                            }
                        } else if (firstTkn.equals(MAX_ARRAY_ALLOC_TKN)) {
                            max_array_alloc_value = Integer.parseInt(Utils.getTokenAtIndex(line, 1));
                            String action = Utils.getTokenAtIndex(line, 2);
                            max_array_alloc_action = RunChecks.actionStringToInt(action);
                            if (max_array_alloc_action == RunChecks.EXCEPTION_ACTION) {
                                max_array_alloc_exception = Utils
                                        .buildExceptionConstructor(Utils.getTokenAtIndex(line, 3));
                            }
                        } else if (firstTkn.equals(DEADLOCKS_ENABLED_TKN)) {
                            String boolStr = Utils.getTokenAtIndex(line, 1); // MssgException
                            deadlocks_enabled = Boolean.parseBoolean(boolStr);
                        } else if (firstTkn.equals(TOCTOU_ACTION_TKN)) {
                            String action = Utils.getTokenAtIndex(line, 1);
                            toctou_action = RunChecks.actionStringToInt(action);
                            if (toctou_action == RunChecks.EXCEPTION_ACTION) {
                                toctou_exception_constructor = Utils
                                        .buildExceptionConstructor(Utils.getTokenAtIndex(line, 2));
                            }
                        } else if (firstTkn.equals(MAX_MEMORY_TKN)) {
                            max_memory = Float.parseFloat(Utils.getTokenAtIndex(line, 1));
                        } else if (firstTkn.equals(MAX_FILES_TKN)) {
                            max_files = Integer.parseInt(Utils.getTokenAtIndex(line, 1));
                        } else { // Unknown token
                            throw new MsgException("Unknown token: " + Utils.getTokenAtIndex(line, 0)); // MsgException
                        }
                    }

                } catch (MsgException ex) {
                    Ansi.error("unable to parse config file '%s' on line %d: %s", null, getPath(),
                               reader.getLineNumber(), ex.getMessage());
                    System.exit(1);
                }
            }

        } catch (MsgException | IOException ex) {
            Ansi.error("unable to parse config file '%s' on line %d: %s", null, getPath(), reader.getLineNumber(),
                       ex.getMessage());
            System.exit(1);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception io) {
            }
        }
    }

    /**
    * Fill globals: methodDescs, configChks, shells, tokenizers, trustedExecPaths, Notify.logFile
    *
    * Run checks insuring "integrity" of the config data read.
    * ex: All references to check names refer to config-file-defined checks
    * ex: If action is "replace", the replace char is specified
    * ex; If action is exception, that an exception is specified
    *                             and can actually create a exception class from the specified exception
    *                             and that that exception class can be instantiated and thrown
    * Note: Do not use File to read config_inst. The File constructor at start of program will
    *       try to read config_inst. If File constructor is then called, will attempt to read
    *       config_inst file ... infinite loop
    *
    * @return true if when this method has returned the config file is successfully read
    *         false if either at this call or some previous call, there was error reading
    *         the config file
    */
    public synchronized static ConfigFile getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ConfigFile();
        }
        return INSTANCE;
    }

    private void buildRecursiveCheck(String line) throws MsgException {
        final String[] lineRay = line.split("\\s+"); // PatternSyntaxException

        if (lineRay.length < 3) {
            throw new MsgException(
                    "Recursive checks requires at least 3 tokens. There are " + lineRay.length + " tokens.");
        }
        // max_stack_size 2000 

        recursiveMaxStackSize = Integer.parseInt(lineRay[1]);
        recursiveMaxStackAction = RunChecks.actionStringToInt(lineRay[2]);

        if (recursiveMaxStackAction == RunChecks.EXCEPTION_ACTION) {
            if (lineRay.length < 4) {
                throw new MsgException(
                        "The action for " + lineRay[0] + " is " + RunChecks.actionIntToString(loopCheckAction) + // MsgException
                                ".\nExpected a subclass of RuntimeException to follow "
                                + RunChecks.actionIntToString(loopCheckAction));
            }

            recursiveMaxStackException = Utils.buildExceptionConstructor(lineRay[lineRay.length - 1]); // MsgException
        }
    }

    /**
     * Parses the loop_check command and initializes the appropriate
     * member variables.
     * @param line String of the line to parse.
     * @throws MsgException
     */
    private void buildLoopCheck(String line, boolean definite) throws MsgException {
        final String[] lineRay = line.split("\\s+"); // PatternSyntaxException

        if (lineRay.length < 3) {
            throw new MsgException("Loop checks requires at least 3 tokens. There are " + lineRay.length + " tokens.");
        }

        if (definite) {
            loopCheckMaxItersDef = Integer.parseInt(lineRay[1]);
            try {
                // Check to see if there is a timeout arg
                loopCheckMaxTimeDef = Integer.parseInt(lineRay[2]) * 1000;
                loopCheckActionDef = RunChecks.actionStringToInt(lineRay[3]);
            } catch (NumberFormatException e) {
                // No timeout arg
                loopCheckActionDef = RunChecks.actionStringToInt(lineRay[2]);
            }
        } else {
            loopCheckMaxIters = Integer.parseInt(lineRay[1]);
            try {
                // Check to see if there is a timeout arg
                loopCheckMaxTime = Integer.parseInt(lineRay[2]) * 1000;
                loopCheckAction = RunChecks.actionStringToInt(lineRay[3]);
            } catch (NumberFormatException e) {
                // No timeout arg
                loopCheckAction = RunChecks.actionStringToInt(lineRay[2]);
            }
        }

        if (loopCheckAction == RunChecks.EXCEPTION_ACTION) {
            if (lineRay.length < 4) {
                throw new MsgException(
                        "The action for " + lineRay[0] + " is " + RunChecks.actionIntToString(loopCheckAction) + // MsgException
                                ".\nExpected a subclass of RuntimeException to follow "
                                + RunChecks.actionIntToString(loopCheckAction));
            }

            if (definite)
                loopCheckExceptionDef = Utils.buildExceptionConstructor(lineRay[lineRay.length - 1]); // MsgException
            else
                loopCheckException = Utils.buildExceptionConstructor(lineRay[lineRay.length - 1]); // MsgException
        }
    }

    private void buildMimeTypes(LineNumberReader reader) throws IOException, MsgException {
        mimeTypesMap = new HashMap<String, Set<String>>();
        contentTypesAllowed = new HashSet<String>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (Utils.getTokenAtIndex(line, 0).equals(END_TKN)
                    && Utils.getTokenAtIndex(line, 1).equals(ALLOWED_MIMETYPES_TKN))
                break;

            String[] splitStr = line.trim().split("\\s*:\\s*");
            if (splitStr.length != 2)
                throw new MsgException("Error parsing '" + line + "'.  Expecting a line of the form '<file-ext>, ..., "
                        + "<file-ext>: <content-type>, ..., <content-type>'");

            Set<String> contentTypes = new HashSet<String>(Arrays.asList(splitStr[1].split("\\s*,\\s*")));
            contentTypesAllowed.addAll(contentTypes);
            for (String fileExt : splitStr[0].split("\\s*,\\s*")) {
                mimeTypesMap.put(fileExt, contentTypes);
            }
        }
    }

    private void read_unallowed_mimetype_action(final String line) throws MsgException {
        final String action = Utils.getTokenAtIndex(line, 1); // MsgException

        if (RunChecks.LOG_ONLY_ACTION_TKN.equals(action)) {
            unallowed_mimetype_action = RunChecks.LOG_ONLY_ACTION;
        } else if (RunChecks.EXCEPTION_ACTION_TKN.equals(action)) {
            unallowed_mimetype_action = RunChecks.EXCEPTION_ACTION;

            // line looks like: "unallowed_mimetype_action exception some.runtime.exception"
            final String spec = Utils.getTokenAtIndex(line, 2); // MsgException
            unallowed_mimetype_exception_constructor = Utils.buildExceptionConstructor(spec); // MsgException

        } else if (RunChecks.JAIL_ACTION_TKN.equals(action)) {
            unallowed_mimetype_action = RunChecks.JAIL_ACTION;

            // line looks like: "unallowed_mimetype_action exception some.runtime.exception"
            final String spec = Utils.getTokenAtIndex(line, 2); // MsgException
            unallowed_mimetype_jail_file = spec; // MsgException
            File file = new File(unallowed_mimetype_jail_file);
            if (!file.exists()) {
                if (!file.mkdirs())
                    throw new MsgException("Unable to create jail directory '" + unallowed_mimetype_jail_file
                            + "' for untrusted mimetypes.");
            } else if (!file.isDirectory())
                throw new MsgException(
                        "Jail path '" + unallowed_mimetype_jail_file + "' refers to a file and not a directory.");
        } else if (RunChecks.TERMINATE_ACTION_TKN.equals(action)) {
            unallowed_mimetype_action = RunChecks.TERMINATE_ACTION;

        } else {
            throw new MsgException("Unknown arguments follow " + UNALLOWED_MIMETYPE_ACTION_TKN);
        }
    }

    private void read_null_byte_action(final String line) throws MsgException {
        final String action = Utils.getTokenAtIndex(line, 1); // MsgException

        if (RunChecks.LOG_ONLY_ACTION_TKN.equals(action)) {
            null_byte_action = RunChecks.LOG_ONLY_ACTION;
            null_byte_replace_char = -1;
        } else if (RunChecks.EXCEPTION_ACTION_TKN.equals(action)) {
            null_byte_action = RunChecks.EXCEPTION_ACTION;

            // line looks like: "unallowed_mimetype_action exception some.runtime.exception"
            final String spec = Utils.getTokenAtIndex(line, 2); // MsgException
            null_byte_exception_constructor = Utils.buildExceptionConstructor(spec); // MsgException

        } else if (RunChecks.REPLACE_ACTION_TKN.equals(action)) {
            null_byte_action = RunChecks.REPLACE_ACTION;

            // line looks like: "unallowed_mimetype_action exception some.runtime.exception"
            final String replaceCharStr = Utils.getTokenAtIndex(line, 2); // MsgException
            if (replaceCharStr == null || replaceCharStr.length() != 1)
                throw new MsgException("invalid replacement character for null bytes: " + replaceCharStr);
            else
                null_byte_replace_char = replaceCharStr.charAt(0);
        } else if (RunChecks.REMOVE_ACTION_TKN.equals(action)) {
            null_byte_action = RunChecks.REMOVE_ACTION;
        } else if (RunChecks.TERMINATE_ACTION_TKN.equals(action)) {
            null_byte_action = RunChecks.TERMINATE_ACTION;
        } else {
            throw new MsgException("Unknown arguments follow " + UNALLOWED_MIMETYPE_ACTION_TKN);
        }
    }

    private void read_trusted_exception_output_action(final String line) throws MsgException {
        final String action = Utils.getTokenAtIndex(line, 1); // MsgException

        if (RunChecks.LOG_ONLY_ACTION_TKN.equals(action)) {
            trusted_exception_output_action = RunChecks.LOG_ONLY_ACTION;
            trusted_exception_output_replace_char = -1;
        } else if (RunChecks.EXCEPTION_ACTION_TKN.equals(action)) {
            trusted_exception_output_action = RunChecks.LOG_ONLY_ACTION;
            trusted_exception_output_replace_char = -1;
            throw new MsgException("trusted_exception_output action can not be exception");
        } else if (RunChecks.REPLACE_ACTION_TKN.equals(action)) {
            trusted_exception_output_action = RunChecks.REPLACE_ACTION;

            // line looks like: "unallowed_mimetype_action exception some.runtime.exception"
            final String replaceCharStr = Utils.getTokenAtIndex(line, 2); // MsgException
            if (replaceCharStr == null || replaceCharStr.length() != 1)
                throw new MsgException("invalid replacement character for null bytes: " + replaceCharStr);
            else
                trusted_exception_output_replace_char = (byte) replaceCharStr.charAt(0);
        } else if (RunChecks.REMOVE_ACTION_TKN.equals(action)) {
            trusted_exception_output_action = RunChecks.LOG_ONLY_ACTION;
            trusted_exception_output_replace_char = -1;
        } else if (RunChecks.TERMINATE_ACTION_TKN.equals(action)) {
            trusted_exception_output_action = RunChecks.TERMINATE_ACTION;

            throw new MsgException("trusted_exception_output action can not be remove");
        } else if (RunChecks.IGNORE_ACTION_TKN.equals(action)) {
            trusted_exception_output_action = RunChecks.IGNORE_ACTION;
            trusted_exception_output_replace_char = -1;
        } else {
            throw new MsgException("Unknown arguments follow " + UNALLOWED_MIMETYPE_ACTION_TKN);
        }
    }

    // Read thru config file collecting check names
    private Vector<String> readConfig_collectCheckNames(final LineNumberReader reader) throws MsgException {
        try {
            final Vector<String> retval = new Vector<String>();
            String line;
            while ((line = reader.readLine()) != null) { // IOException
                                                         // IllegalArgumentException
                line = line.trim();

                if (line.startsWith(CHECK_TKN)) {
                    String checkName = Utils.getTokenAtIndex(line, 1); // MsgException
                    int idx = checkName.indexOf('{');
                    if (idx > 0)
                        checkName = checkName.substring(0, idx);
                    retval.addElement(checkName);
                }
            }

            return retval;

        } catch (IOException ex) {
            throw new MsgException(ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new MsgException(ex.getMessage());
        }
    }

    private Vector<String> readConfig_createShellList(final String line) {
        final Vector<String> retval = new Vector<String>();
        final String[] shellRay = line.split("\\s+"); // PatternSyntaxException
        // shellRay[0] is shell_list
        // shells start at shellRay[1]
        for (int i = 1; i < shellRay.length; i++) {
            retval.addElement(shellRay[i]);
        }

        return retval;
    }

    // line looks like "trusted_exec_paths path1:path2:path3"
    // Put the colon delimited path elements in global trustedExecPaths
    private void buildTrustedPaths(final String line) {
        final String tokens[] = line.split("\\s+");
        if (tokens.length > 1) {
            final String paths[] = tokens[1].split(":");
            // remove trailing '/' chars from each path in paths[]
            for (int i = 0; i < paths.length; i++) {
                if (paths[i].endsWith("/")) {
                    paths[i] = paths[i].replaceAll("/+$", ""); // Removes all trailing '/' chars
                }
                trustedExecPaths.add(paths[i]);
            }
        }
    }

    // Set global trusted_exec_paths_action
    // If trusted_exec_paths_action is exception, set global trusted_env_paths_exception_constructor
    // line looks like "trusted_exec_paths_action log_only"
    //                 "trusted_exec_paths_action exception <RuntimeException>
    //                 "trusted_exec_paths_action terminate"
    //
    private void read_trusted_exec_paths_action_line(final String line) throws MsgException {
        final String action = Utils.getTokenAtIndex(line, 1); // MsgException

        if (RunChecks.LOG_ONLY_ACTION_TKN.equals(action)) {
            trusted_exec_paths_action = RunChecks.LOG_ONLY_ACTION;

        } else if (RunChecks.EXCEPTION_ACTION_TKN.equals(action)) {
            trusted_exec_paths_action = RunChecks.EXCEPTION_ACTION;

            // line looks like: "trusted_exec_paths_action exception some.runtime.exception"
            final String spec = Utils.getTokenAtIndex(line, 2); // MsgException
            trusted_env_paths_exception_constructor = Utils.buildExceptionConstructor(spec); // MsgException

        } else if (RunChecks.TERMINATE_ACTION_TKN.equals(action)) {
            trusted_exec_paths_action = RunChecks.TERMINATE_ACTION;

        } else {
            throw new MsgException("Unknown arguments follow " + TRUSTED_EXEC_PATHS_ACTION_TKN);
        }
    }

    protected void read_malformed_http_request_action(final String line) throws MsgException {
        final String action = Utils.getTokenAtIndex(line, 1); // MsgException
        malformed_http_request_action = RunChecks.actionStringToInt(action);

        if (malformed_http_request_action == RunChecks.EXCEPTION_ACTION) {
            final String spec = Utils.getTokenAtIndex(line, 2); // MsgException
            malformed_http_request_exception_constructor = Utils.buildExceptionConstructor(spec); // MsgException
        }
    }

    protected void read_tainted_redirect_action(final String line) throws MsgException {
        final String action = Utils.getTokenAtIndex(line, 1); // MsgException
        tainted_redirect_action = RunChecks.actionStringToInt(action);

        if (tainted_redirect_action == RunChecks.EXCEPTION_ACTION) {
            final String spec = Utils.getTokenAtIndex(line, 2); // MsgException
            tainted_redirect_exception_constructor = Utils.buildExceptionConstructor(spec); // MsgException
        }
    }

    public ClassNode toClassNode(boolean confinementOn) {
        ClassNode classNode = new ClassNode(Opcodes.ASM5);
        classNode.version = Opcodes.V1_7;
        classNode.access = Opcodes.ACC_PUBLIC;
        classNode.superName = "pac/config/BaseConfig";
        classNode.name = "pac/config/runtime/RuntimeConfig";

        MethodNode constructor = new MethodNode(Opcodes.ASM5, Opcodes.ACC_PUBLIC, "<init>", "()V", null, new String[0]);
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new InsnNode(Opcodes.DUP));
        constructor.instructions
                .add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "pac/config/BaseConfig", "<init>", "()V", false));

        // We have to create an array of ports, since the RuntimeConfig will get loaded
        // really early.
        constructor.instructions.add(AsmUtils.numToInstruction(trustedPortsSet.size()));
        constructor.instructions.add(new IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_INT));
        int idx = 0;
        for (int port : trustedPortsSet) {
            constructor.instructions.add(new InsnNode(Opcodes.DUP));
            constructor.instructions.add(AsmUtils.numToInstruction(idx));
            constructor.instructions.add(AsmUtils.numToInstruction(port));
            constructor.instructions.add(new InsnNode(Opcodes.IASTORE));
            idx++;
        }
        constructor.instructions
                .add(new FieldInsnNode(Opcodes.PUTFIELD, "pac/config/runtime/RuntimeConfig", "trustedPorts", "[I"));

        addPrimitive(constructor.instructions, "deadlocks_enabled", "Z", deadlocks_enabled);
        addPrimitive(constructor.instructions, "toctou_action", "I", toctou_action);
        addExceptionConstructor(constructor.instructions, "toctou_exception_constructor", toctou_exception_constructor);
        addPrimitive(constructor.instructions, "max_memory", "F", max_memory);
        addPrimitive(constructor.instructions, "max_files", "I", max_files);
        addPrimitive(constructor.instructions, "trustAllPorts", "Z", trustAllPorts);
        addPrimitive(constructor.instructions, "server_socket_timeout", "I", server_socket_timeout);
        addPrimitive(constructor.instructions, "client_socket_timeout", "I", client_socket_timeout);
        addPrimitive(constructor.instructions, "max_array_alloc_value", "I", max_array_alloc_value);
        addPrimitive(constructor.instructions, "max_array_alloc_action", "I", max_array_alloc_action);
        addPrimitive(constructor.instructions, "loopCheckMaxIters", "I", loopCheckMaxIters);
        addPrimitive(constructor.instructions, "loopCheckMaxTime", "I", loopCheckMaxTime);
        addPrimitive(constructor.instructions, "loopCheckAction", "I", loopCheckAction);
        addPrimitive(constructor.instructions, "loopCheckMaxItersDef", "I", loopCheckMaxItersDef);
        addPrimitive(constructor.instructions, "loopCheckMaxTimeDef", "I", loopCheckMaxTimeDef);
        addPrimitive(constructor.instructions, "loopCheckActionDef", "I", loopCheckActionDef);
        addPrimitive(constructor.instructions, "recursiveMaxStackSize", "I", recursiveMaxStackSize);
        addPrimitive(constructor.instructions, "recursiveMaxStackAction", "I", recursiveMaxStackAction);
        addPrimitive(constructor.instructions, "untrusted_key_can_access_property_value", "Z",
                     untrusted_key_can_access_property_value);
        addPrimitive(constructor.instructions, "taint_arguments", "Z", taint_arguments);
        addPrimitive(constructor.instructions, "taint_env_vars", "Z", taint_env_vars);
        addPrimitive(constructor.instructions, "taint_db_reads", "Z", taint_db_reads);
        addPrimitive(constructor.instructions, "unallowed_mimetype_action", "I", unallowed_mimetype_action);
        addPrimitive(constructor.instructions, "test_harness_enabled", "Z", test_harness_enabled);
        addPrimitive(constructor.instructions, "can_set_catalog_with_untrusted_data", "Z",
                     can_set_catalog_with_untrusted_data);
        addPrimitive(constructor.instructions, "can_set_clientinfo_with_untrusted_properties", "Z",
                     can_set_clientinfo_with_untrusted_properties);
        addPrimitive(constructor.instructions, "can_set_clientinfo_with_untrusted_name_or_value", "Z",
                     can_set_clientinfo_with_untrusted_name_or_value);
        addPrimitive(constructor.instructions, "can_set_savepoint_with_untrusted_name", "Z",
                     can_set_savepoint_with_untrusted_name);
        addPrimitive(constructor.instructions, "can_set_typemap_with_untrusted_map", "Z",
                     can_set_typemap_with_untrusted_map);
        addPrimitive(constructor.instructions, "trusted_exception_output_action", "I", trusted_exception_output_action);
        addPrimitive(constructor.instructions, "trusted_exception_output_replace_char", "B",
                     trusted_exception_output_replace_char);
        addPrimitive(constructor.instructions, "null_byte_action", "I", null_byte_action);
        addPrimitive(constructor.instructions, "null_byte_replace_char", "I", null_byte_replace_char);
        addPrimitive(constructor.instructions, "trusted_exec_paths_action", "I", trusted_exec_paths_action);
        addPrimitive(constructor.instructions, "overflow_action", "I", overflow_action);
        addPrimitive(constructor.instructions, "underflow_action", "I", underflow_action);
        addPrimitive(constructor.instructions, "infinity_action", "I", infinity_action);
        addPrimitive(constructor.instructions, "overflow_print_action", "I", overflow_print_action);
        addPrimitive(constructor.instructions, "underflow_print_action", "I", underflow_print_action);
        addPrimitive(constructor.instructions, "infinity_print_action", "I", infinity_print_action);
        addPrimitive(constructor.instructions, "malformed_http_request_action", "I", malformed_http_request_action);
        addPrimitive(constructor.instructions, "tainted_redirect_action", "I", tainted_redirect_action);
        addPrimitive(constructor.instructions, "isUnitTesting", "Z", isUnitTesting);
        addExceptionConstructor(constructor.instructions, "loopCheckException", loopCheckExceptionDef);
        addExceptionConstructor(constructor.instructions, "loopCheckExceptionDef", loopCheckExceptionDef);
        addExceptionConstructor(constructor.instructions, "recursiveMaxStackException", recursiveMaxStackException);
        addExceptionConstructor(constructor.instructions, "max_array_alloc_exception", max_array_alloc_exception);
        addExceptionConstructor(constructor.instructions, "unallowed_mimetype_exception_constructor",
                                unallowed_mimetype_exception_constructor);
        addExceptionConstructor(constructor.instructions, "null_byte_exception_constructor",
                                null_byte_exception_constructor);
        addExceptionConstructor(constructor.instructions, "trusted_env_paths_exception_constructor",
                                trusted_env_paths_exception_constructor);
        addExceptionConstructor(constructor.instructions, "overflow_exception", overflow_exception);
        addExceptionConstructor(constructor.instructions, "underflow_exception", underflow_exception);
        addExceptionConstructor(constructor.instructions, "infinity_exception", infinity_exception);
        addExceptionConstructor(constructor.instructions, "malformed_http_request_exception_constructor",
                                malformed_http_request_exception_constructor);
        addExceptionConstructor(constructor.instructions, "tainted_redirect_exception_constructor",
                                tainted_redirect_exception_constructor);
        addString(constructor.instructions, "unallowed_mimetype_jail_file", unallowed_mimetype_jail_file);
        addString(constructor.instructions, "log_file", log_file);
        addString(constructor.instructions, "overflow_print_replace", overflow_print_replace);
        addString(constructor.instructions, "underflow_print_replace", underflow_print_replace);
        addString(constructor.instructions, "infinity_print_replace", infinity_print_replace);
        addCollection(constructor.instructions, "shells", shells, confinementOn);
        addCollection(constructor.instructions, "trustedExecPaths", trustedExecPaths, confinementOn);
        addCollection(constructor.instructions, "contentTypesAllowed", contentTypesAllowed, confinementOn);
        addMap(constructor.instructions, "mimeTypesMap", mimeTypesMap);
        addMap(constructor.instructions, "tokenizers", tokenizers);
        addMap(constructor.instructions, "configChksMap", configChksMap);
        addMap(constructor.instructions, "methodDescMap", methodDescMap);
        constructor.instructions.add(new InsnNode(Opcodes.RETURN));
        classNode.methods.add(constructor);

        return classNode;
    }

    @SuppressWarnings("unchecked")
    private void addMap(InsnList insnList, String fieldName, Map<String, ?> value) {
        if (value == null)
            return;
        Type fieldType = Type.getType(value.getClass());
        String fieldTypeName = fieldType.getInternalName();
        String fieldDesc = fieldType.getDescriptor();
        insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
        insnList.add(new TypeInsnNode(Opcodes.NEW, fieldTypeName));
        insnList.add(new InsnNode(Opcodes.DUP));
        insnList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, fieldTypeName, "<init>", "()V", false));
        insnList.add(new FieldInsnNode(Opcodes.PUTFIELD, "pac/config/runtime/RuntimeConfig", fieldName, fieldDesc));
        for (Entry<String, ?> entry : value.entrySet()) {
            insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
            insnList.add(new FieldInsnNode(Opcodes.GETFIELD, "pac/config/runtime/RuntimeConfig", fieldName, fieldDesc));
            insnList.add(new LdcInsnNode(entry.getKey()));
            Object obj = entry.getValue();
            if (obj instanceof Set) {
                Set<String> set = (Set<String>) obj;
                String valueType = Type.getType(set.getClass()).getInternalName();
                insnList.add(new TypeInsnNode(Opcodes.NEW, valueType));
                insnList.add(new InsnNode(Opcodes.DUP));
                insnList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, valueType, "<init>", "()V", false));
                for (String s : set) {
                    insnList.add(new InsnNode(Opcodes.DUP));
                    insnList.add(new LdcInsnNode(s));
                    insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, valueType, "add", "(Ljava/lang/Object;)Z",
                            false));
                    insnList.add(new InsnNode(Opcodes.POP));
                }
            } else if (obj instanceof Desc) {
                String buildStr = buildStrMap.get(obj);
                insnList.add(new LdcInsnNode(buildStr));
                insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "pac/config/Desc", "createDesc",
                        "(Ljava/lang/String;Lpac/config/AbstractConfig;)Lpac/config/Desc;", false));
            } else if (obj instanceof Chk) {
                String buildStr = buildStrMap.get(obj);
                if (buildStr != null) {
                    insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    String checkName = entry.getKey();
                    int cweNumber = ((Chk) obj).cweNumber;
                    if (cweNumber > 0)
                        insnList.add(new LdcInsnNode(checkName + "{CWE-" + cweNumber + "}"));
                    else
                        insnList.add(new LdcInsnNode(checkName));
                    insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "pac/config/AbstractConfig", "getCheck",
                            "(Ljava/lang/String;)Lpac/config/Chk;", false));
                    BufferedReader in = new BufferedReader(new StringReader(buildStr));
                    String line;
                    try {
                        while ((line = in.readLine()) != null) {
                            insnList.add(new InsnNode(Opcodes.DUP));
                            insnList.add(new LdcInsnNode(line));
                            insnList.add(new InsnNode(Opcodes.ACONST_NULL));
                            insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "pac/config/Chk",
                                    "buildChk_parseLine",
                                    "(Ljava/lang/String;Ljava/util/Vector;Lpac/config/AbstractConfig;)V", false));
                        }
                    } catch (IOException e) {
                        Ansi.error("unable to build check \"%s\"", null, ((Chk) obj).check_name);
                        System.exit(1);
                    }
                }
            } else if (obj instanceof Tokenizer) {
                String buildStr = buildStrMap.get(obj);
                if (buildStr != null) {
                    insnList.add(new TypeInsnNode(Opcodes.NEW, "pac/config/Tokenizer"));
                    insnList.add(new InsnNode(Opcodes.DUP));
                    insnList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "pac/config/Tokenizer", "<init>", "()V",
                            false));
                    BufferedReader in = new BufferedReader(new StringReader(buildStr));
                    String line;
                    try {
                        while ((line = in.readLine()) != null) {
                            insnList.add(new InsnNode(Opcodes.DUP));
                            insnList.add(new LdcInsnNode(line));
                            insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "pac/config/Tokenizer",
                                    "buildTkn_parseLine", "(Ljava/lang/String;)V", false));
                        }
                    } catch (IOException e) {
                        Ansi.error("unable to build tokenizer: %s", null, buildStr);
                        System.exit(1);
                    }
                }
            }
            insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, fieldTypeName, "put",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false));
            insnList.add(new InsnNode(Opcodes.POP));
        }
    }

    private void addCollection(InsnList insnList, String fieldName, Collection<String> value, boolean confinementOn) {
        if (value == null)
            return;
        Type fieldType = Type.getType(value.getClass());
        String fieldTypeName = fieldType.getInternalName();
        String fieldDesc = fieldType.getDescriptor();
        insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
        insnList.add(new TypeInsnNode(Opcodes.NEW, fieldTypeName));
        insnList.add(new InsnNode(Opcodes.DUP));
        insnList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, fieldTypeName, "<init>", "()V", false));
        insnList.add(new FieldInsnNode(Opcodes.PUTFIELD, "pac/config/runtime/RuntimeConfig", fieldName, fieldDesc));
        for (String s : value) {
            insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
            insnList.add(new FieldInsnNode(Opcodes.GETFIELD, "pac/config/runtime/RuntimeConfig", fieldName, fieldDesc));
            insnList.add(new LdcInsnNode(s));
            if (confinementOn) {
                insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "pac/util/TaintUtils", "trustConstant",
                        "(Ljava/lang/String;)Ljava/lang/String;", false));
            }
            insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, fieldTypeName, "add", "(Ljava/lang/Object;)Z",
                    false));
            insnList.add(new InsnNode(Opcodes.POP));
        }
    }

    private void addExceptionConstructor(InsnList insnList, String fieldName, Constructor<?> exConstructor) {
        if (exConstructor == null)
            return;
        insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
        insnList.add(new LdcInsnNode(exConstructor.getName()));
        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "pac/config/Utils", "buildExceptionConstructor",
                "(Ljava/lang/String;)Ljava/lang/reflect/Constructor;", false));
        insnList.add(new FieldInsnNode(Opcodes.PUTFIELD, "pac/config/runtime/RuntimeConfig", fieldName,
                "Ljava/lang/reflect/Constructor;"));
    }

    private void addPrimitive(InsnList insnList, String fieldName, String typeDesc, Object value) {
        insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
        switch (Type.getType(typeDesc).getSort()) {
        case Type.BOOLEAN:
            insnList.add(((Boolean) value).booleanValue() ? AsmUtils.numToInstruction(1)
                    : AsmUtils.numToInstruction(0));
            break;
        case Type.CHAR:
            insnList.add(AsmUtils.numToInstruction((int) ((Character) value).charValue()));
            break;
        default:
            insnList.add(AsmUtils.numToInstruction((Number) value));
            break;
        }
        insnList.add(new FieldInsnNode(Opcodes.PUTFIELD, "pac/config/runtime/RuntimeConfig", fieldName, typeDesc));
    }

    private void addString(InsnList insnList, String fieldName, String value) {
        if (value == null)
            return;
        insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
        insnList.add(new LdcInsnNode(value));
        insnList.add(new FieldInsnNode(Opcodes.PUTFIELD, "pac/config/runtime/RuntimeConfig", fieldName,
                "Ljava/lang/String;"));
    }

    public int getLoopTimeout(boolean definiteTaint) {
        return definiteTaint ? loopCheckMaxTimeDef : loopCheckMaxTime;
    }
}
