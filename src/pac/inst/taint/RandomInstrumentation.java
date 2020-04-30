package pac.inst.taint;

import java.util.Random;

import pac.config.Notify;
import pac.config.NotifyMsg;
import pac.config.RunChecks;
import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationLocation;
import pac.inst.InstrumentationMethod;
import pac.util.Ret;
import pac.util.TaintUtils;
import pac.util.TaintValues;

@InstrumentationClass("java/util/Random")
public final class RandomInstrumentation {

    // TODO add more instrumentation here...

    @InstrumentationMethod(canExtend = true, instrumentationLocation = InstrumentationLocation.APP)
    public static final int nextInt(Random rand, int i, int i_t, Ret ret) {
        if ((i_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED
                && ((i_t & (TaintValues.OVERFLOW_MASK | TaintValues.INFINITY)) != 0)) {
            // If we had arbitrary precision, this number would be really big,
            // so let's return a random integer over all integers.
            NotifyMsg notifyMsg = new NotifyMsg("Random.nextInt(int)", "Random.nextInt(int)", 190);
            notifyMsg.setAction(RunChecks.REPLACE_ACTION);
            notifyMsg.append("Attempting to generate a random number between 0 and " + i + ", but " + i + " is "
                    + TaintUtils.toTrustString(i_t) + " and has overflowed/underflowed.  Returning a random integer "
                    + "over all integers.");
            Notify.notifyAndRespond(notifyMsg);
            ret.taint = TaintValues.unset(i_t, TaintValues.INFINITY | TaintValues.OVERFLOW_MASK);
            return rand.nextInt();
        }
        ret.taint = i_t;
        return rand.nextInt(i);
    }

    @InstrumentationMethod(canExtend = true, instrumentationLocation = InstrumentationLocation.JDK, name = "nextInt", descriptor = "(IILpac/util/Ret;)I")
    public static final int nextInt_jdk(Random rand, int i, int i_t, Ret ret) {
        ret.taint = i_t;
        return rand.nextInt(i);
    }

}
