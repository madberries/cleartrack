package pac.inst.taint;

import java.math.BigDecimal;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.util.Ret;
import pac.util.TaintUtils;

@InstrumentationClass("java/math/BigDecimal")
public final class BigDecimalInstrumentation {

  @InstrumentationMethod
  public static final BigDecimal divide(BigDecimal num, BigDecimal divisor, Ret ret) {
    BigDecimal res = num.divide(divisor);
    String str = res.toString();
    num.intValue(ret);
    int taint = ret.taint;
    divisor.intValue(ret);
    taint |= ret.taint;
    TaintUtils.mark(str, taint, 0, str.length() - 1);
    return new BigDecimal(str, ret);
  }

}
