package pac.test;

import org.junit.Assert;
import org.junit.Test;

import pac.util.TaintUtils;

public class PostMethodInstrumentationTest {

  @Test
  public void fooTest() {
    FooClass fooObject = new FooClass();

    // The post-method instrumentation should insert a taint instruction after this method call:
    //   String s = (String) TaintValues.taintObject(fooObject.fooMethod());
    String s = fooObject.fooMethod();
    byte[] ba = s.getBytes();
    if (ba.length < 0) {
      Assert.fail("unreachable");
    }

    Assert.assertTrue("object not tainted", TaintUtils.isTainted(s));
  }

}
