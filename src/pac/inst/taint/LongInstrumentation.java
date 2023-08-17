package pac.inst.taint;

import pac.config.BaseConfig;
import pac.config.Notify;
import pac.config.NotifyMsg;
import pac.config.RunChecks;
import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.inst.InvocationType;
import pac.util.Ret;
import pac.util.TaintUtils;
import pac.util.TaintValues;

@InstrumentationClass("java/lang/CleartrackLong")
public final class LongInstrumentation {
  public static final long DEFAULT_VALUE = 0L;

  @InstrumentationMethod(invocationType = InvocationType.STATIC, name = "valueOf",
      descriptor = "(JILpac/util/Ret;)Ljava/lang/Long;")
  public static final CleartrackLong valueOf(long l, int l_t, Ret ret) {
    return new CleartrackLong(l, l_t, ret);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final long parseLong(String s, Ret ret) {
    if (s == null) { // This is what the JDK does for nulls.
      throw new NumberFormatException("null");
    }
    int taint = TaintUtils.isTrusted(s) ? TaintValues.TRUSTED : TaintValues.TAINTED;
    try {
      ret.taint = taint;
      return Long.parseLong(s);
    } catch (NumberFormatException e) {
      if (TaintUtils.isTainted(s)) {
        NotifyMsg notifyMsg =
            new NotifyMsg("Long.parseLong(String)", "Long.parseLong(" + s + ")", 20);
        notifyMsg.setAction(RunChecks.REPLACE_ACTION);
        notifyMsg.append("Tainted string \"" + s
            + "\" is not a parseable long (returning the default value: " + DEFAULT_VALUE + ")");
        Notify.notifyAndRespond(notifyMsg);
        return DEFAULT_VALUE;
      }
      throw e; // Throw the original exception on trusted strings.
    }
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final String toString(long num, int num_t, Ret ret) {
    if ((num_t & TaintValues.OVERFLOW) == TaintValues.OVERFLOW) {
      String result = BaseConfig.getInstance().getOverflowReplacement();
      if (result != null)
        return result;
    }
    if ((num_t & TaintValues.UNDERFLOW) == TaintValues.UNDERFLOW) {
      String result = BaseConfig.getInstance().getUnderflowReplacement();
      if (result != null)
        return result;
    }
    if ((num_t & TaintValues.INFINITY) == TaintValues.INFINITY) {
      String result = BaseConfig.getInstance().getInfinityReplacement();
      if (result != null)
        return result;
    }
    return CleartrackLong.toString(num, num_t, ret);
  }
}
