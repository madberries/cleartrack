package pac.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import org.junit.Assert;
import org.junit.Test;

import pac.inst.taint.FileInputStreamInstrumentation;
import pac.util.TaintUtils;

public class FileReaderInstrumentationTest {

    @Test
    public void fileReaderTest() {
        FileReader fileReader = null;
        File file;
        String path;

        // ***
        // FileReader(String) constructor. String param is trusted. Its path is refers to trusted location.
        // Test that returned FileReader is not tainted.
        path = TaintUtils.trust("/usr/bin/file");
        try {
            fileReader = new FileReader(path); // FileNotFoundException
            Assert.assertTrue("bytes read from file reader should be trusted", TaintUtils.isTrusted(fileReader.read()));
        } catch (IOException ex) {
            Assert.fail("FileReader(String) threw unexpected exception: " + ex.getMessage());
        } finally {
            try {
                fileReader.close();
            } catch (Exception ex) {
            }
        }

        // Test FileReader(File) with same test as above
        file = new File(path);
        try {
            fileReader = new FileReader(file); // FileNotFoundException
            Assert.assertTrue("bytes read from file reader should be trusted", TaintUtils.isTrusted(fileReader.read()));
        } catch (IOException ex) {
            Assert.fail("FileReader(File) threw unexpected exception");
        } finally {
            try {
                fileReader.close();
            } catch (Exception ex) {
            }
        }
        // ***

        // ***
        // FileReader(String) constructor. String param is not trusted. Its path is refers to trusted location.
        // Test that returned FileReader is not tainted.
        path = TaintUtils.trust("/usr/bin/file");
        try {
            fileReader = new FileReader(path); // FileNotFoundException
            Assert.assertTrue("bytes read from file reader should be trusted", TaintUtils.isTrusted(fileReader.read()));
        } catch (IOException ex) {
            Assert.fail("FileReader(String) threw unexpected exception");
        }

        path = "/usr/bin/file";
        TaintUtils.taint(path);
        try {
            fileReader = new FileReader(path); // FileNotFoundException
            Assert.fail("FileReader should have throw a FileNotFoundException.");
        } catch (FileNotFoundException ex) {

        }

        // Test FileReader(File) with same test as above
        TaintUtils.trust(path);
        file = new File(path);
        try {
            fileReader = new FileReader(file); // FileNotFoundException
            Assert.assertTrue("bytes read from file reader should be trusted", TaintUtils.isTrusted(fileReader.read()));
        } catch (IOException ex) {
            Assert.fail("FileReader(File) threw unexpected exception");
        } finally {
            try {
                fileReader.close();
            } catch (Exception ex) {
            }
        }
        // ***

        // ***
        // FileReader(String) constructor.  String param is trusted. Its path is refers to untrusted location.
        // Test that returned FileReader is tainted.
        // path = "config_inst";
        final String prop = System.getProperty("user.dir");
        file = new File(prop, "config_inst");
        path = file.toString();
        TaintUtils.trust(path); // trusted char data but untrusted location
        boolean shouldTrust = FileInputStreamInstrumentation.shouldTrustContent(file);
        try {
            fileReader = new FileReader(path); // FileNotFoundException
            Assert.assertFalse("bytes read from file reader should not be trusted",
                               TaintUtils.isTrusted(fileReader.read()) != shouldTrust);
        } catch (IOException ex) {
            Assert.fail("FileReader(String) threw unexpected exception.");
        } finally {
            try {
                fileReader.close();
            } catch (Exception ex) {
            }
        }

        // Test fileReader(File) with same test as above
        file = new File(path);
        shouldTrust = FileInputStreamInstrumentation.shouldTrustContent(file);
        try {
            fileReader = new FileReader(file); // FileNotFoundException
            Assert.assertFalse("bytes read from file reader should not be trusted",
                               TaintUtils.isTrusted(fileReader.read()) != shouldTrust);
        } catch (IOException ex) {
            Assert.fail("FileReader(String) threw unexpected exception.");
        } finally {
            try {
                fileReader.close();
            } catch (Exception ex) {
            }
        }
        // ***

        // ***
        // Test that FileReader is tainted when create from untrusted file location
        // Test that BufferedReader inherits taint from Filereader
        // Test that LineNumberReader inherits taint from BufferedReader
        // Test that data read from untrusted LineNumberReader is itself untrusted
        file = new File(prop, "config_inst");
        shouldTrust = FileInputStreamInstrumentation.shouldTrustContent(file);
        try {
            fileReader = new FileReader(file); // FileNotFoundException
            BufferedReader reader = new BufferedReader(fileReader);
            LineNumberReader lr = new LineNumberReader(reader);

            for (int i = 0; i < 5; i++) {
                String line = lr.readLine(); // IOException
                if (line != null && line.length() > 0) {
                    Assert.assertTrue("BufferedReader should return untrusted lines of text",
                                      TaintUtils.isTainted(line) != shouldTrust);
                }
            }

            lr.close();

        } catch (FileNotFoundException ex) {
            Assert.fail("FileReader(String) threw unexpected exception.");
        } catch (IOException ex) {
            Assert.fail("FileReader(String) threw unexpected IOException.");
        } finally {
            try {
                fileReader.close();
            } catch (Exception ex) {
            }
        }
    }
    
}
