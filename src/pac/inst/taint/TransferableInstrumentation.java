package pac.inst.taint;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.util.Ret;
import pac.util.TaintUtils;
import pac.util.TaintValues;
import pac.wrap.ByteArrayTaint;
import pac.wrap.CharArrayTaint;

@InstrumentationClass(value = "java/awt/datatransfer/Transferable", isInterface = true)
public final class TransferableInstrumentation {

  @InstrumentationMethod
  public static final Object getTransferData(Transferable trans, DataFlavor flavor)
      throws UnsupportedFlavorException, IOException {
    return getTransferData(trans, flavor, null);
  }

  @InstrumentationMethod
  public static final Object getTransferData(Transferable trans, DataFlavor flavor, Ret ret)
      throws UnsupportedFlavorException, IOException {
    Object contents = trans.getTransferData(flavor);

    // Non-tracked clipboard contents came from some source external to the program, so let's taint
    // this.
    if (contents instanceof String) {
      TaintUtils.taint((String) contents, TaintValues.GUI);
    } else if (contents instanceof StringBuffer) {
      TaintUtils.taint((StringBuffer) contents, TaintValues.GUI);
    } else if (contents instanceof StringBuilder) {
      TaintUtils.taint((StringBuilder) contents, TaintValues.GUI);
    } else if (contents instanceof CharArrayTaint) {
      CharArrayTaint.taint((CharArrayTaint) contents, TaintValues.GUI);
    } else if (contents instanceof ByteArrayTaint) {
      ByteArrayTaint.taint((ByteArrayTaint) contents, TaintValues.GUI);
    }
    return contents;
  }

}
