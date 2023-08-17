package pac.inst.taint;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.inst.InstrumentationType;
import pac.util.TaintUtils;
import pac.util.TaintValues;

/**
 * Encoded regions are tracked inside of TaintValues. When a string is encoded, the new string is
 * marked with that encoding and any previous encoding is lost. The idea is that we presume that if
 * a string is encoded for A and then we turn around and encode it for B, then B will have made any
 * characters of A's potentially unsafe encoding safe for B.
 * 
 * @author jeikenberry
 */
@InstrumentationClass(value = "org/owasp/esapi/Encoder", isInterface = true)
public final class EncoderInstrumentation {

  /*
   * Let's instrument this class in a way that removes all dependencies on the ESAPI API, so that
   * our instrumentation does not require an instrumented library on the boot classpath.
   */

  @InstrumentationMethod(instrumentationType = InstrumentationType.INSERT_AFTER,
      name = "encodeForCSS", descriptor = "(Ljava/lang/String;Lpac/util/Ret;)Ljava/lang/String;")
  public static final String encodeForCSS(String encodedStr) {
    TaintUtils.markOr(encodedStr, TaintValues.CSS_TYPE, 0, encodedStr.length() - 1);
    return encodedStr;
  }

  @InstrumentationMethod(instrumentationType = InstrumentationType.INSERT_AFTER,
      name = "encodeForDN", descriptor = "(Ljava/lang/String;Lpac/util/Ret;)Ljava/lang/String;")
  public static final String encodeForDN(String encodedStr) {
    TaintUtils.markOr(encodedStr, TaintValues.DN_TYPE, 0, encodedStr.length() - 1);
    return encodedStr;
  }

  @InstrumentationMethod(instrumentationType = InstrumentationType.INSERT_AFTER,
      name = "encodeForHTML", descriptor = "(Ljava/lang/String;Lpac/util/Ret;)Ljava/lang/String;")
  public static final String encodeForHTML(String encodedStr) {
    TaintUtils.markOr(encodedStr, TaintValues.HTML_TYPE, 0, encodedStr.length() - 1);
    return encodedStr;
  }

  @InstrumentationMethod(instrumentationType = InstrumentationType.INSERT_AFTER,
      name = "encodeForHTMLAttribute",
      descriptor = "(Ljava/lang/String;Lpac/util/Ret;)Ljava/lang/String;")
  public static final String encodeForHTMLAttribute(String encodedStr) {
    TaintUtils.markOr(encodedStr, TaintValues.HTML_TYPE, 0, encodedStr.length() - 1);
    return encodedStr;
  }

  @InstrumentationMethod(instrumentationType = InstrumentationType.INSERT_AFTER,
      name = "encodeForJavaScript",
      descriptor = "(Ljava/lang/String;Lpac/util/Ret;)Ljava/lang/String;")
  public static final String encodeForJavaScript(String encodedStr) {
    TaintUtils.markOr(encodedStr, TaintValues.JAVASCRIPT_TYPE, 0, encodedStr.length() - 1);
    return encodedStr;
  }

  @InstrumentationMethod(instrumentationType = InstrumentationType.INSERT_AFTER,
      name = "encodeForLDAP", descriptor = "(Ljava/lang/String;Lpac/util/Ret;)Ljava/lang/String;")
  public static final String encodeForLDAP(String encodedStr) {
    TaintUtils.markOr(encodedStr, TaintValues.LDAP_TYPE, 0, encodedStr.length() - 1);
    return encodedStr;
  }

  @InstrumentationMethod(instrumentationType = InstrumentationType.INSERT_AFTER,
      name = "encodeForOS",
      descriptor = "(Lorg/owasp/esapi/codecs/Codec;Ljava/lang/String;Lpac/util/Ret;)Ljava/lang/String;")
  public static final String encodeForOS(String encodedStr) {
    TaintUtils.markOr(encodedStr, TaintValues.OS_TYPE, 0, encodedStr.length() - 1);
    return encodedStr;
  }

  @InstrumentationMethod(instrumentationType = InstrumentationType.INSERT_AFTER,
      name = "encodeForSQL",
      descriptor = "(Lorg/owasp/esapi/codecs/Codec;Ljava/lang/String;Lpac/util/Ret;)Ljava/lang/String;")
  public static final String encodeForSQL(String encodedStr) {
    TaintUtils.markOr(encodedStr, TaintValues.SQL_TYPE, 0, encodedStr.length() - 1);
    return encodedStr;
  }

  @InstrumentationMethod(instrumentationType = InstrumentationType.INSERT_AFTER,
      name = "encodeForURL", descriptor = "(Ljava/lang/String;Lpac/util/Ret;)Ljava/lang/String;")
  public static final String encodeForURL(String encodedStr) {
    TaintUtils.markOr(encodedStr, TaintValues.URL_TYPE, 0, encodedStr.length() - 1);
    return encodedStr;
  }

  @InstrumentationMethod(instrumentationType = InstrumentationType.INSERT_AFTER,
      name = "encodeForVBScript",
      descriptor = "(Ljava/lang/String;Lpac/util/Ret;)Ljava/lang/String;")
  public static final String encodeForVBScript(String encodedStr) {
    TaintUtils.markOr(encodedStr, TaintValues.VBSCRIPT_TYPE, 0, encodedStr.length() - 1);
    return encodedStr;
  }

  @InstrumentationMethod(instrumentationType = InstrumentationType.INSERT_AFTER,
      name = "encodeForXML", descriptor = "(Ljava/lang/String;Lpac/util/Ret;)Ljava/lang/String;")
  public static final String encodeForXML(String encodedStr) {
    TaintUtils.markOr(encodedStr, TaintValues.XML_TYPE, 0, encodedStr.length() - 1);
    return encodedStr;
  }

  @InstrumentationMethod(instrumentationType = InstrumentationType.INSERT_AFTER,
      name = "encodeForXMLAttribute",
      descriptor = "(Ljava/lang/String;Lpac/util/Ret;)Ljava/lang/String;")
  public static final String encodeForXMLAttribute(String encodedStr) {
    TaintUtils.markOr(encodedStr, TaintValues.XML_TYPE, 0, encodedStr.length() - 1);
    return encodedStr;
  }

  @InstrumentationMethod(instrumentationType = InstrumentationType.INSERT_AFTER,
      name = "encodeForXPath", descriptor = "(Ljava/lang/String;Lpac/util/Ret;)Ljava/lang/String;")
  public static final String encodeForXPath(String encodedStr) {
    TaintUtils.markOr(encodedStr, TaintValues.XPATH_TYPE, 0, encodedStr.length() - 1);
    return encodedStr;
  }

}
