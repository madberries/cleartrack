package pac.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
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
import sun.nio.cs.StreamDecoder;

public class ReaderInstrumentationTest {
    private Reader testReader;
    private Writer writerToTestReader;

    @Before
    public void setup() {
        testReader = new PipedReader();
        try {
            writerToTestReader = new PipedWriter((PipedReader) testReader);
        } catch (IOException e) {
            Assert.fail("unable to initialize writer to test reader: " + e.getMessage());
        }
    }

    @After
    public void teardown() {

    }

    private void prepareInput(String s) {
        try {
            writerToTestReader.write(s);
        } catch (IOException e) {
            Assert.fail("unable to write to test reader: " + e.getMessage());
        }
    }

    @Test
    public void bufferedReaderTest() {
        try {
            BufferedReader br = new BufferedReader(testReader);

            char[] c1 = new char[100];
            int c1read = 0;

            String s = "here have some input";
            TaintUtils.taint(s);
            prepareInput(s);

            // Note: reader(char[]) is not defined in BufferedReader. It is defined in Reader.
            //       Will call uninstrumented Reader.read(char[], int, int)
            //       Then BufferedReader is fills its buffer by calling the Instrumented Reader(char[], int, int)
            //       So BuffereredReaderInstrumentation.read
            c1read = br.read(c1); // IOException

            if (c1read > 0) {
                Assert.assertTrue("tainted area not marked tainted", CharArrayTaint.isTainted(c1, 0, c1read - 1));
                Assert.assertFalse("untainted area marked tainted", CharArrayTaint.isTainted(c1, c1read, 99));
            } else {
                Assert.fail("no input read from test reader");
            }

            char[] c2 = new char[100];
            int c2read = 0;

            s = "more input, you know you love it";
            TaintUtils.taint(s);
            prepareInput(s);

            c2read = br.read(c2, 10, c2.length - 10); // IOExcetpion

            if (c2read > 0) {
                Assert.assertFalse("untainted area marked tainted", CharArrayTaint.isTainted(c2, 0, 9));
                Assert.assertTrue("tainted area not marked tainted", CharArrayTaint.isTainted(c2, 10, 10 + c2read - 1));
                Assert.assertFalse("untainted area marked tainted", CharArrayTaint.isTainted(c2, 10 + c2read, 99));
            } else {
                Assert.fail("no input read from test reader");
            }

            char[] c3 = new char[100];
            int c3read = 0;
            CharBuffer charBuf = CharBuffer.wrap(c3); // c3 is the source of chars

            s = "I have not yet begun to input!";
            TaintUtils.taint(s);
            prepareInput(s);

            // call to Reader.read(char[], int, int) is instrumented
            c3read = br.read(charBuf); // read chars and writes them into charBuf
                                       // IOException
            if (c3read > 0) {
                Assert.assertTrue("BufferedReader is tainted. BufferedReader(CharBuffer) did not put tainted data in CharBuffer",
                                  CharArrayTaint.isTainted(charBuf.array(), 0, c3read - 1));
                Assert.assertTrue("BufferedReader is tainted. BufferedReader(CharBuffer) did not put tainted data in backing array",
                                  CharArrayTaint.isTainted(c3, 0, c3read - 1));

                Assert.assertFalse("BufferedReader is tainted. BufferedReader(CharBuffer) tainted beyond where data was read in CharBuffer",
                                   CharArrayTaint.isTainted(charBuf.array(), c3read, 99));
                Assert.assertFalse("BufferedReader is tainted. BufferedReader(CharBuffer) tainted beyond where data was read in backing array",
                                   CharArrayTaint.isTainted(c3, c3read, 99));
            } else {
                Assert.fail("no input read from test reader");
            }

            // Test with br as trusted
            char[] c4 = new char[100];
            CharBuffer cb_4 = CharBuffer.wrap(c4, 3, 40); // Note The 3 means the reads will put chars in starting at
                                                          //      position 3 in both CharBuffer and array
            br = new BufferedReader(testReader);

            prepareInput(TaintUtils.trust("God I'm good, son. Hear me, Verne?"));

            // will fill cb_4 and c4 starting at position or index 3
            c3read = br.read(cb_4); // IOException

            if (c3read > 0) {
                Assert.assertTrue("BufferedReader.read(charBuffer) Untouched area of backing array is not marked trusted",
                                  CharArrayTaint.isTrusted(c4, 0, 3 - 1));

                Assert.assertTrue("BufferedReader is trusted. BufferedReader.read(charBuffer) returned data in buffer that is not trusted",
                                  CharArrayTaint.isTrusted(c4, 3, 3 + c3read - 1));
                Assert.assertTrue("BufferedReader is trusted. BufferedReader.read(charBuffer) returned data in array that is not trusted",
                                  CharArrayTaint.isTrusted(cb_4.array(), 3, 3 + c3read - 1));

                Assert.assertFalse("untainted area marked tainted", CharArrayTaint.isTainted(c4, c3read, 99));
            } else {
                Assert.fail("no input read from test reader");
            }

            // ***
            // With a trusted BufferedReader test that chars read from Buffered.readLine will be trusted
            prepareInput(TaintUtils.trust("I'm positive\nthat was you.\nI know that was you"));
            String line = br.readLine(); // IOException
            Assert.assertTrue("BufferedReader.readLine() returned null", line != null);
            Assert.assertTrue("BufferedReader is trusted. BufferedReader.readLine() faile to return trusted data",
                              TaintUtils.isTrusted(line));

        } catch (IOException ex) {
            Assert.fail(ex.getMessage());
        }
    }

    @Test
    public void fileReaderTest() {
        File f = new File(TaintUtils.trust("test/pac/test/FileInputStreamTestFile"));
        boolean shouldTrust = FileInputStreamInstrumentation.shouldTrustContent(f);
        FileReader fr = null;

        try {
            fr = new FileReader(f);
        } catch (IOException e) {
            Assert.fail("error opening test file: " + e.getMessage());
        }

        char[] c1 = new char[10];
        int c1read = 0;

        try {
            c1read = fr.read(c1);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        if (c1read > 0) {
            Assert.assertTrue("tainted area not marked tainted",
                              CharArrayTaint.isTainted(c1, 0, c1read - 1) != shouldTrust);
            Assert.assertFalse("untainted area marked tainted", CharArrayTaint.isTainted(c1, c1read, 99));
        } else {
            Assert.fail("no input read from test reader");
        }

        char[] c2 = new char[20];
        int c2read = 0;

        try {
            c2read = fr.read(c2, 5, 10);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        if (c2read > 0) {
            Assert.assertFalse("untainted area marked tainted", CharArrayTaint.isTainted(c2, 0, 4));
            Assert.assertTrue("tainted area not marked tainted", CharArrayTaint.isTainted(c2, 5, 14) != shouldTrust);
            Assert.assertFalse("untainted area marked tainted", CharArrayTaint.isTainted(c2, 15, 19));
        } else {
            Assert.fail("no input read from test reader");
        }

        char[] c3 = new char[100];
        int c3read = 0;
        CharBuffer cb = CharBuffer.wrap(c3);

        prepareInput("I have not yet begun to input!");

        try {
            c3read = fr.read(cb);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        if (c3read > 0) {
            Assert.assertTrue("tainted char array area not marked tainted",
                              CharArrayTaint.isTainted(c3, 0, c3read - 1) != shouldTrust);
            Assert.assertFalse("untainted area int char array marked tainted",
                               CharArrayTaint.isTainted(c3, c3read, 99));
        } else {
            Assert.fail("no input read from test reader");
        }

    }

    @Test
    // Test each of the four InputStreamReader constructors with the same tests
    public void inputStreamReaderTest() {
        for (int i = 0; i < 4; i++) {
            inputStreamReaderTest_common(i);
        }
    }

    // Call one of the four InputStreamReader constructors
    private InputStreamReader call_InputStreamReader_Constructor(int whichConstructor, final PipedInputStream is) {
        InputStreamReader isr = null;
        try {
            final Charset charset = Charset.forName("UTF-8"); // "IOS-8859-1");
            CharsetDecoder decoder = charset.newDecoder();

            switch (whichConstructor) {
            case 0:
                isr = new InputStreamReader(is);
                break;
            case 1:
                isr = new InputStreamReader(is, charset);
                break;
            case 2:
                isr = new InputStreamReader(is, decoder);
                break;
            case 3:
                isr = new InputStreamReader(is, "UTF-8"); // UnsupportedEncodingException
                break;
            default:
                isr = new InputStreamReader(is);
                break;
            }

        } catch (UnsupportedEncodingException ex) {
            Assert.fail(ex.getMessage());
        }

        return isr;
    }

    private void inputStreamReaderTest_common(final int whichConstructor) {
        try {
            PipedInputStream is = new PipedInputStream();
            PipedOutputStream os = null;

            os = new PipedOutputStream(is); // IOException
            // taint of returnd isr will match that of "is"
            InputStreamReader isr = call_InputStreamReader_Constructor(whichConstructor, is);

            char[] c1 = new char[100];
            int c1read = 0;

            /*
             * FIXME Changed this from output.write(byte[]) to output.write(byte[],int,int)
             * There are polymorphic reasons that make static analysis here...
             */

            byte[] bytes = "here have some input".getBytes();
            ByteArrayTaint.taint(bytes);
            os.write(bytes, 0, bytes.length); // IOException

            c1read = isr.read(c1); // IOException
            if (c1read > 0) {
                Assert.assertTrue("tainted area not marked tainted", CharArrayTaint.isTainted(c1, 0, c1read - 1));
                Assert.assertFalse("untainted area marked tainted", CharArrayTaint.isTainted(c1, c1read, 99));
            } else {
                Assert.fail("no input read from test reader");
            }

            char[] c2 = new char[100];
            int c2read = 0;

            bytes = "more input, you know you love it".getBytes();
            ByteArrayTaint.taint(bytes);
            os.write(bytes, 0, bytes.length); // IOException

            c2read = isr.read(c2, 10, c2.length - 10); // IOException
            if (c2read > 0) {
                Assert.assertFalse("untainted area marked tainted", CharArrayTaint.isTainted(c2, 0, 9));
                Assert.assertTrue("tainted area not marked tainted", CharArrayTaint.isTainted(c2, 10, 10 + c2read - 1));
                Assert.assertFalse("untainted area marked tainted", CharArrayTaint.isTainted(c2, 10 + c2read, 99));
            } else {
                Assert.fail("no input read from test reader");
            }

            char[] c3 = new char[100];
            int c3read = 0;
            CharBuffer cb = CharBuffer.wrap(c3);

            bytes = "I have not yet begun to input!".getBytes();
            ByteArrayTaint.taint(bytes);
            os.write(bytes, 0, bytes.length);

            c3read = isr.read(cb); // IOException

            if (c3read > 0) {
                Assert.assertTrue("tainted char array area not marked tainted",
                                  CharArrayTaint.isTainted(c3, 0, c3read - 1));
                Assert.assertFalse("untainted char array area marked tainted",
                                   CharArrayTaint.isTainted(c3, c3read, 99));
            } else {
                Assert.fail("no input read from test reader");
            }

            // *****
            // Run the identical above tests but this time with a TRUSTED PipedInputStream, which
            // should propagate to create a TRUSTED InputStreamReader and ultimately to the data
            // that is read being TRUSTED
            int count;
            is.close(); // IOException
            isr.close(); // IOException
            os.close();

            is = new PipedInputStream();
            os = new PipedOutputStream(is); // IOException

            // ***
            char[] c_1 = new char[100];

            isr = call_InputStreamReader_Constructor(whichConstructor, is);

            bytes = TaintUtils.trust("here have some input").getBytes();
            os.write(bytes, 0, bytes.length); // IOException
            count = isr.read(c_1); // IOException
            if (count > 0) {
                Assert.assertTrue("tainted area not marked tainted", CharArrayTaint.isTrusted(c_1, 0, count - 1));
                Assert.assertFalse("untainted area marked tainted", CharArrayTaint.isTainted(c_1, count, 99));
            } else {
                Assert.fail("no input read from test reader");
            }

            // ***
            char[] c_2 = new char[100];
            bytes = TaintUtils.trust("more input, you know you love it").getBytes();
            os.write(bytes, 0, bytes.length); // IOException
            count = isr.read(c_2, 10, c_2.length - 10); // IOException
            if (c2read > 0) {
                Assert.assertFalse("untainted area marked tainted", CharArrayTaint.isTainted(c_2, 0, 9));
                Assert.assertTrue("tainted area not marked tainted", CharArrayTaint.isTrusted(c_2, 10, 10 + count - 1));
                Assert.assertFalse("untainted area marked tainted", CharArrayTaint.isTainted(c_2, 10 + count, 99));
            } else {
                Assert.fail("no input read from test reader");
            }

            // ***
            char[] c_3 = new char[100];
            CharBuffer cb_3 = CharBuffer.wrap(c_3);
            bytes = TaintUtils.trust("I have not yet begun to input!").getBytes();
            os.write(bytes, 0, bytes.length); // IOException
            count = isr.read(cb_3); // IOException
            if (count > 0) {
                Assert.assertTrue("tainted area in array not marked tainted",
                                  CharArrayTaint.isTrusted(c_3, 0, count - 1));
                Assert.assertFalse("untainted area in array marked tainted", CharArrayTaint.isTainted(c_3, count, 99));
            } else {
                Assert.fail("no input read from test reader");
            }

            os.close();

        } catch (IOException ex) {
            Assert.fail(ex.getMessage());
        }
    }

    @Test
    public void readableTest() {
        Readable r = testReader;
        char[] charray = new char[20];
        CharBuffer cb = CharBuffer.wrap(charray);

        String s = "Here have some test input.";
        TaintUtils.taint(s);
        prepareInput(s);

        int charsRead = 0;
        try {
            charsRead = r.read(cb);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        if (charsRead > 0) {
            Assert.assertTrue("tainted char array area not marked tainted",
                              CharArrayTaint.isTainted(charray, 0, charsRead - 1));
            Assert.assertFalse("untainted char array area marked tainted",
                               CharArrayTaint.isTainted(charray, charsRead, 19));
        } else {
            Assert.fail("no input read from test readable");
        }
    }

    @Test
    public void readerTest() {
        char[] c1 = new char[100];
        int c1read = 0;

        String s = "here have some input";
        TaintUtils.taint(s);
        prepareInput(s);

        try {
            c1read = testReader.read(c1);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        if (c1read > 0) {
            Assert.assertTrue("tainted area not marked tainted", CharArrayTaint.isTainted(c1, 0, c1read - 1));
            Assert.assertFalse("untainted area marked tainted", CharArrayTaint.isTainted(c1, c1read, 99));
        } else {
            Assert.fail("no input read from test reader");
        }

        // Test the above again with trusted testReader
        s = TaintUtils.trust("here have some input");
        prepareInput(s);

        char[] c1_B = new char[100];
        try {
            c1read = testReader.read(c1_B);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        if (c1read > 0) {
            Assert.assertTrue("trusted area not marked tainted", CharArrayTaint.isTrusted(c1_B, 0, c1read - 1));
            Assert.assertFalse("untainted area marked tainted", CharArrayTaint.isTainted(c1_B, c1read, 99));
        } else {
            Assert.fail("no input read from test reader");
        }

        char[] c2 = new char[100];
        int c2read = 0;

        s = "more input, you know you love it";
        TaintUtils.taint(s);
        prepareInput(s);

        try {
            c2read = testReader.read(c2, 10, c2.length - 10);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        if (c2read > 0) {
            Assert.assertFalse("untainted area marked tainted", CharArrayTaint.isTainted(c2, 0, 9));
            Assert.assertTrue("tainted area not marked tainted", CharArrayTaint.isTainted(c2, 10, 10 + c2read - 1));
            Assert.assertFalse("untainted area marked tainted", CharArrayTaint.isTainted(c2, 10 + c2read, 99));
        } else {
            Assert.fail("no input read from test reader");
        }

        char[] c3 = new char[100];
        int c3read = 0;
        CharBuffer cb = CharBuffer.wrap(c3);

        s = "I have not yet begun to input!";
        TaintUtils.taint(s);
        prepareInput(s);

        try {
            c3read = testReader.read(cb);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        if (c3read > 0) {
            Assert.assertTrue("tainted char array area not marked tainted",
                              CharArrayTaint.isTainted(c3, 0, c3read - 1));
            Assert.assertFalse("untainted char array area marked tainted", CharArrayTaint.isTainted(c3, c3read, 99));
        } else {
            Assert.fail("no input read from test reader");
        }
    }

    @Test
    public void streamDecoderTest() {
        try {
            PipedInputStream is = new PipedInputStream();
            PipedOutputStream os = null;
            os = new PipedOutputStream(is); // IOException
            StreamDecoder sd = StreamDecoder.forInputStreamReader(is, new Object(), Charset.defaultCharset());

            // ***
            // test StreamDecoder.read(char[]) with tainted StreamDecoder
            char[] c1 = new char[100];
            int c1read = 0;
            /*
             * FIXME Changed this from output.write(byte[]) to output.write(byte[],int,int)
             * There are polymorphic reasons that make static analysis here...
             */
            byte[] bytes = "here have some input".getBytes();
            ByteArrayTaint.taint(bytes);
            os.write(bytes, 0, bytes.length); // IOException
            c1read = sd.read(c1); // IOException
            if (c1read > 0) {
                Assert.assertTrue("tainted area not marked tainted", CharArrayTaint.isTainted(c1, 0, c1read - 1));
                Assert.assertFalse("untainted area marked tainted", CharArrayTaint.isTainted(c1, c1read, 99));
            } else {
                Assert.fail("no input read from test reader");
            }

            // ***
            // test StreamDecoder.read(char[],int,int) with tainted StreamDecoder
            char[] c2 = new char[100];
            int c2read = 0;
            bytes = "more input, you know you love it".getBytes();
            ByteArrayTaint.taint(bytes);
            os.write(bytes, 0, bytes.length); // IOException
            c2read = sd.read(c2, 10, c2.length - 10); // IOException
            if (c2read > 0) {
                Assert.assertFalse("untainted area marked tainted", CharArrayTaint.isTainted(c2, 0, 9));
                Assert.assertTrue("tainted area not marked tainted", CharArrayTaint.isTainted(c2, 10, 10 + c2read - 1));
                Assert.assertFalse("untainted area marked tainted", CharArrayTaint.isTainted(c2, 10 + c2read, 99));
            } else {
                Assert.fail("no input read from test reader");
            }

            // ***
            // test StreamDecoder.read(CharBuffer) with tainted StreamDecoder
            char[] c3 = new char[100];
            int c3read = 0;
            CharBuffer cb = CharBuffer.wrap(c3);
            bytes = "I have not yet begun to input!".getBytes();
            ByteArrayTaint.taint(bytes);
            os.write(bytes, 0, bytes.length); // IOException

            c3read = sd.read(cb); // IOException

            if (c3read > 0) {
                Assert.assertTrue("tainted char array area not marked tainted",
                                  CharArrayTaint.isTainted(c3, 0, c3read - 1));
                Assert.assertFalse("untainted char array area marked tainted",
                                   CharArrayTaint.isTainted(c3, c3read, 99));
            } else {
                Assert.fail("no input read from test reader");
            }

            // ***
            // test StreamDecoder.read(char[]) with trusted StreamDecoder
            c1 = new char[100];
            c1read = 0;
            bytes = TaintUtils.trust("here have some input").getBytes();
            os.write(bytes, 0, bytes.length); // IOException
            c1read = sd.read(c1); // IOException
            if (c1read > 0) {
                Assert.assertTrue("tainted area not marked tainted", CharArrayTaint.isTrusted(c1, 0, c1read - 1));
                Assert.assertFalse("out of bounds area marked tainted", CharArrayTaint.isTainted(c1, c1read, 99));
                ;
            } else {
                Assert.fail("no input read from test reader");
            }

            // ***
            // test StreamDecoder.read(char[],int,int) with trusted StreamDecoder
            c2 = new char[100];
            c2read = 0;
            bytes = TaintUtils.trust("more input, you know you love it").getBytes();
            os.write(bytes, 0, bytes.length); // IOException
            c2read = sd.read(c2, 10, c2.length - 10); // IOException
            if (c2read > 0) {
                Assert.assertFalse("out of bonds area marked tainted", CharArrayTaint.isTainted(c2, 0, 9));
                Assert.assertTrue("tainted area not marked tainted", CharArrayTaint.isTrusted(c2, 10, 10 + c2read - 1));
                Assert.assertFalse("out of bounds area marked tainted", CharArrayTaint.isTainted(c2, 10 + c2read, 99));
            } else {
                Assert.fail("no input read from test reader");
            }

            // ***
            // test StreamDecoder.read(CharBuffer) with trusted StreamDecoder
            c3 = new char[100];
            c3read = 0;
            cb = CharBuffer.wrap(c3);
            bytes = TaintUtils.trust("I have not yet begun to input!").getBytes();
            os.write(bytes, 0, bytes.length); // IOException
            c3read = sd.read(cb); // IOException
            if (c3read > 0) {
                Assert.assertTrue("tainted char array area not marked tainted",
                                  CharArrayTaint.isTrusted(c3, 0, c3read - 1));
                Assert.assertFalse("untainted char array area marked tainted",
                                   CharArrayTaint.isTainted(c3, c3read, 99));
            } else {
                Assert.fail("no input read from test reader");
            }

            os.close();
        } catch (IOException ex) {
            Assert.fail(ex.getMessage());
        }
    }

    @Test
    public void stringReaderTest() {
        String str = TaintUtils
                .trust("This is a big old input string for use as test input to the\nStringReader instrumentation unit test in the ReaderInstrumentationTest class.");
        str = TaintUtils.taint(str, 10, 20);
        StringReader sr = new StringReader(str);
        //        Assert.assertTrue("StringReader(String) was created with a trusted String. StringReader is not trusted", TaintValues.isTainted(sr));

        // ***
        // Test StringReader.read(char[]) with an untrusted StringReader
        char[] c1 = new char[10];
        int c1read = 0;

        // first pass should be entirely trusted...
        try {
            c1read = sr.read(c1);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        if (c1read > 0) {
            Assert.assertTrue("trusted area not marked trusted", CharArrayTaint.isTrusted(c1, 0, c1read - 1));
            Assert.assertFalse("untainted area marked tainted", CharArrayTaint.isTainted(c1, c1read, 99));
        } else {
            Assert.fail("no input read from test reader");
        }

        // second pass should be entirely tainted...
        try {
            c1read = sr.read(c1);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        if (c1read > 0) {
            Assert.assertFalse("untainted area marked tainted", CharArrayTaint.isTainted(c1, c1read, 99));
            Assert.assertTrue("StringReader(char[]): untrusted StringReader failed to taint the characers it read",
                              CharArrayTaint.isTainted(c1, 0, c1read - 1));
        } else {
            Assert.fail("no input read from test reader");
        }

        // ***
        // Test StringReader.read(char[], int, int)  with an untrusted StringReader
        char[] c2 = new char[20];
        int c2read = 0;

        try {
            c2read = sr.read(c2, 5, 10);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        if (c2read > 0) {
            Assert.assertFalse("untainted area marked tainted", CharArrayTaint.isTainted(c2, 0, 4));
            Assert.assertFalse("unknown area not marked unknown", CharArrayTaint.isTrusted(c2, 5, 14));
            Assert.assertFalse("untainted area marked tainted", CharArrayTaint.isTainted(c2, 15, 19));
            Assert.assertTrue("StringReader.read(char[]): untrusted StringReader failed to taint the characters is read",
                              CharArrayTaint.isTainted(c2, 5, 5 + 10 - 1));
        } else {
            Assert.fail("no input read from test reader");
        }

        // ***
        // Test StringReader.read(CharBuffer) with an untrusted StringReader
        char[] c3 = new char[100];
        int c3read = 0;
        CharBuffer cb = CharBuffer.wrap(c3);

        prepareInput(TaintUtils.trust("I have not yet begun to input!"));

        try {
            c3read = sr.read(cb);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        if (c3read > 0) {
            Assert.assertTrue("unknown char array area not marked as trusted",
                              CharArrayTaint.isTrusted(c3, c3read, c3.length - 1));
            Assert.assertTrue("StringReader.read(CharBuffer): untrusted StringReader failed to taint the chars it wrote to char array",
                              CharArrayTaint.isTrusted(c3, 0, c3read - 1));
        } else {
            Assert.fail("no input read from test reader");
        }

        // ***
        // ***
        // Perform the above test with trusted StringReader
        str = TaintUtils
                .trust("This is a big old input string for use as test input to the\nStringReader instrumentation unit test in the ReaderInstrumentationTest class.");
        sr = new StringReader(str);

        // ***
        // Test StringReader.read(char[]) with a trusted StringReader
        char[] c4 = new char[10];
        int numRead = 0;

        try {
            numRead = sr.read(c4);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        if (numRead > 0) {
            Assert.assertTrue("StringReader.read(char[]): trusted StringReader wrote data that is not trusted",
                              CharArrayTaint.isTrusted(c4, 0, numRead - 1));
        } else {
            Assert.fail("no input read from test reader");
        }

        // ***
        // Test StringReader.read(char[], int, int)  with a trusted StringReader
        char[] c5 = new char[20];

        try {
            numRead = sr.read(c5, 5, 10);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        if (numRead > 0) {
            Assert.assertFalse("StringReader.read(char[], int, int): trusted StringReader wrote data that is not trusted",
                               CharArrayTaint.isTainted(c5, 0, 4));
            Assert.assertTrue("StringReader.read(char[], int, int): trusted StringReader wrote data that is not trusted",
                              CharArrayTaint.isTrusted(c5, 5, 14));
            Assert.assertFalse("StringReader.read(char[], int, int): trusted StringReader wront data that is not trusted",
                               CharArrayTaint.isTainted(c5, 15, 19));
        } else {
            Assert.fail("no input read from test reader");
        }

        // ***
        // Test StringReader.read(CharBuffer) with a trusted StringReader
        char[] c6 = new char[100];
        CharBuffer cb_6 = CharBuffer.wrap(c6);

        prepareInput(TaintUtils.trust("I have not yet begun to input!"));

        try {
            numRead = sr.read(cb_6);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }

        if (numRead > 0) {
            Assert.assertTrue("trusted char array area not marked trusted",
                              CharArrayTaint.isTrusted(c6, 0, numRead - 1));
            Assert.assertFalse("untainted char array area marked tainted", CharArrayTaint.isTainted(c6, numRead, 99));
            Assert.assertTrue("StringReader.read(CharBuffer): trusted StringReader failed to trust the chars it wrote to char array",
                              CharArrayTaint.isTrusted(c6, 0, numRead - 1));
        } else {
            Assert.fail("no input read from test reader");
        }

    }

    @Test
    public void lineNumberReaderTest() {
        try {
            // ***
            // Test LineNumberReader.read(char[], int, int);
            // Test the data read from a trusted LineNumberReader
            char[] array_1 = new char[50];
            prepareInput(TaintUtils.trust("Data for LineNumberReaderTest to read"));
            LineNumberReader reader = new LineNumberReader(testReader);
            reader.read(array_1, 5, 10); // IOException
            Assert.assertFalse("LineNumberReader(char[], int, int) Data in this from 0 thru 4 should not be tainted",
                               CharArrayTaint.isTainted(array_1, 0, 4));
            Assert.assertTrue("A trusted LineNumberReader read data. The data is not trusted",
                              CharArrayTaint.isTrusted(array_1, 5, 9));

            // ***
            // Test LineNumberReader.read(char[], int, int);
            // Test the data read from a tainted LineNumberReader
            char[] array_2 = new char[50];
            String s = "More data for a tainted LineNumberReaderTest to read";
            TaintUtils.taint(s);
            prepareInput(s);
            reader = new LineNumberReader(testReader);
            reader.read(array_2, 5, 10); // IOException
            Assert.assertFalse("LineNumberReader(char[], int, int) Data in this from 0 thru 4 should not be tainted",
                               CharArrayTaint.isTainted(array_2, 0, 4));
            Assert.assertTrue("A tainted LineNumberReader read data. The data is not tained",
                              CharArrayTaint.isTainted(array_2, 5, 9));
            Assert.assertFalse("A tainted LineNumberReader read data. The data is trusted",
                               CharArrayTaint.isTrusted(array_2, 5, 9));

        } catch (IOException ex) {
            Assert.fail("LineNumberReader.read(char[], int, int) throw IOException " + ex.getMessage());
        }

        // Test that for an untrusted LineNumberReader, the data read from LineNumberReader.readLine will be tainted
        File file = new File(TaintUtils.trust("./cleartrack.log"));
        boolean shouldTrust = FileInputStreamInstrumentation.shouldTrustContent(file);
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(file); // FileNotFoundException

            BufferedReader reader = new BufferedReader(fileReader);
            LineNumberReader lr = new LineNumberReader(reader, 100);

            for (int i = 0; i < 5; i++) {
                String line = lr.readLine(); // IOException
                if (line != null && line.length() > 0) {
                    Assert.assertTrue("BufferedReader should return untrusted lines of text",
                                      TaintUtils.isTainted(line) != shouldTrust);
                }
            }

            lr.close();
        } catch (FileNotFoundException ex) {
            Assert.fail("FileReader(File) threw unexpected exception.");
        } catch (IOException ex) {
            Assert.fail("FileReader(File) threw unexpected IOException.");
        } finally {
            try {
                fileReader.close();
            } catch (Exception ex) {
            }
        }

        // ***
        // Test that for a trusted LineNumberReader, the data read from LineNumberReader.readLine will be trusted
        // Same test both LineNumberReader constructors with trusted file location
        String path = TaintUtils.trust("/usr/bin/file");
        file = new File(path);

        try {
            fileReader = new FileReader(file); // FileNotFoundException
            BufferedReader reader = new BufferedReader(fileReader);
            LineNumberReader lr = new LineNumberReader(reader);

            for (int i = 0; i < 5; i++) {
                String line = lr.readLine(); // IOException
                if (line != null && line.length() > 0) {
                    Assert.assertTrue("A line of test read from trusted LineNumberReader failed to be trusted",
                                      TaintUtils.isTrusted(line));
                }
            }

            lr.close();

        } catch (FileNotFoundException ex) {
            Assert.fail("FileReader(File) threw unexpected exception.");
        } catch (IOException ex) {
            Assert.fail("FileReader(File) threw unexpected IOException.");
        } finally {
            try {
                fileReader.close();
            } catch (Exception ex) {
            }
        }

        // Test propagation of trust from trusted file location thru File, BuffererReader, thru LineNumberReader
        // Test that for a trusted LineNumberReader, the data read from LineNumberReader.readLine will be trusted
        try {
            fileReader = new FileReader(file); // FileNotFoundException
            BufferedReader reader = new BufferedReader(fileReader);
            LineNumberReader lr = new LineNumberReader(reader, 100);

            for (int i = 0; i < 5; i++) {
                String line = lr.readLine(); // IOException
                if (line != null && line.length() > 0) {
                    Assert.assertTrue("A line of text read from trusted LineNumberReader failed to be trusted",
                                      TaintUtils.isTrusted(line));
                }
            }

            lr.close();

        } catch (FileNotFoundException ex) {
            Assert.fail("FileReader(File) threw unexpected exception.");
        } catch (IOException ex) {
            Assert.fail("FileReader(File) threw unexpected IOException.");
        } finally {
            try {
                fileReader.close();
            } catch (Exception ex) {
            }
        }

    }

    @Test
    public void customReaderTest() throws IOException {
        class PartiallyTaintedReader extends Reader {
            private char[] tainted, trusted;
            private int inputLength, pos;

            public PartiallyTaintedReader(char[] tainted, char[] trusted) {
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

            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                for (int i = off; i < len; i++) {
                    cbuf[i] = (char) read();
                }
                return len;
            }

            @Override
            public void close() throws IOException {

            }

        }

        String tainted = "tainted";
        String trusted = TaintUtils.trust("trusted");
        TaintUtils.taint(tainted);
        Reader in = null;

        try {
            in = new PartiallyTaintedReader(tainted.toCharArray(), trusted.toCharArray());
            Random rand = new Random();
            char[] buf = new char[rand.nextInt(100)];
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
                                    sr.getTaint() & InputStreamTest.NONPRIM_MASK);
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

    /*
     * A test to ensure file names containing union are allowed to read.
     */
    // TODO Enable this test after diversification code can derandomize file names.
    //  @Test
    //  public void testFileReadWithUnion() throws FileNotFoundException
    //  {
    //      String fileName = "test/pac/test/sqlinjection/sql-inject-data/sql-union.tsv";
    //      FileReader fr = new FileReader(fileName);
    //  }
}
