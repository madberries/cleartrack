package pac.test;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import pac.util.Ret;
import pac.util.TaintUtils;
import pac.wrap.ByteArrayTaint;
import pac.wrap.CharArrayTaint;

public class ReflectionTest {
  private static final int ITERS = 1000;

  private static Random rand = new Random();
  private static boolean varBool;
  private static byte varByte;
  private static short varShort;
  private static char varChar;
  private static int varInt;
  private static long varLong;
  private static float varFloat;
  private static double varDouble;

  private static boolean[] varBoolArr;
  private static byte[] varByteArr;
  private static short[] varShortArr;
  private static char[] varCharArr;
  private static int[] varIntArr;
  private static long[] varLongArr;
  private static float[] varFloatArr;
  private static double[] varDoubleArr;

  // TODO: Add tests for multidimensional arrays.

  class TestInvocationHandler implements InvocationHandler {
    private Object testImpl;

    public TestInvocationHandler(Object impl) {
      this.testImpl = impl;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      return method.invoke(testImpl, args);
    }

    //#if remove==false
    public Object invoke(Object arg0, Method arg1, Object[] arg2, Ret ret) throws Throwable {
      // This is never actually called. It's only here so that eclipse does not mark this as having
      // a compiler error.
      return null;
    }
    //#endif
  }

  interface ProxyTest {
    public int value();
  }

  class ProxyTestImpl implements ProxyTest {
    public int value() {
      return 200000000;
    }
  }

  @Test
  public void proxyClassTest() {
    ProxyTest t = (ProxyTest) Proxy.newProxyInstance(ProxyTest.class.getClassLoader(),
        new Class<?>[] {ProxyTest.class}, new TestInvocationHandler(new ProxyTestImpl()));
    System.out.println("value: " + t.value());
  }

  @Test
  public void reflectString() {
    try {
      String s, s1, s2, s3;

      // NOTE: this test is not really relevant since we are disregarding all null characters.

      char[] ca = new char[] {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j'};
      CharArrayTaint.taint(ca, 3, 6);
      s = String.class.getConstructor(char[].class).newInstance(ca);
      Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s, 0, 2));
      Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s, 3, 6));
      Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s, 7, 9));

      s2 = String.class.getConstructor(String.class).newInstance(TaintUtils.trust("0123456789"));
      s2 = TaintUtils.taint(s2, 2, 5);
      s = String.class.getConstructor(String.class).newInstance(s2);
      Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s, 0, 1));
      Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s, 2, 5));
      Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s, 6, 9));

      s1 = String.class.getConstructor(String.class).newInstance(TaintUtils.trust("0123456789"));
      s2 = String.class.getConstructor(String.class).newInstance(TaintUtils.trust("abcdefghij"));
      s1 = TaintUtils.taint(s1, 3, 7);
      s2 = TaintUtils.taint(s2, 2, 6);
      s3 = (String) String.class.getMethod("concat", String.class).invoke(s1, s2);
      Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s3, 0, 2));
      Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s3, 3, 7));
      Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s3, 8, 11));
      Assert.assertTrue("tainted region contains no taint marker",
          TaintUtils.isTainted(s3, 12, 16));
      Assert.assertFalse("untainted region contains taint marker",
          TaintUtils.isTainted(s3, 17, 19));

      s1 = String.class.getConstructor(String.class).newInstance(TaintUtils.trust("0123456789"));
      s1 = TaintUtils.taint(s1, 3, 5);
      s1 = TaintUtils.taint(s1, 7, 7);
      // 0123456789
      // ???xxx?x??

      s2 = (String) String.class.getMethod("substring", int.class, int.class).invoke(s1, 2, 9);
      // 2345678
      // ?xxx?x?
      Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s2, 0, 0));
      Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s2, 1, 3));
      Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s2, 4, 4));
      Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s2, 5, 5));
      Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s2, 6, 6));

      s3 = (String) String.class.getMethod("substring", int.class).invoke(s1, 4);
      // 456789
      // xx?x??
      Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s3, 0, 1));
      Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s3, 2, 2));
      Assert.assertTrue("tainted region contains no taint marker", TaintUtils.isTainted(s3, 3, 3));
      Assert.assertFalse("untainted region contains taint marker", TaintUtils.isTainted(s3, 4, 5));

      // char[] c = new char[] {'S', 'E', 'L', 'E', 'C', 'T'};
      // String str1 = String.class.getConstructor(String.class).newInstance("SELECT");
      // String str2 = String.class.getConstructor(char[].class).newInstance(c);
      // Assert.assertEquals("Strings were not properly derandomized",
      // String.class.getMethod("compareTo", String.class).invoke(str1, str2), 0);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void reflectArrays() {
    try {
      byte[] a1 = new byte[10];
      ByteArrayTaint.taint(a1, 3, 6);

      byte[] a2 = (byte[]) Arrays.class.getMethod("copyOfRange", byte[].class, int.class, int.class)
          .invoke(null, a1, 2, 10); // to index exclusive
      Assert.assertFalse(ByteArrayTaint.isTainted(a2, 0, 0));
      Assert.assertTrue(ByteArrayTaint.isTainted(a2, 1, 4));
      Assert.assertFalse(ByteArrayTaint.isTainted(a2, 5, 7));

      byte[] a3 = (byte[]) Arrays.class.getMethod("copyOfRange", byte[].class, int.class, int.class)
          .invoke(null, a1, 4, 10); // to index exclusive
      Assert.assertTrue(ByteArrayTaint.isTainted(a3, 0, 2));
      Assert.assertFalse(ByteArrayTaint.isTainted(a3, 3, 5));

      byte[] a4 = (byte[]) Arrays.class.getMethod("copyOfRange", byte[].class, int.class, int.class)
          .invoke(null, a1, 0, 6); // to index exclusive
      Assert.assertFalse(ByteArrayTaint.isTainted(a4, 0, 2));
      Assert.assertTrue(ByteArrayTaint.isTainted(a4, 3, 5));
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void reflectStringBuilder() {
    try {
      StringBuilder sb = StringBuilder.class.newInstance();
      sb.append(TaintUtils.trust("0123456789"));

      // char[]
      char[] ca1 = TaintUtils.trust("0123456789").toCharArray();
      CharArrayTaint.taint(ca1, 4, 7);
      StringBuilder.class.getMethod("append", char[].class).invoke(sb, ca1);

      Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 0, 13));
      Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 14, 17));
      Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 18, 19));

      // char[] int int
      char[] ca2 = TaintUtils.trust("01234567890123456789").toCharArray();
      CharArrayTaint.taint(ca2, 3, 7);
      CharArrayTaint.taint(ca2, 12, 18);
      StringBuilder.class.getMethod("append", char[].class, int.class, int.class).invoke(sb, ca2, 5,
          10);

      Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 18, 19));
      Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 20, 22));
      Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(sb, 23, 26));
      Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(sb, 27, 29));
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testFieldReflect() throws NoSuchFieldException, SecurityException,
      IllegalArgumentException, IllegalAccessException {
    Field field = ReflectionTest.class.getDeclaredField("varBool");
    field.set(null, true);
    Object val = field.get(null);
    Assert.assertTrue("varBool should have been set to true", ((Boolean) val).booleanValue());
    Assert.assertTrue("varBool should have been set to true", varBool);
    varBool = false;
    field.setBoolean(null, true);
    boolean bResult = field.getBoolean(null);
    Assert.assertTrue("varBool should have been set to true", bResult);
    Assert.assertTrue("varBool should have been set to true", varBool);

    field = ReflectionTest.class.getDeclaredField("varByte");
    for (int i = 0; i < ITERS; i++) {
      byte b = (byte) rand.nextInt(Byte.MAX_VALUE);
      field.set(null, b);
      val = field.get(null);
      Assert.assertEquals("varByte should have been set to " + b, ((Byte) val).byteValue(), b);
      Assert.assertEquals("varByte should have been set to " + b, varByte, b);

      b = (byte) rand.nextInt(Byte.MAX_VALUE);
      field.setByte(null, b);
      byte result = field.getByte(null);
      Assert.assertEquals("varByte should have been set to " + b, result, b);
      Assert.assertEquals("varByte should have been set to " + b, varByte, b);
    }

    field = ReflectionTest.class.getDeclaredField("varShort");
    for (int i = 0; i < ITERS; i++) {
      short s = (short) rand.nextInt(Short.MAX_VALUE);
      field.set(null, s);
      val = field.get(null);
      Assert.assertEquals("varShort should have been set to " + s, ((Short) val).shortValue(), s);
      Assert.assertEquals("varShort should have been set to " + s, varShort, s);

      s = (short) rand.nextInt(Short.MAX_VALUE);
      field.setShort(null, s);
      short result = field.getShort(null);
      Assert.assertEquals("varShort should have been set to " + s, result, s);
      Assert.assertEquals("varShort should have been set to " + s, varShort, s);
    }

    field = ReflectionTest.class.getDeclaredField("varChar");
    for (int i = 0; i < ITERS; i++) {
      char c = (char) rand.nextInt(Character.MAX_VALUE);
      field.set(null, c);
      val = field.get(null);
      Assert.assertEquals("varChar should have been set to " + c, ((Character) val).charValue(), c);
      Assert.assertEquals("varChar should have been set to " + c, varChar, c);

      c = (char) rand.nextInt(Character.MAX_VALUE);
      field.setChar(null, c);
      char result = field.getChar(null);
      Assert.assertEquals("varChar should have been set to " + c, result, c);
      Assert.assertEquals("varChar should have been set to " + c, varChar, c);
    }

    field = ReflectionTest.class.getDeclaredField("varInt");
    for (int i = 0; i < ITERS; i++) {
      int j = rand.nextInt();
      field.set(null, j);
      val = field.get(null);
      Assert.assertEquals("varInt should have been set to " + j, ((Integer) val).intValue(), j);
      Assert.assertEquals("varInt should have been set to " + j, varInt, j);

      j = rand.nextInt();
      field.setInt(null, j);
      int result = field.getInt(null);
      Assert.assertEquals("varInt should have been set to " + j, result, j);
      Assert.assertEquals("varInt should have been set to " + j, varInt, j);
    }

    field = ReflectionTest.class.getDeclaredField("varLong");
    for (int i = 0; i < ITERS; i++) {
      long l = rand.nextLong();
      field.set(null, l);
      val = field.get(null);
      Assert.assertEquals("varLong should have been set to " + l, ((Long) val).longValue(), l);
      Assert.assertEquals("varLong should have been set to " + l, varLong, l);

      l = rand.nextLong();
      field.setLong(null, l);
      long result = field.getLong(null);
      Assert.assertEquals("varLong should have been set to " + l, result, l);
      Assert.assertEquals("varLong should have been set to " + l, varLong, l);
    }

    field = ReflectionTest.class.getDeclaredField("varFloat");
    for (int i = 0; i < ITERS; i++) {
      float f = rand.nextFloat();
      field.set(null, f);
      val = field.get(null);
      Assert.assertEquals("varFloat should have been set to " + f, ((Float) val).floatValue(), f,
          0);
      Assert.assertEquals("varFloat should have been set to " + f, varFloat, f, 0);

      f = rand.nextFloat();
      field.setFloat(null, f);
      float result = field.getFloat(null);
      Assert.assertEquals("varFloat should have been set to " + f, result, f, 0);
      Assert.assertEquals("varFloat should have been set to " + f, varFloat, f, 0);
    }

    field = ReflectionTest.class.getDeclaredField("varDouble");
    for (int i = 0; i < ITERS; i++) {
      double d = rand.nextDouble();
      field.set(null, d);
      val = field.get(null);
      Assert.assertEquals("varDouble should have been set to " + d, ((Double) val).doubleValue(), d,
          0);
      Assert.assertEquals("varDouble should have been set to " + d, varDouble, d, 0);

      d = rand.nextDouble();
      field.setDouble(null, d);
      double result = field.getDouble(null);
      Assert.assertEquals("varDouble should have been set to " + d, result, d, 0);
      Assert.assertEquals("varDouble should have been set to " + d, varDouble, d, 0);
    }
  }

  private boolean areEqual(boolean[] arr1, boolean[] arr2) {
    if (arr1.length != arr2.length)
      return false;
    for (int i = 0; i < arr1.length; i++) {
      if (arr1[i] != arr2[i])
        return false;
    }
    if (Array.getLength(arr1) != Array.getLength(arr2))
      return false;
    int length = Array.getLength(arr1);
    for (int i = 0; i < length; i++) {
      if (Array.getBoolean(arr1, i) != Array.getBoolean(arr2, i))
        return false;
    }
    return true;
  }

  private boolean areEqual(byte[] arr1, byte[] arr2) {
    if (arr1.length != arr2.length)
      return false;
    for (int i = 0; i < arr1.length; i++) {
      if (arr1[i] != arr2[i])
        return false;
    }
    if (Array.getLength(arr1) != Array.getLength(arr2))
      return false;
    int length = Array.getLength(arr1);
    for (int i = 0; i < length; i++) {
      if (Array.getByte(arr1, i) != Array.getByte(arr2, i))
        return false;
    }
    return true;
  }

  private boolean areEqual(short[] arr1, short[] arr2) {
    if (arr1.length != arr2.length)
      return false;
    for (int i = 0; i < arr1.length; i++) {
      if (arr1[i] != arr2[i])
        return false;
    }
    if (Array.getLength(arr1) != Array.getLength(arr2))
      return false;
    int length = Array.getLength(arr1);
    for (int i = 0; i < length; i++) {
      if (Array.getShort(arr1, i) != Array.getShort(arr2, i))
        return false;
    }
    return true;
  }

  private boolean areEqual(char[] arr1, char[] arr2) {
    if (arr1.length != arr2.length)
      return false;
    for (int i = 0; i < arr1.length; i++) {
      if (arr1[i] != arr2[i])
        return false;
    }
    if (Array.getLength(arr1) != Array.getLength(arr2))
      return false;
    int length = Array.getLength(arr1);
    for (int i = 0; i < length; i++) {
      if (Array.getChar(arr1, i) != Array.getChar(arr2, i))
        return false;
    }
    return true;
  }

  private boolean areEqual(int[] arr1, int[] arr2) {
    if (arr1.length != arr2.length)
      return false;
    for (int i = 0; i < arr1.length; i++) {
      if (arr1[i] != arr2[i])
        return false;
    }
    if (Array.getLength(arr1) != Array.getLength(arr2))
      return false;
    int length = Array.getLength(arr1);
    for (int i = 0; i < length; i++) {
      if (Array.getInt(arr1, i) != Array.getInt(arr2, i))
        return false;
    }
    return true;
  }

  private boolean areEqual(long[] arr1, long[] arr2) {
    if (arr1.length != arr2.length)
      return false;
    for (int i = 0; i < arr1.length; i++) {
      if (arr1[i] != arr2[i])
        return false;
    }
    if (Array.getLength(arr1) != Array.getLength(arr2))
      return false;
    int length = Array.getLength(arr1);
    for (int i = 0; i < length; i++) {
      if (Array.getLong(arr1, i) != Array.getLong(arr2, i))
        return false;
    }
    return true;
  }

  private boolean areEqual(float[] arr1, float[] arr2) {
    if (arr1.length != arr2.length)
      return false;
    for (int i = 0; i < arr1.length; i++) {
      if (arr1[i] != arr2[i])
        return false;
    }
    if (Array.getLength(arr1) != Array.getLength(arr2))
      return false;
    int length = Array.getLength(arr1);
    for (int i = 0; i < length; i++) {
      if (Array.getFloat(arr1, i) != Array.getFloat(arr2, i))
        return false;
    }
    return true;
  }

  private boolean areEqual(double[] arr1, double[] arr2) {
    if (arr1.length != arr2.length)
      return false;
    for (int i = 0; i < arr1.length; i++) {
      if (arr1[i] != arr2[i])
        return false;
    }
    if (Array.getLength(arr1) != Array.getLength(arr2))
      return false;
    int length = Array.getLength(arr1);
    for (int i = 0; i < length; i++) {
      if (Array.getDouble(arr1, i) != Array.getDouble(arr2, i))
        return false;
    }
    return true;
  }

  @Test
  public void testArrayFieldReflect() throws NoSuchFieldException, SecurityException,
      IllegalArgumentException, IllegalAccessException {
    Field field = ReflectionTest.class.getDeclaredField("varBoolArr");
    boolean[] bArray = new boolean[] {true, false, true, false, true};
    field.set(null, bArray);
    boolean[] bResult = (boolean[]) field.get(null);
    Assert.assertTrue("boolean arrays should be equal", areEqual(bArray, bResult));
    Assert.assertTrue("boolean arrays should be equal", areEqual(bArray, varBoolArr));

    field = ReflectionTest.class.getDeclaredField("varByteArr");
    byte[] byArray = new byte[] {0, 1, 2, 3, 4, 5};
    field.set(null, byArray);
    byte[] byResult = (byte[]) field.get(null);
    Assert.assertTrue("byte arrays should be equal", areEqual(byArray, byResult));
    Assert.assertTrue("byte arrays should be equal", areEqual(byArray, varByteArr));

    field = ReflectionTest.class.getDeclaredField("varShortArr");
    short[] sArray = new short[] {0, 1, 2, 3, 4, 5};
    field.set(null, sArray);
    short[] sResult = (short[]) field.get(null);
    Assert.assertTrue("short arrays should be equal", areEqual(sArray, sResult));
    Assert.assertTrue("short arrays should be equal", areEqual(sArray, varShortArr));

    field = ReflectionTest.class.getDeclaredField("varCharArr");
    char[] cArray = new char[] {'a', 'b', 'c', 'd', 'e'};
    field.set(null, cArray);
    char[] cResult = (char[]) field.get(null);
    Assert.assertTrue("char arrays should be equal", areEqual(cArray, cResult));
    Assert.assertTrue("char arrays should be equal", areEqual(cArray, varCharArr));

    field = ReflectionTest.class.getDeclaredField("varIntArr");
    int[] iArray = new int[] {0, 1, 2, 3, 4, 5};
    field.set(null, iArray);
    int[] iResult = (int[]) field.get(null);
    Assert.assertTrue("int arrays should be equal", areEqual(iArray, iResult));
    Assert.assertTrue("int arrays should be equal", areEqual(iArray, varIntArr));

    field = ReflectionTest.class.getDeclaredField("varLongArr");
    long[] lArray = new long[] {0, 1, 2, 3, 4, 5};
    field.set(null, lArray);
    long[] lResult = (long[]) field.get(null);
    Assert.assertTrue("long arrays should be equal", areEqual(lArray, lResult));
    Assert.assertTrue("long arrays should be equal", areEqual(lArray, varLongArr));

    field = ReflectionTest.class.getDeclaredField("varFloatArr");
    float[] fArray = new float[] {0, 1, 2, 3, 4, 5};
    field.set(null, fArray);
    float[] fResult = (float[]) field.get(null);
    Assert.assertTrue("float arrays should be equal", areEqual(fArray, fResult));
    Assert.assertTrue("float arrays should be equal", areEqual(fArray, varFloatArr));

    field = ReflectionTest.class.getDeclaredField("varDoubleArr");
    double[] dArray = new double[] {0, 1, 2, 3, 4, 5};
    field.set(null, dArray);
    double[] dResult = (double[]) field.get(null);
    Assert.assertTrue("double arrays should be equal", areEqual(dArray, dResult));
    Assert.assertTrue("double arrays should be equal", areEqual(dArray, varDoubleArr));
  }

  public boolean fooBool(String[] args) {
    return false;
  }

  public byte fooByte(String[] args) {
    return 0;
  }

  public short fooShort(String[] args) {
    return 0;
  }

  public char fooChar(String[] args) {
    return '0';
  }

  public int fooInt(String[] args) {
    return 0;
  }

  public float fooFloat(String[] args) {
    return 0.0f;
  }

  public long fooLong(String[] args) {
    return 0;
  }

  public double fooDouble(String[] args) {
    return 0.0;
  }

  public boolean[] fooBoolArray(String[] args) {
    return new boolean[] {false};
  }

  public byte[] fooByteArray(String[] args) {
    return new byte[] {0};
  }

  public short[] fooShortArray(String[] args) {
    return new short[] {0};
  }

  public char[] fooCharArray(String[] args) {
    return new char[] {'0'};
  }

  public int[] fooIntArray(String[] args) {
    return new int[] {0};
  }

  public float[] fooFloatArray(String[] args) {
    return new float[] {0.0f};
  }

  public long[] fooLongArray(String[] args) {
    return new long[] {0};
  }

  public double[] fooDoubleArray(String[] args) {
    return new double[] {0.0};
  }

  public Object reflectMethod(String methodName) throws NoSuchMethodException, SecurityException,
      IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    Method method = ReflectionTest.class.getMethod(methodName, new Class<?>[] {String[].class});
    return method.invoke(this, new Object[] {new String[0]});
  }

  @Test
  public void reflectMethodPrimitiveTest() throws NoSuchMethodException, SecurityException,
      IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    // Make sure that primitive return types return from reflected methods are compatible with their
    // wrapped type.
    Boolean boolResult = (Boolean) reflectMethod("fooBool");
    Byte byteResult = (Byte) reflectMethod("fooByte");
    Short shortResult = (Short) reflectMethod("fooShort");
    Character charResult = (Character) reflectMethod("fooChar");
    Integer intResult = (Integer) reflectMethod("fooInt");
    Long longResult = (Long) reflectMethod("fooLong");
    Float floatResult = (Float) reflectMethod("fooFloat");
    Double doubleResult = (Double) reflectMethod("fooDouble");
    System.out.printf("primitive reflection results: %s %d %d %c %d %d %f %f\n", boolResult,
        byteResult, shortResult, charResult, intResult, longResult, floatResult, doubleResult);
  }

  @Test
  public void reflectMethodPrimitiveArrayTest() throws NoSuchMethodException, SecurityException,
      IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    // Make sure that primitive return types return from reflected methods are compatible with their
    // wrapped type.
    boolean[] boolResult = (boolean[]) reflectMethod("fooBoolArray");
    byte[] byteResult = (byte[]) reflectMethod("fooByteArray");
    short[] shortResult = (short[]) reflectMethod("fooShortArray");
    char[] charResult = (char[]) reflectMethod("fooCharArray");
    int[] intResult = (int[]) reflectMethod("fooIntArray");
    long[] longResult = (long[]) reflectMethod("fooLongArray");
    float[] floatResult = (float[]) reflectMethod("fooFloatArray");
    double[] doubleResult = (double[]) reflectMethod("fooDoubleArray");
    System.out.printf(
        "primitive array reflection results: {%s} {%d} {%d} {%c} {%d} {%d} {%f} {%f}\n",
        boolResult[0], byteResult[0], shortResult[0], charResult[0], intResult[0], longResult[0],
        floatResult[0], doubleResult[0]);
  }
}
