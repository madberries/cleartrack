package pac.test;

import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;

import pac.util.TaintUtils;
import pac.wrap.ByteArrayTaint;

public class ByteBufferInstrumentationTest {

    @Test
    public void arrayTest() {
        ByteBuffer bb = ByteBuffer.allocate(10);
        byte[] ba = bb.array();
        ByteArrayTaint.taint(ba, 3, 7);
        Assert.assertFalse("non-tainted region not marked as tainted", ByteArrayTaint.isTainted(ba, 0, 2));
        Assert.assertTrue("tainted region not marked as tainted", ByteArrayTaint.isTainted(ba, 3, 7));
        Assert.assertFalse("non-tainted region not marked as tainted", ByteArrayTaint.isTainted(ba, 8, 9));
    }

    @Test
    public void compactTest() {
        ByteBuffer bb = ByteBuffer.allocate(20);
        ByteArrayTaint.taint(bb.array(), 13, 17);
        bb.position(10);

        bb.compact();
        byte[] ba = bb.array();
        Assert.assertFalse("untainted region marked as tainted", ByteArrayTaint.isTainted(ba, 0, 2));
        Assert.assertTrue("tainted region not marked as tainted", ByteArrayTaint.isTainted(ba, 3, 7));
        Assert.assertFalse("untainted region marked as tainted", ByteArrayTaint.isTainted(ba, 8, 9));
    }

    @Test
    public void duplicateTest() {
        ByteBuffer bb = ByteBuffer.allocate(10);
        ByteArrayTaint.taint(bb.array(), 3, 7);

        ByteBuffer bb2 = bb.duplicate();
        Assert.assertTrue("tainted region not marked as tainted", ByteArrayTaint.isTainted(bb2.array(), 3, 7));
    }

    @Test
    public void getTest() {
        byte[] bytes = TaintUtils.trust("0123456789").getBytes();
        ByteArrayTaint.taint(bytes, 5, 7);
        ByteBuffer bb = ByteBuffer.wrap(bytes);

        bb.position(3);
        byte[] ba = new byte[7];
        bb.get(ba);
        Assert.assertFalse("untainted region marked as tainted", ByteArrayTaint.isTainted(ba, 0, 1));
        Assert.assertTrue("tainted region not marked as tainted", ByteArrayTaint.isTainted(ba, 2, 4));
        Assert.assertFalse("untainted region marked as tainted", ByteArrayTaint.isTainted(ba, 5, 6));

        bb.rewind();
        byte[] ba2 = new byte[10];
        bb.get(ba2, 2, 7);
        Assert.assertFalse("untainted region marked as tainted", ByteArrayTaint.isTainted(ba2, 0, 6));
        Assert.assertTrue("tainted region not marked as tainted", ByteArrayTaint.isTainted(ba2, 7, 8));
        Assert.assertFalse("untainted region marked as tainted", ByteArrayTaint.isTainted(ba2, 9, 9));
    }

    @Test
    public void putTest() {
        ByteBuffer bb = ByteBuffer.allocate(10);

        byte[] ba = new byte[5];
        ByteArrayTaint.taint(ba, 2, 3);
        bb.put(ba);
        Assert.assertFalse("untainted region marked as tainted", ByteArrayTaint.isTainted(bb.array(), 0, 1));
        Assert.assertTrue("tainted region not marked as tainted", ByteArrayTaint.isTainted(bb.array(), 2, 3));
        Assert.assertFalse("untainted region marked as tainted", ByteArrayTaint.isTainted(bb.array(), 4, 9));

        byte[] ba2 = new byte[10];
        ByteArrayTaint.taint(ba2, 4, 6);
        bb.put(ba2, 3, 5);
        Assert.assertFalse("untainted region marked as tainted", ByteArrayTaint.isTainted(bb.array(), 0, 1));
        Assert.assertTrue("tainted region not marked as tainted", ByteArrayTaint.isTainted(bb.array(), 2, 3));
        Assert.assertFalse("untainted region marked as tainted", ByteArrayTaint.isTainted(bb.array(), 4, 5));
        Assert.assertTrue("tainted region not marked as tainted", ByteArrayTaint.isTainted(bb.array(), 6, 8));
        Assert.assertFalse("untainted region marked as tainted", ByteArrayTaint.isTainted(bb.array(), 9, 9));

        ByteBuffer bb2 = ByteBuffer.allocate(10);
        bb.rewind();
        bb2.put(bb);
        Assert.assertFalse("untainted region marked as tainted", ByteArrayTaint.isTainted(bb2.array(), 0, 1));
        Assert.assertTrue("tainted region not marked as tainted", ByteArrayTaint.isTainted(bb2.array(), 2, 3));
        Assert.assertFalse("untainted region marked as tainted", ByteArrayTaint.isTainted(bb2.array(), 4, 5));
        Assert.assertTrue("tainted region not marked as tainted", ByteArrayTaint.isTainted(bb2.array(), 6, 8));
        Assert.assertFalse("untainted region marked as tainted", ByteArrayTaint.isTainted(bb2.array(), 9, 9));
    }

    @Test
    public void sliceTest() {
        ByteBuffer bb = ByteBuffer.allocate(20);
        byte[] ba = bb.array();
        ByteArrayTaint.taint(ba, 13, 17);
        bb.position(10);
        ByteBuffer bb2 = bb.slice();
        byte[] ba2 = new byte[10];
        bb2.get(ba2);
        Assert.assertFalse("untainted region marked as tainted", ByteArrayTaint.isTainted(ba2, 0, 2));
        Assert.assertTrue("tainted region not marked as tainted", ByteArrayTaint.isTainted(ba2, 3, 7));
        Assert.assertFalse("untainted region marked as tainted", ByteArrayTaint.isTainted(ba2, 8, 9));
    }

    @Test
    public void wrapTest() {
        byte[] ba1 = new byte[10];
        ByteArrayTaint.taint(ba1, 3, 7);
        ByteBuffer bb1 = ByteBuffer.wrap(ba1);
        Assert.assertFalse("untainted region marked as tainted", ByteArrayTaint.isTainted(bb1.array(), 0, 2));
        Assert.assertTrue("tainted region not marked as tainted", ByteArrayTaint.isTainted(bb1.array(), 3, 7));
        Assert.assertFalse("untainted region marked as tainted", ByteArrayTaint.isTainted(bb1.array(), 8, 9));

        byte[] ba2 = new byte[20];
        ByteArrayTaint.taint(ba2, 13, 17);
        ByteBuffer bb2 = ByteBuffer.wrap(ba2, 10, 10);
        byte[] ba3 = new byte[10];
        bb2.get(ba3);
        Assert.assertFalse("untainted region marked as tainted", ByteArrayTaint.isTainted(ba3, 0, 2));
        Assert.assertTrue("tainted region not marked as tainted", ByteArrayTaint.isTainted(ba3, 3, 7));
        Assert.assertFalse("untainted region marked as tainted", ByteArrayTaint.isTainted(ba3, 8, 9));
    }

}
