package pac.inst.taint;

import java.awt.event.KeyEvent;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.util.Ret;
import pac.util.TaintValues;

@InstrumentationClass("java/awt/event/KeyEvent")
public final class KeyEventInstrumentation {

  // PRIMITIVE TAINT WRAPPERS

  @InstrumentationMethod
  public static final char getKeyChar(KeyEvent event, Ret ret) {
    char c = event.getKeyChar();
    // Make sure that all characters coming in through a keyboard event will come across as tainted.
    ret.taint = TaintValues.TAINTED | TaintValues.GUI;
    return c;
  }

}
