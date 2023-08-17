package pac.test;

import org.junit.Assert;
import org.junit.Test;

import pac.util.TaintUtils;

/**
 * This class tests the automatic tagging of String constants with trust data.
 * 
 * @author ppiselli
 */
public class ConstantTrustTest {

  @Test
  public void constantTest() {
    String s = "12345";
    Assert.assertTrue("constant string was not trusted",
        TaintUtils.isTrusted(s, 0, s.length() - 1));
  }

}
