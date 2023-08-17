package pac.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

import pac.util.TaintUtils;

public class URLConnectionInstrumentationTest {

  private void readUrlStream(String urlPath, boolean shouldTrust)
      throws MalformedURLException, IOException {
    InputStream is = null;
    try {
      is = new URL(urlPath).openStream();
      Assert.assertTrue(
          shouldTrust ? "URLConnection InputStream not trusted"
              : "URLConnection InputStream not tainted",
          TaintUtils.isTrusted(is.read()) == shouldTrust);
    } finally {
      if (is != null)
        is.close();
    }
  }

  @Test
  public void testUrlFileStream() throws MalformedURLException, IOException {
    readUrlStream("file:///dev/zero", false);
    readUrlStream("file:///bin/ls", true);
  }

  @Test
  public void testGetResourceAsStream() throws IOException {
    InputStream in = ClassLoader.getSystemResourceAsStream("sun/nio/cs/ext/sjis0213.dat");
    if (in != null) {
      Assert.assertTrue("URLConnection InputStream not trusted", TaintUtils.isTrusted(in.read()));
    }
  }

}
