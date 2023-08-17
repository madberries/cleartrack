package pac.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import pac.config.CleartrackException;
import pac.config.BaseConfig;
import pac.util.TaintUtils;

public class BackstopConfinementTest {

  // static boolean exit = false;
  // static Socket client;
  // static ServerSocket server;
  // static Thread serverThread;
  static File tempFile = null;
  static PrintWriter pw = null;

  //Commented out to avoid possible deadlock.
  //
  //  @BeforeClass
  //  public static void setup() {
  //    serverThread = new Thread() {
  //      public void run() {
  //        try {
  //          server = new ServerSocket(9999);
  //          server.accept();
  //          while (!exit) {
  //            try {
  //              sleep(1000);
  //            } catch (InterruptedException e) {
  //            }
  //          }
  //          server.close();
  //        } catch (IOException e) {
  //          e.printStackTrace();
  //        }
  //      }
  //    };
  //    serverThread.start();
  //    try {
  //      client = new Socket("localhost", 9999);
  //      OutputStream os = client.getOutputStream();
  //      pw = new PrintWriter(os);
  //    } catch (UnknownHostException e) {
  //      e.printStackTrace();
  //    } catch (IOException e) {
  //      e.printStackTrace();
  //    }
  //  }

  //  @AfterClass
  //  public static void teardown() {
  //      exit = true;
  //      pw.close();
  //      try {
  //          client.close();
  //          serverThread.interrupt();
  //      } catch (IOException e) {
  //          e.printStackTrace();
  //      }
  //  }

  @BeforeClass
  public static void setup() throws IOException {
    // Dummy file used to create a PrintWriter.
    tempFile = File.createTempFile("TempFile", null);
    pw = new PrintWriter(new FileOutputStream(tempFile));
    BaseConfig.getInstance().setIsUnitTesting(pw, true);
  }

  @AfterClass
  public static void teardown() {
    BaseConfig.getInstance().setIsUnitTesting(pw, false);
    pw.close();
    tempFile.delete();
  }

  @Test
  public void testSocket_XQuery() {
    String login = "john";
    String pass = "' or '1'='1";
    TaintUtils.trust(login);
    TaintUtils.taint(pass);
    String xlogin = "//users/user[login/text()='" + login + "' and password/text() = '" + pass
        + "']/home_dir/text()";
    pw.println(xlogin);
    /*
     * String xlogin_fixed = "//users/user[login/text()='" + login + "' and password/text() = \"" +
     * pass + "\"]/home_dir/text()";
     */
    String pass_fixed = pass.replaceAll("'", "''");
    String xlogin_fixed = "//users/user[login/text()='" + login + "' and password/text() = '"
        + pass_fixed + "']/home_dir/text()";
    Assert.assertTrue("Checking of tainted XQUERY expression failed to escape the quotes",
        BaseConfig.getInstance().getSanitizedSocketString().equals(xlogin_fixed));
  }

  @Test
  public void testSocket_SQL() {
    String name = "Abigail";
    String password = "' or '1'='1";
    TaintUtils.trust(name);
    TaintUtils.taint(password);
    String query =
        "SELECT id FROM employees where name = '" + name + "' AND password = '" + password + "';";
    pw.println(query);
    String query_fixed = "SELECT id FROM employees where name = '" + name + "' AND password = '"
        + password.replace("'", "''") + "';";
    Assert.assertTrue("Checking of tainted SQL statement failed to escape the quotes",
        BaseConfig.getInstance().getSanitizedSocketString().equals(query_fixed));
  }

  @Test
  public void testSocket_LDAP() {
    String data = "joe)(|(password=*)";
    TaintUtils.taint(data);
    String search = "(cn=" + data + ")";
    pw.println(search);
    String search_fixed = "(cn=joe\\29\\28|\\28password=\\2a\\29)";
    Assert.assertTrue(
        "Checking of tainted LDAP expression failed to replace the special characters",
        BaseConfig.getInstance().getSanitizedSocketString().equals(search_fixed));
  }

  @Test
  public void testSocket_Command() {
    String cmd = "/bin/ls ./config_inst";
    TaintUtils.trust(cmd);
    cmd = TaintUtils.taint(cmd, 8, 20);
    pw.println(cmd);
    String cmd_fixed = "/bin/ls __config_inst";
    Assert.assertTrue("Checking of tainted command failed to replace the black prefix './'",
        BaseConfig.getInstance().getSanitizedSocketString().equals(cmd_fixed));
  }

  @Test
  public void testSocket_Unknown() {
    boolean caughtEx = false;
    try {
      String data = "data!";
      TaintUtils.taint(data);
      String str = "Test data is " + data + "!";
      pw.println(str);
    } catch (CleartrackException e) {
      caughtEx = true;
    }
    Assert.assertTrue(
        "Checking of tainted non numeric/alphanumeric string failed to throw RuntimeException",
        caughtEx);
  }

}
