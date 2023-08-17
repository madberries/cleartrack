package pac.test;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.URLStreamHandler;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import pac.config.CleartrackException;
import pac.util.TaintUtils;
import pac.util.TaintValues;

public class URLInstrumentationTest {
  private static Random rand = new Random();

  /**
   * @return random taint value.
   */
  private static int randomTaint() {
    int taint = rand.nextInt(TaintValues.TRUST_MASK);
    if (taint == 2)
      return taint + 1;
    return taint;
  }

  /**
   * Creates a random string with random taint that can be encoded.
   * 
   * @param len length of the string to create.
   * @return String
   */
  private static String createRandomString(int len) {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < len; i++) {
      // TODO: Investigate why taint for a space does not properly get decoded.
      char c = (char) (rand.nextInt(94) + 33);
      buf.append(TaintUtils.toString(c, randomTaint()));
    }
    return buf.toString();
  }

  /**
   * Creates a random encoded string such that encoded characters contain mixed taint.
   * 
   * @param len length of the string to create.
   * @return String
   * @throws IOException
   */
  private static String[] createRandomMalformedString(int len) throws IOException {
    StringBuffer encoded = new StringBuffer();
    StringBuffer decoded = new StringBuffer();
    for (int i = 0; i < len; i++) {
      if (rand.nextFloat() >= 0.33f) { // Non-encoded character.
        char c = (char) (rand.nextInt(26) + 'A');
        String s = TaintUtils.toString(c, randomTaint());
        encoded.append(s);
        decoded.append(s);
      } else { // Encoded character.
        // Determine random taint for each character in the encoding such that at least one of them
        // differs.
        int randTaint1 = randomTaint();
        int randTaint2 = randomTaint();
        int randTaint3;
        if (randTaint1 == randTaint2) {
          do {
            randTaint3 = randomTaint();
          } while (randTaint1 == randTaint3 && randTaint2 == randTaint3);
        } else {
          randTaint3 = randomTaint();
        }

        // Append a random encoded character.
        encoded.append(TaintUtils.toString('%', randTaint1));
        String hexStr = Integer.toHexString(32 + rand.nextInt(90)).toUpperCase();
        encoded.append(TaintUtils.toString(hexStr.charAt(0), randTaint2));
        encoded.append(TaintUtils.toString(hexStr.charAt(1), randTaint3));
        if (randTaint1 == TaintValues.TAINTED || randTaint2 == TaintValues.TAINTED
            || randTaint3 == TaintValues.TAINTED)
          decoded.append(TaintUtils.toString('_', TaintValues.TAINTED));
        else if (randTaint1 == TaintValues.UNKNOWN || randTaint2 == TaintValues.UNKNOWN
            || randTaint3 == TaintValues.UNKNOWN)
          decoded.append(TaintUtils.toString('_', TaintValues.UNKNOWN));
        else
          decoded.append(TaintUtils.toString('_', TaintValues.TRUSTED));
      }
    }
    return new String[] {encoded.toString(), decoded.toString()};
  }

  @Test
  public void urlFileTraversalTest() throws MalformedURLException {

    final String CWD = new File("").getAbsolutePath();

    // Benign Tests:

    String str = TaintUtils.trust("/some/trusted/path/file.txt");
    TaintUtils.mark(str, TaintValues.TAINTED, 19, str.length() - 1);
    URL url = new URL("file", null, str);
    Assert.assertEquals(str, url.getPath());

    str = TaintUtils.trust("/some/trusted/path/x/../file.txt");
    TaintUtils.mark(str, TaintValues.TAINTED, 19, str.length() - 1);
    url = new URL("file", null, str);
    Assert.assertEquals(str, url.getPath());

    str = TaintUtils.trust("/some/trusted/path/some/dir/../bob/../bob");
    TaintUtils.mark(str, TaintValues.TAINTED, 19, 30);
    url = new URL("file", null, str);
    Assert.assertEquals(str, url.getPath());

    // Attack Tests:

    str = TaintUtils.trust("/some/trusted/path/x/../../joe/file.txt");
    TaintUtils.mark(str, TaintValues.TAINTED, 19, str.length() - 1);
    url = new URL("file", null, str);
    Assert.assertEquals("/some/trusted/path/x/__/__/joe/file.txt", url.getPath());

    str = TaintUtils.trust(CWD + "/some/dir/../bob/../bob/../../..");
    TaintUtils.mark(str, TaintValues.TAINTED, CWD.length() + 1, CWD.length() + 11);
    TaintUtils.mark(str, TaintValues.TAINTED, CWD.length() + 24, str.length() - 1);
    url = new URL("file", null, str);
    Assert.assertEquals(CWD + "/some/dir/__/bob/../bob/__/__/__", url.getPath());

    str = TaintUtils.trust("/some/trusted/path/some/dir/../bob/../bob/../..");
    TaintUtils.mark(str, TaintValues.TAINTED, 19, 30);
    TaintUtils.mark(str, TaintValues.TAINTED, 42, str.length() - 1);
    url = new URL("file", null, str);
    Assert.assertEquals("/some/trusted/path/some/dir/__/bob/../bob/__/__", url.getPath());
  }

  @Test
  public void urlEncoderTest() throws UnsupportedEncodingException {
    int count = 0;
    while (count < 10000) {
      String orig = createRandomString(50);
      String encoded = URLEncoder.encode(orig, "UTF-8");
      String decoded = URLDecoder.decode(encoded, "UTF-8");
      if (!TaintUtils.hasEqualTaint(orig, decoded, TaintValues.TRUST_MASK)) {
        System.out.println("original:\n" + TaintUtils.createTaintDisplayLines(orig));
        System.out.println("encoded:\n" + TaintUtils.createTaintDisplayLines(encoded));
        System.out.println("decoded:\n" + TaintUtils.createTaintDisplayLines(decoded));
        Assert.fail("original string's taint does not match the encoded string's taint");
      }
      count++;
      if (count % 1000 == 0)
        System.out.println("Finished " + count + " URL encoding/decoding tests");
    }
  }

  @Test
  public void malformedUrlDecoderTest() throws IOException {
    int count = 0;
    while (count < 10000) {
      // Create valid encoded string with malformed taint.
      String[] strs = createRandomMalformedString(50);

      String decoded = URLDecoder.decode(strs[0], "UTF-8");
      if (!TaintUtils.hasEqualTaint(strs[1], decoded)) {
        System.out.println("encoded:\n" + TaintUtils.createTaintDisplayLines(strs[0]));
        System.out.println("decoded:\n" + TaintUtils.createTaintDisplayLines(decoded));
        System.out.println("expected:\n" + TaintUtils.createTaintDisplayLines(strs[1]));
        Assert.fail("decoded malformed URL has unexpected taint");
      }
      count++;
      if (count % 1000 == 0)
        System.out.println("Finished " + count + " malformed URL decoding tests");
    }
  }

  @Test
  public void URLTest() {
    try {
      Boolean caughtEx;
      String protocol_http = "http";
      String host = "www.soc.uts.edu.au";
      int port = 80;
      String file;
      URL url = null;
      String path;

      // **** Test URL(String)
      // Test URL(String) the path part is tainted and contain attack chars.
      //                                         ________________________________
      String spec = "http://www.soc.uts.edu.au:80/MosaicDocs%20ol/url-primer.html";
      spec = TaintUtils.trust(spec);
      spec = TaintUtils.taint(spec, 28, spec.length() - 1);
      caughtEx = false;
      try {
        url = new URL(spec); // MalformedURLException
      } catch (CleartrackException ex) {
        caughtEx = true;
        url = null;
      }
      Assert.assertFalse(
          "URL(String) path component was untrusted and contains attack chars. Should have thrown exception",
          caughtEx);

      //                                                   ________________________________
      spec = TaintUtils.trust("file://www.soc.uts.edu.au:80/MosaicDocs%20ol/url-primer.html");
      spec = TaintUtils.taint(spec, 28, spec.length() - 1);
      caughtEx = false;
      try {
        url = new URL(spec); // MalformedURLException
      } catch (CleartrackException ex) {
        caughtEx = true;
        url = null;
      }
      Assert.assertTrue(
          "URL(String) path component was untrusted and contains attack chars. Should have thrown exception",
          caughtEx);

      // Test URL(String) the path part is tainted and starts with slash.
      //                                                   ______________________________
      spec = TaintUtils.trust("http://www.soc.uts.edu.au:80/MosaicDocs/ol/url-primer.html");
      spec = TaintUtils.taint(spec, 28, spec.length() - 1);
      caughtEx = false;
      try {
        url = new URL(spec); // MalformedURLException
      } catch (CleartrackException ex) {
        caughtEx = true;
        url = null;
      }
      Assert.assertFalse(
          "URL(String) path component was untrusted and begins with slash. Should have thrown exception",
          caughtEx);

      //                                                   ______________________________
      spec = TaintUtils.trust("file://www.soc.uts.edu.au:80/MosaicDocs/ol/url-primer.html");
      spec = TaintUtils.taint(spec, 28, spec.length() - 1);
      caughtEx = false;
      try {
        url = new URL(spec); // MalformedURLException
      } catch (CleartrackException ex) {
        caughtEx = true;
        url = null;
      }
      Assert.assertTrue(
          "URL(String) path component was untrusted and begins with slash. Should have thrown exception",
          caughtEx);

      // ***** Test URL(String,String,int,String)
      // *****
      // Test URL(String,String,int,String) with tainted file that begins with attack slash.
      file = TaintUtils.trust("/my/attack.html");
      TaintUtils.taint(file);
      caughtEx = false;
      try {
        url = new URL(protocol_http, host, port, file); // MalformedURLException
      } catch (CleartrackException ex) {
        caughtEx = true;
        url = null;
      }
      Assert.assertFalse(
          "URL(String,String,int,String) file begins with slash. Should have thrown exception",
          caughtEx);

      final String protocol_file = TaintUtils.trust("file");
      try {
        url = new URL(protocol_file, host, port, file); // MalformedURLException
      } catch (CleartrackException ex) {
        caughtEx = true;
        url = null;
      }
      Assert.assertTrue(
          "URL(String,String,int,String) file begins with slash. Should have thrown exception",
          caughtEx);

      // ***
      // Test URL(String,String,int,String) with trusted file that begins with slash.
      file = TaintUtils.trust("/my/attack.html");
      caughtEx = false;
      try {
        url = new URL(protocol_http, host, port, file); // MalformedURLException
      } catch (CleartrackException ex) {
        caughtEx = true;
        url = null;
      }
      Assert.assertFalse(
          "URL(String,String,int,String) file is trusted. Should not have thrown exception",
          caughtEx);
      if (url != null) {
        path = url.getPath();
        Assert.assertTrue(
            "URL(String,String,int,String)  URL.getPath() failed to return the correct path of file",
            "/my/attack.html".equals(file));
        Assert.assertTrue(
            "URL(String,String,int,String)  file is trusted. URL.getPath() failed to return the trusted path",
            TaintUtils.isTrusted(path));
      }

      try {
        url = new URL(protocol_file, host, port, file); // MalformedURLException
      } catch (CleartrackException ex) {
        caughtEx = true;
        url = null;
      }
      Assert.assertFalse(
          "URL(String,String,int,String) file is trusted. Should not have thrown exception",
          caughtEx);
      if (url != null) {
        path = url.getPath();
        Assert.assertTrue(
            "URL(String,String,int,String)  URL.getPath() failed to return the correct path of file",
            "/my/attack.html".equals(file));
        Assert.assertTrue(
            "URL(String,String,int,String)  file is trusted. URL.getPath() failed to return the trusted path",
            TaintUtils.isTrusted(path));
      }

      // ***
      // Test URL(String,String,int,String) with tainted file that contains internal dot dot (..)
      file = "my/../attack.html";
      TaintUtils.taint(file);
      caughtEx = false;
      try {
        url = new URL(protocol_http, host, port, file); // MalformedURLException
      } catch (CleartrackException ex) {
        caughtEx = true;
        url = null;
      }
      Assert.assertFalse("Should not have thrown exception", caughtEx);

      // ***
      // Test URL(String,String,int,String) with trusted file that contains internal dot dot (..)
      file = TaintUtils.trust("my/../attack.html");
      caughtEx = false;
      try {
        url = new URL(protocol_http, host, port, file); // MalformedURLException
      } catch (CleartrackException ex) {
        caughtEx = true;
        url = null;
      }
      Assert.assertFalse(
          "URL(String,String,int,String) file is trusted. Should not have thrown exception",
          caughtEx);
      if (url != null) {
        path = url.getPath();
        Assert.assertTrue(
            "URL(String,String,int,String)  URL.getPath() failed to return the correct path of file",
            "my/../attack.html".equals(file));
        Assert.assertTrue(
            "URL(String,String,int,String)  file is trusted. URL.getPath() failed to return the trusted path",
            TaintUtils.isTrusted(path));
      }

      caughtEx = false;
      try {
        url = new URL(protocol_file, host, port, file); // MalformedURLException
      } catch (CleartrackException ex) {
        caughtEx = true;
        url = null;
      }
      Assert.assertFalse(
          "URL(String,String,int,String) file is trusted. Should not have thrown exception",
          caughtEx);
      if (url != null) {
        path = url.getPath();
        Assert.assertTrue(
            "URL(String,String,int,String)  URL.getPath() failed to return the correct path of file",
            "my/../attack.html".equals(file));
        Assert.assertTrue(
            "URL(String,String,int,String)  file is trusted. URL.getPath() failed to return the trusted path",
            TaintUtils.isTrusted(path));
      }

      URLStreamHandler handler = null;

      // Test URL(String,String,int,String,URLStreamHandler) with tainted file that begins with
      // attack slash.
      file = "/my/attack.html";
      TaintUtils.taint(file);
      caughtEx = false;
      try {
        url = new URL(protocol_http, host, port, file, handler); // MalformedURLException
      } catch (CleartrackException ex) {
        caughtEx = true;
        url = null;
      }
      Assert.assertFalse(
          "URL(String,String,int,String,URLStreamHandler) file is untrusted and begins with slash. Should have thrown exception",
          caughtEx);

      caughtEx = false;
      try {
        url = new URL(protocol_file, host, port, file, handler); // MalformedURLException
      } catch (CleartrackException ex) {
        caughtEx = true;
        url = null;
      }
      Assert.assertTrue(
          "URL(String,String,int,String,URLStreamHandler) file is untrusted and begins with slash. Should have thrown exception",
          caughtEx);

      // ***
      // Test URL(String,String,int,String) with trusted file.
      file = TaintUtils.trust("/my/attack.html");
      caughtEx = false;
      try {
        url = new URL(protocol_http, host, port, file, handler); // MalformedURLException
      } catch (CleartrackException ex) {
        caughtEx = true;
        url = null;
      }
      Assert.assertFalse(
          "URL(String,String,int,String,URLStreamHandler) file is trusted. Should not have thrown exception",
          caughtEx);
      if (url != null) {
        path = url.getPath();
        Assert.assertTrue(
            "URL(String,String,int,String)  URL.getPath() failed to return the correct path of file",
            "/my/attack.html".equals(file));
        Assert.assertTrue(
            "URL(String,String,int,String)  file is trusted. URL.getPath() failed to return the trusted path",
            TaintUtils.isTrusted(path));
      }

      caughtEx = false;
      try {
        url = new URL(protocol_file, host, port, file, handler); // MalformedURLException
      } catch (CleartrackException ex) {
        caughtEx = true;
        url = null;
      }
      Assert.assertFalse(
          "URL(String,String,int,String,URLStreamHandler) file is trusted. Should not have thrown exception",
          caughtEx);
      if (url != null) {
        path = url.getPath();
        Assert.assertTrue(
            "URL(String,String,int,String)  URL.getPath() failed to return the correct path of file",
            "/my/attack.html".equals(file));
        Assert.assertTrue(
            "URL(String,String,int,String)  file is trusted. URL.getPath() failed to return the trusted path",
            TaintUtils.isTrusted(path));
      }

      // Test URL(String,String,int,String,URLStreamHandler) with tainted file that contains attack
      // dot dot.
      file = "my/../attack.html";
      TaintUtils.taint(file);
      caughtEx = false;
      try {
        url = new URL(protocol_http, host, port, file, handler); // MalformedURLException
      } catch (CleartrackException ex) {
        caughtEx = true;
        url = null;
      }
      Assert.assertFalse("Should not have thrown exception", caughtEx);

      // ***
      // Test URL(String,String,int,String) with trusted file.
      file = TaintUtils.trust("my/../attack.html");
      caughtEx = false;
      try {
        url = new URL(protocol_http, host, port, file, handler); // MalformedURLException
      } catch (CleartrackException ex) {
        caughtEx = true;
        url = null;
      }
      Assert.assertFalse(
          "URL(String,String,int,String,URLStreamHandler) file is trusted. Should not have thrown exception",
          caughtEx);
      if (url != null) {
        path = url.getPath();
        Assert.assertTrue(
            "URL(String,String,int,String)  URL.getPath() failed to return the correct path of file",
            "my/../attack.html".equals(file));
        Assert.assertTrue(
            "URL(String,String,int,String)  file is trusted. URL.getPath() failed to return the trusted path",
            TaintUtils.isTrusted(path));
      }

      caughtEx = false;
      try {
        url = new URL(protocol_file, host, port, file, handler); // MalformedURLException
      } catch (CleartrackException ex) {
        caughtEx = true;
        url = null;
      }
      Assert.assertFalse(
          "URL(String,String,int,String,URLStreamHandler) file is trusted. Should not have thrown exception",
          caughtEx);
      if (url != null) {
        path = url.getPath();
        Assert.assertTrue(
            "URL(String,String,int,String)  URL.getPath() failed to return the correct path of file",
            "my/../attack.html".equals(file));
        Assert.assertTrue(
            "URL(String,String,int,String)  file is trusted. URL.getPath() failed to return the trusted path",
            TaintUtils.isTrusted(path));
      }

      // ***
      // Test URL(String,String,String) with tainted file that begins with attack slash.
      file = "/my/attack.html";
      TaintUtils.taint(file);
      caughtEx = false;
      try {
        url = new URL(protocol_http, host, file); // MalformedURLException
      } catch (CleartrackException ex) {
        caughtEx = true;
        url = null;
      }
      Assert.assertFalse(
          "URL(String,String,String) file is untrusted and begins with slash. Should have thrown exception",
          caughtEx);

      caughtEx = false;
      try {
        url = new URL(protocol_file, host, file); // MalformedURLException
      } catch (CleartrackException ex) {
        caughtEx = true;
        url = null;
      }
      Assert.assertTrue(
          "URL(String,String,String) file is untrusted and begins with slash. Should have thrown exception",
          caughtEx);

      // Test URL(String,String,String) with tainted file that begins with attack slash.
      file = "my%20attack.html";
      TaintUtils.taint(file);
      caughtEx = false;
      try {
        url = new URL(protocol_http, host, file); // MalformedURLException
      } catch (CleartrackException ex) {
        caughtEx = true;
        url = null;
      }
      Assert.assertFalse(
          "URL(String,String,String) file is untrusted and contains an encoding. Should have thrown exception",
          caughtEx);

      caughtEx = false;
      try {
        url = new URL(protocol_file, host, file); // MalformedURLException
      } catch (CleartrackException ex) {
        caughtEx = true;
        url = null;
      }
      Assert.assertTrue(
          "URL(String,String,String) file is untrusted and contains an encoding. Should have thrown exception",
          caughtEx);

      // ***
      // Test URL(URL, String).
      file = TaintUtils.trust("http://www.soc.uts.edu.au/my/legal/");
      url = null; // keep compiler from complaining
      try {
        url = new URL(protocol_http, host, file); // MalformedURLException
      } catch (CleartrackException ex) {
        Assert.fail("URL(String,String,String unexpected exception from trusted file");
      }

      file = TaintUtils.trust("file://www.soc.uts.edu.au/my/legal/");
      url = null; // keep compiler from complaining
      try {
        url = new URL(protocol_file, host, file); // MalformedURLException
      } catch (CleartrackException ex) {
        Assert.fail("URL(String,String,String unexpected exception from trusted file");
      }

      // if file begins with slash, it takes the place of the file in url
      file = "http:///my/attack";
      TaintUtils.taint(file);
      caughtEx = false;
      try {
        new URL(url, file);
      } catch (CleartrackException ex) {
        caughtEx = true;
        url = null;
      }
      Assert.assertFalse(
          "URL(URL,String) file is untrusted and begins with slash. Should have thrown exception",
          caughtEx);

      file = "file:///my/attack";
      TaintUtils.taint(file);
      caughtEx = false;
      try {
        new URL(url, file);
      } catch (CleartrackException ex) {
        caughtEx = true;
        url = null;
      }
      Assert.assertTrue(
          "URL(URL,String) file is untrusted and begins with slash. Should have thrown exception",
          caughtEx);

      // ***
      // Test URL(URL, String, URLStreamHandler).
      file = TaintUtils.trust("http://www.soc.uts.edu.au/my/legal/");
      url = null;
      try {
        url = new URL(protocol_http, host, file); // MalformedURLException
      } catch (CleartrackException ex) {
        Assert.fail("URL(String,String,String unexpected exception from trusted file");
      }

      file = TaintUtils.trust("file://www.soc.uts.edu.au/my/legal/");
      url = null;
      try {
        url = new URL(protocol_http, host, file); // MalformedURLException
      } catch (CleartrackException ex) {
        Assert.fail("URL(String,String,String unexpected exception from trusted file");
      }

      // If file begins with slash, it takes the place of the file in url.
      file = "http:///my/attack";
      TaintUtils.taint(file);
      caughtEx = false;
      try {
        new URL(url, file, handler); // MalformedURLException
      } catch (CleartrackException ex) {
        caughtEx = true;
        url = null;
      }
      Assert.assertFalse(
          "URL(URL,String) file is untrusted and begins with slash. Should have thrown exception",
          caughtEx);

      file = "file:///my/attack";
      TaintUtils.taint(file);
      caughtEx = false;
      try {
        new URL(url, file, handler); // MalformedURLException
      } catch (CleartrackException ex) {
        caughtEx = true;
        url = null;
      }
      Assert.assertTrue(
          "URL(URL,String) file is untrusted and begins with slash. Should have thrown exception",
          caughtEx);

    } catch (MalformedURLException ex) {
      Assert.fail("URL test " + ex.getMessage());
    }
  }
}
