package java.lang;

import pac.util.Ret;

/**
 * This is a fake skeleton class representing an instrumented dangerous class. Since these class are
 * generated (and needed) at instrumentation time, we must provide a skeleton class so that the
 * instrumentation can complete without runtime errors.
 * 
 * @author jeikenberry
 */
public class CleartrackLong {
  public long value;
  public int value_t;

  public CleartrackLong(long l, int l_t, Ret dummy) {
    // TODO Auto-generated constructor stub
  }

  public long longValue() {
    // TODO Auto-generated method stub
    return 0;
  }

  public long longValue(Ret dummy) {
    // TODO Auto-generated method stub
    return 0;
  }

  public String toString(Ret dummy) {
    // TODO Auto-generated method stub
    return null;
  }

  public static String toString(long num, int num_t, Ret dummy) {
    // TODO Auto-generated method stub
    return null;
  }

  public static CleartrackLong valueOf(long l, int l_t, Ret dummy) {
    return null;
  }

  public static Object toUnknownObject(Long obj) {
    // TODO Auto-generated method stub
    return null;
  }

  public static Object toUnknownObject(Long[] obj) {
    // TODO Auto-generated method stub
    return null;
  }

  public static Object toUnknownObject(Long[][] obj) {
    // TODO Auto-generated method stub
    return null;
  }

  public static Object toUnknownObject(Long[][][] obj) {
    // TODO Auto-generated method stub
    return null;
  }
}
