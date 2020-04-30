package pac.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.jar.JarInputStream;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Assert;
import org.junit.Test;

import pac.inst.taint.FileInputStreamInstrumentation;
import pac.util.TaintUtils;
import pac.wrap.ByteArrayTaint;

/**
 * Tests to ensure that the constructors to InflaterInputStream properly show taint when based on a tainted input stream.
 * @author ppiselli
 *
 */
public class InflaterInputStreamInstrumentationTest {

    @Test
    public void constructerTest() {
        try {
            // ***
            // test InflaterInputStream(FileInputStream) from an untrusted File location
            // all InputStreams created from untrusted location will be trusted
            String str = TaintUtils.trust("test/pac/test/FileInputStreamTestFile");
            File f = new File(str);
            boolean shouldTrust = FileInputStreamInstrumentation.shouldTrustContent(f);
            FileInputStream fis = new FileInputStream(f);
            DeflaterInputStream dis = new DeflaterInputStream(fis);
            InflaterInputStream iis = new InflaterInputStream(dis);
            Assert.assertFalse("InflaterInputStream based on file should not be trusted",
                               TaintUtils.isTrusted(iis.read()) != shouldTrust);
            try {
                iis.close();
            } catch (Exception ex) {
            }

            // ***
            // test InflaterInputStream(FileInputStream) from a trusted File location
            str = TaintUtils.trust("/usr/bin/less"); // all InputStreams created from trusted location will be trusted
            f = new File(str);
            fis = new FileInputStream(f);
            dis = new DeflaterInputStream(fis);
            iis = new InflaterInputStream(dis);
            Assert.assertTrue("InflaterInputStream based on file should be trusted", TaintUtils.isTrusted(iis.read()));

            try {
                if (iis != null) {
                    iis.close();
                }
            } catch (Exception ex) {
            }

        } catch (IOException e) {
            Assert.fail("IOException: " + e.getMessage());
        }
    }

    @Test
    public void gzipTest() {
        try {
            // ***
            // test InflaterInputStream(FileInputStream) from an untrusted File location
            // all InputStreams created from untrusted location will be trusted
            String str = TaintUtils.trust("test/pac/test/FileInputStreamTestFile.gz");
            File f = new File(str);
            boolean shouldTrust = FileInputStreamInstrumentation.shouldTrustContent(f);
            FileInputStream fis = new FileInputStream(f);
            GZIPInputStream gzip = new GZIPInputStream(fis);
            try {
                byte[] bytes = new byte[10];
                gzip.read(bytes);
                gzip.close();
                Assert.assertTrue("all bytes read from tainted zip file should be tainted",
                                  ByteArrayTaint.isTainted(bytes, 0, bytes.length - 1) != shouldTrust);
            } catch (Exception ex) {
                Assert.fail("IOException: " + ex.getMessage());
            }
        } catch (IOException e) {
            Assert.fail("IOException: " + e.getMessage());
        }
    }

    @Test
    public void zipTest() {
        try {
            // ***
            // test InflaterInputStream(FileInputStream) from an untrusted File location
            // all InputStreams created from untrusted location will be trusted
            String str = TaintUtils.trust("test/pac/test/FileInputStreamTestFile.zip");
            File f = new File(str);
            boolean shouldTrust = FileInputStreamInstrumentation.shouldTrustContent(f);
            FileInputStream fis = new FileInputStream(f);
            ZipInputStream zip = new ZipInputStream(fis);
            try {
                byte[] bytes = new byte[10];
                ZipEntry zipEntry = zip.getNextEntry();
                Assert.assertNotNull("zip file should have an entry", zipEntry);
                zip.read(bytes);
                zip.closeEntry();
                zip.close();
                Assert.assertTrue("all bytes read from tainted zip file should be tainted",
                                  ByteArrayTaint.isTainted(bytes, 0, bytes.length - 1) != shouldTrust);
            } catch (Exception ex) {
                Assert.fail("IOException: " + ex.getMessage());
            }
        } catch (IOException e) {
            Assert.fail("IOException: " + e.getMessage());
        }
    }

    @Test
    public void jarTest() {
        try {
            // ***
            // test InflaterInputStream(FileInputStream) from an untrusted File location
            // all InputStreams created from untrusted location will be trusted
            String str = TaintUtils.trust("cleartrack.jar");
            File f = new File(str);
            boolean shouldTrust = FileInputStreamInstrumentation.shouldTrustContent(f);
            FileInputStream fis = new FileInputStream(f);
            JarInputStream jar = new JarInputStream(fis);
            try {
                byte[] bytes = new byte[10];
                int nread = -1;
                do {
                    ZipEntry zipEntry = jar.getNextEntry();
                    Assert.assertNotNull("jar file should have an entry", zipEntry);
                    nread = jar.read(bytes);
                    jar.closeEntry();
                } while (nread < 0);
                jar.close();
                Assert.assertTrue("all bytes read from tainted zip file should be tainted",
                                  ByteArrayTaint.isTainted(bytes, 0, bytes.length - 1) != shouldTrust);
            } catch (Exception ex) {
                Assert.fail("IOException: " + ex.getMessage());
            }
        } catch (IOException e) {
            Assert.fail("IOException: " + e.getMessage());
        }
    }
}
