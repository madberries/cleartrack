package pac.inst.taint;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.util.Ret;
import pac.wrap.ByteArrayTaint;
import sun.nio.ch.FileChannelImpl;

@InstrumentationClass("java/nio/channels/ReadableByteChannel")
public class ReadableByteChannelInstrumentation {

  // This is only here because bytes can be read from a FileInputStream in an unconventional way, by
  // acquiring the channel from the stream. See java.util.Scanner, for example.
  @InstrumentationMethod
  public static final int read(ReadableByteChannel rbc, ByteBuffer dst, Ret ret)
      throws IOException {
    int bytesRead = rbc.read(dst, ret);
    if (bytesRead <= 0 || !(rbc instanceof FileChannelImpl))
      return bytesRead;
    int fdTaint = ((FileChannelImpl) rbc).fd.fd_t;
    ByteArrayTaint baTaint = dst.hb_t;
    if (baTaint == null)
      return bytesRead;
    int[] taintArr = baTaint.taint;
    if (taintArr == null)
      return bytesRead;
    Arrays.fill(taintArr, 0, bytesRead, fdTaint);
    return bytesRead;
  }

}
