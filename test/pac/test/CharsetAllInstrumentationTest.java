package pac.test;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Test;

import pac.util.TaintUtils;
import pac.util.TaintValues;
import pac.wrap.ByteArrayTaint;
import pac.wrap.CharArrayTaint;

/**
 * Test cases for Charset, CharsetEncoder, and CharsetDecoder.
 * 
 * @author ppiselli
 *
 */
public class CharsetAllInstrumentationTest {

    @Test
    public void charsetTest() {
        Charset cs = Charset.defaultCharset();
        String s = TaintUtils.trust("0123456789");
        s = TaintUtils.taint(s, 6, 8);
        CharBuffer cb = CharBuffer.wrap(s);
        ByteBuffer bb = cs.encode(cb);

        // ensure that the entire byte buffer is tainted
        Assert.assertFalse("trusted region not marked trusted", ByteArrayTaint.isTainted(bb.array(), 0, 5));
        Assert.assertTrue("tainted region not marked tainted", ByteArrayTaint.isTainted(bb.array(), 6, 8));
        Assert.assertFalse("trusted region not marked trusted", ByteArrayTaint.isTainted(bb.array(), 9, 9));

        s = TaintUtils.trust("0123456789");
        s = TaintUtils.taint(s, 2, 5);
        ByteBuffer bb2 = cs.encode(s);

        // ensure that the entire byte buffer is tainted
        Assert.assertFalse("trusted region not marked trusted", ByteArrayTaint.isTainted(bb2.array(), 0, 1));
        Assert.assertTrue("tainted region not marked tainted", ByteArrayTaint.isTainted(bb2.array(), 2, 5));
        Assert.assertFalse("trusted region not marked trusted", ByteArrayTaint.isTainted(bb2.array(), 6, 9));

        ByteArrayTaint.trust(bb.array(), 0, 9);
        ByteArrayTaint.taint(bb.array(), 4, 8);
        CharBuffer cb2 = cs.decode(bb);

        // ensure that the entire char buffer is tainted
        Assert.assertFalse("trusted region not marked trusted", CharArrayTaint.isTainted(cb2.array(), 0, 3));
        Assert.assertTrue("tainted region not marked tainted", CharArrayTaint.isTainted(cb2.array(), 4, 8));
        Assert.assertFalse("trusted region not marked trusted", CharArrayTaint.isTainted(cb2.array(), 9, 9));
    }

    @Test
    public void charsetEncoderTest() {
        CharsetEncoder cse = Charset.defaultCharset().newEncoder();
        ByteBuffer bb = null;
        String s = TaintUtils.trust("0123456789");
        s = TaintUtils.taint(s, 6, 8);
        CharBuffer cb = CharBuffer.wrap(s);

        try {
            bb = cse.encode(cb);
        } catch (Exception e) {
            Assert.fail("unexpected exception while encoding: " + e.getMessage());
        }

        // ensure that the entire output is tainted
        Assert.assertFalse("trusted region not marked trusted", ByteArrayTaint.isTainted(bb.array(), 0, 5));
        Assert.assertTrue("tainted region not marked tainted", ByteArrayTaint.isTainted(bb.array(), 6, 8));
        Assert.assertFalse("trusted region not marked trusted", ByteArrayTaint.isTainted(bb.array(), 9, 9));

        cb.rewind();
        cse = Charset.defaultCharset().newEncoder();
        ByteBuffer bb2 = ByteBuffer.allocate(30);
        cse.encode(cb, bb2, true);

        // ensure that the entire output is tainted
        Assert.assertFalse("trusted region not marked trusted", ByteArrayTaint.isTainted(bb2.array(), 0, 5));
        Assert.assertTrue("tainted region not marked tainted", ByteArrayTaint.isTainted(bb2.array(), 6, 8));
        Assert.assertFalse("trusted region not marked trusted", ByteArrayTaint.isTainted(bb2.array(), 9, 9));
    }

    @Test
    public void charsetDecoderTest() {
        CharsetDecoder csd = Charset.defaultCharset().newDecoder();
        String s = TaintUtils.trust("0123456789");
        s = TaintUtils.taint(s, 6, 8);
        ByteBuffer bb = ByteBuffer.wrap(s.getBytes());
        CharBuffer cb = null;

        try {
            cb = csd.decode(bb);
        } catch (Exception e) {
            Assert.fail("unexpected exception while decoding: " + e.getMessage());
        }

        // ensure that the entire output is tainted
        Assert.assertFalse("trusted region not marked trusted", CharArrayTaint.isTainted(cb.array(), 0, 5));
        Assert.assertTrue("tainted region not marked tainted", CharArrayTaint.isTainted(cb.array(), 6, 8));
        Assert.assertFalse("trusted region not marked trusted", CharArrayTaint.isTainted(cb.array(), 9, 9));

        bb.rewind();
        csd = Charset.defaultCharset().newDecoder();
        CharBuffer cb2 = CharBuffer.allocate(20);
        csd.decode(bb, cb2, true);

        // ensure that the entire output is tainted
        Assert.assertFalse("trusted region not marked trusted", CharArrayTaint.isTainted(cb2.array(), 0, 5));
        Assert.assertTrue("tainted region not marked tainted", CharArrayTaint.isTainted(cb2.array(), 6, 8));
        Assert.assertFalse("trusted region not marked trusted", CharArrayTaint.isTainted(cb2.array(), 9, 9));
    }

    @Test
    public void encodingTest() {
        String str = TaintUtils.trust("0123xxx456789");
        str = TaintUtils.taint(str, 4, 6);

        int accurate = 0, taintOff = 0, unencodable = 0;
        for (Entry<String, Charset> entry : Charset.availableCharsets().entrySet()) {
            switch (testEncoding(entry.getKey(), str)) {
            case ACCURATE_TAINT:
                System.out.println("accurate taint with charset " + entry.getValue());
                accurate++;
                break;
            case TAINT_OFF:
                System.out.println("conservative taint with charset " + entry.getValue());
                taintOff++;
                break;
            case UNENCODABLE:
                System.out.println("unencodable charset " + entry.getValue());
                unencodable++;
                break;
            }
        }
        System.out.println("***********************************************");
        System.out.println("default charset: " + Charset.defaultCharset());
        System.out.println("total charsets preserving accurate taint: " + accurate);
        System.out.println("total charsets with conservative taint: " + taintOff);
        System.out.println("total charsets that are unencodable: " + unencodable);
    }

    private static final int ACCURATE_TAINT = 0, TAINT_OFF = 1, UNENCODABLE = 2;

    public int testEncoding(String encoding, String str) {
        try {
            Charset charset = Charset.forName(encoding);
            if (!charset.canEncode()) { // Some charsets are not able to encode
                return UNENCODABLE;
            }

            // encode the specified string...
            CharsetEncoder encoder = charset.newEncoder();
            encoder.onMalformedInput(CodingErrorAction.REPORT);
            ByteBuffer bb = encoder.encode(CharBuffer.wrap(str));
            byte[] b = bb.array();

            // decode the encoded string...
            CharsetDecoder decoder = charset.newDecoder();
            CharBuffer ascii = decoder.decode(bb);

            // Ensure that:
            //   a) the decoded string equals the original
            //   b) the taint of the decoded and original strings are equals
            String newStr = ascii.hasArray() ? new String(ascii.array(), 0, str.length()) : ascii.toString();
            if (!str.equals(newStr)) {
                Assert.fail("Decoded string \"" + newStr + "\" does not match original string \"" + str + "\"");
            }
            if (TaintUtils.hasEqualTaint(str, newStr, TaintValues.TRUST_MASK))
                return ACCURATE_TAINT;
            if (!TaintUtils.isTrusted(newStr))
                return TAINT_OFF;

            System.err.println(TaintUtils.createTaintDisplayLines(str));
            System.err.println(ByteArrayTaint.createTaintDisplayLines(b));
            System.err.println(TaintUtils.createTaintDisplayLines(newStr));
            Assert.fail("We somehow ended up with a trusted byte array or new string"
                    + " on that byte array when encoding/decoding with " + encoding);
        } catch (CharacterCodingException e) {
            return UNENCODABLE;
        } catch (UnsupportedOperationException e) {
            return UNENCODABLE;
        }
        return ACCURATE_TAINT;
    }

}
