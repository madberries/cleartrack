package pac.inst.taint;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.inst.InvocationType;
import pac.util.Ret;
import pac.util.TaintUtils;

@InstrumentationClass("java/net/URLDecoder")
public final class URLDecoderInstrumentation {

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final String decode(String str, String encoding, Ret ret) throws UnsupportedEncodingException {
        String s = URLDecoder.decode(str, encoding, ret);
        s = TaintUtils.urlDecodedTaint(s, str);
        return s;
    }
    
}
