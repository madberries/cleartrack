package pac.inst.taint;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.inst.InvocationType;
import pac.util.Ret;
import pac.util.TaintValues;

@InstrumentationClass("java/lang/StrictMath")
public final class StrictMathInstrumentation {

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double acos(double a, int a_t, Ret ret) {
        ret.taint = a_t;
        return StrictMath.acos(a);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double asin(double a, int a_t, Ret ret) {
        ret.taint = a_t;
        return StrictMath.asin(a);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double atan(double a, int a_t, Ret ret) {
        ret.taint = a_t;
        return StrictMath.atan(a);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double atan2(double y, int y_t, double x, int x_t, Ret ret) {
        ret.taint = x_t | y_t;
        return StrictMath.atan2(y, x);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double cbrt(double a, int a_t, Ret ret) {
        ret.taint = a_t;
        return StrictMath.cbrt(a);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double ceil(double a, int a_t, Ret ret) {
        ret.taint = a_t;
        return StrictMath.ceil(a);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double copySign(double magnitude, int magnitude_t, double sign, int sign_t, Ret ret) {
        ret.taint = magnitude_t | sign_t;
        return StrictMath.copySign(magnitude, sign);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final float copySign(float magnitude, int magnitude_t, float sign, int sign_t, Ret ret) {
        ret.taint = magnitude_t | sign_t;
        return StrictMath.copySign(magnitude, sign);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double cos(double a, int a_t, Ret ret) {
        ret.taint = a_t;
        return StrictMath.cos(a);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double cosh(double x, int x_t, Ret ret) {
        ret.taint = x_t;
        return StrictMath.cosh(x);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double exp(double a, int a_t, Ret ret) {
        ret.taint = a_t;
        return StrictMath.exp(a);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double expm1(double x, int x_t, Ret ret) {
        ret.taint = x_t;
        return StrictMath.expm1(x);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double floor(double a, int a_t, Ret ret) {
        ret.taint = a_t;
        return StrictMath.floor(a);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final int getExponent(double d, int d_t, Ret ret) {
        ret.taint = d_t;
        return StrictMath.getExponent(d);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final int getExponent(float f, int f_d, Ret ret) {
        ret.taint = f_d;
        return StrictMath.getExponent(f);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double hypot(double x, int x_t, double y, int y_t, Ret ret) {
        ret.taint = x_t | y_t;
        return StrictMath.hypot(x, y);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double IEEEremainder(double x, int x_t, double y, int y_t, Ret ret) {
        ret.taint = x_t | y_t;
        return StrictMath.IEEEremainder(x, y);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double log(double a, int a_t, Ret ret) {
        ret.taint = a_t;
        return StrictMath.log(a);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double log10(double a, int a_t, Ret ret) {
        ret.taint = a_t;
        return StrictMath.log10(a);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double log1p(double a, int a_t, Ret ret) {
        ret.taint = a_t;
        return StrictMath.log1p(a);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double nextAfter(double start, int start_t, double direction, int direction_t, Ret ret) {
        ret.taint = start_t | direction_t;
        return StrictMath.nextAfter(start, direction);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final float nextAfter(float start, int start_t, double direction, int direction_t, Ret ret) {
        ret.taint = start_t | direction_t;
        return StrictMath.nextAfter(start, direction);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double nextUp(double d, int d_t, Ret ret) {
        ret.taint = d_t;
        return StrictMath.nextUp(d);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final float nextUp(float f, int f_t, Ret ret) {
        ret.taint = f_t;
        return StrictMath.nextUp(f);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double pow(double a, int a_t, double b, int b_t, Ret ret) {
        ret.taint = a_t | b_t;
        return StrictMath.pow(a, b);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double random(Ret ret) {
        ret.taint = TaintValues.TRUSTED;
        return StrictMath.random();
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double rint(double a, int a_t, Ret ret) {
        ret.taint = a_t;
        return StrictMath.rint(a);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double round(double a, int a_t, Ret ret) {
        ret.taint = a_t;
        return StrictMath.round(a);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final float round(float a, int a_t, Ret ret) {
        ret.taint = a_t;
        return StrictMath.round(a);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double scalb(double d, int d_t, int scaleFactor, int scaleFactor_t, Ret ret) {
        ret.taint = d_t | scaleFactor_t;
        return StrictMath.scalb(d, scaleFactor);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final float scalb(float f, int f_t, int scaleFactor, int scaleFactor_t, Ret ret) {
        ret.taint = f_t | scaleFactor_t;
        return StrictMath.scalb(f, scaleFactor);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double signum(double d, int d_t, Ret ret) {
        ret.taint = d_t;
        return StrictMath.signum(d);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final float signum(float f, int f_t, Ret ret) {
        ret.taint = f_t;
        return StrictMath.signum(f);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double sin(double a, int a_t, Ret ret) {
        ret.taint = a_t;
        return StrictMath.sin(a);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double sinh(double x, int x_t, Ret ret) {
        ret.taint = x_t;
        return StrictMath.sinh(x);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double sqrt(double a, int a_t, Ret ret) {
        ret.taint = a_t;
        return StrictMath.sqrt(a);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double tan(double a, int a_t, Ret ret) {
        ret.taint = a_t;
        return StrictMath.tan(a);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double tanh(double x, int x_t, Ret ret) {
        ret.taint = x_t;
        return StrictMath.tanh(x);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double toDegrees(double angrad, int angrad_t, Ret ret) {
        ret.taint = angrad_t;
        return StrictMath.toDegrees(angrad);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double toRadians(double angdeg, int angdeg_t, Ret ret) {
        ret.taint = angdeg_t;
        return StrictMath.toDegrees(angdeg);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final double ulp(double d, int d_t, Ret ret) {
        ret.taint = d_t;
        return StrictMath.ulp(d);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final float ulp(float f, int f_t, Ret ret) {
        ret.taint = f_t;
        return StrictMath.ulp(f);
    }
}
