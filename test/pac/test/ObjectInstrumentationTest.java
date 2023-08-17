package pac.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Random;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import pac.util.TaintUtils;
import pac.wrap.ByteArrayTaint;
import pac.wrap.IntArrayTaint;

public class ObjectInstrumentationTest implements Serializable {
  private static final long serialVersionUID = -3326881942920215072L;

  class MyObject {
    @Override
    public String toString() {
      return "MyObj";
    }
  }

  @AfterClass
  public static void cleanup() {
    new File("object.obj").delete();
  }

  @Test
  public void newTest() {
    MyObject myObj = new MyObject();
    System.out.println(myObj.toString());
  }

  @Test
  public void toStringTest() {
    String s = TaintUtils.trust("01234567890");
    s = TaintUtils.taint(s, 3, 7);
    Object o1 = new StringBuffer(s);
    s = o1.toString();

    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(s, 0, 2));
    Assert.assertTrue("tainted region marked as untainted", TaintUtils.isTainted(s, 3, 7));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(s, 8, 9));

    s = TaintUtils.trust("0123456789");
    s = TaintUtils.taint(s, 4, 8);
    char[] o2 = s.toCharArray();
    s = new String(o2);
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(s, 0, 3));
    Assert.assertTrue("tainted region marked as untainted", TaintUtils.isTainted(s, 4, 8));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(s, 9, 9));

    byte[] b = TaintUtils.trust("0123456789").getBytes();
    ByteArrayTaint.taint(b, 2, 6);
    Object o3 = ByteBuffer.wrap(b);
    s = new String(((ByteBuffer) o3).array(), 0, 10);
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(s, 0, 1));
    Assert.assertTrue("tainted region marked as untainted", TaintUtils.isTainted(s, 2, 6));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(s, 7, 9));

    Object o4 = new Object() {
      String myString = TaintUtils.trust("0123456789");

      public String toString() {
        return myString;
      }
    };
    s = o4.toString();
    s = TaintUtils.taint(s, 5, 7);
    // Objects of unknown types should be completely tainted.
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(s, 0, 4));
    Assert.assertTrue("tainted region marked as untainted", TaintUtils.isTainted(s, 5, 7));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(s, 8, 9));
  }

  @Test
  public void serializationTest()
      throws FileNotFoundException, IOException, ClassNotFoundException {
    Random rand = new Random();
    int[] data = new int[20];
    for (int i = 0; i < data.length; i++) {
      data[i] = TaintUtils.random(rand);
    }
    int start = TaintUtils.random(rand);
    int end = TaintUtils.random(rand);
    if (start > end) {
      int temp = start;
      start = end;
      end = temp;
    }
    SerializableObject obj = new SerializableObject(data, start, end);

    ObjectOutputStream out = null;
    ObjectInputStream in = null;

    try {
      out = new ObjectOutputStream(new FileOutputStream(new File("object.obj")));

      out.writeObject(obj);
      out.close();
      out = null;

      in = new ObjectInputStream(new FileInputStream(new File("object.obj")));
      SerializableObject serializedObj = (SerializableObject) in.readObject();
      in.close();
      in = null;

      Assert.assertTrue("serialized object does not match original",
          obj.start == serializedObj.start && TaintUtils.taintMatchesValue(serializedObj.start));
      Assert.assertTrue("serialized object does not match original",
          obj.end == serializedObj.end && TaintUtils.taintMatchesValue(serializedObj.end));
      Assert.assertTrue("serialized object does not match original",
          IntArrayTaint.equals(obj.data, serializedObj.data));

      PipedInputStream pis = new PipedInputStream();
      PipedOutputStream pos = new PipedOutputStream(pis);
      out = new ObjectOutputStream(pos);
      out.writeObject(obj);
      out.close();
      out = null;

      in = new ObjectInputStream(pis);
      serializedObj = (SerializableObject) in.readObject();
      in.close();
      in = null;

      Assert.assertTrue("serialized object does not match original",
          obj.start == serializedObj.start && TaintUtils.taintMatchesValue(serializedObj.start));
      Assert.assertTrue("serialized object does not match original",
          obj.end == serializedObj.end && TaintUtils.taintMatchesValue(serializedObj.end));
      Assert.assertTrue("serialized object does not match original",
          IntArrayTaint.equals(obj.data, serializedObj.data));
    } finally {
      if (out != null)
        out.close();
      if (in != null)
        in.close();
    }
  }

  class SerializableObject implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = -8919252314739226981L;

    int[] data;
    int start, end;

    SerializableObject(int[] data, int start, int end) {
      this.data = data;
      this.start = start;
      this.end = end;
    }
  }
}
