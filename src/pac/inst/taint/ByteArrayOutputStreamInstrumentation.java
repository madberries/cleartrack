package pac.inst.taint;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationLocation;
import pac.inst.InstrumentationMethod;
import pac.inst.InvocationType;
import pac.util.Ret;
import pac.util.Overflows;
import pac.util.TaintValues;

@InstrumentationClass("java/io/ByteArrayOutputStream")
public class ByteArrayOutputStreamInstrumentation extends OutputStreamInstrumentation {

  @InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR,
      instrumentationLocation = InstrumentationLocation.APP)
  public static final ByteArrayOutputStream init(int length, int length_t, Ret ret)
      throws IOException {
    Overflows.checkOverflow(length_t, "new-array", "byte", null, length);
    if ((length_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED)
      length = Overflows.checkAllocSize("byte", length);
    return new ByteArrayOutputStream(length, length_t, ret);
  }

}
