package java.lang;

import pac.util.Ret;

/**
 * This is a fake skeleton class representing an instrumented dangerous class.
 * Since these class are generated (and needed) at instrumentation time, we must
 * provide a skeleton class so that the instrumentation can complete without
 * runtime errors.
 * 
 * @author jeikenberry
 */
public class CleartrackShort {
    public short value;
    public int value_t;

    public CleartrackShort(short s, int s_t, Ret dummy) {
        // TODO Auto-generated constructor stub
    }

    public short shortValue() {
        // TODO Auto-generated method stub
        return 0;
    }

    public String toString(Ret dummy) {
        // TODO Auto-generated method stub
        return null;
    }

    public short shortValue(Ret dummy) {
        // TODO Auto-generated method stub
        return 0;
    }

    public static CleartrackShort valueOf(short s, int s_t, Ret dummy) {
        return null;
    }

    public static Object toUnknownObject(Short obj) {
        // TODO Auto-generated method stub
        return null;
    }

    public static Object toUnknownObject(Short[] obj) {
        // TODO Auto-generated method stub
        return null;
    }

    public static Object toUnknownObject(Short[][] obj) {
        // TODO Auto-generated method stub
        return null;
    }

    public static Object toUnknownObject(Short[][][] obj) {
        // TODO Auto-generated method stub
        return null;
    }
}
