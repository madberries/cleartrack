package pac.inst.taint;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import pac.config.Notify;
import pac.config.NotifyMsg;
import pac.config.RunChecks;
import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.inst.InvocationType;
import pac.util.Ret;
import pac.util.TaintValues;
import pac.wrap.ByteArrayTaint;

@InstrumentationClass("java/io/RandomAccessFile")
public final class RandomAccessFileInstrumentation {

    // CONSTRUCTORS
    @InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR)
    public static final RandomAccessFile init(File file, String mode, Ret ret) throws FileNotFoundException {
        RandomAccessFile raf = new RandomAccessFile(file, mode); // FileNotFoundException

        // mark fileReader tainted if file is not a path that config file lists as ok
        try {
            final NotifyMsg notifyMsg = new NotifyMsg("RandomAccessFile(File, String)",
                    "RandomAccessFile(" + file.toString() + ", " + mode + ")", 0); // cwe-0 signals not to output msg to test harness

            // If canonical path of file is untrusted, then taint raf object
            if (!RunChecks.checkLegalFileName(raf.getFD(), file, notifyMsg)) { // MsgException
                notifyMsg.append("Setting RandomAccessFile to tainted.");

                // Output message to log file only. cwe is 0, message will not go to test harness
                Notify.notifyAndRespond(notifyMsg); // IOException
            }

        } catch (IOException ex) {
            Notify.error("Exception: " + ex + "\n");
        }

        return raf;
    }

    @InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR)
    public static final RandomAccessFile init(String name, String mode, Ret ret) throws FileNotFoundException {
        RandomAccessFile raf = new RandomAccessFile(name, mode); // FileNotFoundException

        // mark fileReader tainted if filename is not a path that config file lists as ok
        try {
            final NotifyMsg notifyMsg = new NotifyMsg("RandomAccessFile(String, String)",
                    "RandomAccessFile(" + name + "," + mode + ")", 0); // cwe-0 signals not to output msg to test harness

            // If canonical path of file is untrusted, then taint fileReader object
            if (!RunChecks.checkLegalFileName(raf.getFD(), name, notifyMsg)) { // MsgException
                notifyMsg.append("Setting RandomAccessFile to tainted.");

                // Output message to log file only. Because cwe is 0, message will not go to test harness
                Notify.notifyAndRespond(notifyMsg); // IOException
            }

        } catch (IOException ex) {
            Notify.error("Exception: " + ex + "\n");
        }

        return raf;
    }

    // PRIMITIVE TAINT WRAPPERS
    @InstrumentationMethod
    public static final int read(RandomAccessFile raf, Ret ret) throws IOException {
        int b = raf.read();
        ret.taint = raf.getFD().fd_t;
        return b;
    }

    @InstrumentationMethod
    public static final int read(RandomAccessFile raf, ByteArrayTaint b, Ret ret) throws IOException {
        int rval = raf.read(b.value);
        Arrays.fill(b.taint, raf.getFD().fd_t);
        ret.taint = TaintValues.TRUSTED;
        return rval;
    }

    @InstrumentationMethod
    public static final int read(RandomAccessFile raf, ByteArrayTaint b, int off, int off_t, int len, int len_t,
                                 Ret ret)
            throws IOException {
        int rval = raf.read(b.value, off, len);
        Arrays.fill(b.taint, off, off + len, raf.getFD().fd_t);
        ret.taint = TaintValues.TRUSTED;
        return rval;
    }

} // class RandomAccessFile
