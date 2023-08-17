package pac.inst.taint;

import pac.config.BaseConfig;
import pac.config.CleartrackException;
import pac.inst.Instrumentable;
import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationLocation;
import pac.inst.InstrumentationMethod;
import pac.inst.InstrumentationType;
import pac.util.ThreadMonitor;
import pac.util.Ret;
import pac.util.TaintValues;

@InstrumentationClass("java/lang/Object")
public final class ObjectInstrumentation {

  @InstrumentationMethod(instrumentationType = InstrumentationType.INSERT_AFTER,
      instrumentationLocation = InstrumentationLocation.TRANS, name = "getClass",
      descriptor = "()Ljava/lang/Class;")
  public static final Class<?> getOriginalClass(Class<?> c) {
    if (c == CleartrackBoolean.class)
      return Boolean.class;
    if (c == CleartrackByte.class)
      return Byte.class;
    if (c == CleartrackCharacter.class)
      return Character.class;
    if (c == CleartrackShort.class)
      return Short.class;
    if (c == CleartrackInteger.class)
      return Integer.class;
    if (c == CleartrackLong.class)
      return Long.class;
    if (c == CleartrackFloat.class)
      return Float.class;
    if (c == CleartrackDouble.class)
      return Double.class;
    return c;
  }

  @InstrumentationMethod(canExtend = true, skippedDescriptor = "()Ljava/lang/String;",
      name = "toString", descriptor = "(Lpac/util/Ret;)Ljava/lang/String;")
  public static final String toString2(Object obj, Ret ret) {
    return toString(obj, ret);
  }

  @InstrumentationMethod
  public static final String toString(Object obj, Ret ret) {
    if (obj instanceof Instrumentable) {
      return ((Instrumentable) obj).toString(ret);
    }
    if (obj instanceof CleartrackInteger) {
      CleartrackInteger si = (CleartrackInteger) obj;
      si.intValue(ret);
      if ((ret.taint & TaintValues.OVERFLOW) == TaintValues.OVERFLOW) {
        String result = BaseConfig.getInstance().getOverflowReplacement();
        if (result != null)
          return result;
      }
      if ((ret.taint & TaintValues.UNDERFLOW) == TaintValues.UNDERFLOW) {
        String result = BaseConfig.getInstance().getUnderflowReplacement();
        if (result != null)
          return result;
      }
      if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY) {
        String result = BaseConfig.getInstance().getInfinityReplacement();
        if (result != null)
          return result;
      }
      return si.toString(ret);
    }
    if (obj instanceof CleartrackByte) {
      CleartrackByte sb = (CleartrackByte) obj;
      sb.byteValue(ret);
      if ((ret.taint & TaintValues.OVERFLOW) == TaintValues.OVERFLOW) {
        String result = BaseConfig.getInstance().getOverflowReplacement();
        if (result != null)
          return result;
      }
      if ((ret.taint & TaintValues.UNDERFLOW) == TaintValues.UNDERFLOW) {
        String result = BaseConfig.getInstance().getUnderflowReplacement();
        if (result != null)
          return result;
      }
      if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY) {
        String result = BaseConfig.getInstance().getInfinityReplacement();
        if (result != null)
          return result;
      }
      return sb.toString(ret);
    }
    if (obj instanceof CleartrackShort) {
      CleartrackShort ss = (CleartrackShort) obj;
      ss.shortValue(ret);
      if ((ret.taint & TaintValues.OVERFLOW) == TaintValues.OVERFLOW) {
        String result = BaseConfig.getInstance().getOverflowReplacement();
        if (result != null)
          return result;
      }
      if ((ret.taint & TaintValues.UNDERFLOW) == TaintValues.UNDERFLOW) {
        String result = BaseConfig.getInstance().getUnderflowReplacement();
        if (result != null)
          return result;
      }
      if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY) {
        String result = BaseConfig.getInstance().getInfinityReplacement();
        if (result != null)
          return result;
      }
      return ss.toString(ret);
    }
    if (obj instanceof CleartrackCharacter) {
      CleartrackCharacter sc = (CleartrackCharacter) obj;
      sc.charValue(ret);
      if ((ret.taint & TaintValues.OVERFLOW) == TaintValues.OVERFLOW) {
        String result = BaseConfig.getInstance().getOverflowReplacement();
        if (result != null)
          return result;
      }
      if ((ret.taint & TaintValues.UNDERFLOW) == TaintValues.UNDERFLOW) {
        String result = BaseConfig.getInstance().getUnderflowReplacement();
        if (result != null)
          return result;
      }
      if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY) {
        String result = BaseConfig.getInstance().getInfinityReplacement();
        if (result != null)
          return result;
      }
      return sc.toString(ret);
    }
    if (obj instanceof CleartrackLong) {
      CleartrackLong sl = (CleartrackLong) obj;
      sl.longValue(ret);
      if ((ret.taint & TaintValues.OVERFLOW) == TaintValues.OVERFLOW) {
        String result = BaseConfig.getInstance().getOverflowReplacement();
        if (result != null)
          return result;
      }
      if ((ret.taint & TaintValues.UNDERFLOW) == TaintValues.UNDERFLOW) {
        String result = BaseConfig.getInstance().getUnderflowReplacement();
        if (result != null)
          return result;
      }
      if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY) {
        String result = BaseConfig.getInstance().getInfinityReplacement();
        if (result != null)
          return result;
      }
      return sl.toString(ret);
    }
    if (obj instanceof CleartrackDouble) {
      CleartrackDouble sd = (CleartrackDouble) obj;
      sd.doubleValue(ret);
      if ((ret.taint & TaintValues.OVERFLOW) == TaintValues.OVERFLOW) {
        String result = BaseConfig.getInstance().getOverflowReplacement();
        if (result != null)
          return result;
      }
      if ((ret.taint & TaintValues.UNDERFLOW) == TaintValues.UNDERFLOW) {
        String result = BaseConfig.getInstance().getUnderflowReplacement();
        if (result != null)
          return result;
      }
      if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY) {
        String result = BaseConfig.getInstance().getInfinityReplacement();
        if (result != null)
          return result;
      }
      return sd.toString(ret);
    }
    if (obj instanceof CleartrackFloat) {
      CleartrackFloat sf = (CleartrackFloat) obj;
      sf.floatValue(ret);
      if ((ret.taint & TaintValues.OVERFLOW) == TaintValues.OVERFLOW) {
        String result = BaseConfig.getInstance().getOverflowReplacement();
        if (result != null)
          return result;
      }
      if ((ret.taint & TaintValues.UNDERFLOW) == TaintValues.UNDERFLOW) {
        String result = BaseConfig.getInstance().getUnderflowReplacement();
        if (result != null)
          return result;
      }
      if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY) {
        String result = BaseConfig.getInstance().getInfinityReplacement();
        if (result != null)
          return result;
      }
      return sf.toString(ret);
    }
    return obj.toString();
  }

  @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP)
  public static final void wait(Object obj) throws InterruptedException {
    // Monitors have been replaced by ReentrantLocks, so call our version of this method.
    try {
      ThreadMonitor.INSTANCE._wait(obj);
    } catch (InterruptedException e) {
      try {
        ThreadMonitor.INSTANCE.handleInterrupts();
      } catch (CleartrackException se) {
        // Need to set the bit back in case they catch the CleartrackException.
        Thread.currentThread().interrupt();
        throw se;
      }
      throw e; // Not our interrupt, so throw it.
    }
  }

  @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP)
  public static final void wait(Object obj, long timeout) throws InterruptedException {
    // Monitors have been replaced by ReentrantLocks, so call our version of this method.
    try {
      ThreadMonitor.INSTANCE._wait(obj, timeout);
    } catch (InterruptedException e) {
      try {
        ThreadMonitor.INSTANCE.handleInterrupts();
      } catch (CleartrackException se) {
        // Need to set the bit back in case they catch the CleartrackException.
        Thread.currentThread().interrupt();
        throw se;
      }
      throw e; // Not our interrupt, so throw it.
    }
  }

  @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP)
  public static final void wait(Object obj, long timeout, int nanos) throws InterruptedException {
    // Monitors have been replaced by ReentrantLocks, so call our version of this method.
    try {
      ThreadMonitor.INSTANCE._wait(obj, timeout, nanos);
    } catch (InterruptedException e) {
      try {
        ThreadMonitor.INSTANCE.handleInterrupts();
      } catch (CleartrackException se) {
        // Need to set the bit back in case they catch the CleartrackException.
        Thread.currentThread().interrupt();
        throw se;
      }
      throw e; // Not our interrupt, so throw it.
    }
  }

  @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP)
  public static final void notify(Object obj) {
    // Monitors have been replaced by ReentrantLocks, so call our version of this method.
    ThreadMonitor.INSTANCE._notify(obj);
  }

  @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP)
  public static final void notifyAll(Object obj) {
    // Monitors have been replaced by ReentrantLocks, so call our version of this method.
    ThreadMonitor.INSTANCE._notifyAll(obj);
  }

}
