package java.lang;

import pac.util.Ret;

/**
 * This is a fake skeleton class representing an instrumented dangerous class. Since these class are
 * generated (and needed) at instrumentation time, we must provide a skeleton class so that the
 * instrumentation can complete without runtime errors.
 * 
 * @author jeikenberry
 */
public class CleartrackDouble {
  public double value;
  public int value_t;

  public CleartrackDouble(double d, int d_dup, Ret dummy) {
    // TODO Auto-generated constructor stub
  }

  public double doubleValue() {
    // TODO Auto-generated method stub
    return 0;
  }

  public double doubleValue(Ret dummy) {
    // TODO Auto-generated method stub
    return 0;
  }

  public String toString(Ret dummy) {
    // TODO Auto-generated method stub
    return null;
  }

  public static String toString(double d, int d_t, Ret dummy) {
    // TODO Auto-generated method stub
    return null;
  }

  public static String toHexString(double d, int d_t, Ret dummy) {
    // TODO Auto-generated method stub
    return null;
  }

  public static CleartrackDouble valueOf(double d, int d_t, Ret dummy) {
    return null;
  }

  public static Object toUnknownObject(Double obj) {
    // TODO Auto-generated method stub
    return null;
  }

  public static Object toUnknownObject(Double[] obj) {
    // TODO Auto-generated method stub
    return null;
  }

  public static Object toUnknownObject(Double[][] obj) {
    // TODO Auto-generated method stub
    return null;
  }

  public static Object toUnknownObject(Double[][][] obj) {
    // TODO Auto-generated method stub
    return null;
  }
}
