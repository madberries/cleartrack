package pac.inst.taint;

import java.io.BufferedInputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import pac.config.NotifyMsg;
import pac.config.RunChecks;
import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.util.Ret;
import pac.util.TaintUtils;
import pac.util.TaintValues;

@InstrumentationClass("java/net/URL")
public final class URLInstrumentation {

  @InstrumentationMethod
  public static final InputStream openStream(URL url, Ret ret) throws IOException {
    InputStream is = url.openStream();
    String protocol = url.getProtocol();
    if (protocol.equals("jar")) {
      is.ss_hasUniformTaint = true;
      is.ss_taint = TaintValues.JAR | TaintValues.TRUSTED;
      if (is instanceof FilterInputStream) {
        InputStream in2 = ((FilterInputStream) is).in;
        in2.ss_hasUniformTaint = true;
        in2.ss_taint = is.ss_taint;
      }
    } else if (protocol.equals("file")) {
      FileDescriptor fd = null;
      if (is instanceof FileInputStream) {
        fd = ((FileInputStream) is).fd;
      } else if (is instanceof BufferedInputStream) {
        InputStream is2 = ((BufferedInputStream) is).in;
        if (is2 instanceof FileInputStream)
          fd = ((FileInputStream) is2).fd;
      }
      if (fd == null)
        return is; // Don't know how to handle this.

      final String path = url.getPath(ret);
      final NotifyMsg notifyMsg =
          new NotifyMsg("URL.openStream()", "URL.openStream(" + path + ")", 0);

      RunChecks.checkLegalFileName(fd, path, notifyMsg);
      is.ss_hasUniformTaint = true;
      is.ss_taint = fd.fd_t;
    } else {
      is.ss_hasUniformTaint = true;
      is.ss_taint = TaintValues.URL | TaintValues.TAINTED;
    }
    return is;
  }

  public static final void trustURL(URL url, int mask) {
    if (url == null)
      return;
    if (url.protocol != null)
      TaintUtils.trust(url.protocol, mask);
    if (url.file != null)
      TaintUtils.trust(url.file, mask);
    if (url.authority != null)
      TaintUtils.trust(url.authority, mask);
    if (url.host != null)
      TaintUtils.trust(url.host, mask);
    if (url.path != null)
      TaintUtils.trust(url.path, mask);
    if (url.query != null)
      TaintUtils.trust(url.query, mask);
    if (url.ref != null)
      TaintUtils.trust(url.ref, mask);
    if (url.userInfo != null)
      TaintUtils.trust(url.userInfo, mask);
    url.port_t = TaintValues.TRUSTED | mask;
  }

  public static final void taintURL(URL url, int mask) {
    if (url == null)
      return;
    if (url.protocol != null)
      TaintUtils.taint(url.protocol, mask);
    if (url.file != null)
      TaintUtils.taint(url.file, mask);
    if (url.authority != null)
      TaintUtils.taint(url.authority, mask);
    if (url.host != null)
      TaintUtils.taint(url.host, mask);
    if (url.path != null)
      TaintUtils.taint(url.path, mask);
    if (url.query != null)
      TaintUtils.taint(url.query, mask);
    if (url.ref != null)
      TaintUtils.taint(url.ref, mask);
    if (url.userInfo != null)
      TaintUtils.taint(url.userInfo, mask);
    url.port_t = TaintValues.TAINTED | mask;
  }

}
