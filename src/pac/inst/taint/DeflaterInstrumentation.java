package pac.inst.taint;

import java.util.zip.Deflater;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.util.Ret;
import pac.util.TaintValues;
import pac.wrap.ByteArrayTaint;

@InstrumentationClass("java/util/zip/Deflater")
public final class DeflaterInstrumentation {

    @InstrumentationMethod
    public static final int deflate(Deflater def, ByteArrayTaint b, int off, int off_t, int len, int len_t, int flush,
                                    int flush_t, Ret ret) {
        int res = def.deflate((byte[]) b.value, off, len, flush);
        ret.taint = TaintValues.TRUSTED;
        if (ByteArrayTaint.isTainted(def.buf_t, ret))
            ByteArrayTaint.taint(b, off, off_t, off + len - 1, off_t | len_t, ret);
        else
            ByteArrayTaint.trust(b, off, off_t, off + len - 1, off_t | len_t, ret);
        return res;
    }
    
}
