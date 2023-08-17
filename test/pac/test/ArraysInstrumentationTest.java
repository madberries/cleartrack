package pac.test;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import pac.config.CleartrackException;
import pac.util.TaintUtils;
import pac.wrap.ByteArrayTaint;
import pac.wrap.CharArrayTaint;
import pac.wrap.IntArrayTaint;

public class ArraysInstrumentationTest {
  static Random rand = new Random();

  private static int[][] deepClone(int[][] array) {
    int[][] result = new int[array.length][];
    for (int i = 0; i < result.length; i++) {
      result[i] = array[i].clone();
    }
    return result;
  }

  @Test
  public void cloneTest() {
    int[][] array = {{1, 2, 3}, {4, 5, 6}};
    @SuppressWarnings("unused")
    int[][] arrayClone = deepClone(array);
  }

  @Test
  public void copyOfTest() {
    {
      byte[] a1 = new byte[10];
      ByteArrayTaint.taint(a1, 3, 4);

      byte[] a2 = Arrays.copyOf(a1, 10);
      Assert.assertFalse(ByteArrayTaint.isTainted(a2, 0, 2));
      Assert.assertTrue(ByteArrayTaint.isTainted(a2, 3, 4));
      Assert.assertFalse(ByteArrayTaint.isTainted(a2, 5, 9));
    }

    {
      char[] a1 = new char[10];
      CharArrayTaint.taint(a1, 3, 6);

      char[] a2 = Arrays.copyOf(a1, 10);
      Assert.assertFalse(CharArrayTaint.isTainted(a2, 0, 2));
      Assert.assertTrue(CharArrayTaint.isTainted(a2, 3, 6));
      Assert.assertFalse(CharArrayTaint.isTainted(a2, 7, 9));
    }

    {
      int[] a1 = new int[10];
      IntArrayTaint.taint(a1, 3, 4);

      int[] a2 = Arrays.copyOf(a1, 10);
      Assert.assertFalse(IntArrayTaint.isTainted(a2, 0, 2));
      Assert.assertTrue(IntArrayTaint.isTainted(a2, 3, 6));
      Assert.assertFalse(IntArrayTaint.isTainted(a2, 7, 9));
    }
  }

  @Test
  public void copyOfRangeTest() {
    {
      byte[] a1 = new byte[10];
      ByteArrayTaint.taint(a1, 3, 6);

      byte[] a2 = Arrays.copyOfRange(a1, 2, 10); // To index exclusive.
      Assert.assertFalse(ByteArrayTaint.isTainted(a2, 0, 0));
      Assert.assertTrue(ByteArrayTaint.isTainted(a2, 1, 4));
      Assert.assertFalse(ByteArrayTaint.isTainted(a2, 5, 7));

      byte[] a3 = Arrays.copyOfRange(a1, 4, 10); // To index exclusive.
      Assert.assertTrue(ByteArrayTaint.isTainted(a3, 0, 2));
      Assert.assertFalse(ByteArrayTaint.isTainted(a3, 3, 5));

      byte[] a4 = Arrays.copyOfRange(a1, 0, 6); // To index exclusive.
      Assert.assertFalse(ByteArrayTaint.isTainted(a4, 0, 2));
      Assert.assertTrue(ByteArrayTaint.isTainted(a4, 3, 5));
    }

    {
      char[] a1 = new char[10];
      CharArrayTaint.taint(a1, 3, 6);

      char[] a2 = Arrays.copyOfRange(a1, 2, 10); // To index exclusive.
      Assert.assertFalse(CharArrayTaint.isTainted(a2, 0, 0));
      Assert.assertTrue(CharArrayTaint.isTainted(a2, 1, 4));
      Assert.assertFalse(CharArrayTaint.isTainted(a2, 5, 7));

      char[] a3 = Arrays.copyOfRange(a1, 4, 10); // To index exclusive.
      Assert.assertTrue(CharArrayTaint.isTainted(a3, 0, 2));
      Assert.assertFalse(CharArrayTaint.isTainted(a3, 3, 5));

      char[] a4 = Arrays.copyOfRange(a1, 0, 6); // To index exclusive.
      Assert.assertFalse(CharArrayTaint.isTainted(a4, 0, 2));
      Assert.assertTrue(CharArrayTaint.isTainted(a4, 3, 5));
    }

    {
      int[] a1 = new int[10];
      IntArrayTaint.taint(a1, 3, 6);

      int[] a2 = Arrays.copyOfRange(a1, 2, 10); // To index exclusive.
      Assert.assertFalse(IntArrayTaint.isTainted(a2, 0, 0));
      Assert.assertTrue(IntArrayTaint.isTainted(a2, 1, 4));
      Assert.assertFalse(IntArrayTaint.isTainted(a2, 5, 7));

      int[] a3 = Arrays.copyOfRange(a1, 4, 10); // To index exclusive.
      Assert.assertTrue(IntArrayTaint.isTainted(a3, 0, 2));
      Assert.assertFalse(IntArrayTaint.isTainted(a3, 3, 5));

      int[] a4 = Arrays.copyOfRange(a1, 0, 6); // To index exclusive.
      Assert.assertFalse(IntArrayTaint.isTainted(a4, 0, 2));
      Assert.assertTrue(IntArrayTaint.isTainted(a4, 3, 5));
    }
  }

  @Test
  public void sortTest() {
    // Populate an array of random tainted integers where the taint matches the value.
    int[] nums = new int[100];
    for (int i = 0; i < nums.length; i++)
      nums[i] = TaintUtils.random(rand);

    Arrays.sort(nums);

    // Ensure not only that the array is sorted, but that the taint at each index matches the value
    // at the same index.
    int last = Integer.MIN_VALUE;
    for (int i = 0; i < nums.length; i++) {
      Assert.assertTrue("The array that we sorted is not sorted", last <= nums[i]);
      last = nums[i];
      Assert.assertTrue("The taint does not match the value at index " + i,
          TaintUtils.taintMatchesValue(nums[i]));
    }
  }

  @Test
  public void overflowUnderflowTest() {
    boolean exCaught;

    int MAX = TaintUtils.taint(Integer.MAX_VALUE);

    System.out.println("testing boolean arrays...");
    boolean[] boolArr = new boolean[100];
    for (int i = 0; i < 1000; i++) {
      int overIdx = MAX + rand.nextInt(MAX) + 1;
      String overStr = "" + overIdx;
      Assert.assertEquals("Overflowed index should equals \"OVERFLOW\" when converted to a string.",
          overStr, "OVERFLOW");

      // Test boolean array store:
      exCaught = false;
      try {
        boolArr[overIdx] = rand.nextBoolean();
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);

      exCaught = false;
      try {
        Array.setBoolean(boolArr, overIdx, rand.nextBoolean());
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);

      // Test boolean array load:
      exCaught = false;
      try {
        System.out.println("a[" + overStr + "] = " + boolArr[overIdx]);
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);

      exCaught = false;
      try {
        System.out.println("a[" + overStr + "] = " + Array.getBoolean(boolArr, overIdx));
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);
    }

    System.out.println("testing byte arrays...");
    byte[] byteArr = new byte[100];
    for (int i = 0; i < 1000; i++) {
      int overIdx = MAX + rand.nextInt(MAX) + 1;
      String overStr = "" + overIdx;
      Assert.assertEquals("Overflowed index should equals \"OVERFLOW\" when converted to a string.",
          overStr, "OVERFLOW");

      // Test byte array store:
      exCaught = false;
      try {
        byteArr[overIdx] = (byte) rand.nextInt(8);
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);

      exCaught = false;
      try {
        Array.setByte(byteArr, overIdx, (byte) rand.nextInt(8));
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);

      // Test byte array load:
      exCaught = false;
      try {
        System.out.println("a[" + overStr + "] = " + byteArr[overIdx]);
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);

      exCaught = false;
      try {
        System.out.println("a[" + overStr + "] = " + Array.getByte(byteArr, overIdx));
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);
    }

    System.out.println("testing character arrays...");
    char[] charArr = new char[100];
    for (int i = 0; i < 1000; i++) {
      int overIdx = MAX + rand.nextInt(MAX) + 1;
      String overStr = "" + overIdx;
      Assert.assertEquals("Overflowed index should equals \"OVERFLOW\" when converted to a string.",
          overStr, "OVERFLOW");

      // Test char array store:
      exCaught = false;
      try {
        charArr[overIdx] = (char) rand.nextInt(8);
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);

      exCaught = false;
      try {
        Array.setChar(charArr, overIdx, (char) rand.nextInt(8));
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);

      // Test char array load:
      exCaught = false;
      try {
        System.out.println("a[" + overStr + "] = " + charArr[overIdx]);
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);

      exCaught = false;
      try {
        System.out.println("a[" + overStr + "] = " + Array.getChar(charArr, overIdx));
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);
    }

    System.out.println("testing short arrays...");
    short[] shortArr = new short[100];
    for (int i = 0; i < 1000; i++) {
      int overIdx = MAX + rand.nextInt(MAX) + 1;
      String overStr = "" + overIdx;
      Assert.assertEquals("Overflowed index should equals \"OVERFLOW\" when converted to a string.",
          overStr, "OVERFLOW");

      // Test short array store:
      exCaught = false;
      try {
        shortArr[overIdx] = (short) rand.nextInt(16);
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);

      exCaught = false;
      try {
        Array.setShort(shortArr, overIdx, (short) rand.nextInt(16));
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);

      // Test short array load:
      exCaught = false;
      try {
        System.out.println("a[" + overStr + "] = " + shortArr[overIdx]);
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);

      exCaught = false;
      try {
        System.out.println("a[" + overStr + "] = " + Array.getShort(shortArr, overIdx));
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);
    }

    System.out.println("testing int arrays...");
    int[] intArr = new int[100];
    for (int i = 0; i < 1000; i++) {
      int overIdx = MAX + rand.nextInt(MAX) + 1;
      String overStr = "" + overIdx;
      Assert.assertEquals("Overflowed index should equals \"OVERFLOW\" when converted to a string.",
          overStr, "OVERFLOW");

      // Test int array store:
      exCaught = false;
      try {
        intArr[overIdx] = rand.nextInt();
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);

      exCaught = false;
      try {
        Array.setInt(intArr, overIdx, rand.nextInt());
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);

      // Test int array load:
      exCaught = false;
      try {
        System.out.println("a[" + overStr + "] = " + intArr[overIdx]);
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);

      exCaught = false;
      try {
        System.out.println("a[" + overStr + "] = " + Array.getInt(intArr, overIdx));
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);
    }

    System.out.println("testing float arrays...");
    float[] floatArr = new float[100];
    for (int i = 0; i < 1000; i++) {
      int overIdx = MAX + rand.nextInt(MAX) + 1;
      String overStr = "" + overIdx;
      Assert.assertEquals("Overflowed index should equals \"OVERFLOW\" when converted to a string.",
          overStr, "OVERFLOW");

      // Test float array store:
      exCaught = false;
      try {
        floatArr[overIdx] = rand.nextFloat();
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);

      exCaught = false;
      try {
        Array.setFloat(floatArr, overIdx, rand.nextFloat());
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);

      // Test float array load:
      exCaught = false;
      try {
        System.out.println("a[" + overStr + "] = " + floatArr[overIdx]);
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);

      exCaught = false;
      try {
        System.out.println("a[" + overStr + "] = " + Array.getFloat(floatArr, overIdx));
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);
    }

    System.out.println("testing long arrays...");
    long[] longArr = new long[100];
    for (int i = 0; i < 1000; i++) {
      int overIdx = MAX + rand.nextInt(MAX) + 1;
      String overStr = "" + overIdx;
      Assert.assertEquals("Overflowed index should equals \"OVERFLOW\" when converted to a string.",
          overStr, "OVERFLOW");

      // Test long array store:
      exCaught = false;
      try {
        longArr[overIdx] = rand.nextLong();
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);

      exCaught = false;
      try {
        Array.setLong(longArr, overIdx, rand.nextLong());
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);

      // Test long array load:
      exCaught = false;
      try {
        System.out.println("a[" + overStr + "] = " + longArr[overIdx]);
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);

      exCaught = false;
      try {
        System.out.println("a[" + overStr + "] = " + Array.getLong(longArr, overIdx));
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);
    }

    System.out.println("testing double arrays...");
    double[] doubleArr = new double[100];
    for (int i = 0; i < 1000; i++) {
      int overIdx = MAX + rand.nextInt(MAX) + 1;
      String overStr = "" + overIdx;
      Assert.assertEquals("Overflowed index should equals \"OVERFLOW\" when converted to a string.",
          overStr, "OVERFLOW");

      // Test double array store:
      exCaught = false;
      try {
        doubleArr[overIdx] = rand.nextDouble();
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);

      exCaught = false;
      try {
        Array.setDouble(doubleArr, overIdx, rand.nextDouble());
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);

      // Test double array load:
      exCaught = false;
      try {
        System.out.println("a[" + overStr + "] = " + doubleArr[overIdx]);
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);

      exCaught = false;
      try {
        System.out.println("a[" + overStr + "] = " + Array.getDouble(doubleArr, overIdx));
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);
    }

    System.out.println("testing object arrays...");
    Object[] objArr = new Object[100];
    for (int i = 0; i < 1000; i++) {
      int overIdx = MAX + rand.nextInt(MAX) + 1;
      String overStr = "" + overIdx;
      Assert.assertEquals("Overflowed index should equals \"OVERFLOW\" when converted to a string.",
          overStr, "OVERFLOW");

      // Test object array store:
      exCaught = false;
      try {
        objArr[overIdx] = new Object();
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);

      exCaught = false;
      try {
        Array.set(objArr, overIdx, new Object());
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);

      // Test object array load:
      exCaught = false;
      try {
        System.out.println("a[" + overStr + "] = " + objArr[overIdx]);
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);

      exCaught = false;
      try {
        System.out.println("a[" + overStr + "] = " + Array.get(objArr, overIdx));
      } catch (CleartrackException e) {
        exCaught = true;
      }
      Assert.assertTrue("CleartrackException was expected on overflowed array access", exCaught);
    }
  }

  public void divideByZeroTest() {
    int[] intArr = new int[100];
    double[] doubleArr = new double[100];
    float[] floatArr = new float[100];
    char[] charArr = new char[100];
    boolean[] boolArr = new boolean[100];
    byte[] byteArr = new byte[100];
    short[] shortArr = new short[100];
    long[] longArr = new long[100];
    Object[] objArr = new Object[100];

    int i = rand.nextInt() / 0;

    // Test array loads on infinite indices...

    boolean exCaught = false;
    try {
      intArr[i] = rand.nextInt();
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite array index",
        exCaught);

    exCaught = false;
    try {
      boolArr[i] = rand.nextBoolean();
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite array index",
        exCaught);

    exCaught = false;
    try {
      byteArr[i] = (byte) rand.nextInt(Byte.MAX_VALUE);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite array index",
        exCaught);

    exCaught = false;
    try {
      charArr[i] = (char) rand.nextInt(Character.MAX_VALUE);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite array index",
        exCaught);

    exCaught = false;
    try {
      shortArr[i] = (short) rand.nextInt(Short.MAX_VALUE);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite array index",
        exCaught);

    exCaught = false;
    try {
      longArr[i] = rand.nextLong();
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite array index",
        exCaught);

    exCaught = false;
    try {
      floatArr[i] = rand.nextFloat();
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite array index",
        exCaught);

    exCaught = false;
    try {
      doubleArr[i] = rand.nextDouble();
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite array index",
        exCaught);

    exCaught = false;
    try {
      objArr[i] = new Object();
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite array index",
        exCaught);

    // Test array stores on infinite indices...

    exCaught = false;
    try {
      System.out.println("value: " + intArr[i]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite array index",
        exCaught);

    exCaught = false;
    try {
      System.out.println("value: " + boolArr[i]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite array index",
        exCaught);

    exCaught = false;
    try {
      System.out.println("value: " + byteArr[i]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite array index",
        exCaught);

    exCaught = false;
    try {
      System.out.println("value: " + charArr[i]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite array index",
        exCaught);

    exCaught = false;
    try {
      System.out.println("value: " + shortArr[i]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite array index",
        exCaught);

    exCaught = false;
    try {
      System.out.println("value: " + longArr[i]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite array index",
        exCaught);

    exCaught = false;
    try {
      System.out.println("value: " + floatArr[i]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite array index",
        exCaught);

    exCaught = false;
    try {
      System.out.println("value: " + doubleArr[i]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite array index",
        exCaught);

    exCaught = false;
    try {
      System.out.println("value: " + objArr[i]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite array index",
        exCaught);
  }

  @Test
  public void boolNewArrayTest() {
    boolean exCaught = false;
    try {
      System.out.println("arr ref = " + new boolean[rand.nextInt() / 0]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite newarray dimension",
        exCaught);

    exCaught = false;
    try {
      System.out.println("arr ref = " + new boolean[1][rand.nextInt() / 0]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite newarray dimension",
        exCaught);

    exCaught = false;
    try {
      System.out.println("arr ref = " + new boolean[1][1][rand.nextInt() / 0]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite newarray dimension",
        exCaught);
  }

  @Test
  public void byteNewArrayTest() {
    boolean exCaught = false;
    try {
      System.out.println("arr ref = " + new byte[rand.nextInt() / 0]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite newarray dimension",
        exCaught);

    exCaught = false;
    try {
      System.out.println("arr ref = " + new byte[1][rand.nextInt() / 0]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite newarray dimension",
        exCaught);

    exCaught = false;
    try {
      System.out.println("arr ref = " + new byte[1][1][rand.nextInt() / 0]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite newarray dimension",
        exCaught);
  }

  @Test
  public void shortNewArrayTest() {
    boolean exCaught = false;
    try {
      System.out.println("arr ref = " + new short[rand.nextInt() / 0]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite newarray dimension",
        exCaught);

    exCaught = false;
    try {
      System.out.println("arr ref = " + new short[1][rand.nextInt() / 0]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite newarray dimension",
        exCaught);

    exCaught = false;
    try {
      System.out.println("arr ref = " + new short[1][1][rand.nextInt() / 0]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite newarray dimension",
        exCaught);
  }

  @Test
  public void charNewArrayTest() {
    boolean exCaught = false;
    try {
      System.out.println("arr ref = " + ((Object) new char[rand.nextInt() / 0]));
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite newarray dimension",
        exCaught);

    exCaught = false;
    try {
      System.out.println("arr ref = " + new char[1][rand.nextInt() / 0]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite newarray dimension",
        exCaught);

    exCaught = false;
    try {
      System.out.println("arr ref = " + new char[1][1][rand.nextInt() / 0]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite newarray dimension",
        exCaught);
  }

  @Test
  public void intNewArrayTest() {
    boolean exCaught = false;
    try {
      System.out.println("arr ref = " + new int[rand.nextInt() / 0]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite newarray dimension",
        exCaught);

    exCaught = false;
    try {
      System.out.println("arr ref = " + new int[1][rand.nextInt() / 0]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite newarray dimension",
        exCaught);

    exCaught = false;
    try {
      System.out.println("arr ref = " + new int[1][1][rand.nextInt() / 0]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite newarray dimension",
        exCaught);
  }

  @Test
  public void longNewArrayTest() {
    boolean exCaught = false;
    try {
      System.out.println("arr ref = " + new long[rand.nextInt() / 0]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite newarray dimension",
        exCaught);

    exCaught = false;
    try {
      System.out.println("arr ref = " + new long[1][rand.nextInt() / 0]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite newarray dimension",
        exCaught);

    exCaught = false;
    try {
      System.out.println("arr ref = " + new long[1][1][rand.nextInt() / 0]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite newarray dimension",
        exCaught);
  }

  @Test
  public void floatNewArrayTest() {
    boolean exCaught = false;
    try {
      System.out.println("arr ref = " + new float[rand.nextInt() / 0]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite newarray dimension",
        exCaught);

    exCaught = false;
    try {
      System.out.println("arr ref = " + new float[1][rand.nextInt() / 0]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite newarray dimension",
        exCaught);

    exCaught = false;
    try {
      System.out.println("arr ref = " + new float[1][1][rand.nextInt() / 0]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite newarray dimension",
        exCaught);
  }

  @Test
  public void doubleNewArrayTest() {
    boolean exCaught = false;
    try {
      System.out.println("arr ref = " + new double[rand.nextInt() / 0]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite newarray dimension",
        exCaught);

    exCaught = false;
    try {
      System.out.println("arr ref = " + new double[1][rand.nextInt() / 0]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite newarray dimension",
        exCaught);

    exCaught = false;
    try {
      System.out.println("arr ref = " + new double[1][1][rand.nextInt() / 0]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite newarray dimension",
        exCaught);
  }

  @Test
  public void objectNewArrayTest() {
    boolean exCaught = false;
    try {
      System.out.println("arr ref = " + new Object[rand.nextInt() / 0]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite newarray dimension",
        exCaught);

    exCaught = false;
    try {
      System.out.println("arr ref = " + new Object[1][rand.nextInt() / 0]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite newarray dimension",
        exCaught);

    exCaught = false;
    try {
      System.out.println("arr ref = " + new Object[1][1][rand.nextInt() / 0]);
    } catch (CleartrackException e) {
      exCaught = true;
    }
    Assert.assertTrue("CleartrackException should have been caught on infinite newarray dimension",
        exCaught);
  }
}
