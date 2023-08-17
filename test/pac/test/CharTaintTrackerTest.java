package pac.test;

import org.junit.Assert;
import org.junit.Test;
import org.owasp.esapi.ESAPI;

import pac.util.TaintUtils;
import pac.wrap.ByteArrayTaint;
import pac.wrap.CharArrayTaint;

public class CharTaintTrackerTest {

  @Test
  public void charArrayToCharArrayTest() {
    String tainted = "tainted string";
    TaintUtils.taint(tainted);
    char[] ch1 = tainted.toCharArray();
    char[] ch2 = new char[ch1.length];
    for (int i = 0; i < ch1.length; i++)
      ch2[i] = ch1[i];
    Assert.assertTrue("Losing taint information when copying from char[] to char[]",
        CharArrayTaint.isTainted(ch2, 0, tainted.length() - 1));
  }

  @Test
  public void byteArrayToCharArrayTest() {
    String tainted = "tainted string";
    TaintUtils.taint(tainted);
    byte[] buf = tainted.getBytes();
    char[] ch = new char[buf.length];
    for (int i = 0; i < buf.length; i++)
      ch[i] = (char) buf[i];
    Assert.assertTrue("Losing taint information when copying from byte[] to char[]",
        CharArrayTaint.isTainted(ch, 0, tainted.length() - 1));
  }

  @Test
  public void charArrayToByteArrayTest() {
    String tainted = "tainted string";
    TaintUtils.taint(tainted);
    char[] ch = tainted.toCharArray();
    byte[] buf = new byte[ch.length];
    for (int i = 0; i < ch.length; i++)
      buf[i] = (byte) ch[i];
    Assert.assertTrue("Losing taint information when copying from char[] to byte[]",
        ByteArrayTaint.isTainted(buf, 0, tainted.length() - 1));
  }

  @Test
  public void byteArrayToByteArrayTest() {
    String tainted = "tainted string";
    TaintUtils.taint(tainted);
    byte[] buf1 = tainted.getBytes();
    byte[] buf2 = new byte[buf1.length];
    for (int i = 0; i < buf1.length; i++)
      buf2[i] = buf1[i];
    Assert.assertTrue("Losing taint information when copying from byte[] to byte[]",
        ByteArrayTaint.isTainted(buf2, 0, tainted.length() - 1));
  }

  @Test
  public void stringToStringBufferTest() {
    String tainted = "tainted string";
    TaintUtils.taint(tainted);
    StringBuffer strBuf = new StringBuffer();
    for (int i = 0; i < tainted.length(); i++)
      strBuf.append(tainted.charAt(i));
    Assert.assertTrue("Losing taint information when copying into StringBuffer",
        TaintUtils.isTainted(strBuf.toString(), 0, tainted.length() - 1));
  }

  @Test
  public void nestedLoopTest1() {
    char[] tainted = "0123456789".toCharArray();
    CharArrayTaint.taint(tainted, 4, 6);
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < tainted.length; i++) {
      char c = tainted[i];
      for (int j = 0; j < 5; j++) {
        if (j + i > 5)
          break;
      }
      sb.append(c);
    }
    String ca = sb.toString();
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(ca, 0, 3));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(ca, 4, 6));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(ca, 7, 9));
  }

  @Test
  public void nestedLoopTest2() {
    char[] tainted = "0123456789".toCharArray();
    CharArrayTaint.taint(tainted, 4, 6);
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < 2; i++) {
      for (int j = 0; j < tainted.length; j++) {
        sb.append(tainted[j]);
      }
    }
    String ca = sb.toString();
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(ca, 0, 3));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(ca, 4, 6));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(ca, 7, 9));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(ca, 10, 13));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(ca, 14, 16));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(ca, 17, 19));
  }

  public char customCharAtMethod(char[] arr, int i) {
    return arr[i];
  }

  @Test
  public void methodCallTest() {
    char[] tainted = "0123456789".toCharArray();
    CharArrayTaint.taint(tainted, 4, 6);
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < tainted.length; i++) {
      char c = customCharAtMethod(tainted, i);
      for (int j = 0; j < 5; j++) {
        if (j + i > 5)
          break;
      }
      sb.append(c);
    }
    String ca = sb.toString();
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(ca, 0, 3));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(ca, 4, 6));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(ca, 7, 9));
  }

  @Test
  public void charAppendSquaresText() {
    String env_out = System.getenv("PATH");
    int[] squares = new int[env_out.length()];

    for (int i = 0; i < squares.length; i++) {
      squares[i] = i * i;
    }

    StringBuffer output = new StringBuffer();
    output.append('\"');
    Assert.assertTrue("Incoming character constant should be trusted.",
        TaintUtils.isTrusted(output.toString(), output.length() - 1, output.length() - 1));
    for (int i = 0; i < env_out.length(); i++) {
      char ch;
      int idx;
      if (i == 0) {
        ch = env_out.charAt(i);
        idx = i;
      } else {
        ch = env_out.charAt(squares[i] / i);
        idx = squares[i] / i;
      }
      output.append(ch);
      Assert.assertTrue(
          "Incoming character constant should match the taint value of the previous read character.",
          TaintUtils.hasEqualTaint(env_out.charAt(idx), output.charAt(output.length() - 1)));
    }
    output.append('"');
    Assert.assertTrue("Incoming character constant should be trusted.",
        TaintUtils.isTrusted(output.toString(), output.length() - 1, output.length() - 1));
  }

  private static float[] disassemble(String var) {
    char[] array = var.toCharArray(); // CLEARTRACK:DataType:Char
    float[] arrayout = new float[array.length];
    int i = 0;
    for (char letter : array) {
      arrayout[i] = (int) letter;
      i++;
      // System.out.println("Diassembling");
    }
    Assert.assertTrue("disassembled strings metadata does not match of the float array",
        CharArrayTaint.hasEqualTaint(array, arrayout));
    return arrayout;
  }

  private static String assemble(float[] arrayin) {
    int i = 0;
    char[] arrayinter = new char[arrayin.length];
    for (float number : arrayin) {
      arrayinter[i] = (char) number;
      // System.out.println(number);
      i++;
    }
    Assert.assertTrue("float arrays metadata does not match that of the new char array",
        CharArrayTaint.hasEqualTaint(arrayin, arrayinter));
    String back = "";
    for (char letter : arrayinter) {
      back = back.concat(Character.toString(letter));
    }
    return back;
  }

  @Test
  public void castTest() {
    String str = TaintUtils.trust("This is a tainted string");
    str = TaintUtils.taint(str, 2, 5);
    str = TaintUtils.taint(str, 8, 13);

    String newStr = assemble(disassemble(str));
    Assert.assertTrue("disassembled strings metadata does not match that of the original string",
        TaintUtils.hasEqualTaint(str, newStr));
  }

  @Test
  public void checkEncoding() {
    String s = TaintUtils.trust("0123456789");
    String enc = ESAPI.encoder().encodeForHTML(s);
    char[] ch = new char[10];
    for (int i = 0; i < ch.length; i++)
      ch[i] = enc.charAt(i);
    String newS = new String(ch);
    Assert.assertTrue("new string should have the same encoding as the original string",
        TaintUtils.hasEqualTaint(enc, newS));
  }

}
