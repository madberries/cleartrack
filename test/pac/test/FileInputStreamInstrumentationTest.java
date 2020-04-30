package pac.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import org.junit.Assert;
import org.junit.Test;

import pac.inst.taint.FileInputStreamInstrumentation;
import pac.util.TaintUtils;
import pac.wrap.CharArrayTaint;

public class FileInputStreamInstrumentationTest {

    @Test
    public void fileTest() {
        File file;
        String pathName;
        boolean caughtEx;
        FileInputStream fileInputStream = null;
        FileDescriptor fd = null;

        // ***
        // Test that FileInputStream(pathName)  pathName char data is untrusted
        //                                      pathName path is untrusted
        //   o alters attack string "../StaticAnalyzer/README.txt" to __/StaticAnalyzer/README.txt
        //   o then attempts to open __/StaticAnalyzer/README.txt for
        //   o throws FileNotFoundException
        pathName = TaintUtils.trust("../StaticAnalyzer/README.txt");
        TaintUtils.taint(pathName);
        caughtEx = false;
        try {
            fileInputStream = new FileInputStream(pathName); // FileNotFoundException
        } catch (RuntimeException ex) {
            Assert.fail("FileInputStream(String) threw unexcepted RuntimeException");
        } catch (FileNotFoundException ex) {
            caughtEx = true;
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close(); // IOException
                }
            } catch (Exception ex) {
            }
        }
        Assert.assertTrue("FileInputStream(String) failed to throw exception", caughtEx);

        // Test FileInputStream(File) same as above
        file = new File(pathName);
        caughtEx = false;
        try {
            fileInputStream = new FileInputStream(file); // FileNotFoundException
        } catch (RuntimeException ex) {
            Assert.fail("FileInputStream(File) threw unexcepted RuntimeException");
        } catch (FileNotFoundException ex) {
            caughtEx = true;
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close(); // IOException
                }
            } catch (Exception ex) {
            }
        }
        Assert.assertTrue("FileInputStream(File) failed to throw exception", caughtEx);

        // Test FileInputStream(pathName) pathName char data is trusted
        //                                pathName location is untrusted
        // The returned FileInputStream should be untrusted
        pathName = new String("../cleartrack/README.md");
        boolean shouldTrust = FileInputStreamInstrumentation.shouldTrustContent(new File(pathName));
        try {
            fileInputStream = new FileInputStream(pathName); // FileNotFoundException
            fd = fileInputStream.getFD(); // IOException
        } catch (RuntimeException ex) {
            Assert.fail("FileInputStream(String) threw unexpected exception");
        } catch (FileNotFoundException ex) {
            Assert.fail("FileInputStream(String) threw unexpected exception");
        } catch (IOException ex) {
            Assert.fail("FileInputStream(String).getFD() threw unexpected exception");
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close(); // IOException
                }
            } catch (Exception ex) {
            }
        }
        Assert.assertTrue("FileInputStream(String) where String trusted char data but an untrusted location. The returned FileInpuStream was not tainted",
                          TaintUtils.isTainted(fileInputStream) != shouldTrust);
        Assert.assertTrue("FileInputStream(String) returned a tainted FileInputStream. But getFD() then returned an descriptor that was not tainted",
                          TaintUtils.isTainted(fd) != shouldTrust);

        // Test that tainted FileInputStream returns a tainted FileDescriptor
        fileInputStream = new FileInputStream(fd);
        Assert.assertTrue("FileInputStream(FileDescriptor) - FileDescriptor is tainted, but the FileInputStream constructor returned an untainted object.",
                          TaintUtils.isTainted(fileInputStream) == TaintUtils.isTainted(fd));

        // Test FileInputStream(String) same as above; String pathName char data is trusted but location is NOT trusted
        file = new File(pathName);
        shouldTrust = FileInputStreamInstrumentation.shouldTrustContent(file);
        try {
            fileInputStream = new FileInputStream(file); // FileNotFoundException
            fd = fileInputStream.getFD();
        } catch (RuntimeException ex) {
            Assert.fail("FileInputStream(File) threw unexpected exception");
        } catch (FileNotFoundException ex) {
            Assert.fail("FileInputStream(File) threw unexpected exception");
        } catch (IOException ex) {
            Assert.fail("FileInputStream(File).getFD() threw unexpected exception");
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close(); // IOException
                }
            } catch (Exception ex) {
            }
        }
        Assert.assertTrue("FileInputStream(File) where String has trusted char data but has untrusted location. The returned FileInpuStream was not tainted",
                          TaintUtils.isTainted(fileInputStream) != shouldTrust);
        Assert.assertTrue("FileInputStream(File) returned a tainted FileInputStream. But getFD() then returned an descriptor that was not tainted",
                          TaintUtils.isTainted(fd) != shouldTrust);

        // Test FileInputStream(pathName) pathName chardata is trusted
        //                                pathName path is trusted 
        pathName = new String("/usr/bin/file");
        try {
            fileInputStream = new FileInputStream(pathName); // FileNotFoundException
            fd = fileInputStream.getFD();
        } catch (RuntimeException ex) {
            Assert.fail("FileInputStream(String) threw unexpected exception");
        } catch (FileNotFoundException ex) {
            Assert.fail("FileInputStream(String) threw unexpected exception");
        } catch (IOException ex) {
            Assert.fail("FileInputStream(String).getFD() threw unexpected IOException");
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close(); // IOException
                }
            } catch (Exception ex) {
            }
        }
        Assert.assertFalse("FileInputStream(String) where location is trusted. The returned FileInpuStream was tainted",
                           TaintUtils.isTainted(fileInputStream));
        Assert.assertFalse("FileInputStream(String) returned an un tainted FileInputStream. But getFD() then returned an descriptor that was tainted",
                           TaintUtils.isTainted(fd));

        // Test that untainted FileInputStream returns an untainted FileDescriptor
        fileInputStream = new FileInputStream(fd);
        Assert.assertTrue("FileInputStream(FileDescriptor) - FileDescriptor is untainted, but FileInputStream constructor returned a tainted object.",
                          TaintUtils.isTainted(fileInputStream) == TaintUtils.isTainted(fd));

        // Test FileInputStream(File) same as above
        file = new File(pathName);
        try {
            fileInputStream = new FileInputStream(file); // FileNotFoundException
        } catch (RuntimeException ex) {
            Assert.fail("FileInputStream(String) threw unexpected exception");
        } catch (FileNotFoundException ex) {
            Assert.fail("FileInputStream(String) threw unexpected exception");
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close(); // IOException
                }
            } catch (Exception ex) {
            }
        }
        Assert.assertFalse("FileInputStream(File) File location is trusted. The returned FileInpuStream was tainted",
                           TaintUtils.isTainted(fileInputStream));
        // ***

        // ***
        // FileInputStream(File) - File is in untrusted location.
        // Test that constructor returns a tainted FileInputStream.
        // Test that fileInputStream.getFD() return a tainted FileDescriptor from the tainted fileInputStream
        caughtEx = false;
        try {
            String filename = "abc";
            TaintUtils.taint(filename);
            file = File.createTempFile(filename, null); // IOException
            file.deleteOnExit();
            fileInputStream = new FileInputStream(file); // file is not a path listed as trusted in config file.
                                                         // So the returned fileInputStream should tainted
                                                         // FileNotFoundException
            fd = fileInputStream.getFD(); // fileInputStream is tainted. So the return fd should be tainted
                                          // IOException
        } catch (RuntimeException ex) {
            caughtEx = true;
        } catch (FileNotFoundException ex) {
            caughtEx = true;
        } catch (IOException ex) {
            caughtEx = true;
        }

        Assert.assertFalse("FileInputStream.getFD() threw an unexpected IOException", caughtEx);

        Assert.assertTrue("FileInputStream(File) File is in untrusted location but the returned FileInputStream is not tainted",
                          TaintUtils.isTainted(fileInputStream));
        Assert.assertTrue("FileInputStream.getFD() FileInputStream is tainted but the returned FileDescriptor is not tainted",
                          TaintUtils.isTainted(fd));
        // ***

        // Test that FileInputStream is created tainted from from file of untrusted location
        // Test that BufferInputStream is created tainted from tainted FileInputStream
        // Test that data read from tainted BufferInputStream is tainted
        final String prop = System.getProperty("user.dir");
        file = new File(prop, "config_inst");
        shouldTrust = FileInputStreamInstrumentation.shouldTrustContent(file);
        try {
            fileInputStream = new FileInputStream(file); // FileNotFoundException
            InputStreamReader isReader = new InputStreamReader(fileInputStream);
            BufferedReader reader = new BufferedReader(isReader);
            for (int i = 0; i < 3; i++) {
                String line = reader.readLine(); // IOException
                if (line != null && line.length() > 0) {
                    Assert.assertFalse("Data read from BufferedReader comes from a file whose location is not trusted. Data was unexpectedy trusted",
                                       TaintUtils.isTrusted(line) != shouldTrust);
                    Assert.assertTrue("BufferedReader should return untrusted lines of text",
                                      TaintUtils.isTainted(line) != shouldTrust);
                }
            }

            // *** Perform the above test again, using  BufferedRader(Reader, int) constructor
            reader.close(); // IOException

            fileInputStream = new FileInputStream(file); // FileNotFoundException
            isReader = new InputStreamReader(fileInputStream);
            reader = new BufferedReader(isReader, 100);
            for (int i = 0; i < 3; i++) {
                String line = reader.readLine(); // IOException
                if (line != null && line.length() > 0) {
                    Assert.assertFalse("Data read from BufferedReader comes from a file whose location is not trusted. Data was unexpectedy trusted",
                                       TaintUtils.isTrusted(line) != shouldTrust);
                    Assert.assertTrue("BufferedReader should return untrusted lines of text",
                                      TaintUtils.isTainted(line) != shouldTrust);
                }
            }

            // *** Perform the test again.
            // File location still is untrusted
            // This time test BufferedReader.read(char[], int, int)
            reader.close(); // IOException

            fileInputStream = new FileInputStream(file); // FileNotFoundException
            isReader = new InputStreamReader(fileInputStream);
            reader = new BufferedReader(isReader);
            char[] cbuf = new char[100];
            int numread = reader.read(cbuf, // destination
                                      10, // offset in cbuf
                                      80); // max num char allowed to read
                                           // IOException
            if (numread > 0) {
                Assert.assertFalse("Data read from BufferedReader is read into char array starting at index 10. 0 through 9 is unexpectedly tainted.",
                                   CharArrayTaint.isTainted(cbuf, 0, 9));
                Assert.assertTrue("Data read from BufferedReader is read into char array starting at index 10. 0 through 9 is unexpectedly trusted.",
                                  CharArrayTaint.isTrusted(cbuf, 0, 9));
                Assert.assertTrue("Data read from untrusted BufferedReader is unexpectedly not tainted",
                                  CharArrayTaint.isTainted(cbuf, 10, 79) != shouldTrust);
                Assert.assertFalse("Data read from untrusted BufferedReader is unexpectedly trusted",
                                   CharArrayTaint.isTrusted(cbuf, 10, 79) != shouldTrust);

            }

            reader.close();
        } catch (FileNotFoundException ex) {
            Assert.fail("FileInputStream(File) threw FileNotFoundException");
        } catch (IOException ex) {
            Assert.fail("FileInputStream(File) threw IOException");
        } finally {
            try {
                fileInputStream.close();
            } catch (Exception ex) {
            }
        }
        // ***

        // ***
        // Test that untainted FileInputStream will NOT taint InputStreamReader or BufferedReader
        // and so finally the data read is not tainted
        // Test that BufferedReader is trusted when it is created from a trusted InputStream
        // Test that chars read from trusted BufferedReader are themselves trusted
        pathName = "/usr/bin/file";
        TaintUtils.trust(pathName);
        file = new File(pathName);
        try {
            fileInputStream = new FileInputStream(file); // FileNotFoundException
            InputStreamReader isReader = new InputStreamReader(fileInputStream);
            BufferedReader reader = new BufferedReader(isReader);
            for (int i = 0; i < 3; i++) {
                String line = reader.readLine(); // IOException
                if (line != null && line.length() > 0) {
                    Assert.assertTrue("BufferedReader comes from a file whose location is trusted. Yet chars read from BufferedReader are not trusted",
                                      TaintUtils.isTrusted(line));
                    Assert.assertFalse("BufferedReader should return untrusted lines of text",
                                       TaintUtils.isTainted(line));
                }
            }

            // *** Perform the above test again, this time use BufferedRader(Reader, int) constructor
            reader.close(); // IOException
            fileInputStream = new FileInputStream(file); // FileNotFoundException
            isReader = new InputStreamReader(fileInputStream);
            reader = new BufferedReader(isReader, 100);
            for (int i = 0; i < 3; i++) {
                String line = reader.readLine(); // IOException
                if (line != null && line.length() > 0) {
                    Assert.assertTrue("BufferedReader comes from a file whose location is trusted. Yet chars read from BufferedReader are not trusted",
                                      TaintUtils.isTrusted(line));
                    Assert.assertFalse("BufferedReader should return untrusted lines of text",
                                       TaintUtils.isTainted(line));
                }
            }

            // *** Perform the test again. This time test BufferedReader.read(char[], int, int)
            // file comes from trusted location
            reader.close(); // IOException

            fileInputStream = new FileInputStream(file); // FileNotFoundException
            isReader = new InputStreamReader(fileInputStream);
            reader = new BufferedReader(isReader);
            char[] cbuf = new char[120];
            int numread = reader.read(cbuf, // destination
                                      10, // offset in cbuf
                                      80); // max num char allowed to read
                                           // IOException
            if (numread > 0) {
                Assert.assertFalse("Data read from BufferedReader is read into char array starting at index 10. 0 through 9 is unexpectedly tainted.",
                                   CharArrayTaint.isTainted(cbuf, 0, 9));
                Assert.assertTrue("Data read from BufferedReader is read into char array starting at index 10. 0 through 9 is unexpectedly trusted.",
                                  CharArrayTaint.isTrusted(cbuf, 0, 9));
                Assert.assertFalse("Data read from trusted BufferedReader is unexpectedly tainted",
                                   CharArrayTaint.isTainted(cbuf, 10, 79));
                Assert.assertTrue("Data read from untrusted BufferedReader is unexpectedly not trusted",
                                  CharArrayTaint.isTrusted(cbuf, 10, 79));
            }

            reader.close();

        } catch (FileNotFoundException ex) {
            Assert.fail("FileInputStream(File) threw FileNotFoundException");
        } catch (IOException ex) {
            Assert.fail("FileInputStream(File) threw IOException");
        } finally {
            try {
                fileInputStream.close();
            } catch (Exception ex) {
            }
        }
        // ***
    }

    /*
     * TODO we will need to add some instrumentation to trust all jar files
     * if we want to get this test working again.
     * 
     * @Test
     * public void ReadPropertiesAsResource()
     * {
     *     String testKey = "A_PROP";
     *     Properties properties = new Properties();
     *     
     *     try {
     *         JarBean jb = new JarBean();
     *         properties = jb.getProperties();
     *     } catch (Throwable t) {
     *     }
     *     
     *     for(String prop : properties.stringPropertyNames())
     *     {
     *         String pvalue = properties.getProperty(prop);
     *         Assert.assertFalse("Property Key: " + prop + " read from a jar should be trusted",
     *             CharArrayTaint.isTainted(prop));
     *         Assert.assertFalse("Property Value: " + pvalue  + " read from a jar should be trusted",
     *             CharArrayTaint.isTainted(pvalue));
     *         Assert.assertTrue("Property Value: " + pvalue  + " read from a jar should be trusted",
     *             CharArrayTaint.isTrusted(pvalue));
     *     }
     *     Assert.assertTrue("Properties File must have Key: A_PROP", properties.containsKey(testKey));
     * }
     */
}
