package pac.inst.taint;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.inst.InvocationType;
import pac.util.Ret;
import pac.util.TaintUtils;

@InstrumentationClass("java/net/URLEncoder")
public final class URLEncoderInstrumentation {

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final String encode(String str, String encoding, Ret ret) throws UnsupportedEncodingException {
        String s = URLEncoder.encode(str, encoding, ret);
        s = TaintUtils.urlEncodedTaint(s, str); // unused for non-chartaint
        return s;
    }

}
