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

@InstrumentationClass("java/lang/CleartrackFloat")
public final class FloatInstrumentation {
  public static final float DEFAULT_VALUE = 0f;

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final int floatToRawIntBits(float f, int f_t, Ret ret) {
    ret.taint = f_t;
    return Float.floatToRawIntBits(f);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final int floatToIntBits(float f, int f_t, Ret ret) {
    ret.taint = f_t;
    return Float.floatToIntBits(f);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC, name = "valueOf",
      descriptor = "(FILpac/util/Ret;)Ljava/lang/Float;")
  public static final CleartrackFloat valueOf(float f, int f_t, Ret ret) {
    return new CleartrackFloat(f, f_t, ret);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final String toString(float f, int f_t, Ret ret) {
    if ((f_t & TaintValues.INFINITY) == TaintValues.INFINITY) {
      String result = BaseConfig.getInstance().getInfinityReplacement();
      if (result != null)
        return result;
    }
    String s = CleartrackFloat.toString(f, f_t, ret);
    TaintUtils.mark(s, f_t, 0, s.length() - 1);
    return s;
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final String toHexString(float f, int f_t, Ret ret) {
    if ((f_t & TaintValues.INFINITY) == TaintValues.INFINITY) {
      String result = BaseConfig.getInstance().getInfinityReplacement();
      if (result != null)
        return result;
    }
    String s = CleartrackFloat.toHexString(f, f_t, ret);
    TaintUtils.mark(s, f_t, 0, s.length() - 1);
    return s;
  }

  @InstrumentationMethod(descriptor = "()Ljava/lang/String;")
  public static final String toString(CleartrackFloat fObj, Ret ret) {
    fObj.floatValue(ret);
    int f_t = ret.taint;
    if ((f_t & TaintValues.INFINITY) == TaintValues.INFINITY) {
      String result = BaseConfig.getInstance().getInfinityReplacement();
      if (result != null)
        return result;
    }
    String s = fObj.toString(ret);
    TaintUtils.mark(s, f_t, 0, s.length() - 1);
    return s;
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final float parseFloat(String s, Ret ret) {
    int taint = TaintUtils.isTrusted(s) ? TaintValues.TRUSTED : TaintValues.TAINTED;
    try {
      ret.taint = taint;
      return Float.parseFloat(s);
    } catch (NumberFormatException e) {
      if (TaintUtils.isTainted(s)) {
        NotifyMsg notifyMsg =
            new NotifyMsg("Float.parseFloat(String)", "Float.parseFloat(" + s + ")", 20);
        notifyMsg.setAction(RunChecks.REPLACE_ACTION);
        notifyMsg.append("Tainted string \"" + s
            + "\" is not a parseable float (returning the default value: " + DEFAULT_VALUE + ")");
        Notify.notifyAndRespond(notifyMsg);
        return DEFAULT_VALUE;
      }
      throw e; // Throw the original exception on trusted strings.
    }
  }
}
