package pac.util;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import pac.config.BaseConfig;
import pac.config.Notify;
import pac.config.NotifyMsg;
import pac.config.RunChecks;

/**
 * Utility class for handling overflows and optionally logging them out to the log file.
 * 
 * @author jeikenberry
 */
public final class Overflows {
  //#if log_overflow==true
  private static Map<Overflow, Integer> overflowed;
  private static Map<Overflow, Integer> dangerousOps;
  private static Map<Overflow, Integer> cleared;

  private static final Object lock = new Object();

  /**
   * Marks that a particular operation has overflowed (if logging is enabled).
   * 
   * @param taint of the overflowed result.
   * @param operation String of the operation.
   * @return taint
   */
  public static final int overflow(int taint, String operation) {
    if (Notify.initialized) {
      synchronized (lock) {
        if (overflowed == null)
          overflowed = new TreeMap<Overflow, Integer>();
        Overflow overflow = new Overflow(taint, operation);
        Integer count = overflowed.get(overflow);
        if (count == null) {
          overflowed.put(overflow, 1);
        } else {
          overflowed.put(overflow, count + 1);
        }
      }
    }
    return taint;
  }

  /**
   * Logs the overflow/underflow (if the taint is marked with one).
   * 
   * @param taint of the single operand.
   * @param operation that caused the overflow log.
   * @throws IOException
   */
  public static final void logOverflow(int taint, String operation) {
    if (!Notify.initialized)
      return;

    if ((taint & (TaintValues.OVERFLOW | TaintValues.UNDERFLOW | TaintValues.INFINITY)) != 0) {
      synchronized (lock) {
        if (dangerousOps == null)
          dangerousOps = new TreeMap<Overflow, Integer>();
        Overflow overflow = new Overflow(taint, operation);
        Integer count = dangerousOps.get(overflow);
        if (count == null) {
          dangerousOps.put(overflow, 1);
        } else {
          dangerousOps.put(overflow, count + 1);
        }
      }
    }
  }
  //#endif

  /**
   * Called the handle overflows/underflows from dangerous array index operations.
   * 
   * @param taint of the single operand.
   * @param operation that caused the overflow check.
   * @param value if operation was "store-array", or null.
   * @param indices of the array.
   * @throws IOException
   */
  public static final void checkOverflow(int taint, String operation, String type, Object value,
      int... indices) {
    //#if log_overflow==true
    logOverflow(taint, operation);
    //#endif
    if (!TaintUtils.isTrusted(taint)) {
      if ((taint & TaintValues.INFINITY) == TaintValues.INFINITY) {
        BaseConfig.getInstance().handleInfinity(buildDescription(operation, type, value, indices),
            operation);
      }
      if ((taint & TaintValues.OVERFLOW) == TaintValues.OVERFLOW) {
        BaseConfig.getInstance().handleOverflow(buildDescription(operation, type, value, indices),
            operation);
      }
      if ((taint & TaintValues.UNDERFLOW) == TaintValues.UNDERFLOW) {
        BaseConfig.getInstance().handleUnderflow(buildDescription(operation, type, value, indices),
            operation);
      }
    }
  }

  /**
   * Builds a description of the overflow check for array indexing. For compatibility reasons, do
   * not attempt to call this method directly from the operation. Instead, it needs to be called
   * only when a tainted underflow/overflow/infinity has occurred.
   * 
   * @param operation
   * @param type
   * @param value
   * @param indices
   * @return
   */
  private static final String buildDescription(String operation, String type, Object value,
      int... indices) {
    StringBuilder buf = new StringBuilder();
    if (operation.equals("new-array"))
      buf.append("new ");
    buf.append(type);
    for (int i = 0; i < indices.length; i++) {
      buf.append('[');
      buf.append(indices[i]);
      buf.append(']');
    }
    if (value == null)
      return buf.toString();
    buf.append(" = ");
    if (value instanceof Character) {
      buf.append("'");
      buf.append(value);
      buf.append("'");
    } else {
      buf.append(value);
    }
    return buf.toString();
  }

  /**
   * Called the handle overflows/underflows from dangerous operations.
   * 
   * @param op1_taint first operands taint
   * @param op2_taint second operands taint
   * @param description of operation
   * @param operation that caused the overflow check
   * @throws IOException
   */
  public static final void checkOverflow(int op1_taint, int op2_taint, String description,
      String operation) throws IOException {
    //#if log_overflow==true
    logOverflow(op1_taint | op2_taint, operation);
    //#endif
    if (!TaintUtils.isTrusted(op1_taint)) {
      if (!TaintUtils.isTrusted(op2_taint)
          && (op1_taint & TaintValues.INFINITY) == TaintValues.INFINITY
          && (op2_taint & TaintValues.INFINITY) == TaintValues.INFINITY)
        BaseConfig.getInstance().handleInfinity(description, operation);
      else if ((op1_taint & TaintValues.OVERFLOW) == TaintValues.OVERFLOW)
        BaseConfig.getInstance().handleOverflow(description, operation);
      else if ((op1_taint & TaintValues.UNDERFLOW) == TaintValues.UNDERFLOW)
        BaseConfig.getInstance().handleUnderflow(description, operation);
    } else if (!TaintUtils.isTrusted(op2_taint)) {
      if ((op2_taint & TaintValues.OVERFLOW) == TaintValues.OVERFLOW)
        BaseConfig.getInstance().handleOverflow(description, operation);
      else if ((op2_taint & TaintValues.UNDERFLOW) == TaintValues.UNDERFLOW)
        BaseConfig.getInstance().handleUnderflow(description, operation);
    }
  }

  /**
   * Called to clear overflow/underflow from taint.
   * 
   * @param taint to clear.
   * @param operation that caused the clear.
   * @return taint with the overflow/underflow bits unset.
   */
  public static final int clearOverflow(int taint, String operation) {
    //#if log_overflow==true
    boolean reset = false;
    if ((taint & TaintValues.OVERFLOW_MASK) != 0) {
      taint &= ~TaintValues.OVERFLOW_MASK;
      reset = true;
    }

    if (Notify.initialized && reset) {
      synchronized (lock) {
        if (cleared == null)
          cleared = new TreeMap<Overflow, Integer>();
        Overflow overflow = new Overflow(taint, operation);
        Integer count = cleared.get(overflow);
        if (count == null)
          cleared.put(overflow, 1);
        else
          cleared.put(overflow, count + 1);
      }
    }
    //#else
    taint &= ~TaintValues.OVERFLOW_MASK;
    //#endif
    return taint;
  }

  /**
   * Called to clear overflow/underflow from taint only if one of the operands does not have an
   * overflow or underflow bit set.
   * 
   * @param op1_taint first operands taint.
   * @param op2_taint second operands taint.
   * @param operation that caused the clear.
   * @return taint with the overflow/underflow bits unset.
   */
  public static final int clearOverflow(int op1_taint, int op2_taint, String operation) {
    if (neitherOverOrUnderFlow(op1_taint) || neitherOverOrUnderFlow(op2_taint))
      return clearOverflow(op1_taint | op2_taint, operation);
    return op1_taint | op2_taint;
  }

  private static final boolean neitherOverOrUnderFlow(int taint) {
    return (taint & (TaintValues.OVERFLOW | TaintValues.UNDERFLOW)) == 0;
  }

  public static final void outOfBounds(String operation, String type, Object array, int idx) {
    NotifyMsg notifyMsg =
        new NotifyMsg(buildDescription(operation, type, null, idx), operation, 129);
    notifyMsg.append("Attempting to access " + type + " array element on an '" + operation
        + "' operation that went out of bound at index " + idx + ".");
    String value = type.equals("object") ? "null" : "0";
    if (operation.equals("array-load"))
      notifyMsg.append(" Returning default value (" + value + ").");
    else if (operation.equals("array-store"))
      notifyMsg.append(" Returning without storing.");
    notifyMsg.setAction(RunChecks.REPLACE_ACTION);
    Notify.notifyAndRespond(notifyMsg);
  }

  public static final int checkAllocSize(String type, int size) throws IOException {
    int max = BaseConfig.getInstance().getMaxAllocSize();
    if (size >= max) {
      return BaseConfig.getInstance()
          .handleMaxAllocation(buildDescription("new-array", type, null, size), "new-array", size);
    }
    return size;
  }

  //#if log_overflow==true
  public static final void print(Writer out) throws IOException {
    synchronized (lock) {
      print(out, "OVERFLOWS", overflowed);
      print(out, "DANGEROUS OPS", dangerousOps);
      print(out, "CLEARED", cleared);
    }
  }

  private static final void print(Writer out, String title, Map<Overflow, Integer> overflows)
      throws IOException {
    if (overflows == null)
      return;
    out.write("\n" + title + ": \n");
    int trusted = 0;
    int tainted = 0;
    int trustedUniq = 0;
    int taintedUniq = 0;
    // int hashCodes = 0;

    HashMap<String, Integer> totalMap = new HashMap<String, Integer>();
    HashMap<String, Integer> distinctMap = new HashMap<String, Integer>();
    TreeSet<String> taintedLocations = new TreeSet<String>();
    TreeSet<String> trustedLocations = new TreeSet<String>();
    for (Entry<Overflow, Integer> entry : overflows.entrySet()) {
      Overflow overflow = entry.getKey();
      int count = entry.getValue();
      // out.write(count + " -- " + overflow.toString() + "\n");
      if (overflow.isTainted()) {
        taintedLocations.add(overflow.stackTop);
        tainted += count;
        taintedUniq++;
      } else {
        trustedLocations.add(overflow.stackTop);
        trusted += count;
        trustedUniq++;
      }
      Integer c = totalMap.get(overflow.op);
      if (c == null) {
        totalMap.put(overflow.op, count);
      } else {
        totalMap.put(overflow.op, c + count);
      }
      c = distinctMap.get(overflow.op);
      if (c == null) {
        distinctMap.put(overflow.op, 1);
      } else {
        distinctMap.put(overflow.op, c + 1);
      }
      // if (overflow.isHashCode())
      //   hashCodes++;
    }
    out.write("BY OPERATION:\n");
    for (String op : totalMap.keySet()) {
      out.write("  " + op + " -- " + totalMap.get(op) + " total / " + distinctMap.get(op)
          + " distinct\n");
    }
    out.write("TOTAL TRUSTED: " + trusted + "\n");
    out.write("TOTAL TAINTED: " + tainted + "\n");
    // out.write("TOTAL HASH CODES: " + hashCodes + "\n");
    out.write("TOTAL: " + (trusted + tainted) + "\n");
    out.write("TOTAL TRUSTED (DISTINCT): " + trustedUniq + "\n");
    out.write("TOTAL TAINTED (DISTINCT): " + taintedUniq + "\n");
    // out.write("TOTAL HASH CODES: " + hashCodes + "\n");
    out.write("TOTAL (DISTINCT): " + (trustedUniq + taintedUniq) + "\n");
    // if (dangerousOps == overflows) {
    out.write("STACK LOCATIONS:\n");
    for (String loc : taintedLocations) {
      out.write("  - " + loc + " -- TAINTED\n");
    }
    for (String loc : trustedLocations) {
      out.write("  - " + loc + " -- TRUSTED\n");
    }
    // }
  }

  private static final class Overflow implements Comparable<Overflow> {
    private StackTraceElement[] stackTrace;
    private String stackTop, op;
    // private boolean hashCode, clear;
    private int taint;

    public Overflow(int taint, String op) {
      this.stackTrace = Thread.currentThread().getStackTrace();
      this.taint = taint;
      this.op = op;
      boolean foundTop = false;
      for (int i = 3; i < stackTrace.length; i++) {
        if (!foundTop && !stackTrace[i].getClassName().startsWith("pac.")) {
          this.stackTop = stackTrace[i].getClassName() + "." + stackTrace[i].getMethodName()
              + " [line " + stackTrace[i].getLineNumber() + "]";
          foundTop = true;
          break;
        }
      }
    }

    public boolean isTainted() {
      return !TaintUtils.isTrusted(taint);
    }

    public String toString() {
      StringBuilder buf = new StringBuilder();
      if (TaintUtils.isTrusted(taint))
        buf.append("TRUSTED / ");
      else
        buf.append("TAINTED / ");
      buf.append(op + " / ");
      if ((taint & TaintValues.INFINITY) == TaintValues.INFINITY) {
        buf.append("INFINITY / ");
      }
      if ((taint & (TaintValues.OVERFLOW | TaintValues.UNDERFLOW)) != 0) {
        buf.append("OVERFLOW / ");
      }
      buf.append(stackTop);
      return buf.toString();
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Overflow))
        return false;
      return toString().equals(((Overflow) obj).toString());
    }

    @Override
    public int compareTo(Overflow overflow) {
      return toString().compareTo(overflow.toString());
    }
  }
  //#endif
}
