package pac.test;

import java.sql.SQLException;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import pac.util.TaintUtils;

public class SanitizedStatementInjectionTest // extends StatementInjectionTest
{
  StatementInjectionTest sit = new StatementInjectionTest();

  @BeforeClass
  static public void setupSuite() throws SQLException, ClassNotFoundException {
    StatementInjectionTest.setupSuite();
  }

  @AfterClass
  public static void teardownSuite() {
    StatementInjectionTest.teardownSuite();
  }

  @Before
  public void setupTest() throws SQLException {
    sit.setupTest();
  }

  public String sanitizeUsingString1(String str) {
    String result = "";
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      if (c == '\'') {
        result = result + '\'';
        // Assert.assertTrue("Incoming character constant should match the taint value of the
        // previous read character.",
        // CharTaint.equalTaint(str.charAt(i), result.charAt(result.length()-1)));
      }
      result = result + c;
      Assert.assertTrue(
          "Incoming character constant should match the taint value of the previous read character.",
          TaintUtils.hasEqualTaint(str.charAt(i), result.charAt(result.length() - 1)));
    }
    return result;
  }

  public String sanitizeUsingString2(String str) {
    String result = "";
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      result = result + c;
      Assert.assertTrue(
          "Incoming character constant should match the taint value of the previous read character.",
          TaintUtils.hasEqualTaint(str.charAt(i), result.charAt(result.length() - 1)));
      if (c == '\'') {
        result = result + '\'';
        // Assert.assertTrue("Incoming character constant should match the taint value of the
        // previous read character.",
        // CharTaint.equalTaint(str.charAt(i), result.charAt(result.length()-1)));
      }
    }
    return result;
  }

  public String sanitizeUsingStringBuffer1(String str) {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      if (c == '\'') {
        buf.append('\'');
        // Assert.assertTrue("Incoming character constant should match the taint value of the
        // previous read character.",
        // CharTaint.equalTaint(str.charAt(i), buf.charAt(buf.length()-1)));
      }
      buf.append(c);
      Assert.assertTrue(
          "Incoming character constant should match the taint value of the previous read character.",
          TaintUtils.hasEqualTaint(str.charAt(i), buf.charAt(buf.length() - 1)));
    }
    return buf.toString();
  }

  public String sanitizeUsingStringBuffer2(String str) {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      buf.append(c);
      Assert.assertTrue(
          "Incoming character constant should match the taint value of the previous read character.",
          TaintUtils.hasEqualTaint(str.charAt(i), buf.charAt(buf.length() - 1)));
      if (c == '\'') {
        buf.append('\'');
        // Assert.assertTrue("Incoming character constant should match the taint value of the
        // previous read character.",
        // CharTaint.equalTaint(str.charAt(i), buf.charAt(buf.length()-1)));
      }
    }
    return buf.toString();
  }

  @Test
  public void sanitizedInputTest1() {
    final String TEST_NAME = "Brian";
    final String TEST_PASSWORD =
        sanitizeUsingStringBuffer1(sit.getQueryFromFile("injectUsingOr", "password"));
    TaintUtils.taint(TEST_NAME);
    String resultString = null;
    try {
      resultString = sit.verifyPassword(TEST_NAME, TEST_PASSWORD);
    } catch (Exception e) {
      // We expect a bogus query to either return nothing or fail somehow.
      System.out.println("inject or test threw " + e.getMessage());
    }
    Assert.assertNull("Password query returned a result for a bogus password.", resultString);
  }

  // @Test
  public void sanitizedInputTest2() {
    final String TEST_NAME = "Brian";
    final String TEST_PASSWORD =
        sanitizeUsingStringBuffer2(sit.getQueryFromFile("injectUsingOr", "password"));
    TaintUtils.taint(TEST_NAME);
    String resultString = null;
    try {
      resultString = sit.verifyPassword(TEST_NAME, TEST_PASSWORD);
    } catch (Exception e) {
      // We expect a bogus query to either return nothing or fail somehow.
      System.out.println("inject or test threw " + e.getMessage());
    }
    Assert.assertNull("Password query returned a result for a bogus password.", resultString);
  }

  @Test
  public void sanitizedInputTest3() {
    final String TEST_NAME = "Brian";
    final String TEST_PASSWORD =
        sanitizeUsingString1(sit.getQueryFromFile("injectUsingOr", "password"));
    TaintUtils.taint(TEST_NAME);
    String resultString = null;
    try {
      resultString = sit.verifyPassword(TEST_NAME, TEST_PASSWORD);
    } catch (Exception e) {
      // We expect a bogus query to either return nothing or fail somehow.
      System.out.println("inject or test threw " + e.getMessage());
    }
    Assert.assertNull("Password query returned a result for a bogus password.", resultString);
  }

  // @Test
  public void sanitizedInputTest4() {
    final String TEST_NAME = "Brian";
    final String TEST_PASSWORD =
        sanitizeUsingString2(sit.getQueryFromFile("injectUsingOr", "password"));
    TaintUtils.taint(TEST_NAME);
    String resultString = null;
    try {
      resultString = sit.verifyPassword(TEST_NAME, TEST_PASSWORD);
    } catch (Exception e) {
      // We expect a bogus query to either return nothing or fail somehow.
      System.out.println("inject or test threw " + e.getMessage());
    }
    Assert.assertNull("Password query returned a result for a bogus password.", resultString);
  }
}
