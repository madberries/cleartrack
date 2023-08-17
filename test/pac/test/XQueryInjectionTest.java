package pac.test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.xquery.XQConnection;
import javax.xml.xquery.XQDataSource;
import javax.xml.xquery.XQException;
import javax.xml.xquery.XQExpression;
import javax.xml.xquery.XQResultSequence;

import org.junit.Assert;
import org.junit.Test;
import org.owasp.esapi.ESAPI;

import pac.config.CleartrackException;
import pac.util.TaintUtils;

import com.xqjapi.sedna.SednaXQDataSource;

public class XQueryInjectionTest {
  private static String XQUERY_SERVER = System.getenv("XQUERY_SERVER");

  static {
    if (XQUERY_SERVER != null)
      TaintUtils.trust(XQUERY_SERVER);
  }

  @Test
  public void xqueryUsingString() throws XQException {
    if (XQUERY_SERVER == null || XQUERY_SERVER.equals("")) {
      System.err
          .println("XQUERY_SERVER environment variable is not set.  Skipping XQuery tests...");
      return;
    }
    XQDataSource xqs = new SednaXQDataSource();
    xqs.setProperty("serverName", XQUERY_SERVER);
    xqs.setProperty("databaseName", "test");

    XQConnection conn = xqs.getConnection("SYSTEM", "MANAGER");
    XQExpression xqe = conn.createExpression();

    String bookTitle = "Some bogus title' or '1'='1";
    TaintUtils.taint(bookTitle);
    String xqueryString = "doc('books.xml')//book/title/text()='" + bookTitle + "'";

    // Nothing special for now... basically just ensure that normal XPath style query attacks are
    // caught.
    XQResultSequence rs = xqe.executeQuery(xqueryString);

    while (rs.next()) {
      String value = rs.getItemAsString(null);
      Assert.assertTrue(
          "XQuery expression returned '" + value + "' when it " + " should have returned 'false'",
          value.equals("false"));
    }

    conn.close();
  }

  @Test
  public void testInappropriateEncoding() throws XQException {
    if (XQUERY_SERVER == null || XQUERY_SERVER.equals("")) {
      System.err
          .println("XQUERY_SERVER environment variable is not set.  Skipping XQuery tests...");
      return;
    }
    XQDataSource xqs = new SednaXQDataSource();
    xqs.setProperty("serverName", XQUERY_SERVER);
    xqs.setProperty("databaseName", "test");

    XQConnection conn = xqs.getConnection("SYSTEM", "MANAGER");
    XQExpression xqe = conn.createExpression();

    String bookTitle = ESAPI.encoder().encodeForXML("bogus string' or '1'='1");
    TaintUtils.taint(bookTitle);
    String xqueryString = "doc('books.xml')//book/title/text()='" + bookTitle + "'";

    // Nothing special for now... basically just ensure that normal XPath style query attacks are
    // caught.
    CleartrackException caughtEx = null;
    try {
      xqe.executeQuery(xqueryString);
    } catch (CleartrackException e) {
      caughtEx = e;
    }

    conn.close();
    Assert.assertNotNull("Stone soup exception was expected due to inappropriate encoding.",
        caughtEx);
  }

  @Test
  public void xqueryUsingInputStream() throws XQException {
    if (XQUERY_SERVER == null || XQUERY_SERVER.equals("")) {
      System.err
          .println("XQUERY_SERVER environment variable is not set.  Skipping XQuery tests...");
      return;
    }
    XQDataSource xqs = new SednaXQDataSource();
    xqs.setProperty("serverName", XQUERY_SERVER);
    xqs.setProperty("databaseName", "test");

    XQConnection conn = xqs.getConnection("SYSTEM", "MANAGER");
    XQExpression xqe = conn.createExpression();

    String bookTitle = "Some bogus title' or '1'='1";
    TaintUtils.taint(bookTitle);
    String xqueryString = "doc('books.xml')//book/title/text()='" + bookTitle + "'";

    // Nothing special for now... basically just ensure that normal XPath style query attacks are
    // caught.
    XQResultSequence rs = xqe.executeQuery(new ByteArrayInputStream(xqueryString.getBytes()));

    while (rs.next()) {
      String value = rs.getItemAsString(null);
      Assert.assertTrue(
          "XQuery expression returned '" + value + "' when it " + " should have returned 'false'",
          value.equals("false"));
    }

    conn.close();
  }

  @Test
  public void xqueryUsingReader() throws XQException {
    if (XQUERY_SERVER == null || XQUERY_SERVER.equals("")) {
      System.err
          .println("XQUERY_SERVER environment variable is not set.  Skipping XQuery tests...");
      return;
    }
    XQDataSource xqs = new SednaXQDataSource();
    xqs.setProperty("serverName", XQUERY_SERVER);
    xqs.setProperty("databaseName", "test");

    XQConnection conn = xqs.getConnection("SYSTEM", "MANAGER");
    XQExpression xqe = conn.createExpression();

    String bookTitle = "Some bogus title' or '1'='1";
    TaintUtils.taint(bookTitle);
    String xqueryString = "doc('books.xml')//book/title/text()='" + bookTitle + "'";

    // Nothing special for now... basically just ensure that normal XPath style query attacks are
    // caught.
    XQResultSequence rs = xqe.executeQuery(new StringReader(xqueryString));

    while (rs.next()) {
      String value = rs.getItemAsString(null);
      Assert.assertTrue(
          "XQuery expression returned '" + value + "' when it " + " should have returned 'false'",
          value.equals("false"));
    }

    conn.close();
  }

  /**
   * 
   * Injection strings harvested from Xpath Blind Explorer 1.0 tool.
   * 
   * @throws IOException
   * @throws XQException
   */
  @Test
  public void XPathBlindExplorerInjections() throws IOException, XQException {
    if (XQUERY_SERVER == null || XQUERY_SERVER.equals("")) {
      System.err
          .println("XQUERY_SERVER environment variable is not set.  Skipping XQuery tests...");
      return;
    }
    Set<String> injections = getInjections();

    XQDataSource xqs = new SednaXQDataSource();
    xqs.setProperty("serverName", XQUERY_SERVER);
    xqs.setProperty("databaseName", "test");

    XQConnection conn = xqs.getConnection("SYSTEM", "MANAGER");
    XQExpression xqe = conn.createExpression();

    int count = 0;
    int injectedCount = 0;
    for (String bookTitle : injections) {
      TaintUtils.taint(bookTitle);

      // String xqueryString = "doc('books.xml')//book/title/text()='" + bookTitle + "'";

      String xqueryString =
          "for $x in doc('books.xml')//book/title[text()='" + bookTitle + "'] return string($x)";
      boolean injected = executeXQuery(xqe, xqueryString);
      if (injected) {
        injectedCount++;
      }
      count++;
      if (count % 100 == 0) {
        System.out.println("tested injections so far: " + count);
      }

      // Assert.fail("The entire XQuery should have been tainted, and so an exception was
      // expected.");
    }
    conn.close();
    System.out.println("Total Injection Tests: " + count);
    System.out.println("Successfull Injections Count: " + injectedCount);
  }

  private boolean executeXQuery(XQExpression xqe, String xqueryString) throws XQException {
    boolean injected = false;
    try {

      ArrayList<String> results = new ArrayList<String>();
      // System.out.println("Query: " + bookTitle);
      XQResultSequence rs = null;

      try {
        rs = xqe.executeQuery(xqueryString);
      } catch (XQException e) {
        System.out.println("Got bad xquery: " + xqueryString + " exception = " + e);
      }
      while (rs.next()) {
        String value = rs.getItemAsString(null);
        results.add(value);
      }
      Assert.assertTrue(
          "XQuery expression returned '" + results + "' when it " + " should have returned nothing",
          results.size() == 0);
      if (results.size() > 0) {
        injected = true;
        // System.out.println("Injected XQuery: " + bookTitle);
        // System.out.println("Result: " + results);
      }
    } catch (RuntimeException e) {
      System.out.println(e.getMessage());
    }
    return injected;
  }

  public Set<String> getInjections() throws IOException {
    Set<String> injections = new TreeSet<String>();
    BufferedReader br = new BufferedReader(new FileReader("test/pac/test/xquery-injection.csv"));
    String aline;
    int nLines = 0;
    while ((aline = br.readLine()) != null) {
      injections.add(aline);
      nLines++;
    }
    br.close();
    System.out.println("Read injection strings: " + nLines);
    System.out.println("Returning injection strings: " + injections.size());
    return injections;
  }
}
