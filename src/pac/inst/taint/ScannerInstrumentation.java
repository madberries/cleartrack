package pac.inst.taint;

import java.util.Scanner;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationLocation;
import pac.inst.InstrumentationMethod;
import pac.util.Ret;
import pac.util.TaintValues;
import pac.wrap.CharArrayTaint;

@InstrumentationClass("java/util/Scanner")
public class ScannerInstrumentation {

  @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP)
  public static final String next(Scanner scanner, Ret ret) {
    String next = scanner.next(ret);
    CharArrayTaint chArr = next.value_t;
    if (chArr == null)
      return next;
    int[] taint = chArr.taint;
    // Scanners perform some bitwise operations while decoding the string. This will cause problems
    // where these strings are parsed into numbers that are then cast down (which would normally
    // cause an overflow).
    for (int i = 0; i < taint.length; i++) {
      taint[i] = TaintValues.unset(taint[i], TaintValues.BITWISE_EXPR);
    }
    return next;
  }

}
