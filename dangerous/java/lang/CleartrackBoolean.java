package java.lang;

import pac.util.Ret;

/**
 * This is a fake skeleton class representing an instrumented dangerous class. Since these class are
 * generated (and needed) at instrumentation time, we must provide a skeleton class so that the
 * instrumentation can complete without runtime errors.
 * 
 * @author jeikenberry
 */
public class CleartrackBoolean {
  public boolean value;
  public int value_t;

  public CleartrackBoolean(boolean b, int b_t, Ret dummy) {
    // TODO Auto-generated constructor stub
  }

  public boolean booleanValue() {
    // TODO Auto-generated method stub
    return false;
  }

  public boolean booleanValue(Ret dummy) {
    // TODO Auto-generated method stub
    return false;
  }

  public static CleartrackBoolean valueOf(boolean b, int b_t, Ret dummy) {
    return null;
  }

  public static Object toUnknownObject(Boolean obj) {
    // TODO Auto-generated method stub
    return null;
  }

  public static Object toUnknownObject(Boolean[] obj) {
    // TODO Auto-generated method stub
    return null;
  }

  public static Object toUnknownObject(Boolean[][] obj) {
    // TODO Auto-generated method stub
    return null;
  }

  public static Object toUnknownObject(Boolean[][][] obj) {
    // TODO Auto-generated method stub
    return null;
  }
}
