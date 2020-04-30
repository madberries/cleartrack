package pac.test;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import pac.util.TaintUtils;
import pac.wrap.ByteArrayTaint;

public class DataInputInstrumentationTest {
    private DataInput dataInput;
    private DataOutputStream dataOutput;

    @Before
    public void setup() {
        PipedInputStream pipeInput = new PipedInputStream();
        dataInput = new DataInputStream(pipeInput);
        PipedOutputStream pipeOut = null;

        try {
            pipeOut = new PipedOutputStream(pipeInput);
        } catch (IOException e) {
            Assert.fail("error opening piped output stream: " + e.getMessage());
        }

        dataOutput = new DataOutputStream(pipeOut);
    }

    @After
    public void teardown() {
    }

    @Test
    public void readFullyTest() {
        byte[] bytesWrite = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 };
        ByteArrayTaint.taint(bytesWrite);

        // test DataInput.readFully(byte[])   DataInput is tainted
        byte[] bytesRead = new byte[10];
        try {
            dataOutput.write(bytesWrite);
        } catch (IOException e) {
            Assert.fail("error writing test bytes: " + e.getMessage());
        }
        try {
            dataInput.readFully(bytesRead);
        } catch (IOException e) {
            Assert.fail("error reading test bytes: " + e.getMessage());
        }
        // make sure the entire input is tainted
        Assert.assertTrue("tainted input not marked as tainted", ByteArrayTaint.isTainted(bytesRead, 0, 9));

        // ***
        // test DataInput.readFully(byte[],int,int)   // DataInput is tainted
        byte[] bytesRead2 = new byte[10];
        try {
            dataInput.readFully(bytesRead2, 3, 5);
        } catch (IOException e) {
            Assert.fail("error reading test bytes: " + e.getMessage());
        }
        Assert.assertFalse("untainted range marked as tainted", ByteArrayTaint.isTainted(bytesRead2, 0, 2));
        Assert.assertTrue("tainted range not marked as tainted", ByteArrayTaint.isTainted(bytesRead2, 3, 7));
        Assert.assertFalse("untainted range marked as tainted", ByteArrayTaint.isTainted(bytesRead2, 8, 9));

        // ***
        // test DataInput.readFully(byte[])   DataInput is trusted
        bytesWrite = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 };
        bytesRead = new byte[10];
        try {
            dataOutput.write(bytesWrite);
        } catch (IOException e) {
            Assert.fail("error writing test bytes: " + e.getMessage());
        }
        try {
            dataInput.readFully(bytesRead);
        } catch (IOException e) {
            Assert.fail("error reading test bytes: " + e.getMessage());
        }
        // make sure the entire input is tainted
        Assert.assertTrue("unstrusted part of byte array marked as trusted", ByteArrayTaint.isTainted(bytesRead, 0, 5));
        Assert.assertTrue("trusted input not marked as trusted", ByteArrayTaint.isTrusted(bytesRead, 6, 9));

        // ***
        // test DataInput.readFully(byte[],int,int)   // DataInput is trusted
        bytesRead = new byte[10];
        try {
            dataInput.readFully(bytesRead, 3, 5);
        } catch (IOException e) {
            Assert.fail("error reading test bytes: " + e.getMessage());
        }
        Assert.assertFalse("untainted range marked as tainted", ByteArrayTaint.isTainted(bytesRead, 0, 2));
        Assert.assertTrue("untainted range marked as tainted", ByteArrayTaint.isTrusted(bytesRead, 3, 7));
        Assert.assertFalse("untainted range marked as tainted", ByteArrayTaint.isTainted(bytesRead, 8, 9));
    }

    @Test
    public void readLineTest() {
        String s = TaintUtils.trust("line1\nline2\nline3\n");
        TaintUtils.taint(s);

        try {
            dataOutput.writeUTF(s);
        } catch (IOException e) {
            Assert.fail("error writing test bytes: " + e.getMessage());
        }

        try {
            for (int i = 0; i < 3; i++) {
                s = dataInput.readLine();
                // make sure the entire input is tainted
                Assert.assertTrue("tainted input not marked as tainted", TaintUtils.isTainted(s, 0, s.length()));
            }
        } catch (IOException e) {
            Assert.fail("error reading test bytes: " + e.getMessage());
        }

        s = TaintUtils.trust("line1\nline2\nline3\n");

        try {
            dataOutput.writeUTF(s);
        } catch (IOException e) {
            Assert.fail("error writing test bytes: " + e.getMessage());
        }
        try {
            for (int i = 0; i < 3; i++) {
                s = dataInput.readLine();
                // test that entire input is trusted
                Assert.assertTrue("tainted input not marked as tainted", TaintUtils.isTrusted(s, 0, s.length() - 1)); // 0, s.length()));
            }
        } catch (IOException e) {
            Assert.fail("error reading test bytes: " + e.getMessage());
        }
    }

    @Test
    public void readUTFTest() {
        String s = TaintUtils.trust("this is a test string");
        TaintUtils.taint(s);

        try {
            dataOutput.writeUTF(s);
        } catch (IOException e) {
            Assert.fail("error writing test bytes: " + e.getMessage());
        }

        try {
            s = dataInput.readUTF();
        } catch (IOException e) {
            Assert.fail("error reading test bytes: " + e.getMessage());
        }

        // make sure the entire input is tainted
        Assert.assertTrue("tainted input not marked as tainted", TaintUtils.isTainted(s, 0, s.length()));

        s = TaintUtils.trust("this is a test string");

        try {
            dataOutput.writeUTF(s);
        } catch (IOException e) {
            Assert.fail("error writing test bytes: " + e.getMessage());
        }

        try {
            s = dataInput.readUTF();
        } catch (IOException e) {
            Assert.fail("error reading test bytes: " + e.getMessage());
        }
        // test that entire input is trusted
        Assert.assertTrue("tainted input not marked as tainted", TaintUtils.isTrusted(s, 0, s.length()));
    }
}
