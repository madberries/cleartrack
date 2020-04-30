package pac.wrap;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;

import pac.util.Ret;
import pac.util.Overflows;
import pac.util.TaintValues;

public final class FloatArrayTaint implements Serializable, TaintableArray {
    private static final long serialVersionUID = 3820857917525388483L;

    public float[] value;
    public int[] taint;

    protected FloatArrayTaint(float[] value, int[] taint) {
        this.value = value;
        this.taint = taint;
    }

    public final float load(int idx, int idx_t, Ret ret) throws IOException {
        Overflows.checkOverflow(idx_t, "array-load", "float", null, idx);
        int idxTaint = idx_t & TaintValues.TRUST_MASK;
        if (idxTaint != TaintValues.TRUSTED && (idx < 0 || idx >= taint.length)) { // index out of bounds
            Overflows.outOfBounds("array-load", "float", value, idx);
            ret.taint = TaintValues.TRUSTED;
            return 0.0f; //return default value
        }
        // only set trust bits from the idx taint...
        ret.taint = idxTaint | taint[idx];
        return value[idx];
    }

    public final float load_noCheck(int idx, int idx_t, Ret ret) throws IOException {
        // only set trust bits from the idx taint...
        ret.taint = (idx_t & TaintValues.TRUST_MASK) | taint[idx];
        return value[idx];
    }

    public final void store(int idx, int idx_t, float val, int val_t) throws IOException {
        Overflows.checkOverflow(idx_t, "array-store", "float", val, idx);
        int idxTaint = idx_t & TaintValues.TRUST_MASK;
        if (idxTaint != TaintValues.TRUSTED && (idx < 0 || idx >= taint.length)) { // index out of bounds
            Overflows.outOfBounds("array-store", "boolean", value, idx);
            return; // do not cause a null pointer exception.
        }
        value[idx] = val;
        taint[idx] = val_t | idxTaint;
    }

    public final void store_noCheck(int idx, int idx_t, float val, int val_t) throws IOException {
        value[idx] = val;
        taint[idx] = val_t | (idx_t & TaintValues.TRUST_MASK);
    }

    public static final FloatArrayTaint newArray(int len, int len_t) throws IOException {
        Overflows.checkOverflow(len_t, "new-array", "float", null, len);
        if ((len_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
            len = Overflows.checkAllocSize("float", len);
        return new FloatArrayTaint(new float[len], new int[len]);
    }

    public static final FloatArrayTaint newArray_noCheck(int len, int len_t) throws IOException {
        return new FloatArrayTaint(new float[len], new int[len]);
    }

    public static final FloatArrayTaint[] newArray(int len1, int len1_t, int len2, int len2_t) throws IOException {
        int lenTaint = len1_t | len2_t;
        Overflows.checkOverflow(lenTaint, "new-array", "float", null, len1, len2);
        if ((len1_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
            len1 = Overflows.checkAllocSize("float", len1);
        if ((len2_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
            len2 = Overflows.checkAllocSize("float", len2);
        float[][] value = new float[len1][len2];
        FloatArrayTaint[] taint = new FloatArrayTaint[len1];
        for (int i = 0; i < value.length; i++) {
            taint[i] = new FloatArrayTaint(value[i], new int[len2]);
        }
        return taint;
    }

    public static final FloatArrayTaint[] newArray_noCheck(int len1, int len1_t, int len2, int len2_t)
            throws IOException {
        float[][] value = new float[len1][len2];
        FloatArrayTaint[] taint = new FloatArrayTaint[len1];
        for (int i = 0; i < value.length; i++) {
            taint[i] = new FloatArrayTaint(value[i], new int[len2]);
        }
        return taint;
    }

    public static final FloatArrayTaint[][] newArray(int len1, int len1_t, int len2, int len2_t, int len3, int len3_t)
            throws IOException {
        int lenTaint = len1_t | len2_t | len3_t;
        Overflows.checkOverflow(lenTaint, "new-array", "float", null, len1, len2, len3);
        if ((len1_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
            len1 = Overflows.checkAllocSize("float", len1);
        if ((len2_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
            len2 = Overflows.checkAllocSize("float", len2);
        if ((len3_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
            len3 = Overflows.checkAllocSize("float", len3);
        float[][][] value = new float[len1][len2][len3];
        FloatArrayTaint[][] taint = new FloatArrayTaint[len1][len2];
        for (int i = 0; i < value.length; i++) {
            for (int j = 0; j < value[i].length; j++) {
                taint[i][j] = new FloatArrayTaint(value[i][j], new int[len3]);
            }
        }
        return taint;
    }

    public static final FloatArrayTaint[][] newArray_noCheck(int len1, int len1_t, int len2, int len2_t, int len3,
                                                             int len3_t)
            throws IOException {
        float[][][] value = new float[len1][len2][len3];
        FloatArrayTaint[][] taint = new FloatArrayTaint[len1][len2];
        for (int i = 0; i < value.length; i++) {
            for (int j = 0; j < value[i].length; j++) {
                taint[i][j] = new FloatArrayTaint(value[i][j], new int[len3]);
            }
        }
        return taint;
    }

    public static final FloatArrayTaint toTaintArray(float[] arr) {
        if (arr == null)
            return null;
        int[] taint = new int[arr.length];
        Arrays.fill(taint, TaintValues.UNKNOWN);
        return new FloatArrayTaint(arr, taint);
    }

    public static final FloatArrayTaint[] toTaintArray(float[][] arr) {
        if (arr == null)
            return null;
        FloatArrayTaint[] arrTaint = new FloatArrayTaint[arr.length];
        for (int i = 0; i < arrTaint.length; i++) {
            arrTaint[i] = toTaintArray(arr[i]);
        }
        return arrTaint;
    }

    public static final FloatArrayTaint[][] toTaintArray(float[][][] arr) {
        if (arr == null)
            return null;
        FloatArrayTaint[][] arrTaint = new FloatArrayTaint[arr.length][];
        for (int i = 0; i < arrTaint.length; i++) {
            arrTaint[i] = toTaintArray(arr[i]);
        }
        return arrTaint;
    }

    public static final Object toTaintArray(Object arr) {
        if (arr == null)
            return null;
        int len = Array.getLength(arr);
        if (arr.getClass() == float[].class)
            return new FloatArrayTaint((float[]) arr, new int[len]);
        Object arrValue = Array.newInstance(FloatArrayTaint.class, len);
        for (int i = 0; i < len; i++) {
            Array.set(arrValue, i, toTaintArray(Array.get(arr, i)));
        }
        return arrValue;
    }

    public static final float[] toValueArray(FloatArrayTaint arr) {
        if (arr == null)
            return null;
        return arr.value;
    }

    public static final float[][] toValueArray(FloatArrayTaint[] arr) {
        if (arr == null)
            return null;
        float[][] arrValue = new float[arr.length][];
        for (int i = 0; i < arrValue.length; i++) {
            arrValue[i] = toValueArray(arr[i]);
        }
        return arrValue;
    }

    public static final float[][][] toValueArray(FloatArrayTaint[][] arr) {
        if (arr == null)
            return null;
        float[][][] arrValue = new float[arr.length][][];
        for (int i = 0; i < arrValue.length; i++) {
            arrValue[i] = toValueArray(arr[i]);
        }
        return arrValue;
    }

    public static final Object toValueArray(Object arr) {
        if (arr == null)
            return null;
        if (arr.getClass() == FloatArrayTaint.class)
            return ((TaintableArray) arr).getValue();
        int len = Array.getLength(arr);
        Object arrValue = Array.newInstance(float[].class, len);
        for (int i = 0; i < len; i++) {
            Array.set(arrValue, i, toValueArray(Array.get(arr, i)));
        }
        return arrValue;
    }

    @Override
    public final boolean equals(Object obj) {
        if (!(obj instanceof FloatArrayTaint))
            return false;
        return ((FloatArrayTaint) obj).value == value;
        // TODO log anytime values are equal but taint fields are not equal
    }

    @Override
    public final Object clone() {
        return new FloatArrayTaint(value.clone(), taint.clone());
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
