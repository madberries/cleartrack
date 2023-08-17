package pac.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import pac.util.TaintUtils;
import pac.wrap.CharArrayTaint;

public class StringBufferInstrumentationTest {

  @Test
  public void constructorTest() {
    String cs = TaintUtils.trust("hullabaloo");
    cs = TaintUtils.taint(cs, 4, 4);
    StringBuffer sb = new StringBuffer(cs);
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 0, 3));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 4, 7));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 8, 9));

    String s = TaintUtils.trust("bababalooba");
    s = TaintUtils.taint(s, 3, 4);
    sb = new StringBuffer(s);
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 0, 2));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 3, 6));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 7, 9));
  }

  @Test
  public void appendTest() {
    StringBuffer sb = new StringBuffer();
    sb.append(TaintUtils.trust("0123456789"));

    // Test append(char[]):
    char[] ca1 = TaintUtils.trust("0123456789").toCharArray();
    CharArrayTaint.taint(ca1, 4, 7);
    sb.append(ca1);

    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 0, 13));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 14, 17));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 18, 19));

    // Test append(char[], int, int):
    char[] ca2 = TaintUtils.trust("01234567890123456789").toCharArray();
    CharArrayTaint.taint(ca2, 3, 7);
    CharArrayTaint.taint(ca2, 12, 18);
    sb.append(ca2, 5, 10);

    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 18, 19));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 20, 22));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 23, 26));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 27, 29));

    // Test append(CharSequence):
    String cs1 = TaintUtils.trust("0123456789");
    cs1 = TaintUtils.taint(cs1, 3, 6);
    sb.append(cs1);

    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 30, 32));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 33, 36));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 37, 39));

    // Test append(CharSequence, int, int):
    String cs2 = TaintUtils.trust("01234567890123456789");
    cs2 = TaintUtils.taint(cs2, 4, 8);
    cs2 = TaintUtils.taint(cs2, 13, 19);
    sb.append(cs2, 7, 17); // End exclusive.

    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 37, 39));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 40, 41));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 42, 45));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 46, 49));

    // Test append(Object):
    Object obj = new Object() {
      String myString = TaintUtils.trust("0123456789");

      public String toString() {
        TaintUtils.taint(myString);
        return myString;
      }
    };
    sb.append(obj);

    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 50, 59));

    // Make sure the new taint doesn't spill past the end of the buffer.
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 60, 69));

    // Test append(String):
    String s = TaintUtils.trust("0123456789");
    s = TaintUtils.taint(s, 3, 5);
    sb.append(s);

    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 60, 62));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 63, 67));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 68, 69));

    // Test append(StringBuffer):
    StringBuffer sb2 = new StringBuffer(TaintUtils.trust("01234567890123456789"));
    TaintUtils.taint(sb2, 3, 7);
    TaintUtils.taint(sb2, 13, 18);
    sb.append(sb2);
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 70, 72));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 73, 77));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 78, 82));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 83, 88));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 89, 89));

    // Test numeric appends...
    
    sb.append(true);
    Assert.assertTrue("trusted region not marked as trusted", TaintUtils.isTrusted(sb, 90, 93));

    sb.append(1.234f);
    Assert.assertTrue("trusted region not marked as trusted", TaintUtils.isTrusted(sb, 94, 98));

    sb.append(1.234d);
    Assert.assertTrue("trusted region not marked as trusted", TaintUtils.isTrusted(sb, 99, 103));

    sb.append(1234);
    Assert.assertTrue("trusted region not marked as trusted", TaintUtils.isTrusted(sb, 104, 107));

    sb.append(1234L);
    Assert.assertTrue("trusted region not marked as trusted", TaintUtils.isTrusted(sb, 108, 111));
  }

  @Test
  public void deleteTest() {
    StringBuffer sb = new StringBuffer(TaintUtils.trust("01234567890123456789"));
    TaintUtils.taint(sb, 3, 7);
    TaintUtils.taint(sb, 13, 17);
    sb.delete(6, 11); // Delete chars 6-10, result should be:

    // 012345678901234
    // OOOXXXOOXXXXXOO

    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 0, 2));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 3, 5));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 6, 7));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 8, 12));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 13, 14));

    sb.deleteCharAt(4);

    // 01234567890123
    // OOOXXOOXXXXXOO

    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 0, 2));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 3, 4));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 5, 6));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 7, 11));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 12, 13));
  }

  @Test
  public void getCharsTest() {
    StringBuffer sb = new StringBuffer(TaintUtils.trust("01234567890123456789"));
    TaintUtils.taint(sb, 3, 7);
    TaintUtils.taint(sb, 13, 17);
    char[] ca = new char[20];
    sb.getChars(5, 15, ca, 3);

    // sb
    // 01234567890123456789
    // OOOXXXXXXXOOOXXXXXOO

    // ca
    // 01234567890123456789
    // OOOXXXXXOOOXXOOOOOOO

    Assert.assertFalse("untainted region marked as tainted", CharArrayTaint.isTainted(ca, 0, 2));
    Assert.assertTrue("tainted region not marked as tainted", CharArrayTaint.isTainted(ca, 3, 7));
    Assert.assertFalse("untainted region marked as tainted", CharArrayTaint.isTainted(ca, 8, 10));
    Assert.assertTrue("tainted region not marked as tainted", CharArrayTaint.isTainted(ca, 11, 12));
    Assert.assertFalse("untainted region marked as tainted", CharArrayTaint.isTainted(ca, 13, 19));
  }

  @Test
  public void insertPrimitiveTest() {

    // Test insert(int, boolean):
    StringBuffer sb = new StringBuffer(TaintUtils.trust("0123456789"));
    TaintUtils.taint(sb, 3, 5);
    sb.insert(5, true);

    // expected output
    // 01234TRUE56789
    // OOOXXOOOOXXXOO

    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 0, 2));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 3, 4));
    Assert.assertTrue("untainted region marked as tainted", TaintUtils.isTrusted(sb, 5, 8));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 9, 11));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 12, 13));

    // Test insert(int, char):
    StringBuffer sb2 = new StringBuffer(TaintUtils.trust("0123456789"));
    TaintUtils.taint(sb2, 3, 5);
    sb2.insert(5, 'X');

    // expected output
    // 01234X56789
    // OOOXXOXXXOO

    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb2, 0, 2));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb2, 3, 4));
    Assert.assertTrue("untainted region marked as tainted", TaintUtils.isTrusted(sb2, 5, 5));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb2, 6, 8));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb2, 9, 10));

    // Test insert(int, double):
    StringBuffer sb3 = new StringBuffer(TaintUtils.trust("0123456789"));
    TaintUtils.taint(sb3, 3, 5);
    double d = 0.25d;
    sb3.insert(5, d);

    // expected output
    // 012340.2556789
    // OOOXXOOOOXXXOO

    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb3, 0, 2));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb3, 3, 4));
    Assert.assertTrue("untainted region marked as tainted", TaintUtils.isTrusted(sb3, 5, 8));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb3, 9, 11));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb3, 12, 13));

    // Test insert(int, float):
    StringBuffer sb4 = new StringBuffer(TaintUtils.trust("0123456789"));
    TaintUtils.taint(sb4, 3, 5);
    float f = 0.25f;
    sb4.insert(5, f);

    // expected output
    // 012340.2556789
    // OOOXXOOOOXXXOO

    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb4, 0, 2));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb4, 3, 4));
    Assert.assertTrue("untainted region marked as tainted", TaintUtils.isTrusted(sb4, 5, 8));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb4, 9, 11));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb4, 12, 13));

    // Test insert(int, int):
    StringBuffer sb5 = new StringBuffer(TaintUtils.trust("0123456789"));
    TaintUtils.taint(sb5, 3, 5);
    int i = 1234;
    sb5.insert(5, i);

    // expected output
    // 01234123456789
    // OOOXXOOOOXXXOO

    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb5, 0, 2));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb5, 3, 4));
    Assert.assertTrue("untainted region marked as tainted", TaintUtils.isTrusted(sb5, 5, 8));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb5, 9, 11));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb5, 12, 13));

    // Test insert(int, long):
    StringBuffer sb6 = new StringBuffer(TaintUtils.trust("0123456789"));
    TaintUtils.taint(sb6, 3, 5);
    long l = 1234l;
    sb6.insert(5, l);

    // expected output
    // 01234123456789
    // OOOXXOOOOXXXOO

    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb6, 0, 2));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb6, 3, 4));
    Assert.assertTrue("untainted region marked as tainted", TaintUtils.isTrusted(sb6, 5, 8));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb6, 9, 11));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb6, 12, 13));
  }

  @Test
  public void insertObjectTest() {
    // Test insert(int, char[]):
    StringBuffer sb = new StringBuffer(TaintUtils.trust("0123456789"));
    TaintUtils.taint(sb, 3, 5);
    char[] ca = TaintUtils.trust("0123456789").toCharArray();
    CharArrayTaint.taint(ca, 4, 7);
    sb.insert(5, ca);

    // expected output
    // 01234012345678956789
    // OOOXXOOOOXXXXOOXXXOO

    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 0, 2));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 3, 4));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 5, 8));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 9, 12));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 13, 14));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 15, 17));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 18, 19));

    // Test insert(int, char[], int, int):
    sb = new StringBuffer(TaintUtils.trust("0123456789"));
    TaintUtils.taint(sb, 3, 5);
    char[] ca2 = TaintUtils.trust("0123456789").toCharArray();
    CharArrayTaint.taint(ca2, 4, 4);
    sb.insert(5, ca2, 2, 7);

    // expected output
    // 01234234567856789
    // OOOXXOOXXXXOXXXOO

    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 0, 2));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 3, 4));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 5, 6));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 7, 10));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 11, 11));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 12, 14));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 15, 16));

    // Test insert(int, CharSequence):
    sb = new StringBuffer(TaintUtils.trust("0123456789"));
    TaintUtils.taint(sb, 3, 5);
    String cs = TaintUtils.trust(TaintUtils.trust("0123456789"));
    cs = TaintUtils.taint(cs, 4, 4);
    sb.insert(5, cs);

    // expected output
    // 01234012345678956789
    // OOOXXOOOOXXXXOOXXXOO

    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 0, 2));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 3, 4));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 5, 8));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 9, 12));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 13, 14));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 15, 17));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 18, 19));

    // Test insert(int, CharSequence, int, int):
    sb = new StringBuffer(TaintUtils.trust("0123456789"));
    TaintUtils.taint(sb, 3, 5);
    String cs2 = TaintUtils.trust(TaintUtils.trust("0123456789"));
    cs2 = TaintUtils.taint(cs2, 4, 4);
    sb.insert(5, cs2, 2, 9); // End exclusive.

    // expected output
    // 01234234567856789
    // OOOXXOOXXXXOXXXOO

    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 0, 2));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 3, 4));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 5, 6));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 7, 10));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 11, 11));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 12, 14));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 15, 16));

    // Test insert(int, Object):
    sb = new StringBuffer(TaintUtils.trust("0123456789"));
    TaintUtils.taint(sb, 3, 5);
    Object obj = new Object() {
      String myString = TaintUtils.trust(TaintUtils.trust("0123456789"));

      // Need to know the size of the toString output for the asserts.
      public String toString() {
        TaintUtils.taint(myString, 4, 7);
        return myString;
      }
    };
    sb.insert(5, obj);

    // expected output
    // 01234012345678956789
    // OOOXXOOOOXXXXOOXXXOO

    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 0, 2));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 3, 4));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 5, 8));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 9, 12));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 13, 14));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 15, 17));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 18, 19));

    // Test insert(int, String):
    sb = new StringBuffer(TaintUtils.trust("0123456789"));
    TaintUtils.taint(sb, 3, 5);
    String s = TaintUtils.trust(TaintUtils.trust("0123456789"));
    s = TaintUtils.taint(s, 4, 4);
    sb.insert(5, s);

    // expected output
    // 01234012345678956789
    // OOOXXOOOOXXXXOOXXXOO

    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 0, 2));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 3, 4));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 5, 8));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 9, 12));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 13, 14));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 15, 17));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 18, 19));
  }

  @Test
  public void replaceTest() {
    StringBuffer sb = new StringBuffer(TaintUtils.trust("01234567890123456789"));
    TaintUtils.taint(sb, 5, 14);
    // 01234567890123456789
    // ?????XXXXXXXXXX?????
    String s = TaintUtils.trust("01234");
    s = TaintUtils.taint(s, 2, 3); // ??XX?
    sb.replace(7, 14, s);
    // 01234567890123456789
    // 012345601234456789
    // ?????XX??XX?X?????

    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 0, 4));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 5, 6));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 7, 8));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 9, 10));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 11, 11));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 12, 12));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 13, 17));
  }

  @Test
  public void reverseTest() {
    StringBuffer sb = new StringBuffer(TaintUtils.trust("0123456789"));
    TaintUtils.taint(sb, 3, 4);
    TaintUtils.taint(sb, 7, 9);
    sb.reverse();

    // expected output
    // 0123456789 indices
    // 9876543210 contents
    // XXXOOXXOOO taint
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 0, 2));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 3, 4));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 5, 6));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 7, 9));
  }

  @Test
  public void setLengthTest() {
    StringBuffer sb = new StringBuffer(TaintUtils.trust("0123456789"));
    TaintUtils.taint(sb, 5, 9);
    sb.setLength(8);
    String s = sb.toString();
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(s, 0, 4));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(s, 5, 7));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(s, 8, 9));
  }

  @Test
  public void subSequenceTest() {
    StringBuffer sb = new StringBuffer(TaintUtils.trust("0123456789"));
    TaintUtils.taint(sb, 1, 3);
    TaintUtils.taint(sb, 6, 8);
    String cs = (String) sb.subSequence(2, 7); // End inclusive.
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(cs, 0, 1));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(cs, 2, 3));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(cs, 4, 5));
  }

  @Test
  public void substringTest() {
    StringBuffer sb = new StringBuffer(TaintUtils.trust("0123456789"));
    TaintUtils.taint(sb, 1, 3);
    TaintUtils.taint(sb, 6, 8);
    String s = sb.substring(2, 7); // End inclusive.
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(s, 0, 1));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(s, 2, 3));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(s, 4, 5));
  }

  @Test
  public void toStringTest() {
    StringBuffer sb = new StringBuffer(TaintUtils.trust("0123456789"));
    TaintUtils.taint(sb, 1, 3);
    TaintUtils.taint(sb, 6, 8);
    String s = sb.toString(); // End inclusive.
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(s, 0, 0));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(s, 1, 3));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(s, 4, 5));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(s, 6, 8));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(s, 9, 9));
  }

  @Test
  public void nullByteTest() throws IOException {
    String str = "abcde";
    FileInputStream in = new FileInputStream(new File("/dev/zero"));
    char nb = (char) in.read();
    in.close();
    str = str + nb;
    StringBuffer sb = new StringBuffer(str);
    sb.append(nb);
    sb.insert(5, nb);
    sb.setCharAt(2, nb);
    byte[] bytes = sb.toString().getBytes();
    for (int i = 0; i < bytes.length; i++) {
      Assert.assertFalse("String '" + str + "' contains a null byte at index " + i, bytes[i] == 0);
    }
  }

}
