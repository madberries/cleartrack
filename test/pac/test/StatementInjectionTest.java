package pac.test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.h2.tools.Server;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.codecs.Codec;
import org.owasp.esapi.codecs.MySQLCodec;
import org.owasp.esapi.codecs.MySQLCodec.Mode;
import org.owasp.esapi.errors.EncodingException;
import org.postgresql.util.PSQLException;

import pac.config.CleartrackException;
import pac.util.InstrumentationDialog;
import pac.util.TaintUtils;
import pac.wrap.ByteArrayTaint;

import com.google.common.base.Splitter;

public class StatementInjectionTest {
    // CONSTANTS
    public static final String DB_HOST = "jdbc:h2:tcp://localhost/testdb";
    public static final String DB_OPTS = "?allowMultiQueries=true&";
    public static final String DB_ACCT = "user=cleartrack&password=test";
    public static final String DB_CONNECT_STRING = DB_HOST + DB_OPTS + DB_ACCT;

    public static final String QUERIES_FILE_NAME = "test/pac/test/queries.txt";

    public static final String MSSQL_SERVER = System.getenv("MSSQL_SERVER");
    public static final String MYSQL_SERVER = System.getenv("MYSQL_SERVER");
    public static final String PSQL_SERVER = System.getenv("PSQL_SERVER");

    static {
        // Trust all servers because we only intend to test whether injections
        // are sanitized across all SQL queries.
        if (MSSQL_SERVER != null)
            TaintUtils.trust(MSSQL_SERVER);
        if (MYSQL_SERVER != null)
            TaintUtils.trust(MYSQL_SERVER);
        if (PSQL_SERVER != null)
            TaintUtils.trust(PSQL_SERVER);
    }

    // TEST TABLE DATA

    public static final String TEST_TABLE_NAME = "employees";
    public static final String TEST_TABLE_COLUMNS[] = { "id", "VARCHAR(20)", "count", "INTEGER(10)", "name",
            "VARCHAR(20)", "hired", "DATE", "email", "VARCHAR(30)", "position", "VARCHAR(20)", "password",
            "VARCHAR(20)" };

    public final static Object TEST_TABLE_DATA[][] = {
            { "user01", 0, "Abigail", "1985-01-02", "abigail@example.com", "supervisor", "password1" },
            { "user02", 1, "Brian", "1990-07-13", "brian@example.com", "programmer", "password2" },
            { "user03", 2, "Calvin", "1982-04-22", "calvin@example.com", "manager", "password3" },
            { "user04", 3, "Devlin", "1987-12-03", "devlin@example.com", "programmer", "password4" }, };

    // STATIC SECTION FOR ALL TESTS

    static Server dbServer;
    static boolean ins_enabled = true;

    /**
     * Sets up the database that will be used for all unit tests. Instantiates
     * an embedded SQL server, loads the appropriate client JDBC driver, and
     * populates the database with tables of test data.
     * 
     * @throws SQLException
     *             on error creating test database
     * @throws ClassNotFoundException
     *             on missing JDBC driver
     */
    @BeforeClass
    public static void setupSuite() throws SQLException, ClassNotFoundException {

        // turn off demo popups
        InstrumentationDialog.setShowPopUps(false);
        // instantiate the embedded database server
        if (dbServer == null)
            dbServer = Server.createTcpServer("").start();

        // load the database client driver
        Class.forName("org.h2.Driver");
        ins_enabled = isInstrumentationEnabled();
    }

    @AfterClass
    public static void teardownSuite() {
        if (dbServer != null)
            dbServer.stop();
        dbServer = null;
    }

    // PER-TEST INSTANCE INFORMATION

    public Connection connection;

    @Before
    public void setupTest() throws SQLException {
        // connect to the database
        connection = DriverManager.getConnection("jdbc:h2:~/test", "sa", "");

        // recreate the test database tables
        try {
            recreateTables();
        } catch (SQLException e) {
            System.out.print("Error creating database tables:" + e.getMessage());
            throw e;
        }
    }

    /**
     * Creates test tables and fills them with test data.
     * 
     * @throws SQLException
     */
    public void recreateTables() throws SQLException {
        Statement stmt = connection.createStatement();
        try {
            // Delete any existing DB
            stmt.execute("DROP ALL OBJECTS");

            // create the test table
            createTable(stmt, TEST_TABLE_NAME, TEST_TABLE_COLUMNS);

            // populate the test table
            populateTable(stmt, TEST_TABLE_NAME, TEST_TABLE_DATA);

        } finally {
            stmt.close();
        }
    }

    /**
     * Creates a table in the test database.
     * 
     * @param stmt
     * @param tableName
     *            Name of the table to create.
     * @param columns
     *            Even elements are column names. Odd elements are the type of
     *            the preceding named column.
     * @throws SQLException
     */
    private void createTable(Statement stmt, String tableName, String[] columns) throws SQLException {
        StringBuffer s = new StringBuffer();
        s.append("CREATE TABLE " + tableName + "(");
        for (int i = 0; i < columns.length; i += 2) {
            if (columns.length > 2 && i > 0) {
                s.append(", ");
            }
            s.append(" " + columns[i] + " " + columns[i + 1]);
        }
        s.append(");");
        // System.out.println(s.toString());
        stmt.execute(s.toString());
    }

    /**
     * Populates a table with test data.
     * 
     * @param stmt
     *            Statement for executing SQL commands.
     * @param tableName
     *            Name of the table in the database
     * @param data
     *            An array of row data arrays. Each sub-array is the column data
     *            for one row in the table.
     * @throws SQLException
     */
    private void populateTable(Statement stmt, String tableName, Object[][] data) throws SQLException {
        for (Object employeeData[] : data) {
            StringBuffer s = new StringBuffer();
            s.append("INSERT INTO " + tableName + " VALUES (");
            for (int i = 0; i < employeeData.length; i++) {
                if (i > 0 && employeeData.length > 1) {
                    s.append(", ");
                }
                if (employeeData[i] instanceof String)
                    s.append("'" + employeeData[i] + "'");
                else
                    s.append("" + employeeData[i]);
            }
            s.append(");");
            stmt.execute(s.toString());
        }
    }

    @After
    public void teardownTest() throws SQLException {
        connection.close();
    }

    // QUERIES

    public int queryNumberOfEmployees() throws SQLException {
        int resultNum;
        Statement stmt = null;
        ResultSet rs = null;

        stmt = connection.createStatement();
        String query = "SELECT COUNT(*) FROM " + TEST_TABLE_NAME;
        rs = stmt.executeQuery(query);
        if (!rs.next()) {
            throw new RuntimeException("No result set for query: " + query);
        }

        resultNum = rs.getInt(1);

        stmt.close();
        return resultNum;
    }

    public String lookupEmail(final String TEST_NAME) throws SQLException {
        String resultString = null;
        Statement stmt = null;
        ResultSet rs = null;

        stmt = connection.createStatement();
        String query = String.format("SELECT email FROM %s WHERE name='%s';", TEST_TABLE_NAME, TEST_NAME);
        boolean result = stmt.execute(query);
        if (result) {
            rs = stmt.getResultSet();
            if (rs.next()) {
                resultString = rs.getString(1);
            }
        }

        stmt.close();
        return resultString;
    }

    public boolean queryHasEmployee(final String TEST_NAME) throws SQLException {
        boolean result;
        Statement stmt = null;
        ResultSet rs = null;

        stmt = connection.createStatement();
        String query = String.format("SELECT * FROM %s WHERE name='%s'", TEST_TABLE_NAME, TEST_NAME);
        rs = stmt.executeQuery(query);
        result = rs.next();

        stmt.close();
        return result;
    }

    public static String passwordQueryFormat = "SELECT id FROM %s WHERE name='%s' AND password='%s';";

    public static String getPasswordSqlQuery(String TEST_NAME, String TEST_PASSWORD) {
        String query = String.format(passwordQueryFormat, TEST_TABLE_NAME, TEST_NAME, TEST_PASSWORD);
        return query;
    }

    public static ArrayList<String> executeSqlStatement(Connection connection, String sql) throws SQLException {

        Statement stmt = connection.createStatement();

        ResultSet rs = stmt.executeQuery(sql);

        ArrayList<String> results = new ArrayList<String>();

        while (rs.next()) {
            results.add(rs.getString(1));
        }
        stmt.close();
        if (results.size() > 0) {
            System.out.println("Sql Executed: " + sql + "\n");
            System.out.println("results: " + results);
        }

        return results;
    }

    public String verifyPassword(final String TEST_NAME, final String TEST_PASSWORD) throws SQLException {
        Statement stmt = connection.createStatement();
        String returnValue = verifyPassword(TEST_NAME, TEST_PASSWORD, stmt);
        stmt.close();
        return returnValue;
    }

    public String verifyPassword(final String TEST_NAME, final String TEST_PASSWORD, final Statement stmt)
            throws SQLException {
        String resultString = null;

        ResultSet rs = null;

        String query = getPasswordSqlQuery(TEST_NAME, TEST_PASSWORD);
        rs = stmt.executeQuery(query);
        if (rs.next()) {
            resultString = rs.getString(1);
        }

        if (resultString != null) {
            System.out.println("Query Format: " + passwordQueryFormat);
            System.out.println("TEST_NAME=" + TEST_NAME);
            System.out.println("TEST_PASSWORD=" + TEST_PASSWORD);
            System.out.println("resultString: " + resultString);
            System.out.println("Sql Executed: " + query + "\n");
        }

        return resultString;
    }

    public String verifyPasswordNoQuotes(final String TEST_NAME, final String TEST_PASSWORD) throws SQLException {
        Statement stmt = connection.createStatement();
        String resultString = verifyPasswordNoQuotes(TEST_NAME, TEST_PASSWORD, stmt);
        stmt.close();
        return resultString;
    }

    public String verifyPasswordNoQuotes(final String TEST_NAME, final String TEST_PASSWORD, final Statement stmt)
            throws SQLException {
        String resultString = null;

        ResultSet rs = null;

        String queryFormat = "SELECT id FROM %s WHERE name='%s' AND password=%s;";
        String query = String.format(queryFormat, TEST_TABLE_NAME, TEST_NAME, TEST_PASSWORD);
        rs = stmt.executeQuery(query);
        if (rs.next()) {
            resultString = rs.getString(1);
        }

        if (resultString != null) {
            System.out.println("Query Format: " + queryFormat);
            System.out.println("TEST_NAME=" + TEST_NAME);
            System.out.println("TEST_PASSWORD=" + TEST_PASSWORD);
            System.out.println("resultString: " + resultString);
            System.out.println("Sql Executed: " + query + "\n");
        }

        return resultString;
    }

    public String verifyPasswordNonString(final String TEST_NAME, final String TEST_PASSWORD) throws SQLException {
        String resultString = null;
        Statement stmt = null;
        ResultSet rs = null;

        stmt = connection.createStatement();
        String query = String.format("SELECT id FROM %s WHERE name='%s' OR count=%s;", TEST_TABLE_NAME, TEST_NAME,
                                     TEST_PASSWORD);
        rs = stmt.executeQuery(query);
        if (rs.next()) {
            resultString = rs.getString(1);
        }

        stmt.close();
        return resultString;
    }

    /**
     * Given a test name and a tag, retrieves the corresponding query from the
     * file of queries.
     */
    String getQueryFromFile(String testName, String tag) {
        InputStream in = null;
        BufferedReader reader = null;
        try {
            in = new FileInputStream(QUERIES_FILE_NAME);
            reader = new BufferedReader(new InputStreamReader(in));

            // Skip header.
            reader.readLine();

            /*
             * Find the (first) line with matching test name and tag. The lines
             * have the format:
             * 
             * test name<tab>tag<tab>query<tab>comment<eol>
             */
            String line;
            while ((line = reader.readLine()) != null) {
                String parts[] = line.split("\t");
                if (parts[0].equals(testName) && parts[1].equals(tag)) {
                    return parts[2];
                }
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
    }

    // TESTS

    @Test
    public void testSetCatalog() {
        // Unfortunately we must skip this if MySQL is not set up, since H2 does
        // not support
        // some of these features...
        if (MYSQL_SERVER == null || MYSQL_SERVER.equals("")) {
            System.err.println("MYSQL_SERVER environment variable is not set.  Skipping MySQL tests...");
            return;
        }

        try {
            boolean caughtEx;

            useMysql();

            // *******
            // *** Connection.setCatalog(String) -
            // ***
            // Test Connection.setCatalog(String) - test that tainted
            // catalogName throws exception
            String origCatalogName = connection.getCatalog(); // SQLException
            String catalogName = "myCatalog";
            TaintUtils.taint(catalogName);
            caughtEx = false;
            try {
                connection.setCatalog(catalogName); // SQLException
            } catch (CleartrackException ex) {
                caughtEx = true;
            }
            Assert.assertTrue("Connection.setCatalog(String) setting catalog with tainted string should throw StoneSouopException",
                              caughtEx);
            String newCatalogName = connection.getCatalog();
            Assert.assertTrue("Connection.setCatalog(String) setting catalog with tainted string should not have altered connection's current catalog",
                              origCatalogName.equals(newCatalogName));

            // ***
            // Test Connection.setCatalog(String) - test that trusted
            // catalogName will throw exception because of non-existent data
            // base name
            catalogName = "myCatalog";
            TaintUtils.trust(catalogName);
            caughtEx = false;
            try {
                connection.setCatalog(catalogName); // SQLException
            } catch (SQLException ex) {
                caughtEx = true;
            }
            Assert.assertTrue("Connection.setCatalog(String) setting catalog to a non-existant name failed to throw a connect exception",
                              caughtEx);
            newCatalogName = connection.getCatalog();
            Assert.assertFalse("Connection.setCatalog(String) setting catalog with trusted string should not have altered connection's current catalog",
                               catalogName.equals(newCatalogName));

            // ********
            // *** Test Connection.setClientInfo(Properties)
            // ***
            // Test Connection.setClientInfo(Properties) test that tainted
            // properties don't get set in connection's client info properties
            Properties properties = new Properties();
            String name = "propName";
            TaintUtils.taint(name);
            String value = "propValue";
            TaintUtils.taint(value);
            properties.put(name, value);
            caughtEx = false;
            try {
                connection.setClientInfo(properties); // SQLClientInfoException
            } catch (CleartrackException ex) {
                caughtEx = true;
            }
            String val = connection.getClientInfo("propName");
            Assert.assertTrue("Connection.setClientInfo(Properties) property \"propName\" is tainted and should thrown CleartrackException",
                              caughtEx);
            Assert.assertTrue("Connection.setClientInfo(Properties) property \"propName\" is tainted and should not have been set",
                              val == null);

            // ***
            // Test Connection.setClientInfo(Properties) test that trusted
            // properties DO set connection's client info properties
            properties.clear();
            name = "propName_2";
            TaintUtils.trust(name);
            value = "propValue_2";
            TaintUtils.trust(value);
            properties.put(name, value);
            try {
                connection.setClientInfo(properties); // SQLClientInfoException
            } catch (CleartrackException ex) {
                Assert.fail("Connection.setClientInfo(Properties) was called with trusted properties. Should not have thrown StroneSoupException");
            }
            String newVal = connection.getClientInfo("propName_2");
            Assert.assertTrue("Connection.setClientInfo(Properties) Properties is trusted and should have been set",
                              newVal.equals(value));

            // ********
            // **** Connection.setClientInfo(String, String)
            // ***
            // Test Connection.setClientInfo(String, String) test that untrusted
            // name and/or value will not be
            name = "myPropName";
            TaintUtils.taint(name);
            value = "myPropValue";
            TaintUtils.taint(value);
            caughtEx = false;
            try {
                connection.setClientInfo(name, value); // SQLClientInfoException
            } catch (CleartrackException ex) {
                caughtEx = true;
            }
            Assert.assertTrue("Connection.setClientInfo(String,String) name and/or value are tainted. Should have thrown exception",
                              caughtEx);
            newVal = connection.getClientInfo(name);
            Assert.assertFalse("Connection.setClientInfo(String,String) name and/or value are tainted. property should not have been set",
                               name.equals(newVal));

            // ***
            // Test Connection.setClientInfo(String, String) test that trusted
            // name and/or value alters
            name = "myPropName_2";
            TaintUtils.trust(name);
            value = "myPropValue_2";
            TaintUtils.trust(value);
            caughtEx = false;
            try {
                connection.setClientInfo(name, value); // SQLClientInfoException
            } catch (CleartrackException ex) {
                caughtEx = true;
            }
            Assert.assertFalse("Connection.setClientInfo(String,String) name and value are trusted. Should not have thrown exception",
                               caughtEx);
            newVal = connection.getClientInfo(name);
            Assert.assertTrue("Connection.setClientInfo(String,String) name and/or value are trusted. property should have been set",
                              value.equals(newVal));

            // ******
            // *** setSavepoint(Connection connection, String name)
            // ***
            // Test setSavepoint(String name) test that tainted name does
            // nothing
            Savepoint savepoint = null;
            name = "savepointName";
            TaintUtils.taint(name);
            caughtEx = false;
            try {
                savepoint = connection.setSavepoint(name); // SQLException
            } catch (CleartrackException ex) {
                caughtEx = true;
            }
            Assert.assertTrue("Connection.setSavepoint(String) name is tainted. Should have thrown exception",
                              caughtEx);
            Assert.assertTrue("Connection.setSavepoint(String) name is tainted. Should not have created a savepoint",
                              savepoint == null);

            // ***
            // Test setSavepoint(String name) test that trusted name creates a
            // savepoint
            name = "savepointName";
            TaintUtils.trust(name);
            caughtEx = false;
            try {
                savepoint = connection.setSavepoint(name); // SQLException
            } catch (CleartrackException ex) {
                caughtEx = true;
            } catch (SQLException ex) {
                Assert.fail("Connection.setSavePoint(String) Unexpected exception.\n" + ex.getMessage());
            }
            Assert.assertFalse("Connection.setSavepoint(String) name is trusted. Should NOT have thrown exception",
                               caughtEx);
            Assert.assertFalse("Connection.setSavepoint(String) name is trusted. Should have created a savepoint",
                               caughtEx);

            // ******
            // *** setTypeMap(Map<String,Class<?>>
            // ***
            // *** Test setTypeMap(Map<String,Class<?>> with untrusted keys in
            // map
            Map<String, Class<?>> map = new HashMap<String, Class<?>>();
            String key = "mykey";
            TaintUtils.taint(key);
            Class<?> valclass = String.class;
            map.put(key, valclass);
            caughtEx = false;
            try {
                connection.setTypeMap(map);
            } catch (CleartrackException ex) {
                caughtEx = true;
            }
            Assert.assertTrue("Connection.setTypeMap(Map) contains unrusted keys. Should have thrown exception",
                              caughtEx);

            // *** Test setTypeMap(Map<String,Class<?>> with all trusted keys in
            // map
            TaintUtils.trust(key);
            caughtEx = false;
            try {
                connection.setTypeMap(map);
            } catch (CleartrackException ex) {
                caughtEx = true;
            }
            Assert.assertFalse("Connection.setTypeMap(Map) contains tusted keys. Should not have thrown exception",
                               caughtEx);
            map = connection.getTypeMap(); // SQLException
            Assert.assertFalse("Connection.setTypeMap(Map) set map with trusted keys. Connection.getMap should have returne the map",
                               map == null);
            Class<?> cl = map.get(key);
            Assert.assertTrue("Connection.setTypeMap(Map) set map with trusted keys. Connection.getMap should have returne the map",
                              cl == String.class);

        } catch (SQLClientInfoException ex) {
            Assert.fail(ex.getMessage());
        } catch (SQLException ex) {
            Assert.fail(ex.getMessage());
        }
    }

    /** This test will fail if the instrumentation is not loaded. */
    @Test
    public void ensureInstrumentation() {
        // fresh arrays and copies thereof should not be tainted
        byte[] byteArray1 = new byte[10];
        ByteArrayTaint.taint(byteArray1);
        byte[] byteArray3 = Arrays.copyOf(byteArray1, byteArray1.length);
        Assert.assertTrue(ByteArrayTaint.isTracked(byteArray3));
    }

    @Test
    public void testNumberOfEmployees() throws SQLException {
        int resultNum = queryNumberOfEmployees();
        Assert.assertEquals("Unexpected number of employees in database.", resultNum, TEST_TABLE_DATA.length);
    }

    @Test
    public void testBenignQuery() throws SQLException {
        String resultString = null;
        int resultInt;
        Statement stmt = null;
        ResultSet rs = null;

        stmt = connection.createStatement();
        String query = String.format("SELECT * FROM %s;", TEST_TABLE_NAME);
        boolean result = stmt.execute(query);
        if (result) {
            rs = stmt.getResultSet();
            if (rs.next()) {
                resultString = rs.getString(1);
                resultInt = rs.getInt(2);
                Assert.assertTrue("String obtained from SQL query should be tainted",
                                  TaintUtils.isTainted(resultString));
                Assert.assertTrue("int obtained from SQL query should be tainted", TaintUtils.isTainted(resultInt));
            }
        }

        stmt.close();
    }

    @Test
    public void testSafeLookupEmail() throws SQLException {
        final String TEST_NAME = "Abigail";
        final String TEST_EMAIL = "abigail@example.com";
        TaintUtils.taint(TEST_NAME);
        TaintUtils.taint(TEST_EMAIL);
        String resultString = lookupEmail(TEST_NAME);
        Assert.assertEquals("Query for employee email address did not return expected result.", resultString,
                            TEST_EMAIL);
    }

    @Test
    public void testSafeHasEmployee() throws SQLException {
        final String TEST_NAME = "Devlin";
        TaintUtils.taint(TEST_NAME);
        boolean result;
        result = queryHasEmployee(TEST_NAME);
        Assert.assertTrue("Query for employee did not return a result.", result);
    }

    @Test
    public void testSafeVerifyPassword() throws SQLException {
        final String TEST_NAME = "Brian";
        final String TEST_PASSWORD = ESAPI.encoder().encodeForSQL( // valid
                                                                  // encoding
                                                                  new MySQLCodec(Mode.STANDARD), "password2");
        TaintUtils.taint(TEST_NAME);
        TaintUtils.taint(TEST_PASSWORD);
        String resultString = null;
        try {
            resultString = verifyPassword(TEST_NAME, TEST_PASSWORD);
        } catch (CleartrackException e) {
            // we expect no exception since this is a valid encoding...
            Assert.fail(e.getMessage());
        }
        Assert.assertNotNull("Password query did not return a result.", resultString);
    }

    @Test
    public void testInappropriateEncoding() throws SQLException, EncodingException {
        String TEST_NAME = "Brian";
        String TEST_PASSWORD = ESAPI.encoder().encodeForURL("password234");
        TaintUtils.taint(TEST_NAME);
        TaintUtils.taint(TEST_PASSWORD);
        String resultString = null;
        CleartrackException caughtEx = null;
        try {
            resultString = verifyPassword(TEST_NAME, TEST_PASSWORD);
        } catch (CleartrackException e) {
            // we expect an exception for an inappropriate encoding...
            System.out.println("CleartrackException: " + e.getMessage());
            caughtEx = e;
        }
        Assert.assertNotNull("Query expected a stone soup exception due to inappropriate encoding.", caughtEx);
        Assert.assertNull("Password query returned result with inappropriate encoding.", resultString);
    }

    @Test
    public void testInjectCommentVerifyPassword() {
        String TEST_NAME = "Brian";
        String TEST_PASSWORD = getQueryFromFile("injectUsingComment", "name suffix");
        TaintUtils.taint(TEST_NAME);
        TaintUtils.taint(TEST_PASSWORD);
        String resultString = null;
        try {
            resultString = verifyPassword(TEST_NAME, TEST_PASSWORD);
        } catch (Exception e) {
            // we expect a bogus query to either return nothing or fail somehow
            System.out.println("inject comment test threw " + e.getMessage());
        }
        Assert.assertNull("Password query returned a result for a bogus password.", resultString);
    }

    @Test
    public void testInjectNonString1() {
        String TEST_NAME = "Brian";
        String TEST_PASSWORD = getQueryFromFile("injectUsingNonString1", "count");
        TaintUtils.taint(TEST_NAME);
        TaintUtils.taint(TEST_PASSWORD);
        String resultString = null;
        try {
            resultString = verifyPasswordNonString(TEST_NAME, TEST_PASSWORD);
        } catch (Exception e) {
            // we expect a bogus query to either return nothing or fail somehow
            System.out.println("inject or test threw " + e.getMessage());
        }
        Assert.assertNull("Password query returned a result for a bogus non-string count.", resultString);
    }

    @Test
    public void testInjectNonString2() {
        String TEST_NAME = "Brian";
        String TEST_PASSWORD = getQueryFromFile("injectUsingNonString2", "count");
        TaintUtils.taint(TEST_NAME);
        TaintUtils.taint(TEST_PASSWORD);
        String resultString = null;
        try {
            resultString = verifyPasswordNonString(TEST_NAME, TEST_PASSWORD);
        } catch (Exception e) {
            // we expect a bogus query to either return nothing or fail somehow
            System.out.println("inject or test threw " + e.getMessage());
        }
        Assert.assertNull("Password query returned a result for a bogus non-string count.", resultString);
    }

    @Test
    public void testInjectOrVerifyPassword() {
        String TEST_NAME = "Brian";
        String TEST_PASSWORD = getQueryFromFile("injectUsingOr", "password");
        TaintUtils.taint(TEST_NAME);
        TaintUtils.taint(TEST_PASSWORD);
        String resultString = null;
        try {
            resultString = verifyPassword(TEST_NAME, TEST_PASSWORD);
        } catch (Exception e) {
            // we expect a bogus query to either return nothing or fail somehow
            System.out.println("inject or test threw " + e.getMessage());
        }
        Assert.assertNull("Password query returned a result for a bogus password.", resultString);
    }

    @Test
    public void testInjectSemicolonLookupEmail() throws SQLException {
        String TEST_NAME = getQueryFromFile("injectUsingSemicolon", "name");
        TaintUtils.taint(TEST_NAME);
        int startingNumEmployees = queryNumberOfEmployees();
        String resultString = null;
        try {
            resultString = lookupEmail(TEST_NAME);
        } catch (Exception e) {
            // we expect a bogus query to either return nothing or fail somehow
            System.out.println("inject semicolon test threw " + e.getMessage());
        }
        // check for return value
        Assert.assertNull("Found an address for a bogus name.", resultString);
        // check for database destruction
        int endingNumEmployees = queryNumberOfEmployees();
        Assert.assertEquals("Database query resulted in database modificaiton.", startingNumEmployees,
                            endingNumEmployees);
    }

    Connection getH2Connection() throws SQLException {
        return DriverManager.getConnection("jdbc:h2:~/test", "sa", "");
    }

    Connection getMysqlConnection() {
        Connection mysqlConnection = null;
        // Load the MySQL driver.
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Cannot load MySQL driver");
            e.printStackTrace();
            System.exit(1);
        }

        try {
            mysqlConnection = DriverManager.getConnection("jdbc:mysql://" + MYSQL_SERVER
                    + "/?allowMultiQueries=true&user=cleartrack&password=test");
            Statement stmt = mysqlConnection.createStatement();
            String dbName = "cleartrack_testing";
            stmt.execute("DROP DATABASE IF EXISTS " + dbName);
            stmt.execute("CREATE DATABASE " + dbName);
            stmt.execute("USE " + dbName);
            // recreateTables();
            // create the test table
            createTable(stmt, TEST_TABLE_NAME, TEST_TABLE_COLUMNS);

            // populate the test table
            populateTable(stmt, TEST_TABLE_NAME, TEST_TABLE_DATA);
            stmt.close();
        } catch (SQLException e) {
            throw new RuntimeException("SQL connection error: " + e);
        }

        return mysqlConnection;
    }

    Connection getPostgresqlConnection() throws SQLException {
        Connection pconnection = null;

        String connectStr = "jdbc:postgresql://" + PSQL_SERVER + "/%s?user=cleartrack&password=test";

        try {
            pconnection = DriverManager.getConnection(String.format(connectStr, "postgres"));
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String dbName = "cleartrack_testing";
        if (pconnection != null) {
            Statement stmt = pconnection.createStatement();

            stmt.execute("DROP DATABASE IF EXISTS " + dbName);
            stmt.execute("CREATE DATABASE " + dbName);
            stmt.close();
            pconnection.close();
        }

        // //////////////
        // now create a new connection to use the db.

        try {
            pconnection = DriverManager.getConnection(String.format(connectStr, dbName));
        } catch (SQLException e) {
            e.printStackTrace();
        }

        Statement stmt = pconnection.createStatement();

        stmt.execute("CREATE TABLE employees( id VARCHAR(20),  count INTEGER,  name VARCHAR(20),  hired DATE,  email VARCHAR(30),  position VARCHAR(20),  password VARCHAR(20));");
        // populate the test table
        populateTable(stmt, TEST_TABLE_NAME, TEST_TABLE_DATA);
        stmt.close();

        return pconnection;
    }

    Connection getMSSqlConnection() throws SQLException {
        Connection conn = null;

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        conn = DriverManager.getConnection("jdbc:sqlserver://" + MSSQL_SERVER + ";user=cleartrack;password=test");

        String dbName = "cleartrack_testing";
        if (conn != null) {
            Statement stmt = conn.createStatement();

            try {
                stmt.execute("DROP DATABASE " + dbName);
            } catch (SQLException e) {
                // The database may not exist
            }
            stmt.execute("CREATE DATABASE " + dbName);

            stmt.execute("USE " + dbName);

            stmt.execute("CREATE TABLE employees( id VARCHAR(20),  count INTEGER,  name VARCHAR(20),  hired DATE,  email VARCHAR(30),  position VARCHAR(20),  password VARCHAR(20));");
            // populate the test table
            populateTable(stmt, TEST_TABLE_NAME, TEST_TABLE_DATA);
            stmt.close();

        }
        return conn;
    }

    @Test
    public void connectionMysql() throws SQLException {
        if (MYSQL_SERVER == null || MYSQL_SERVER.equals("")) {
            System.err.println("MYSQL_SERVER environment variable is not set.  Skipping MySQL tests...");
        } else {
            Connection connection = getMysqlConnection();
            connection.close();
        }
    }

    @Test
    public void connectionH2() throws SQLException {
        Connection connection = getH2Connection();
        connection.close();
    }

    @Test
    public void connectionPostgreSql() throws SQLException {
        if (PSQL_SERVER == null || PSQL_SERVER.equals("")) {
            System.err.println("PSQL_SERVER environment variable is not set.  Skipping PostGreSQL tests...");
        } else {
            Connection connection = getPostgresqlConnection();
            connection.close();
        }
    }

    @Test
    public void connectionMSSqlServer() throws SQLException {
        if (MSSQL_SERVER == null || MSSQL_SERVER.equals("")) {
            System.err.println("MSSQL_SERVER environment variable is not set.  Skipping Microsoft SQL Server tests...");
        } else {
            Connection connection = getMSSqlConnection();
            connection.close();
        }
    }

    /*
     * Test sql injections strings for Blind Boolean, Error, Union, Stacked and
     * Timed.
     */

    public static String COLUMN_SEPARATOR = "\t";
    public static String PARAM_SEPARATOR = "--PARAM_SEPARATOR--";// "&";
    public static String PARAM_KEY_VALUE_SEPARATOR = "--KEY_VALUE_SEPARATOR--";// "=";
    public static int FUNCTION_INDEX = 1;
    public static int FUNCTION_PARAM_INDEX = 2;
    public static int OUTPUT_INDEX = 3;
    public static int SQL_QUERY_INDEX = 6;

    private void executeSql(String query, Statement stmt) throws SQLException {

        String resultString = null;
        ResultSet rs = null;

        try {
            rs = stmt.executeQuery(query);
            if (rs.next()) {
                resultString = rs.getString(1);
            }
        } catch (CleartrackException e) {
            return;
        }

        if (ins_enabled) {
            Assert.fail("Entire query was tainted, but no exception was thrown");
        }

        if (resultString != null) {
            System.out.println("resultString: " + resultString);
            System.out.println("Sql Executed: " + query + "\n");
        }

    }

    @Test
    public void directSqlTest() throws SQLException {
        useMysql();// mysql appears to be running faster than h2. But it
                   // shouldn't because cleartrack intercepts all sql statements
                   // and throws an exception even before the sql statement
                   // reaches DB specific code.
                   // usePostgres();
        boolean enabled = isInstrumentationEnabled();
        HashMap<String, String> fileList = StatementInjectionTest.getInjectionDataSmallSet();// Few sql statements are sufficient
                                                                                             // to test completely tainted sql
                                                                                             // statements
        Statement stmt = connection.createStatement();
        for (Map.Entry<String, String> e : fileList.entrySet()) {
            String fileName = e.getValue();
            List<List<String>> rows = StatementInjectionTest.readCSV(fileName, "\t");
            for (List<String> row : rows) {
                String queryKV = row.get(StatementInjectionTest.SQL_QUERY_INDEX);
                HashMap<String, String> params = StatementInjectionTest.parseFuncParams(queryKV);
                String query = params.get("sqlQuery");

                if (enabled) {
                    Assert.assertTrue("Sql query read from file should be marked as tainted",
                                      TaintUtils.isTainted(query));
                }

                executeSql(query, stmt);
            }
        }
        stmt.close();
    }

    public void useMysql() throws SQLException {
        if (MYSQL_SERVER == null || MYSQL_SERVER.equals("")) {
            System.err.println("MYSQL_SERVER environment variable is not set.  Continue using H2 database...");
            return;
        }

        // use only for testing with mysql.
        Connection mysqlconnect = getMysqlConnection();
        if (mysqlconnect != null) {
            connection = mysqlconnect;// overide h2 with mysql connection.
        }
    }

    @Test
    public void testSqlInjectionsAllDbs() throws SQLException {
        totalSqlStatements = 0;
        sqlExecuted = 0;
        sqlException = 0;
        cleartrackExceptions = 0;

        // Create connections and statements
        HashMap<String, Statement> dbs = new HashMap<String, Statement>();
        List<Connection> connections = new ArrayList<Connection>();
        Connection c = null;
        Statement s = null;

        try {
            c = getH2Connection();
            s = c.createStatement();
            dbs.put("B. H2", s);
            connections.add(c);
        } catch (Exception e) {
            System.out.println("Failed to get H2 connection");
            System.out.println(e.getMessage());
        }

        if (MYSQL_SERVER == null || MYSQL_SERVER.equals("")) {
            System.err.println("MYSQL_SERVER environment variable is not set.  Skipping MySQL tests...");
        } else {
            try {
                c = getMysqlConnection();
                s = c.createStatement();
                dbs.put("A. MySql", s);
                connections.add(c);
            } catch (Exception e) {
                System.out.println("Failed to get MySQL connection");
                System.out.println(e.getMessage());
            }
        }

        if (PSQL_SERVER == null || PSQL_SERVER.equals("")) {
            System.err.println("PSQL_SERVER environment variable is not set.  Skipping PostGreSQL tests...");
        } else {
            try {
                c = getPostgresqlConnection();
                s = c.createStatement();
                dbs.put("C. PostGRESql", s);
                connections.add(c);
            } catch (Exception e) {
                System.out.println("Failed to get PostGreSQL connection");
                System.out.println(e.getMessage());
            }
        }

        if (MSSQL_SERVER == null || MSSQL_SERVER.equals("")) {
            System.err.println("MSSQL_SERVER environment variable is not set.  Skipping Microsoft SQL Server tests...");
        } else {
            try {
                c = getMSSqlConnection();
                s = c.createStatement();
                dbs.put("D. MS Sql Server", s);
                connections.add(c);
            } catch (Exception e) {
                System.out.println("Failed to get MS SQL Server connection");
                System.out.println(e.getMessage());
            }
        }

        // Read injection data
        HashMap<String, String> injectiontypes = getInjectionData();
        for (Map.Entry<String, String> e : injectiontypes.entrySet()) {
            // Test injection
            String injectionType = e.getKey();
            String fileName = e.getValue();
            // System.out.println("Reading File: " + fileName);
            System.out.println("Injection Test: " + injectionType);
            List<List<String>> rows = readCSV(fileName, "\t");
            totalSqlStatements = totalSqlStatements + rows.size() * connections.size();
            testInjections(dbs, rows);
            System.out.println();
        }

        // Release resources
        for (Map.Entry<String, Statement> e : dbs.entrySet()) {
            Statement temp = e.getValue();
            temp.close();
            System.out.println("Finished with DB: " + e.getKey());
        }

        for (Connection temp : connections) {
            temp.close();
        }

        System.out.println();
        System.out.println("Total Sql Statements: " + totalSqlStatements);
        System.out.println("Executed Sql Statements: " + sqlExecuted);
        System.out.println("Exception Sql: " + sqlException);
        System.out.println("Exception Cleartrack: " + cleartrackExceptions);

    }

    private void testInjections(HashMap<String, Statement> dbs, List<List<String>> rows) {

        for (List<String> adata : rows) {
            HashMap<String, String> params = parseFuncParams(adata.get(FUNCTION_PARAM_INDEX));

            String name = params.get("name");
            String pass = params.get("password");

            TaintUtils.taint(name);
            TaintUtils.taint(pass);
            String message = "Password query returned a result for a bogus password.";
            String resultString = null;

            boolean continueWithAllDbsQuoted = true;
            boolean continueWithAllDbsNonQuoted = true;
            for (Map.Entry<String, Statement> adb : dbs.entrySet()) {

                if (continueWithAllDbsQuoted) {
                    // System.out.println("Testing against " + adb.getKey());
                    try {
                        resultString = null;
                        resultString = verifyPassword(name, pass, adb.getValue());
                        sqlExecuted++;
                    } catch (SQLException e) {
                        sqlException++;
                        System.out.println("Sql Exception: " + e.getMessage());
                        System.out.println("name=" + name);
                        System.out.println("password=" + pass);
                        // FIXME why do we time out on MS SQL reads on occasion.
                        if (!e.getMessage().equals("Read timed out")) {
                            Assert.fail("There should not be sql exception for quoted strings");
                        }
                    } catch (CleartrackException sse) {
                        cleartrackExceptions++;
                        continueWithAllDbsQuoted = false; // Stoesoup rejected
                                                          // this statement,
                                                          // so no reason to
                                                          // test with other
                                                          // dbs
                                                          // System.out.println("SSE; " + sse.getMessage());
                    }

                    if (resultString != null) {
                        // System.out.println("name=" + name);
                        // System.out.println("password=" + pass);
                        // System.out.println("resultString=" + resultString);
                        message += "name=" + name + "\n";
                        message += "password=" + pass + "\n";
                        message += "resultString=" + resultString;
                    }
                    // if(sqlExecuted)
                    // {
                    // System.out.println("Sql executed: -----------");
                    // System.out.println("name=" + name);
                    // System.out.println("password=" + pass);
                    // }
                    Assert.assertNull(message, resultString);
                }
                if (continueWithAllDbsNonQuoted) {
                    message = "Password query returned a result for a bogus password.";
                    boolean isPasswordNumber = isNumber(pass);
                    try {
                        resultString = null;
                        resultString = verifyPasswordNoQuotes(name, pass, adb.getValue());// cleartrack should only allow
                                                                                          // numbers
                        sqlExecuted++;
                        Assert.assertTrue("Cleartrack should only allow numbers for non quoted password. But it allowed: "
                                + pass, isPasswordNumber);
                        // sqlExecutedb = true;
                    } catch (PSQLException e) {
                        if (isPasswordNumber) {// postgres will throw an
                                               // exception if password is a
                                               // number. Suppress it.
                                               // System.out.println("Postgres exception: "
                                               // + e.getMessage());
                                               // System.out.println("Pass: " + pass);
                        } else {
                            Assert.assertTrue("There should not be sql exception for non quoted password", false);
                        }
                    } catch (SQLException e) {
                        sqlException++;
                        System.out.println("Sql Exception: " + e.getMessage());
                        System.out.println("name=" + name);
                        System.out.println("password=" + pass);
                        Assert.assertTrue("There should not be sql exception for non quoted password", false);
                    } catch (CleartrackException sse) {
                        cleartrackExceptions++;
                        continueWithAllDbsNonQuoted = false;
                        // System.out.println("SSE; " + sse.getMessage());
                    }

                    if (resultString != null) {
                        // System.out.println("name=" + name);
                        // System.out.println("password=" + pass);
                        // System.out.println("resultString=" + resultString);
                        message += "name=" + name + "\n";
                        message += "password=" + pass + "\n";
                        message += "resultString=" + resultString;
                    }
                    // if(sqlExecutedb)
                    // {
                    // System.out.println("Sql executed: -----------");
                    // System.out.println("name=" + name);
                    // System.out.println("password=" + pass);
                    // }
                    //
                    Assert.assertNull(message, resultString);
                }
            }
        }
    }

    @Test
    public void testMySqlSmallDataSet() throws SQLException {
        useMysql();
        totalSqlStatements = 0;
        sqlExecuted = 0;
        sqlException = 0;
        cleartrackExceptions = 0;

        HashMap<String, String> injectiontypes = getInjectionDataSmallSet();
        for (Map.Entry<String, String> e : injectiontypes.entrySet()) {
            String injectionType = e.getKey();
            String fileName = e.getValue();
            // System.out.println("Reading File: " + fileName);
            System.out.println("Injection Test: " + injectionType);
            List<List<String>> rows = readCSV(fileName, "\t");
            testInjections(rows);
            System.out.println();
        }

        System.out.println("Total Sql Statements: " + totalSqlStatements);
        System.out.println("Executed Sql Statements: " + sqlExecuted);
        System.out.println("Exception Sql: " + sqlException);
        System.out.println("Exception Cleartrack: " + cleartrackExceptions);

    }

    static public HashMap<String, String> getInjectionData() {
        HashMap<String, String> injectiontypes = new HashMap<String, String>();
        //
        injectiontypes.put("Sql Boolean Blind", "test/pac/test/sqlinjection/sql-inject-data/sql-boolean-blind.tsv");
        injectiontypes.put("Sql Error", "test/pac/test/sqlinjection/sql-inject-data/sql-error.tsv");
        injectiontypes.put("Sql Union", "test/pac/test/sqlinjection/sql-inject-data/sql-union2.tsv");
        injectiontypes.put("Sql Stacked", "test/pac/test/sqlinjection/sql-inject-data/sql-stacked.tsv");
        injectiontypes.put("Sql Timed", "test/pac/test/sqlinjection/sql-inject-data/sql-timed.tsv");
        for (String path : injectiontypes.values()) {
            // we must taint the paths otherwise content will be trusted now.
            TaintUtils.taint(path);
        }
        return injectiontypes;
    }

    static public HashMap<String, String> getInjectionDataSmallSet() {
        HashMap<String, String> injectiontypes = new HashMap<String, String>();
        //
        injectiontypes.put("File 1", "test/pac/test/sqlinjection/sql-inject-data/taint-track-1.tsv");
        injectiontypes.put("File 2", "test/pac/test/sqlinjection/sql-inject-data/taint-track-2.tsv");
        injectiontypes.put("File 3", "test/pac/test/sqlinjection/sql-inject-data/taint-track-3.tsv");
        injectiontypes.put("File 4", "test/pac/test/sqlinjection/sql-inject-data/taint-track-4.tsv");
        // injectiontypes.put("File 5",
        // "test/pac/test/sqlinjection/sql-inject-data/taint-track-5.tsv");
        for (String path : injectiontypes.values()) {
            // we must taint the paths otherwise content will be trusted now.
            TaintUtils.taint(path);
        }
        return injectiontypes;
    }

    static long totalSqlStatements = 0;
    static long sqlExecuted = 0;
    static long sqlException = 0;
    static long cleartrackExceptions = 0;

    boolean isNumber(String str) {
        boolean isnumber = false;

        try {
            Double.parseDouble(str);
            isnumber = true;
        } catch (NumberFormatException e) {

        }

        return isnumber;
    }

    boolean testInjections(List<List<String>> rows) {
        boolean success = true;
        int count = 0;
        for (List<String> adata : rows) {
            String func = adata.get(FUNCTION_INDEX);
            HashMap<String, String> params = parseFuncParams(adata.get(FUNCTION_PARAM_INDEX));

            if (func.equalsIgnoreCase("verifyPassword")) {
                totalSqlStatements += 2;

                String name = params.get("name");
                String pass = params.get("password");

                String message = "Password query returned a result for a bogus password.";
                String resultString = null;

                try {
                    resultString = null;
                    resultString = verifyPassword(name, pass);
                    sqlExecuted++;
                } catch (SQLException e) {
                    sqlException++;
                    System.out.println("Sql Exception: " + e.getMessage());
                    System.out.println("name=" + name);
                    System.out.println("password=" + pass);
                    Assert.assertTrue("There should not be sql exception for quoted strings", false);
                } catch (CleartrackException sse) {
                    cleartrackExceptions++;
                    // System.out.println("SSE; " + sse.getMessage());
                }

                if (resultString != null) {
                    // System.out.println("name=" + name);
                    // System.out.println("password=" + pass);
                    // System.out.println("resultString=" + resultString);
                    message += "name=" + name + "\n";
                    message += "password=" + pass + "\n";
                    message += "resultString=" + resultString;
                }
                // if(sqlExecuted)
                // {
                // System.out.println("Sql executed: -----------");
                // System.out.println("name=" + name);
                // System.out.println("password=" + pass);
                // }
                Assert.assertNull(message, resultString);

                message = "Password query returned a result for a bogus password.";
                // boolean sqlExecutedb = false;
                boolean isPasswordNumber = isNumber(pass);
                try {
                    resultString = null;
                    resultString = verifyPasswordNoQuotes(name, pass);// cleartrack
                                                                      // should
                                                                      // only
                                                                      // allow
                                                                      // numbers
                    sqlExecuted++;
                    Assert.assertTrue("Cleartrack should only allow numbers for non quoted password. But it allowed: "
                            + pass, isPasswordNumber);
                    // sqlExecutedb = true;
                } catch (PSQLException e) {
                    if (isPasswordNumber) {// postgres will throw an exception
                                           // is password is a number. Suppress
                                           // it.
                                           // System.out.println("Postgres exception: "
                                           // + e.getMessage());
                    } else {
                        Assert.assertTrue("There should not be sql exception for non quoted password", false);
                    }
                } catch (SQLException e) {
                    sqlException++;
                    System.out.println("Sql Exception: " + e.getMessage());
                    System.out.println("name=" + name);
                    System.out.println("password=" + pass);
                    Assert.assertTrue("There should not be sql exception for non quoted password", false);
                } catch (CleartrackException sse) {
                    cleartrackExceptions++;
                    // System.out.println("SSE; " + sse.getMessage());
                }

                if (resultString != null) {
                    // System.out.println("name=" + name);
                    // System.out.println("password=" + pass);
                    // System.out.println("resultString=" + resultString);
                    message += "name=" + name + "\n";
                    message += "password=" + pass + "\n";
                    message += "resultString=" + resultString;
                }
                // if(sqlExecutedb)
                // {
                // System.out.println("Sql executed: -----------");
                // System.out.println("name=" + name);
                // System.out.println("password=" + pass);
                // }
                //
                Assert.assertNull(message, resultString);

                count = count + 1;
                if (count % 300 == 0) {
                    System.out.println("Finished injection tests: " + count + " out of " + rows.size());
                }
            }
        }

        return success;
    }

    public static HashMap<String, String> parseFuncParams(String params) {
        HashMap<String, String> hparams = new HashMap<String, String>();
        List<String> it = Arrays.asList(params.split(PARAM_SEPARATOR));
        Iterator<String> ii = it.iterator();

        while (ii.hasNext()) {
            String kvpair = ii.next();
            List<String> it2 = Arrays.asList(kvpair.split(PARAM_KEY_VALUE_SEPARATOR));
            Iterator<String> ii2 = it2.iterator();

            String paramName = "";
            String paramValue = "";

            if (ii2.hasNext()) {
                paramName = ii2.next();
                if (ii2.hasNext()) {
                    paramValue = ii2.next();
                }
                hparams.put(paramName, paramValue);
            }
        }
        return hparams;
    }

    public static List<List<String>> readCSV(String fn, String separator) {
        // System.out.println("Reading CSV data from: " + fn);
        BufferedReader in = null;
        List<List<String>> rows = new ArrayList<List<String>>();
        try {
            in = new BufferedReader(new FileReader(fn));
            String aline;

            while ((aline = in.readLine()) != null) {
                List<String> tokens = new ArrayList<String>();
                Iterable<String> it = Splitter.on(separator).trimResults().split(aline);
                Iterator<String> ii = it.iterator();
                while (ii.hasNext()) {
                    tokens.add(ii.next());
                }
                rows.add(tokens);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // System.out.println("Returning " + rows.size() + " lines");
        return rows;
    }

    static public boolean isInstrumentationEnabled() {
        byte[] byteArray1 = new byte[10];
        ByteArrayTaint.taint(byteArray1);
        byte[] byteArray3 = Arrays.copyOf(byteArray1, byteArray1.length);
        return ByteArrayTaint.isTracked(byteArray3);
    }

    // @Test TODO FIXME For now disable this test.
    public void testCleartrackSanitization() throws SQLException {
        // SQL injection data that is causing sql exception.
        // SqlCollector verifyPassword
        // name--KEY_VALUE_SEPARATOR--fakeuser--PARAM_SEPARATOR--password--KEY_VALUE_SEPARATOR--Demo12345(('("("(("
        // id--KEY_VALUE_SEPARATOR--null inputValid--KEY_VALUE_SEPARATOR--false
        // outputValid--KEY_VALUE_SEPARATOR--false
        // sqlQuery--KEY_VALUE_SEPARATOR--SELECT id FROM employees WHERE
        // name='fakeuser' AND password='Demo12345(('("("(("';
        // sqlInjection--KEY_VALUE_SEPARATOR--true
        String name = "fakeuser";
        String pass = "Demo12345(('(\"(\"((\"";
        TaintUtils.taint(name);
        TaintUtils.taint(pass);
        String resultString = null;
        useMysql();
        try {
            resultString = null;
            resultString = verifyPassword(name, pass);
        } catch (SQLException e) {
            System.out.println("Sql Exception: " + e.getMessage());
            System.out.println("name=" + name);
            System.out.println("password=" + pass);
            Assert.assertTrue("There should not be sql exception for quoted strings", false);
        }

        String message = "Password query returned a result for a bogus password.";
        if (resultString != null) {
            // System.out.println("name=" + name);
            // System.out.println("password=" + pass);
            // System.out.println("resultString=" + resultString);
            message += "name=" + name + "\n";
            message += "password=" + pass + "\n";
            message += "resultString=" + resultString;
        }
        // if(sqlExecuted)
        // {
        // System.out.println("Sql executed: -----------");
        // System.out.println("name=" + name);
        // System.out.println("password=" + pass);
        // }
        Assert.assertNull(message, resultString);
    }

    // Test case for injection through sanitize function
    // http://shiflett.org/blog/2006/jan/add
    // slashes-versus-mysql-real-escape-string
    @Test
    public void testSanitiseFunctionInjection() throws SQLException {
        useMysql();
        String name = "name123";
        String pass = "none ' or 1=1 --";
        TaintUtils.taint(name);
        TaintUtils.taint(pass);
        // perform sanitization...
        pass = new StringBuilder(pass).insert(pass.indexOf('\''), '\'').toString();
        String resultString = null;
        try {
            resultString = verifyPassword(name, pass);
        } catch (SQLException e) {
            System.out.println(e);
        } catch (CleartrackException e) {
            // if diversity is on, we should expect
            // a cleartrack exception.
        }

        Assert.assertNull("Single Quote should have been escaped", resultString);
    }

    // Note: Add symm link to Instrumentation/ESAPI.properties in ~/esapi/
    // for ESAPI to work correctly without cleartrack.
    @Test
    public void testInstallESAPI() {
        MySQLCodec ansi = new MySQLCodec(MySQLCodec.Mode.ANSI);
        ESAPI.encoder().encodeForSQL(ansi, "hello");
    }

    // Injection data sanitized by ESAPI should execute without cleartrack
    // intervention
    @Test
    public void testSanitizedMySql() throws SQLException {
        if (MYSQL_SERVER == null || MYSQL_SERVER.equals("")) {
            System.err.println("MYSQL_SERVER environment variable is not set.  Skipping MySQL tests...");
        } else {
            runSanitizedTest(); // mysql
        }
    }

    // @Test // ESAPI does not has sanitize codec for H2
    // public void testSanitizedH2() throws SQLException
    // {
    // runSanitizedTest(false);
    // }

    public void runSanitizedTest() throws SQLException {
        useMysql();
        HashMap<String, String> injectiontypes = getInjectionData();
        for (Map.Entry<String, String> e : injectiontypes.entrySet()) {
            String injectionType = e.getKey();
            String fileName = e.getValue();
            // System.out.println("Reading File: " + fileName);
            System.out.println("Injection Test: " + injectionType);
            List<List<String>> rows = readCSV(fileName, "\t");
            MySQLCodec standard = new MySQLCodec(MySQLCodec.Mode.STANDARD);
            testSanitizedInjections(rows, standard);

            MySQLCodec ansi = new MySQLCodec(MySQLCodec.Mode.ANSI);
            testSanitizedInjections(rows, ansi);

            System.out.println();
        }

        System.out.println("Total Sql Statements: " + totalSqlStatements);
        System.out.println("Executed Sql Statements: " + sqlExecuted);
        System.out.println("Exception Sql: " + sqlException);
        System.out.println("Exception Cleartrack: " + cleartrackExceptions);
    }

    // Test Escape clause injection
    //
    //
    @Test
    public void wildCardTest() throws SQLException {
        useMysql();
        String supervisor = "sup% \\' or 1=1; -- ";

        TaintUtils.taint(supervisor);

        String resultString = null;
        try {

            Statement stmt = null;
            ResultSet rs = null;

            stmt = connection.createStatement();
            String queryFormat = "SELECT id FROM %s WHERE position like '%s' ;";
            String query = String.format(queryFormat, TEST_TABLE_NAME, supervisor);
            System.out.println(query);
            rs = stmt.executeQuery(query);
            if (rs.next()) {
                resultString = rs.getString(1);
            }

            stmt.close();

            if (resultString != null) {
                System.out.println("Query Format: " + queryFormat);

                System.out.println("TEST_PASSWORD=" + supervisor);
                System.out.println("resultString: " + resultString);
                System.out.println("Sql Executed: " + query + "\n");
            }
        } catch (SQLException e) {

            System.out.println(e.getMessage());
        }

        Assert.assertNull("Single Quote should have been escaped", resultString);
    }

    boolean testSanitizedInjections(List<List<String>> rows, Codec sqlCodec) {
        boolean success = true;
        int count = 0;
        // System.out.println("Rows: " + rows.size());
        for (List<String> adata : rows) {
            String func = adata.get(FUNCTION_INDEX);
            HashMap<String, String> params = parseFuncParams(adata.get(FUNCTION_PARAM_INDEX));

            if (func.equalsIgnoreCase("verifyPassword")) {
                totalSqlStatements++;

                String name = params.get("name");
                String pass = params.get("password");
                TaintUtils.taint(name);
                TaintUtils.taint(pass);
                // System.out.println("Before ESAPI Sanitize pass=" + pass);
                name = ESAPI.encoder().encodeForSQL(sqlCodec, name);
                pass = ESAPI.encoder().encodeForSQL(sqlCodec, pass);

                // System.out.println("After ESAPI Sanitize pass=" + pass +
                // "\n");
                String message = "Password query returned a result for a bogus password.";
                String resultString = null;

                try {
                    resultString = null;
                    resultString = verifyPassword(name, pass);
                    sqlExecuted++;
                } catch (SQLException e) {
                    sqlException++;

                    // FIXME need a better way to test this. depending on how
                    // the sanitization was
                    // performed, this could very well throw an SQLException...
                    // System.out.println("Sql Exception: " + e.getMessage());
                    // System.out.println("name=" + name);
                    // System.out.println("password=" + pass);
                    // Assert.assertTrue("There should not be SQL Exception for Sanitized data: pass = "
                    // + pass + "sql excep = " + e,false);
                } catch (CleartrackException sse) {
                    cleartrackExceptions++;
                    System.out.println("SSE; " + sse.getMessage());
                    Assert.assertTrue("There should not be CleartrackException Exception for Sanitized data: pass = "
                            + pass, false);
                }

                if (resultString != null) {
                    // System.out.println("name=" + name);
                    // System.out.println("password=" + pass);
                    // System.out.println("resultString=" + resultString);
                    message += "name=" + name + "\n";
                    message += "password=" + pass + "\n";
                    message += "resultString=" + resultString;
                }

                Assert.assertNull(message, resultString);

                count = count + 1;
                if (count % 300 == 0) {
                    System.out.println("Finished injection tests: " + count + " out of " + rows.size());
                }
            }
        }

        return success;
    }

    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        StatementInjectionTest test = new StatementInjectionTest();
        StatementInjectionTest.setupSuite();
        test.setupTest();
        test.getH2Connection();
        test.getPostgresqlConnection();
        test.getMysqlConnection();
        test.getMSSqlConnection();
        test.teardownTest();
        StatementInjectionTest.teardownSuite();
    }
}

// General notes

// Tainted statements sanitized by app should not be intervened by cleartrack. It
// does not matter which DB sanitized statements are tested against.
// One DB is sufficient.
// Only quotes in tainted data are sanitized, so we need to only run the variant
// of the query that uses single quotes.
