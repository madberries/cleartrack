package pac.wrap;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.Arrays;

import pac.util.Ret;
import pac.util.Overflows;
import pac.util.TaintValues;

public final class ByteArrayTaint implements Serializable, TaintableArray {
    private static final long serialVersionUID = 1978427475157407922L;

    public byte[] value;
    public int[] taint;
    public Charset charset;

    protected ByteArrayTaint(byte[] value, int[] taint) {
        this.value = value;
        this.taint = taint;
    }

    public final byte load(int idx, int idx_t, Ret ret) throws IOException {
        Overflows.checkOverflow(idx_t, "array-load", "byte", null, idx);
        int idxTaint = idx_t & TaintValues.TRUST_MASK;
        if (idxTaint != TaintValues.TRUSTED && (idx < 0 || idx >= taint.length)) { // index out of bounds
            Overflows.outOfBounds("array-load", "byte", value, idx);
            ret.taint = TaintValues.TRUSTED;
            return 0; //return default value
        }
        // only set trust bits from the idx taint...
        ret.taint = idxTaint | taint[idx];
        return value[idx];
    }

    public final byte load_noCheck(int idx, int idx_t, Ret ret) throws IOException {
        // only set trust bits from the idx taint...
        ret.taint = (idx_t & TaintValues.TRUST_MASK) | taint[idx];
        return value[idx];
    }

    public final void store(int idx, int idx_t, byte val, int val_t) throws IOException {
        int idxTaint = idx_t & TaintValues.TRUST_MASK;
        Overflows.checkOverflow(idx_t, "array-store", "byte", null, idx);
        if (idxTaint != TaintValues.TRUSTED && (idx < 0 || idx >= taint.length)) { // index out of bounds
            Overflows.outOfBounds("array-store", "byte", value, idx);
            return; // do not cause a null pointer exception.
        }
        value[idx] = val;
        taint[idx] = val_t | idxTaint;
    }

    public final void store_noCheck(int idx, int idx_t, byte val, int val_t) throws IOException {
        value[idx] = val;
        taint[idx] = val_t | (idx_t & TaintValues.TRUST_MASK);
    }

    public static final ByteArrayTaint newArray(int len, int len_t) throws IOException {
        Overflows.checkOverflow(len_t, "new-array", "byte", null, len);
        if ((len_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
            len = Overflows.checkAllocSize("byte", len);
        // System.out.printf ("ByteArrayTaint.newArray: allocating %d bytes\n", lenVal);
        return new ByteArrayTaint(new byte[len], new int[len]);
    }

    public static final ByteArrayTaint newArray_noCheck(int len, int len_t) throws IOException {
        // System.out.printf ("ByteArrayTaint.newArray: allocating %d bytes\n", lenVal);
        return new ByteArrayTaint(new byte[len], new int[len]);
    }

    public static final ByteArrayTaint[] newArray(int len1, int len1_t, int len2, int len2_t) throws IOException {
        int lenTaint = len1_t | len2_t;
        Overflows.checkOverflow(lenTaint, "new-array", "byte", null, len1, len2);
        if ((len1_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
            len1 = Overflows.checkAllocSize("byte", len1);
        if ((len2_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
            len2 = Overflows.checkAllocSize("byte", len2);
        byte[][] value = new byte[len1][len2];
        ByteArrayTaint[] taint = new ByteArrayTaint[len1];
        for (int i = 0; i < value.length; i++) {
            taint[i] = new ByteArrayTaint(value[i], new int[len2]);
        }
        return taint;
    }

    public static final ByteArrayTaint[] newArray_noCheck(int len1, int len1_t, int len2, int len2_t)
            throws IOException {
        byte[][] value = new byte[len1][len2];
        ByteArrayTaint[] taint = new ByteArrayTaint[len1];
        for (int i = 0; i < value.length; i++) {
            taint[i] = new ByteArrayTaint(value[i], new int[len2]);
        }
        return taint;
    }

    public static final ByteArrayTaint[][] newArray(int len1, int len1_t, int len2, int len2_t, int len3, int len3_t)
            throws IOException {
        int lenTaint = len1_t | len2_t | len3_t;
        Overflows.checkOverflow(lenTaint, "new-array", "byte", null, len1, len2, len3);
        if ((len1_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
            len1 = Overflows.checkAllocSize("byte", len1);
        if ((len2_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
            len2 = Overflows.checkAllocSize("byte", len2);
        if ((len3_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
            len3 = Overflows.checkAllocSize("byte", len3);
        byte[][][] value = new byte[len1][len2][len3];
        ByteArrayTaint[][] taint = new ByteArrayTaint[len1][len2];
        for (int i = 0; i < value.length; i++) {
            for (int j = 0; j < value[i].length; j++) {
                taint[i][j] = new ByteArrayTaint(value[i][j], new int[len3]);
            }
        }
        return taint;
    }

    public static final ByteArrayTaint[][] newArray_noCheck(int len1, int len1_t, int len2, int len2_t, int len3,
                                                            int len3_t)
            throws IOException {
        byte[][][] value = new byte[len1][len2][len3];
        ByteArrayTaint[][] taint = new ByteArrayTaint[len1][len2];
        for (int i = 0; i < value.length; i++) {
            for (int j = 0; j < value[i].length; j++) {
                taint[i][j] = new ByteArrayTaint(value[i][j], new int[len3]);
            }
        }
        return taint;
    }

    public static final ByteArrayTaint toTaintArray(byte[] arr) {
        if (arr == null)
            return null;
        int[] taint = new int[arr.length];
        Arrays.fill(taint, TaintValues.UNKNOWN);
        return new ByteArrayTaint(arr, taint);
    }

    public static final ByteArrayTaint[] toTaintArray(byte[][] arr) {
        if (arr == null)
            return null;
        ByteArrayTaint[] arrTaint = new ByteArrayTaint[arr.length];
        for (int i = 0; i < arrTaint.length; i++) {
            arrTaint[i] = toTaintArray(arr[i]);
        }
        return arrTaint;
    }

    public static final ByteArrayTaint[][] toTaintArray(byte[][][] arr) {
        if (arr == null)
            return null;
        ByteArrayTaint[][] arrTaint = new ByteArrayTaint[arr.length][];
        for (int i = 0; i < arrTaint.length; i++) {
            arrTaint[i] = toTaintArray(arr[i]);
        }
        return arrTaint;
    }

    public static final Object toTaintArray(Object arr) {
        if (arr == null)
            return null;
        int len = Array.getLength(arr);
        if (arr.getClass() == byte[].class)
            return new ByteArrayTaint((byte[]) arr, new int[len]);
        Object arrValue = Array.newInstance(ByteArrayTaint.class, len);
        for (int i = 0; i < len; i++) {
            Array.set(arrValue, i, toTaintArray(Array.get(arr, i)));
        }
        return arrValue;
    }

    public static final byte[] toValueArray(ByteArrayTaint arr) {
        if (arr == null)
            return null;
        return arr.value;
    }

    public static final byte[][] toValueArray(ByteArrayTaint[] arr) {
        if (arr == null)
            return null;
        byte[][] arrValue = new byte[arr.length][];
        for (int i = 0; i < arrValue.length; i++) {
            arrValue[i] = toValueArray(arr[i]);
        }
        return arrValue;
    }

    public static final byte[][][] toValueArray(ByteArrayTaint[][] arr) {
        if (arr == null)
            return null;
        byte[][][] arrValue = new byte[arr.length][][];
        for (int i = 0; i < arrValue.length; i++) {
            arrValue[i] = toValueArray(arr[i]);
        }
        return arrValue;
    }

    public static final Object toValueArray(Object arr) {
        if (arr == null)
            return null;
        if (arr.getClass() == ByteArrayTaint.class)
            return ((TaintableArray) arr).getValue();
        int len = Array.getLength(arr);
        Object arrValue = Array.newInstance(byte[].class, len);
        for (int i = 0; i < len; i++) {
            Array.set(arrValue, i, toValueArray(Array.get(arr, i)));
        }
        return arrValue;
    }

    @Override
    public final boolean equals(Object obj) {
        if (!(obj instanceof ByteArrayTaint))
            return false;
        return ((ByteArrayTaint) obj).value == value;
        // TODO log anytime values are equal but taint fields are not equal
    }

    @Override
    public final Object clone() {
        return new ByteArrayTaint(value.clone(), taint.clone());
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

    public static final String createTaintDisplayLines(ByteArrayTaint arr) {
        if (arr == null)
            return null;
        return TaintValues.toString(arr.taint, 0, arr.taint.length) + "\n" + new String(arr.value) + "\n";
    }

    public static final String createTaintDisplayLines(ByteArrayTaint arr, int offset, int length) {
        if (arr == null)
            return null;
        return TaintValues.toString(arr.taint, offset, length) + "\n" + new String(arr.value, offset, length) + "\n";
    }

    public static final void taint(ByteArrayTaint b, int inputType) {
        taint(b, inputType, 0, b.length() - 1);
    }

    public static final void trust(ByteArrayTaint b, int inputType) {
        trust(b, inputType, 0, b.length() - 1);
    }

    public static final void taint(ByteArrayTaint b, int inputType, int start, int end) {
        for (int i = start; i <= end; i++) {
            b.taint[i] |= TaintValues.TAINTED | inputType;
        }
    }

    public static final void trust(ByteArrayTaint b, int inputType, int start, int end) {
        for (int i = start; i <= end; i++) {
            b.taint[i] = (b.taint[i] & ~TaintValues.TRUST_MASK) | inputType;
        }
    }

    // **********************************************************
    // These following methods are here because the JUnit tests
    // need to refer to the instrumented copy (for querying and
    // manipulating taint).
    // **********************************************************

    public static final boolean isTainted(byte[] b) {
        return true;
    }

    public static final boolean isTainted(byte[] b, int start, int end) {
        return true;
    }

    public static final boolean isTrusted(byte[] b, int start, int end) {
        return false;
    }

    public static final void taint(byte[] b) {
    }

    public static final void taint(byte[] b, int start, int end) {
    }

    public static final void trust(byte[] b) {
    }

    public static final void trust(byte[] b, int start, int end) {
    }

    public static final boolean hasEqualTaint(byte[] arr1, byte[] arr2, int length) {
        return false;
    }

    public static final boolean hasEqualTaint(byte[] arr1, byte[] arr2, int mask, int length) {
        return false;
    }

    public static final boolean isAllTainted(byte[] b, int start, int end) {
        return true;
    }

    public static final boolean isTracked(byte[] b) {
        return false;
    }

    public static final String createTaintDisplayLines(byte[] bytes) {
        return "";
    }

    public static final boolean isTainted(ByteArrayTaint b, Ret ret) {
        return isTainted(b, 0, TaintValues.TRUSTED, b.length(), TaintValues.TRUSTED, ret);
    }

    public static final boolean isTainted(ByteArrayTaint b, int start, int start_t, int end, int end_t, Ret ret) {
        ret.taint = TaintValues.TRUSTED;
        int[] taint = b.taint;
        for (int i = start; i <= end; i++) {
            if (i >= taint.length)
                break;
            if ((taint[i] & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
                return true;
        }
        return false;
    }

    public static final boolean isTrusted(ByteArrayTaint b, int start, int start_t, int end, int end_t, Ret ret) {
        ret.taint = TaintValues.TRUSTED;
        int[] taint = b.taint;
        if (taint == null)
            return false;
        for (int i = start; i <= end; i++) {
            if (i >= taint.length)
                break;
            if ((taint[i] & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
                return false;
        }
        return true;
    }

    public static final void taint(ByteArrayTaint b, Ret ret) {
        taint(b, 0, TaintValues.TRUSTED, b.length() - 1, TaintValues.TRUSTED, ret);
    }

    public static final void taint(ByteArrayTaint b, int start, int start_t, int end, int end_t, Ret ret) {
        for (int i = start; i <= end; i++) {
            b.taint[i] |= TaintValues.TAINTED;
        }
    }

    public static final void trust(ByteArrayTaint b, Ret ret) {
        trust(b, 0, TaintValues.TRUSTED, b.length() - 1, TaintValues.TRUSTED, ret);
    }

    public static final void trust(ByteArrayTaint b, int start, int start_t, int end, int end_t, Ret ret) {
        for (int i = start; i <= end; i++) {
            b.taint[i] &= ~TaintValues.TRUST_MASK;
        }
    }

    public static final boolean hasEqualTaint(ByteArrayTaint arr1, ByteArrayTaint arr2, int length, int length_t,
                                              Ret ret) {
        return hasEqualTaint(arr1, arr2, 0xffffffff, TaintValues.TRUSTED, length, length_t, ret);
    }

    public static final boolean hasEqualTaint(ByteArrayTaint arr1, ByteArrayTaint arr2, int mask, int mask_t,
                                              int length, int length_t, Ret ret) {
        ret.taint = TaintValues.TRUSTED;
        for (int i = 0; i < length; i++) {
            if ((arr1.taint[i] & mask) != (arr2.taint[i] & mask))
                return false;
        }
        return true;
    }

    public static final boolean isAllTainted(ByteArrayTaint b, int start, int start_t, int end, int end_t, Ret ret) {
        ret.taint = TaintValues.TRUSTED;
        int[] taint = b.taint;
        for (int i = start; i <= end; i++) {
            if (i >= taint.length)
                break;
            if ((taint[i] & TaintValues.TRUST_MASK) == TaintValues.TRUSTED)
                return false;
        }
        return true;
    }

    public static final boolean isTracked(ByteArrayTaint b, Ret ret) {
        ret.taint = TaintValues.TRUSTED;
        int[] taint = b.taint;
        int len = taint.length;
        for (int i = 0; i < len; i++) {
            if ((taint[i] & TaintValues.TRUST_MASK) != TaintValues.UNKNOWN)
                return true;
        }
        return false;
    }

    public static final String createTaintDisplayLines(ByteArrayTaint arr, Ret ret) {
        return createTaintDisplayLines(arr);
    }
}
