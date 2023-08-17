package pac.test;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import pac.util.TaintUtils;

public class PropertiesInstrumentationTest {

  @Test
  public void commandLinePropertyTest() {
    String propVal = System.getProperty("pac.test.testProperty");
    Assert.assertNotNull("pac.test.testProperty is not set", propVal);
    Assert.assertTrue("command line property should be tainted",
        TaintUtils.isAllTainted(propVal, 0, propVal.length()));
  }

  private void assertPropertyTrust(String propName, boolean trust) {
    String trustStr = trust ? "TRUSTED" : "TAINTED";
    Assert.assertTrue(propName + " property should be " + trustStr,
        TaintUtils.isTrusted(System.getProperty(propName)) == trust);
  }

  @Test
  public void standardPropertiesTest() {
    assertPropertyTrust("java.version", true);
    assertPropertyTrust("java.vendor", true);
    assertPropertyTrust("java.vendor.url", true);
    assertPropertyTrust("java.home", true);
    assertPropertyTrust("java.class.version", true);
    assertPropertyTrust("java.class.path", true);
    assertPropertyTrust("os.name", true);
    assertPropertyTrust("os.arch", true);
    assertPropertyTrust("os.version", true);
    assertPropertyTrust("file.separator", true);
    assertPropertyTrust("path.separator", true);
    assertPropertyTrust("line.separator", true);
    // We reset this property to "root" from the command line, so this value should be tainted.
    // TODO: Comment back out when properties are fully working again...
    // assertPropertyTrust("user.name", false);
    assertPropertyTrust("user.home", true);
    assertPropertyTrust("user.dir", true);
  }

  private void assertEnvVarTrust(String envVar, boolean trust) {
    String value = System.getenv(envVar);
    if (value == null)
      return;
    String trustStr = trust ? "TRUSTED" : "TAINTED";
    Assert.assertTrue(envVar + " environment variable should be " + trustStr,
        TaintUtils.isTrusted(value) == trust);
  }

  @Test
  public void standardEnvVarTest() {
    assertEnvVarTrust("JAVA_HOME", true);
    assertEnvVarTrust("OSTYPE", true);
    assertEnvVarTrust("MACHTYPE", true);
  }

  @Test
  public void propertiesTest() {
    try {
      PipedWriter writePipe = new PipedWriter();
      TaintUtils.taint(writePipe);
      PipedReader readPipe = new PipedReader(writePipe);

      BufferedWriter writer = new BufferedWriter(writePipe);

      // ALL PROPERTIES SHOULD BE TRUSTED BASED ON THE CONFIG FILE SETTINGS...

      // ***
      // Test Properties.load(Reader) where Reader is tainted.
      String str = "one=onesy\ntwo=twosy\nthree=threesy\n";
      writer.write(str, 0, str.length()); // IOException
      writer.flush();
      writer.close(); // Must close or whatever reads readPipe hangs.
      Properties props = new Properties();
      props.load(readPipe);
      Enumeration<?> keys = props.propertyNames();
      while (keys.hasMoreElements()) {
        final String key = (String) keys.nextElement();
        final String val = props.getProperty(key);
        Assert.assertTrue(
            "property read from untrusted Reader. The propterty should NOT be trusted",
            TaintUtils.isTainted(val));
      }

      // ***
      // Test Properties.load(Reader) where Reader is trusted.
      writePipe = new PipedWriter();
      TaintUtils.trust(writePipe);
      readPipe = new PipedReader(writePipe);
      writer = new BufferedWriter(writePipe);
      writer.write(str, 0, str.length()); // IOException
      writer.flush();
      writer.close(); // Must close or whatever reads readPipe hangs.
      props = new Properties();
      props.load(readPipe);
      keys = props.propertyNames();
      while (keys.hasMoreElements()) {
        final String key = (String) keys.nextElement();
        final String val = props.getProperty(key);
        Assert.assertTrue("property read from trusted Reader. The propterty is not trusted",
            TaintUtils.isTrusted(val));
      }

      // ***
      // Test Properties.load(writer) where Writer is tainted.
      PipedOutputStream outPipe = new PipedOutputStream();
      TaintUtils.taint(outPipe);
      PipedInputStream inPipe = new PipedInputStream(outPipe);
      BufferedOutputStream outStream = new BufferedOutputStream(outPipe);
      PrintWriter p_writer = new PrintWriter(outStream);
      p_writer.println(str); // , 0, str.length()); // IOException
      p_writer.flush(); // Must flush.
      outStream.close(); // Must close this.
      p_writer.close(); // Must close or whatever reads readPipe hangs.
      props = new Properties();
      props.load(inPipe);
      keys = props.propertyNames();
      while (keys.hasMoreElements()) {
        final String key = (String) keys.nextElement();
        final String val = props.getProperty(key);
        Assert.assertFalse(
            "property read from untrusted InputStream. The propterty should NOT be trusted",
            TaintUtils.isTrusted(val));
      }

      // ***
      // Test Properties.load(writer) where Writer is trusted.
      outPipe = new PipedOutputStream();
      TaintUtils.trust(outPipe);
      inPipe = new PipedInputStream(outPipe);
      outStream = new BufferedOutputStream(outPipe);
      p_writer = new PrintWriter(outStream);
      p_writer.println(str); // , 0, str.length()); // IOException
      p_writer.flush(); // Must flush.
      outStream.close(); // Must close this.
      p_writer.close(); // Must close or whatever reads readPipe hangs.
      props = new Properties();
      props.load(inPipe);
      keys = props.propertyNames();
      while (keys.hasMoreElements()) {
        final String key = (String) keys.nextElement();
        final String val = props.getProperty(key);
        Assert.assertTrue("property read from trusted InputStream. The propterty is trusted",
            TaintUtils.isTrusted(val));
      }

    } catch (IOException ex) {
      Assert.fail(ex.getMessage());
    }

  }

  @Test
  public void propertiesTest_2() {
    Properties properties = System.getProperties();
    Set<String> set = properties.stringPropertyNames();
    for (String str : set) {
      String taint = TaintUtils.createTaintDisplayLines(str);
      if (taint == null)
        System.out.println("NO-TAINT for " + str);
      else
        System.out.println(taint);
    }
  }

}
