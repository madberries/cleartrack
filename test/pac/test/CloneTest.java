package pac.test;

import org.junit.Assert;
import org.junit.Test;

import pac.wrap.CharArrayTaint;

public class CloneTest implements Cloneable {

    private static char[] create() {
        char[] b = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j' };
        CharArrayTaint.taint(b);
        return b;
    }

    @Test
    public void cloneTest() throws CloneNotSupportedException {
        char[] b = new char[30];
        CharArrayTaint.taint(b, 5, 14);
        CharArrayTaint.taint(b, 20, 25);
        char[] b2 = b.clone();
        Assert.assertTrue("Cloned object should not be the same object.", b != b2);

        String msg = "The original object and cloned object do not contain the same metadata.";
        Assert.assertTrue(msg, CharArrayTaint.hasEqualTaint(b, b2));

        char[] b3 = (char[]) create().clone();
        Assert.assertTrue("Cloned object is not tainted", CharArrayTaint.isAllTainted(b3, 0, b3.length - 1));
    }

}
