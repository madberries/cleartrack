package pac.inst.taint;

import java.io.IOException;
import java.lang.reflect.Array;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.inst.InvocationType;
import pac.util.Ret;
import pac.util.Overflows;
import pac.util.TaintValues;
import pac.wrap.BooleanArrayTaint;
import pac.wrap.ByteArrayTaint;
import pac.wrap.CharArrayTaint;
import pac.wrap.DoubleArrayTaint;
import pac.wrap.FloatArrayTaint;
import pac.wrap.IntArrayTaint;
import pac.wrap.LongArrayTaint;
import pac.wrap.ShortArrayTaint;
import pac.wrap.TaintableArray;

@InstrumentationClass("java/lang/reflect/Array")
public final class ArrayInstrumentation {

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final int getLength(Object array, Ret ret) {
    if (array instanceof TaintableArray)
      return ((TaintableArray) array).length();
    return Array.getLength(array);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final Object newInstance(Class<?> klass, int length, int length_t, Ret ret)
      throws IOException {
    if (!klass.isPrimitive()) {
      // int lenVal = IntTaint.value(length);
      // int lenTaint = IntTaint.taint(length);
      // Overflows.checkOverflow(lenTaint, "new-array", klass.toString(), lenVal);
      // if ((length.taint & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
      // length.value = lenVal = Overflows.checkAllocSize("new-array", lenVal);
      Class<?> wrapperType = ClassInstrumentation.primTypeMap.get(klass);
      return Array.newInstance(wrapperType == null ? klass : wrapperType, length);
    } else {
      if (klass == boolean.class) {
        return BooleanArrayTaint.newArray(length, length_t);
      } else if (klass == byte.class) {
        return ByteArrayTaint.newArray(length, length_t);
      } else if (klass == short.class) {
        return ShortArrayTaint.newArray(length, length_t);
      } else if (klass == char.class) {
        return CharArrayTaint.newArray(length, length_t);
      } else if (klass == int.class) {
        return IntArrayTaint.newArray(length, length_t);
      } else if (klass == long.class) {
        return LongArrayTaint.newArray(length, length_t);
      } else if (klass == float.class) {
        return FloatArrayTaint.newArray(length, length_t);
      } else {
        return DoubleArrayTaint.newArray(length, length_t);
      }
    }
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final Object get(Object array, int index, int index_t, Ret ret) throws IOException {
    if (array instanceof BooleanArrayTaint) {
      boolean res = ((BooleanArrayTaint) array).load(index, index_t, ret);
      return new CleartrackBoolean(res, ret.taint, ret);
    } else if (array instanceof ByteArrayTaint) {
      byte res = ((ByteArrayTaint) array).load(index, index_t, ret);
      return new CleartrackByte(res, ret.taint, ret);
    } else if (array instanceof ShortArrayTaint) {
      short res = ((ShortArrayTaint) array).load(index, index_t, ret);
      return new CleartrackShort(res, ret.taint, ret);
    } else if (array instanceof CharArrayTaint) {
      char res = ((CharArrayTaint) array).load(index, index_t, ret);
      return new CleartrackCharacter(res, ret.taint, ret);
    } else if (array instanceof IntArrayTaint) {
      int res = ((IntArrayTaint) array).load(index, index_t, ret);
      return new CleartrackInteger(res, ret.taint, ret);
    } else if (array instanceof FloatArrayTaint) {
      float res = ((FloatArrayTaint) array).load(index, index_t, ret);
      return new CleartrackFloat(res, ret.taint, ret);
    } else if (array instanceof LongArrayTaint) {
      long res = ((LongArrayTaint) array).load(index, index_t, ret);
      return new CleartrackLong(res, ret.taint, ret);
    } else if (array instanceof DoubleArrayTaint) {
      double res = ((DoubleArrayTaint) array).load(index, index_t, ret);
      return new CleartrackDouble(res, ret.taint, ret);
    }
    Overflows.checkOverflow(index_t, "array-load", "object", null, index);
    if ((index_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED
        && (index < 0 || index >= Array.getLength(array))) { // Index out of bounds.
      Overflows.outOfBounds("array-load", "object", index, index);
      return null; // TODO: Return a new instance of the array element type.
    }
    return Array.get(array, index);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final boolean getBoolean(Object array, int index, int index_t, Ret ret)
      throws IOException {
    if (!(array instanceof BooleanArrayTaint))
      throw new IllegalArgumentException("Argument is not a boolean[]");
    return ((BooleanArrayTaint) array).load(index, index_t, ret);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final byte getByte(Object array, int index, int index_t, Ret ret)
      throws IOException {
    if (array instanceof BooleanArrayTaint || !(array instanceof ByteArrayTaint))
      throw new IllegalArgumentException("Argument is not a byte[]");
    return ((ByteArrayTaint) array).load(index, index_t, ret);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final short getShort(Object array, int index, int index_t, Ret ret)
      throws IOException {
    if (!(array instanceof ShortArrayTaint))
      throw new IllegalArgumentException("Argument is not a short[]");
    return ((ShortArrayTaint) array).load(index, index_t, ret);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final char getChar(Object array, int index, int index_t, Ret ret)
      throws IOException {
    if (!(array instanceof CharArrayTaint))
      throw new IllegalArgumentException("Argument is not a char[]");
    return ((CharArrayTaint) array).load(index, index_t, ret);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final int getInt(Object array, int index, int index_t, Ret ret) throws IOException {
    if (!(array instanceof IntArrayTaint))
      throw new IllegalArgumentException("Argument is not an int[]");
    return ((IntArrayTaint) array).load(index, index_t, ret);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final long getLong(Object array, int index, int index_t, Ret ret)
      throws IOException {
    if (!(array instanceof LongArrayTaint))
      throw new IllegalArgumentException("Argument is not a long[]");
    return ((LongArrayTaint) array).load(index, index_t, ret);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final float getFloat(Object array, int index, int index_t, Ret ret)
      throws IOException {
    if (!(array instanceof FloatArrayTaint))
      throw new IllegalArgumentException("Argument is not a float[]");
    return ((FloatArrayTaint) array).load(index, index_t, ret);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double getDouble(Object array, int index, int index_t, Ret ret)
      throws IOException {
    if (!(array instanceof DoubleArrayTaint))
      throw new IllegalArgumentException("Argument is not a double[]");
    return ((DoubleArrayTaint) array).load(index, index_t, ret);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final void set(Object array, int index, int index_t, Object value, Ret ret)
      throws IOException {
    if (array instanceof BooleanArrayTaint) {
      ((BooleanArrayTaint) array).store(index, index_t,
          ((CleartrackBoolean) value).booleanValue(ret), ret.taint);
      return;
    } else if (array instanceof ByteArrayTaint) {
      ((ByteArrayTaint) array).store(index, index_t, ((CleartrackByte) value).byteValue(ret),
          ret.taint);
      return;
    } else if (array instanceof ShortArrayTaint) {
      ((ShortArrayTaint) array).store(index, index_t, ((CleartrackShort) value).shortValue(ret),
          ret.taint);
      return;
    } else if (array instanceof CharArrayTaint) {
      ((CharArrayTaint) array).store(index, index_t, ((CleartrackCharacter) value).charValue(ret),
          ret.taint);
      return;
    } else if (array instanceof IntArrayTaint) {
      ((IntArrayTaint) array).store(index, index_t, ((CleartrackInteger) value).intValue(ret),
          ret.taint);
      return;
    } else if (array instanceof FloatArrayTaint) {
      ((FloatArrayTaint) array).store(index, index_t, ((CleartrackFloat) value).floatValue(ret),
          ret.taint);
      return;
    } else if (array instanceof LongArrayTaint) {
      ((LongArrayTaint) array).store(index, index_t, ((CleartrackLong) value).longValue(ret),
          ret.taint);
      return;
    } else if (array instanceof DoubleArrayTaint) {
      ((DoubleArrayTaint) array).store(index, index_t, ((CleartrackDouble) value).doubleValue(ret),
          ret.taint);
      return;
    }
    Overflows.checkOverflow(index_t, "array-store", "object", value, index);
    if ((index_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED
        && (index < 0 || index >= Array.getLength(array))) { // index out of bounds
      Overflows.outOfBounds("array-store", "boolean", value, index);
      return; // do not cause a null pointer exception.
    }
    Array.set(array, index, value);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final void setBoolean(Object array, int index, int index_t, boolean value,
      int value_t, Ret ret) throws IOException {
    if (!(array instanceof BooleanArrayTaint))
      throw new IllegalArgumentException("Argument is not a boolean[]");
    ((BooleanArrayTaint) array).store(index, index_t, value, value_t);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final void setByte(Object array, int index, int index_t, byte value, int value_t,
      Ret ret) throws IOException {
    if ((array instanceof BooleanArrayTaint) || !(array instanceof ByteArrayTaint))
      throw new IllegalArgumentException("Argument is not a byte[]");
    ((ByteArrayTaint) array).store(index, index_t, value, value_t);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final void setShort(Object array, int index, int index_t, short value, int value_t,
      Ret ret) throws IOException {
    if (!(array instanceof ShortArrayTaint))
      throw new IllegalArgumentException("Argument is not a short[]");
    ((ShortArrayTaint) array).store(index, index_t, value, value_t);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final void setChar(Object array, int index, int index_t, char value, int value_t,
      Ret ret) throws IOException {
    if (!(array instanceof CharArrayTaint))
      throw new IllegalArgumentException("Argument is not a char[]");
    ((CharArrayTaint) array).store(index, index_t, value, value_t);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final void setInt(Object array, int index, int index_t, int value, int value_t,
      Ret ret) throws IOException {
    if (!(array instanceof IntArrayTaint))
      throw new IllegalArgumentException("Argument is not an int[]");
    ((IntArrayTaint) array).store(index, index_t, value, value_t);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final void setLong(Object array, int index, int index_t, long value, int value_t,
      Ret ret) throws IOException {
    if (!(array instanceof LongArrayTaint))
      throw new IllegalArgumentException("Argument is not a long[]");
    ((LongArrayTaint) array).store(index, index_t, value, value_t);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final void setFloat(Object array, int index, int index_t, float value, int value_t,
      Ret ret) throws IOException {
    if (!(array instanceof FloatArrayTaint))
      throw new IllegalArgumentException("Argument is not a float[]");
    ((FloatArrayTaint) array).store(index, index_t, value, value_t);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final void setDouble(Object array, int index, int index_t, double value,
      int value_t, Ret ret) throws IOException {
    if (!(array instanceof DoubleArrayTaint))
      throw new IllegalArgumentException("Argument is not a double[]");
    ((DoubleArrayTaint) array).store(index, index_t, value, value_t);
  }

}
