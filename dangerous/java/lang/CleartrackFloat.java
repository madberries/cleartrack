package java.lang;

import pac.util.Ret;

/**
 * This is a fake skeleton class representing an instrumented dangerous class. Since these class are
 * generated (and needed) at instrumentation time, we must provide a skeleton class so that the
 * instrumentation can complete without runtime errors.
 * 
 * @author jeikenberry
 */
public class CleartrackFloat {
  public float value;
  public int value_t;

  public CleartrackFloat(float f, int f_t, Ret dummy) {
    // TODO Auto-generated constructor stub
  }

  public float floatValue() {
    // TODO Auto-generated method stub
    return 0;
  }

  public float floatValue(Ret dummy) {
    // TODO Auto-generated method stub
    return 0;
  }

  public String toString(Ret dummy) {
    // TODO Auto-generated method stub
    return null;
  }

  public static String toString(float f, int f_t, Ret dummy) {
    // TODO Auto-generated method stub
    return null;
  }

  public static String toHexString(float f, int f_t, Ret dummy) {
    // TODO Auto-generated method stub
    return null;
  }

  public static CleartrackFloat valueOf(float f, int f_t, Ret dummy) {
    return null;
  }

  public static Object toUnknownObject(Float obj) {
    // TODO Auto-generated method stub
    return null;
  }

  public static Object toUnknownObject(Float[] obj) {
    // TODO Auto-generated method stub
    return null;
  }

  public static Object toUnknownObject(Float[][] obj) {
    // TODO Auto-generated method stub
    return null;
  }

  public static Object toUnknownObject(Float[][][] obj) {
    // TODO Auto-generated method stub
    return null;
  }
}
