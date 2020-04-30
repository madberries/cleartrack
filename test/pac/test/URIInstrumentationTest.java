package pac.test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Test;

import pac.config.CleartrackException;
import pac.util.TaintUtils;
import pac.util.TaintValues;

public class URIInstrumentationTest {

    @Test
    public void uriFileTraversalTest() throws URISyntaxException {
        final String CWD = new File("").getAbsolutePath();

        // Benign Tests...

        String str = TaintUtils.trust("/some/trusted/path/file.txt");
        TaintUtils.mark(str, TaintValues.TAINTED, 19, str.length() - 1);
        URI url = new URI("file", null, str, (String) null);
        Assert.assertEquals(str, url.getPath());

        str = TaintUtils.trust("/some/trusted/path/x/../file.txt");
        TaintUtils.mark(str, TaintValues.TAINTED, 19, str.length() - 1);
        url = new URI("file", null, str, (String) null);
        Assert.assertEquals(str, url.getPath());

        str = TaintUtils.trust("/some/trusted/path/some/dir/../bob/../bob");
        TaintUtils.mark(str, TaintValues.TAINTED, 19, 30);
        url = new URI("file", null, str, (String) null);
        Assert.assertEquals(str, url.getPath());

        // Attack Tests...

        str = TaintUtils.trust("/some/trusted/path/x/../../joe/file.txt");
        TaintUtils.mark(str, TaintValues.TAINTED, 19, str.length() - 1);
        url = new URI("file", null, str, (String) null);
        Assert.assertEquals("/some/trusted/path/x/__/__/joe/file.txt", url.getPath());

        str = TaintUtils.trust(CWD + "/some/dir/../bob/../bob/../../..");
        TaintUtils.mark(str, TaintValues.TAINTED, CWD.length() + 1, CWD.length() + 11);
        TaintUtils.mark(str, TaintValues.TAINTED, CWD.length() + 24, str.length() - 1);
        url = new URI("file", null, str, (String) null);
        Assert.assertEquals(CWD + "/some/dir/__/bob/../bob/__/__/__", url.getPath());

        str = TaintUtils.trust("/some/trusted/path/some/dir/../bob/../bob/../..");
        TaintUtils.mark(str, TaintValues.TAINTED, 19, 30);
        TaintUtils.mark(str, TaintValues.TAINTED, 42, str.length() - 1);
        url = new URI("file", null, str, (String) null);
        Assert.assertEquals("/some/trusted/path/some/dir/__/bob/../bob/__/__", url.getPath());
    }

    // FIXME URI checks are only applied to files, so this test needs rewritten
    //    @Test
    public void URITest() {
        try {
            Boolean caughtEx;
            String scheme = TaintUtils.trust("http");
            String host = TaintUtils.trust("java.sun.com");
            int port = 8080;
            String userInfo = TaintUtils.trust("alfredo");
            String query = TaintUtils.trust("queryString");
            String fragment = TaintUtils.trust("fragment");
            @SuppressWarnings("unused")
            URI uri;
            String path;
            String ssp;

            // ***
            // *** URI(String);
            path = TaintUtils.trust("http://rjk@sweatpea:8080/Users/rjk?query_str?field1=valu");
            path = TaintUtils.taint(path, 24, 33);
            caughtEx = false;
            try {
                uri = new URI(path);
            } catch (CleartrackException ex) {
                caughtEx = true;
                uri = null;
            }
            Assert.assertTrue("URI(String) taint path component starting with slash. Did not throw excpetion",
                              caughtEx);

            path = TaintUtils.trust("http://rjk@sweatpea:8080/Users/rjk?query_str?field1=valu");
            path = TaintUtils.taint(path, 0, 23);
            path = TaintUtils.taint(path, 35, 55);
            caughtEx = false;
            try {
                uri = new URI(path);
            } catch (CleartrackException ex) {
                caughtEx = true;
                uri = null;
            }
            Assert.assertFalse("URI(String) trusted path component threw excpetion", caughtEx);

            // ***
            // Teset URI(String)
            path = TaintUtils.trust("http://java.sun.com/j2se/1.3");
            path = TaintUtils.taint(path, 19, 24);
            caughtEx = false;
            try {
                uri = new URI(path);
            } catch (CleartrackException ex) {
                caughtEx = true;
                uri = null;
            }
            Assert.assertTrue("URI(String) tainted path component starting with slash did not throw excpetion",
                              caughtEx);

            // ******
            // *** URI(String,String,String)
            ssp = TaintUtils.trust("mainto:java-net@java.sun.com");
            caughtEx = false;
            try {
                uri = new URI(scheme, ssp, fragment); // URISyntaxException
            } catch (CleartrackException ex) {
                caughtEx = true;
                uri = null;
            }
            Assert.assertFalse("URI(String) trusted path component threw excpetion", caughtEx);

            // Test URI(String,String,String) with trusted path that begins with slash
            ssp = TaintUtils.trust("//rjk@sweatpea:8080/Users/rjk?query_str?field1=valu");
            caughtEx = false;
            try {
                uri = new URI(scheme, ssp, fragment); // URISyntaxException
            } catch (CleartrackException ex) {
                caughtEx = true;
                uri = null;
            }
            Assert.assertFalse("URI(String,String,int,String) file in ssp is trusted. Should not have thrown exception",
                               caughtEx);

            // ******
            // Test URI(String,String,int,String) with tainted path that begins with slash
            ssp = "//rjk@sweatpea:8080/Users/rjk?query_str?field1=valu";
            TaintUtils.taint(ssp);
            caughtEx = false;
            try {
                uri = new URI(scheme, ssp, fragment); // URISyntaxException
            } catch (CleartrackException ex) {
                caughtEx = true;
                uri = null;
            }
            Assert.assertTrue("URI(String,String,int,String) file in ssp is tainted. Begins with slash. Should have thrown exception",
                              caughtEx);

            // ***
            // Test URI(String,String,int,String) with tainted path that contains attach chars
            ssp = "//rjk@sweatpea:8080/Users/rj%20kramm?query_str?field1=valu";
            TaintUtils.taint(ssp);
            caughtEx = false;
            try {
                uri = new URI(scheme, ssp, fragment); // URISyntaxException
            } catch (CleartrackException ex) {
                caughtEx = true;
                uri = null;
            }
            Assert.assertTrue("URI(String,String,int,String) file in ssp is tainted. Contains illegal chars. Should have thrown exception",
                              caughtEx);

            ssp = "//sweatpea/Users/rjk?query_str?field1=valu";
            TaintUtils.taint(ssp);
            caughtEx = false;
            try {
                uri = new URI(scheme, ssp, fragment); // URISyntaxException
            } catch (CleartrackException ex) {
                caughtEx = true;
                uri = null;
            }
            Assert.assertTrue("URI(String,String,int,String) file in ssp is tainted. Contains illegal chars. Should have thrown exception",
                              caughtEx);

            // ********
            // *** URI(String,String,String,int,String,String,String)
            // ***
            // Test URI(String,String,String,int,String,String,String) with trusted path
            path = TaintUtils.trust("/rjk/path");
            caughtEx = false;
            try {
                uri = new URI(scheme, userInfo, host, port, path, query, fragment); //  URISyntaxException
            } catch (CleartrackException ex) {
                caughtEx = true;
                uri = null;
            }
            Assert.assertFalse("URI(String,String,String,int,String,Sring,String) file is trusted. Should not have thrown exception",
                               caughtEx);

            // ***
            // Test URI(String,String,String,int,String,String,String) with trusted path
            path = TaintUtils.trust("/rjk/path");
            caughtEx = false;
            try {
                uri = new URI(null, userInfo, null, -1, path, query, fragment); //  URISyntaxException
            } catch (CleartrackException ex) {
                caughtEx = true;
                uri = null;
            }
            Assert.assertFalse("URI(String,String,String,int,String,Sring,String) file is trusted. Should not have thrown exception",
                               caughtEx);

            // *******
            // *** URI(String,String,String,String,String)
            //
            // Test URI(String,String,String,String,String) with trusted path
            path = TaintUtils.trust("/rjk/path");
            caughtEx = false;
            try {
                uri = new URI(scheme, host, path, fragment); //  URISyntaxException
            } catch (CleartrackException ex) {
                caughtEx = true;
                uri = null;
            }
            Assert.assertFalse("URI(String,String,String,Sring) file is trusted. Should not have thrown exception",
                               caughtEx);

            // ***
            // Test URI(String,String,String,String,String) with tainted path that begins with slash
            path = "/rjk/path";
            TaintUtils.taint(path);
            caughtEx = false;
            try {
                uri = new URI(scheme, host, path, fragment); //  URISyntaxException
            } catch (CleartrackException ex) {
                caughtEx = true;
                uri = null;
            }
            Assert.assertTrue("URI(String,String,String,Sring) file is tainted. Should have thrown exception",
                              caughtEx);

            // ****
            // *** URI(String,String,String,String,String)
            //
            // Test URI(String,String,String,String,String) with trusted path
            String authority = "rudolfo@host:8088";

            path = TaintUtils.trust("/rjk/path");
            caughtEx = false;
            try {
                uri = new URI(scheme, authority, path, query, fragment); //  URISyntaxException
            } catch (CleartrackException ex) {
                caughtEx = true;
                uri = null;
            }
            Assert.assertFalse("URI(String,String,String,String,String) path is trusted. Should have thrown exception",
                               caughtEx);

            // **
            // Test URI(String,String,String,String,String) with tainted path starting with slash
            path = "/rjk/path";
            TaintUtils.taint(path);
            caughtEx = false;
            try {
                uri = new URI(scheme, authority, path, query, fragment); //  URISyntaxException
            } catch (CleartrackException ex) {
                caughtEx = true;
                uri = null;
            }
            Assert.assertTrue("URI(String,String,String,String,String) path is tainted. Should have thrown exception",
                              caughtEx);

            // **
            // Test URI(String,String,String,String,String) with tainted legal path
            path = "rjk/path";
            TaintUtils.taint(path);
            caughtEx = false;
            try {
                uri = new URI(null, authority, path, query, fragment); //  URISyntaxException
            } catch (CleartrackException ex) {
                caughtEx = true;
                uri = null;
            }
            Assert.assertFalse("URI(String,String,String,String,String) path is tainted but not illegal. Should not have thrown exception",
                               caughtEx);

        } catch (URISyntaxException ex) {
            Assert.fail("URISyntaxException thrown in URI test: " + ex.getMessage());
        }
    }
    
}
