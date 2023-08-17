/**
 * 
 */
package pac.util;

/**
 * The hashmap is no longer used to store metadata. All metadata is stored
 * in primitives (not on objects). Since primitives in turn become objects, the
 * taint is stored inside these wrapper objects and stored into fields (or
 * passed around as parameters).
 * 
 * Taint is stored as a 32-bit integer which is masked off into 4 sections:
 * 
 * Higher order Lower order
 * --------------------------------
 * PPPPPIIIIIIIIIIIIIIEEEEEEEEEEETT
 * 
 * P - Primitive/Numeric Info
 * I - Input Type
 * E - Encoding
 * T - Taint
 * 
 * @author ppiselli
 */
public final class TaintValues {
  public static final int NUM_OF_ENCODINGS = 11;
  public static final int NUM_OF_INPUTTYPES = 14;
  public static final int NUM_OF_PRIMITIVETYPES = 5;

  /*
   * We can save an extra bit by assigning the following values to taint levels. Trusted should be
   * zero, since any uninitialized value should beassumed trusted. Also, consider each case.
   * 
   *      Tr Ta Un
   *      -- -- --
   * Tr | Tr Ta Un
   * Ta | Ta Ta Ta
   * Un | Un Ta Un
   * 
   * Therefore the only possible valid assignments for each trust level is Tr = 0, Un = 1, Ta = 3
   * (under bitwise-or).
   */
  public static final int TRUSTED = 0;
  public static final int UNKNOWN = 1;
  public static final int TAINTED = 3;

  // Encoding Types...
  public static final int XPATH_TYPE = 4;
  public static final int CSS_TYPE = 8;
  public static final int DN_TYPE = 16;
  public static final int HTML_TYPE = 32;
  public static final int JAVASCRIPT_TYPE = 64;
  public static final int LDAP_TYPE = 128;
  public static final int OS_TYPE = 256;
  public static final int SQL_TYPE = 512;
  public static final int URL_TYPE = 1024;
  public static final int VBSCRIPT_TYPE = 2048;
  public static final int XML_TYPE = 4096;

  // Input Types...
  public static final int SHELL = 8192;
  public static final int DATABASE = 16384;
  public static final int PROPERTY = 32768;
  public static final int ENV_VAR = 65536;
  public static final int FILE = 1 << 17;
  public static final int INIT_FILE = 1 << 18;
  public static final int PROGRAM_ARG = 1 << 19;
  public static final int SOCKET = 1 << 20;
  public static final int URL = 1 << 21;
  public static final int CONSOLE = 1 << 22;
  public static final int JAR = 1 << 23;
  public static final int GUI = 1 << 24;
  public static final int PROCESS = 1 << 25;
  public static final int EQUALS = 1 << 26;

  // Primitive Type Checks...
  public static final int OVERFLOW = 1 << 27;
  public static final int UNDERFLOW = 1 << 28;
  public static final int BOUND_CHECK = 1 << 29;
  public static final int INFINITY = 1 << 30;
  public static final int BITWISE_EXPR = 1 << 31;

  /**
   * Whether or not to consider unknown trusted. Note that this only applies to the is_trusted() and
   * is_untrusted() routines.
   */
  public static boolean unknown_is_trusted = false;

  /*
   * Masks for each region inside the taint value.
   */
  public static final int TRUST_MASK = TAINTED;
  public static final int ENCODING_MASK = ((1 << NUM_OF_ENCODINGS) - 1) << 2;
  public static final int INPUTTYPE_MASK = ((1 << NUM_OF_INPUTTYPES) - 1) << (2 + NUM_OF_ENCODINGS);
  public static final int NONPRIM_MASK = TAINTED | ENCODING_MASK | INPUTTYPE_MASK;
  public static final int OVERFLOW_MASK = OVERFLOW | UNDERFLOW;

  /**
   * Unsets a taint bit or set of taint bits from the supplied taint value.
   */
  public static final int unset(int taint, int mask) {
    return taint & ~mask;
  }

  /**
   * @return true if taint has mask bits set, otherwise false.
   */
  public static final boolean isSet(int taint, int mask) {
    return (taint & mask) == mask;
  }

  /**
   * 
   * Converts a taint array to a nice printable string. The first row of characters represents the
   * input type and the second row of characters represents the taint bits.<br/>
   * 
   * <h3><u>taint row:</u></h3>
   * <table>
   *    <tr><td>unknown</td><td>?</td>
   *    <tr><td>trusted</td><td>.</td>
   *    <tr><td>tainted</td><td>_</td>
   * </table>
   * 
   * <h3><u>input row:</u></h3>
   * <table>
   *    <tr><td>unknown</td><td>?</td>
   *    <tr><td>equals (i.e. trusted string comparison)</td><td>=</td>
   *    <tr><td>command line argument</td><td>a</td>
   *    <tr><td>database</td><td>d</td>
   *    <tr><td>environment variable</td><td>e</td>
   *    <tr><td>file</td><td>f</td>
   *    <tr><td>GUI</td><td>g</td></tr>
   *    <tr><td>server initialization file</td><td>i</td>
   *    <tr><td>JAR</td><td>j</td></tr>
   *    <tr><td>console (i.e. stdin)</td><td>k</td></tr>
   *    <tr><td>property</td><td>p</td></tr>
   *    <tr><td>socket</td><td>s</td></tr>
   *    <tr><td>URL</td><td>u</td></tr>
   *    <tr><td>process</td><td>x</td></tr>
   *    <tr><td>shell (don't think this is used anymore)</td><td>z</td>
   * </table></br>
   *
   * @param taint int[]
   * @param offset int the start index.
   * @param length int the number of taint values to copy.
   * @return String representation of {@code taint}.
   */
  public static final String toString(int[] taint, int offset, int length) {
    StringBuilder taintPart = new StringBuilder();
    StringBuilder inputPart = new StringBuilder();
    for (int i = offset; i < length; i++) {
      if ((taint[i] & TaintValues.TAINTED) == TaintValues.TAINTED)
        taintPart.append('_');
      else if ((taint[i] & TaintValues.UNKNOWN) == TaintValues.UNKNOWN)
        taintPart.append('?');
      else
        taintPart.append('.');

      char c = '?';
      if (isSet(taint[i], TaintValues.SHELL))
        c = 'z';
      else if (isSet(taint[i], TaintValues.DATABASE))
        c = 'd';
      else if (isSet(taint[i], TaintValues.PROPERTY))
        c = 'p';
      else if (isSet(taint[i], TaintValues.ENV_VAR))
        c = 'e';
      else if (isSet(taint[i], TaintValues.FILE))
        c = 'f';
      else if (isSet(taint[i], TaintValues.INIT_FILE))
        c = 'i';
      else if (isSet(taint[i], TaintValues.PROGRAM_ARG))
        c = 'a';
      else if (isSet(taint[i], TaintValues.SOCKET))
        c = 's';
      else if (isSet(taint[i], TaintValues.URL))
        c = 'u';
      else if (isSet(taint[i], TaintValues.CONSOLE))
        c = 'k';
      else if (isSet(taint[i], TaintValues.JAR))
        c = 'j';
      else if (isSet(taint[i], TaintValues.GUI))
        c = 'g';
      else if (isSet(taint[i], TaintValues.PROCESS))
        c = 'x';
      else if (isSet(taint[i], TaintValues.EQUALS))
        c = '=';

      inputPart.append(c);
    }
    return inputPart.toString() + "\n" + taintPart.toString();
  }
}
