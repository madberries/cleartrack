package pac.inst.taint;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.util.Ret;

@InstrumentationClass("java/nio/charset/CharsetEncoder")
public final class CharsetEncoderInstrumentation {

  @InstrumentationMethod
  public static final ByteBuffer encode(CharsetEncoder ce, CharBuffer in, Ret ret)
      throws CharacterCodingException {
    ByteBuffer out = ce.encode(in, ret);
    if (out.hasArray())
      StringInstrumentation.putCharset(out.array(ret), ce.charset(ret));
    return out;
  }

  @InstrumentationMethod
  public static final CoderResult encode(CharsetEncoder ce, CharBuffer in, ByteBuffer out,
      boolean endOfInput, int endOfInput_t, Ret ret) {
    CoderResult res = ce.encode(in, out, endOfInput, endOfInput_t, ret);
    if (!res.isError()) {
      if (out.hasArray())
        StringInstrumentation.putCharset(out.array(ret), ce.charset(ret));
    }
    return res;
  }

}
