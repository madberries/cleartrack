package pac.util.parsers;

public interface LangConstants {
    public static final int UNKNOWN = 0;
    public static final int COMMAND = 1;
    public static final int LDAP = 2;
    public static final int SQL = 4;
    public static final int XQUERY = 8;
    public static final int COMMAND_LDAP_SQL_XQUERY = COMMAND | LDAP | SQL | XQUERY;
    public static final int COMMAND_XQUERY = COMMAND | XQUERY;
    public static final int LDAP_SQL_XQUERY = LDAP | SQL | XQUERY;
    public static final int LDAP_SQL = LDAP | SQL;
    public static final int LDAP_XQUERY = LDAP | XQUERY;
    public static final int SQL_XQUERY = SQL | XQUERY;
}
