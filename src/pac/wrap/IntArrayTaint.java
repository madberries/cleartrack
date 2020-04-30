package pac.wrap;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;

import pac.util.Ret;
import pac.util.Overflows;
import pac.util.TaintValues;

public final class IntArrayTaint implements Serializable, TaintableArray {
    private static final long serialVersionUID = 1088397075508936666L;

    public int[] value;
    public int[] taint;

    protected IntArrayTaint(int[] value, int[] taint) {
        this.value = value;
        this.taint = taint;
    }

    public final int load(int idx, int idx_t, Ret ret) throws IOException {
        Overflows.checkOverflow(idx_t, "array-load", "int", null, idx);
        int idxTaint = idx_t & TaintValues.TRUST_MASK;
        if (idxTaint != TaintValues.TRUSTED && (idx < 0 || idx >= taint.length)) { // index out of bounds
            Overflows.outOfBounds("array-load", "int", value, idx);
            ret.taint = TaintValues.TRUSTED;
            return 0; //return default value
        }
        // only set trust bits from the idx taint...
        ret.taint = idxTaint | taint[idx];
        return value[idx];
    }

    public final int load_noCheck(int idx, int idx_t, Ret ret) throws IOException {
        // only set trust bits from the idx taint...
        ret.taint = (idx_t & TaintValues.TRUST_MASK) | taint[idx];
        return value[idx];
    }

    public final void store(int idx, int idx_t, int val, int val_t) throws IOException {
        Overflows.checkOverflow(idx_t, "array-store", "int", val, idx);
        int idxTaint = idx_t & TaintValues.TRUST_MASK;
        if (idxTaint != TaintValues.TRUSTED && (idx < 0 || idx >= taint.length)) { // index out of bounds
            Overflows.outOfBounds("array-store", "int", value, idx);
            return; // do not cause a null pointer exception.
        }
        value[idx] = val;
        taint[idx] = val_t | idxTaint;
    }

    public final void store_noCheck(int idx, int idx_t, int val, int val_t) throws IOException {
        value[idx] = val;
        taint[idx] = val_t | (idx_t & TaintValues.TRUST_MASK);
    }

    public static final IntArrayTaint newArray(int len, int len_t) throws IOException {
        if ((len_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED) {
            // wow, this apparently gets called so early that String
            // cannot even be loaded yet.
            Overflows.checkOverflow(len_t, "new-array", "int", null, len);
            len = Overflows.checkAllocSize("int", len);
        }
        return new IntArrayTaint(new int[len], new int[len]);
    }

    public static final IntArrayTaint newArray_noCheck(int len, int len_t) throws IOException {
        return new IntArrayTaint(new int[len], new int[len]);
    }

    public static final IntArrayTaint[] newArray(int len1, int len1_t, int len2, int len2_t) throws IOException {
        int lenTaint = len1_t | len2_t;
        Overflows.checkOverflow(lenTaint, "new-array", "int", null, len1, len2);
        if ((len1_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
            len1 = Overflows.checkAllocSize("int", len1);
        if ((len2_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
            len2 = Overflows.checkAllocSize("int", len2);
        int[][] value = new int[len1][len2];
        IntArrayTaint[] taint = new IntArrayTaint[len1];
        for (int i = 0; i < value.length; i++) {
            taint[i] = new IntArrayTaint(value[i], new int[len2]);
        }
        return taint;
    }

    public static final IntArrayTaint[] newArray_noCheck(int len1, int len1_t, int len2, int len2_t)
            throws IOException {
        int[][] value = new int[len1][len2];
        IntArrayTaint[] taint = new IntArrayTaint[len1];
        for (int i = 0; i < value.length; i++) {
            taint[i] = new IntArrayTaint(value[i], new int[len2]);
        }
        return taint;
    }

    public static final IntArrayTaint[][] newArray(int len1, int len1_t, int len2, int len2_t, int len3, int len3_t)
            throws IOException {
        int lenTaint = len1_t | len2_t | len3_t;
        Overflows.checkOverflow(lenTaint, "new-array", "int", null, len1, len2, len3);
        if ((len1_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
            len1 = Overflows.checkAllocSize("int", len1);
        if ((len2_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
            len2 = Overflows.checkAllocSize("int", len2);
        if ((len3_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
            len3 = Overflows.checkAllocSize("int", len3);
        int[][][] value = new int[len1][len2][len3];
        IntArrayTaint[][] taint = new IntArrayTaint[len1][len2];
        for (int i = 0; i < value.length; i++) {
            for (int j = 0; j < value[i].length; j++) {
                taint[i][j] = new IntArrayTaint(value[i][j], new int[len3]);
            }
        }
        return taint;
    }

    public static final IntArrayTaint[][] newArray_noCheck(int len1, int len1_t, int len2, int len2_t, int len3,
                                                           int len3_t)
            throws IOException {
        int[][][] value = new int[len1][len2][len3];
        IntArrayTaint[][] taint = new IntArrayTaint[len1][len2];
        for (int i = 0; i < value.length; i++) {
            for (int j = 0; j < value[i].length; j++) {
                taint[i][j] = new IntArrayTaint(value[i][j], new int[len3]);
            }
        }
        return taint;
    }

    public static final IntArrayTaint toTaintArray(int[] arr) {
        if (arr == null)
            return null;
        int[] taint = new int[arr.length];
        Arrays.fill(taint, TaintValues.UNKNOWN);
        return new IntArrayTaint(arr, taint);
    }

    public static final IntArrayTaint[] toTaintArray(int[][] arr) {
        if (arr == null)
            return null;
        IntArrayTaint[] arrTaint = new IntArrayTaint[arr.length];
        for (int i = 0; i < arrTaint.length; i++) {
            arrTaint[i] = toTaintArray(arr[i]);
        }
        return arrTaint;
    }

    public static final IntArrayTaint[][] toTaintArray(int[][][] arr) {
        if (arr == null)
            return null;
        IntArrayTaint[][] arrTaint = new IntArrayTaint[arr.length][];
        for (int i = 0; i < arrTaint.length; i++) {
            arrTaint[i] = toTaintArray(arr[i]);
        }
        return arrTaint;
    }

    public static final Object toTaintArray(Object arr) {
        if (arr == null)
            return null;
        int len = Array.getLength(arr);
        if (arr.getClass() == int[].class)
            return new IntArrayTaint((int[]) arr, new int[len]);
        Object arrValue = Array.newInstance(IntArrayTaint.class, len);
        for (int i = 0; i < len; i++) {
            Array.set(arrValue, i, toTaintArray(Array.get(arr, i)));
        }
        return arrValue;
    }

    public static final void main(String[] args) {
        int[][] x = new int[][] { { 1, 2, 3 }, { 3, 4, 5 } };
        Object o = toTaintArray((Object) x);
        System.out.println(o.toString());
    }

    public static final int[] toValueArray(IntArrayTaint arr) {
        if (arr == null)
            return null;
        return arr.value;
    }

    public static final int[][] toValueArray(IntArrayTaint[] arr) {
        if (arr == null)
            return null;
        int[][] arrValue = new int[arr.length][];
        for (int i = 0; i < arrValue.length; i++) {
            arrValue[i] = toValueArray(arr[i]);
        }
        return arrValue;
    }

    public static final int[][][] toValueArray(IntArrayTaint[][] arr) {
        if (arr == null)
            return null;
        int[][][] arrValue = new int[arr.length][][];
        for (int i = 0; i < arrValue.length; i++) {
            arrValue[i] = toValueArray(arr[i]);
        }
        return arrValue;
    }

    public static final Object toValueArray(Object arr) {
        if (arr == null)
            return null;
        if (arr.getClass() == IntArrayTaint.class)
            return ((TaintableArray) arr).getValue();
        int len = Array.getLength(arr);
        Object arrValue = Array.newInstance(int[].class, len);
        for (int i = 0; i < len; i++) {
            Array.set(arrValue, i, toValueArray(Array.get(arr, i)));
        }
        return arrValue;
    }

    @Override
    public final boolean equals(Object obj) {
        if (!(obj instanceof IntArrayTaint))
            return false;
        return ((IntArrayTaint) obj).value == value;
        // TODO log anytime values are equal but taint fields are not equal
    }

    @Override
    public final Object clone() {
        return new IntArrayTaint(value.clone(), taint.clone());
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

    // **********************************************************
    // These following methods are here because the JUnit tests
    // need to refer to the instrumented copy (for querying and
    // manipulating taint).
    // **********************************************************

    public static final boolean isTainted(int[] b, int start, int end) {
        return true;
    }

    public static final void taint(int[] b, int start, int end) {
    }

    public static final boolean equals(int[] arr1, int[] arr2) {
        return false;
    }

    public static final boolean isTainted(IntArrayTaint b, int start, int start_t, int end, int end_t, Ret ret) {
        ret.taint = TaintValues.TRUSTED;
        int[] taint = b.taint;
        for (int i = start; i <= end; i++) {
            if (i >= taint.length)
                break;
            if ((taint[i] & TaintValues.TAINTED) == TaintValues.TAINTED
                    || (taint[i] & TaintValues.UNKNOWN) == TaintValues.UNKNOWN)
                return true;
        }
        return false;
    }

    public static final void taint(IntArrayTaint b, int start, int start_t, int end, int end_t, Ret ret) {
        for (int i = start; i <= end; i++) {
            b.taint[i] |= TaintValues.TAINTED;
        }
    }

    public static final boolean equals(IntArrayTaint arr1, IntArrayTaint arr2, Ret ret) {
        ret.taint = TaintValues.TRUSTED;
        if (arr1.taint.length != arr2.taint.length || arr1.value.length != arr2.value.length
                || arr1.taint.length != arr1.value.length)
            return false;
        int len = arr1.taint.length;
        for (int i = 0; i < len; i++) {
            if (arr1.taint[i] != arr2.taint[i] || arr1.value[i] != arr2.value[i])
                return false;
        }
        return true;
    }
}
