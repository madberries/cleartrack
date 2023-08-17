package pac.test;

import java.io.FileInputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.junit.Assert;
import org.junit.Test;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.codecs.MySQLCodec;
import org.owasp.esapi.codecs.MySQLCodec.Mode;
import org.w3c.dom.Document;

import pac.config.CleartrackException;
import pac.util.TaintUtils;

public class XPathInstrumentationTest {

  @Test
  public void verifyPasswordTest() {
    XPath xpath = XPathFactory.newInstance().newXPath();
    String login = "john";
    String pass = "bob";
    TaintUtils.trust(login);
    TaintUtils.taint(pass);
    try {
      XPathExpression xlogin = xpath.compile("//users/user[login/text()='" + login
          + "' and password/text() = '" + pass + "']/home_dir/text()");
      Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder()
          .parse(new FileInputStream("test/pac/test/users.xml"));

      String homedir = xlogin.evaluate(d);
      Assert.assertTrue("Bypassed XPath query using bogus password.",
          homedir == null || homedir.length() == 0);
    } catch (Exception e) {
      Assert.fail("Exception throw: " + e.getMessage());
    }
  }

  @Test
  public void testInappropriateEncoding() {
    XPath xpath = XPathFactory.newInstance().newXPath();
    String login = "john";
    String pass = ESAPI.encoder().encodeForSQL(new MySQLCodec(Mode.STANDARD), "' or '1'='1");
    TaintUtils.trust(login);
    TaintUtils.taint(pass);
    try {
      String test = "//users/user[login/text()='" + login + "' and password/text() = '" + pass
          + "']/home_dir/text()";
      XPathExpression xlogin = xpath.compile(test);
      Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder()
          .parse(new FileInputStream("test/pac/test/users.xml"));
      String homedir = xlogin.evaluate(d);
      Assert.assertTrue("Bypassed XPath query using bogus password.",
          homedir == null || homedir.length() == 0);
    } catch (CleartrackException sse) {
      System.out.println("CleartrackException: " + sse.getMessage());
      return;
    } catch (Exception e) {
      Assert.fail("CleartrackException expected, but received: " + e.getMessage());
    }
    Assert.fail("Inappropriate encoding was not detected.");
  }

}
