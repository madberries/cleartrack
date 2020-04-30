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
public class CleartrackInteger {
    public int value;
    public int value_t;

    public CleartrackInteger(int i, int i_t, Ret d) {

    }

    public String toString(Ret dummy) {
        // TODO Auto-generated method stub
        return null;
    }

    public static String toString(int num, int num_t, Ret dummy) {
        // TODO Auto-generated method stub
        return null;
    }

    public int intValue() {
        // TODO Auto-generated method stub
        return 0;
    }

    public int intValue(Ret dummy) {
        // TODO Auto-generated method stub
        return 0;
    }

    public static CleartrackInteger valueOf(int i, int i_t, Ret dummy) {
        return null;
    }

    public static Object toUnknownObject(Integer obj) {
        // TODO Auto-generated method stub
        return null;
    }

    public static Object toUnknownObject(Integer[] obj) {
        // TODO Auto-generated method stub
        return null;
    }

    public static Object toUnknownObject(Integer[][] obj) {
        // TODO Auto-generated method stub
        return null;
    }

    public static Object toUnknownObject(Integer[][][] obj) {
        // TODO Auto-generated method stub
        return null;
    }
}
