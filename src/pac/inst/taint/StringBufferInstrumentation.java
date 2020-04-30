package pac.inst.taint;

import java.io.IOException;

import pac.config.BaseConfig;
import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationLocation;
import pac.inst.InstrumentationMethod;
import pac.util.Ret;
import pac.util.TaintValues;

@InstrumentationClass("java/lang/StringBuffer")
public final class StringBufferInstrumentation {

    @InstrumentationMethod
    public static final StringBuffer append(StringBuffer sb, int num, int num_t, Ret ret) {
        if ((num_t & TaintValues.OVERFLOW) == TaintValues.OVERFLOW) {
            String result = BaseConfig.getInstance().getOverflowReplacement();
            if (result != null)
                return sb.append(result, ret);
        }
        if ((num_t & TaintValues.UNDERFLOW) == TaintValues.UNDERFLOW) {
            String result = BaseConfig.getInstance().getUnderflowReplacement();
            if (result != null)
                return sb.append(result, ret);
        }
        if ((num_t & TaintValues.INFINITY) == TaintValues.INFINITY) {
            String result = BaseConfig.getInstance().getInfinityReplacement();
            if (result != null)
                return sb.append(result, ret);
        }
        return sb.append(num, num_t, ret);
    }

    @InstrumentationMethod
    public static final StringBuffer append(StringBuffer sb, float num, int num_t, Ret ret) {
        if ((num_t & TaintValues.INFINITY) == TaintValues.INFINITY) {
            String result = BaseConfig.getInstance().getInfinityReplacement();
            if (result != null)
                return sb.append(result, ret);
        }
        return sb.append(num, num_t, ret);
    }

    @InstrumentationMethod
    public static final StringBuffer append(StringBuffer sb, long num, int num_t, Ret ret) {
        if ((num_t & TaintValues.OVERFLOW) == TaintValues.OVERFLOW) {
            String result = BaseConfig.getInstance().getOverflowReplacement();
            if (result != null)
                return sb.append(result, ret);
        }
        if ((num_t & TaintValues.UNDERFLOW) == TaintValues.UNDERFLOW) {
            String result = BaseConfig.getInstance().getUnderflowReplacement();
            if (result != null)
                return sb.append(result, ret);
        }
        if ((num_t & TaintValues.INFINITY) == TaintValues.INFINITY) {
            String result = BaseConfig.getInstance().getInfinityReplacement();
            if (result != null)
                return sb.append(result, ret);
        }
        return sb.append(num, num_t, ret);
    }

    @InstrumentationMethod
    public static final StringBuffer append(StringBuffer sb, double num, int num_t, Ret ret) {
        if ((num_t & TaintValues.INFINITY) == TaintValues.INFINITY) {
            String result = BaseConfig.getInstance().getInfinityReplacement();
            if (result != null)
                return sb.append(result, ret);
        }
        return sb.append(num, num_t, ret);
    }

    // PRIMITIVE TAINT WRAPPERS
    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP)
    public static final StringBuffer append(StringBuffer sb, char c, int c_t, Ret ret) throws IOException {
        synchronized (sb) {
            if (c == 0 && (c_t & TaintValues.TAINTED) == TaintValues.TAINTED) { // CWE-626 Null Byte Interaction Error
                int replaceChar = BaseConfig.getInstance()
                        .reportNullByte("StringBuffer.append(char)",
                                        "Attempting to append a null character into the StringBuffer '" + sb + "'.");
                if (replaceChar == 0)
                    return sb;
                if (replaceChar > 0) {
                    c = (char) replaceChar;
                    c_t = TaintValues.TRUSTED;
                }
            }
            sb.append(c, c_t, ret);
        }
        return sb;
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP)
    public static final StringBuffer insert(StringBuffer sb, int offset, int offset_t, char c, int c_t, Ret ret)
            throws IOException {
        synchronized (sb) {
            if (c == 0 && (c_t & TaintValues.TAINTED) == TaintValues.TAINTED) { // CWE-626 Null Byte Interaction Error
                int replaceChar = BaseConfig.getInstance()
                        .reportNullByte("StringBuffer.insert(char)",
                                        "Attempting to insert a null character into the StringBuffer '" + sb
                                                + "' at index " + offset + ".");
                if (replaceChar == 0)
                    return sb;
                if (replaceChar > 0) {
                    c = (char) replaceChar;
                    c_t = TaintValues.TRUSTED;
                }
            }
            sb.insert(offset, offset_t, c, c_t, ret);
        }
        return sb;
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP)
    public static final void setCharAt(StringBuffer sb, int index, int index_t, char c, int c_t, Ret ret)
            throws IOException {
        synchronized (sb) {
            if (c == 0 && (c_t & TaintValues.TAINTED) == TaintValues.TAINTED) { // CWE-626 Null Byte Interaction Error
                int replaceChar = BaseConfig.getInstance()
                        .reportNullByte("StringBuffer.setCharAt(char)",
                                        "Attempting to set a null character in the StringBuffer '" + sb + "' at index "
                                                + index + ".");
                if (replaceChar == 0)
                    return;
                if (replaceChar > 0) {
                    c = (char) replaceChar;
                    c_t = TaintValues.TRUSTED;
                }
            }
            sb.setCharAt(index, index_t, c, c_t, ret);
        }
    }

}
