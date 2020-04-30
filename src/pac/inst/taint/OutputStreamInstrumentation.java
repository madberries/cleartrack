package pac.inst.taint;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Stack;

import pac.config.BaseConfig;
import pac.config.NotifyMsg;
import pac.config.RunChecks;
import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.util.Ret;
import pac.util.TaintValues;
import pac.wrap.ByteArrayTaint;

@InstrumentationClass("java/io/OutputStream")
public class OutputStreamInstrumentation {

    public static final boolean isAllowableTrustedChar(int b) {
        return b == 0 || b == '\n' || b == '\r' || b == ' ' || b == '\t';
    }

    protected static final void blockTrustedChars(ByteArrayTaint b, int offset, int length, NotifyMsg notifyMsg) {
        if (b == null || notifyMsg.inWriteCall())
            return;
        if (b.taint == null)
            return;
        int last = Math.min(offset + length, b.taint.length);
        byte[] valueArr = b.value;
        boolean writtenOrigOutput = false;
        if (BaseConfig.getInstance().getTrustedExceptionAction() != RunChecks.REPLACE_ACTION) {
            for (int i = offset; i < last; i++) {
                if ((b.taint[i] & TaintValues.TRUST_MASK) == TaintValues.TAINTED || isAllowableTrustedChar(valueArr[i]))
                    continue;
                if (!writtenOrigOutput) {
                    notifyMsg.append(ByteArrayTaint.createTaintDisplayLines(b, offset, length));
                    writtenOrigOutput = true;
                }
            }
        } else {
            byte replaceChar = BaseConfig.getInstance().getTrustedExceptionChar();
            for (int i = offset; i < last; i++) {
                if ((b.taint[i] & TaintValues.TRUST_MASK) == TaintValues.TAINTED || isAllowableTrustedChar(valueArr[i]))
                    continue;
                if (!writtenOrigOutput) {
                    notifyMsg.append(ByteArrayTaint.createTaintDisplayLines(b, offset, length));
                    writtenOrigOutput = true;
                }
                valueArr[i] = replaceChar;
            }
        }
    }

    @InstrumentationMethod(canExtend = true)
    public static final void write(OutputStream os, ByteArrayTaint b, Ret ret) throws IOException {
        Stack<NotifyMsg> stack = Thread.currentThread().ss_estack;
        if (stack == null || stack.isEmpty()) {
            if (os instanceof FileOutputStream) {
                FileOutputStreamInstrumentation.write((FileOutputStream) os, b, ret);
            } else {
                ByteArrayTaint bwrap = SocketInstrumentation
                        .checkSocketOutput(os, "OutputStream.write(byte[], int, int)", b, ret);
                os.write(bwrap, ret);
            }
            return;
        }
        NotifyMsg notifyMsg = stack.peek();
        try {
            notifyMsg.enterWriteCall();
            blockTrustedChars(b, 0, b.taint.length, notifyMsg);
            if (os instanceof FileOutputStream) {
                FileOutputStreamInstrumentation.write((FileOutputStream) os, b, ret);
            } else {
                ByteArrayTaint bwrap = SocketInstrumentation
                        .checkSocketOutput(os, "OutputStream.write(byte[], int, int)", b, ret);
                os.write(bwrap, ret);
            }
        } finally {
            notifyMsg.exitWriteCall();
        }
    }

    @InstrumentationMethod(canExtend = true)
    public static final void write(OutputStream os, ByteArrayTaint b, int off, int off_t, int len, int len_t, Ret ret)
            throws IOException {
        Stack<NotifyMsg> stack = Thread.currentThread().ss_estack;
        if (stack == null || stack.isEmpty()) {
            if (os instanceof FileOutputStream) {
                FileOutputStreamInstrumentation.write((FileOutputStream) os, b, off, off_t, len, len_t, ret);
            } else {
                ByteArrayTaint bwrap = SocketInstrumentation
                        .checkSocketOutput(os, "OutputStream.write(byte[], int, int)", b, ret);
                os.write(bwrap, off, off_t, len, len_t, ret);
            }
            return;
        }
        NotifyMsg notifyMsg = stack.peek();
        try {
            notifyMsg.enterWriteCall();
            blockTrustedChars(b, off, len, notifyMsg);
            if (os instanceof FileOutputStream) {
                FileOutputStreamInstrumentation.write((FileOutputStream) os, b, off, off_t, len, len_t, ret);
            } else {
                ByteArrayTaint bwrap = SocketInstrumentation
                        .checkSocketOutput(os, "OutputStream.write(byte[], int, int)", b, ret);
                os.write(bwrap, off, off_t, len, len_t, ret);
            }
        } finally {
            notifyMsg.exitWriteCall();
        }
    }

    @InstrumentationMethod(canExtend = true)
    public static final void close(OutputStream out, Ret ret) throws IOException {
        if (out instanceof FileOutputStream) {
            FileOutputStreamInstrumentation.close((FileOutputStream) out, ret);
        } else {
            out.close();
        }
    }

}
