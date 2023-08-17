package pac.inst.taint;

import pac.config.Notify;
import pac.config.NotifyMsg;
import pac.config.RunChecks;
import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.inst.InvocationType;
import pac.util.Ret;
import pac.util.TaintUtils;
import pac.util.TaintValues;

@InstrumentationClass("java/lang/CleartrackShort")
public final class ShortInstrumentation {
  public static final short DEFAULT_VALUE = 0;

  @InstrumentationMethod(invocationType = InvocationType.STATIC, name = "valueOf",
      descriptor = "(SILpac/util/Ret;)Ljava/lang/Short;")
  public static final CleartrackShort valueOf(short s, int s_t, Ret ret) {
    return new CleartrackShort(s, s_t, ret);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final short parseShort(String s, Ret ret) {
    if (s == null) { // This is what the JDK does for nulls.
      throw new NumberFormatException("null");
    }
    int taint = TaintUtils.isTrusted(s) ? TaintValues.TRUSTED : TaintValues.TAINTED;
    try {
      ret.taint = taint;
      return Short.parseShort(s);
    } catch (NumberFormatException e) {
      if (TaintUtils.isTainted(s)) {
        NotifyMsg notifyMsg =
            new NotifyMsg("Short.parseShort(String)", "Short.parseShort(" + s + ")", 20);
        notifyMsg.setAction(RunChecks.REPLACE_ACTION);
        notifyMsg.append("Tainted string \"" + s
            + "\" is not a parseable short (returning the default value: " + DEFAULT_VALUE + ")");
        Notify.notifyAndRespond(notifyMsg);
        return DEFAULT_VALUE;
      }
      throw e; // Throw the original exception on trusted strings.
    }
  }
}
