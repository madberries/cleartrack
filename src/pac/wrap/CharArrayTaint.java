package pac.wrap;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;

import pac.util.Ret;
import pac.util.Overflows;
import pac.util.TaintUtils;
import pac.util.TaintValues;

public final class CharArrayTaint implements Serializable, TaintableArray {
  private static final long serialVersionUID = -2643275876156658807L;

  public static boolean unknown_is_trusted = true;

  public char[] value;
  public int[] taint;

  public CharArrayTaint(char[] value, int[] taint) {
    this.value = value;
    this.taint = taint;
  }

  public final char load(int idx, int idx_t, Ret ret) throws IOException {
    Overflows.checkOverflow(idx_t, "array-load", "char", null, idx);
    int idxTaint = idx_t & TaintValues.TRUST_MASK;
    if (idxTaint != TaintValues.TRUSTED && (idx < 0 || idx >= taint.length)) { // Index out of
                                                                               // bounds.
      Overflows.outOfBounds("array-load", "char", value, idx);
      ret.taint = TaintValues.TRUSTED;
      return ' '; // Return the default value.
    }
    // The return taint be the OR of the taint at index idx and the taint of idx.
    ret.taint = idxTaint | taint[idx];
    return value[idx];
  }

  public final char load_noCheck(int idx, int idx_t, Ret ret) throws IOException {
    // The return taint be the OR of the taint at index idx and the taint of idx.
    ret.taint = (idx_t & TaintValues.TRUST_MASK) | taint[idx];
    return value[idx];
  }

  public final void store(int idx, int idx_t, char val, int val_t) throws IOException {
    Overflows.checkOverflow(idx_t, "array-store", "char", val, idx);
    int idxTaint = idx_t & TaintValues.TRUST_MASK;
    if (idxTaint != TaintValues.TRUSTED && (idx < 0 || idx >= taint.length)) { // Index out of
                                                                               // bounds.
      Overflows.outOfBounds("array-store", "char", value, idx);
      return; // Do not throw a null pointer exception.
    }
    value[idx] = val;
    taint[idx] = val_t | idxTaint;
  }

  public final void store_noCheck(int idx, int idx_t, char val, int val_t) throws IOException {
    value[idx] = val;
    taint[idx] = val_t | (idx_t & TaintValues.TRUST_MASK);
  }

  public static final void mark(CharArrayTaint arr, int taint, int start, int end) {
    Arrays.fill(arr.taint, start, end + 1, taint);
  }

  public static final void trust(CharArrayTaint b, int inputType) {
    if (b == null)
      return;
    trust(b, inputType, 0, b.value.length - 1);
  }

  public static final void trust(CharArrayTaint b, int inputType, int start, int end) {
    for (int i = start; i <= end; i++) {
      b.taint[i] = (b.taint[i] & ~TaintValues.TRUST_MASK) | inputType;
    }
  }

  public static final void taint(CharArrayTaint b, int inputType) {
    taint(b, inputType, 0, b.value.length - 1);
  }

  public static final void taint(CharArrayTaint b, int inputType, int start, int end) {
    for (int i = start; i <= end; i++) {
      b.taint[i] |= TaintValues.TAINTED | inputType;
    }
  }

  public static final boolean isAllTainted(CharArrayTaint b, int start, int end) {
    int[] taint = b.taint;
    for (int i = start; i <= end; i++) {
      if (i >= taint.length)
        break;
      if ((taint[i] & TaintValues.TAINTED) != TaintValues.TAINTED
          && (taint[i] & TaintValues.UNKNOWN) != TaintValues.UNKNOWN)
        return false;
    }
    return true;
  }

  public static final boolean isTainted(CharArrayTaint b) {
    return b == null ? true : isTainted(b, 0, b.value.length - 1);
  }

  public static final boolean isTainted(CharArrayTaint b, int start, int end) {
    int[] taint = b.taint;
    for (int i = start; i <= end; i++) {
      if (i >= taint.length)
        break;
      if (TaintUtils.isUntrusted(taint[i])) {
        // if ((taint[i] & TaintValues.TAINTED) == TaintValues.TAINTED ||
        // (taint[i] & TaintValues.UNKNOWN) == TaintValues.UNKNOWN)
        return true;
      }
    }
    return false;
  }

  public static final boolean isTrusted(CharArrayTaint b) {
    return b == null ? false : isTrusted(b, 0, b.value.length - 1);
  }

  public static final boolean isTrusted(CharArrayTaint b, int start, int end) {
    int[] taint = b.taint;
    for (int i = start; i <= end; i++) {
      if (i >= taint.length)
        break;
      if (TaintUtils.isUntrusted(taint[i]))
        return false;
    }
    return true;
  }

  public static final boolean hasEqualTaint(CharArrayTaint arr1, CharArrayTaint arr2, int mask) {
    if (arr1.taint.length != arr2.taint.length)
      return false;
    int len = arr1.taint.length;
    for (int i = 0; i < len; i++) {
      if ((arr1.taint[i] & mask) != (arr2.taint[i] & mask))
        return false;
    }
    return true;
  }

  public static final CharArrayTaint newArray(int len, int len_t) throws IOException {
    Overflows.checkOverflow(len_t, "new-array", "char", null, len);
    if ((len_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
      len = Overflows.checkAllocSize("char", len);
    return new CharArrayTaint(new char[len], new int[len]);
  }

  public static final CharArrayTaint newArray_noCheck(int len, int len_t) throws IOException {
    return new CharArrayTaint(new char[len], new int[len]);
  }

  public static final CharArrayTaint[] newArray(int len1, int len1_t, int len2, int len2_t)
      throws IOException {
    int lenTaint = len1_t | len2_t;
    Overflows.checkOverflow(lenTaint, "new-array", "char", null, len1, len2);
    if ((len1_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
      len1 = Overflows.checkAllocSize("char", len1);
    if ((len2_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
      len2 = Overflows.checkAllocSize("char", len2);
    char[][] value = new char[len1][len2];
    CharArrayTaint[] taint = new CharArrayTaint[len1];
    for (int i = 0; i < value.length; i++) {
      taint[i] = new CharArrayTaint(value[i], new int[len2]);
    }
    return taint;
  }

  public static final CharArrayTaint[] newArray_noCheck(int len1, int len1_t, int len2, int len2_t)
      throws IOException {
    char[][] value = new char[len1][len2];
    CharArrayTaint[] taint = new CharArrayTaint[len1];
    for (int i = 0; i < value.length; i++) {
      taint[i] = new CharArrayTaint(value[i], new int[len2]);
    }
    return taint;
  }

  public static final CharArrayTaint[][] newArray(int len1, int len1_t, int len2, int len2_t,
      int len3, int len3_t) throws IOException {
    int lenTaint = len1_t | len2_t | len3_t;
    Overflows.checkOverflow(lenTaint, "new-array", "char", null, len1, len2, len3);
    if ((len1_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
      len1 = Overflows.checkAllocSize("char", len1);
    if ((len2_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
      len2 = Overflows.checkAllocSize("char", len2);
    if ((len3_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
      len3 = Overflows.checkAllocSize("char", len3);
    char[][][] value = new char[len1][len2][len3];
    CharArrayTaint[][] taint = new CharArrayTaint[len1][len2];
    for (int i = 0; i < value.length; i++) {
      for (int j = 0; j < value[i].length; j++) {
        taint[i][j] = new CharArrayTaint(value[i][j], new int[len3]);
      }
    }
    return taint;
  }

  public static final CharArrayTaint[][] newArray_noCheck(int len1, int len1_t, int len2,
      int len2_t, int len3, int len3_t) throws IOException {
    char[][][] value = new char[len1][len2][len3];
    CharArrayTaint[][] taint = new CharArrayTaint[len1][len2];
    for (int i = 0; i < value.length; i++) {
      for (int j = 0; j < value[i].length; j++) {
        taint[i][j] = new CharArrayTaint(value[i][j], new int[len3]);
      }
    }
    return taint;
  }

  public static final CharArrayTaint toTaintArray(char[] arr) {
    if (arr == null)
      return null;
    int[] taint = new int[arr.length];
    Arrays.fill(taint, TaintValues.UNKNOWN);
    return new CharArrayTaint(arr, taint);
  }

  public static final CharArrayTaint toTaintArray(char[] arr, int initialTaint) {
    if (arr == null)
      return null;
    int[] taint = new int[arr.length];
    if (initialTaint != 0)
      Arrays.fill(taint, initialTaint);
    return new CharArrayTaint(arr, taint);
  }

  public static final CharArrayTaint[] toTaintArray(char[][] arr) {
    if (arr == null)
      return null;
    CharArrayTaint[] arrTaint = new CharArrayTaint[arr.length];
    for (int i = 0; i < arrTaint.length; i++) {
      arrTaint[i] = toTaintArray(arr[i]);
    }
    return arrTaint;
  }

  public static final CharArrayTaint[][] toTaintArray(char[][][] arr) {
    if (arr == null)
      return null;
    CharArrayTaint[][] arrTaint = new CharArrayTaint[arr.length][];
    for (int i = 0; i < arrTaint.length; i++) {
      arrTaint[i] = toTaintArray(arr[i]);
    }
    return arrTaint;
  }

  public static final CharArrayTaint[][][] toTaintArray(char[][][][] arr) {
    if (arr == null)
      return null;
    CharArrayTaint[][][] arrTaint = new CharArrayTaint[arr.length][][];
    for (int i = 0; i < arrTaint.length; i++) {
      arrTaint[i] = toTaintArray(arr[i]);
    }
    return arrTaint;
  }

  public static final Object toTaintArray(Object arr) {
    if (arr == null)
      return null;
    int len = Array.getLength(arr);
    if (arr.getClass() == char[].class)
      return new CharArrayTaint((char[]) arr, new int[len]);
    Object arrValue = Array.newInstance(CharArrayTaint.class, len);
    for (int i = 0; i < len; i++) {
      Array.set(arrValue, i, toTaintArray(Array.get(arr, i)));
    }
    return arrValue;
  }

  public static final char[] toValueArray(CharArrayTaint arr) {
    if (arr == null)
      return null;
    return arr.value;
  }

  public static final char[][] toValueArray(CharArrayTaint[] arr) {
    if (arr == null)
      return null;
    char[][] arrValue = new char[arr.length][];
    for (int i = 0; i < arrValue.length; i++) {
      arrValue[i] = toValueArray(arr[i]);
    }
    return arrValue;
  }

  public static final char[][][] toValueArray(CharArrayTaint[][] arr) {
    if (arr == null)
      return null;
    char[][][] arrValue = new char[arr.length][][];
    for (int i = 0; i < arrValue.length; i++) {
      arrValue[i] = toValueArray(arr[i]);
    }
    return arrValue;
  }

  public static final char[][][][] toValueArray(CharArrayTaint[][][] arr) {
    if (arr == null)
      return null;
    char[][][][] arrValue = new char[arr.length][][][];
    for (int i = 0; i < arrValue.length; i++) {
      arrValue[i] = toValueArray(arr[i]);
    }
    return arrValue;
  }

  public static final Object toValueArray(Object arr) {
    if (arr == null)
      return null;
    if (arr.getClass() == CharArrayTaint.class)
      return ((TaintableArray) arr).getValue();
    int len = Array.getLength(arr);
    Object arrValue = Array.newInstance(char[].class, len);
    for (int i = 0; i < len; i++) {
      Array.set(arrValue, i, toValueArray(Array.get(arr, i)));
    }
    return arrValue;
  }

  @Override
  public final boolean equals(Object obj) {
    if (!(obj instanceof CharArrayTaint))
      return false;
    return ((CharArrayTaint) obj).value == value;
    // TODO: Log any time values are equal but taint fields are not equal.
  }

  @Override
  public final Object clone() {
    return new CharArrayTaint(value.clone(), taint.clone());
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
    return createTaintDisplayLines(this);
  }

  public static final String createTaintDisplayLines(CharArrayTaint arr) {
    if (arr == null)
      return null;
    return TaintValues.toString(arr.taint, 0, arr.taint.length) + "\n" + new String(arr.value)
        + "\n";
  }

  // ***********************************************************************************************
  // These following methods are here because the JUnit tests need to refer to the instrumented copy
  // (for querying and manipulating taint).
  // ***********************************************************************************************

  public static final boolean isTainted(char[] b) {
    return true;
  }

  public static final boolean isTainted(char[] b, int start, int end) {
    return true;
  }

  public static final boolean isAllTainted(char[] b, int start, int end) {
    return true;
  }

  public static final boolean isTrusted(char[] b) {
    return false;
  }

  public static final boolean isTrusted(char[] b, int start, int end) {
    return false;
  }

  public static final void taint(char[] b) {}

  public static final void taint(char[] b, int start, int end) {}

  public static final void trust(char[] b) {}

  public static final void trust(char[] b, int start, int end) {}

  public static final boolean hasEqualTaint(char[] arr1, byte[] arr2) {
    return false;
  }

  public static final boolean hasEqualTaint(char[] arr1, char[] arr2) {
    return false;
  }

  public static final boolean hasEqualTaint(char[] arr1, char[] arr2, int mask) {
    return false;
  }

  public static final boolean hasEqualTaint(char[] arr1, float[] arr2) {
    return false;
  }

  public static final boolean hasEqualTaint(float[] arr1, char[] arr2) {
    return false;
  }

  public static final String createTaintDisplayLines(char[] chars) {
    return "";
  }

  public static final boolean isTainted(CharArrayTaint b, Ret ret) {
    ret.taint = TaintValues.TRUSTED;
    return isTainted(b);
  }

  /** Returns true if any characters from start to end are tainted **/
  public static final boolean isTainted(CharArrayTaint b, int start, int start_t, int end,
      int end_t, Ret ret) {
    ret.taint = TaintValues.TRUSTED;
    return isTainted(b, start, end);
  }

  public static final boolean isAllTainted(CharArrayTaint b, int start, int start_t, int end,
      int end_t, Ret ret) {
    ret.taint = TaintValues.TRUSTED;
    return isAllTainted(b, start, end);
  }

  public static final boolean isTrusted(CharArrayTaint b, Ret ret) {
    ret.taint = TaintValues.TRUSTED;
    return isTrusted(b);
  }

  /** Returns ture if every character from start to end is trusted **/
  public static final boolean isTrusted(CharArrayTaint b, int start, int start_t, int end,
      int end_t, Ret ret) {
    ret.taint = TaintValues.TRUSTED;
    return isTrusted(b, start, end);
  }

  public static final void taint(CharArrayTaint b, Ret ret) {
    taint(b, 0, TaintValues.TRUSTED, b.length() - 1, TaintValues.TRUSTED, ret);
  }

  public static final void taint(CharArrayTaint b, int start, int start_t, int end, int end_t,
      Ret ret) {
    for (int i = start; i <= end; i++) {
      b.taint[i] |= TaintValues.TAINTED;
    }
  }

  public static final void trust(CharArrayTaint b, Ret ret) {
    trust(b, 0, TaintValues.TRUSTED, b.length() - 1, TaintValues.TRUSTED, ret);
  }

  public static final void trust(CharArrayTaint b, int start, int start_t, int end, int end_t,
      Ret ret) {
    for (int i = start; i <= end; i++) {
      b.taint[i] &= ~TaintValues.TRUST_MASK;
    }
  }

  public static final boolean hasEqualTaint(CharArrayTaint arr1, ByteArrayTaint arr2, Ret ret) {
    ret.taint = TaintValues.TRUSTED;
    if (arr1.taint.length != arr2.taint.length)
      return false;
    int len = arr1.taint.length;
    for (int i = 0; i < len; i++) {
      if ((arr1.taint[i] & TaintValues.NONPRIM_MASK) != (arr2.taint[i] & TaintValues.NONPRIM_MASK))
        return false;
    }
    return true;
  }

  public static final boolean hasEqualTaint(CharArrayTaint arr1, CharArrayTaint arr2, Ret ret) {
    ret.taint = TaintValues.TRUSTED;
    return hasEqualTaint(arr1, arr2, 0xffffffff);
  }

  public static final boolean hasEqualTaint(CharArrayTaint arr1, CharArrayTaint arr2, int mask,
      int mask_t, Ret ret) {
    ret.taint = TaintValues.TRUSTED;
    return hasEqualTaint(arr1, arr2, mask);
  }

  public static final boolean hasEqualTaint(CharArrayTaint arr1, FloatArrayTaint arr2, Ret ret) {
    ret.taint = TaintValues.TRUSTED;
    if (arr1.taint.length != arr2.taint.length)
      return false;
    int len = arr1.taint.length;
    for (int i = 0; i < len; i++) {
      if ((arr1.taint[i] & TaintValues.NONPRIM_MASK) != (arr2.taint[i] & TaintValues.NONPRIM_MASK))
        return false;
    }
    return true;
  }

  public static final boolean hasEqualTaint(FloatArrayTaint arr1, CharArrayTaint arr2, Ret ret) {
    ret.taint = TaintValues.TRUSTED;
    if (arr1.taint.length != arr2.taint.length)
      return false;
    int len = arr1.taint.length;
    for (int i = 0; i < len; i++) {
      if ((arr1.taint[i] & TaintValues.NONPRIM_MASK) != (arr2.taint[i] & TaintValues.NONPRIM_MASK))
        return false;
    }
    return true;
  }

  public static final String createTaintDisplayLines(CharArrayTaint arr, Ret ret) {
    return createTaintDisplayLines(arr);
  }
}
