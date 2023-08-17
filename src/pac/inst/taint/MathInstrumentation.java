package pac.inst.taint;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.inst.InvocationType;
import pac.util.Ret;
import pac.util.TaintValues;

@InstrumentationClass("java/lang/Math")
public final class MathInstrumentation {

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double acos(double a, int a_t, Ret ret) {
    ret.taint = a_t;
    return Math.acos(a);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double asin(double a, int a_t, Ret ret) {
    ret.taint = a_t;
    return Math.asin(a);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double atan(double a, int a_t, Ret ret) {
    ret.taint = a_t;
    return Math.atan(a);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double atan2(double y, int y_t, double x, int x_t, Ret ret) {
    ret.taint = y_t | x_t;
    return Math.atan2(y, x);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double cbrt(double a, int a_t, Ret ret) {
    ret.taint = a_t;
    return Math.cbrt(a);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double ceil(double a, int a_t, Ret ret) {
    ret.taint = a_t;
    return Math.ceil(a);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double copySign(double magnitude, int magnitude_t, double sign, int sign_t,
      Ret ret) {
    ret.taint = magnitude_t | sign_t;
    return Math.copySign(magnitude, sign);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final float copySign(float magnitude, int magnitude_t, float sign, int sign_t,
      Ret ret) {
    ret.taint = magnitude_t | sign_t;
    return Math.copySign(magnitude, sign);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double cos(double a, int a_t, Ret ret) {
    ret.taint = a_t;
    return Math.cos(a);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double cosh(double x, int x_t, Ret ret) {
    ret.taint = x_t;
    return Math.cosh(x);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double exp(double a, int a_t, Ret ret) {
    ret.taint = a_t;
    return Math.exp(a);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double expm1(double x, int x_t, Ret ret) {
    ret.taint = x_t;
    return Math.expm1(x);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double floor(double a, int a_t, Ret ret) {
    ret.taint = a_t;
    return Math.floor(a);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final int getExponent(double d, int d_t, Ret ret) {
    ret.taint = d_t;
    return Math.getExponent(d);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final int getExponent(float f, int f_t, Ret ret) {
    ret.taint = f_t;
    return Math.getExponent(f);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double hypot(double x, int x_t, double y, int y_t, Ret ret) {
    ret.taint = y_t | x_t;
    return Math.hypot(x, y);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double IEEEremainder(double x, int x_t, double y, int y_t, Ret ret) {
    ret.taint = y_t | x_t;
    return Math.IEEEremainder(x, y);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double log(double a, int a_t, Ret ret) {
    ret.taint = a_t;
    return Math.log(a);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double log10(double a, int a_t, Ret ret) {
    ret.taint = a_t;
    return Math.log10(a);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double log1p(double a, int a_t, Ret ret) {
    ret.taint = a_t;
    return Math.log1p(a);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double nextAfter(double start, int start_t, double direction, int direction_t,
      Ret ret) {
    ret.taint = start_t | direction_t;
    return Math.nextAfter(start, direction);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final float nextAfter(float start, int start_t, double direction, int direction_t,
      Ret ret) {
    ret.taint = start_t | direction_t;
    return Math.nextAfter(start, direction);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double nextUp(double d, int d_t, Ret ret) {
    ret.taint = d_t;
    return Math.nextUp(d);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final float nextUp(float f, int f_t, Ret ret) {
    ret.taint = f_t;
    return Math.nextUp(f);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double pow(double a, int a_t, double b, int b_t, Ret ret) {
    ret.taint = a_t | b_t;
    return Math.pow(a, b);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double random(Ret ret) {
    ret.taint = TaintValues.TRUSTED;
    return Math.random();
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double rint(double a, int a_t, Ret ret) {
    ret.taint = a_t;
    return Math.rint(a);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double round(double a, int a_t, Ret ret) {
    ret.taint = a_t;
    return Math.round(a);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final float round(float a, int a_t, Ret ret) {
    ret.taint = a_t;
    return Math.round(a);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double scalb(double d, int d_t, int scaleFactor, int scaleFactor_t, Ret ret) {
    ret.taint = d_t | scaleFactor_t;
    return Math.scalb(d, scaleFactor);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final float scalb(float f, int f_t, int scaleFactor, int scaleFactor_t, Ret ret) {
    ret.taint = f_t | scaleFactor_t;
    return Math.scalb(f, scaleFactor);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double signum(double d, int d_t, Ret ret) {
    ret.taint = d_t;
    return Math.signum(d);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final float signum(float f, int f_t, Ret ret) {
    ret.taint = f_t;
    return Math.signum(f);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double sin(double a, int a_t, Ret ret) {
    ret.taint = a_t;
    return Math.sin(a);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double sinh(double x, int x_t, Ret ret) {
    ret.taint = x_t;
    return Math.sinh(x);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double sqrt(double a, int a_t, Ret ret) {
    ret.taint = a_t;
    return Math.sqrt(a);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double tan(double a, int a_t, Ret ret) {
    ret.taint = a_t;
    return Math.tan(a);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double tanh(double a, int a_t, Ret ret) {
    ret.taint = a_t;
    return Math.tanh(a);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double toDegrees(double angrad, int angrad_t, Ret ret) {
    ret.taint = angrad_t;
    return Math.toDegrees(angrad);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double toRadians(double angdeg, int angdeg_t, Ret ret) {
    ret.taint = angdeg_t;
    return Math.toRadians(angdeg);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final double ulp(double d, int d_t, Ret ret) {
    ret.taint = d_t;
    return Math.ulp(d);
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final float ulp(float f, int f_t, Ret ret) {
    ret.taint = f_t;
    return Math.ulp(f);
  }

}
