package pac.test;

import org.junit.Assert;
import org.junit.Test;

import pac.util.TaintUtils;

public class CharSequenceInstrumentationTest {

  @Test
  public void subSequenceTest() {
    CharSequence cs = TaintUtils.trust("0123456789");
    TaintUtils.taint(cs.toString(), 4, 7); // End inclusive.
    CharSequence cs2 = cs.subSequence(2, 8); // End exclusive.
    Assert.assertFalse("untainted region marked as tainted",
        TaintUtils.isTainted(cs2.toString(), 0, 1));
    Assert.assertTrue("tainted region not marked as tainted",
        TaintUtils.isTainted(cs2.toString(), 2, 5));
    Assert.assertFalse("untainted region marked as tainted",
        TaintUtils.isTainted(cs2.toString(), 6, 7));
  }

  @Test
  public void toStringTest() {
    String s = TaintUtils.trust("0123456789");
    s = TaintUtils.taint(s, 4, 7); // End inclusive.
    CharSequence cs = new StringBuffer(s); // Use string buffer so toString is not just identity.
    String s2 = cs.toString();
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(s2, 0, 3));
    Assert.assertTrue("tainted region not marked as tainted", TaintUtils.isTainted(s2, 4, 7));
    Assert.assertFalse("untainted region marked as tainted", TaintUtils.isTainted(s2, 8, 9));
  }

}
