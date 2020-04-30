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

@InstrumentationClass("java/lang/CleartrackInteger")
public final class IntegerInstrumentation {
    public static final int DEFAULT_VALUE = 0;

    private static class CleartrackIntegerCache {
        static final int low = -128;
        static final int high;
        static final CleartrackInteger cache[][];

        static {
            // high value may be configured by property
            int h = 127;
            String integerCacheHighPropValue = sun.misc.VM.getSavedProperty("java.lang.Integer.IntegerCache.high");
            if (integerCacheHighPropValue != null) {
                int i = Integer.parseInt(integerCacheHighPropValue);
                i = Math.max(i, 127);
                // Maximum array size is Integer.MAX_VALUE
                h = Math.min(i, Integer.MAX_VALUE - (-low) - 1);
            }
            high = h;

            Ret ret = new Ret();
            cache = new CleartrackInteger[TaintValues.TRUST_MASK + 1][];
            init(TaintValues.TRUSTED, ret);
            init(TaintValues.UNKNOWN, ret);
            init(TaintValues.TAINTED, ret);
        }

        private static void init(int taint, Ret ret) {
            CleartrackInteger[] cacheArr = cache[taint] = new CleartrackInteger[(high - low) + 1];

            int j = low;
            for (int k = 0; k < cacheArr.length; k++)
                cacheArr[k] = new CleartrackInteger(j++, taint, ret);
        }

        private CleartrackIntegerCache() {
        }
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC, name = "valueOf", descriptor = "(IILpac/util/Ret;)Ljava/lang/Integer;")
    public static final CleartrackInteger valueOf(int i, int i_t, Ret ret) {
        if (i >= CleartrackIntegerCache.low && i <= CleartrackIntegerCache.high)
            return CleartrackIntegerCache.cache[i_t & TaintValues.TRUST_MASK][i + (-CleartrackIntegerCache.low)];
        return new CleartrackInteger(i, i_t, ret);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final int parseInt(String s, int radix, int radix_t, Ret ret) {
        if (s == null) { // this is what the JDK does for nulls
            throw new NumberFormatException("null");
        }
        int taint = TaintUtils.isTrusted(s) ? TaintValues.TRUSTED : TaintValues.TAINTED;
        try {
            ret.taint = taint;
            return Integer.parseInt(s, radix);
        } catch (NumberFormatException e) {
            if (TaintUtils.isTainted(s)) {
                NotifyMsg notifyMsg = new NotifyMsg("Integer.parseInt(String)", "Integer.parseInt(" + s + ")", 20);
                notifyMsg.setAction(RunChecks.REPLACE_ACTION);
                notifyMsg.append("Tainted string \"" + s + "\" is not a parseable int (returning the default value: "
                        + DEFAULT_VALUE + ")");
                Notify.notifyAndRespond(notifyMsg);
                return DEFAULT_VALUE;
            }
            throw e; // Throw the original exception on trusted strings
        }
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final String toString(int num, int num_t, Ret ret) {
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
        return CleartrackInteger.toString(num, num_t, ret);
    }
}
