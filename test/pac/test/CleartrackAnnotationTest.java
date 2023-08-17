package pac.test;

import org.junit.Assert;
import org.junit.Test;

import pac.util.TaintUtils;
// import pac.annotation.CopyMetadata;

public class CleartrackAnnotationTest {

  //@Test
  //  @CopyMetadata(
  //          sourceIndex = 1, // 0th local is "this", so foo is 1st
  //          destIndex = 2, 
  //          fromOffset = 0, 
  //          toOffset = 0,
  //          length = 5, 
  //          toLength = 5,
  //          insertBefore = 6
  //          )
  public void annotationTest() {
    // Bytecode for this method should look like (apparently we need to include labels and line
    // numbers):

    // LABEL
    // LINE NUMBER
    // 0: LDC "abcde"
    // 1: ASTORE 1
    // LABEL
    // LINE NUMBER
    // 2: ALOAD 1
    // 3: INVOKESTATIC <taint>
    // LABEL
    // LINE NUMBER
    // 4: LDC "fghij"
    // 5: ASTORE 2
    // LABEL
    // LINE NUMBER
    // 6: LDC <debug string>
    // 7: ALOAD 2
    // 8: INVOKESTATIC <isTainted>
    // 9: INVOKESTATIC <assertTrue>
    // LABEL
    // LINE NUMBER
    // 10: RETURN

    // We want our annotation to insert instrumentation before the 7th instruction (index 6).

    String foo = "abcde";
    TaintUtils.taint(foo);
    String bar = "fghij";

    Assert.assertTrue("annotation-based tainting did not work", TaintUtils.isTainted(bar));
  }

  @Test
  public void dummyTest() {
    // This test is probably irrelevant now.
  }

}
