package pac.inst.taint;

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.util.Ret;
import pac.util.TaintValues;
import pac.wrap.ByteArrayTaint;

@InstrumentationClass("java/util/zip/Inflater")
public final class InflaterInstrumentation {

  @InstrumentationMethod
  public static final int inflate(Inflater inf, ByteArrayTaint b, int off, int off_t, int len,
      int len_t, Ret ret) throws DataFormatException {
    int res = inf.inflate((byte[]) b.value, off, len);
    ret.taint = TaintValues.TRUSTED;
    if (ByteArrayTaint.isTainted(inf.buf_t, ret))
      ByteArrayTaint.taint(b, off, off_t, off + len - 1, off_t | len_t, ret);
    else
      ByteArrayTaint.trust(b, off, off_t, off + len - 1, off_t | len_t, ret);
    return res;
  }

}
