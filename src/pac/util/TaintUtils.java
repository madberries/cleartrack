package pac.util;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import pac.wrap.ByteArrayTaint;
import pac.wrap.CharArrayTaint;

public final class TaintUtils {
    private static final int IO_BUFFER_SIZE = 4 * 1024;

    private static String CWD = null;

    // Whether or not to consider unknown trusted. Note that this only applies
    // to the is_trusted() and is_untrusted() routines
    public static boolean trustUnknown = false;

    /**
    * @param s encoded. all chars are unmapped. Contains encodings
    * @param s_unencoded - No encodings. Containts both mapped/unmapped chars
    *                      Is the string sent to URLEncoder.encode(s_unencoded)
    *                      which returned String s.
    *                      This param is required only for chartaint
    */
    public static final String urlEncodedTaint(String s, String s_unencoded) {
        int[] taint = s.value_t.taint;
        char[] value = s.value_t.value;
        int last = value.length - 3;

        // ensure that each encoded value starting with '%'
        // has the same conservative joint taint in the resulting
        // encoded string.
        for (int i = 0; i <= last; i++) {
            if (value[i] == '%') {
                int joint = taint[i] | taint[i + 1] | taint[i + 2];
                taint[i++] = joint;
                taint[i++] = joint;
                taint[i] = joint;
            }
        }
        return s;
    }

    /**
    * @param s The decoded string returned from URLDecoder.decode(str-unmapped, ...)
    *          If running chartain contains only unmapped chars
    * @param str Is a string that was passed to URLDecorder.decode(str-unmapped, ...)
    *            If running chartaint contains mapped/unmapped chars
    * @return String s but each char in s tainted according to the corresponding char
    *         is str. Note that the char in s may correspond to a three character
    *         encoding String in str - in which case if any character in the three 
    *         char encoding string is tainted, then the corresponing char in s
    *         is tainted
    */
    public static final String urlDecodedTaint(String s, String str) {
        int[] taint = s.value_t.taint;
        char[] value = s.value_t.value;
        int[] encodedTaint = str.value_t.taint;
        char[] encodedValue = str.value_t.value;
        int last = value.length;
        int k = 0;

        // ensure that the string was properly decoded, by going
        // through encoded values starting with '%' and finding the
        // joint taint.
        for (int i = 0; i < last; i++) {
            if (encodedValue[k] == '%') {
                taint[i] = encodedTaint[k++] | encodedTaint[k++] | encodedTaint[k++];
            } else {
                k++;
            }
        }
        return s;
    }

    /**
     * Copies from input stream to output stream while maintaining taint.
     * 
     * @param in
     * @param out
     * @param length
     * @return
     * @throws IOException
     */
    public static final void copyTaint(InputStream in, OutputStream out, Ret ret) throws IOException {
        try {
            long length = Long.MAX_VALUE;
            int len = (int) Math.min(length, IO_BUFFER_SIZE);
            ByteArrayTaint buffer = ByteArrayTaint.newArray(len, TaintValues.TRUSTED);
            while (length > 0) {
                len = in.read(buffer, 0, TaintValues.TRUSTED, len, TaintValues.TRUSTED, ret);
                if (len < 0) {
                    break;
                }
                if (out != null) {
                    out.write(buffer, 0, TaintValues.TRUSTED, len, TaintValues.TRUSTED, ret);
                }
                length -= len;
                len = (int) Math.min(length, IO_BUFFER_SIZE);
            }
        } finally {
            // TODO don't really know if i should be closing the input stream
            in.close();
            out.close();
        }
    }

    /**
     * Copies from reader to write while maintaining taint.
     * 
     * @param in
     * @param out
     * @param length
     * @return
     * @throws IOException
     */
    public static final void copyTaint(Reader in, Writer out, Ret ret) throws IOException {
        try {
            long length = Long.MAX_VALUE;
            int len = (int) Math.min(length, IO_BUFFER_SIZE);
            CharArrayTaint buffer = CharArrayTaint.newArray(len, TaintValues.TRUSTED);
            while (length > 0) {
                len = in.read(buffer, 0, TaintValues.TRUSTED, len, TaintValues.TRUSTED, ret);
                if (len < 0) {
                    break;
                }
                if (out != null) {
                    out.write(buffer, 0, TaintValues.TRUSTED, len, TaintValues.TRUSTED, ret);
                }
                length -= len;
                len = (int) Math.min(length, IO_BUFFER_SIZE);
            }
        } finally {
            // TODO don't really know if i should be closing the input stream
            in.close();
            out.close();
        }
    }

    public static final String copyTaint(String from, String to, int length) {
        if (from.value_t == null)
            from.value_t = CharArrayTaint.toTaintArray(from.value, TaintValues.UNKNOWN);
        CharArrayTaint fromArr = from.value_t;
        if (to.value_t == null)
            to.value_t = CharArrayTaint.toTaintArray(to.value, TaintValues.UNKNOWN);
        CharArrayTaint toArr = to.value_t;
        for (int i = 0; i < length; i++) {
            toArr.taint[i] = fromArr.taint[i];
        }
        return to;
    }

    // METHODS FOR DISPLAYING PRIMITIVE TAINT

    /**
     * If all of path is trusted, then trust canonicalPath.  Otherwise copy all of
     * the taint from path to canonicalPath up to the differing character index.
     * Then taint the remaining characters in canonicalPath.
     * 
     * @param path String of the original path
     * @param canonicalPath String of the canonical form of path
     */
    public static final String copyTaintPath(String path, String canonicalPath) {
        if (isTrusted(path, 0, path.length())) {
            trust(canonicalPath);
        } else {
            int toLen = canonicalPath.length();
            int len = Math.min(path.length(), toLen);
            int lastSepIdx = -1;
            int i = 0;
            for (; i < len; i++) {
                char c = path.charAt(i);
                if (c != canonicalPath.charAt(i))
                    break;
                else {
                    canonicalPath.value_t.taint[i] = path.value_t.taint[i];
                    if (c == File.separatorChar)
                        lastSepIdx = i;
                }
            }

            if (i == len)
                return canonicalPath;

            for (int x = lastSepIdx + 1; x < toLen; x++) {
                canonicalPath.value_t.taint[i] |= TaintValues.TAINTED;
            }
        }
        return canonicalPath;
    }

    /**
     * Trusts all (absolute) path prefixes that canonicalize to the CWD
     * 
     * @param path
     */
    public static final String trustPrefixCWD(String path) {
        if (!path.startsWith(File.separator))
            return path;
        if (CWD == null) {
            try {
                CWD = new File("").getCanonicalPath();
            } catch (IOException e) {
                CWD = new File("").getAbsolutePath();
            }
        }
        String canonPath;
        try {
            canonPath = new File(path).getCanonicalPath();
        } catch (IOException e) {
            canonPath = new File(path).getAbsolutePath();
        }
        if (path.equals(CWD)) {
            path = trust(path, TaintValues.EQUALS);
        } else {
            int idx = -1;
            // attempt to find the CWD...
            while ((idx = path.indexOf(File.separator, idx + 1)) >= 0) {
                String subpath = path.substring(0, idx + 1);
                try {
                    canonPath = new File(subpath).getCanonicalPath();
                } catch (IOException e) {
                    canonPath = new File(subpath).getAbsolutePath();
                }
                if (canonPath.equals(CWD)) {
                    // We found a canonicalized prefix that equals CWD, so 
                    // trust that part of the path...
                    path = trust(path, TaintValues.EQUALS, 0, idx);
                    return path;
                }
            }
        }
        return path;
    }

    public static final String createTaintDisplayLines(File f) {
        return createTaintDisplayLines(f.toString());
    }

    public static final String createTaintDisplayLines(String s) {
        return CharArrayTaint.createTaintDisplayLines(s.value_t);
    }

    public static final String createTaintDisplayLines(StringBuffer sb) {
        return CharArrayTaint.createTaintDisplayLines(sb.value_t);
    }

    public static final String createTaintDisplayLines(StringBuilder sb) {
        return CharArrayTaint.createTaintDisplayLines(sb.value_t);
    }

    /**
     * Returns ORed taint over all regions of taint in a given String and over a
     * specified bit-mask
     **/
    public static final int getTaintOr(String s, int mask) {
        CharArrayTaint chArr = s.value_t;
        if (chArr == null)
            return TaintValues.UNKNOWN;
        int[] taint = chArr.taint;
        int len = taint.length;
        int orTaint = 0;
        for (int i = 0; i < len; i++) {
            orTaint |= taint[i] & mask;
        }
        return orTaint;
    }

    public static final boolean hasEqualTaint(String s1, String s2) {
        return CharArrayTaint.hasEqualTaint(s1.value_t, s2.value_t, 0xffffffff);
    }

    public static final boolean hasEqualTaint(String s1, String s2, int mask) {
        return CharArrayTaint.hasEqualTaint(s1.value_t, s2.value_t, mask);
    }

    public static final boolean hasUnknown(String str) {
        CharArrayTaint arr = str.value_t;
        if (arr == null)
            return true;
        int[] taint = arr.taint;
        int len = taint.length;
        for (int i = 0; i < len; i++) {
            if ((taint[i] & TaintValues.TRUST_MASK) == TaintValues.UNKNOWN)
                return true;
        }
        return false;
    }

    public static final boolean isAllTainted(String s, int start, int end) {
        return CharArrayTaint.isAllTainted(s.value_t, start, end);
    }

    public static final boolean isAllTainted(StringBuffer s, int start, int end) {
        return CharArrayTaint.isAllTainted(s.value_t, start, end);
    }

    public static final boolean isAllTainted(StringBuilder s, int start, int end) {
        return CharArrayTaint.isAllTainted(s.value_t, start, end);
    }

    public static final boolean isTainted(FileDescriptor fd) {
        return (fd.fd_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED;
    }

    public static final boolean isTainted(FileInputStream fis) {
        try {
            return isTainted(fis.getFD());
        } catch (IOException e) {
            return true;
        }
    }

    public static final boolean isTainted(RandomAccessFile file) {
        try {
            return isTainted(file.getFD());
        } catch (IOException e) {
            return true;
        }
    }

    public static final boolean isTainted(String s) {
        return CharArrayTaint.isTainted(s.value_t);
    }

    public static final boolean isTainted(String s, int start, int end) {
        return CharArrayTaint.isTainted(s.value_t, start, end);
    }

    public static final boolean isTainted(StringBuffer s) {
        return CharArrayTaint.isTainted(s.value_t);
    }

    public static final boolean isTainted(StringBuffer s, int start, int end) {
        return CharArrayTaint.isTainted(s.value_t, start, end);
    }

    public static final boolean isTainted(StringBuilder s) {
        return CharArrayTaint.isTainted(s.value_t);
    }

    public static final boolean isTainted(StringBuilder s, int start, int end) {
        return CharArrayTaint.isTainted(s.value_t, start, end);
    }

    public static final boolean isTracked(String s) {
        if (s.value_t == null)
            return false;
        int[] taint = s.value_t.taint;
        int len = taint.length;
        for (int i = 0; i < len; i++) {
            if ((taint[i] & TaintValues.TRUST_MASK) != TaintValues.UNKNOWN)
                return true;
        }
        return false;
    }

    public static final boolean isTrusted(FileDescriptor fd) {
        return (fd.fd_t & TaintValues.TRUST_MASK) == TaintValues.TRUSTED;
    }

    /** returns true if taint indicates trust, false otherwise **/
    public static final boolean isTrusted(int taint) {
        if (trustUnknown)
            return ((taint & TaintValues.TRUST_MASK) != TaintValues.TAINTED);
        else
            // unknown is NOT trusted
            return ((taint & TaintValues.TRUST_MASK) == TaintValues.TRUSTED);
    }

    public static final boolean isTrusted(RandomAccessFile file) {
        try {
            return isTrusted(file.getFD());
        } catch (IOException e) {
            return true;
        }
    }

    public static final boolean isTrusted(String s) {
        return CharArrayTaint.isTrusted(s.value_t);
    }

    public static final boolean isTrusted(String s, int start, int end) {
        return CharArrayTaint.isTrusted(s.value_t, start, end);
    }

    public static final boolean isTrusted(StringBuffer s) {
        return CharArrayTaint.isTrusted(s.value_t);
    }

    public static final boolean isTrusted(StringBuffer s, int start, int end) {
        return CharArrayTaint.isTrusted(s.value_t, start, end);
    }

    public static final boolean isTrusted(StringBuilder s) {
        return CharArrayTaint.isTrusted(s.value_t);
    }

    public static final boolean isTrusted(StringBuilder s, int start, int end) {
        return CharArrayTaint.isTrusted(s.value_t, start, end);
    }

    /** return true if taint indicates untrust, false otherwise **/
    public static final boolean isUntrusted(int taint) {
        if (trustUnknown)
            return ((taint & TaintValues.TRUST_MASK) == TaintValues.TAINTED);
        else
            // unknown is NOT trusted
            return ((taint & TaintValues.TRUST_MASK) != TaintValues.TRUSTED);
    }

    public static final int taintAt(String s, int idx) {
        CharArrayTaint chArr = s.value_t;
        if (chArr == null)
            return TaintValues.UNKNOWN;
        int[] taint = chArr.taint;
        return taint[idx];
    }

    public static final String mark(String s, int taint, int start, int end) {
        if (s.value_t == null) {
            s.value_t = new CharArrayTaint(s.value, new int[s.value.length]);
            // TODO fill with unknown taint
        }
        Arrays.fill(s.value_t.taint, start, end + 1, taint);
        return s;
    }

    public static final void mark(StringBuilder s, int taint, int start, int end) {
        if (s.value_t == null) {
            s.value_t = new CharArrayTaint(s.value, new int[s.value.length]);
            // TODO fill with unknown taint
        }
        Arrays.fill(s.value_t.taint, start, end + 1, taint);
    }

    public static final String markOr(String s, int taint, int start, int end) {
        if (s.value_t == null) {
            s.value_t = new CharArrayTaint(s.value, new int[s.value.length]);
            taint |= TaintValues.UNKNOWN;
            // TODO fill with unknown taint
        }

        int[] taintArr = s.value_t.taint;
        for (int i = start; i <= end; i++) {
            taintArr[i] |= taint;
        }
        return s;
    }

    public static final void markOr(StringBuffer s, int taint, int start, int end) {
        if (s.value_t == null) {
            s.value_t = new CharArrayTaint(s.value, new int[s.value.length]);
            taint |= TaintValues.UNKNOWN;
            // TODO fill with unknown taint
        }

        int[] taintArr = s.value_t.taint;
        for (int i = start; i <= end; i++) {
            taintArr[i] |= taint;
        }
    }

    public static final void markOr(StringBuilder s, int taint, int start, int end) {
        if (s.value_t == null) {
            s.value_t = new CharArrayTaint(s.value, new int[s.value.length]);
            taint |= TaintValues.UNKNOWN;
            // TODO fill with unknown taint
        }

        int[] taintArr = s.value_t.taint;
        for (int i = start; i <= end; i++) {
            taintArr[i] |= taint;
        }
    }

    /** Set whether or not values with unknown taint should be trusted **/
    public static final void setTrustUnknown(boolean trustUnknown) {
        TaintUtils.trustUnknown = trustUnknown;
    }

    public static final void taint(OutputStream os) {
        os.ss_hasUniformTaint = true;
        os.ss_taint = TaintValues.TAINTED;
    }

    public static final String taint(String s) {
        return markOr(s, TaintValues.TAINTED, 0, s.length() - 1);
    }

    public static final String taint(String s, int inputType) {
        return markOr(s, TaintValues.TAINTED | inputType, 0, s.length() - 1);
    }

    public static final String taint(String s, int start, int end) {
        return markOr(s, TaintValues.TAINTED, start, end);
    }

    public static final void taint(StringBuffer sb) {
        markOr(sb, TaintValues.TAINTED, 0, sb.length() - 1);
    }

    public static final void taint(StringBuffer sb, int inputType) {
        markOr(sb, TaintValues.TAINTED | inputType, 0, sb.length() - 1);
    }

    public static final void taint(StringBuffer sb, int start, int end) {
        markOr(sb, TaintValues.TAINTED, start, end);
    }

    public static final void taint(StringBuilder sb) {
        markOr(sb, TaintValues.TAINTED, 0, sb.length() - 1);
    }

    public static final void taint(StringBuilder sb, int inputType) {
        markOr(sb, TaintValues.TAINTED | inputType, 0, sb.length() - 1);
    }

    public static final void taint(StringBuilder sb, int start, int end) {
        markOr(sb, TaintValues.TAINTED, start, end);
    }

    public static final void taint(Writer w) {
        w.ss_hasUniformTaint = true;
        w.ss_taint = TaintValues.TAINTED;
    }

    public static final String toString(char c, int taint) {
        return toString(c, taint, new Ret());
    }

    private static final String toString(char c, int taint, Ret ret) {
        return CleartrackCharacter.toString(c, taint, ret);
    }

    /** returns a string that describes the trust status of taint **/
    public static final String toTrustString(int taint) {
        taint &= TaintValues.TRUST_MASK;
        if (taint == TaintValues.TRUSTED)
            return "trusted";
        else if (taint == TaintValues.UNKNOWN)
            return "unknown";
        else if (taint == TaintValues.TAINTED)
            return "tainted";
        else
            return "confused";
    }

    public static final void trust(OutputStream os) {
        os.ss_hasUniformTaint = true;
        os.ss_taint = TaintValues.TRUSTED;
    }

    public static final String trust(String s) {
        if (s == null)
            return null;
        char[] chArr = s.value;
        s.value_t = new CharArrayTaint(chArr, new int[chArr.length]);
        return s;
    }

    public static final String trust(String s, int inputType) {
        if (s.value_t == null) {
            char[] chArr = s.value;
            s.value_t = new CharArrayTaint(chArr, new int[chArr.length]);
            if (inputType == 0)
                return s;
        }
        CharArrayTaint.trust(s.value_t, inputType);
        return s;
    }

    public static final String trust(String s, int inputType, int start, int end) {
        if (s.value_t == null) {
            s.value_t = CharArrayTaint.toTaintArray(s.value, TaintValues.TRUSTED);
            // TODO fill with unknown taint
        }
        CharArrayTaint.trust(s.value_t, inputType, start, end);
        return s;
    }

    public static final void trust(StringBuffer sb, int inputType) {
        if (sb.value_t == null) {
            sb.value_t = CharArrayTaint.toTaintArray(sb.value, TaintValues.TRUSTED | inputType);
        } else {
            CharArrayTaint.trust(sb.value_t, inputType);
        }
    }

    public static final void trust(StringBuffer sb, int inputType, int start, int end) {
        if (sb.value_t == null) {
            sb.value_t = CharArrayTaint.toTaintArray(sb.value, TaintValues.TRUSTED | inputType);
            // TODO fill with unknown taint
        }
        CharArrayTaint.trust(sb.value_t, inputType, start, end);
    }

    public static final void trust(StringBuilder sb, int inputType) {
        if (sb.value_t == null) {
            sb.value_t = CharArrayTaint.toTaintArray(sb.value, TaintValues.TRUSTED | inputType);
        } else {
            CharArrayTaint.trust(sb.value_t, inputType);
        }
    }

    public static final void trust(StringBuilder sb, int inputType, int start, int end) {
        if (sb.value_t == null) {
            sb.value_t = CharArrayTaint.toTaintArray(sb.value, TaintValues.TRUSTED | inputType);
            // TODO fill with unknown taint
        }
        CharArrayTaint.trust(sb.value_t, inputType, start, end);
    }

    public static final void trust(Writer w) {
        w.ss_hasUniformTaint = true;
        w.ss_taint = TaintValues.TRUSTED;
    }

    /**
     * Trusts a String constant.  Do not call this unless you know what
     * you're doing.  This should only be called by the instrumentation
     * since we make some assumptions that a constants taint cannot be
     * mutated (since a String itself is immutable).
     * 
     * @param s
     * @return s
     */
    public static final String trustConstant(String s) {
        /* TODO make sure that we can do safely do this optimization...
         * i don't believe that we can ever taint a string constant, since
         * strings themselves are immutable. there could be some instrumentation
         * code that does this though.
         */
        if (s.value_t == null) {
            char[] chArr = s.value;
            s.value_t = new CharArrayTaint(chArr, new int[chArr.length]);
        }
        return s;
    }

    /********************************************************************************
     * The following methods are never called, but only exist because the JUnit tests
     * make use of these methods.  The calls, however, will refer to the instrumented
     * call of this method.
     ********************************************************************************/

    public static final int taint(int i) {
        return i;
    }

    public static final long taint(long value) {
        return value;
    }

    public static final boolean isTainted(int i) {
        return true;
    }

    public static final boolean isTainted(float value) {
        return true;
    }

    public static final boolean isTainted(long value) {
        return true;
    }

    public static final boolean isTainted(double value) {
        return true;
    }

    //	public static final boolean isTrusted(int i) { return false; }  **already defined above
    public static final boolean isTrusted(boolean bool) {
        return false;
    }

    public static final boolean isTrusted(float value) {
        return false;
    }

    public static final boolean isTrusted(long value) {
        return false;
    }

    public static final boolean isTrusted(double value) {
        return false;
    }

    public static final boolean isUnknown(int b) {
        return true;
    }

    public static final boolean isUnderflowSet(int i) {
        return false;
    }

    public static final boolean isUnderflowSet(long i) {
        return false;
    }

    public static final boolean isOverflowSet(int i) {
        return false;
    }

    public static final boolean isOverflowSet(long i) {
        return false;
    }

    public static final boolean isInfinitySet(int i) {
        return false;
    }

    public static final boolean isInfinitySet(float i) {
        return false;
    }

    public static final boolean isInfinitySet(long i) {
        return false;
    }

    public static final boolean isInfinitySet(double d) {
        return false;
    }

    public static final boolean isBoundSet(int i) {
        return false;
    }

    public static final boolean isBoundSet(float i) {
        return false;
    }

    public static final boolean taintMatchesValue(int i) {
        return false;
    }

    public static final boolean hasEqualTaint(char c1, char c2) {
        return false;
    }

    public static final int random(Random rand) {
        return rand.nextInt();
    }

    public static final int taint(int i, int i_t, Ret ret) {
        ret.taint = TaintValues.TAINTED;
        return i;
    }

    public static final boolean isTainted(int i, int i_t, Ret ret) {
        ret.taint = TaintValues.TRUSTED;
        return (i_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED;
    }

    public static final boolean isTrusted(int i, int i_t, Ret ret) {
        ret.taint = TaintValues.TRUSTED;
        return (i_t & TaintValues.TRUST_MASK) == TaintValues.TRUSTED;
    }

    public static final int random(Random rand, Ret ret) {
        int val = rand.nextInt(TaintValues.TRUST_MASK);
        if (val == 2)
            val++;
        val |= 1 << (2 + rand.nextInt(TaintValues.NUM_OF_ENCODINGS));
        ret.taint = val;
        return val;
    }

    public static final boolean taintMatchesValue(int i, int i_t, Ret ret) {
        ret.taint = TaintValues.TRUSTED;
        return (i & TaintValues.NONPRIM_MASK) == (i_t & TaintValues.NONPRIM_MASK);
    }

    public static final boolean isUnderflowSet(int i, int i_t, Ret ret) {
        ret.taint = TaintValues.TRUSTED;
        return (i_t & TaintValues.UNDERFLOW) == TaintValues.UNDERFLOW;
    }

    public static final boolean isOverflowSet(int i, int i_t, Ret ret) {
        ret.taint = TaintValues.TRUSTED;
        return (i_t & TaintValues.OVERFLOW) == TaintValues.OVERFLOW;
    }

    public static final boolean isInfinitySet(int i, int i_t, Ret ret) {
        ret.taint = TaintValues.TRUSTED;
        return (i_t & TaintValues.INFINITY) == TaintValues.INFINITY;
    }

    public static final boolean isBoundSet(int i, int i_t, Ret ret) {
        ret.taint = TaintValues.TRUSTED;
        return (i_t & TaintValues.BOUND_CHECK) == TaintValues.BOUND_CHECK;
    }

    public static final boolean isUnknown(int i, int i_t, Ret ret) {
        ret.taint = TaintValues.TRUSTED;
        return (i_t & TaintValues.TRUST_MASK) == TaintValues.UNKNOWN;
    }

    public static final boolean hasEqualTaint(char c1, int c1_t, char c2, int c2_t, Ret ret) {
        ret.taint = TaintValues.TAINTED;
        return (c1_t & TaintValues.NONPRIM_MASK) == (c2_t & TaintValues.NONPRIM_MASK);
    }

    public static final boolean isTrusted(boolean bool, int bool_t, Ret ret) {
        ret.taint = TaintValues.TRUSTED;
        return (bool_t & TaintValues.TRUST_MASK) == TaintValues.TRUSTED;
    }

    public static final boolean isTrusted(float f, int f_t, Ret ret) {
        ret.taint = TaintValues.TRUSTED;
        return (f_t & TaintValues.TRUST_MASK) == TaintValues.TRUSTED;
    }

    public static final boolean isTainted(float f, int f_t, Ret ret) {
        ret.taint = TaintValues.TRUSTED;
        return (f_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED;
    }

    public static final boolean isBoundSet(float f, int f_t, Ret ret) {
        ret.taint = TaintValues.TRUSTED;
        return (f_t & TaintValues.BOUND_CHECK) == TaintValues.BOUND_CHECK;
    }

    public static final boolean isInfinitySet(float f, int f_t, Ret ret) {
        ret.taint = TaintValues.TRUSTED;
        return (f_t & TaintValues.INFINITY) == TaintValues.INFINITY;
    }

    public static final long taint(long value, int value_t, Ret ret) {
        ret.taint = TaintValues.TAINTED;
        return value;
    }

    public static final boolean isTrusted(long l, int l_t, Ret ret) {
        return (l_t & TaintValues.TRUST_MASK) == TaintValues.TRUSTED;
    }

    public static final boolean isTainted(long l, int l_t, Ret ret) {
        return (l_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED;
    }

    public static final boolean isUnderflowSet(long l, int l_t, Ret ret) {
        return (l_t & TaintValues.UNDERFLOW) == TaintValues.UNDERFLOW;
    }

    public static final boolean isOverflowSet(long l, int l_t, Ret ret) {
        return (l_t & TaintValues.OVERFLOW) == TaintValues.OVERFLOW;
    }

    public static final boolean isInfinitySet(long l, int l_t, Ret ret) {
        return (l_t & TaintValues.INFINITY) == TaintValues.INFINITY;
    }

    public static final boolean isTrusted(double d, int d_t, Ret ret) {
        ret.taint = TaintValues.TRUSTED;
        return (d_t & TaintValues.TRUST_MASK) == TaintValues.TRUSTED;
    }

    public static final boolean isTainted(double d, int d_t, Ret ret) {
        ret.taint = TaintValues.TRUSTED;
        return (d_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED;
    }

    public static final boolean isInfinitySet(double d, int d_t, Ret ret) {
        ret.taint = TaintValues.TRUSTED;
        return (d_t & TaintValues.INFINITY) == TaintValues.INFINITY;
    }

    // String Routines...

    public static final StringBuilder newStringBuilder(String s) {
        return new StringBuilder(s, new Ret());
    }

    public static final StringBuilder newStringBuilder() {
        return new StringBuilder(new Ret());
    }

    public static final StringBuilder delete(StringBuilder sb, int beginIdx, int endIdx) {
        return sb.delete(beginIdx, 0, endIdx, 0, new Ret());
    }

    public static final StringBuilder append(StringBuilder sb, char c) {
        return sb.append(c, 0, new Ret());
    }

    public static final StringBuilder append(StringBuilder sb, String str) {
        return sb.append(str, new Ret());
    }

    public static final StringBuilder insert(StringBuilder sb, int idx, char c) {
        return sb.insert(idx, 0, c, 0, new Ret());
    }

    public static final StringBuilder insert(StringBuilder sb, int idx, String str) {
        return sb.insert(idx, 0, str, new Ret());
    }

    public static final void setCharAt(StringBuilder sb, int idx, char c) {
        sb.setCharAt(idx, 0, c, 0, new Ret());
    }

    public static final StringBuilder replace(StringBuilder s, int beginIndex, int endIndex, String replaceStr) {
        return s.replace(beginIndex, 0, endIndex, 0, replaceStr, new Ret());
    }

    public static final String toString(StringBuilder sb) {
        return sb.toString(new Ret());
    }

    public static final String[] split(String s, String regex) {
        return s.split(regex, new Ret());
    }

    public static final String substring(String s, int beginIndex) {
        return s.substring(beginIndex, 0, new Ret());
    }

    public static final String substring(String s, int beginIndex, int endIndex) {
        return s.substring(beginIndex, 0, endIndex, 0, new Ret());
    }

    public static final String replace(String s, CharSequence target, CharSequence replacement) {
        return s.replace(target, replacement, new Ret());
    }

    public static final String replaceAll(String s, String regex, String replacement) {
        return s.replaceAll(regex, replacement, new Ret());
    }

    public static final String replaceFirst(String s, String regex, String replacement) {
        return s.replaceFirst(regex, replacement, new Ret());
    }

    public static final String trim(String s) {
        return s.trim(new Ret());
    }

    public static boolean charEquals(char ch_1, char ch_2) {
        return (ch_1 == ch_2);
    }

    public static boolean stringEquals(String str_1, String str_2) {
        return str_1.equals(str_2);
    }

    public static boolean charSetContains(Set<Character> set, char findMe) {
        return set.contains(findMe);
    }

    public static boolean characterIsWhitespace(char ch) {
        return Character.isWhitespace(ch);
    }

    public static boolean charMapContainsKey(Map<Character, Character> map, char key) {
        return map.containsKey(key);
    }

    /**
    * Call this method to retrieve a mapped/unmapped value from a Map
    * where the key can be mapped/unmapped
    */
    public static String stringMapGet(Map<String, String> map, String key) {
        return map.get(key);
    }

    public static int stringIndexOf(String str, String findMe, int start) {
        return str.indexOf(findMe, start);
    }

    public static int stringIndexOf(String str, char findMe, int start) {
        return str.indexOf(findMe, start);
    }

    public static boolean stringSetContains(Set<String> set, String key) {
        return set.contains(key);
    }

    public static String whiteSpaceRegExp() {
        return "\\s";
    }
}
