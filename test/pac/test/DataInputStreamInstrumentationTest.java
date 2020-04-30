package pac.test;

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

public class DataInputStreamInstrumentationTest {
    private DataInputStream dataInput;
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
    public void readTest() {
        byte[] bytesWrite = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 };
        ByteArrayTaint.taint(bytesWrite);
        byte[] bytesRead = new byte[10];

        // ***
        // test DataInput.read(byte[]) with tainted DataInput
        try {
            dataOutput.write(bytesWrite);
        } catch (IOException e) {
            Assert.fail("error writing test bytes: " + e.getMessage());
        }

        int nBytesRead = 0;
        try {
            nBytesRead = dataInput.read(bytesRead);
        } catch (IOException e) {
            Assert.fail("error reading test bytes: " + e.getMessage());
        }

        Assert.assertTrue("no bytes read", nBytesRead > 0);

        // make sure the entire input is tainted
        Assert.assertTrue("tainted input not marked as tainted", ByteArrayTaint.isTainted(bytesRead, 0, 9));

        // ***
        // test DataInput.read(byte[],int,int) with tainted DataInput
        byte[] bytesRead2 = new byte[10];
        try {
            nBytesRead = dataInput.read(bytesRead2, 3, 5);
        } catch (IOException e) {
            Assert.fail("error reading test bytes: " + e.getMessage());
        }

        Assert.assertTrue("no bytes read", nBytesRead > 0);

        Assert.assertFalse("untainted range marked as tainted", ByteArrayTaint.isTainted(bytesRead2, 0, 2));
        Assert.assertTrue("tainted range not marked as tainted", ByteArrayTaint.isTainted(bytesRead2, 3, 7));
        Assert.assertFalse("untainted range marked as tainted", ByteArrayTaint.isTainted(bytesRead2, 8, 9));

        bytesWrite = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 };
        try {
            dataOutput.write(bytesWrite);
        } catch (IOException e) {
            Assert.fail("error writing test bytes: " + e.getMessage());
        }
        try {
            nBytesRead = dataInput.read(bytesRead);
        } catch (IOException e) {
            Assert.fail("error reading test bytes: " + e.getMessage());
        }
        Assert.assertTrue("no bytes read", nBytesRead > 0);
        // test that the entire input is trusted
        Assert.assertTrue("tainted input not marked as tainted", ByteArrayTaint.isTainted(bytesRead, 0, 5));
        Assert.assertTrue("trust input not marked as trust", ByteArrayTaint.isTrusted(bytesRead, 6, 9));

        // ***
        // test DataInput.read(byte[],int,int) with trusted DataInput
        bytesRead = new byte[10];
        try {
            nBytesRead = dataInput.read(bytesRead2, 3, 5);
        } catch (IOException e) {
            Assert.fail("error reading test bytes: " + e.getMessage());
        }

        Assert.assertTrue("no bytes read", nBytesRead > 0);
        Assert.assertTrue("tainted range not marked as tainted",
                          ByteArrayTaint.isTrusted(bytesRead2, 0, bytesRead2.length - 1));
    }

    @Test
    public void readFullyTest() {
        byte[] bytesWrite = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0 };
        ByteArrayTaint.taint(bytesWrite);
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

        byte[] bytesRead2 = new byte[10];
        try {
            dataInput.readFully(bytesRead2, 3, 5);
        } catch (IOException e) {
            Assert.fail("error reading test bytes: " + e.getMessage());
        }

        Assert.assertFalse("untainted range marked as tainted", ByteArrayTaint.isTainted(bytesRead2, 0, 2));
        Assert.assertTrue("tainted range not marked as tainted", ByteArrayTaint.isTainted(bytesRead2, 3, 7));
        Assert.assertFalse("untainted range marked as tainted", ByteArrayTaint.isTainted(bytesRead2, 8, 9));
    }

    @Test
    @SuppressWarnings(value = { "deprecation" })
    public void readLineTest() {
        String s = null;

        try {
            dataOutput.writeUTF("line1\nline2\nline3");
        } catch (IOException e) {
            Assert.fail("error writing test bytes: " + e.getMessage());
        }

        try {
            s = dataInput.readLine();
        } catch (IOException e) {
            Assert.fail("error reading test bytes: " + e.getMessage());
        }

        // make sure the entire input is tainted
        //        Assert.assertTrue("tainted input not marked as tainted", TaintValues.isTracked(s));

        try {
            dataOutput.writeUTF("line1\nline2\nline3");
        } catch (IOException e) {
            Assert.fail("error writing test bytes: " + e.getMessage());
        }
        try {
            s = dataInput.readLine();
        } catch (IOException e) {
            Assert.fail("error reading test bytes: " + e.getMessage());
        }
        // make sure the entire input is tainted
        Assert.assertTrue("tainted input not marked as tainted", TaintUtils.isTrusted(s, 0, s.length() - 1));
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
        Assert.assertTrue("tainted input not marked as tainted", TaintUtils.isTainted(s));

        // ***
        // test DataInputStream.readUTF(dataInput) with tainted DataInput that String read is tainted
        String s2 = TaintUtils.trust("this is yet another test string");
        TaintUtils.taint(s2);
        try {
            dataOutput.writeUTF(s2);
        } catch (IOException e) {
            Assert.fail("error writing test bytes: " + e.getMessage());
        }
        try {
            s2 = DataInputStream.readUTF(dataInput);
        } catch (IOException e) {
            Assert.fail("error reading test bytes: " + e.getMessage());
        }
        // make sure the entire input is tainted
        Assert.assertTrue("tainted input not marked as tainted", TaintUtils.isTainted(s2));

        try {
            dataOutput.writeUTF(TaintUtils.trust("this is a test string"));
        } catch (IOException e) {
            Assert.fail("error writing test bytes: " + e.getMessage());
        }
        try {
            s = dataInput.readUTF();
        } catch (IOException e) {
            Assert.fail("error reading test bytes: " + e.getMessage());
        }
        // make sure the entire input is tainted
        Assert.assertTrue("trusted input not marked as trusted", TaintUtils.isTrusted(s, 0, s.length() - 1));

        // ***
        // test DataInputStream.readUTF(dataInput) with tainted DataInput that String read is trusted
        try {
            dataOutput.writeUTF(TaintUtils.trust("this is yet another test string"));
        } catch (IOException e) {
            Assert.fail("error writing test bytes: " + e.getMessage());
        }
        try {
            s = DataInputStream.readUTF(dataInput);
        } catch (IOException e) {
            Assert.fail("error reading test bytes: " + e.getMessage());
        }
        // make sure the entire input is tainted
        Assert.assertTrue("trusted input not marked as trusted", TaintUtils.isTrusted(s, 0, s.length() - 1));

    }
}
