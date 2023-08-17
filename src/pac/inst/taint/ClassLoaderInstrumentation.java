package pac.inst.taint;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.inst.InvocationType;
import pac.util.Ret;
import pac.util.TaintUtils;
import pac.util.TaintValues;

@InstrumentationClass("java/lang/ClassLoader")
public final class ClassLoaderInstrumentation {

  // @InstrumentationMethod(canExtend = true)
  // public static final Enumeration<URL> findResources(ClassLoader cl, String name, Ret ret)
  //     throws IOException {
  //   final Enumeration<URL> urlEnum = cl.findResources(name, ret);
  //   if (urlEnum == null)
  //     return null;
  //   return new URLTaintEnumeration(urlEnum, TaintUtils.isTrusted(name) 
  //     || !TaintUtils.isTracked(name));
  // }

  @InstrumentationMethod(canExtend = true)
  public static final Enumeration<URL> getResources(ClassLoader cl, String name, Ret ret)
      throws IOException {
    final Enumeration<URL> urlEnum = cl.getResources(name, ret);
    if (urlEnum == null)
      return null;
    return new URLTaintEnumeration(urlEnum,
        TaintUtils.isTrusted(name) || !TaintUtils.isTracked(name));
  }

  // @InstrumentationMethod(canExtend = true)
  // public static final URL findResource(ClassLoader cl, String name, Ret ret) {
  //   URL url = cl.findResource(name, ret);
  //   if (url == null)
  //     return null;
  //   boolean isTrusted = TaintUtils.isTrusted(name) || !TaintUtils.isTracked(name);
  //   if (isTrusted) {
  //     URLInstrumentation.trustURL(url, TaintUtils.getTaintOr(name, TaintValues.INPUTTYPE_MASK));
  //   } else {
  //     URLInstrumentation.taintURL(url, TaintUtils.getTaintOr(name, TaintValues.INPUTTYPE_MASK));
  //   }
  //   return url;
  // }

  @InstrumentationMethod(canExtend = true)
  public static final URL getResource(ClassLoader cl, String name, Ret ret) {
    URL url = cl.getResource(name, ret);
    if (url == null)
      return null;
    int taint = TaintUtils.getTaintOr(name, TaintValues.INPUTTYPE_MASK);
    int taintBit = taint & TaintValues.TRUST_MASK;
    if (taintBit == TaintValues.TRUSTED) {
      URLInstrumentation.trustURL(url, taint);
    } else if (taintBit == TaintValues.UNKNOWN) {
      URLInstrumentation.trustURL(url, TaintValues.unset(taint, TaintValues.UNKNOWN));
    } else {
      URLInstrumentation.taintURL(url, taint);
    }
    return url;
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC, canExtend = true)
  public static final URL getSystemResource(String name, Ret ret) {
    URL url = ClassLoader.getSystemResource(name, ret);
    if (url == null)
      return null;
    int taint = TaintUtils.getTaintOr(name, TaintValues.INPUTTYPE_MASK);
    int taintBit = taint & TaintValues.TRUST_MASK;
    if (taintBit == TaintValues.TRUSTED) {
      URLInstrumentation.trustURL(url, taint);
    } else if (taintBit == TaintValues.UNKNOWN) {
      URLInstrumentation.trustURL(url, TaintValues.unset(taint, TaintValues.UNKNOWN));
    } else {
      URLInstrumentation.taintURL(url, taint);
    }
    return url;
  }

  @InstrumentationMethod(canExtend = true)
  public static final InputStream getResourceAsStream(ClassLoader cl, String name, Ret ret) {
    InputStream inStream = cl.getResourceAsStream(name, ret);
    if (inStream == null)
      return null;
    inStream.ss_hasUniformTaint = true;
    inStream.ss_taint = TaintValues.JAR | TaintValues.TRUSTED;
    if (inStream instanceof FilterInputStream) {
      InputStream in2 = ((FilterInputStream) inStream).in;
      in2.ss_hasUniformTaint = true;
      in2.ss_taint = inStream.ss_taint;
    }
    return inStream;
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC, canExtend = true)
  public static final InputStream getSystemResourceAsStream(String name, Ret ret) {
    InputStream inStream = ClassLoader.getSystemResourceAsStream(name, ret);
    if (inStream == null)
      return null;
    inStream.ss_hasUniformTaint = true;
    inStream.ss_taint = TaintValues.JAR | TaintValues.TRUSTED;
    if (inStream instanceof FilterInputStream) {
      InputStream in2 = ((FilterInputStream) inStream).in;
      in2.ss_hasUniformTaint = true;
      in2.ss_taint = inStream.ss_taint;
    }
    return inStream;
  }

  public static final class URLTaintEnumeration implements Enumeration<URL> {
    private Enumeration<URL> urlEnum;
    private boolean trusted;

    public URLTaintEnumeration(Enumeration<URL> urlEnum, boolean trusted) {
      this.urlEnum = urlEnum;
      this.trusted = trusted;
    }

    @Override
    public boolean hasMoreElements() {
      return urlEnum.hasMoreElements();
    }

    @Override
    public URL nextElement() {
      URL url = urlEnum.nextElement();
      if (url == null)
        return url;
      if (trusted) {
        URLInstrumentation.trustURL(url, 0);
      } else {
        URLInstrumentation.taintURL(url, 0);
      }
      return url;
    }
  }

}
