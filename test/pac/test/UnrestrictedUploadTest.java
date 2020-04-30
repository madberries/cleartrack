package pac.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import pac.util.TaintUtils;

public class UnrestrictedUploadTest {
    private static final int IO_BUFFER_SIZE = 4 * 1024;
    
    @AfterClass
    public static void cleanup() {
        new File("test.gif").delete();
        new File("test.php").delete();
        new File("test_gif.out").delete();
        new File("test_php.out").delete();
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] b = new byte[IO_BUFFER_SIZE];
        int read;
        while ((read = in.read(b)) != -1) {
            out.write(b, 0, read);
        }
    }

    public void outputTest(byte[] ba) throws IOException {
        // Try to fool file output stream by passing it an
        // open file descriptor...
        String file = "test.php";
        TaintUtils.taint(file);
        FileOutputStream outputFile1 = new FileOutputStream(file);
        FileDescriptor fd = outputFile1.getFD();

        FileOutputStream outputFile2 = new FileOutputStream(fd);
        outputFile2.write(ba);
        outputFile2.close();
        outputFile1.close();
    }

    @Test
    public void fileDescriptorTest() {
        // Test to ensure that we are appropriately
        // tracking file descriptors that refer to
        // tainted files...
        Exception caughtEx = null;
        FileInputStream inputFile1 = null;
        FileDescriptor fd = null;
        String file = TaintUtils.trust("test/pac/test/test.gif");
        TaintUtils.taint(file);
        try {
            inputFile1 = new FileInputStream(file);
            fd = inputFile1.getFD();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
        FileInputStream inputFile2 = new FileInputStream(fd);
        try {
            int b;
            ByteArrayOutputStream bais = new ByteArrayOutputStream();
            while ((b = inputFile2.read()) >= 0) {
                bais.write(b);
            }
            byte[] ba = bais.toByteArray();
            bais.close();
            inputFile2.close();
            inputFile1.close();
            outputTest(ba);
        } catch (IOException e) {
            Assert.fail("unexpected exception: " + e);
        } catch (RuntimeException e) {
            caughtEx = e;
        }
        Assert.assertNotNull("File '" + file + "' was written successfully when a RuntimeException " + "was expected.",
                             caughtEx);
    }

    @Test
    public void dangerousWriteTest() {
        Exception caughtEx = null;
        String file = "test_php.out";
        try {
            TaintUtils.taint(file);
            PrintWriter pw = new PrintWriter(file);
            pw.println("<?php");
            pw.println("   system($_GET['cmd']);");
            pw.println("?>");
            pw.close();
        } catch (FileNotFoundException e) {
            Assert.fail("unexpected exception: " + e);
        } catch (RuntimeException e) {
            caughtEx = e;
        }
        Assert.assertNotNull("File '" + file + "' was written successfully when a RuntimeException " + "was expected.",
                             caughtEx);
    }

    @Test
    public void wrongExtensionTest() {
        Exception caughtEx = null;
        String fileIn = TaintUtils.trust("test/pac/test/test.gif");
        String fileOut = "test_gif.out";
        try {
            fileIn = TaintUtils.taint(fileIn, 3, fileIn.length() - 1);
            TaintUtils.taint(fileOut);
            FileInputStream in = new FileInputStream(fileIn);
            FileOutputStream out = new FileOutputStream(fileOut);
            copy(in, out);
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            Assert.fail("unexpected exception: " + e);
        } catch (IOException e) {
            Assert.fail("unexpected exception: " + e);
        } catch (RuntimeException e) {
            caughtEx = e;
        }
        Assert.assertNotNull("File '" + fileOut + "' was written successfully when a RuntimeException "
                + "was expected.", caughtEx);
    }

    @Test
    public void benignWriteTest() {
        Exception caughtEx = null;
        String fileIn = TaintUtils.trust("test/pac/test/test.gif");
        String fileOut = "test.gif";
        try {
            fileIn = TaintUtils.taint(fileIn, 3, fileIn.length() - 1);
            TaintUtils.taint(fileOut);
            FileInputStream in = new FileInputStream(fileIn);
            FileOutputStream out = new FileOutputStream(fileOut);
            copy(in, out);
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            Assert.fail("unexpected exception: " + e);
        } catch (IOException e) {
            Assert.fail("unexpected exception: " + e);
        } catch (RuntimeException e) {
            caughtEx = e;
        }
        Assert.assertNull("File '" + fileOut + "' threw a runtime exception upon writing "
                + "the file, when no exception was expected.", caughtEx);
    }
}
