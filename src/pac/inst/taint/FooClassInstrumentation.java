package pac.inst.taint;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.inst.InstrumentationType;
import pac.util.TaintUtils;

@InstrumentationClass("pac/test/FooClass")
public final class FooClassInstrumentation {

  @InstrumentationMethod(instrumentationType = InstrumentationType.INSERT_AFTER, name = "fooMethod",
      descriptor = "(Lpac/util/Ret;)Ljava/lang/String;")
  public static final String postInstFooMethod(String str) {
    TaintUtils.taint(str);
    return str;
  }

}
