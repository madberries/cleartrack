package pac.inst.taint;

import java.io.BufferedInputStream;
import java.io.IOException;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.util.Ret;
import pac.wrap.ByteArrayTaint;

@InstrumentationClass("java/io/BufferedInputStream")
public final class BufferedInputStreamInstrumentation extends InputStreamInstrumentation {

  @InstrumentationMethod
  public static final void close(BufferedInputStream io, Ret ret) throws IOException {
    if (io.buf_t != null) {
      if (io.buf != io.buf_t.value)
        io.buf_t.value = io.buf;
    } else {
      if (io.buf != null)
        io.buf_t = ByteArrayTaint.toTaintArray(io.buf);
    }
    io.close();
  }

}
