package pac.config;

public class ConfigFileTokens {
    // check tokens..
    public static final String CHECK_TKN = "check";
    public static final String PREFIX_WHITE_TKN = "prefix_white";
    public static final String INTERNAL_WHITE_TKN = "internal_white";
    public static final String SUFFIX_WHITE_TKN = "suffix_white";
    public static final String WHITE_FAILURE_ACTION_TKN = "white_failure_action";
    public static final String PREFIX_BLACK_TKN = "prefix_black";
    public static final String INTERNAL_BLACK_TKN = "internal_black";
    public static final String SUFFIX_BLACK_TKN = "suffix_black";
    public static final String BLACK_FAILURE_ACTION_TKN = "black_failure_action";
    public static final String VAR_VALUE_WHITE_TKN = "var_value_white";
    public static final String VAR_VALUE_BLACK_TKN = "var_value_black";
    public static final String TOKEN_TKN = "token";
    public static final String ARRAY_TKN = "array";
    public static final String QSTRING_TKN = "qstring";
    public static final String TRAVERSAL_COUNT_TKN = "traversal_count";
    public static final String TOKEN_WHITESPACE_TKN = "token_whitespace";
    public static final String WHITE_ENCODINGS_TKN = "white_encodings";
    public static final String BLACK_ENCODINGS_TKN = "black_encodings";
    public static final String WHITE_ENCODINGS_ACTION_TKN = "white_encodings_action";
    public static final String BLACK_ENCODINGS_ACTION_TKN = "black_encodings_action";
    public static final String CHECK_CRITERIA_TKN = "check_criteria";
    public static final String TOKEN_MATCH_TKN = "token_match";
    public static final String TOKEN_NOMATCH_TKN = "token_nomatch";
    public static final String END_TKN = "end";
    // ***

    // tokenizer tokens...
    public static final String TOKENIZER_TKN = "tokenizer";
    public static final String TOKENS_TKN = "tokens";
    public static final String QUOTED_BLOCK_TKN = "quoted_block";
    public static final String CHAR_MAP_TKN = "char_map";
    // ***

    // These tokens must be outside "check" and "end check"
    public static final String LOG_FILE_TKN = "log_file";
    public static final String SHELL_LIST_TKN = "shell_list";
    public static final String DESC_TKN = "desc";
    public static final String TRUSTED_EXEC_PATHS_TKN = "trusted_exec_paths";
    public static final String TRUSTED_EXEC_PATHS_ACTION_TKN = "trusted_exec_paths_action";
    public static final String TEST_HARNESS_ENABLED_TKN = "test_harness_enabled";
    public static final String LOOP_CHECK_TKN = "loop_check";
    public static final String LOOP_CHECK_DEF_TKN = "loop_check_def";
    public static final String MAX_STACK_SIZE_TKN = "max_stack_size";
    public static final String SERVER_SOCKET_TIMEOUT_TKN = "server_socket_timeout";
    public static final String CLIENT_SOCKET_TIMEOUT_TKN = "client_socket_timeout";
    public static final String NULL_BYTE_ACTION_TKN = "null_byte_action";
    public static final String TAINTED_REDIRECT_ACTION_TKN = "tainted_redirect_action";
    public static final String MALFORMED_HTTP_REQUEST_ACTION_TKN = "malformed_http_request_action";
    public static final String TRUSTED_EXCEPTION_OUTPUT_TKN = "trusted_exception_output";
    public static final String OVERFLOW_TKN = "overflow";
    public static final String UNDERFLOW_TKN = "underflow";
    public static final String INFINITY_TKN = "infinity";
    public static final String OVERFLOW_PRINT_TKN = "overflow_print";
    public static final String UNDERFLOW_PRINT_TKN = "underflow_print";
    public static final String INFINITY_PRINT_TKN = "infinity_print";
    public static final String MAX_ARRAY_ALLOC_TKN = "max_array_alloc";
    public static final String ALLOWED_MIMETYPES_TKN = "allowed_mimetypes";
    public static final String UNALLOWED_MIMETYPE_ACTION_TKN = "unallowed_mimetype_action";
    public static final String UNTRUSTED_KEY_CAN_ACCESS_PROPERTY_TKN = "untrusted_key_can_access_property";
    public static final String TAINT_ARGUMENTS_TKN = "taint_arguments";
    public static final String TAINT_ENV_VARS_TKN = "taint_env_vars";
    public static final String TAINT_DB_READS_TKN = "taint_db_reads";
    public static final String TRUSTED_PORTS_TKN = "trusted_ports";
    public static final String DEADLOCKS_ENABLED_TKN = "deadlocks_enabled";
    public static final String TOCTOU_ACTION_TKN = "toctou_action";
    public static final String MAX_MEMORY_TKN = "max_memory";
    public static final String MAX_FILES_TKN = "max_files";

    // Connection methods
    public static final String CAN_SET_CATALOG_WITH_UNTRUSTED_DATA_TKN = "can_set_catalog_with_untrusted_data";
    public static final String CAN_SET_CATALOG_WITH_UNTRUSTED_PROPERTIES_TKN = "can_set_clientinfo_with_untrusted_properties";
    public static final String CAN_SET_CATALOG_WITH_UNTRUSTED_NAME_OR_VALUE_TKN = "can_set_clientinfo_with_untrusted_name_or_value";
    public static final String CAN_SET_SAVEPOINT_WITH_UNTRUSTED_NAME_TKN = "can_set_savepoint_with_untrusted_name";
    public static final String CAN_SET_TYPEMAP_WITH_UNTRUSTED_MAP_TKN = "can_set_typemap_with_untrusted_map";
}
