package pac.util;

import java.util.Iterator;

/**
 * Convenience class for iterating over taint regions of a 
 * string (over the character array).
 * 
 * @author jeikenberry
 */
public class StringRegionIterator implements Iterator<StringRegionIterator.StringRegion> {
    private String data;
    private int offset;
    private int length, mask;

    /**
     * Constructs an iterator of StringRegions with no taint regions
     * masked out.
     * 
     * @param str
     */
    public StringRegionIterator(String str) {
        this(str, 0xffffffff);
    }

    /**
     * Constructs an iterator of StringRegions with the specified
     * mask applied to the taint.
     * 
     * @param str
     * @param mask
     */
    public StringRegionIterator(String str, int mask) {
        data = str;
        length = str.length();
        offset = 0;
        this.mask = mask;
    }

    @Override
    public boolean hasNext() {
        return offset < length;
    }

    @Override
    public StringRegion next() {
        int start = offset;
        char c = data.charAt(offset);
        int c_t = TaintUtils.taintAt(data, offset);
        int taint = c_t & mask;
        StringBuilder buf = TaintUtils.newStringBuilder();
        do {
            int len = buf.length();
            TaintUtils.append(buf, c);
            TaintUtils.markOr(buf, c_t, len, len);
            offset++;
            if (offset >= length)
                break;
            c = data.charAt(offset);
            c_t = TaintUtils.taintAt(data, offset);
        } while ((c_t & mask) == taint);

        return new StringRegion(TaintUtils.toString(buf), start, offset - 1, taint);
    }

    @Override
    public void remove() {
        throw new RuntimeException("StringRegionIterator does not support remove()");
    }

    /**
     * This class represents a contiguous sequence of tainted characters.
     * 
     * @author jeikenberry
     */
    public class StringRegion {
        private String reg;
        private int taint, start, end;

        private StringRegion(String reg, int start, int end, int taint) {
            this.reg = reg;
            this.start = start;
            this.end = end;
            this.taint = taint;
        }

        public String getString() {
            return reg;
        }

        public int getTaint() {
            return taint;
        }

        public boolean isTrusted() {
            return TaintUtils.isTrusted(taint);
        }

        public boolean isUntrusted() {
            return TaintUtils.isUntrusted(taint);
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public String toString() {
            return TaintUtils.createTaintDisplayLines(reg);
        }
    }
}
