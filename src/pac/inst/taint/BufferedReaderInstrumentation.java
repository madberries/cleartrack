package pac.inst.taint;

import java.io.BufferedReader;
import java.io.IOException;

import pac.config.BaseConfig;
import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.inst.taint.SocketInstrumentation.SocketType;
import pac.util.Ret;
import pac.util.TaintUtils;
import pac.wrap.CharArrayTaint;

@InstrumentationClass("java/io/BufferedReader")
public final class BufferedReaderInstrumentation {

    // BUFFEREDREADER INSTANCE METHODS

    @InstrumentationMethod(canExtend = true)
    public static final String readLine(BufferedReader br, Ret ret) throws IOException {
        String str;
        if (br.ss_hasUniformTaint) {
            // If the buffered reader is marked with taint, we can
            // optimize for this and call the uninstrumented readLine()
            str = br.readLine();
            if (str == null)
                return null;
            TaintUtils.mark(str, br.ss_taint, 0, str.length() - 1);
        } else {
            str = br.readLine(ret);
            if (str == null)
                return null;
        }

        if (br.ss_socktype == SocketType.SERVER_SOURCE)
            str = BaseConfig.getInstance().runServerSocketInputCheck("BufferedReader.readLine()", str, ret);
        return StringInstrumentation.removeOrReplaceNullChars("BufferedReader.readLine()", str, (char) 0, ret);
    }

    @InstrumentationMethod(canExtend = true)
    public static final int read(BufferedReader br, CharArrayTaint cbuf, int off, int off_t, int len, int len_t,
                                 Ret ret)
            throws IOException {
        if (br.ss_hasUniformTaint) {
            int res = br.read(cbuf.value, off, len);
            CharArrayTaint.mark(cbuf, br.ss_taint, off, off + len - 1);
            ret.taint = br.ss_taint;
            return res;
        } else {
            return br.read(cbuf, off, off_t, len, len_t, ret);
        }
    }

}
