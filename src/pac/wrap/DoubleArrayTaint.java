package pac.wrap;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;

import pac.util.Ret;
import pac.util.Overflows;
import pac.util.TaintValues;

public final class DoubleArrayTaint implements Serializable, TaintableArray {
  private static final long serialVersionUID = 4582850924520669912L;

  public double[] value;
  public int[] taint;

  protected DoubleArrayTaint(double[] value, int[] taint) {
    this.value = value;
    this.taint = taint;
  }

  public final double load(int idx, int idx_t, Ret ret) throws IOException {
    Overflows.checkOverflow(idx_t, "array-load", "double", null, idx);
    int idxTaint = idx_t & TaintValues.TRUST_MASK;
    if (idxTaint != TaintValues.TRUSTED && (idx < 0 || idx >= taint.length)) { // Index out of
                                                                               // bounds.
      Overflows.outOfBounds("array-load", "double", value, idx);
      ret.taint = TaintValues.TRUSTED;
      return 0.0; // Return the default value.
    }
    // The return taint be the OR of the taint at index idx and the taint of idx.
    ret.taint = idxTaint | taint[idx];
    return value[idx];
  }

  public final double load_noCheck(int idx, int idx_t, Ret ret) throws IOException {
    // The return taint be the OR of the taint at index idx and the taint of idx.
    ret.taint = (idx_t & TaintValues.TRUST_MASK) | taint[idx];
    return value[idx];
  }

  public final void store(int idx, int idx_t, double val, int val_t) throws IOException {
    Overflows.checkOverflow(idx_t, "array-store", "double", val, idx);
    int idxTaint = idx_t & TaintValues.TRUST_MASK;
    if (idxTaint != TaintValues.TRUSTED && (idx < 0 || idx >= taint.length)) { // Index out of
                                                                               // bounds.
      Overflows.outOfBounds("array-store", "double", value, idx);
      return; // Do not throw a null pointer exception.
    }
    value[idx] = val;
    taint[idx] = val_t | idxTaint;
  }

  public final void store_noCheck(int idx, int idx_t, double val, int val_t) throws IOException {
    value[idx] = val;
    taint[idx] = val_t | (idx_t & TaintValues.TRUST_MASK);
  }

  public static final DoubleArrayTaint newArray(int len, int len_t) throws IOException {
    Overflows.checkOverflow(len_t, "new-array", "double", null, len);
    if ((len_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
      len = Overflows.checkAllocSize("double", len);
    return new DoubleArrayTaint(new double[len], new int[len]);
  }

  public static final DoubleArrayTaint newArray_noCheck(int len, int len_t) throws IOException {
    return new DoubleArrayTaint(new double[len], new int[len]);
  }

  public static final DoubleArrayTaint[] newArray(int len1, int len1_t, int len2, int len2_t)
      throws IOException {
    int lenTaint = len1_t | len2_t;
    Overflows.checkOverflow(lenTaint, "new-array", "double", null, len1, len2);
    if ((len1_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
      len1 = Overflows.checkAllocSize("double", len1);
    if ((len2_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
      len2 = Overflows.checkAllocSize("double", len2);
    double[][] value = new double[len1][len2];
    DoubleArrayTaint[] taint = new DoubleArrayTaint[len1];
    for (int i = 0; i < value.length; i++) {
      taint[i] = new DoubleArrayTaint(value[i], new int[len2]);
    }
    return taint;
  }

  public static final DoubleArrayTaint[] newArray_noCheck(int len1, int len1_t, int len2,
      int len2_t) throws IOException {
    double[][] value = new double[len1][len2];
    DoubleArrayTaint[] taint = new DoubleArrayTaint[len1];
    for (int i = 0; i < value.length; i++) {
      taint[i] = new DoubleArrayTaint(value[i], new int[len2]);
    }
    return taint;
  }

  public static final DoubleArrayTaint[][] newArray(int len1, int len1_t, int len2, int len2_t,
      int len3, int len3_t) throws IOException {
    int lenTaint = len1_t | len2_t | len3_t;
    Overflows.checkOverflow(lenTaint, "new-array", "double", null, len1, len2, len3);
    if ((len1_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
      len1 = Overflows.checkAllocSize("double", len1);
    if ((len2_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
      len2 = Overflows.checkAllocSize("double", len2);
    if ((len3_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
      len3 = Overflows.checkAllocSize("double", len3);
    double[][][] value = new double[len1][len2][len3];
    DoubleArrayTaint[][] taint = new DoubleArrayTaint[len1][len2];
    for (int i = 0; i < value.length; i++) {
      for (int j = 0; j < value[i].length; j++) {
        taint[i][j] = new DoubleArrayTaint(value[i][j], new int[len3]);
      }
    }
    return taint;
  }

  public static final DoubleArrayTaint[][] newArray_noCheck(int len1, int len1_t, int len2,
      int len2_t, int len3, int len3_t) throws IOException {
    double[][][] value = new double[len1][len2][len3];
    DoubleArrayTaint[][] taint = new DoubleArrayTaint[len1][len2];
    for (int i = 0; i < value.length; i++) {
      for (int j = 0; j < value[i].length; j++) {
        taint[i][j] = new DoubleArrayTaint(value[i][j], new int[len3]);
      }
    }
    return taint;
  }

  public static final DoubleArrayTaint toTaintArray(double[] arr) {
    if (arr == null)
      return null;
    int[] taint = new int[arr.length];
    Arrays.fill(taint, TaintValues.UNKNOWN);
    return new DoubleArrayTaint(arr, taint);
  }

  public static final DoubleArrayTaint[] toTaintArray(double[][] arr) {
    if (arr == null)
      return null;
    DoubleArrayTaint[] arrTaint = new DoubleArrayTaint[arr.length];
    for (int i = 0; i < arrTaint.length; i++) {
      arrTaint[i] = toTaintArray(arr[i]);
    }
    return arrTaint;
  }

  public static final DoubleArrayTaint[][] toTaintArray(double[][][] arr) {
    if (arr == null)
      return null;
    DoubleArrayTaint[][] arrTaint = new DoubleArrayTaint[arr.length][];
    for (int i = 0; i < arrTaint.length; i++) {
      arrTaint[i] = toTaintArray(arr[i]);
    }
    return arrTaint;
  }

  public static final Object toTaintArray(Object arr) {
    if (arr == null)
      return null;
    int len = Array.getLength(arr);
    if (arr.getClass() == double[].class)
      return new DoubleArrayTaint((double[]) arr, new int[len]);
    Object arrValue = Array.newInstance(DoubleArrayTaint.class, len);
    for (int i = 0; i < len; i++) {
      Array.set(arrValue, i, toTaintArray(Array.get(arr, i)));
    }
    return arrValue;
  }

  public static final double[] toValueArray(DoubleArrayTaint arr) {
    if (arr == null)
      return null;
    return arr.value;
  }

  public static final double[][] toValueArray(DoubleArrayTaint[] arr) {
    if (arr == null)
      return null;
    double[][] arrValue = new double[arr.length][];
    for (int i = 0; i < arrValue.length; i++) {
      arrValue[i] = toValueArray(arr[i]);
    }
    return arrValue;
  }

  public static final double[][][] toValueArray(DoubleArrayTaint[][] arr) {
    if (arr == null)
      return null;
    double[][][] arrValue = new double[arr.length][][];
    for (int i = 0; i < arrValue.length; i++) {
      arrValue[i] = toValueArray(arr[i]);
    }
    return arrValue;
  }

  public static final Object toValueArray(Object arr) {
    if (arr == null)
      return null;
    if (arr.getClass() == DoubleArrayTaint.class)
      return ((TaintableArray) arr).getValue();
    int len = Array.getLength(arr);
    Object arrValue = Array.newInstance(double[].class, len);
    for (int i = 0; i < len; i++) {
      Array.set(arrValue, i, toValueArray(Array.get(arr, i)));
    }
    return arrValue;
  }

  @Override
  public final boolean equals(Object obj) {
    if (!(obj instanceof DoubleArrayTaint))
      return false;
    return ((DoubleArrayTaint) obj).value == value;
    // TODO: Log any time values are equal but taint fields are not equal.
  }

  @Override
  public final Object clone() {
    return new DoubleArrayTaint(value.clone(), taint.clone());
  }

  @Override
  public final int length() {
    return value.length;
  }

  @Override
  public final Object getValue() {
    return value;
  }

  @Override
  public final int[] getTaint() {
    return taint;
  }

  @Override
  public final String toString() {
    return Arrays.toString(value);
  }
}
