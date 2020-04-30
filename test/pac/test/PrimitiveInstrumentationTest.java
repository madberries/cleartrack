package pac.test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import pac.config.CleartrackException;
import pac.util.TaintUtils;
import pac.wrap.IntArrayTaint;

public class PrimitiveInstrumentationTest {
    public static final boolean TEST_BOOL = true;
    public static final byte TEST_BYTE = 5;
    public static final short TEST_SHORT = 5;
    public static final char TEST_CHAR = 'a';
    public static final int TEST_INT = 5;
    public static final float TEST_FLOAT = 3.1415f;
    public static final long TEST_LONG = 5;
    public static final double TEST_DOUBLE = 3.1415d;
    public static final String TEST_STR = "constant string";
    public static final char[] TEST_CHAR_ARR = new char[] { 'a', 'b', 'c' };

    Random rand = new Random();
    static final int total_ops = 10000;

    @Test
    public void stackFrameTest() {
        // ASM does not compute stack frames correctly, when merging between
        // dangerous types and their respective subtype.
        Integer x;
        if (rand.nextBoolean())
            x = Integer.valueOf(5); // CleartrackInteger
        else
            x = Integer.valueOf("5"); // Integer
        System.out.println("result: " + x.intValue()); // VERIFY ERROR on intValue()!
    }

    private void testAllConstant(boolean boolVal, byte byteVal, short shortVal, char charVal, int intVal,
                                 float floatVal, long longVal, double doubleVal) {
        Assert.assertTrue("constant boolean field has incorrect value", boolVal == true);
        Assert.assertTrue("constant byte field has incorrect value", byteVal == 5);
        Assert.assertTrue("constant short field has incorrect value", shortVal == 5);
        Assert.assertTrue("constant int field has incorrect value", charVal == 'a');
        Assert.assertTrue("constant long field has incorrect value", intVal == 5);
        Assert.assertTrue("constant float field has incorrect value", floatVal == 3.1415f);
        Assert.assertTrue("constant double field has incorrect value", longVal == 5);
        Assert.assertTrue("constant char field has incorrect value", doubleVal == 3.1415);
        Assert.assertEquals("constant String field has incorrect value", TEST_STR, "constant string");

        Assert.assertTrue("constant boolean field should be trusted", TaintUtils.isTrusted(boolVal));
        Assert.assertTrue("constant byte field should be trusted", TaintUtils.isTrusted(byteVal));
        Assert.assertTrue("constant short field should be trusted", TaintUtils.isTrusted(shortVal));
        Assert.assertTrue("constant char field should be trusted", TaintUtils.isTrusted(charVal));
        Assert.assertTrue("constant int field should be trusted", TaintUtils.isTrusted(intVal));
        Assert.assertTrue("constant long field should be trusted", TaintUtils.isTrusted(longVal));
        Assert.assertTrue("constant float field should be trusted", TaintUtils.isTrusted(floatVal));
        Assert.assertTrue("constant double field should be trusted", TaintUtils.isTrusted(doubleVal));
    }

    @Test
    public void testConstants()
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        testAllConstant(TEST_BOOL, TEST_BYTE, TEST_SHORT, TEST_CHAR, TEST_INT, TEST_FLOAT, TEST_LONG, TEST_DOUBLE);

        boolean boolVal = PrimitiveInstrumentationTest.class.getField("TEST_BOOL").getBoolean(null);
        byte byteVal = PrimitiveInstrumentationTest.class.getField("TEST_BYTE").getByte(null);
        short shortVal = PrimitiveInstrumentationTest.class.getField("TEST_SHORT").getShort(null);
        int intVal = PrimitiveInstrumentationTest.class.getField("TEST_INT").getInt(null);
        char charVal = PrimitiveInstrumentationTest.class.getField("TEST_CHAR").getChar(null);
        float floatVal = PrimitiveInstrumentationTest.class.getField("TEST_FLOAT").getFloat(null);
        long longVal = PrimitiveInstrumentationTest.class.getField("TEST_LONG").getLong(null);
        double doubleVal = PrimitiveInstrumentationTest.class.getField("TEST_DOUBLE").getDouble(null);
        testAllConstant(boolVal, byteVal, shortVal, charVal, intVal, floatVal, longVal, doubleVal);

        boolean boolVal2 = (Boolean) PrimitiveInstrumentationTest.class.getField("TEST_BOOL").get(null);
        byte byteVal2 = (Byte) PrimitiveInstrumentationTest.class.getField("TEST_BYTE").get(null);
        short shortVal2 = (Short) PrimitiveInstrumentationTest.class.getField("TEST_SHORT").get(null);
        int intVal2 = (Integer) PrimitiveInstrumentationTest.class.getField("TEST_INT").get(null);
        char charVal2 = (Character) PrimitiveInstrumentationTest.class.getField("TEST_CHAR").get(null);
        float floatVal2 = (Float) PrimitiveInstrumentationTest.class.getField("TEST_FLOAT").get(null);
        long longVal2 = (Long) PrimitiveInstrumentationTest.class.getField("TEST_LONG").get(null);
        double doubleVal2 = (Double) PrimitiveInstrumentationTest.class.getField("TEST_DOUBLE").get(null);
        testAllConstant(boolVal2, byteVal2, shortVal2, charVal2, intVal2, floatVal2, longVal2, doubleVal2);
    }

    @Test
    public void primitiveClassTest() {
        System.out.println("Boolean class: " + Boolean.class);
        System.out.println("boolean class: " + boolean.class);
    }

    @Test
    public void booleanTest() {
        Boolean b = false;
        String s = b.toString();
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));

        s = Boolean.toString(true);
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));

        s = String.valueOf(false);
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));
    }

    @Test
    public void byteTest() {
        Byte b = 33;
        String s = b.toString();
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));

        s = Byte.toString((byte) 0xaa);
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));

        // TODO figure out why this calls StringOf(I) instead
        s = String.valueOf((byte) 0x98);
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));
    }

    @Test
    public void characterTest() {
        Character c = 'A';
        String s = c.toString();
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));

        s = Character.toString('B');
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));

        s = String.valueOf('B');
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));
    }

    @Test
    public void doubleTest() {
        Double d = 0.12345d;
        String s = d.toString();
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));

        s = Double.toString(0.123456d);
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));

        s = Double.toHexString(0.123456d);
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));

        s = String.valueOf(0.123456d);
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));
    }

    @Test
    public void floatTest() {
        Float f = 0.123f;
        String s = f.toString();
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));

        s = Float.toString(0.12f);
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));

        s = Float.toHexString(0.12f);
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));

        s = String.valueOf(0.12f);
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));
    }

    @Test
    public void integerTest() {
        Integer i = 123;
        String s = i.toString();
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));

        s = Integer.toString(1234);
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));

        s = Integer.toBinaryString(1234);
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));

        s = Integer.toHexString(1234);
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));

        s = Integer.toOctalString(1234);
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));

        s = String.valueOf(1234);
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));

    }

    @Test
    public void longTest() {
        Long l = 123456L;
        String s = l.toString();
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));

        s = Long.toString(12345L);
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));

        s = Long.toString(12345L, 5);
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));

        s = Long.toString(12345L, 10);
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));

        s = Long.toBinaryString(12345L);
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));

        s = Long.toHexString(12345L);
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));

        s = Long.toOctalString(12345L);
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));

        s = String.valueOf(12345L);
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));
    }

    @Test
    public void shortTest() {
        Short sh = 123;
        String s = sh.toString();
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));

        s = Short.toString((short) 12);
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));

        s = String.valueOf((short) 12);
        Assert.assertTrue("string based on primitive data was not trusted", TaintUtils.isTrusted(s));
    }

    @Test
    public void complicatedPrimitiveTest() {
        String[] numStrs = { "0", "1", "2", "3", "4", "5" };
        int[] nums = new int[numStrs.length];
        for (int i = 0; i < nums.length; i++) {
            nums[i] = Integer.parseInt(numStrs[i]);
            if (i % 2 == 0)
                IntArrayTaint.taint(nums, i, i);
        }

        for (int x = 0; x < 100; x++) {
            int i = rand.nextInt(numStrs.length);
            int num = (i < 3) ? ((i < 1) ? nums[0] : ((i == 2) ? nums[2] : nums[1]))
                    : ((i > 4) ? nums[5] : ((i == 4) ? nums[4] : nums[3]));
            Integer iObj = new Integer(num);
            Assert.assertEquals("primitive value was not properly trusted/tainted",
                                TaintUtils.isTrusted(iObj.intValue()), i % 2 == 1);
        }
    }

    @Test
    public void bitwiseResetTest() {
        byte[] bytes;
        short[] shorts;

        bytes = new byte[] { 12, 97, -32, 8, 81, -23 };
        for (int i = 0; i < bytes.length; i += 2) {
            short s = (short) (((bytes[i] & 0xff) << 8) | (bytes[i + 1] & 0xff));
            Assert.assertFalse("truncations on bitwise expressions should not overflow/underflow",
                               TaintUtils.isOverflowSet(s) || TaintUtils.isUnderflowSet(s));
        }

        shorts = new short[] { -245, -300, -978, -129, -437, -10000 };
        for (int i = 0; i < shorts.length; i++) {
            byte b = (byte) shorts[i];
            Assert.assertTrue("truncations on arithmetic expressions should overflow/underflow",
                              TaintUtils.isOverflowSet(b) || TaintUtils.isUnderflowSet(b));
        }

        bytes = new byte[] { -67, -97, -56, -82, -49, -100 };
        for (int i = 0; i < bytes.length; i += 2) {
            byte b = (byte) (bytes[i] + bytes[i + 1]);
            Assert.assertTrue("truncations on arithmetic expressions should not overflow/underflow",
                              TaintUtils.isOverflowSet(b) || TaintUtils.isUnderflowSet(b));
        }

        bytes = new byte[] { -67, -97, -56, -82, -49, -100 };
        for (int i = 0; i < bytes.length; i += 2) {
            byte b = (byte) (bytes[i] * bytes[i + 1]);
            Assert.assertTrue("truncations on arithmetic expressions should not overflow/underflow",
                              TaintUtils.isOverflowSet(b) || TaintUtils.isUnderflowSet(b));
        }
        bytes = new byte[] { 67, 97, 56, 82, 49, 100 };
        for (int i = 0; i < bytes.length; i += 2) {
            byte b = (byte) (bytes[i] + bytes[i + 1]);
            Assert.assertTrue("truncations on arithmetic expressions should not overflow/underflow",
                              TaintUtils.isOverflowSet(b) || TaintUtils.isUnderflowSet(b));
        }
    }

    @Test
    public void bigIntegerTest() {
        BigInteger i, j, k;
        int result;
        String op1 = "456";
        String op2 = "100";

        TaintUtils.trust(op1);
        TaintUtils.trust(op2);
        i = new BigInteger(op1);
        j = new BigInteger(op2);
        k = i.add(j);
        result = k.intValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTrusted(result));

        TaintUtils.trust(op1);
        TaintUtils.taint(op2);
        i = new BigInteger(op1);
        j = new BigInteger(op2);
        k = i.add(j);
        result = k.intValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTainted(result));

        TaintUtils.taint(op1);
        TaintUtils.trust(op2);
        i = new BigInteger(op1);
        j = new BigInteger(op2);
        k = i.add(j);
        result = k.intValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTainted(result));

        TaintUtils.taint(op1);
        TaintUtils.taint(op2);
        i = new BigInteger(op1);
        j = new BigInteger(op2);
        k = i.add(j);
        result = k.intValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTainted(result));

        TaintUtils.trust(op1);
        TaintUtils.trust(op2);
        i = new BigInteger(op1);
        j = new BigInteger(op2);
        k = i.subtract(j);
        result = k.intValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTrusted(result));

        TaintUtils.trust(op1);
        TaintUtils.taint(op2);
        i = new BigInteger(op1);
        j = new BigInteger(op2);
        k = i.subtract(j);
        result = k.intValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTainted(result));

        TaintUtils.taint(op1);
        TaintUtils.trust(op2);
        i = new BigInteger(op1);
        j = new BigInteger(op2);
        k = i.subtract(j);
        result = k.intValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTainted(result));

        TaintUtils.taint(op1);
        TaintUtils.taint(op2);
        i = new BigInteger(op1);
        j = new BigInteger(op2);
        k = i.subtract(j);
        result = k.intValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTainted(result));

        TaintUtils.trust(op1);
        TaintUtils.trust(op2);
        i = new BigInteger(op1);
        j = new BigInteger(op2);
        k = i.multiply(j);
        result = k.intValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTrusted(result));

        TaintUtils.trust(op1);
        TaintUtils.taint(op2);
        i = new BigInteger(op1);
        j = new BigInteger(op2);
        k = i.multiply(j);
        result = k.intValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTainted(result));

        TaintUtils.taint(op1);
        TaintUtils.trust(op2);
        i = new BigInteger(op1);
        j = new BigInteger(op2);
        k = i.multiply(j);
        result = k.intValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTainted(result));

        TaintUtils.taint(op1);
        TaintUtils.taint(op2);
        i = new BigInteger(op1);
        j = new BigInteger(op2);
        k = i.multiply(j);
        result = k.intValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTainted(result));

        TaintUtils.trust(op1);
        TaintUtils.trust(op2);
        i = new BigInteger(op1);
        j = new BigInteger(op2);
        k = i.divide(j);
        result = k.intValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTrusted(result));

        TaintUtils.trust(op1);
        TaintUtils.taint(op2);
        i = new BigInteger(op1);
        j = new BigInteger(op2);
        k = i.divide(j);
        result = k.intValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTainted(result));

        TaintUtils.taint(op1);
        TaintUtils.trust(op2);
        i = new BigInteger(op1);
        j = new BigInteger(op2);
        k = i.divide(j);
        result = k.intValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTainted(result));

        TaintUtils.taint(op1);
        TaintUtils.taint(op2);
        i = new BigInteger(op1);
        j = new BigInteger(op2);
        k = i.divide(j);
        result = k.intValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTainted(result));

        op2 = "2";
        TaintUtils.trust(op1);
        TaintUtils.trust(op2);
        i = new BigInteger(op1);
        k = i.pow(Integer.parseInt(op2));
        result = k.intValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTrusted(result));

        TaintUtils.trust(op1);
        TaintUtils.taint(op2);
        i = new BigInteger(op1);
        k = i.pow(Integer.parseInt(op2));
        result = k.intValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTainted(result));

        TaintUtils.taint(op1);
        TaintUtils.trust(op2);
        i = new BigInteger(op1);
        k = i.pow(Integer.parseInt(op2));
        result = k.intValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTainted(result));

        TaintUtils.taint(op1);
        TaintUtils.taint(op2);
        i = new BigInteger(op1);
        k = i.pow(Integer.parseInt(op2));
        result = k.intValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTainted(result));
    }

    @Test
    public void bigDecimalTest() {
        BigDecimal i, j, k;
        double result;
        String op1 = "456.0";
        String op2 = "100.0";

        TaintUtils.trust(op1);
        TaintUtils.trust(op2);
        i = new BigDecimal(op1);
        j = new BigDecimal(op2);
        k = i.add(j);
        result = k.doubleValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTrusted(result));

        TaintUtils.trust(op1);
        TaintUtils.taint(op2);
        i = new BigDecimal(op1);
        j = new BigDecimal(op2);
        k = i.add(j);
        result = k.doubleValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTainted(result));

        TaintUtils.taint(op1);
        TaintUtils.trust(op2);
        i = new BigDecimal(op1);
        j = new BigDecimal(op2);
        k = i.add(j);
        result = k.doubleValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTainted(result));

        TaintUtils.taint(op1);
        TaintUtils.taint(op2);
        i = new BigDecimal(op1);
        j = new BigDecimal(op2);
        k = i.add(j);
        result = k.doubleValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTainted(result));

        TaintUtils.trust(op1);
        TaintUtils.trust(op2);
        i = new BigDecimal(op1);
        j = new BigDecimal(op2);
        k = i.subtract(j);
        result = k.doubleValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTrusted(result));

        TaintUtils.trust(op1);
        TaintUtils.taint(op2);
        i = new BigDecimal(op1);
        j = new BigDecimal(op2);
        k = i.subtract(j);
        result = k.doubleValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTainted(result));

        TaintUtils.taint(op1);
        TaintUtils.trust(op2);
        i = new BigDecimal(op1);
        j = new BigDecimal(op2);
        k = i.subtract(j);
        result = k.doubleValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTainted(result));

        TaintUtils.taint(op1);
        TaintUtils.taint(op2);
        i = new BigDecimal(op1);
        j = new BigDecimal(op2);
        k = i.subtract(j);
        result = k.doubleValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTainted(result));

        TaintUtils.trust(op1);
        TaintUtils.trust(op2);
        i = new BigDecimal(op1);
        j = new BigDecimal(op2);
        k = i.multiply(j);
        result = k.doubleValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTrusted(result));

        TaintUtils.trust(op1);
        TaintUtils.taint(op2);
        i = new BigDecimal(op1);
        j = new BigDecimal(op2);
        k = i.multiply(j);
        result = k.doubleValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTainted(result));

        TaintUtils.taint(op1);
        TaintUtils.trust(op2);
        i = new BigDecimal(op1);
        j = new BigDecimal(op2);
        k = i.multiply(j);
        result = k.doubleValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTainted(result));

        TaintUtils.taint(op1);
        TaintUtils.taint(op2);
        i = new BigDecimal(op1);
        j = new BigDecimal(op2);
        k = i.multiply(j);
        result = k.doubleValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTainted(result));

        TaintUtils.trust(op1);
        TaintUtils.trust(op2);
        i = new BigDecimal(op1);
        j = new BigDecimal(op2);
        k = i.divide(j);
        result = k.doubleValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTrusted(result));

        TaintUtils.trust(op1);
        TaintUtils.taint(op2);
        i = new BigDecimal(op1);
        j = new BigDecimal(op2);
        k = i.divide(j);
        result = k.doubleValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTainted(result));

        TaintUtils.taint(op1);
        TaintUtils.trust(op2);
        i = new BigDecimal(op1);
        j = new BigDecimal(op2);
        k = i.divide(j);
        result = k.doubleValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTainted(result));

        TaintUtils.taint(op1);
        TaintUtils.taint(op2);
        i = new BigDecimal(op1);
        j = new BigDecimal(op2);
        k = i.divide(j);
        result = k.doubleValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTainted(result));

        op2 = "2";
        TaintUtils.trust(op1);
        TaintUtils.trust(op2);
        i = new BigDecimal(op1);
        k = i.pow(Integer.parseInt(op2));
        result = k.intValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTrusted(result));

        TaintUtils.trust(op1);
        TaintUtils.taint(op2);
        i = new BigDecimal(op1);
        k = i.pow(Integer.parseInt(op2));
        result = k.intValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTainted(result));

        TaintUtils.taint(op1);
        TaintUtils.trust(op2);
        i = new BigDecimal(op1);
        k = i.pow(Integer.parseInt(op2));
        result = k.intValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTainted(result));

        TaintUtils.taint(op1);
        TaintUtils.taint(op2);
        i = new BigDecimal(op1);
        k = i.pow(Integer.parseInt(op2));
        result = k.intValue();
        Assert.assertTrue("result should be trusted", TaintUtils.isTainted(result));
    }

    private void checkOperation(double result, boolean trusted) {
        if (trusted)
            Assert.assertTrue("result of an operation on trusted values should be trusted",
                              TaintUtils.isTrusted(result));
        else
            Assert.assertTrue("result of an operation on tainted values should be tainted",
                              TaintUtils.isTainted(result));
    }

    public void mathTest(boolean trusted) {
        String num = "" + (rand.nextInt(10) + 1);
        if (trusted)
            TaintUtils.trust(num);
        else
            TaintUtils.taint(num);

        int i = Integer.parseInt(num);
        double d = Double.parseDouble(num);
        float f = Float.parseFloat(num);
        long l = Long.parseLong(num);

        checkOperation(Math.abs(i), trusted);
        checkOperation(Math.abs(d), trusted);
        checkOperation(Math.abs(f), trusted);
        checkOperation(Math.abs(l), trusted);
        checkOperation(Math.acos(d), trusted);
        checkOperation(Math.asin(d), trusted);
        checkOperation(Math.atan(d), trusted);
        checkOperation(Math.atan2(d, d), trusted);
        checkOperation(Math.sin(d), trusted);
        checkOperation(Math.cos(d), trusted);
        checkOperation(Math.tan(d), trusted);
        checkOperation(Math.sinh(d), trusted);
        checkOperation(Math.cosh(d), trusted);
        checkOperation(Math.tanh(d), trusted);
        checkOperation(Math.cbrt(d), trusted);
        checkOperation(Math.pow(d, d), trusted);
        checkOperation(Math.exp(d), trusted);
        checkOperation(Math.expm1(d), trusted);
        checkOperation(Math.ceil(d), trusted);
        checkOperation(Math.floor(d), trusted);
        checkOperation(Math.copySign(d, d), trusted);
        checkOperation(Math.copySign(f, f), trusted);
        checkOperation(Math.hypot(d, d), trusted);
        checkOperation(Math.IEEEremainder(d, d), trusted);
        checkOperation(Math.max(d, d), trusted);
        checkOperation(Math.max(l, l), trusted);
        checkOperation(Math.max(i, i), trusted);
        checkOperation(Math.max(f, f), trusted);
        checkOperation(Math.min(d, d), trusted);
        checkOperation(Math.min(l, l), trusted);
        checkOperation(Math.min(i, i), trusted);
        checkOperation(Math.min(f, f), trusted);
        checkOperation(Math.log(d), trusted);
        checkOperation(Math.log10(d), trusted);
        checkOperation(Math.log1p(d), trusted);
        checkOperation(Math.random(), true);
        checkOperation(Math.rint(d), trusted);
        checkOperation(Math.nextAfter(d, d), trusted);
        checkOperation(Math.nextAfter(f, f), trusted);
        checkOperation(Math.nextUp(d), trusted);
        checkOperation(Math.nextUp(f), trusted);
        checkOperation(Math.scalb(d, i), trusted);
        checkOperation(Math.scalb(f, i), trusted);
        checkOperation(Math.signum(d), trusted);
        checkOperation(Math.signum(f), trusted);
        checkOperation(Math.sqrt(d), trusted);
        checkOperation(Math.getExponent(d), trusted);
        checkOperation(Math.getExponent(f), trusted);
        checkOperation(Math.toDegrees(d), trusted);
        checkOperation(Math.toRadians(d), trusted);
        checkOperation(Math.ulp(d), trusted);
        checkOperation(Math.ulp(f), trusted);

        checkOperation(StrictMath.abs(i), trusted);
        checkOperation(StrictMath.abs(d), trusted);
        checkOperation(StrictMath.abs(f), trusted);
        checkOperation(StrictMath.abs(l), trusted);
        checkOperation(StrictMath.acos(d), trusted);
        checkOperation(StrictMath.asin(d), trusted);
        checkOperation(StrictMath.atan(d), trusted);
        checkOperation(StrictMath.atan2(d, d), trusted);
        checkOperation(StrictMath.sin(d), trusted);
        checkOperation(StrictMath.cos(d), trusted);
        checkOperation(StrictMath.tan(d), trusted);
        checkOperation(StrictMath.sinh(d), trusted);
        checkOperation(StrictMath.cosh(d), trusted);
        checkOperation(StrictMath.tanh(d), trusted);
        checkOperation(StrictMath.cbrt(d), trusted);
        checkOperation(StrictMath.pow(d, d), trusted);
        checkOperation(StrictMath.exp(d), trusted);
        checkOperation(StrictMath.expm1(d), trusted);
        checkOperation(StrictMath.ceil(d), trusted);
        checkOperation(StrictMath.floor(d), trusted);
        checkOperation(StrictMath.copySign(d, d), trusted);
        checkOperation(StrictMath.copySign(f, f), trusted);
        checkOperation(StrictMath.hypot(d, d), trusted);
        checkOperation(StrictMath.IEEEremainder(d, d), trusted);
        checkOperation(StrictMath.max(d, d), trusted);
        checkOperation(StrictMath.max(l, l), trusted);
        checkOperation(StrictMath.max(i, i), trusted);
        checkOperation(StrictMath.max(f, f), trusted);
        checkOperation(StrictMath.min(d, d), trusted);
        checkOperation(StrictMath.min(l, l), trusted);
        checkOperation(StrictMath.min(i, i), trusted);
        checkOperation(StrictMath.min(f, f), trusted);
        checkOperation(StrictMath.log(d), trusted);
        checkOperation(StrictMath.log10(d), trusted);
        checkOperation(StrictMath.log1p(d), trusted);
        checkOperation(StrictMath.random(), true);
        checkOperation(StrictMath.rint(d), trusted);
        checkOperation(StrictMath.nextAfter(d, d), trusted);
        checkOperation(StrictMath.nextAfter(f, f), trusted);
        checkOperation(StrictMath.nextUp(d), trusted);
        checkOperation(StrictMath.nextUp(f), trusted);
        checkOperation(StrictMath.scalb(d, i), trusted);
        checkOperation(StrictMath.scalb(f, i), trusted);
        checkOperation(StrictMath.signum(d), trusted);
        checkOperation(StrictMath.signum(f), trusted);
        checkOperation(StrictMath.sqrt(d), trusted);
        checkOperation(StrictMath.getExponent(d), trusted);
        checkOperation(StrictMath.getExponent(f), trusted);
        checkOperation(StrictMath.toDegrees(d), trusted);
        checkOperation(StrictMath.toRadians(d), trusted);
        checkOperation(StrictMath.ulp(d), trusted);
        checkOperation(StrictMath.ulp(f), trusted);
    }

    @Test
    public void mathTest() {
        mathTest(true);
        mathTest(false);
    }

    private void overflowUnderflowIntegerTest(Operation operation) {
        BigInteger maxInt = BigInteger.valueOf(Integer.MAX_VALUE);
        BigInteger minInt = BigInteger.valueOf(Integer.MIN_VALUE);
        int overflowed = 0, underflowed = 0;
        for (int i = 0; i < total_ops; i++) {
            BigInteger a = new BigInteger(31, rand);
            int aInt = a.intValue();
            BigInteger b = new BigInteger(31, rand);

            BigInteger result = null;
            int resultInt = 0;
            int bInt = b.intValue();
            switch (operation) {
            case add:
                result = a.add(b);
                resultInt = aInt + bInt;
                break;
            case multiply:
                result = a.multiply(b);
                resultInt = aInt * bInt;
                break;
            case subtract:
                result = a.subtract(b);
                resultInt = aInt - bInt;
                break;
            }

            // check for overflow / underflow...
            if (result.compareTo(maxInt) > 0) {
                Assert.assertTrue("We should have overflowed on " + aInt + " " + operation.getOp() + " " + bInt + " = "
                        + result, TaintUtils.isOverflowSet(resultInt));
                overflowed++;
            } else if (result.compareTo(minInt) < 0) {
                Assert.assertTrue("We should have underflowed on " + aInt + " " + operation.getOp() + " " + bInt + " = "
                        + result, TaintUtils.isUnderflowSet(resultInt));
                underflowed++;
            } else {
                Assert.assertFalse("We should not have overflowed or underflowed on " + aInt + " " + operation.getOp()
                        + " " + bInt + " = " + result,
                                   TaintUtils.isOverflowSet(resultInt) || TaintUtils.isUnderflowSet(resultInt));
            }
        }
        System.out.println("Tested " + operation + " operation on " + total_ops + " pairs of random integers ("
                + overflowed + " overflowed and " + underflowed + " underflowed)");
    }

    private void overflowUnderflowLongTest(Operation operation) {
        BigInteger maxInt = BigInteger.valueOf(Long.MAX_VALUE);
        BigInteger minInt = BigInteger.valueOf(Long.MIN_VALUE);
        int overflowed = 0, underflowed = 0;
        for (int i = 0; i < total_ops; i++) {
            BigInteger a = new BigInteger(63, rand);
            long aInt = a.longValue();
            BigInteger b = new BigInteger(63, rand);

            BigInteger result = null;
            long resultInt = 0;
            long bInt = b.longValue();
            switch (operation) {
            case add:
                result = a.add(b);
                resultInt = aInt + bInt;
                break;
            case multiply:
                result = a.multiply(b);
                resultInt = aInt * bInt;
                break;
            case subtract:
                result = a.subtract(b);
                resultInt = aInt - bInt;
                break;
            }

            // check for overflow / underflow...
            if (result.compareTo(maxInt) > 0) {
                Assert.assertTrue("We should have overflowed on " + aInt + " " + operation.getOp() + " " + bInt + " = "
                        + result, TaintUtils.isOverflowSet(resultInt));
                if (operation == Operation.multiply) {
                    Assert.assertTrue("Overflowed/underflowed result should been replaced with OVERFLOW/UNDERFLOW",
                                      "OVERFLOW".equals("" + resultInt) || "UNDERFLOW".equals("" + resultInt));
                } else {
                    Assert.assertEquals("We should have replaced overflowed value with \"OVERFLOW\"", "" + resultInt,
                                        "OVERFLOW");
                }
                overflowed++;
            } else if (result.compareTo(minInt) < 0) {
                Assert.assertTrue("We should have underflowed on " + aInt + " " + operation.getOp() + " " + bInt + " = "
                        + result, TaintUtils.isUnderflowSet(resultInt));
                if (operation == Operation.multiply) {
                    Assert.assertTrue("Overflowed/underflowed result should been replaced with OVERFLOW/UNDERFLOW",
                                      "OVERFLOW".equals("" + resultInt) || "UNDERFLOW".equals("" + resultInt));
                } else {
                    Assert.assertEquals("We should have replaced underflowed value with \"UNDERFLOW\"", "" + resultInt,
                                        "UNDERFLOW");
                }
                underflowed++;
            } else {
                Assert.assertFalse("We should not have overflowed or underflowed on " + aInt + " " + operation.getOp()
                        + " " + bInt + " = " + result,
                                   TaintUtils.isOverflowSet(resultInt) || TaintUtils.isUnderflowSet(resultInt));
            }
        }
        System.out.println("Tested " + operation + " operation on " + total_ops + " pairs of random integers ("
                + overflowed + " overflowed and " + underflowed + " underflowed)");
    }

    @Test
    public void overflowUnderflowTest() {
        overflowUnderflowIntegerTest(Operation.add);
        overflowUnderflowIntegerTest(Operation.subtract);
        overflowUnderflowIntegerTest(Operation.multiply);

        overflowUnderflowLongTest(Operation.add);
        overflowUnderflowLongTest(Operation.subtract);
        overflowUnderflowLongTest(Operation.multiply);
    }

    @Test
    public void divideByZeroTest() {
        int i = rand.nextInt() / 0; // mark as INFINITY
        long l = rand.nextLong() / 0;
        float f = rand.nextFloat() / 0;
        double d = rand.nextDouble() / 0;
        int iters = 0;
        do {
            Assert.assertEquals("toString() on int should be INFINITY", "INFINITY", "" + i);
            Assert.assertTrue("expression on INFINITY result should be INFINITY", TaintUtils.isInfinitySet(i));
            Assert.assertEquals("toString() on long should be INFINITY", "INFINITY", "" + l);
            Assert.assertTrue("expression on INFINITY result should be INFINITY", TaintUtils.isInfinitySet(l));
            Assert.assertEquals("toString() on float should be INFINITY", "INFINITY", "" + f);
            Assert.assertTrue("expression on INFINITY result should be INFINITY", TaintUtils.isInfinitySet(f));
            Assert.assertEquals("toString() on double should be INFINITY", "INFINITY", "" + d);
            Assert.assertTrue("expression on INFINITY result should be INFINITY", TaintUtils.isInfinitySet(d));
            switch (rand.nextInt(13)) {
            case 0:
                i = i - rand.nextInt();
                f = f - rand.nextFloat();
                d = d - rand.nextDouble();
                l = l - rand.nextLong();
                break;
            case 1:
                i = i + rand.nextInt();
                f = f + rand.nextFloat();
                d = d + rand.nextDouble();
                l = l + rand.nextLong();
                break;
            case 2:
                i = i / rand.nextInt();
                f = f / rand.nextFloat();
                d = d / rand.nextDouble();
                l = l / rand.nextLong();
                break;
            case 3:
                i = i * rand.nextInt();
                f = f * rand.nextFloat();
                d = d * rand.nextDouble();
                l = l * rand.nextLong();
                break;
            case 4:
                i = i >> rand.nextInt();
                l = l >> rand.nextInt();
                d = d - rand.nextDouble();
                l = l - rand.nextLong();
                break;
            case 5:
                i = i << rand.nextInt();
                l = l << rand.nextInt();
                d = d + rand.nextDouble();
                l = l + rand.nextLong();
                break;
            case 6:
                i = i >>> rand.nextInt();
                l = l >>> rand.nextInt();
                d = d / rand.nextDouble();
                l = l / rand.nextLong();
                break;
            case 7:
                i = i & rand.nextInt();
                l = l & rand.nextLong();
                d = d * rand.nextDouble();
                l = l * rand.nextLong();
                break;
            case 8:
                i = i ^ rand.nextInt();
                l = l ^ rand.nextLong();
                f = (long) f;
                d = (int) d;
                break;
            case 9:
                i = i | rand.nextInt();
                l = l | rand.nextLong();
                f = (int) f;
                d = (float) d;
                break;
            case 10:
                i = (char) i;
                l = (char) l;
                f = (char) f;
                d = (char) d;
                break;
            case 11:
                i = (byte) i;
                l = (byte) l;
                f = (byte) f;
                d = (byte) d;
                break;
            case 12:
                i = (short) i;
                l = (short) l;
                f = (short) f;
                d = (short) d;
                break;
            }
        } while (++iters < total_ops);
    }

    /* FIXME this will no longer work since we lose references on primitives */
    //	@Test
    public void boundsCheckTest() {
        // Upper bound comparison test.
        int i = 500;
        int upper = 200000;
        int lower = 100;
        Assert.assertFalse("Upper and lower bound bit should both be unset",
                           TaintUtils.isBoundSet(i) || TaintUtils.isBoundSet(upper) || TaintUtils.isBoundSet(lower));
        if (i < upper)
            ; // do nothing...
        Assert.assertTrue("Bound bit should be set", TaintUtils.isBoundSet(i));
        Assert.assertFalse("Bound bit should be unset", TaintUtils.isBoundSet(lower));
        Assert.assertTrue("Bound bit should be set", TaintUtils.isBoundSet(upper));

        // Lower bound comparison test.
        i = 500;
        upper = 200000;
        lower = 100;
        Assert.assertFalse("Upper and lower bound bit should both be unset",
                           TaintUtils.isBoundSet(i) || TaintUtils.isBoundSet(upper) || TaintUtils.isBoundSet(lower));
        if (i > lower)
            ; // do nothing...
        Assert.assertTrue("Bound bit should be set", TaintUtils.isBoundSet(i));
        Assert.assertFalse("Bound bit should be unset", TaintUtils.isBoundSet(upper));
        Assert.assertTrue("Bound bit should be set", TaintUtils.isBoundSet(lower));

        // Upper and Lower bound comparison test.
        i = 500;
        upper = 200000;
        lower = 100;
        Assert.assertFalse("Upper and lower bound bit should both be unset",
                           TaintUtils.isBoundSet(i) || TaintUtils.isBoundSet(upper) || TaintUtils.isBoundSet(lower));
        if (i > lower && i < upper)
            ; // do nothing...
        Assert.assertTrue("Bound bit should be set", TaintUtils.isBoundSet(i));
        Assert.assertTrue("Bound bit should be set", TaintUtils.isBoundSet(upper));
        Assert.assertTrue("Bound bit should be set", TaintUtils.isBoundSet(lower));

        // == comparison test.
        i = 500;
        upper = 200000;
        lower = 100;
        Assert.assertFalse("Upper and lower bound bit should both be unset",
                           TaintUtils.isBoundSet(i) || TaintUtils.isBoundSet(upper) || TaintUtils.isBoundSet(lower));
        if (i == lower)
            ; // do nothing...
        Assert.assertTrue("Bound bit should be set", TaintUtils.isBoundSet(i));
        Assert.assertTrue("Bound bit should be set", TaintUtils.isBoundSet(lower));
        Assert.assertFalse("Bound bit should be unset", TaintUtils.isBoundSet(upper));

        // != comparison test.
        i = 500;
        upper = 200000;
        lower = 100;
        Assert.assertFalse("Upper and lower bound bit should both be unset",
                           TaintUtils.isBoundSet(i) || TaintUtils.isBoundSet(upper) || TaintUtils.isBoundSet(lower));
        if (i != lower)
            ; // do nothing...
        Assert.assertTrue("Bound bit should be set", TaintUtils.isBoundSet(i));
        Assert.assertTrue("Bound bit should be set", TaintUtils.isBoundSet(lower));
        Assert.assertFalse("Bound bit should be unset", TaintUtils.isBoundSet(upper));
    }

    @Test
    public void taintedBranchIntTest() {
        int MAX = TaintUtils.taint(Integer.MAX_VALUE);

        System.out.println("testing tainted branches...");
        for (int i = 0; i < 100; i++) {
            CleartrackException catchEx = null;

            try {
                int overIdx = MAX + rand.nextInt(MAX) + 1;
                if (overIdx > 200)
                    break;
            } catch (CleartrackException e) {
                catchEx = e;
            }

            Assert.assertNotNull("tainted conditional should have thrown an exception", catchEx);
        }
    }

    @Test
    public void taintedBranchLongTest() {
        long MAX = TaintUtils.taint(Long.MAX_VALUE);

        System.out.println("testing tainted branches...");
        for (int i = 0; i < 100; i++) {
            CleartrackException catchEx = null;

            try {
                long overIdx = MAX + Math.abs(rand.nextLong()) + 1;
                if (overIdx > 200)
                    break;
            } catch (CleartrackException e) {
                catchEx = e;
            }

            Assert.assertNotNull("tainted conditional should have thrown an exception", catchEx);
        }
    }

    private enum Operation {
        add("+"), subtract("-"), multiply("*");

        private String op;

        Operation(String op) {
            this.op = op;
        }

        String getOp() {
            return op;
        }
    }
}
