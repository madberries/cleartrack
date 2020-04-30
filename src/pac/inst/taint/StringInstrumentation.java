package pac.inst.taint;

import java.io.IOException;
import java.nio.charset.Charset;

import pac.config.BaseConfig;
import pac.config.Notify;
import pac.config.NotifyMsg;
import pac.config.RunChecks;
import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationLocation;
import pac.inst.InstrumentationMethod;
import pac.inst.InvocationType;
import pac.util.Ret;
import pac.util.TaintUtils;
import pac.util.TaintValues;
import pac.wrap.ByteArrayTaint;
import pac.wrap.CharArrayTaint;
import pac.wrap.IntArrayTaint;

@InstrumentationClass("java/lang/String")
public final class StringInstrumentation {

    public static final Charset getCharset(ByteArrayTaint buf) {
        return buf.charset;
    }

    public static final void putCharset(ByteArrayTaint buf, Charset charset) {
        buf.charset = charset;
    }

    protected static final String removeOrReplaceNullChars(String description, String s, char oldChar, Ret ret)
            throws IOException {
        // MySQL stuff will break if we don't bipass this even for unknown regions
        // since there are weird conversions that create strings on byte arrays
        // containing null bytes.
        if (!TaintUtils.isTainted(s))
            return s;
        int len = s.length();
        if (len == 0)
            return s;
        int index = s.indexOf(oldChar);
        if (index < 0)
            return s;

        BaseConfig config = BaseConfig.getInstance();
        int newChar = config.getNullByteReplaceChar();
        if (newChar < 0) { // it must be log only
            StringBuilder msg = new StringBuilder("Encountered a String containing null bytes at indicies = ");
            msg.append(index);
            index = s.indexOf(oldChar, index + 1);
            while (index >= 0 && index < len) {
                msg.append(", ");
                msg.append(index);
                index = s.indexOf(oldChar, index + 1);
            }
            msg.append(": ");
            msg.append(s);
            msg.append('.');
            config.reportNullByte(description, msg.toString());
            return s;
        } else {
            StringBuilder sb = new StringBuilder(s, ret);
            StringBuilder msg;
            if (oldChar != 0) { // replace the null byte...
                msg = new StringBuilder(
                        "Attempting to create a String from bytes/chars containing null bytes at indices = ");
                msg.append(index);
                sb.setCharAt(index, TaintValues.TRUSTED, (char) newChar, TaintValues.TRUSTED, ret);
                index = s.indexOf(oldChar, index + 1);
                while (index >= 0 && index < len) {
                    msg.append(", ");
                    msg.append(index);
                    sb.setCharAt(index, TaintValues.TRUSTED, (char) newChar, TaintValues.TRUSTED, ret);
                    index = s.indexOf(oldChar, index + 1);
                }
            } else { // remove the null bytes...
                msg = new StringBuilder("Attempting to replace string with null bytes at indices = ");
                msg.append(index);
                sb.deleteCharAt(index);
                int start = index;
                index = s.lastIndexOf(oldChar, len - 1);
                while (index > start) {
                    msg.append(", ");
                    msg.append(index);
                    sb.deleteCharAt(index - 1, TaintValues.TRUSTED, ret);
                    index = s.lastIndexOf(oldChar, index - 1);
                }
            }
            msg.append(": ");
            msg.append(s);
            msg.append('.');
            config.reportNullByte(description, msg.toString());
            return sb.toString(ret);
        }
    }

    //	@InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final String valueOf(char c, int c_t, Ret ret) throws IOException {
        String s = String.valueOf(c);
        if (c == 0 && (c_t & TaintValues.TAINTED) == TaintValues.TAINTED) { //CWE-626 Null Byte Interaction Error
            int replaceChar = BaseConfig.getInstance()
                    .reportNullByte("String.valueOf(char)", "Attempting to convert null character to a String.");
            if (replaceChar == 0)
                s = "";
            else if (replaceChar < 0)
                s = String.valueOf(c, c_t, ret);
            else
                s = String.valueOf(replaceChar, TaintValues.TRUSTED, ret);
        } else {
            s = String.valueOf(c);
        }
        return s;
    }

    // STRING CONSTRUCTORS

    @InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR, instrumentationLocation = InstrumentationLocation.APP)
    public static final String init(ByteArrayTaint bytes, Ret ret) throws IOException {
        String s = new String(bytes, ret);
        return removeOrReplaceNullChars("String(byte[])", s, (char) 0, ret);
    }

    @InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR, instrumentationLocation = InstrumentationLocation.APP)
    public static final String init(CharArrayTaint chars, Ret ret) throws IOException {
        String s = new String(chars, ret);
        return removeOrReplaceNullChars("String(char[])", s, (char) 0, ret);
    }

    @InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR, instrumentationLocation = InstrumentationLocation.APP)
    public static final String init(ByteArrayTaint bytes, Charset charset, Ret ret) throws IOException {
        String s = new String(bytes, charset, ret);
        return removeOrReplaceNullChars("String(byte[], Charset)", s, (char) 0, ret);
    }

    @Deprecated
    @InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR, instrumentationLocation = InstrumentationLocation.APP)
    public static final String init(ByteArrayTaint ascii, int hibyte, int hibyte_t, Ret ret) throws IOException {
        String s = new String(ascii, hibyte, hibyte_t, ret);
        return removeOrReplaceNullChars("String(byte[], int)", s, (char) 0, ret);
    }

    @InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR, instrumentationLocation = InstrumentationLocation.APP)
    public static final String init(ByteArrayTaint bytes, String charsetName, Ret ret) throws IOException {
        String s = new String(bytes, charsetName, ret);
        return removeOrReplaceNullChars("String(byte[], String)", s, (char) 0, ret);
    }

    @InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR, instrumentationLocation = InstrumentationLocation.APP)
    public static final String init(ByteArrayTaint bytes, int offset, int offset_t, int length, int length_t, Ret ret)
            throws IOException {
        String s = new String(bytes, offset, offset_t, length, length_t, ret);
        return removeOrReplaceNullChars("String(byte[], int, int)", s, (char) 0, ret);
    }

    @InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR, instrumentationLocation = InstrumentationLocation.APP)
    public static final String init(CharArrayTaint chars, int offset, int offset_t, int count, int count_t, Ret ret)
            throws IOException {
        String s = new String(chars, offset, offset_t, count, count_t, ret);
        return removeOrReplaceNullChars("String(chars[], int, int)", s, (char) 0, ret);
    }

    @InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR, instrumentationLocation = InstrumentationLocation.APP)
    public static final String init(IntArrayTaint codePoints, int offset, int offset_t, int count, int count_t, Ret ret)
            throws IOException {
        String s = new String(codePoints, offset, offset_t, count, count_t, ret);
        return removeOrReplaceNullChars("String(int[], int, int)", s, (char) 0, ret);
    }

    @InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR, instrumentationLocation = InstrumentationLocation.APP)
    public static final String init(ByteArrayTaint bytes, int offset, int offset_t, int length, int length_t,
                                    Charset charset, Ret ret)
            throws IOException {
        String s = new String(bytes, offset, offset_t, length, length_t, charset, ret);
        return removeOrReplaceNullChars("String(byte[], int, int, Charset)", s, (char) 0, ret);
    }

    @Deprecated
    @InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR, instrumentationLocation = InstrumentationLocation.APP)
    public static final String init(ByteArrayTaint ascii, int offset, int offset_t, int hibyte, int hibyte_t, int count,
                                    int count_t, Ret ret)
            throws IOException {
        String s = new String(ascii, offset, offset_t, hibyte, hibyte_t, count, count_t, ret);
        return removeOrReplaceNullChars("String(byte[], int, int, int)", s, (char) 0, ret);
    }

    @InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR, instrumentationLocation = InstrumentationLocation.APP)
    public static final String init(ByteArrayTaint bytes, int offset, int offset_t, int length, int length_t,
                                    String charsetName, Ret ret)
            throws IOException {
        String s = new String(bytes, offset, offset, length, length_t, charsetName, ret);
        // NOTE: This was a hack required to instrument postgres, so the following 
        // code is not needed since we no longer instrument database drivers...
        //		if (ByteArrayTaint.isTrusted(bytes, offset, offset_t, 
        //				offset+length-1, offset_t | length_t, ret) &&
        //				!TaintUtils.isTrusted(s)) {
        //			s = new String(bytes, offset, offset_t, length, length_t, charsetName, ret);
        //			TaintUtils.trust(s);
        //		}
        return removeOrReplaceNullChars("String(byte[], int, int, String)", s, (char) 0, ret);
    }

    @InstrumentationMethod
    public static final boolean equals(String s, Object obj, Ret ret) {
        if (s.equals(obj, ret)) {
            String s2 = (String) obj;
            boolean sTrusted = TaintUtils.isTrusted(s);
            boolean s2Trusted = TaintUtils.isTrusted(s2);
            if (sTrusted && !s2Trusted) {
                TaintUtils.trust(s2, TaintValues.EQUALS);
            } else if (!sTrusted && s2Trusted) {
                TaintUtils.trust(s, TaintValues.EQUALS);
            }
            return true;
        }
        return false;
    }

    @InstrumentationMethod
    public static final boolean equalsIgnoreCase(String s, String s2, Ret ret) {
        if (s.equalsIgnoreCase(s2, ret)) {
            boolean sTrusted = TaintUtils.isTrusted(s);
            boolean s2Trusted = TaintUtils.isTrusted(s2);
            if (sTrusted && !s2Trusted) {
                TaintUtils.trust(s2, TaintValues.EQUALS);
            } else if (!sTrusted && s2Trusted) {
                TaintUtils.trust(s, TaintValues.EQUALS);
            }
            return true;
        }
        return false;
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP)
    public static final int compareTo(String s1, String s2, Ret ret) {
        String errorMsg = null;
        try {
            if (s1 == null) {
                if (s2 == null) {
                    errorMsg = "Unchecked null pointer on string comparison: " + s1 + ".compareTo(" + s2 + ")";
                    return 0;
                }
                errorMsg = "Unchecked null pointer on string comparison: " + s1 + ".compareTo(" + s2 + ")";
                return -1;
            } else if (s2 == null) {
                errorMsg = "Unchecked null pointer on string comparison: " + s1 + ".compareTo(" + s2 + ")";
                return 1;
            }
            return s1.compareTo(s2, ret);
        } finally {
            if (errorMsg != null) {
                NotifyMsg notifyMsg = new NotifyMsg("String.compareTo(String)", "String.compareTo(" + s2 + ")", 252);
                notifyMsg.setAction(RunChecks.REPLACE_ACTION);
                notifyMsg.append(errorMsg);
                Notify.notifyAndRespond(notifyMsg);
            }
        }
    }

    @InstrumentationMethod
    public static final int hashCode(String s, Ret ret) {
        int result = s.hashCode(ret);
        ret.taint = TaintValues.unset(ret.taint, TaintValues.OVERFLOW_MASK);
        return result;
    }

    @InstrumentationMethod
    public static final int indexOf(String s, int ch, int ch_t, int fromIndex, int fromIndex_t, Ret ret) {
        int result = s.indexOf(ch, fromIndex);
        if (TaintUtils.isTainted(s)) {
            ret.taint = TaintValues.TAINTED;
        } else {
            ret.taint = TaintValues.TRUSTED;
        }
        ret.taint |= ch_t | fromIndex_t;
        return result;
    }

    @InstrumentationMethod
    public static final int indexOf(String s, String searchStr, int fromIndex, int fromIndex_t, Ret ret) {
        int result = s.indexOf(searchStr, fromIndex);
        if (TaintUtils.isTainted(s)) {
            ret.taint = TaintValues.TAINTED;
        } else {
            ret.taint = TaintUtils.isTainted(searchStr) ? TaintValues.TAINTED : TaintValues.TRUSTED;
        }
        ret.taint |= fromIndex_t;
        return result;
    }

    @InstrumentationMethod
    public static final int lastIndexOf(String s, int ch, int ch_t, int fromIndex, int fromIndex_t, Ret ret) {
        int result = s.lastIndexOf(ch, fromIndex);
        if (TaintUtils.isTainted(s)) {
            ret.taint = TaintValues.TAINTED;
        } else {
            ret.taint = TaintValues.TRUSTED;
        }
        ret.taint |= ch_t | fromIndex_t;
        return result;
    }

    @InstrumentationMethod
    public static final int lastIndexOf(String s, String searchStr, int fromIndex, int fromIndex_t, Ret ret) {
        int result = s.lastIndexOf(searchStr, fromIndex);
        if (TaintUtils.isTainted(s)) {
            ret.taint = TaintValues.TAINTED;
        } else {
            ret.taint = TaintUtils.isTainted(searchStr) ? TaintValues.TAINTED : TaintValues.TRUSTED;
        }
        ret.taint |= fromIndex_t;
        return result;
    }

    @InstrumentationMethod
    public static final String intern(String s, Ret ret) {
        String si = s.intern();
        String newS = new String(s, ret);
        newS.ss_interned = si;
        return newS;
    }

    @InstrumentationMethod
    public static final ByteArrayTaint getBytes(String s, Ret ret) {
        ByteArrayTaint bytes = s.getBytes(ret);
        if (!TaintUtils.isTrusted(s))
            putCharset(bytes, Charset.defaultCharset());
        return bytes;
    }

    @InstrumentationMethod
    public static final ByteArrayTaint getBytes(String s, Charset charset, Ret ret) {
        ByteArrayTaint bytes = s.getBytes(charset, ret);
        if (!TaintUtils.isTrusted(s))
            putCharset(bytes, Charset.defaultCharset());
        return bytes;
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP)
    public static final String replace(String s, char oldChar, int oldChar_t, char newChar, int newChar_t, Ret ret)
            throws IOException {
        if (newChar == 0) {
            String newStr = removeOrReplaceNullChars("String.replace(String, char, char)", s, oldChar, ret);
            if (newStr != s)
                return newStr;

        }
        return s.replace(oldChar, oldChar_t, newChar, newChar_t, ret);
    }

}
