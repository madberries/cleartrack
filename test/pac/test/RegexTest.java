package pac.test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import pac.util.TaintUtils;

/**
 * The purpose of this unit test is to confirm that trust data is maintained across regex
 * operations.  It is not a test of the instrumentation of a specific class as we may
 * need to use various techniques in order to track trust through regex operations.
 * @author ppiselli
 */
public class RegexTest {

    @Test
    public void testPatternMatcher() {
        Pattern pattern = Pattern.compile(".*(a|b)*.*");
        String inputString = TaintUtils.trust("match against this fabulous string");
        inputString = TaintUtils.taint(inputString, 19, 26); // taint "fabulous"
        Matcher matcher = pattern.matcher(inputString);
        Assert.assertTrue("did not match string", matcher.matches());
    }

    @Test
    public void testPatternSplit() {
        // PART 1 : split(CharSequence)
        Pattern pattern = Pattern.compile("c|d");
        String testString = TaintUtils.trust("abracadabra");
        testString = TaintUtils.taint(testString, 3, 8); // taint "acadab"
        String[] outputStrings = pattern.split(testString);

        // make sure the split did what we expected it to do
        Assert.assertEquals(3, outputStrings.length);
        Assert.assertEquals("abra", outputStrings[0]);
        Assert.assertEquals("a", outputStrings[1]);
        Assert.assertEquals("abra", outputStrings[2]);

        // make sure the taint came over
        Assert.assertTrue("trusted area not trusted", TaintUtils.isTrusted(outputStrings[0], 0, 2));
        Assert.assertTrue("tainted area not tainted", TaintUtils.isTainted(outputStrings[0], 3, 3));
        Assert.assertTrue("tainted area not tainted", TaintUtils.isTainted(outputStrings[1], 0, 0));
        Assert.assertTrue("tainted area not tainted", TaintUtils.isTainted(outputStrings[2], 0, 1));
        Assert.assertTrue("trusted area not trusted", TaintUtils.isTrusted(outputStrings[2], 2, 3));

        // PART 2 : split(CharSequence, int)
    }

    @Test
    public void testMatcherAppend() {
        Pattern p = Pattern.compile("cat");
        String input = TaintUtils.trust("one cat two cats in the yard");

        // 0         1         2       
        // 0123456789012345678901234567
        // one cat two cats in the yard

        input = TaintUtils.taint(input, 6, 13); // taint "t two ca"
        input = TaintUtils.taint(input, 20, 23); // taint "the "

        Matcher m = p.matcher(input);

        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, "llama");
        }
        m.appendTail(sb);
        String output = sb.toString();

        // test output
        Assert.assertEquals("one llama two llamas in the yard", output);

        // 0         1         2         3
        // 01234567890123456789012345678901
        // one llama two llamas in the yard

        // by taint spreading rules, taint should have spread to "llama two llamas"
        // and the region "the " should still be tainted
        Assert.assertTrue("trusted region not trusted", TaintUtils.isTrusted(output, 0, 3));
        Assert.assertTrue("tainted region not marked with taint", TaintUtils.isTainted(output, 4, 18));
        Assert.assertTrue("trusted region not trusted", TaintUtils.isTrusted(output, 19, 23));
        Assert.assertTrue("tainted region not marked with taint", TaintUtils.isTainted(output, 24, 27));
        Assert.assertTrue("trusted region not trusted", TaintUtils.isTrusted(output, 28, 31));
    }

    @Test
    public void testMatcherGroup() {
        // PART 1 : group()
        Pattern pattern = Pattern.compile(".*(gar\\w*).*");
        // 0123456789012345678
        // what's this garbage?
        String testString = TaintUtils.trust("what's this garbage?");
        testString = TaintUtils.taint(testString, 15, 17);
        Matcher m = pattern.matcher(testString);
        Assert.assertTrue("test string did not match pattern", m.matches());
        String output = m.group();
        Assert.assertEquals(testString, output);
        Assert.assertTrue("tainted region not marked with taint", TaintUtils.isTainted(output, 15, 17));

        // PART 2 : group(int)
        Pattern pattern2 = Pattern.compile(".*(gar\\w*).*(gar\\w*).*(gar\\w*).*");

        // 0         1         2         3         4
        // 012345678901234567890123456789012345678901
        // what's this garlicy garbage in the garage?

        String testString2 = TaintUtils.trust("what's this garlicy garbage in the garage?");
        testString2 = TaintUtils.taint(testString2, 10, 12); // taint "s g"
        testString2 = TaintUtils.taint(testString2, 26, 28); // taint "e i"

        Matcher m2 = pattern2.matcher(testString2);
        Assert.assertTrue("test string did not match pattern", m2.matches());
        String output1 = m2.group(1);
        String output2 = m2.group(2);
        String output3 = m2.group(3);
        Assert.assertEquals("garlicy", output1);
        Assert.assertEquals("garbage", output2);
        Assert.assertEquals("garage", output3);

        Assert.assertTrue("tainted region not marked with taint", TaintUtils.isTainted(output1, 0, 1));
        Assert.assertTrue("tainted region not marked with taint", TaintUtils.isTainted(output2, 5, 6));
        Assert.assertTrue("trusted region not marked as trusted",
                          TaintUtils.isTrusted(output3, 0, output3.length() - 1));
    }

    @Test
    public void testMatcherReplace() {
        // PART 1 : replaceAll(String)

        Pattern pattern = Pattern.compile("ab");
        String testString = TaintUtils.trust("abracadabra");
        testString = TaintUtils.taint(testString, 2, 5);
        Matcher m = pattern.matcher(testString);
        String output = m.replaceAll("iv");
        Assert.assertEquals(output, "ivracadivra");
        Assert.assertTrue("tainted region not marked tainted", TaintUtils.isTainted(output, 2, 5));

        // PART 2 : replaceFirst(String)
        String output2 = m.replaceFirst("op");
        Assert.assertEquals(output2, "opracadabra");
        Assert.assertTrue("tainted region not marked with taint", TaintUtils.isTainted(output2, 2, 5));
    }

    @Test
    public void testMatcherReset() {
        // PART 1 : reset()
        // PART 2 : reset(CharSequence)
    }

    @Test
    public void testMatcherStart() {
        // PART 1 : start()
        // PART 2 : start(int)
    }

}
