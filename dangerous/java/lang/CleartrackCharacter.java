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
public class CleartrackCharacter {
    public char value;
    public int value_t;

    public CleartrackCharacter(char c, int c_t, Ret dummy) {
        // TODO Auto-generated constructor stub
    }

    public String toString(Ret dummy) {
        // TODO Auto-generated method stub
        return null;
    }

    public static String toString(char c, int c_t, Ret dummy) {
        // TODO Auto-generated method stub
        return null;
    }

    public char charValue() {
        // TODO Auto-generated method stub
        return 0;
    }

    public char charValue(Ret dummy) {
        // TODO Auto-generated method stub
        return 0;
    }

    public static CleartrackCharacter valueOf(char c, int c_t, Ret dummy) {
        return null;
    }

    public static Object toUnknownObject(Character obj) {
        // TODO Auto-generated method stub
        return null;
    }

    public static Object toUnknownObject(Character[] obj) {
        // TODO Auto-generated method stub
        return null;
    }

    public static Object toUnknownObject(Character[][] obj) {
        // TODO Auto-generated method stub
        return null;
    }

    public static Object toUnknownObject(Character[][][] obj) {
        // TODO Auto-generated method stub
        return null;
    }
}
