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

@InstrumentationClass("java/lang/CleartrackByte")
public final class ByteInstrumentation {
  public static final byte DEFAULT_VALUE = 0;

  @InstrumentationMethod(invocationType = InvocationType.STATIC, name = "valueOf",
      descriptor = "(BILpac/util/Ret;)Ljava/lang/Byte;")
  public static final CleartrackByte valueOf(byte b, int b_t, Ret ret) {
    return new CleartrackByte(b, b_t, ret);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final byte parseByte(String s, Ret ret) {
    if (s == null) { // This is what the JDK does for nulls.
      throw new NumberFormatException("null");
    }
    int taint = TaintUtils.isTrusted(s) ? TaintValues.TRUSTED : TaintValues.TAINTED;
    try {
      ret.taint = taint;
      return Byte.parseByte(s);
    } catch (NumberFormatException e) {
      if (TaintUtils.isTainted(s)) {
        NotifyMsg notifyMsg =
            new NotifyMsg("Byte.parseByte(String)", "Byte.parseByte(" + s + ")", 20);
        notifyMsg.setAction(RunChecks.REPLACE_ACTION);
        notifyMsg.append("Tainted string \"" + s
            + "\" is not a parseable byte (returning the default value: " + DEFAULT_VALUE + ")");
        Notify.notifyAndRespond(notifyMsg);
        return DEFAULT_VALUE;
      }
      throw e; // Throw the original exception on trusted strings.
    }
  }
}
