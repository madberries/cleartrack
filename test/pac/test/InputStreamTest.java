package pac.test;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.io.SequenceInputStream;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.Random;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import pac.inst.taint.FileInputStreamInstrumentation;
import pac.util.StringRegionIterator;
import pac.util.StringRegionIterator.StringRegion;
import pac.util.TaintUtils;
import pac.util.TaintValues;
import pac.wrap.ByteArrayTaint;
import pac.wrap.CharArrayTaint;

public class InputStreamTest {
    public static final int NONPRIM_MASK = (1 << (2 + TaintValues.NUM_OF_ENCODINGS + TaintValues.NUM_OF_INPUTTYPES))
            - 1;
    private InputStream testInput;
    private OutputStream outputToTestInput;

    @Before
    public void setup() {
        testInput = new PipedInputStream();
        try {
            outputToTestInput = new PipedOutputStream((PipedInputStream) testInput);
        } catch (IOException e) {
            Assert.fail("unable to prepare output to test input stream: " + e.getMessage());
        }
    }

    @After
    public void teardown() {

    }

    private void prepareInput(String s) {
        try {
            outputToTestInput.write(s.getBytes());
        } catch (IOException e) {
            Assert.fail("error writing to test input stream: " + e.getMessage());
        }
    }

    @Test
    public void bufferedInputStreamTest() {

        BufferedInputStream bis = new BufferedInputStream(testInput);

        // ***
        // test BufferedInputStream.read(byte[])  BufferedInputStream is tainted
        byte[] b1 = new byte[100];
        int b1read = 0;

        String s = "have some test input";
        TaintUtils.taint(s);
        prepareInput(s);

        // Note: This calls InputStreamInstrumentation.read(byte[]) even if
        try {
            b1read = bis.read(b1);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        if (b1read > 0) {
            Assert.assertTrue("tainted area not marked tainted", ByteArrayTaint.isTainted(b1, 0, b1read - 1));
            Assert.assertFalse("untainted area marked tainted", ByteArrayTaint.isTainted(b1, b1read, 99));
        } else {
            Assert.fail("no input read from test input stream");
        }

        // ***
        // test BufferedInputStream.read(byte[],int,int)  BufferedInputStream is tainted
        byte[] b2 = new byte[100];
        int b2read = 0;

        s = "have some more input";
        TaintUtils.taint(s);
        prepareInput(s);

        try {
            b2read = bis.read(b2, 10, b2.length - 10);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        if (b2read > 0) {
            Assert.assertFalse("untainted area marked tainted", ByteArrayTaint.isTainted(b2, 0, 9));
            Assert.assertTrue("tainted area not marked tainted", ByteArrayTaint.isTainted(b2, 10, 10 + b2read - 1));
            Assert.assertFalse("untainted area marked tainted", ByteArrayTaint.isTainted(b2, 10 + b2read, 99));
        } else {
            Assert.fail("no input read from test input stream");
        }

        // ***
        // test BufferedInputStream.read(byte[])  BufferedInputStream is trusted
        b1 = new byte[100];
        int numread = 0;
        prepareInput(TaintUtils.trust("have some test input"));
        try {
            numread = bis.read(b1);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        if (numread > 0) {
            Assert.assertTrue("trusted area not marked tainted", ByteArrayTaint.isTrusted(b1, 0, numread - 1));
            Assert.assertFalse("untainted area marked tainted", ByteArrayTaint.isTainted(b1, numread, 99));
        } else {
            Assert.fail("no input read from test input stream");
        }

        // test BufferedInputStream.read(byte[],int,int)  BufferedInputStream is tainted
        b2 = new byte[100];
        b2read = 0;
        prepareInput(TaintUtils.trust("have some more input"));
        try {
            b2read = bis.read(b2, 10, b2.length - 10);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        if (b2read > 0) {
            Assert.assertFalse("unknown area marked tainted", ByteArrayTaint.isTainted(b2, 0, 9));
            Assert.assertTrue("trusted area not marked trusted", ByteArrayTaint.isTrusted(b2, 10, 10 + b2read - 1));
            Assert.assertFalse("unknonw area marked tainted", ByteArrayTaint.isTainted(b2, 10 + b2read, 99));
        } else {
            Assert.fail("no input read from test input stream");
        }
    }

    @Test
    public void byteArrayInputStreamTest() {

        // ***
        // Test ByteArrayInputStream.read(byte[]) that tainted ByteArrayInputStream writes tainted data to byte array
        //
        byte[] buffer1 = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        ByteArrayTaint.taint(buffer1);
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer1);
        //        Assert.assertTrue("ByteArrayInpustream is not tainted though it was created from a tained byte array", CharArrayTaint.isTainted(bais));
        byte[] b1 = new byte[100];
        int numread = 0;

        try {
            numread = bais.read(b1);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        if (numread > 0) {
            Assert.assertTrue("tainted area not marked tainted", ByteArrayTaint.isTainted(b1, 0, numread - 1));
            Assert.assertFalse("untainted area marked tainted", ByteArrayTaint.isTainted(b1, numread, 99));
        } else {
            Assert.fail("no input read from test input stream");
        }

        // ***
        // Same test as above but with trusted ByteArrayInputStream
        byte[] buffer2 = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        bais = new ByteArrayInputStream(buffer2);
        byte[] b2 = new byte[100];

        try {
            numread = bais.read(b2);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        if (numread > 0) {
            Assert.assertTrue("trusted area not marked tainted", ByteArrayTaint.isTrusted(b2, 0, numread - 1));
            Assert.assertFalse("untainted area marked tainted", ByteArrayTaint.isTainted(b2, numread, 99));
        } else {
            Assert.fail("no input read from test input stream");
        }

        //        Assert.assertTrue("ByteArrayInpustream is not tainted though it was created from a tained byte array", CharArrayTaint.isTainted(bais));
        byte[] b3 = new byte[100];
        byte[] bInput = "have some more input".getBytes();
        ByteArrayTaint.taint(bInput);
        bais = new ByteArrayInputStream(bInput);

        try {
            numread = bais.read(b3, 10, b3.length - 10);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        if (numread > 0) {
            Assert.assertFalse("untainted area marked tainted", ByteArrayTaint.isTainted(b3, 0, 9));
            Assert.assertTrue("tainted area not marked tainted", ByteArrayTaint.isTainted(b3, 10, 10 + numread - 1));
            Assert.assertFalse("untainted area marked tainted", ByteArrayTaint.isTainted(b3, 10 + numread, 99));
        } else {
            Assert.fail("no input read from test input stream");
        }

        // ***
        // Same test as above but with trusted ByteArrayInputStream
        byte[] b4 = new byte[100];
        bInput = TaintUtils.trust("have some more input").getBytes();
        bais = new ByteArrayInputStream(bInput);

        try {
            numread = bais.read(b4, 10, b4.length - 10);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        if (numread > 0) {
            Assert.assertFalse("untainted area marked tainted", ByteArrayTaint.isTainted(b4, 0, 9));
            Assert.assertTrue("tainted area not marked tainted", ByteArrayTaint.isTrusted(b4, 10, 10 + numread - 1));
            Assert.assertFalse("untainted area marked tainted", ByteArrayTaint.isTainted(b4, 10 + numread, 99));
        } else {
            Assert.fail("no input read from test input stream");
        }

        // reading from byte array with mixed taint values...
        byte[] buffer5 = new byte[] { 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        ByteArrayTaint.taint(buffer5, 5, 14);
        bais = new ByteArrayInputStream(buffer5);
        byte[] b5 = new byte[100];

        try {
            numread = bais.read(b5, 10, b5.length - 10);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        if (numread > 0) {
            Assert.assertFalse("untainted area marked tainted", ByteArrayTaint.isTainted(b5, 0, 9));
            Assert.assertTrue("trusted area not marked trusted", ByteArrayTaint.isTrusted(b5, 10, 14));
            Assert.assertTrue("tainted area marked untainted", ByteArrayTaint.isTainted(b5, 15, 24));
            Assert.assertTrue("trusted area not marked trusted", ByteArrayTaint.isTrusted(b5, 25, 29));
            Assert.assertFalse("untainted area marked tainted", ByteArrayTaint.isTainted(b5, 30, b5.length - 1));
        } else {
            Assert.fail("no input read from test input stream");
        }

    }

    // Called twice:
    //   Read data from a FileInputStream that is created from trusted char data filename
    //   Read data from a FileInputStream that is created from tainted char data filename
    // In either case the data read should be tainted because the filename is an untrusted location
    private void fileInputStreamTestCommon(final boolean trustOrNot) {
        String str = TaintUtils.trust("test/pac/test/FileInputStreamTestFile");
        if (!trustOrNot) {
            TaintUtils.taint(str);
        }
        File testFile = new File(str);
        FileInputStream fis = null;
        boolean shouldTrust = FileInputStreamInstrumentation.shouldTrustContent(testFile);
        try {
            fis = new FileInputStream(testFile);
            Assert.assertTrue("FileInputStream not marked as taint source", TaintUtils.isTainted(fis) != shouldTrust);
        } catch (IOException e) {
            Assert.fail("error opening file input stream: " + e.getMessage());
        }

        byte[] b1 = new byte[10];
        int b1read = 0;
        try {
            b1read = fis.read(b1);
        } catch (IOException e) {
            Assert.fail("error while reading: " + e.getMessage());
        }

        // check for taint
        if (b1read > 0) {
            Assert.assertTrue("tainted area not marked tainted",
                              ByteArrayTaint.isTainted(b1, 0, b1read - 1) != shouldTrust);
            Assert.assertFalse("untainted area marked tainted", ByteArrayTaint.isTainted(b1, b1read, 99));
        } else {
            Assert.fail("no input read from test input stream");
        }

        byte[] b2 = new byte[20];
        int b2read = 0;
        try {
            b2read = fis.read(b2, 5, 10);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        if (b2read > 0) {
            Assert.assertFalse("untainted area marked tainted", ByteArrayTaint.isTainted(b2, 0, 4));
            Assert.assertTrue("tainted area not marked tainted", ByteArrayTaint.isTainted(b2, 5, 14) != shouldTrust);
            Assert.assertFalse("untainted area marked tainted", ByteArrayTaint.isTainted(b2, 15, 19));
        } else {
            Assert.fail("no input read from test input stream");
        }
        try {
            fis.close();
        } catch (IOException ex) {
        }
    }

    @Test
    public void fileInputStreamTest() {
        // Test FileInputStream(fileName): Test that if fileName is an untrusted location that bytes read from that
        //                                 file are untrusted - regardless if the fileName char data is (true) trusted
        //                                 or (false) tainted
        fileInputStreamTestCommon(false);
        fileInputStreamTestCommon(true);
    }

    @Test
    public void filterInputStreamTest() {

        FilterInputStream fis = new BufferedInputStream(testInput);

        // ***
        // test FilterInputStream.read(byte[]) with tainted FilterInputStream
        byte[] b1 = new byte[100];
        int b1read = 0;
        String s = "have some test input";
        TaintUtils.taint(s);
        prepareInput(s);
        try {
            b1read = fis.read(b1);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        if (b1read > 0) {
            Assert.assertTrue("tainted area not marked tainted", ByteArrayTaint.isTainted(b1, 0, b1read - 1));
            Assert.assertFalse("untainted area marked tainted", ByteArrayTaint.isTainted(b1, b1read, 99));
        } else {
            Assert.fail("no input read from test input stream");
        }

        // ***
        // test FilterInputStream.read(byte[],int,int) with tainted FilterInputStream
        byte[] b2 = new byte[100];
        int b2read = 0;
        s = "have some more input";
        TaintUtils.taint(s);
        prepareInput(s);
        try {
            b2read = fis.read(b2, 10, b2.length - 10);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        if (b2read > 0) {
            Assert.assertFalse("untainted area marked tainted", ByteArrayTaint.isTainted(b2, 0, 9));
            Assert.assertTrue("tainted area not marked tainted", ByteArrayTaint.isTainted(b2, 10, 10 + b2read - 1));
            Assert.assertFalse("untainted area marked tainted", ByteArrayTaint.isTainted(b2, 10 + b2read, 99));
        } else {
            Assert.fail("no input read from test input stream");
        }

        // ***
        // test FilterInputStream.read(byte[]) with trusted FilterInputStream
        b1 = new byte[100];
        b1read = 0;
        prepareInput(TaintUtils.trust("have some test input"));
        try {
            b1read = fis.read(b1);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        if (b1read > 0) {
            Assert.assertTrue("entire array should be trusted", ByteArrayTaint.isTrusted(b1, 0, b1.length - 1));
        } else {
            Assert.fail("no input read from test input stream");
        }

        // ***
        // test FilterInputStream.read(byte[],int,int) with trusted FilterInputStream
        b2 = new byte[100];
        b2read = 0;
        prepareInput(TaintUtils.trust("have some more input"));
        try {
            b2read = fis.read(b2, 10, b2.length - 10);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        if (b2read > 0) {
            Assert.assertFalse("out of bounds area marked tainted", ByteArrayTaint.isTainted(b2, 0, 9));
            Assert.assertTrue("tainted area not marked trusted", ByteArrayTaint.isTrusted(b2, 10, 10 + b2read - 1));
            Assert.assertFalse("out of bounds area marked tainted", ByteArrayTaint.isTainted(b2, 10 + b2read, 99));
        } else {
            Assert.fail("no input read from test input stream");
        }

    }

    @Test
    public void inputStreamTest() {

        InputStream is = testInput;

        byte[] b1 = new byte[100];
        int b1read = 0;

        String s = "have some test input";
        TaintUtils.taint(s);
        prepareInput(s);

        try {
            b1read = is.read(b1);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        if (b1read > 0) {
            Assert.assertTrue("tainted area not marked tainted", ByteArrayTaint.isTainted(b1, 0, b1read - 1));
            Assert.assertFalse("untainted area marked tainted", ByteArrayTaint.isTainted(b1, b1read, 99));
        } else {
            Assert.fail("no input read from test input stream");
        }

        byte[] b2 = new byte[100];
        int b2read = 0;

        s = "have some more input";
        TaintUtils.taint(s);
        prepareInput(s);

        try {
            b2read = is.read(b2, 10, b2.length - 10);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        if (b2read > 0) {
            Assert.assertFalse("untainted area marked tainted", ByteArrayTaint.isTainted(b2, 0, 9));
            Assert.assertTrue("tainted area not marked tainted", ByteArrayTaint.isTainted(b2, 10, 10 + b2read - 1));
            Assert.assertFalse("untainted area marked tainted", ByteArrayTaint.isTainted(b2, 10 + b2read, 99));
        } else {
            Assert.fail("no input read from test input stream");
        }
    }

    @Test
    public void multiStreamTest() {
        byte[] buffer1 = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        ByteArrayTaint.taint(buffer1, 2, 5);
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer1);
        BufferedInputStream is = new BufferedInputStream(bais);
        //        Assert.assertTrue("ByteArrayInpustream is not tainted though it was created from a tained byte array", CharArrayTaint.isTainted(bais));
        byte[] b1 = new byte[100];
        int numread = 0;

        try {
            numread = is.read(b1);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        if (numread > 0) {
            Assert.assertTrue("tainted area not marked tainted", ByteArrayTaint.hasEqualTaint(buffer1, b1, numread));
        } else {
            Assert.fail("no input read from test input stream");
        }
    }

    @Test
    public void multiReaderTest() {
        String s = TaintUtils.trust("0123456789");
        s = TaintUtils.taint(s, 2, 5);
        StringReader sr = new StringReader(s);
        BufferedReader br = new BufferedReader(sr);

        String result = null;
        try {
            result = br.readLine();
        } catch (IOException e) {
            Assert.fail("exception thrown on readLine():" + e);
        }

        Assert.assertNotNull("string read is null", result);
        Assert.assertTrue("tainted area not marked tainted", TaintUtils.hasEqualTaint(s, result));
    }

    @Test
    public void charArrayReaderTest() {
        String s = TaintUtils.trust("0123456789");
        s = TaintUtils.taint(s, 2, 5);
        CharArrayReader chArrReader = new CharArrayReader(s.toCharArray());
        BufferedReader br = new BufferedReader(chArrReader);

        String result = null;
        try {
            result = br.readLine();
        } catch (IOException e) {
            Assert.fail("exception thrown on readLine():" + e);
        }

        Assert.assertNotNull("string read is null", result);
        Assert.assertTrue("tainted area not marked tainted", TaintUtils.hasEqualTaint(s, result));
    }

    @Test
    public void readerResetTest() {
        String s = TaintUtils.trust("0123456789");
        s = TaintUtils.taint(s, 2, 5);
        CharArrayReader chArrReader = new CharArrayReader(s.toCharArray());
        BufferedReader br = new BufferedReader(chArrReader);
        char[] ch = new char[10];
        int numread = 0;

        try {
            numread = br.read(ch, 0, 4);
            br.mark(1);
            numread += br.read(ch, 4, 6);
        } catch (IOException e) {
            Assert.fail("exception thrown on read():" + e);
        }

        if (numread > 0) {
            Assert.assertTrue("tainted area not marked tainted", TaintUtils.hasEqualTaint(s, new String(ch)));
        } else {
            Assert.fail("no input read from test input stream");
        }

        try {
            br.reset();
        } catch (IOException e) {
            Assert.fail("uanble to reset reader: " + e);
        }

        ch = new char[10];
        try {
            numread = br.read(ch);
        } catch (IOException e) {
            Assert.fail("exception thrown on read():" + e);
        }

        if (numread > 0) {
            Assert.assertFalse("out of bounds area marked tainted", CharArrayTaint.isTainted(ch, 6, ch.length - 1));
            Assert.assertTrue("tainted area not marked tainted", CharArrayTaint.isTainted(ch, 0, 2));
            Assert.assertTrue("trusted area not marked trusted", CharArrayTaint.isTrusted(ch, 3, 5));
        } else {
            Assert.fail("no input read from test input stream");
        }
    }

    @Test
    public void inputStreamResetTest() {
        byte[] buffer1 = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        ByteArrayTaint.taint(buffer1, 2, 5);
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer1);
        byte[] b = new byte[10];
        int numread = 0;

        bais.mark(0);

        try {
            numread = bais.read(b);
        } catch (IOException e) {
            Assert.fail("exception thrown on read():" + e);
        }

        if (numread > 0) {
            Assert.assertTrue("tainted area not marked tainted", ByteArrayTaint.hasEqualTaint(buffer1, b, numread));
        } else {
            Assert.fail("no input read from test input stream");
        }

        bais.reset();

        b = new byte[10];
        try {
            numread = bais.read(b);
        } catch (IOException e) {
            Assert.fail("exception thrown on read():" + e);
        }

        if (numread > 0) {
            Assert.assertTrue("tainted area not marked tainted", ByteArrayTaint.hasEqualTaint(buffer1, b, numread));
        } else {
            Assert.fail("no input read from test input stream");
        }
    }

    @Test
    public void streamTokenizerTest() {
        String str = TaintUtils.trust("this is a \"quoted string\" followed by a number 55");
        str = TaintUtils.taint(str, 10, 24);
        Reader r = new BufferedReader(new StringReader(str));
        StreamTokenizer tokenizer = new StreamTokenizer(r);

        try {
            while (tokenizer.nextToken() != StreamTokenizer.TT_EOF) {
                if (tokenizer.sval == null)
                    continue;
                if (tokenizer.ttype == '\"')
                    Assert.assertTrue("quoted token should be entirely tainted",
                                      TaintUtils.isAllTainted(tokenizer.sval, 0, tokenizer.sval.length() - 1));
                else
                    Assert.assertTrue("non-quoted token should be entirely trusted",
                                      TaintUtils.isTrusted(tokenizer.sval));
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        str = TaintUtils.trust("now for an entirely trusted string.");
        r = new BufferedReader(new StringReader(str));
        tokenizer = new StreamTokenizer(r);

        try {
            while (tokenizer.nextToken() != StreamTokenizer.TT_EOF) {
                if (tokenizer.sval == null)
                    continue;
                Assert.assertTrue("token should be entirely trusted", TaintUtils.isTrusted(tokenizer.sval));
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void secureReadTest(int inputSize) {
        try {
            final int BUFFER_SIZE = 2048;
            byte[] buff = new byte[BUFFER_SIZE];
            byte inputByte = (byte) TaintUtils.taint('a');
            InputStream sensitiveFile = new FileInputStream("/etc/passwd");
            InputStream otherFile = new RepeatInputStream(inputByte, inputSize);
            sensitiveFile.read(buff);
            sensitiveFile.close();
            otherFile.read(buff);
            otherFile.close();

            for (int i = 0; i < inputSize; i++) {
                Assert.assertEquals("Expected the first " + inputSize + " byte(s) to come from secondary input",
                                    inputByte, buff[i]);
            }
            for (int i = inputSize; i < BUFFER_SIZE; i++) {
                Assert.assertEquals("Expected the remaining bytes after what was "
                        + "read by the secondary input to be zeroed out", 0, buff[i]);
            }
        } catch (IOException ioe) {
            Assert.fail("Unable to read from /etc/passwd due to: " + ioe);
        }
    }

    @Test
    public void secureReadTest() {
        secureReadTest(0);
        secureReadTest(1);
        secureReadTest(100);
    }

    @Test
    public void complicatedStreamTest() {
        String str = TaintUtils.trust("contains some taint");
        str = TaintUtils.taint(str, 9, 12);
        byte[] bytes = str.getBytes();
        InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(bytes));
        BufferedReader br = new BufferedReader(isr);

        try {
            String newStr = br.readLine();
            //String descr = CharArrayTaint.taint_descr(str) + 
            //  ", " + CharArrayTaint.taint_descr(newStr);
            // System.out.println ("taints: " + descr);
            Assert.assertTrue("original string and new string contain different taint: ",
                              TaintUtils.hasEqualTaint(str, newStr, TaintValues.TRUST_MASK));
        } catch (IOException e) {
            Assert.fail("unable to read from reader: " + e);
        }
    }

    @Test
    public void complicatedReaderTest() {
        String str = TaintUtils.trust("contains some taint");
        str = TaintUtils.taint(str, 9, 12);
        LineNumberReader lnr = new LineNumberReader(new StringReader(str));
        BufferedReader br = new BufferedReader(lnr);

        try {
            String newStr = br.readLine();
            Assert.assertTrue("original string and new string contain different taint",
                              TaintUtils.hasEqualTaint(str, newStr));
        } catch (IOException e) {
            Assert.fail("unable to read from reader: " + e);
        }
    }

    @Test
    public void pipeTest() {
        try {
            byte[] orig = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
            ByteArrayTaint.taint(orig, 4, 7);
            byte[] bytes = new byte[orig.length];
            PipedOutputStream output = new PipedOutputStream();
            PipedInputStream input = new PipedInputStream(output);
            output.write(orig);
            input.read(bytes);
            input.close();
            Assert.assertTrue("original bytes should match piped bytes taint",
                              ByteArrayTaint.hasEqualTaint(orig, bytes, TaintValues.TRUST_MASK, bytes.length));
        } catch (IOException e) {
            Assert.fail("Exception: " + e);
        }
    }

    @Test
    public void sequenceInputStreamTest() {
        byte[] buffer1 = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        ByteArrayTaint.taint(buffer1, 2, 6);
        byte[] buffer2 = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        ByteArrayTaint.taint(buffer2, 3, 5);
        SequenceInputStream in = new SequenceInputStream(new ByteArrayInputStream(buffer1),
                new ByteArrayInputStream(buffer2));
        byte[] bytes = new byte[10];

        try {
            in.read(bytes);
            Assert.assertTrue(ByteArrayTaint.hasEqualTaint(buffer1, bytes, buffer1.length));
            in.read(bytes);
            Assert.assertTrue(ByteArrayTaint.hasEqualTaint(buffer2, bytes, buffer2.length));
        } catch (IOException e) {
            Assert.fail("Exception: " + e);
        }
    }

    @Test
    public void pushbackStreamTest() {
        byte[] buffer1 = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        ByteArrayTaint.taint(buffer1, 2, 6);
        PushbackInputStream in = new PushbackInputStream(new ByteArrayInputStream(buffer1), 20);
        byte[] bytes = new byte[5];

        try {
            in.read(bytes, 0, 5);
            Assert.assertTrue("region should be trusted", ByteArrayTaint.isTrusted(bytes, 0, 1));
            Assert.assertTrue("region should be all tainted", ByteArrayTaint.isAllTainted(bytes, 2, 4));
            in.unread(bytes, 3, 2);
            Assert.assertTrue("region should be trusted", ByteArrayTaint.isTrusted(bytes, 0, 1));
            Assert.assertTrue("region should be all tainted", ByteArrayTaint.isAllTainted(bytes, 2, 4));
            in.read(bytes, 0, 5);
            Assert.assertTrue("region should be all tainted", ByteArrayTaint.isAllTainted(bytes, 0, 3));
            Assert.assertTrue("region should be trusted", ByteArrayTaint.isTrusted(bytes, 4, 4));
        } catch (IOException e) {
            Assert.fail("Exception: " + e);
        }
    }

    @Test
    public void skipTest1() {
        String str = TaintUtils.trust("0123456789012345678901234567890123456789");
        str = TaintUtils.taint(str, 10, 29);
        StringReader in = new StringReader(str);
        char[] cbuf = new char[10];
        try {
            in.read(cbuf);
            Assert.assertTrue("data read not trusted", CharArrayTaint.isTrusted(cbuf));
            in.skip(10);
            in.read(cbuf);
            Assert.assertTrue("data read not entirely tainted", CharArrayTaint.isAllTainted(cbuf, 0, cbuf.length - 1));
            in.read(cbuf);
            Assert.assertTrue("data read not trusted", CharArrayTaint.isTrusted(cbuf));
        } catch (IOException e) {
            Assert.fail("Exception: " + e);
        }

        CharArrayReader in2 = new CharArrayReader(str.toCharArray());
        try {
            in2.read(cbuf);
            Assert.assertTrue("data read not trusted", CharArrayTaint.isTrusted(cbuf));
            in2.skip(10);
            in2.read(cbuf);
            Assert.assertTrue("data read not entirely tainted", CharArrayTaint.isAllTainted(cbuf, 0, cbuf.length - 1));
            in2.read(cbuf);
            Assert.assertTrue("data read not trusted", CharArrayTaint.isTrusted(cbuf));
        } catch (IOException e) {
            Assert.fail("Exception: " + e);
        }

        byte[] bytes = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1,
                2, 3, 4, 5, 6, 7, 8, 9 };
        ByteArrayTaint.taint(bytes, 10, 29);
        ByteArrayInputStream in3 = new ByteArrayInputStream(bytes);
        byte[] buf = new byte[10];
        try {
            in3.read(buf);
            Assert.assertFalse("data read not trusted", ByteArrayTaint.isTainted(buf));
            in3.skip(10);
            in3.read(buf);
            Assert.assertTrue("data read not entirely tainted", ByteArrayTaint.isAllTainted(buf, 0, buf.length - 1));
            in3.read(buf);
            Assert.assertFalse("data read not trusted", ByteArrayTaint.isTainted(buf));
        } catch (IOException e) {
            Assert.fail("Exception: " + e);
        }
    }

    @Test
    public void skipTest2() {
        String str = TaintUtils.trust("0123456789012345678901234567890123456789");
        str = TaintUtils.taint(str, 10, 29);
        BufferedReader in = new BufferedReader(new StringReader(str));
        char[] cbuf = new char[10];
        try {
            in.read(cbuf);
            Assert.assertTrue("data read not trusted", CharArrayTaint.isTrusted(cbuf));
            in.skip(10);
            in.read(cbuf);
            Assert.assertTrue("data read not entirely tainted", CharArrayTaint.isAllTainted(cbuf, 0, cbuf.length - 1));
            in.read(cbuf);
            Assert.assertTrue("data read not trusted", CharArrayTaint.isTrusted(cbuf));
        } catch (IOException e) {
            Assert.fail("Exception: " + e);
        }

        in = new BufferedReader(new CharArrayReader(str.toCharArray()));
        try {
            in.read(cbuf);
            Assert.assertTrue("data read not trusted", CharArrayTaint.isTrusted(cbuf));
            in.skip(10);
            in.read(cbuf);
            Assert.assertTrue("data read not entirely tainted", CharArrayTaint.isAllTainted(cbuf, 0, cbuf.length - 1));
            in.read(cbuf);
            Assert.assertTrue("data read not trusted", CharArrayTaint.isTrusted(cbuf));
        } catch (IOException e) {
            Assert.fail("Exception: " + e);
        }

        byte[] bytes = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1,
                2, 3, 4, 5, 6, 7, 8, 9 };
        ByteArrayTaint.taint(bytes, 10, 29);
        BufferedInputStream in2 = new BufferedInputStream(new ByteArrayInputStream(bytes));
        byte[] buf = new byte[10];
        try {
            in2.read(buf);
            Assert.assertFalse("data read not trusted", ByteArrayTaint.isTainted(buf));
            in2.skip(10);
            in2.read(buf);
            Assert.assertTrue("data read not entirely", ByteArrayTaint.isAllTainted(buf, 0, buf.length - 1));
            in2.read(buf);
            Assert.assertFalse("data read not trusted", ByteArrayTaint.isTainted(buf));
        } catch (IOException e) {
            Assert.fail("Exception: " + e);
        }
    }

    @Test
    public void inputStreamExtensionTest() {
        class FooInputStream extends BufferedInputStream {
            public FooInputStream(InputStream is) {
                super(is);
            }
        }

        FooInputStream fis = new FooInputStream(testInput);

        byte[] b1 = new byte[100];
        int b1read = 0;

        String str = "have some test input";
        TaintUtils.taint(str);
        prepareInput(str);

        try {
            b1read = fis.read(b1);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        if (b1read > 0) {
            Assert.assertTrue("tainted area not marked tainted", ByteArrayTaint.isTainted(b1, 0, b1read - 1));
            Assert.assertFalse("untainted area marked tainted", ByteArrayTaint.isTainted(b1, b1read, 99));
        } else {
            Assert.fail("no input read from test input stream");
        }

        byte[] b2 = new byte[100];
        int b2read = 0;

        str = "have some more input";
        TaintUtils.taint(str);
        prepareInput(str);

        try {
            b2read = fis.read(b2, 10, b2.length - 10);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        if (b2read > 0) {
            Assert.assertFalse("untainted area marked tainted", ByteArrayTaint.isTainted(b2, 0, 9));
            Assert.assertTrue("tainted area not marked tainted", ByteArrayTaint.isTainted(b2, 10, 10 + b2read - 1));
            Assert.assertFalse("untainted area marked tainted", ByteArrayTaint.isTainted(b2, 10 + b2read, 99));
        } else {
            Assert.fail("no input read from test input stream");
        }

        try {
            fis.close();
        } catch (IOException e) {
        }
    }

    @Test
    public void customInputStreamTest() throws IOException {
        class PartiallyTaintedInputStream extends InputStream {
            private byte[] tainted, trusted;
            private int inputLength, pos;

            public PartiallyTaintedInputStream(byte[] tainted, byte[] trusted) {
                this.tainted = tainted;
                this.trusted = trusted;
                this.inputLength = tainted.length + trusted.length;
                this.pos = -1;
            }

            @Override
            public int read() throws IOException {
                pos = (pos + 1) % inputLength;
                if (pos >= trusted.length)
                    return tainted[pos - trusted.length];
                return trusted[pos];
            }
        }

        String tainted = "tainted";
        String trusted = TaintUtils.trust("trusted");
        TaintUtils.taint(tainted);
        InputStream in = null;

        try {
            in = new PartiallyTaintedInputStream(tainted.getBytes(), trusted.getBytes());
            Random rand = new Random();
            byte[] buf = new byte[rand.nextInt(100)];
            int read = in.read(buf);
            Assert.assertEquals("we should have read into byte buffer completely", read, buf.length);
            String s = new String(buf);
            int i = 0;
            StringRegionIterator iter = new StringRegionIterator(s);
            while (iter.hasNext()) {
                StringRegion sr = iter.next();
                int expected;
                if (i % 2 == 0)
                    expected = TaintValues.TRUSTED;
                else
                    expected = TaintValues.TAINTED;

                Assert.assertEquals("Unexpected trust value at " + i + "th interval", expected,
                                    sr.getTaint() & NONPRIM_MASK);
                if (sr.getEnd() == read - 1)
                    break;
                Assert.assertEquals("Unexpected trust length at " + i + "th interval", 7,
                                    sr.getEnd() - sr.getStart() + 1);
                i++;
            }
        } finally {
            if (in != null)
                in.close();
        }
    }

    /**
     * Input stream that repeats a specified byte a specified number of times.
     * 
     * @author jeikenberry
     */
    private class RepeatInputStream extends InputStream {
        private int numOfTimes;
        private byte b;

        public RepeatInputStream(byte b, int numOfTimes) {
            this.b = b;
            this.numOfTimes = numOfTimes;
        }

        @Override
        public int read() throws IOException {
            if (--numOfTimes < 0)
                return -1;
            return b;
        }
    }
}
