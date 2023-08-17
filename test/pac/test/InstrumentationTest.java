package pac.test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import pac.util.TaintUtils;
import pac.wrap.ByteArrayTaint;
import pac.wrap.CharArrayTaint;

public class InstrumentationTest {

  @Test
  public void testArraysInstrumentation() {
    // Fresh arrays and copies thereof should not be tainted.
    byte[] byteArray1 = new byte[10];
    byte[] byteArray2 = Arrays.copyOf(byteArray1, byteArray1.length);
    Assert.assertFalse("copy of untainted array marked as tainted",
        ByteArrayTaint.isTainted(byteArray2));

    // Copies of tainted arrays are tainted.
    ByteArrayTaint.taint(byteArray1);
    byte[] byteArray3 = Arrays.copyOf(byteArray1, byteArray1.length);
    Assert.assertTrue("copy of tainted array not tainted", ByteArrayTaint.isTainted(byteArray3));
  }

  @Test
  public void testReaderInstrumentaiton() {
    StringReader reader = new StringReader(TaintUtils.trust("foobar"));
    char[] cbuf = new char[10];
    try {
      reader.read(cbuf);
    } catch (IOException e) {
      Assert.assertTrue(false);
    }
    Assert.assertTrue("StringReader input from trusted String not marked trusted",
        CharArrayTaint.isTrusted(cbuf));
  }

  public void foo() {
    String foo = new String();

    foo.toString();
  }
}
