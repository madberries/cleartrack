package pac.inst.taint;

import java.io.IOException;

import pac.config.Notify;
import pac.config.NotifyMsg;
import pac.config.RunChecks;
import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationLocation;
import pac.inst.InstrumentationMethod;
import pac.inst.InvocationType;
import pac.util.Ret;
import pac.util.Overflows;
import pac.util.TaintUtils;
import pac.util.TaintValues;
import pac.wrap.TaintableArray;

/**
 * Instrumentation methods to be applied to individual JVM opcodes. Like any other method wrapper,
 * these can be inlined for further optimization.
 * 
 * @author ppiselli
 */
@InstrumentationClass("pac/inst/taint/InstructionInstrumentation")
public final class InstructionInstrumentation {

  /**
   * A constant holding the minimum value an {@code int} can have, -2<sup>31</sup>.
   */
  private static final int MIN_INT_VALUE = 0x80000000;

  /**
   * A constant holding the maximum value an {@code int} can have, 2<sup>31</sup>-1.
   */
  private static final int MAX_INT_VALUE = 0x7fffffff;

  private static final long SIGN_BIT_LONG = 0x8000000000000000L;

  @InstrumentationMethod(invocationType = InvocationType.STATIC,
      instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final int acmp(Object o1, Object o2) {
    if (o1 == o2)
      return 0;
    if (o1 == null || o2 == null)
      return 1;

    if ((o1 instanceof String) && (o2 instanceof String)) {
      // handle interning
      Object tmp = ((String) o1).ss_interned;
      if (tmp != null)
        o1 = tmp;

      tmp = ((String) o2).ss_interned;
      if (tmp != null)
        o2 = tmp;

      if (o1 == o2)
        return 0;
    } else if ((o1 instanceof TaintableArray) && (o2 instanceof TaintableArray)) {
      if (((TaintableArray) o1).getValue() == ((TaintableArray) o2).getValue())
        return 0;
      // TODO: Check the taint and output to the log if taints differ.
    }

    return 1;
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC,
      instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final int acmp(String o1, String o2) {
    if (o1 == o2)
      return 0;
    if (o1 == null || o2 == null)
      return 1;

    // handle interning
    String tmp = o1.ss_interned;
    if (tmp != null)
      o1 = tmp;

    tmp = o2.ss_interned;
    if (tmp != null)
      o2 = tmp;

    if (o1 == o2)
      return 0;

    return 1;
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC,
      instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final int acmp(String o1, Object o2) {
    if (o1 == o2)
      return 0;
    if (o1 == null || o2 == null)
      return 1;

    if (o2 instanceof String) {
      // handle interning
      String tmp = o1.ss_interned;
      if (tmp != null)
        o1 = tmp;

      tmp = ((String) o2).ss_interned;
      if (tmp != null)
        o2 = tmp;

      if (o1 == o2)
        return 0;
    }

    return 1;
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC,
      instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final int acmp(Object o1, String o2) {
    if (o1 == o2)
      return 0;
    if (o1 == null || o2 == null)
      return 1;

    if (o1 instanceof String) {
      // handle interning
      String tmp = ((String) o1).ss_interned;
      if (tmp != null)
        o1 = tmp;

      tmp = o2.ss_interned;
      if (tmp != null)
        o2 = tmp;

      if (o1 == o2)
        return 0;
    }

    return 1;
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC,
      instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final int acmp(TaintableArray o1, TaintableArray o2) {
    if (o1 == o2)
      return 0;
    if (o1 == null || o2 == null)
      return 1;

    return o1.getValue() == o2.getValue() ? 0 : 1;
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC,
      instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final int acmp(TaintableArray o1, Object o2) {
    if (o1 == o2)
      return 0;
    if (o1 == null || o2 == null)
      return 1;

    if (o2 instanceof TaintableArray) {
      if (o1.getValue() == ((TaintableArray) o2).getValue())
        return 0;
      // TODO: Check the taint and output to the log if taints differ.
    }

    return 1;
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC,
      instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final int acmp(Object o1, TaintableArray o2) {
    if (o1 == o2)
      return 0;
    if (o1 == null || o2 == null)
      return 1;

    if (o1 instanceof TaintableArray) {
      if (((TaintableArray) o1).getValue() == o2.getValue())
        return 0;
      // TODO: Check the taint and output to the log if taints differ.
    }

    return 1;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final int iadd(int op1, int op1_t, int op2, int op2_t, Ret ret) {
    // Treat the adding of two bitwise expression as an OR operation. So, let's not clear the
    // bitwise bit in this case.
    ret.taint = op1_t | op2_t;
    if (!TaintValues.isSet(op1_t, TaintValues.BITWISE_EXPR)
        || !TaintValues.isSet(op2_t, TaintValues.BITWISE_EXPR))
      ret.taint = TaintValues.unset(ret.taint, TaintValues.BITWISE_EXPR);
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Integer.MAX_VALUE;
    long result = ((long) op1) + op2;
    if (result > MAX_INT_VALUE) {
      ret.taint =
          //#if log_overflow==true
          Overflows.overflow(
          //#endif
              ret.taint | TaintValues.OVERFLOW
          //#if log_overflow==true
              , "+")
          //#endif
      ;
    } else if (result < MIN_INT_VALUE) {
      ret.taint =
          //#if log_overflow==true
          Overflows.overflow(
          //#endif
              ret.taint | TaintValues.UNDERFLOW
          //#if log_overflow==true
              , "+")
          //#endif
      ;
    }
    return (int) result;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final int isub(int op1, int op1_t, int op2, int op2_t, Ret ret) {
    ret.taint = TaintValues.unset(op1_t | op2_t, TaintValues.BITWISE_EXPR);
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Integer.MAX_VALUE;
    long result = ((long) op1) - op2;
    if (result > MAX_INT_VALUE) {
      ret.taint =
          //#if log_overflow==true
          Overflows.overflow(
          //#endif
              ret.taint | TaintValues.OVERFLOW
          //#if log_overflow==true
              , "-")
          //#endif
      ;
      return (int) result;
    } else if (result < MIN_INT_VALUE) {
      ret.taint =
          //#if log_overflow==true
          Overflows.overflow(
          //#endif
              ret.taint | TaintValues.UNDERFLOW
          //#if log_overflow==true
              , "-")
          //#endif
      ;
    }
    return (int) result;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final int imul(int op1, int op1_t, int op2, int op2_t, Ret ret) {
    ret.taint = TaintValues.unset(op1_t | op2_t, TaintValues.BITWISE_EXPR);
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Integer.MAX_VALUE;
    long result = ((long) op1) * op2;
    if (result > MAX_INT_VALUE) {
      ret.taint =
          //#if log_overflow==true
          Overflows.overflow(
          //#endif
              ret.taint | TaintValues.OVERFLOW
          //#if log_overflow==true
              , "*")
          //#endif
      ;
    } else if (result < MIN_INT_VALUE) {
      ret.taint =
          //#if log_overflow==true
          Overflows.overflow(
          //#endif
              ret.taint | TaintValues.UNDERFLOW
          //#if log_overflow==true
              , "*")
          //#endif
      ;
    }
    return (int) result;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final int idiv(int op1, int op1_t, int op2, int op2_t, Ret ret) {
    ret.taint = TaintValues.unset(op1_t | op2_t, TaintValues.BITWISE_EXPR);
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Integer.MAX_VALUE;
    if (op1 == Integer.MIN_VALUE && op2 == -1) {
      ret.taint =
          //#if log_overflow==true
          Overflows.overflow(
          //#endif
              ret.taint | TaintValues.OVERFLOW
          //#if log_overflow==true
              , "/")
          //#endif
      ;
    } else if (op2 == 0 && (ret.taint & TaintValues.TRUST_MASK) != TaintValues.TRUSTED) {
      // ConfigFile.handleInfinity("IDIV = " + op1.value + " / 0", "/", false);
      ret.taint =
          //#if log_overflow==true
          Overflows.overflow(
          //#endif
              ret.taint | TaintValues.INFINITY
          //#if log_overflow==true
              , "/")
          //#endif
      ;
      NotifyMsg notifyMsg = new NotifyMsg("idiv", "idiv", 369);
      notifyMsg.setAction(RunChecks.REPLACE_ACTION);
      notifyMsg.append("Attempting to divide " + op1 + " by zero ("
          + TaintUtils.toTrustString(op2_t) + ").  Returning Integer.MAX_VALUE.");
      Notify.notifyAndRespond(notifyMsg);
      return MAX_INT_VALUE;
    }
    // Presume that application knows what it's doing and is catching the ArithmeticException in the
    // case that we divide by zero in a trusted way.
    return op1 / op2;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final int idiv_no_overflow(int op1, int op1_t, int op2, int op2_t, Ret ret) {
    ret.taint = TaintValues.unset(op1_t | op2_t, TaintValues.BITWISE_EXPR);
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Integer.MAX_VALUE;
    if (op2 == 0 && (ret.taint & TaintValues.TRUST_MASK) != TaintValues.TRUSTED) {
      // ConfigFile.handleInfinity("IDIV = " + op1.value + " / 0", "/", false);
      ret.taint =
          //#if log_overflow==true
          Overflows.overflow(
          //#endif
              ret.taint | TaintValues.INFINITY
          //#if log_overflow==true
              , "/")
          //#endif
      ;
      NotifyMsg notifyMsg = new NotifyMsg("idiv", "idiv", 369);
      notifyMsg.setAction(RunChecks.REPLACE_ACTION);
      notifyMsg.append("Attempting to divide " + op1 + " by zero ("
          + TaintUtils.toTrustString(op2_t) + ").  Returning Integer.MAX_VALUE.");
      Notify.notifyAndRespond(notifyMsg);
      return MAX_INT_VALUE;
    }
    // presume that application knows what it's doing and is catching
    // the ArithmeticException in the case that we divide by zero in
    // a trusted way.
    return op1 / op2;
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC,
      instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final int irem(int op1, int op1_t, int op2, int op2_t, Ret ret) {
    ret.taint = TaintValues.unset(op1_t | op2_t, TaintValues.BITWISE_EXPR);
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY) {
      return Integer.MAX_VALUE;
    } else if (op2 == 0 && (ret.taint & TaintValues.TRUST_MASK) != TaintValues.TRUSTED) {
      // ConfigFile.handleInfinity("IREM = " + op1.value + " % 0", "%", false);
      ret.taint =
          //#if log_overflow==true
          Overflows.overflow(
          //#endif
              ret.taint | TaintValues.INFINITY
          //#if log_overflow==true
              , "%")
          //#endif
      ;
      NotifyMsg notifyMsg = new NotifyMsg("irem", "irem", 369);
      notifyMsg.setAction(RunChecks.REPLACE_ACTION);
      notifyMsg.append("Attempting to divide " + op1 + " by zero ("
          + TaintUtils.toTrustString(op2_t) + ").  Returning Integer.MAX_VALUE.");
      Notify.notifyAndRespond(notifyMsg);
      return MAX_INT_VALUE;
    }
    return op1 % op2;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final int ior(int op1, int op1_t, int op2, int op2_t, Ret ret) {
    ret.taint = op1_t | op2_t | TaintValues.BITWISE_EXPR;
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Integer.MAX_VALUE;
    return op1 | op2;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final int iand(int op1, int op1_t, int op2, int op2_t, Ret ret) {
    if ((op1_t & TaintValues.INFINITY) == TaintValues.INFINITY
        || (op2_t & TaintValues.INFINITY) == TaintValues.INFINITY) {
      ret.taint = op1_t | op2_t | TaintValues.BITWISE_EXPR;
      return Integer.MAX_VALUE;
    }
    ret.taint = Overflows.clearOverflow(op1_t, op2_t, "&") | TaintValues.BITWISE_EXPR;
    return op1 & op2;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final int ixor(int op1, int op1_t, int op2, int op2_t, Ret ret) {
    ret.taint = op1_t | op2_t | TaintValues.BITWISE_EXPR;
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Integer.MAX_VALUE;
    return op1 ^ op2;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final long i2l(int op, int op_t, Ret ret) {
    ret.taint = op_t;
    if ((op_t & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Integer.MAX_VALUE;
    return (long) op;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final byte i2b(int op, int op_t, Ret ret) {
    if ((op_t & TaintValues.INFINITY) == TaintValues.INFINITY) {
      ret.taint = op_t;
      return Byte.MAX_VALUE;
    }
    ret.taint = Overflows.clearOverflow(op_t, "trunc");
    return (byte) op;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final byte i2b_app(int op, int op_t, Ret ret) {
    ret.taint = op_t;
    if ((op_t & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Byte.MAX_VALUE;
    byte result = (byte) op;
    // If the BITWISE_EXPR flag is set, we assume the programmer knows what he's doing...
    if (op > 0xff || (op_t & TaintValues.BITWISE_EXPR) != TaintValues.BITWISE_EXPR) {
      if (result > op)
        ret.taint =
            //#if log_overflow==true
            Overflows.overflow(
            //#endif
                op_t | TaintValues.UNDERFLOW
            //#if log_overflow==true
                , "trunc")
            //#endif
        ;
      else if (result < op)
        ret.taint =
            //#if log_overflow==true
            Overflows.overflow(
            //#endif
                op_t | TaintValues.OVERFLOW
            //#if log_overflow==true
                , "trunc")
            //#endif
        ;
    }
    return result;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final short i2s(int op, int op_t, Ret ret) {
    ret.taint = op_t;
    if ((op_t & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Short.MAX_VALUE;
    ret.taint = Overflows.clearOverflow(op_t, "trunc");
    return (short) op;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final short i2s_app(int op, int op_t, Ret ret) {
    ret.taint = op_t;
    if ((op_t & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Short.MAX_VALUE;
    short result = (short) op;
    // If the BITWISE_EXPR flag is set, we assume the programmer knows what he's doing...
    if (op > 0xffff || (op_t & TaintValues.BITWISE_EXPR) != TaintValues.BITWISE_EXPR) {
      if (result > op)
        ret.taint =
            //#if log_overflow==true
            Overflows.overflow(
            //#endif
                op_t | TaintValues.UNDERFLOW
            //#if log_overflow==true
                , "trunc")
            //#endif
        ;
      else if (result < op)
        ret.taint =
            //#if log_overflow==true
            Overflows.overflow(
            //#endif
                op_t | TaintValues.OVERFLOW
            //#if log_overflow==true
                , "trunc")
            //#endif
        ;
    }
    return result;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final char i2c(int op, int op_t, Ret ret) {
    if ((op_t & TaintValues.INFINITY) == TaintValues.INFINITY) {
      ret.taint = op_t;
      return Character.MAX_VALUE;
    }
    ret.taint = Overflows.clearOverflow(op_t, "trunc");
    return (char) op;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final char i2c_app(int op, int op_t, Ret ret) {
    ret.taint = op_t;
    if ((op_t & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Character.MAX_VALUE;
    if (op < 0) {
      // we need to treat this as an overflow, since chars are
      // unsigned. hence, any conversion back to a signed integer
      // would therefore produce incorrect results...
      ret.taint =
          //#if log_overflow==true
          Overflows.overflow(
          //#endif
              op_t | TaintValues.OVERFLOW
          //#if log_overflow==true
              , "trunc")
          //#endif
      ;
    }
    return (char) op;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final float i2f(int op, int op_t, Ret ret) {
    ret.taint = op_t;
    if ((op_t & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Float.MAX_VALUE;
    return (float) op;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final double i2d(int op, int op_t, Ret ret) {
    ret.taint = op_t;
    if ((op_t & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Double.MAX_VALUE;
    return (double) op;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final int ineg(int op, int op_t, Ret ret) {
    ret.taint = op_t;
    if ((op_t & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Integer.MAX_VALUE;
    return -op;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final int iushr(int op1, int op1_t, int op2, int op2_t, Ret ret) {
    ret.taint = op1_t | op2_t | TaintValues.BITWISE_EXPR;
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Integer.MAX_VALUE;
    if (op2 == 0 && (op2_t & TaintValues.OVERFLOW_MASK) != TaintValues.OVERFLOW_MASK)
      return op1 >>> op2;
    return op1 >>> op2;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final int ishr(int op1, int op1_t, int op2, int op2_t, Ret ret) {
    ret.taint = op1_t | op2_t | TaintValues.BITWISE_EXPR;
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Integer.MAX_VALUE;
    return op1 >> op2;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final int ishl(int op1, int op1_t, int op2, int op2_t, Ret ret) {
    ret.taint = op1_t | op2_t | TaintValues.BITWISE_EXPR;
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Integer.MAX_VALUE;
    int result = op1 << op2;
    if (result < 0 && op1 > 0) {
      if ((result >>> op2) != op1) {
        ret.taint =
            //#if log_overflow==true
            Overflows.overflow(
            //#endif
                ret.taint | TaintValues.OVERFLOW
            //#if log_overflow==true
                , "<<")
            //#endif
        ;
      }
    } else if ((result >> op2) != op1) {
      ret.taint =
          //#if log_overflow==true
          Overflows.overflow(
          //#endif
              ret.taint | TaintValues.OVERFLOW
          //#if log_overflow==true
              , "<<")
          //#endif
      ;
    }
    return result;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static long ladd(long op1, int op1_t, long op2, int op2_t, Ret ret) {
    // Treat the adding of two bitwise expression as an OR operation.
    // So, let's not clear the bitwise bit in this case.
    ret.taint = op1_t | op2_t;
    if (!TaintValues.isSet(op1_t, TaintValues.BITWISE_EXPR)
        || !TaintValues.isSet(op2_t, TaintValues.BITWISE_EXPR))
      ret.taint = TaintValues.unset(ret.taint, TaintValues.BITWISE_EXPR);
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Long.MAX_VALUE;
    long result = op1 + op2;
    long sresult = SIGN_BIT_LONG & result;
    if ((SIGN_BIT_LONG & op1) == sresult)
      return result;
    if ((SIGN_BIT_LONG & op2) == sresult)
      return result;
    if (sresult < 0) {
      ret.taint =
          //#if log_overflow==true
          Overflows.overflow(
          //#endif
              ret.taint | TaintValues.OVERFLOW
          //#if log_overflow==true
              , "+")
          //#endif
      ;
    } else {
      ret.taint =
          //#if log_overflow==true
          Overflows.overflow(
          //#endif
              ret.taint | TaintValues.UNDERFLOW
          //#if log_overflow==true
              , "+")
          //#endif
      ;
    }
    return result;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static long lsub(long op1, int op1_t, long op2, int op2_t, Ret ret) {
    ret.taint = TaintValues.unset(op1_t | op2_t, TaintValues.BITWISE_EXPR);
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Long.MAX_VALUE;
    long result = op1 - op2;
    long sresult = SIGN_BIT_LONG & result;
    if ((SIGN_BIT_LONG & op1) == sresult)
      return result;
    if ((SIGN_BIT_LONG & op2) != sresult)
      return result;
    if (sresult < 0) {
      ret.taint =
          //#if log_overflow==true
          Overflows.overflow(
          //#endif
              ret.taint | TaintValues.OVERFLOW
          //#if log_overflow==true
              , "-")
          //#endif
      ;
    } else {
      ret.taint =
          //#if log_overflow==true
          Overflows.overflow(
          //#endif
              ret.taint | TaintValues.UNDERFLOW
          //#if log_overflow==true
              , "-")
          //#endif
      ;
    }
    return result;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static long lmul(long op1, int op1_t, long op2, int op2_t, Ret ret) {
    ret.taint = TaintValues.unset(op1_t | op2_t, TaintValues.BITWISE_EXPR);
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Long.MAX_VALUE;
    if (op2 == 0)
      return 0;
    long result = op1 * op2;
    long tmp = result / op2;
    if (tmp < op1 || tmp > op1) {
      // Unfortunately we can't tell if we overflow or underflow, so mark both taint bits...
      ret.taint =
          //#if log_overflow==true
          Overflows.overflow(
          //#endif
              ret.taint | TaintValues.UNDERFLOW | TaintValues.OVERFLOW
          //#if log_overflow==true
              , "*")
          //#endif
      ;
    }
    return result;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static long ldiv(long op1, int op1_t, long op2, int op2_t, Ret ret) {
    ret.taint = TaintValues.unset(op1_t | op2_t, TaintValues.BITWISE_EXPR);
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Long.MAX_VALUE;
    if (op1 == Long.MIN_VALUE && op2 == -1) {
      ret.taint =
          //#if log_overflow==true
          Overflows.overflow(
          //#endif
              ret.taint | TaintValues.OVERFLOW
          //#if log_overflow==true
              , "/")
          //#endif
      ;
    } else if (op2 == 0 && (ret.taint & TaintValues.TRUST_MASK) != TaintValues.TRUSTED) {
      // ConfigFile.handleInfinity("LDIV = " + op1.value + " / 0", "/", false);
      ret.taint =
          //#if log_overflow==true
          Overflows.overflow(
          //#endif
              ret.taint | TaintValues.INFINITY
          //#if log_overflow==true
              , "/")
          //#endif
      ;
      NotifyMsg notifyMsg = new NotifyMsg("ldiv", "ldiv", 369);
      notifyMsg.setAction(RunChecks.REPLACE_ACTION);
      notifyMsg.append("Attempting to divide " + op1 + " by zero ("
          + TaintUtils.toTrustString(op2_t) + ").  Returning Long.MAX_VALUE.");
      Notify.notifyAndRespond(notifyMsg);
      return Long.MAX_VALUE;
    }
    // Presume that application knows what it's doing and is catching the ArithmeticException in the
    // case that we divide by zero in a trusted way.
    return op1 / op2;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static long ldiv_no_overflow(long op1, int op1_t, long op2, int op2_t, Ret ret) {
    ret.taint = TaintValues.unset(op1_t | op2_t, TaintValues.BITWISE_EXPR);
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Long.MAX_VALUE;
    if (op2 == 0 && (ret.taint & TaintValues.TRUST_MASK) != TaintValues.TRUSTED) {
      // ConfigFile.handleInfinity("LDIV = " + op1.value + " / 0", "/", false);
      ret.taint =
          //#if log_overflow==true
          Overflows.overflow(
          //#endif
              ret.taint | TaintValues.INFINITY
          //#if log_overflow==true
              , "/")
          //#endif
      ;
      NotifyMsg notifyMsg = new NotifyMsg("ldiv", "ldiv", 369);
      notifyMsg.setAction(RunChecks.REPLACE_ACTION);
      notifyMsg.append("Attempting to divide " + op1 + " by zero ("
          + TaintUtils.toTrustString(op2_t) + ").  Returning Long.MAX_VALUE.");
      Notify.notifyAndRespond(notifyMsg);
      return Long.MAX_VALUE;
    }
    // Presume that application knows what it's doing and is catching the ArithmeticException in the
    // case that we divide by zero in a trusted way.
    return op1 / op2;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static long lrem(long op1, int op1_t, long op2, int op2_t, Ret ret) {
    ret.taint = TaintValues.unset(op1_t | op2_t, TaintValues.BITWISE_EXPR);
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY) {
      return Long.MAX_VALUE;
    } else if (op2 == 0 && (ret.taint & TaintValues.TRUST_MASK) != TaintValues.TRUSTED) {
      // ConfigFile.handleInfinity("LREM = " + op1.value + " % 0", "%", false);
      ret.taint =
          //#if log_overflow==true
          Overflows.overflow(
          //#endif
              ret.taint | TaintValues.INFINITY
          //#if log_overflow==true
              , "%")
          //#endif
      ;
      NotifyMsg notifyMsg = new NotifyMsg("lrem", "lrem", 369);
      notifyMsg.setAction(RunChecks.REPLACE_ACTION);
      notifyMsg.append("Attempting to divide " + op1 + " by zero ("
          + TaintUtils.toTrustString(op2_t) + ").  Returning Long.MAX_VALUE.");
      Notify.notifyAndRespond(notifyMsg);
      return Long.MAX_VALUE;
    }
    return op1 % op2;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static long lor(long op1, int op1_t, long op2, int op2_t, Ret ret) {
    ret.taint = op1_t | op2_t | TaintValues.BITWISE_EXPR;
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Long.MAX_VALUE;
    return op1 | op2;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static long land(long op1, int op1_t, long op2, int op2_t, Ret ret) {
    if ((op1_t & TaintValues.INFINITY) == TaintValues.INFINITY
        || (op2_t & TaintValues.INFINITY) == TaintValues.INFINITY) {
      ret.taint = op1_t | op2_t | TaintValues.BITWISE_EXPR;
      return Long.MAX_VALUE;
    }
    ret.taint = Overflows.clearOverflow(op1_t, op2_t, "&") | TaintValues.BITWISE_EXPR;
    return op1 & op2;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static long lxor(long op1, int op1_t, long op2, int op2_t, Ret ret) {
    ret.taint = op1_t | op2_t | TaintValues.BITWISE_EXPR;
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Long.MAX_VALUE;
    return op1 ^ op2;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static int l2i(long op, int op_t, Ret ret) {
    if ((op_t & TaintValues.INFINITY) == TaintValues.INFINITY) {
      ret.taint = op_t;
      return Integer.MAX_VALUE;
    }
    ret.taint = Overflows.clearOverflow(op_t, "trunc");
    return (int) op;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static int l2i_app(long op, int op_t, Ret ret) {
    ret.taint = op_t;
    if ((op_t & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Integer.MAX_VALUE;
    int result = (int) op;
    if (result > op) {
      ret.taint =
          //#if log_overflow==true
          Overflows.overflow(
          //#endif
              op_t | TaintValues.UNDERFLOW
          //#if log_overflow==true
              , "trunc")
          //#endif
      ;
    } else if (result < op) {
      ret.taint =
          //#if log_overflow==true
          Overflows.overflow(
          //#endif
              op_t | TaintValues.OVERFLOW
          //#if log_overflow==true
              , "trunc")
          //#endif
      ;
    }
    return result;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static float l2f(long op, int op_t, Ret ret) {
    ret.taint = op_t;
    if ((op_t & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Float.MAX_VALUE;
    return (float) op;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static double l2d(long op, int op_t, Ret ret) {
    ret.taint = op_t;
    if ((op_t & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Double.MAX_VALUE;
    return (double) op;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static long lneg(long op, int op_t, Ret ret) {
    ret.taint = op_t;
    if ((op_t & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Long.MAX_VALUE;
    return -op;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static long lushr(long op1, int op1_t, int op2, int op2_t, Ret ret) {
    ret.taint = op1_t | op2_t | TaintValues.BITWISE_EXPR;
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Long.MAX_VALUE;
    if (op2 == 0 && (op2_t & TaintValues.OVERFLOW_MASK) != TaintValues.OVERFLOW_MASK)
      return op1 >>> op2;
    return op1 >>> op2;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static long lshr(long op1, int op1_t, int op2, int op2_t, Ret ret) {
    ret.taint = op1_t | op2_t | TaintValues.BITWISE_EXPR;
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Long.MAX_VALUE;
    return op1 >> op2;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static long lshl(long op1, int op1_t, int op2, int op2_t, Ret ret) {
    ret.taint = op1_t | op2_t | TaintValues.BITWISE_EXPR;
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Long.MAX_VALUE;
    long result = op1 << op2;
    if (result < 0 && op1 > 0) {
      if ((result >>> op2) != op1) {
        ret.taint =
            //#if log_overflow==true
            Overflows.overflow(
            //#endif
                ret.taint | TaintValues.OVERFLOW
            //#if log_overflow==true
                , "<<")
            //#endif
        ;
      }
    } else if ((result >> op2) != op1) {
      ret.taint =
          //#if log_overflow==true
          Overflows.overflow(
          //#endif
              ret.taint | TaintValues.OVERFLOW
          //#if log_overflow==true
              , "<<")
          //#endif
      ;
    }
    return result;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final int lcmp(long op1, int op1_t, long op2, int op2_t, Ret ret) {
    ret.taint = op1_t | op2_t;
    return op1 > op2 ? 1 : (op1 < op2 ? -1 : 0);
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final float fadd(float op1, int op1_t, float op2, int op2_t, Ret ret) {
    ret.taint = TaintValues.unset(op1_t | op2_t, TaintValues.BITWISE_EXPR);
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Float.MAX_VALUE;
    return op1 + op2;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final float fsub(float op1, int op1_t, float op2, int op2_t, Ret ret) {
    ret.taint = TaintValues.unset(op1_t | op2_t, TaintValues.BITWISE_EXPR);
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Float.MAX_VALUE;
    return op1 - op2;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final float fmul(float op1, int op1_t, float op2, int op2_t, Ret ret) {
    ret.taint = TaintValues.unset(op1_t | op2_t, TaintValues.BITWISE_EXPR);
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Float.MAX_VALUE;
    return op1 * op2;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final float fdiv(float op1, int op1_t, float op2, int op2_t, Ret ret) {
    ret.taint = TaintValues.unset(op1_t | op2_t, TaintValues.BITWISE_EXPR);
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Float.MAX_VALUE;
    else if (op2 == 0 && (ret.taint & TaintValues.TRUST_MASK) != TaintValues.TRUSTED) {
      // ConfigFile.handleInfinity("FDIV = " + op1.value + " / 0", "/", false);
      ret.taint =
          //#if log_overflow==true
          Overflows.overflow(
          //#endif
              ret.taint | TaintValues.INFINITY
          //#if log_overflow==true
              , "/")
          //#endif
      ;
      return Float.MAX_VALUE;
    }
    // presume that application knows what it's doing and is catching
    // the ArithmeticException in the case that we divide by zero in
    // a trusted way.
    return op1 / op2;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final float frem(float op1, int op1_t, float op2, int op2_t, Ret ret) {
    ret.taint = TaintValues.unset(op1_t | op2_t, TaintValues.BITWISE_EXPR);
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Float.MAX_VALUE;
    return op1 % op2;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final int f2i(float op, int op_t, Ret ret) {
    ret.taint = op_t;
    if ((op_t & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Integer.MAX_VALUE;
    return (int) op;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final double f2d(float op, int op_t, Ret ret) {
    ret.taint = op_t;
    if ((op_t & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Double.MAX_VALUE;
    return (double) op;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final long f2l(float op, int op_t, Ret ret) {
    ret.taint = op_t;
    if ((op_t & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Long.MAX_VALUE;
    return (long) op;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final float fneg(float op, int op_t, Ret ret) {
    ret.taint = op_t;
    if ((op_t & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Float.MAX_VALUE;
    return -op;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final int fcmpl(float op1, int op1_t, float op2, int op2_t, Ret ret) {
    ret.taint = op1_t | op2_t;
    if (Float.isNaN(op1) || Float.isNaN(op2))
      return -1;
    return op1 > op2 ? 1 : (op1 < op2 ? -1 : 0);
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final int fcmpg(float op1, int op1_t, float op2, int op2_t, Ret ret) {
    ret.taint = op1_t | op2_t;
    if (Float.isNaN(op1) || Float.isNaN(op2))
      return 1;
    return op1 > op2 ? 1 : (op1 < op2 ? -1 : 0);
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final double dadd(double op1, int op1_t, double op2, int op2_t, Ret ret) {
    ret.taint = TaintValues.unset(op1_t | op2_t, TaintValues.BITWISE_EXPR);
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Double.MAX_VALUE;
    return op1 + op2;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final double dsub(double op1, int op1_t, double op2, int op2_t, Ret ret) {
    ret.taint = TaintValues.unset(op1_t | op2_t, TaintValues.BITWISE_EXPR);
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Double.MAX_VALUE;
    return op1 - op2;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final double dmul(double op1, int op1_t, double op2, int op2_t, Ret ret) {
    ret.taint = TaintValues.unset(op1_t | op2_t, TaintValues.BITWISE_EXPR);
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Double.MAX_VALUE;
    return op1 * op2;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final double ddiv(double op1, int op1_t, double op2, int op2_t, Ret ret) {
    ret.taint = TaintValues.unset(op1_t | op2_t, TaintValues.BITWISE_EXPR);
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Double.MAX_VALUE;
    else if (op2 == 0 && (ret.taint & TaintValues.TRUST_MASK) != TaintValues.TRUSTED) {
      // ConfigFile.handleInfinity("DDIV = " + op1.value + " / 0", "/", false);
      ret.taint =
          //#if log_overflow==true
          Overflows.overflow(
          //#endif
              ret.taint | TaintValues.INFINITY
          //#if log_overflow==true
              , "/")
          //#endif
      ;
      return Double.MAX_VALUE;
    }
    // Presume that application knows what it's doing and is catching the ArithmeticException in the
    // case that we divide by zero in a trusted way.
    return op1 / op2;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final double drem(double op1, int op1_t, double op2, int op2_t, Ret ret) {
    ret.taint = TaintValues.unset(op1_t | op2_t, TaintValues.BITWISE_EXPR);
    if ((ret.taint & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Double.MAX_VALUE;
    return op1 % op2;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final int d2i(double op, int op_t, Ret ret) {
    ret.taint = op_t;
    if ((op_t & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Integer.MAX_VALUE;
    return (int) op;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final float d2f(double op, int op_t, Ret ret) {
    ret.taint = op_t;
    if ((op_t & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Float.MAX_VALUE;
    return (float) op;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final long d2l(double op, int op_t, Ret ret) {
    ret.taint = op_t;
    if ((op_t & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Long.MAX_VALUE;
    return (long) op;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final double dneg(double op, int op_t, Ret ret) {
    ret.taint = op_t;
    if ((op_t & TaintValues.INFINITY) == TaintValues.INFINITY)
      return Double.MAX_VALUE;
    return -op;
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final int dcmpl(double op1, int op1_t, double op2, int op2_t, Ret ret) {
    ret.taint = op1_t | op2_t;
    if (Double.isNaN(op1) || Double.isNaN(op2))
      return -1;
    return op1 > op2 ? 1 : (op1 < op2 ? -1 : 0);
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final int dcmpg(double op1, int op1_t, double op2, int op2_t, Ret ret) {
    ret.taint = op1_t | op2_t;
    if (Double.isNaN(op1) || Double.isNaN(op2))
      return 1;
    return op1 > op2 ? 1 : (op1 < op2 ? -1 : 0);
  }

  // @InstrumentationMethod(invocationType = InvocationType.STATIC,
  // instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
  public static final int handleUnsafeUSHR(int result, int result_t, int mask, int mask_t, Ret ret)
      throws IOException {
    int x = result & mask;
    ret.taint = result_t;
    if (x != result && (result_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED) {
      NotifyMsg notifyMsg = new NotifyMsg("IntTaint.handleUnsafeUSHR(int, int)",
          "IntTaint.handleUnsafeUSHR(" + result + ", " + mask + ")", 194);
      notifyMsg.setAction(RunChecks.REMOVE_ACTION);
      notifyMsg.append(
          "Performed a dangerous unsigned right-shift on a tainted value. Masking off sign bits.");
      Notify.notifyAndRespond(notifyMsg);
    }
    return x;
  }

}
