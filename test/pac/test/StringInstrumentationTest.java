package pac.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import pac.util.TaintUtils;
import pac.util.TaintValues;
import pac.wrap.ByteArrayTaint;
import pac.wrap.CharArrayTaint;
import pac.wrap.IntArrayTaint;

public class StringInstrumentationTest {

    // This class is merely intended for testing user defined maps
    private class CustomMap extends HashMap<String, String> {
        /**
         * 
         */
        private static final long serialVersionUID = -3525982393236798936L;

        @Override
        public boolean containsKey(Object key) {
            return super.containsKey(key);
        }
    }
    
    @AfterClass
    public static void cleanup() {
        new File("string.obj").delete();
    }

    @Test
    public void comparisonTest() {
        // comparing trusted string to a tainted string with equal strings...
        String str1 = TaintUtils.trust("some string");
        String str2 = new String("some string".toCharArray());
        TaintUtils.taint(str2);
        Assert.assertTrue(str1 + " != " + str2, str2.equals(str1));
        Assert.assertTrue("tainted string was compared against an equal trusted string, but did not become trusted",
                          TaintUtils.isTrusted(str2));
        TaintUtils.taint(str2);
        Assert.assertTrue(str1 + " != " + str2, str1.equals(str2));
        Assert.assertTrue("tainted string was compared against an equal trusted string, but did not become trusted",
                          TaintUtils.isTrusted(str2));

        // comparing trusted string to a tainted string with unequal strings...
        str1 = TaintUtils.trust("some string");
        str2 = "another string";
        TaintUtils.taint(str2);
        Assert.assertFalse(str1 + " = " + str2, str2.equals(str1));
        Assert.assertFalse("tainted string was compared against an unequal trusted string, but became trusted",
                           TaintUtils.isTrusted(str2));
        TaintUtils.taint(str2);
        Assert.assertFalse(str1 + " = " + str2, str1.equals(str2));
        Assert.assertFalse("tainted string was compared against an unequal trusted string, but became trusted",
                           TaintUtils.isTrusted(str2));

        // comparing tainted string to a tainted string with equal strings...
        str1 = "some string";
        str2 = new String("some string".toCharArray());
        TaintUtils.taint(str1);
        TaintUtils.taint(str2);
        Assert.assertTrue(str1 + " != " + str2, str2.equals(str1));
        Assert.assertFalse("tainted string was compared against an equal trusted string, but did not become trusted",
                           TaintUtils.isTrusted(str2));
        TaintUtils.taint(str2);
        Assert.assertTrue(str1 + " != " + str2, str1.equals(str2));
        Assert.assertFalse("tainted string was compared against an equal trusted string, but did not become trusted",
                           TaintUtils.isTrusted(str2));

        // comparing trusted string to a tainted string with equal strings...
        str1 = TaintUtils.trust("some string");
        str2 = new String("some string".toCharArray());
        TaintUtils.taint(str2);
        Assert.assertTrue(str1 + " != " + str2, str2.equalsIgnoreCase(str1));
        Assert.assertTrue("tainted string was compared against an equal trusted string, but did not become trusted",
                          TaintUtils.isTrusted(str2));
        TaintUtils.taint(str2);
        Assert.assertTrue(str1 + " != " + str2, str1.equalsIgnoreCase(str2));
        Assert.assertTrue("tainted string was compared against an equal trusted string, but did not become trusted",
                          TaintUtils.isTrusted(str2));

        // comparing trusted string to a tainted string with unequal strings...
        str1 = TaintUtils.trust("some string");
        str2 = "another string";
        TaintUtils.taint(str2);
        Assert.assertFalse(str1 + " = " + str2, str2.equalsIgnoreCase(str1));
        Assert.assertFalse("tainted string was compared against an unequal trusted string, but became trusted",
                           TaintUtils.isTrusted(str2));
        TaintUtils.taint(str2);
        Assert.assertFalse(str1 + " = " + str2, str1.equalsIgnoreCase(str2));
        Assert.assertFalse("tainted string was compared against an unequal trusted string, but became trusted",
                           TaintUtils.isTrusted(str2));

        // comparing tainted string to a tainted string with equal strings...
        str1 = "some string";
        str2 = new String("some string".toCharArray());
        TaintUtils.taint(str1);
        TaintUtils.taint(str2);
        Assert.assertTrue(str1 + " != " + str2, str2.equalsIgnoreCase(str1));
        Assert.assertFalse("tainted string was compared against an equal trusted string, but did not become trusted",
                           TaintUtils.isTrusted(str2));
        TaintUtils.taint(str2);
        Assert.assertTrue(str1 + " != " + str2, str1.equalsIgnoreCase(str2));
        Assert.assertFalse("tainted string was compared against an equal trusted string, but did not become trusted",
                           TaintUtils.isTrusted(str2));

        // Map.containsKey() = true, with tainted key
        String key = "key";
        TaintUtils.trust(key);
        String value = "value";
        TaintUtils.trust(value);
        Map<String, String> map = new HashMap<String, String>();
        map.put(key, value);
        String eqKey = new String(new char[] { 'k', 'e', 'y' });
        TaintUtils.taint(eqKey);
        Assert.assertTrue("value from map should initially be tainted", TaintUtils.isTainted(eqKey));
        Assert.assertTrue("map should contain key = \"" + key + "\"", map.containsKey(eqKey));
        Assert.assertFalse("value from map should now be trusted", TaintUtils.isTainted(eqKey));

        // Map.containsKey() = false, with tainted key
        key = "key1";
        TaintUtils.trust(key);
        value = "value";
        TaintUtils.trust(value);
        map = new HashMap<String, String>();
        map.put(key, value);
        eqKey = new String(new char[] { 'k', 'e', 'y' });
        TaintUtils.taint(eqKey);
        Assert.assertTrue("value from map should initially be tainted", TaintUtils.isTainted(eqKey));
        Assert.assertFalse("map should not contain key = \"" + key + "\"", map.containsKey(eqKey));
        Assert.assertTrue("value from map should still be tainted", TaintUtils.isTainted(eqKey));

        // CustomMap.containsKey() = true, with tainted key
        key = "key";
        TaintUtils.trust(key);
        value = "value";
        TaintUtils.trust(value);
        CustomMap map2 = new CustomMap();
        map2.put(key, value);
        eqKey = new String(new char[] { 'k', 'e', 'y' });
        TaintUtils.taint(eqKey);
        Assert.assertTrue("value from map should initially be tainted", TaintUtils.isTainted(eqKey));
        Assert.assertTrue("map should contain key = \"" + key + "\"", map2.containsKey(eqKey));
        Assert.assertFalse("value from map should now be trusted", TaintUtils.isTainted(eqKey));

        // CustomMap.containsKey() = false, with tainted key
        key = "key1";
        TaintUtils.trust(key);
        value = "value";
        TaintUtils.trust(value);
        map2 = new CustomMap();
        map2.put(key, value);
        eqKey = new String(new char[] { 'k', 'e', 'y' });
        TaintUtils.taint(eqKey);
        Assert.assertTrue("value from map should initially be tainted", TaintUtils.isTainted(eqKey));
        Assert.assertFalse("map should not contain key = \"" + key + "\"", map2.containsKey(eqKey));
        Assert.assertTrue("value from map should still be tainted", TaintUtils.isTainted(eqKey));
    }

    @Test
    public void constructorTest() {
        String s;

        char[] ca = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j' };
        CharArrayTaint.taint(ca, 3, 6);
        s = new String(ca);
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s, 0, 2));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s, 3, 6));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s, 7, 9));

        String s2 = TaintUtils.trust("0123456789");
        s2 = TaintUtils.taint(s2, 2, 5);
        s = new String(s2);
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s, 0, 1));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s, 2, 5));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s, 6, 9));

        StringBuilder sbd = new StringBuilder(TaintUtils.trust("0123456789"));
        TaintUtils.taint(sbd, 5, 8);
        s = new String(sbd);
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s, 0, 4));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s, 5, 8));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s, 9, 9));

        StringBuffer sbf = new StringBuffer(TaintUtils.trust("0123456789"));
        TaintUtils.taint(sbf, 1, 4);
        s = new String(sbf);
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s, 0, 0));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s, 1, 4));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s, 5, 9));

        char[] ca2 = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q',
                'r', 's', 't' };
        CharArrayTaint.taint(ca2, 7, 11);
        s = new String(ca2, 5, 10); // taint transforms to 2,6
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s, 0, 1));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s, 2, 6));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s, 7, 9));

        int[] codePoints = new int[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p',
                'q', 'r', 's', 't' };
        IntArrayTaint.taint(codePoints, 10, 12);
        s = new String(codePoints, 7, 10); // taint transforms to 3,5
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s, 0, 2));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s, 3, 5));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s, 6, 9));
    }

    @Test
    public void concatTest() {
        String s1 = TaintUtils.trust("abcdefghij");
        String s2 = TaintUtils.trust("0123456789");
        s1 = TaintUtils.taint(s1, 3, 7);
        s2 = TaintUtils.taint(s2, 2, 6);
        String s3 = s1.concat(s2);
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s3, 0, 2));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s3, 3, 7));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s3, 8, 11));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s3, 12, 16));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s3, 17, 19));
    }

    @Test
    public void getCharsTest() {
        String s = TaintUtils.trust("01234567890123456789");
        char[] ca = new char[20];
        s = TaintUtils.taint(s, 8, 13);
        s.getChars(5, 15, ca, 3);
        Assert.assertFalse("untainted region contains taint marker", CharArrayTaint.isTainted(ca, 0, 5));
        Assert.assertTrue("tainted region contains no taint marker", CharArrayTaint.isTainted(ca, 6, 11));
        Assert.assertFalse("untainted region contains taint marker", CharArrayTaint.isTainted(ca, 12, 19));
    }

    @Test
    public void replaceTest() {
        // individual characters
        String s1 = TaintUtils.trust("AABBABBAAA");
        TaintUtils.taint(s1);

        // verify that replace removes taint
        String s2 = s1.replace('B', 'C');
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s2, 0, s2.length() - 1));

        // regex replace: replace, replaceFirst, replaceAll

        s1 = TaintUtils.trust("AABBABBAAA");
        String rep = TaintUtils.trust("CCC");
        rep = TaintUtils.taint(rep, 1, 1);
        String s3 = s1.replace("BB", rep); // string constant CCC is untrusted
        Assert.assertEquals("replace did not work", "AACCCACCCAAA", s3);
        // TODO is this right???
        Assert.assertTrue("trusted region not trusted", TaintUtils.isTrusted(s3, 0, 2));
        Assert.assertTrue("tainted region not tainted", TaintUtils.isTainted(s3, 3, 3));
        Assert.assertTrue("trusted region not trusted", TaintUtils.isTrusted(s3, 4, 6));
        Assert.assertTrue("tainted region not tainted", TaintUtils.isTainted(s3, 7, 7));
        Assert.assertTrue("trusted region not trusted", TaintUtils.isTrusted(s3, 8, 11));

    }

    /** This test relies heavily on the regex instrumentation, rather than the string instrumentation. */
    @Test
    public void splitTest() {
        String testString = TaintUtils.trust("one banana two banana three banana four");

        // 0         1         2         3
        // 012345678901234567890123456789012345678
        // one banana two banana three banana four
        testString = TaintUtils.taint(testString, 1, 1); // taint the second letter of each number word
        testString = TaintUtils.taint(testString, 12, 12);
        testString = TaintUtils.taint(testString, 23, 23);
        testString = TaintUtils.taint(testString, 36, 36);

        String[] output = testString.split("\\W*banana\\W*");
        Assert.assertEquals("split output not correct", "one", output[0]);
        Assert.assertEquals("split output not correct", "two", output[1]);
        Assert.assertEquals("split output not correct", "three", output[2]);
        Assert.assertEquals("split output not correct", "four", output[3]);

        // check taint status
        Assert.assertTrue(TaintUtils.isTrusted(output[0], 0, 0));
        Assert.assertTrue(TaintUtils.isTainted(output[0], 1, 1));
        Assert.assertTrue(TaintUtils.isTrusted(output[1], 0, 0));
        Assert.assertTrue(TaintUtils.isTainted(output[1], 1, 1));
        Assert.assertTrue(TaintUtils.isTrusted(output[2], 0, 0));
        Assert.assertTrue(TaintUtils.isTainted(output[2], 1, 1));
        Assert.assertTrue(TaintUtils.isTrusted(output[3], 0, 0));
        Assert.assertTrue(TaintUtils.isTainted(output[3], 1, 1));

    }

    @Test
    public void subSequenceTest() {
        String s1 = TaintUtils.trust("0123456789");
        s1 = TaintUtils.taint(s1, 3, 5);
        s1 = TaintUtils.taint(s1, 7, 7);

        String cs = (String) s1.subSequence(2, 9);
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(cs, 0, 0));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(cs, 1, 3));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(cs, 4, 4));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(cs, 5, 5));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(cs, 6, 6));
    }

    @Test
    public void substringTest() {
        String s1 = TaintUtils.trust("0123456789");
        s1 = TaintUtils.taint(s1, 3, 5);
        s1 = TaintUtils.taint(s1, 7, 7);
        // 0123456789
        // ???xxx?x??

        String s2 = s1.substring(2, 9);
        // 2345678
        // ?xxx?x?
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s2, 0, 0));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s2, 1, 3));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s2, 4, 4));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s2, 5, 5));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s2, 6, 6));

        String s3 = s1.substring(4);
        // 456789
        // xx?x??
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s3, 0, 1));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s3, 2, 2));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s3, 3, 3));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s3, 4, 5));
    }

    @Test
    public void toTest() {
        String s = TaintUtils.trust("AbCdEfGhIj");
        s = TaintUtils.taint(s, 2, 4);
        s = TaintUtils.taint(s, 6, 8);

        // toCharArray
        char[] ca = s.toCharArray();
        Assert.assertFalse("untainted region contains taint marker", CharArrayTaint.isTainted(ca, 0, 1));
        Assert.assertTrue("tainted region contains no taint marker", CharArrayTaint.isTainted(ca, 2, 4));
        Assert.assertFalse("untainted region contains taint marker", CharArrayTaint.isTainted(ca, 5, 5));
        Assert.assertTrue("tainted region contains no taint marker", CharArrayTaint.isTainted(ca, 6, 8));
        Assert.assertFalse("untainted region contains taint marker", CharArrayTaint.isTainted(ca, 9, 9));

        // toLower * 2
        String s1 = s.toLowerCase();
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s1, 0, 1));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s1, 2, 4));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s1, 5, 5));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s1, 6, 8));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s1, 9, 9));

        s1 = s.toLowerCase(Locale.US);
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s1, 0, 1));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s1, 2, 4));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s1, 5, 5));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s1, 6, 8));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s1, 9, 9));

        // toUpper * 2
        s1 = s.toUpperCase();
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s1, 0, 1));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s1, 2, 4));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s1, 5, 5));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s1, 6, 8));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s1, 9, 9));

        s1 = s.toUpperCase(Locale.US);
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s1, 0, 1));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s1, 2, 4));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s1, 5, 5));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s1, 6, 8));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s1, 9, 9));
    }

    @Test
    public void copyValueOfTest() {
        char[] ca = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j' };
        CharArrayTaint.taint(ca, 2, 5);
        CharArrayTaint.taint(ca, 7, 8);

        // copy value of
        String s1 = String.copyValueOf(ca);
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s1, 0, 1));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s1, 2, 5));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s1, 6, 6));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s1, 7, 8));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s1, 9, 9));

        String s2 = String.copyValueOf(ca, 3, 6);
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s2, 0, 2));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s2, 3, 3));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s2, 4, 5));
    }

    @Test
    public void formatTest() {

        // NUMERIC TESTING 

        String format1 = TaintUtils.trust("0123456789%d0123456789");
        format1 = TaintUtils.taint(format1, 5, 16);

        // taint should look like
        // OOOOOXXXXXXXXXXXXOOOOO taint
        // 0123456789012345678901 indices
        // 0123456789%d0123456789 values

        String s1 = String.format(format1, 12345);
        // output should look like
        // OOOOOXXXXXOOOOOXXXXXOOOOO taint
        // 0123456789012345678901234 indices
        // 0123456789123450123456789 values

        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s1, 0, 4));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s1, 5, 9));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s1, 10, 14));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s1, 15, 19));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s1, 20, 24));

        // STRING TESTING

        String format2 = TaintUtils.trust("0123456789%s0123456789");
        format2 = TaintUtils.taint(format2, 5, 16);
        // taint should look like
        // OOOOOXXXXXXXXXXXXOOOOO taint
        // 0123456789012345678901 indices
        // 0123456789%d0123456789 values

        String taint2 = TaintUtils.trust("0123456789");
        taint2 = TaintUtils.taint(taint2, 3, 6);

        String s2 = String.format(format2, taint2);
        // output should look like
        // OOOOOXXXXXOOOXXXXOOOXXXXXOOOOO taint
        // 012345678901234567890123456789 indices
        // 012345678901234567890123456789 values
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s2, 0, 4));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s2, 5, 9));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s2, 10, 12));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s2, 13, 16));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s2, 17, 19));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s2, 20, 24));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s2, 25, 29));
    }

    @Test
    public void valueOfTest() {
        char[] ca = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j' };
        CharArrayTaint.taint(ca, 2, 5);
        CharArrayTaint.taint(ca, 7, 8);

        // copy value of
        String s1 = String.valueOf(ca);
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s1, 0, 1));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s1, 2, 5));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s1, 6, 6));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s1, 7, 8));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s1, 9, 9));

        String s2 = String.valueOf(ca, 3, 6);
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s2, 0, 2));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s2, 3, 3));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s2, 4, 5));
    }

    @Test
    public void trimTest() {
        String s1 = TaintUtils.trust("   abcd   ");
        s1 = TaintUtils.taint(s1, 2, 4);
        s1 = TaintUtils.taint(s1, 6, 6);
        s1 = TaintUtils.taint(s1, 8, 8);

        String s2 = s1.trim();

        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s2, 0, 1));
        Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s2, 2, 2));
        Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s2, 3, 3));
    }

    @Test
    public void internTest() {
        String s1 = TaintUtils.trust(new String("abcdefg"));
        String s2 = TaintUtils.trust(new String("abcdefg"));
        // "same" is ==
        String s3 = s1.intern(); // tainted intern
        String s4 = s2.intern(); // trusted intern
        s2 = TaintUtils.taint(s2, 2, 3);
        Assert.assertNotSame(s1, s2);
        Assert.assertSame(s3, s4); // secret instrumentation of == makes different strings look the same
        Assert.assertFalse("strings should not have the same taint", TaintUtils.hasEqualTaint(s3, s4)); // but metadata is different!

        // let's make a new string with matching metadata and ensure it matches the previous
        String s5 = TaintUtils.trust("abcdefg");
        s5 = TaintUtils.taint(s5, 2, 3);
        String s6 = s5.intern();
        Assert.assertNotSame(s5, s2);
        Assert.assertSame(s3, s6);
        Assert.assertSame(s4, s6);
        Assert.assertFalse("strings should not have the same taint", TaintUtils.hasEqualTaint(s3, s6));
        Assert.assertTrue("strings should have the same taint", TaintUtils.hasEqualTaint(s4, s6));

        // and finally make another string that has unique metadata and make sure it matches noone
        String s7 = TaintUtils.trust("abcdefg");
        s7 = TaintUtils.taint(s7, 1, 3);
        String s8 = s7.intern();
        Assert.assertSame(s8, s3); // all interned look alike
        Assert.assertSame(s8, s4);
        Assert.assertFalse("strings should not have the same taint", TaintUtils.hasEqualTaint(s3, s8));
        Assert.assertFalse("strings should not have the same taint", TaintUtils.hasEqualTaint(s4, s8));

        // now we break it by sending the strings to a structure in the JDK that we have not instrumented that does equality testing
        /*
        IdentityHashMap ihm = new IdentityHashMap<String,String>();
        ihm.put(s3, "foo");
        Assert.assertNotNull(ihm.get(s4)); // if s3 really looks == to s4 this will return "foo"
        */
    }

    @Test
    public void nullByteTest() throws IOException {
        String str = "abcde";
        FileInputStream in = new FileInputStream("/dev/zero");
        char nb = (char) in.read();
        in.close();
        char c = nb;
        str = str + nb;
        str = str + c;
        str = str + nb;
        str = str + c;
        byte[] bytes = str.getBytes();
        for (int i = 0; i < bytes.length; i++) {
            Assert.assertFalse("String '" + str + "' contains a null byte at index " + i, bytes[i] == 0);
        }

        String fileName = TaintUtils.trust("test/pac/test/NullCharTestFile");
        TaintUtils.taint(fileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
        String line;
        while ((line = br.readLine()) != null) {
            for (int i = 0; i < line.length(); i++)
                Assert.assertFalse("String '" + line + "' contains a null byte at index " + i, line.charAt(i) == 0);
        }
        br.close();
    }

    public boolean test(boolean t) {
        if (t)
            t = true;
        else
            t = false;
        return t;
    }

    @Test
    public void codePointTest() {
        String str = TaintUtils.trust("0123\u3333\u3333\u3333456789");
        StringBuffer sb = new StringBuffer();
        str = TaintUtils.taint(str, 4, 6);
        for (int i = 0; i < str.codePointCount(0, str.length()); i++) {
            int cp = str.codePointAt(i);
            sb.appendCodePoint(cp);
            System.out.println("codepoint: " + sb);
        }
        Assert.assertTrue("strings should have the same taint",
                          TaintUtils.hasEqualTaint(str, sb.toString(), TaintValues.TRUST_MASK));
    }

    @Test
    public void encodingTest() {
        String str = TaintUtils.trust("0123xxx456789");
        StringBuffer sb = new StringBuffer();
        str = TaintUtils.taint(str, 4, 6);
        for (int i = 0; i < str.codePointCount(0, str.length()); i++) {
            int cp = str.codePointAt(i);
            sb.appendCodePoint(cp);
            System.out.println("codepoint: " + sb);
        }

        System.out.println(TaintUtils.createTaintDisplayLines(str));
        System.out.println(TaintUtils.createTaintDisplayLines(sb));

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

    @Test
    public void serializationTest() throws FileNotFoundException, IOException, ClassNotFoundException {
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        try {
            out = new ObjectOutputStream(new FileOutputStream(new File("string.obj")));
            String str = TaintUtils.trust("0123456789");
            str = TaintUtils.taint(str, 3, 6);
            out.writeObject(str);
            out.close();
            out = null;

            in = new ObjectInputStream(new FileInputStream(new File("string.obj")));
            String serializedStr = (String) in.readObject();
            in.close();
            in = null;

            // FIXME: The taint of the serialized object wont match, since string serialization
            // is not handled like the serialization of other classes.  But it should be
            // at least not trusted.
            Assert.assertFalse("lost taint through serializing and unserializing a string",
                               TaintUtils.isTrusted(serializedStr));

            PipedInputStream pis = new PipedInputStream();
            PipedOutputStream pos = new PipedOutputStream(pis);
            out = new ObjectOutputStream(pos);
            out.writeObject(str);
            out.close();
            out = null;

            in = new ObjectInputStream(pis);
            serializedStr = (String) in.readObject();
            in.close();
            in = null;

            // FIXME: The taint of the serialized object wont match, since string serialization
            // is not handled like the serialization of other classes.  But it should be
            // at least not trusted.
            Assert.assertFalse("lost taint through serializing and unserializing a string",
                               TaintUtils.isTrusted(serializedStr));
        } finally {
            if (out != null)
                out.close();
            if (in != null)
                in.close();
        }
    }
    
}
