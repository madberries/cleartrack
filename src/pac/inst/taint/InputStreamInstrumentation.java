package pac.inst.taint;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.Arrays;

import pac.config.BaseConfig;
import pac.config.Notify;
import pac.config.NotifyMsg;
import pac.config.RunChecks;
import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationLocation;
import pac.inst.InstrumentationMethod;
import pac.util.Ret;
import pac.util.TaintValues;
import pac.wrap.ByteArrayTaint;

@InstrumentationClass("java/io/InputStream")
public class InputStreamInstrumentation {

    private static final void socketTimeout(SocketTimeoutException ste) {
        if (BaseConfig.getInstance().getServerSocketTimeout() >= 0
                && BaseConfig.getInstance().getClientSocketTimeout() >= 0) {
            final NotifyMsg notifyMsg = new NotifyMsg("SocketTimeoutException()", ste.toString(), 239);
            notifyMsg.setAction(RunChecks.REMOVE_ACTION);
            notifyMsg.append("Encountered server or client timeout exception.");
            Notify.notifyAndRespond(notifyMsg);
        }
    }

    /*
     * TODO put this back in once I've implemented this in such a way that we are not
     * gratuitously reporting CWE-252's
     */
    private static final void writeToLog(String method, ByteArrayTaint b, int start, int end) {
        //		NotifyMsg notifyMsg = new NotifyMsg(method, method);
        //		notifyMsg.setCweNumber(252);
        //		notifyMsg.setAction(RunChecks.REMOVE_ACTION);
        //		notifyMsg.append("Zeroing out bytes in byte[] = " + b.value + " from index " + start 
        //				+ " (inclusive) to " + end + " (exclusive)");
        //		Notify.notifyAndRespond(notifyMsg);
    }

    private static final int zeroUnwrittenElements(ByteArrayTaint b, int offset, int length, int bytesRead,
                                                   String method) {
        // In the event of EOF, we should zero out all bytes in the byte array...
        int bytesReadVal = bytesRead < 0 ? 0 : bytesRead;
        int zeroElems = length - bytesReadVal;
        if (zeroElems > 0) {
            int start = offset + bytesReadVal;
            int end = offset + length;
            Arrays.fill(b.value, start, end, (byte) 0);
            Arrays.fill(b.taint, start, end, TaintValues.TRUSTED);
            writeToLog(method, b, start, end);
        }
        return bytesRead;
    }

    private static final int zeroUnwrittenElementsAndTaint(ByteArrayTaint b, int offset, int length, int bytesRead,
                                                           int taint, String method) {
        // In the event of EOF, we should zero out all bytes in the byte array...
        int bytesReadVal = bytesRead < 0 ? 0 : bytesRead;
        if (bytesRead > 0) // copy taint into the read bytes...
            Arrays.fill(b.taint, offset, offset + bytesRead, taint);
        int zeroElems = length - bytesReadVal;
        if (zeroElems > 0) {
            int start = offset + bytesReadVal;
            int end = offset + length;
            Arrays.fill(b.value, start, end, (byte) 0);
            Arrays.fill(b.taint, start, end, TaintValues.TRUSTED);
            writeToLog(method, b, start, end);
        }
        return bytesRead;
    }

    @InstrumentationMethod(canExtend = true)
    public static final int read(InputStream is, Ret ret) throws IOException {
        try {
            if (is.ss_hasUniformTaint) {
                ret.taint = is.ss_taint;
                return is.read();
            } else {
                return is.read(ret);
            }
        } catch (SocketTimeoutException e) {
            socketTimeout(e);
            throw e;
        }
    }

    @InstrumentationMethod(canExtend = true, instrumentationLocation = InstrumentationLocation.APP)
    public static final int read(InputStream is, ByteArrayTaint b, Ret ret) throws IOException {
        try {
            byte[] valueArr = b.value;
            ret.taint = TaintValues.TRUSTED;
            if (is.ss_hasUniformTaint) {
                return zeroUnwrittenElementsAndTaint(b, 0, valueArr.length, is.read(valueArr), is.ss_taint,
                                                     "InputStream.read(byte[])");
            } else {
                return zeroUnwrittenElements(b, 0, valueArr.length, is.read(b, ret), "InputStream.read(byte[])");
            }
        } catch (SocketTimeoutException e) {
            socketTimeout(e);
            throw e;
        }
    }

    @InstrumentationMethod(canExtend = true, instrumentationLocation = InstrumentationLocation.JDK, name = "read", descriptor = "(Lpac/wrap/ByteArrayTaint;Lpac/util/Ret;)I")
    public static final int read_jdk(InputStream is, ByteArrayTaint b, Ret ret) throws IOException {
        try {
            byte[] valueArr = b.value;
            if (is.ss_hasUniformTaint) {
                int bytesRead = is.read(valueArr);
                if (bytesRead > 0)
                    Arrays.fill(b.taint, 0, bytesRead, is.ss_taint);
                ret.taint = is.ss_taint;
                return bytesRead;
            } else {
                return is.read(b, ret);
            }
        } catch (SocketTimeoutException e) {
            socketTimeout(e);
            throw e;
        }
    }

    @InstrumentationMethod(canExtend = true, instrumentationLocation = InstrumentationLocation.APP)
    public static final int read(InputStream is, ByteArrayTaint b, int offset, int offset_t, int length, int length_t,
                                 Ret ret)
            throws IOException {
        try {
            if (is.ss_hasUniformTaint) {
                return zeroUnwrittenElementsAndTaint(b, offset, length, is.read(b.value, offset, length), is.ss_taint,
                                                     "InputStream.read(byte[], int, int)");
            } else {
                return zeroUnwrittenElements(b, offset, length, is.read(b, offset, offset_t, length, length_t, ret),
                                             "InputStream.read(byte[], int, int)");
            }
        } catch (SocketTimeoutException e) {
            socketTimeout(e);
            throw e;
        }
    }

    @InstrumentationMethod(canExtend = true, instrumentationLocation = InstrumentationLocation.JDK, name = "read", descriptor = "(Lpac/wrap/ByteArrayTaint;IIIILpac/util/Ret;)I")
    public static final int read_jdk(InputStream is, ByteArrayTaint b, int offset, int offset_t, int length,
                                     int length_t, Ret ret)
            throws IOException {
        try {
            if (is.ss_hasUniformTaint) {
                int bytesRead = is.read(b.value, offset, length);
                if (bytesRead > 0)
                    Arrays.fill(b.taint, offset, offset + bytesRead, is.ss_taint);
                ret.taint = is.ss_taint;
                return bytesRead;
            } else {
                return is.read(b, offset, offset_t, length, length_t, ret);
            }
        } catch (SocketTimeoutException e) {
            socketTimeout(e);
            throw e;
        }
    }

    @InstrumentationMethod(canExtend = true, instrumentationLocation = InstrumentationLocation.APP)
    public static final long skip(InputStream in, long n, int n_t, Ret ret) throws IOException {
        long val = 0;
        int taint = 0;
        do {
            long nread = in.skip(n - val, n_t, ret);
            taint |= ret.taint;
            val = val + nread;
            // TODO need to report a CWE-252 if we hit a second iteration
        } while (val < n);
        ret.taint = taint;
        return val;
    }

    @InstrumentationMethod(canExtend = true)
    public static final void close(InputStream in, Ret ret) throws IOException {
        if (in instanceof BufferedInputStream)
            BufferedInputStreamInstrumentation.close((BufferedInputStream) in, ret);
        else
            in.close(ret);
    }
    
}
