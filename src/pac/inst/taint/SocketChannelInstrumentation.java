package pac.inst.taint;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Set;

import pac.config.BaseConfig;
import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.util.Ret;

@InstrumentationClass("java/nio/channels/SocketChannel")
public class SocketChannelInstrumentation {
  public static int socket_timeout = 1000;

  private static Set<SocketChannel> serversideSocketChannels = new HashSet<SocketChannel>();

  public synchronized static final boolean isServersideSocketChannel(SocketChannel channel) {
    return serversideSocketChannels.contains(channel);
  }

  public synchronized static final void addServersideSocketChannel(SocketChannel channel) {
    serversideSocketChannels.add(channel);
  }

  // INSTANCE METHODS
  @InstrumentationMethod
  public static final int read(SocketChannel sc, ByteBuffer dat) throws IOException {
    // It doesn't make sense to timeout a SocketChannel that is non-blocking. In this case, invoke
    // the original read().
    if (!sc.isBlocking())
      return sc.read(dat);
    int timeout = BaseConfig.getInstance().getServerSocketTimeout();
    if (timeout >= 0)
      sc.socket().setSoTimeout(timeout);
    InputStream is = sc.socket().getInputStream();
    ReadableByteChannel wc = Channels.newChannel(is);
    return wc.read(dat);
  }

  @InstrumentationMethod
  public static final long read(SocketChannel sc, ByteBuffer[] dat) throws IOException {
    // It doesn't make sense to timeout a SocketChannel that is non-blocking. In this case, invoke
    // the original read().
    if (!sc.isBlocking())
      return sc.read(dat);
    int timeout = BaseConfig.getInstance().getServerSocketTimeout();
    if (timeout >= 0)
      sc.socket().setSoTimeout(timeout);
    InputStream is = sc.socket().getInputStream();
    ReadableByteChannel wc = Channels.newChannel(is);
    long read_cnt = 0;
    for (int i = 0; i < dat.length; i++) {
      int cnt = wc.read(dat[i]);
      if (cnt == -1) {
        if (read_cnt > 0)
          return read_cnt;
        else
          return -1;
      }
      read_cnt += cnt;
    }
    return read_cnt;
  }

  @InstrumentationMethod
  public static final long read(SocketChannel sc, ByteBuffer[] dat, int offset, int length)
      throws IOException {
    // It doesn't make sense to timeout a SocketChannel that is non-blocking. In this case, invoke
    // the original read().
    if (!sc.isBlocking())
      return sc.read(dat);
    int timeout = BaseConfig.getInstance().getServerSocketTimeout();
    if (timeout >= 0)
      sc.socket().setSoTimeout(timeout);
    InputStream is = sc.socket().getInputStream();
    ReadableByteChannel wc = Channels.newChannel(is);
    long read_cnt = 0;
    for (int i = offset; i < length; i++) {
      int cnt = wc.read(dat[i]);
      if (cnt == -1) {
        if (read_cnt > 0)
          return read_cnt;
        else
          return -1;
      }
      read_cnt += cnt;
    }
    return read_cnt;
  }

  @InstrumentationMethod
  public static final int write(SocketChannel channel, ByteBuffer src) throws IOException {
    if (!isServersideSocketChannel(channel)) {
      src = BaseConfig.getInstance().runClientSocketOutputCheck("SocketChannel.write(ByteBuffer)",
          src, new Ret());
    }
    int result = channel.write(src);
    return result;
  }
}
