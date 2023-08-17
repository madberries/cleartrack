package pac.inst.taint;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import pac.config.BaseConfig;
import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationLocation;
import pac.inst.InstrumentationMethod;
import pac.inst.InvocationType;
import pac.util.Ret;
import pac.util.TaintValues;
import pac.wrap.ByteArrayTaint;

@InstrumentationClass("java/net/Socket")
public final class SocketInstrumentation {

  /**
   * Enum for tracking the type of stream (used for backstop confinement).
   * 
   * @author jeikenberry
   */
  public enum SocketType {
    CLIENT_SINK, SERVER_SOURCE, SERVER_SINK
  }

  @InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR)
  public static final Socket init() throws SocketException {
    Socket s = new Socket();
    setSocketTimeout(s);
    return s;
  }

  @InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR,
      instrumentationLocation = InstrumentationLocation.APP)
  public static final Socket init(InetAddress address, int port) throws IOException {
    Socket s = new Socket(address, port);
    setSocketTimeout(s);
    return s;
  }

  @Deprecated
  @InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR,
      instrumentationLocation = InstrumentationLocation.APP)
  public static final Socket init(InetAddress host, int port, boolean stream) throws IOException {
    Socket s = new Socket(host, port, stream);
    setSocketTimeout(s);
    return s;
  }

  @InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR,
      instrumentationLocation = InstrumentationLocation.APP)
  public static final Socket init(InetAddress address, int port, InetAddress localAddr,
      int localPort) throws IOException {
    Socket s = new Socket(address, port, localAddr, localPort);
    setSocketTimeout(s);
    return s;
  }

  @InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR,
      instrumentationLocation = InstrumentationLocation.APP)
  public static final Socket init(Proxy proxy) throws SocketException {
    Socket s = new Socket(proxy);
    setSocketTimeout(s);
    return s;
  }

  //@InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR,
  //                       instrumentationLocation = InstrumentationLocation.APP)
  //  public static final Socket init(SocketImpl impl) throws SocketException {
  //    Socket s = new Socket(impl);
  //    setSocketTimeout(s);
  //    return s;
  //  }

  @InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR,
      instrumentationLocation = InstrumentationLocation.APP)
  public static final Socket init(String host, int port) throws UnknownHostException, IOException {
    Socket s = new Socket(host, port);
    setSocketTimeout(s);
    return s;
  }

  @Deprecated
  @InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR,
      instrumentationLocation = InstrumentationLocation.APP)
  public static final Socket init(String host, int port, boolean stream)
      throws UnknownHostException, IOException {
    Socket s = new Socket(host, port, stream);
    setSocketTimeout(s);
    return s;
  }

  @InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR,
      instrumentationLocation = InstrumentationLocation.APP)
  public static final Socket init(String host, int port, InetAddress localAddr, int localPort)
      throws UnknownHostException, IOException {
    Socket s = new Socket(host, port, localAddr, localPort);
    setSocketTimeout(s);
    return s;
  }

  // INSTANCE METHODS
  
  @InstrumentationMethod
  public static final InputStream getInputStream(Socket s) throws IOException {
    InputStream is = s.getInputStream();
    if (!BaseConfig.getInstance().isPortTrusted(s.getLocalPort())) {
      is.ss_hasUniformTaint = true;
      is.ss_taint = TaintValues.SOCKET | TaintValues.TAINTED;
      if (s.ss_server) {
        is.ss_socktype = SocketType.SERVER_SOURCE;
      }
    } else {
      is.ss_hasUniformTaint = true;
      is.ss_taint = TaintValues.SOCKET | TaintValues.TRUSTED;
    }
    return is;
  }

  @InstrumentationMethod
  public static final OutputStream getOutputStream(Socket s) throws IOException {
    OutputStream os = s.getOutputStream();
    if (s.ss_server)
      os.ss_socktype = SocketType.SERVER_SINK;
    else
      os.ss_socktype = SocketType.CLIENT_SINK;
    return os;
  }

  public static final String checkSocketOutput(Writer sink, String desc, String s, Ret ret)
      throws IOException {
    String result = s;
    if (sink.ss_socktype == SocketType.CLIENT_SINK) {
      result = BaseConfig.getInstance().runClientSocketOutputCheck(desc, s, ret);
    } else if (sink.ss_socktype == SocketType.SERVER_SINK) {
      result = BaseConfig.getInstance().runServerSocketOutputCheck(desc, s, ret);
    }
    return result;
  }

  public static final String checkSocketOutput(OutputStream sink, String desc, String s, Ret ret)
      throws IOException {
    String result = s;
    if (sink.ss_socktype == SocketType.CLIENT_SINK) {
      result = BaseConfig.getInstance().runClientSocketOutputCheck(desc, s, ret);
    } else if (sink.ss_socktype == SocketType.SERVER_SINK) {
      result = BaseConfig.getInstance().runServerSocketOutputCheck(desc, s, ret);
    }
    return result;
  }

  public static final ByteArrayTaint checkSocketOutput(OutputStream sink, String desc,
      ByteArrayTaint b, Ret ret) {
    if (sink.ss_socktype == SocketType.CLIENT_SINK) {
      return BaseConfig.getInstance().runClientSocketOutputCheck(desc, b, ret);
    } else if (sink.ss_socktype == SocketType.SERVER_SINK) {
      return BaseConfig.getInstance().runServerSocketOutputCheck(desc, b, ret);
    }
    return b;
  }

  public static final void setSocketTimeout(Socket socket) throws SocketException {
    setSocketTimeout(socket, false);
  }

  protected static final void setSocketTimeout(Socket socket, boolean server)
      throws SocketException {
    int timeout = server ? BaseConfig.getInstance().getServerSocketTimeout()
        : BaseConfig.getInstance().getClientSocketTimeout();
    if (timeout >= 0)
      socket.setSoTimeout(timeout);
  }

}
