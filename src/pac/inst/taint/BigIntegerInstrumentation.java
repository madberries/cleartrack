package pac.inst.taint;

import java.math.BigInteger;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.util.Ret;
import pac.util.TaintUtils;
import pac.util.TaintValues;

@InstrumentationClass("java/math/BigInteger")
public final class BigIntegerInstrumentation {

    @InstrumentationMethod
    public static final BigInteger pow(BigInteger base, int exp, int exp_t, Ret ret) {
        BigInteger res = base.pow(exp);
        String str = res.toString();
        base.intValue(ret);
        TaintUtils.mark(str, ret.taint | exp_t, 0, str.length() - 1);
        return new BigInteger(str, ret);
    }

    private static final int invertBits(int num) {
        if (num == 0)
            return Integer.MIN_VALUE;
        return -(num + 1);
    }

    @InstrumentationMethod
    public static final int intValue(BigInteger b, Ret ret) {
        int i = b.intValue(ret);
        int i_t = ret.taint;
        if (b.mag.length > 1) {
            i_t = i_t | TaintValues.OVERFLOW;
        } else {
            i_t = i_t & invertBits(TaintValues.OVERFLOW_MASK);
        }
        ret.taint = i_t;
        return i;

    }

    @InstrumentationMethod
    public static final long longValue(BigInteger b, Ret ret) {
        long l = b.longValue(ret);
        int l_t = ret.taint;
        if (b.mag.length > 2) {
            l_t = l_t | TaintValues.OVERFLOW;
        } else {
            l_t = l_t & invertBits(TaintValues.OVERFLOW_MASK);
        }
        ret.taint = l_t;
        return l;
    }
    
}
