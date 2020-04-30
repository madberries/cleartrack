package pac.wrap;

import java.io.IOException;

import pac.util.Overflows;
import pac.util.TaintValues;

/**
 * Utility class for handling arrays of unknown array types (i.e. when the
 * array is of type Object, Object[], etc...)
 * 
 * @author jeikenberry
 */
public final class ArrayTaint {

    /**
     * Called prior to any array accesses to ensure that the index supplied had not
     * been overflowed or underflowed.
     * 
     * @param idx int of the array index
     * @param idxTaint int of the taint associated with idx
     * @param storedObj Object we are storing into the array (if store instruction),
     *      otherwise null.
     * @return int of the index
     * @throws IOException
     */
    public final static void store(Object[] array, int idx, int idxTaint, Object value) throws IOException {
        if ((idxTaint & TaintValues.TRUST_MASK) != TaintValues.TRUSTED) {
            Overflows.checkOverflow(idxTaint, "array-store", "object", value, idx);
            if (idx < 0 || idx > array.length) { // index out of bounds
                Overflows.outOfBounds("array-store", "boolean", value, idx);
                return; // do not cause a null pointer exception.
            }
        }
        array[idx] = value;
    }

    public final static Object load(Object[] array, int idx, int idxTaint) throws IOException {
        if ((idxTaint & TaintValues.TRUST_MASK) != TaintValues.TRUSTED) {
            Overflows.checkOverflow(idxTaint, "array-load", "object", null, idx);
            if (idx < 0 || idx > array.length) { // index out of bounds
                Overflows.outOfBounds("array-load", "boolean", null, idx);
                return null;
            }
        }
        return array[idx];
    }

    /**
     * Called prior to any new array on any non-primitive type to ensure that the
     * length had not overflowed or underflowed.
     * 
     * @param len the array length
     * @param len_t the taint of array length
     * @param dimension int of the dimension to check
     * @return int of the index
     * @throws IOException
     */
    public final static int validateNewArrayLength(int len, int len_t, int dimension) throws IOException {
        if ((len_t & TaintValues.TRUST_MASK) == TaintValues.TRUSTED)
            return len; // Do not run checks on trusted length values
        Overflows.checkOverflow(len_t, "object", null, len);
        return Overflows.checkAllocSize("object", len);
    }

    /**
     * Takes an object and constructs an unknown taint array from that object
     * (provided that the object is a primitive array).
     * 
     * @param obj
     * @return a new taint array representing obj of unknown taint, or
     * 	obj if obj is not a primitive array.
     */
    public final static Object toTaintArray(Object obj) {
        if (obj == null)
            return null;

        Class<?> objClass = obj.getClass();
        if (objClass == char[].class)
            return CharArrayTaint.toTaintArray((char[]) obj);
        if (objClass == byte[].class)
            return ByteArrayTaint.toTaintArray((byte[]) obj);
        if (objClass == int[].class)
            return IntArrayTaint.toTaintArray((int[]) obj);
        if (objClass == boolean[].class)
            return BooleanArrayTaint.toTaintArray((boolean[]) obj);
        if (objClass == short[].class)
            return ShortArrayTaint.toTaintArray((short[]) obj);
        if (objClass == float[].class)
            return FloatArrayTaint.toTaintArray((float[]) obj);
        if (objClass == long[].class)
            return LongArrayTaint.toTaintArray((long[]) obj);
        if (objClass == double[].class)
            return DoubleArrayTaint.toTaintArray((double[]) obj);

        if (objClass == char[][].class)
            return CharArrayTaint.toTaintArray((char[][]) obj);
        if (objClass == byte[][].class)
            return ByteArrayTaint.toTaintArray((byte[][]) obj);
        if (objClass == int[][].class)
            return IntArrayTaint.toTaintArray((int[][]) obj);
        if (objClass == boolean[][].class)
            return BooleanArrayTaint.toTaintArray((boolean[][]) obj);
        if (objClass == short[][].class)
            return ShortArrayTaint.toTaintArray((short[][]) obj);
        if (objClass == float[][].class)
            return FloatArrayTaint.toTaintArray((float[][]) obj);
        if (objClass == long[][].class)
            return LongArrayTaint.toTaintArray((long[][]) obj);
        if (objClass == double[][].class)
            return DoubleArrayTaint.toTaintArray((double[][]) obj);

        if (objClass == char[][][].class)
            return CharArrayTaint.toTaintArray((char[][][]) obj);
        if (objClass == byte[][][].class)
            return ByteArrayTaint.toTaintArray((byte[][][]) obj);
        if (objClass == int[][][].class)
            return IntArrayTaint.toTaintArray((int[][][]) obj);
        if (objClass == boolean[][][].class)
            return BooleanArrayTaint.toTaintArray((boolean[][][]) obj);
        if (objClass == short[][][].class)
            return ShortArrayTaint.toTaintArray((short[][][]) obj);
        if (objClass == float[][][].class)
            return FloatArrayTaint.toTaintArray((float[][][]) obj);
        if (objClass == long[][][].class)
            return LongArrayTaint.toTaintArray((long[][][]) obj);
        if (objClass == double[][][].class)
            return DoubleArrayTaint.toTaintArray((double[][][]) obj);

        // FIXME technically we need to perform this also on Object[][],
        // Object[][][], etc... but let's just leave this for now and assume
        // it doesn't really happen in the real world...
        //		if (objClass == Object[].class) {
        //		    // this object array may contain a primitive array as an element
        //            // so let's call toTaintArray() on each of it's elements.
        //		    Object[] arr = (Object[]) obj;
        //		    for (int i = 0; i < arr.length; i++)
        //		        arr[i] = toTaintArray(arr[i]);
        //		}

        return obj;
    }

    /**
     * Takes an object and obtains the value array from that object
     * (provided that the object is indeed a taint wrapped primitive
     * array type).
     * 
     * @param obj
     * @return the value array from obj, or obj if obj is not a taint
     * 	wrapped primitive array).
     */
    public final static Object toValueArray(Object obj) {
        if (obj == null)
            return null;

        Class<?> objClass = obj.getClass();
        if (objClass == CharArrayTaint.class)
            return CharArrayTaint.toValueArray((CharArrayTaint) obj);
        if (objClass == BooleanArrayTaint.class)
            return BooleanArrayTaint.toValueArray((BooleanArrayTaint) obj);
        if (objClass == ByteArrayTaint.class)
            return ByteArrayTaint.toValueArray((ByteArrayTaint) obj);
        if (objClass == IntArrayTaint.class)
            return IntArrayTaint.toValueArray((IntArrayTaint) obj);
        if (objClass == ShortArrayTaint.class)
            return ShortArrayTaint.toValueArray((ShortArrayTaint) obj);
        if (objClass == FloatArrayTaint.class)
            return FloatArrayTaint.toValueArray((FloatArrayTaint) obj);
        if (objClass == LongArrayTaint.class)
            return LongArrayTaint.toValueArray((LongArrayTaint) obj);
        if (objClass == DoubleArrayTaint.class)
            return DoubleArrayTaint.toValueArray((DoubleArrayTaint) obj);

        if (objClass == CharArrayTaint[].class)
            return CharArrayTaint.toValueArray((CharArrayTaint[]) obj);
        if (objClass == BooleanArrayTaint[].class)
            return BooleanArrayTaint.toValueArray((BooleanArrayTaint[]) obj);
        if (objClass == ByteArrayTaint[].class)
            return ByteArrayTaint.toValueArray((ByteArrayTaint[]) obj);
        if (objClass == IntArrayTaint[].class)
            return IntArrayTaint.toValueArray((IntArrayTaint[]) obj);
        if (objClass == ShortArrayTaint[].class)
            return ShortArrayTaint.toValueArray((ShortArrayTaint[]) obj);
        if (objClass == FloatArrayTaint[].class)
            return FloatArrayTaint.toValueArray((FloatArrayTaint[]) obj);
        if (objClass == LongArrayTaint[].class)
            return LongArrayTaint.toValueArray((LongArrayTaint[]) obj);
        if (objClass == DoubleArrayTaint[].class)
            return DoubleArrayTaint.toValueArray((DoubleArrayTaint[]) obj);

        if (objClass == CharArrayTaint[][].class)
            return CharArrayTaint.toValueArray((CharArrayTaint[][]) obj);
        if (objClass == BooleanArrayTaint[][].class)
            return BooleanArrayTaint.toValueArray((BooleanArrayTaint[][]) obj);
        if (objClass == ByteArrayTaint[][].class)
            return ByteArrayTaint.toValueArray((ByteArrayTaint[][]) obj);
        if (objClass == IntArrayTaint[][].class)
            return IntArrayTaint.toValueArray((IntArrayTaint[][]) obj);
        if (objClass == ShortArrayTaint[][].class)
            return ShortArrayTaint.toValueArray((ShortArrayTaint[][]) obj);
        if (objClass == FloatArrayTaint[][].class)
            return FloatArrayTaint.toValueArray((FloatArrayTaint[][]) obj);
        if (objClass == LongArrayTaint[][].class)
            return LongArrayTaint.toValueArray((LongArrayTaint[][]) obj);
        if (objClass == DoubleArrayTaint[][].class)
            return DoubleArrayTaint.toValueArray((DoubleArrayTaint[][]) obj);

        //		// FIXME technically we need to perform this also on Object[][],
        //		// Object[][][], etc... but let's just leave this for now and assume
        //		// it doesn't really happen in the real world...
        //        if (objClass == Object[].class) {
        //            // this object array may contain a primitive array as an element
        //            // so let's call toValueArray() on each of it's elements.
        //            Object[] arr = (Object[]) obj;
        //            for (int i = 0; i < arr.length; i++)
        //                arr[i] = toValueArray(arr[i]);
        //        }

        return obj;
    }

    /**
     * @param arr Object of the array
     * @return Trusted integer represented the length of arr.
     */
    public final static int length(Object arr) {
        if (arr instanceof TaintableArray)
            return ((TaintableArray) arr).length();
        return ((Object[]) arr).length;
    }

    /*
     * new arrays of where the actual dim and total dim match, call the above methods
     * o.w. 
     * 		anewarray [prim -> anewarray PrimArrayTaint
     * 		anewarray [[prim -> anewarray [PrimArrayTaint
     * 		multianewarray [[[prim -> multianewarray [[PrimArrayTaint
     * 		etc...
     * 
     * aastore / aaload will remain the same
     * iaload, baload, etc... will become IntArrayTaint.load(), ByteArrayTaint.load()
     * iastore, bastore, etc... will become IntArrayTaint.store(), ByteArrayTaint.store()
     * arraylength on primitive array will become ArrayTaint.length()
     * checkcast on primitive array will checkcast on the taint 
     * 		(i.e. [I -> pac/util/IntArrayTaint, [[I -> [Lpac/util/IntArrayTaint;)  [getInternalName()]
     * 		this will work because an object will always be compatible with IntArrayTaint for example
     * 		and object[] will always be compatible with IntArrayTaint[]
     * instanceof same as checkcast
     * 
     * original method -
     * 		get field / static - leave unchanged
     * 		put field / static - need to put original array as well as taint array (toTaintArray on original)
     * 			unless it's a constant (or always trusted) - if so we just put the original value
     * 
     * copied method -
     * 		get field / static - get the taint field (or if it's a constant call toTaintArray on value)
     * 		put field / static - need to put original array as well as taint array (toValueArray on taint)
     * 			unless it's a constant (or always trusted) - if so we just put the original value
     * 
     * native method / uninst method -
     * 		get the original array field from the wrapped object
     * 			and have it pushed to the method call (toValueArray)
     * 		if the method returns an array, wrapped it in an object (toTaintArray)
     * 		check if ArrayTaint.isArrayInstance()
     * 
     * monitors - if it's an array we wrapped then acquire the array field)
     * array methods -  [I.clone() -> Lpac/util/array/IntArrayTaint;.clone()
     */
    
}
