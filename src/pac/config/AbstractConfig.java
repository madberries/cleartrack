package pac.config;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

/**
 * The cleartrack configuration abstraction layer. This class is extended by both
 * the instrumentation configuration and the runtime configuration.  This class
 * really only needs to define methods that are needed by both the instrumentation-time
 * configuration and the run-time configuration.
 * 
 * @author jeikenberry
 */
public abstract class AbstractConfig {
    protected static final String CONFIG_FILE_NAME = "config_inst";
    protected static final Pattern HTTPREQUEST_PAT = Pattern
            .compile("(GET\\s+|HEAD\\s+|POST\\s+|OPTIONS\\s+|PUT\\s+|DELETE\\s+|TRACE\\s+|CONNECT\\s+)(.+)(\\s+HTTP/[0-9.]+)");
    protected static final Pattern HTTPRESPONSE_PAT = Pattern
            .compile("HTTP/[0-9.]+\\s+[0-9]{3}\\s+.*\\r\\n([-a-zA-Z0-9]+:\\s+.*\\r\\n)+\\r\\n[\\x00-\\x7F]*");

    protected String log_file;

    protected HashSet<Integer> trustedPortsSet;
    protected boolean trustAllPorts;

    protected HashMap<String, Desc> methodDescMap; // Collection of ex.
                                                   // "desc Runtime.exec(String[]<check_name>)"
    protected HashMap<String, Chk> configChksMap;
    protected HashMap<String, Tokenizer> tokenizers;

    protected Vector<String> shells;
    protected Vector<String> trustedExecPaths; // list of directories.
                                               // If untrusted String
                                               // Runtime.exe or ProcessBuilder
                                               // is running an executable
                                               // given by "leaf_name" where
                                               // leaf_name and contains no path
                                               // delimiters
                                               // o Then if that leaf executable
                                               // does not live in any of the
                                               // directories
                                               // listed in trustedExecPaths,
                                               // then tack action specified by
                                               // trusted_exec_paths_action
                                               // o Else if that leaf executable
                                               // does live in one of the
                                               // directories listed in
                                               // trustedExecPaths
                                               // Then if the first dir in env
                                               // var PATH where leaf_name is
                                               // exists
                                               // differs from the
                                               // trustedExecPaths dir, then
                                               // take the action
                                               // specified by
                                               // trusted_exec_paths_action
    protected int server_socket_timeout, client_socket_timeout;

    protected int max_array_alloc_value = 1000000;
    protected int max_array_alloc_action = RunChecks.LOG_ONLY_ACTION;
    protected Constructor<?> max_array_alloc_exception;

    // Unchecked loop condition variables, probably tainted
    protected int loopCheckMaxIters;
    protected int loopCheckMaxTime = -1;
    protected int loopCheckAction = -1;
    protected Constructor<?> loopCheckException;

    // Unchecked loop condition variables, definitely tainted
    protected int loopCheckMaxItersDef;
    protected int loopCheckMaxTimeDef = -1;
    protected int loopCheckActionDef = -1;
    protected Constructor<?> loopCheckExceptionDef;

    // Recursive bounds (off by default)
    protected int recursiveMaxStackSize = -1;
    protected int recursiveMaxStackAction = RunChecks.IGNORE_ACTION;
    protected Constructor<?> recursiveMaxStackException;

    protected boolean untrusted_key_can_access_property_value;
    protected boolean taint_arguments, taint_env_vars, taint_db_reads;
    protected int unallowed_mimetype_action;
    protected Constructor<?> unallowed_mimetype_exception_constructor;
    protected String unallowed_mimetype_jail_file;

    protected boolean test_harness_enabled = false;

    protected boolean can_set_catalog_with_untrusted_data = false;
    protected boolean can_set_clientinfo_with_untrusted_properties = false;
    protected boolean can_set_clientinfo_with_untrusted_name_or_value = false;
    protected boolean can_set_savepoint_with_untrusted_name = false;
    protected boolean can_set_typemap_with_untrusted_map = false;

    protected int trusted_exception_output_action = RunChecks.LOG_ONLY_ACTION;
    protected byte trusted_exception_output_replace_char;

    protected int null_byte_action;
    protected int null_byte_replace_char;
    protected Constructor<?> null_byte_exception_constructor;

    protected int trusted_exec_paths_action; // what to do when an executable
                                             // leaf is specified and that
                                             // leaf does not exist in one of
                                             // the dirs listed in config file
                                             // as a trusted_exe_paths or it
                                             // does exist but PATH env var says
                                             // the leaf should be executed from
                                             // some other dir
    protected Constructor<?> trusted_env_paths_exception_constructor;
    protected HashMap<String, Set<String>> mimeTypesMap;
    protected HashSet<String> contentTypesAllowed;

    protected int overflow_action, underflow_action, infinity_action, overflow_print_action, underflow_print_action,
            infinity_print_action;
    protected Constructor<?> overflow_exception, underflow_exception, infinity_exception;
    protected String overflow_print_replace, underflow_print_replace, infinity_print_replace;

    protected int malformed_http_request_action = RunChecks.REPLACE_ACTION;
    protected Constructor<?> malformed_http_request_exception_constructor;
    protected int tainted_redirect_action = RunChecks.REMOVE_ACTION;
    protected Constructor<?> tainted_redirect_exception_constructor;

    protected boolean isUnitTesting = false;

    protected boolean deadlocks_enabled;
    protected int toctou_action;
    protected Constructor<?> toctou_exception_constructor;
    protected float max_memory;
    protected int max_files;

    /**
     * Obtains the check from the check name.
     * 
     * @param checkName String
     * @return Chk the check object
     */
    public Chk getCheck(String checkName) {
        if (checkName == null)
            return null;
        int idx = checkName.indexOf('{');
        String key = checkName;
        if (idx > 0)
            key = checkName.substring(0, idx);

        Chk chk = configChksMap.get(key);
        if (chk == null) {
            chk = new Chk(checkName);
            configChksMap.put(key, chk);
        }
        return chk;
    }

    /**
     * Obtains the descriptor from the descriptor name.
     * 
     * @param descName String
     * @return Desc the descriptor object
     */
    public Desc getDescriptor(String descName) {
        return methodDescMap.get(descName);
    }

    /**
     * Obtains the tokenizer from the tokenizer name.
     * 
     * @param tokenizerName String
     * @return Tokenizer the tokenizer object
     */
    public Tokenizer getTokenizer(String name) {
        return tokenizers.get(name);
    }

    /**
     * @return String of the path to this configuration file
     */
    public String getPath() {
        // Form a full path. Do not use File. File eventually calls this method
        return "./" + CONFIG_FILE_NAME;
    }

    /**
     * @return action index for probable unchecked loops.
     */
    public final int getLoopAction() {
        return loopCheckAction;
    }

    /**
     * @return action index for definitely unchecked loops.
     */
    public final int getLoopActionDefinite() {
        return loopCheckActionDef;
    }

    /**
     * @return action index for deep recursive stack size check.
     */
    public final int getMaxStackAction() {
        return recursiveMaxStackAction;
    }

    public final boolean areDeadlocksEnabled() {
        return deadlocks_enabled;
    }
}
