package pac.inst.taint;

import pac.config.Notify;
import pac.config.NotifyMsg;
import pac.config.RunChecks;
import pac.config.BaseConfig;
import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.inst.InvocationType;
import pac.util.Ret;
import pac.util.TaintUtils;
import pac.util.TaintValues;

@InstrumentationClass("java/lang/CleartrackDouble")
public final class DoubleInstrumentation {
  public static final double DEFAULT_VALUE = 0d;

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final long doubleToRawLongBits(double d, int d_t, Ret ret) {
    ret.taint = d_t;
    return Double.doubleToRawLongBits(d);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final long doubleToLongBits(double d, int d_t, Ret ret) {
    ret.taint = d_t;
    return Double.doubleToLongBits(d);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double longBitsToDouble(long l, int l_t, Ret ret) {
    ret.taint = l_t;
    return Double.longBitsToDouble(l);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC, name = "valueOf",
      descriptor = "(DILpac/util/Ret;)Ljava/lang/Double;")
  public static final CleartrackDouble valueOf(double d, int d_t, Ret ret) {
    return new CleartrackDouble(d, d_t, ret);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final String toString(double d, int d_t, Ret ret) {
    if ((d_t & TaintValues.INFINITY) == TaintValues.INFINITY) {
      String result = BaseConfig.getInstance().getInfinityReplacement();
      if (result != null)
        return result;
    }
    String s = CleartrackDouble.toString(d, d_t, ret);
    TaintUtils.mark(s, d_t, 0, s.length() - 1);
    return s;
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final String toHexString(double d, int d_t, Ret ret) {
    if ((d_t & TaintValues.INFINITY) == TaintValues.INFINITY) {
      String result = BaseConfig.getInstance().getInfinityReplacement();
      if (result != null)
        return result;
    }
    String s = CleartrackDouble.toHexString(d, d_t, ret);
    TaintUtils.mark(s, d_t, 0, s.length() - 1);
    return s;
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double parseDouble(String s, Ret ret) {
    int taint = TaintUtils.isTrusted(s) ? TaintValues.TRUSTED : TaintValues.TAINTED;
    try {
      ret.taint = taint;
      return Double.parseDouble(s);
    } catch (NumberFormatException e) {
      if (TaintUtils.isTainted(s)) {
        NotifyMsg notifyMsg =
            new NotifyMsg("Double.parseDouble(String)", "Double.parseDouble(" + s + ")", 20);
        notifyMsg.setAction(RunChecks.REPLACE_ACTION);
        notifyMsg.append("Tainted string \"" + s
            + "\" is not a parseable double (returning the default value: " + DEFAULT_VALUE + ")");
        Notify.notifyAndRespond(notifyMsg);
        return DEFAULT_VALUE;
      }
      throw e; // Throw the original exception on trusted strings.
    }
  }
}
