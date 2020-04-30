package pac.wrap;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;

import pac.util.Ret;
import pac.util.Overflows;
import pac.util.TaintValues;

public final class ShortArrayTaint implements Serializable, TaintableArray {
    private static final long serialVersionUID = -881983159809630240L;

    public short[] value;
    public int[] taint;

    protected ShortArrayTaint(short[] value, int[] taint) {
        this.value = value;
        this.taint = taint;
    }

    public final short load(int idx, int idx_t, Ret ret) throws IOException {
        Overflows.checkOverflow(idx_t, "array-load", "short", null, idx);
        int idxTaint = idx_t & TaintValues.TRUST_MASK;
        if (idxTaint != TaintValues.TRUSTED && (idx < 0 || idx >= taint.length)) { // index out of bounds
            Overflows.outOfBounds("array-load", "short", value, idx);
            ret.taint = TaintValues.TRUSTED;
            return 0; //return default value
        }
        // only set trust bits from the idx taint...
        ret.taint = idxTaint | taint[idx];
        return value[idx];
    }

    public final short load_noCheck(int idx, int idx_t, Ret ret) throws IOException {
        // only set trust bits from the idx taint...
        ret.taint = (idx_t & TaintValues.TRUST_MASK) | taint[idx];
        return value[idx];
    }

    public final void store(int idx, int idx_t, short val, int val_t) throws IOException {
        Overflows.checkOverflow(idx_t, "array-store", "short", val, idx);
        int idxTaint = idx_t & TaintValues.TRUST_MASK;
        if (idxTaint != TaintValues.TRUSTED && (idx < 0 || idx >= taint.length)) { // index out of bounds
            Overflows.outOfBounds("array-store", "short", value, idx);
            return; // do not cause a null pointer exception.
        }
        value[idx] = val;
        taint[idx] = val_t | idxTaint;
    }

    public final void store_noCheck(int idx, int idx_t, short val, int val_t) throws IOException {
        value[idx] = val;
        taint[idx] = val_t | (idx_t & TaintValues.TRUST_MASK);
    }

    public static final ShortArrayTaint newArray(int len, int len_t) throws IOException {
        Overflows.checkOverflow(len_t, "new-array", "short", null, len);
        if ((len_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
            len = Overflows.checkAllocSize("short", len);
        return new ShortArrayTaint(new short[len], new int[len]);
    }

    public static final ShortArrayTaint newArray_noCheck(int len, int len_t) throws IOException {
        return new ShortArrayTaint(new short[len], new int[len]);
    }

    public static final ShortArrayTaint[] newArray(int len1, int len1_t, int len2, int len2_t) throws IOException {
        int lenTaint = len1_t | len2_t;
        Overflows.checkOverflow(lenTaint, "new-array", "short", null, len1, len2);
        if ((len1_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
            len1 = Overflows.checkAllocSize("short", len1);
        if ((len2_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
            len2 = Overflows.checkAllocSize("short", len2);
        short[][] value = new short[len1][len2];
        ShortArrayTaint[] taint = new ShortArrayTaint[len1];
        for (int i = 0; i < value.length; i++) {
            taint[i] = new ShortArrayTaint(value[i], new int[len2]);
        }
        return taint;
    }

    public static final ShortArrayTaint[] newArray_noCheck(int len1, int len1_t, int len2, int len2_t)
            throws IOException {
        short[][] value = new short[len1][len2];
        ShortArrayTaint[] taint = new ShortArrayTaint[len1];
        for (int i = 0; i < value.length; i++) {
            taint[i] = new ShortArrayTaint(value[i], new int[len2]);
        }
        return taint;
    }

    public static final ShortArrayTaint[][] newArray(int len1, int len1_t, int len2, int len2_t, int len3, int len3_t)
            throws IOException {
        int lenTaint = len1_t | len2_t | len3_t;
        Overflows.checkOverflow(lenTaint, "new-array", "short", null, len1, len2, len3);
        if ((len1_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
            len1 = Overflows.checkAllocSize("short", len1);
        if ((len2_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
            len2 = Overflows.checkAllocSize("short", len2);
        if ((len3_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
            len3 = Overflows.checkAllocSize("short", len3);
        short[][][] value = new short[len1][len2][len3];
        ShortArrayTaint[][] taint = new ShortArrayTaint[len1][len2];
        for (int i = 0; i < value.length; i++) {
            for (int j = 0; j < value[i].length; j++) {
                taint[i][j] = new ShortArrayTaint(value[i][j], new int[len3]);
            }
        }
        return taint;
    }

    public static final ShortArrayTaint[][] newArray_noCheck(int len1, int len1_t, int len2, int len2_t, int len3,
                                                             int len3_t)
            throws IOException {
        short[][][] value = new short[len1][len2][len3];
        ShortArrayTaint[][] taint = new ShortArrayTaint[len1][len2];
        for (int i = 0; i < value.length; i++) {
            for (int j = 0; j < value[i].length; j++) {
                taint[i][j] = new ShortArrayTaint(value[i][j], new int[len3]);
            }
        }
        return taint;
    }

    public static final ShortArrayTaint toTaintArray(short[] arr) {
        if (arr == null)
            return null;
        int[] taint = new int[arr.length];
        Arrays.fill(taint, TaintValues.UNKNOWN);
        return new ShortArrayTaint(arr, taint);
    }

    public static final ShortArrayTaint[] toTaintArray(short[][] arr) {
        if (arr == null)
            return null;
        ShortArrayTaint[] arrTaint = new ShortArrayTaint[arr.length];
        for (int i = 0; i < arrTaint.length; i++) {
            arrTaint[i] = toTaintArray(arr[i]);
        }
        return arrTaint;
    }

    public static final ShortArrayTaint[][] toTaintArray(short[][][] arr) {
        if (arr == null)
            return null;
        ShortArrayTaint[][] arrTaint = new ShortArrayTaint[arr.length][];
        for (int i = 0; i < arrTaint.length; i++) {
            arrTaint[i] = toTaintArray(arr[i]);
        }
        return arrTaint;
    }

    public static final Object toTaintArray(Object arr) {
        if (arr == null)
            return null;
        int len = Array.getLength(arr);
        if (arr.getClass() == short[].class)
            return new ShortArrayTaint((short[]) arr, new int[len]);
        Object arrValue = Array.newInstance(ShortArrayTaint.class, len);
        for (int i = 0; i < len; i++) {
            Array.set(arrValue, i, toTaintArray(Array.get(arr, i)));
        }
        return arrValue;
    }

    public static final short[] toValueArray(ShortArrayTaint arr) {
        if (arr == null)
            return null;
        return arr.value;
    }

    public static final short[][] toValueArray(ShortArrayTaint[] arr) {
        if (arr == null)
            return null;
        short[][] arrValue = new short[arr.length][];
        for (int i = 0; i < arrValue.length; i++) {
            arrValue[i] = toValueArray(arr[i]);
        }
        return arrValue;
    }

    public static final short[][][] toValueArray(ShortArrayTaint[][] arr) {
        if (arr == null)
            return null;
        short[][][] arrValue = new short[arr.length][][];
        for (int i = 0; i < arrValue.length; i++) {
            arrValue[i] = toValueArray(arr[i]);
        }
        return arrValue;
    }

    public static final Object toValueArray(Object arr) {
        if (arr == null)
            return null;
        if (arr.getClass() == ShortArrayTaint.class)
            return ((TaintableArray) arr).getValue();
        int len = Array.getLength(arr);
        Object arrValue = Array.newInstance(short[].class, len);
        for (int i = 0; i < len; i++) {
            Array.set(arrValue, i, toValueArray(Array.get(arr, i)));
        }
        return arrValue;
    }

    @Override
    public final boolean equals(Object obj) {
        if (!(obj instanceof ShortArrayTaint))
            return false;
        return ((ShortArrayTaint) obj).value == value;
        // TODO log anytime values are equal but taint fields are not equal
    }

    @Override
    public final Object clone() {
        return new ShortArrayTaint(value.clone(), taint.clone());
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
