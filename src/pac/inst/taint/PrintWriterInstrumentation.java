package pac.inst.taint;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Stack;

import pac.config.NotifyMsg;
import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.util.Ret;
import pac.util.InstrumentationDialog;
import pac.wrap.ByteArrayTaint;

@InstrumentationClass("java/io/PrintWriter")
public final class PrintWriterInstrumentation {
  private static boolean htmlFormatHack = false;

  public synchronized static final void setHtmlFormatHack(boolean b) {
    htmlFormatHack = b;
  }

  public synchronized static final boolean getHtmlFormatHack() {
    return htmlFormatHack;
  }

  // INSTANCE METHODS

  @InstrumentationMethod(canExtend = true)
  public static final void print(PrintWriter pw, String s, Ret ret) throws IOException {
    Stack<NotifyMsg> stack = Thread.currentThread().ss_estack;
    if (stack == null || stack.isEmpty()) {
      s = SocketInstrumentation.checkSocketOutput(pw, "PrintWriter.print(String)", s, ret);
      // if (ProcessInstrumentation.isShellOutputStream(pw))
      //   s = RuntimeInstrumentation.checkCommand(s);
      if (getHtmlFormatHack()) {
        pw.print(InstrumentationDialog.htmlFormatTrackedString(s), ret);
      } else {
        pw.print(s, ret);
      }
      // Notify.error("Instrumented print " + s);
      return;
    }
    NotifyMsg notifyMsg = stack.peek();
    try {
      notifyMsg.enterWriteCall();
      ByteArrayTaint b = s.getBytes(ret);
      OutputStreamInstrumentation.blockTrustedChars(b, 0, b.taint.length, notifyMsg);
      s = SocketInstrumentation.checkSocketOutput(pw, "PrintWriter.print(String)",
          new String(b, ret), ret);
      // if (ProcessInstrumentation.isShellOutputStream(pw))
      //   s = RuntimeInstrumentation.checkCommand(s);
      if (getHtmlFormatHack()) {
        pw.print(InstrumentationDialog.htmlFormatTrackedString(s), ret);
      } else {
        pw.print(s, ret);
      }
      // Notify.error("Instrumented print " + s);
    } finally {
      notifyMsg.exitWriteCall();
    }
  }
}
