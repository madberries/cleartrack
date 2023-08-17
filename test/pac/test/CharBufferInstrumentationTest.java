package pac.test;

import java.io.IOException;
import java.nio.CharBuffer;

import org.junit.Assert;
import org.junit.Test;

import pac.util.TaintUtils;
import pac.wrap.CharArrayTaint;

public class CharBufferInstrumentationTest {

  @Test
  public void appendTest() {
    CharBuffer cb = CharBuffer.allocate(30);
    String s = TaintUtils.trust("0123456789");
    cb.put(s);

    s = TaintUtils.taint(s, 3, 5);

    cb.append(s);
    Assert.assertFalse("untainted region marked as tainted",
        CharArrayTaint.isTainted(cb.array(), 0, 12));
    Assert.assertTrue("tainted region not marked as tainted",
        CharArrayTaint.isTainted(cb.array(), 13, 17));
    Assert.assertFalse("untainted region marked as tainted",
        CharArrayTaint.isTainted(cb.array(), 18, 19));

    cb.append(s, 2, 9);
    Assert.assertFalse("untainted region marked as tainted",
        CharArrayTaint.isTainted(cb.array(), 0, 12));
    Assert.assertTrue("tainted region not marked as tainted",
        CharArrayTaint.isTainted(cb.array(), 13, 17));
    Assert.assertFalse("untainted region marked as tainted",
        CharArrayTaint.isTainted(cb.array(), 18, 20));
    Assert.assertTrue("tainted region not marked as tainted",
        CharArrayTaint.isTainted(cb.array(), 21, 25));
    Assert.assertFalse("untainted region marked as tainted",
        CharArrayTaint.isTainted(cb.array(), 26, 26));
  }

  @Test
  public void arrayTest() {
    CharBuffer cb = CharBuffer.allocate(10);
    CharArrayTaint.taint(cb.array(), 3, 7);

    char[] ca = cb.array();
    Assert.assertFalse("untainted region marked as tainted", CharArrayTaint.isTainted(ca, 0, 2));
    Assert.assertTrue("tainted region not marked as tainted", CharArrayTaint.isTainted(ca, 3, 7));
    Assert.assertFalse("untainted region marked as tainted", CharArrayTaint.isTainted(ca, 8, 9));
  }

  @Test
  public void compactTest() {
    CharBuffer cb = CharBuffer.allocate(20);
    CharArrayTaint.taint(cb.array(), 13, 17);
    cb.position(10);

    cb.compact();
    Assert.assertFalse("untainted region marked as tainted",
        CharArrayTaint.isTainted(cb.array(), 0, 2));
    Assert.assertTrue("tainted region not marked as tainted",
        CharArrayTaint.isTainted(cb.array(), 3, 7));
    Assert.assertFalse("untainted region marked as tainted",
        CharArrayTaint.isTainted(cb.array(), 8, 9));
  }

  @Test
  public void duplicateTest() {
    CharBuffer bb = CharBuffer.allocate(10);
    CharArrayTaint.taint(bb.array(), 3, 7);

    CharBuffer bb2 = bb.duplicate();
    Assert.assertTrue("tainted region not marked as tainted",
        CharArrayTaint.isTainted(bb2.array(), 3, 7));
  }

  @Test
  public void getTest() {
    CharBuffer cb = CharBuffer.allocate(10);
    CharArrayTaint.taint(cb.array(), 5, 7);

    cb.position(3);
    char[] ca = new char[7];
    cb.get(ca);
    Assert.assertFalse("untainted region marked as tainted", CharArrayTaint.isTainted(ca, 0, 1));
    Assert.assertTrue("tainted region not marked as tainted", CharArrayTaint.isTainted(ca, 2, 4));
    Assert.assertFalse("untainted region marked as tainted", CharArrayTaint.isTainted(ca, 5, 6));

    cb.rewind();
    char[] ca2 = new char[10];
    cb.get(ca2, 2, 7);
    Assert.assertFalse("untainted region marked as tainted", CharArrayTaint.isTainted(ca2, 0, 6));
    Assert.assertTrue("tainted region not marked as tainted", CharArrayTaint.isTainted(ca2, 7, 8));
    Assert.assertFalse("untainted region marked as tainted", CharArrayTaint.isTainted(ca2, 9, 9));
  }

  @Test
  public void putTest() {
    CharBuffer bb = CharBuffer.allocate(10);

    char[] ba = new char[5];
    CharArrayTaint.taint(ba, 2, 2); // start,length
    bb.put(ba);
    Assert.assertFalse("untainted region marked as tainted",
        CharArrayTaint.isTainted(bb.array(), 0, 1));
    Assert.assertTrue("tainted region not marked as tainted",
        CharArrayTaint.isTainted(bb.array(), 2, 3));
    Assert.assertFalse("untainted region marked as tainted",
        CharArrayTaint.isTainted(bb.array(), 4, 9));

    char[] ba2 = new char[10];
    CharArrayTaint.taint(ba2, 4, 6); // start,length
    bb.put(ba2, 3, 5);
    Assert.assertFalse("untainted region marked as tainted",
        CharArrayTaint.isTainted(bb.array(), 0, 1));
    Assert.assertTrue("tainted region not marked as tainted",
        CharArrayTaint.isTainted(bb.array(), 2, 3));
    Assert.assertFalse("untainted region marked as tainted",
        CharArrayTaint.isTainted(bb.array(), 4, 5));
    Assert.assertTrue("tainted region not marked as tainted",
        CharArrayTaint.isTainted(bb.array(), 6, 8));
    Assert.assertFalse("untainted region marked as tainted",
        CharArrayTaint.isTainted(bb.array(), 9, 9));

    CharBuffer bb2 = CharBuffer.allocate(10);
    bb.rewind();
    bb2.put(bb);
    Assert.assertFalse("untainted region marked as tainted",
        CharArrayTaint.isTainted(bb2.array(), 0, 1));
    Assert.assertTrue("tainted region not marked as tainted",
        CharArrayTaint.isTainted(bb2.array(), 2, 3));
    Assert.assertFalse("untainted region marked as tainted",
        CharArrayTaint.isTainted(bb2.array(), 4, 5));
    Assert.assertTrue("tainted region not marked as tainted",
        CharArrayTaint.isTainted(bb2.array(), 6, 8));
    Assert.assertFalse("untainted region marked as tainted",
        CharArrayTaint.isTainted(bb2.array(), 9, 9));

    CharBuffer cb2 = CharBuffer.allocate(20);

    String s = TaintUtils.trust("0123456789");
    s = TaintUtils.taint(s, 4, 4);
    cb2.put(s);
    Assert.assertFalse("untainted region marked as tainted",
        CharArrayTaint.isTainted(cb2.array(), 0, 3));
    Assert.assertTrue("tainted region not marked as tainted",
        CharArrayTaint.isTainted(cb2.array(), 4, 7));
    Assert.assertFalse("untainted region marked as tainted",
        CharArrayTaint.isTainted(cb2.array(), 8, 19));

    cb2.put(s, 3, 9); // start,end-exclusive
    Assert.assertFalse("untainted region marked as tainted",
        CharArrayTaint.isTainted(cb2.array(), 0, 3));
    Assert.assertTrue("tainted region not marked as tainted",
        CharArrayTaint.isTainted(cb2.array(), 4, 7));
    Assert.assertFalse("untainted region marked as tainted",
        CharArrayTaint.isTainted(cb2.array(), 8, 10));
    Assert.assertTrue("tainted region not marked as tainted",
        CharArrayTaint.isTainted(cb2.array(), 11, 14));
    Assert.assertFalse("untainted region marked as tainted",
        CharArrayTaint.isTainted(cb2.array(), 15, 19));
  }

  @Test
  public void readTest() {
    CharBuffer cb = CharBuffer.allocate(10);
    CharArrayTaint.taint(cb.array(), 3, 7);
    CharBuffer cb2 = CharBuffer.allocate(10);

    try {
      cb.read(cb2);
    } catch (IOException e) {
      Assert.fail("exception while reading: " + e.getMessage());
    }

    Assert.assertFalse("untainted region marked as tainted",
        CharArrayTaint.isTainted(cb2.array(), 0, 2));
    Assert.assertTrue("tainted region not marked as tainted",
        CharArrayTaint.isTainted(cb2.array(), 3, 7));
    Assert.assertFalse("untainted region marked as tainted",
        CharArrayTaint.isTainted(cb2.array(), 8, 9));
  }

  @Test
  public void sliceTest() {
    CharBuffer cb = CharBuffer.allocate(20);
    CharArrayTaint.taint(cb.array(), 13, 17);
    cb.position(10);
    CharBuffer cb2 = cb.slice();
    char[] ca = new char[10];
    cb2.get(ca);
    Assert.assertFalse("untainted region marked as tainted", CharArrayTaint.isTainted(ca, 0, 2));
    Assert.assertTrue("tainted region not marked as tainted", CharArrayTaint.isTainted(ca, 3, 7));
    Assert.assertFalse("untainted region marked as tainted", CharArrayTaint.isTainted(ca, 8, 9));
  }

  @Test
  public void subSequenceTest() {
    String s = TaintUtils.trust("01234567890123456789");
    s = TaintUtils.taint(s, 13, 17);
    CharBuffer cb = CharBuffer.wrap(s);
    CharBuffer cb2 = (CharBuffer) cb.subSequence(11, 19);
    char[] ca = new char[8];
    cb2.get(ca);
    Assert.assertFalse("untainted region marked as tainted", CharArrayTaint.isTainted(ca, 0, 1));
    Assert.assertTrue("tainted region not marked as tainted", CharArrayTaint.isTainted(ca, 2, 6));
    Assert.assertFalse("untainted region marked as tainted", CharArrayTaint.isTainted(ca, 7, 7));
  }

  @Test
  public void toStringTest() {
    CharBuffer cb = CharBuffer.allocate(10);
    cb.put("0123456789".toCharArray());
    cb.position(0);
    CharArrayTaint.taint(cb.array(), 3, 7);
    String s = cb.toString();
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(s, 0, 2));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(s, 3, 7));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(s, 8, 9));
  }

  @Test
  public void wrapTest() {
    char[] ba1 = new char[10];
    CharArrayTaint.taint(ba1, 3, 7);
    CharBuffer bb1 = CharBuffer.wrap(ba1);
    Assert.assertFalse("untainted region marked as tainted",
        CharArrayTaint.isTainted(bb1.array(), 0, 2));
    Assert.assertTrue("tainted region not marked as tainted",
        CharArrayTaint.isTainted(bb1.array(), 3, 7));
    Assert.assertFalse("untainted region marked as tainted",
        CharArrayTaint.isTainted(bb1.array(), 8, 9));

    char[] ba2 = new char[20];
    CharArrayTaint.taint(ba2, 13, 17);
    CharBuffer bb2 = CharBuffer.wrap(ba2, 10, 10);
    char[] ca = new char[10];
    bb2.get(ca);
    Assert.assertFalse("untainted region marked as tainted", CharArrayTaint.isTainted(ca, 0, 2));
    Assert.assertTrue("tainted region not marked as tainted", CharArrayTaint.isTainted(ca, 3, 7));
    Assert.assertFalse("untainted region marked as tainted", CharArrayTaint.isTainted(ca, 8, 9));

    String s1 = TaintUtils.trust("0123456789");
    s1 = TaintUtils.taint(s1, 3, 7);
    CharBuffer cb1 = CharBuffer.wrap(s1);
    Assert.assertFalse("untainted region marked as tainted",
        TaintUtils.isTainted(cb1.toString(), 0, 2));
    Assert.assertTrue("tainted region not marked as tainted",
        TaintUtils.isTainted(cb1.toString(), 3, 7));
    Assert.assertFalse("untainted region marked as tainted",
        TaintUtils.isTainted(cb1.toString(), 8, 9));

    String s2 = TaintUtils.trust("01234567890123456789");
    s2 = TaintUtils.taint(s2, 13, 17);
    CharBuffer cb2 = CharBuffer.wrap(s2, 10, 20);
    char[] ca2 = new char[10];
    cb2.get(ca2);
    Assert.assertFalse("untainted region marked as tainted", CharArrayTaint.isTainted(ca2, 0, 2));
    Assert.assertTrue("tainted region not marked as tainted", CharArrayTaint.isTainted(ca2, 3, 7));
    Assert.assertFalse("untainted region marked as tainted", CharArrayTaint.isTainted(ca2, 8, 9));
  }
}
