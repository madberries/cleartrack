package pac.test;

import java.io.FileWriter;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import pac.util.TaintUtils;

public class FileWriterInstrumentationTest {

    @Test
    public void fileWriterTest() {
        String pathName;
        boolean caughtEx;
        FileWriter fileWriter = null;

        // ***
        // Test that FileWriter(pathName)
        //   o alters attack string "../Instrumentation/TODO.txt" to __/Instrumentation/TODO.txt
        //   o then attempts to open __/Instrumentation/TODO.txt for writing
        //   o throws IOException (can not be created)
        pathName = "../../Instrumentation/TODO.txt";
        TaintUtils.taint(pathName);
        caughtEx = false;
        try {
            fileWriter = new FileWriter(pathName); // IOException
        } catch (RuntimeException ex) {
            caughtEx = true;
        } catch (IOException ex) {
            caughtEx = true; // Unable to open __/Instrumentation/TODO.txt
        } finally {
            try {
                if (fileWriter != null) {
                    fileWriter.close(); // IOException
                }
            } catch (Exception ex) {
            }
        }
        Assert.assertTrue("FileWriter(String) failed to throw IOException", caughtEx);
        // ***

        // ***
        // Test that FileWriter(pathName)
        //   o alters attack string "../Instrumentation/TODO.txt" to __/Instrumentation/TODO.txt
        //   o then attempts to open __/Instrumentation/TODO.txt for writing
        //   o throws IOException (can not be created)
        pathName = "../../Instrumentation/TODO.txt";
        TaintUtils.taint(pathName);
        caughtEx = false;
        try {
            fileWriter = new FileWriter(pathName, true); // IOException
        } catch (RuntimeException ex) {
            caughtEx = true;
        } catch (IOException ex) {
            caughtEx = true; // Unable to open __/Instrumentation/TODO.txt
        } finally {
            try {
                if (fileWriter != null) {
                    fileWriter.close(); // IOException
                }
            } catch (Exception ex) {
            }
        }
        Assert.assertTrue("FileWriter(String, boolean) failed to throw IOException", caughtEx);
        // ***
    }

}
