package pac.test;

import java.text.DecimalFormat;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import pac.config.BaseConfig;
import pac.util.TaintUtils;

public class ErrorHandlingTest {
  private static final Random rand = new Random();
  private static final int ITERS = 100;
  private static final int MAX_VAL;

  static {
    // Ensure that MAX_VAL is tainted.
    String num = "500";
    TaintUtils.taint(num);
    MAX_VAL = Integer.parseInt(num);
  }

  private static final String[] tensNames = {"", " ten", " twenty", " thirty", " forty", " fifty",
      " sixty", " seventy", " eighty", " ninety"};

  private static final String[] numNames = {"", " one", " two", " three", " four", " five", " six",
      " seven", " eight", " nine", " ten", " eleven", " twelve", " thirteen", " fourteen",
      " fifteen", " sixteen", " seventeen", " eighteen", " nineteen"};

  private static String convertLessThanOneThousand(int number) {
    String soFar;

    if (number % 100 < 20) {
      soFar = numNames[number % 100];
      number /= 100;
    } else {
      soFar = numNames[number % 10];
      number /= 10;

      soFar = tensNames[number % 10] + soFar;
      number /= 10;
    }
    if (number == 0)
      return soFar;
    return numNames[number] + " hundred" + soFar;
  }

  public static String toEnglish(long number) {
    // 0 to 999 999 999 999
    if (number == 0) {
      return "zero";
    }

    String snumber = Long.toString(number);

    // Pad with "0".
    String mask = "000000000000";
    DecimalFormat df = new DecimalFormat(mask);
    snumber = df.format(number);

    // XXXnnnnnnnnn
    int billions = Integer.parseInt(snumber.substring(0, 3));
    // nnnXXXnnnnnn
    int millions = Integer.parseInt(snumber.substring(3, 6));
    // nnnnnnXXXnnn
    int hundredThousands = Integer.parseInt(snumber.substring(6, 9));
    // nnnnnnnnnXXX
    int thousands = Integer.parseInt(snumber.substring(9, 12));

    String tradBillions;
    switch (billions) {
      case 0:
        tradBillions = "";
        break;
      case 1:
        tradBillions = convertLessThanOneThousand(billions) + " billion ";
        break;
      default:
        tradBillions = convertLessThanOneThousand(billions) + " billion ";
    }
    String result = tradBillions;

    String tradMillions;
    switch (millions) {
      case 0:
        tradMillions = "";
        break;
      case 1:
        tradMillions = convertLessThanOneThousand(millions) + " million ";
        break;
      default:
        tradMillions = convertLessThanOneThousand(millions) + " million ";
    }
    result = result + tradMillions;

    String tradHundredThousands;
    switch (hundredThousands) {
      case 0:
        tradHundredThousands = "";
        break;
      case 1:
        tradHundredThousands = "one thousand ";
        break;
      default:
        tradHundredThousands = convertLessThanOneThousand(hundredThousands) + " thousand ";
    }
    result = result + tradHundredThousands;

    String tradThousand;
    tradThousand = convertLessThanOneThousand(thousands);
    result = result + tradThousand;

    // remove extra spaces!
    return result.replaceAll("^\\s+", "").replaceAll("\\b\\s{2,}\\b", " ");
  }

  @Test
  public void parseIntTest() {
    // TODO: Add tests for other numeric types.
    for (int j = 0; j < ITERS; j++) {
      int expected = Math.abs(rand.nextInt());
      String number = "" + expected;
      String english = toEnglish(expected);
      int i = 0;

      TaintUtils.taint(english);
      try {
        i = Integer.parseInt(english);
      } catch (NumberFormatException e) {
        Assert.fail("We should not see a number format exception on a malformed tainted string: "
            + english);
      }
      Assert.assertTrue("Parsed integer is not zero: " + i, i == 0);

      TaintUtils.taint(number);
      try {
        i = Integer.parseInt(number);
      } catch (NumberFormatException e) {
        Assert.fail("We should not see a number format exception on a well-formed tainted string: "
            + number);
      }
      Assert.assertTrue("Parsed integer is not 5: " + i, i == expected);

      TaintUtils.trust(english);
      NumberFormatException throwEx = null;
      try {
        i = Integer.parseInt(english);
      } catch (NumberFormatException e) {
        throwEx = e;
      }
      Assert.assertNotNull(
          "We should see a number format exception on a malformed trusted string: " + english,
          throwEx);

      TaintUtils.trust(number);
      try {
        i = Integer.parseInt(number);
      } catch (NumberFormatException e) {
        Assert.fail("We should not see a number format exception on a well-formed tainted string: "
            + number);
      }
      Assert.assertTrue("Parsed integer is not 5: " + i, i == expected);
    }
  }

  @Test
  public void indexOutOfBoundsTest() {
    int[] intArray = new int[MAX_VAL - 1];
    for (int i = 0; i < intArray.length; i++) {
      intArray[i] = i + 1;
    }

    // TODO: Add tests for other array types.

    try {
      for (int i = 0; i < ITERS; i++) {
        int idx = rand.nextInt(2 * MAX_VAL);
        int val = intArray[idx];
        // Assert val == 0 iff index out of bounds.
        Assert.assertTrue("Out of bounds array load should have return zero",
            (val == 0) == (idx >= (MAX_VAL - 1) || idx < 0));
        intArray[idx] = MAX_VAL;
        // Assert intArray[idx] == MAX_VAL iff index within array bounds.
        Assert.assertTrue("In bounds array store should have stored: " + MAX_VAL,
            (intArray[idx] != MAX_VAL) == (idx >= (MAX_VAL - 1) || idx < 0));
        // Reset the store.
        intArray[idx] = idx + 1;
      }
    } catch (IndexOutOfBoundsException e) {
      Assert.fail("We should never get an index out of bounds exception: " + e);
    }
  }

  private int getOverAllocLength() {
    int len = rand.nextInt();
    int maxLen = BaseConfig.getInstance().getMaxAllocSize();
    while (len < maxLen)
      len = rand.nextInt();
    return len;
  }

  @Test
  public void memAllocTest() {
    int maxLen = BaseConfig.getInstance().getMaxAllocSize();
    int len = getOverAllocLength();
    boolean[] booleanArray = new boolean[len];
    Assert.assertTrue("int[] should not be bigger than the maximum allowed array length",
        booleanArray.length == maxLen);
    try {
      /*
       * FIXME: this will no longer work because we lost references to primitives.
       * Assert.assertEquals("We should have corrected the actual length value", len, maxLen);
       */
      for (int i = 0; i < booleanArray.length; i++) { // Len should now be maxLen.
        booleanArray[i] = true;
      }
    } catch (IndexOutOfBoundsException e) {
      Assert.fail("We should not go out of the bounds of the array");
    }

    len = getOverAllocLength();
    byte[] byteArray = new byte[len];
    Assert.assertTrue("int[] should not be bigger than the maximum allowed array length",
        byteArray.length == maxLen);
    try {
      // Assert.assertEquals("We should have corrected the actual length value", len, maxLen);
      for (int i = 0; i < byteArray.length; i++) { // Len should now be maxLen.
        byteArray[i] = 1;
      }
    } catch (IndexOutOfBoundsException e) {
      Assert.fail("We should not go out of the bounds of the array");
    }

    len = getOverAllocLength();
    short[] shortArray = new short[len];
    Assert.assertTrue("int[] should not be bigger than the maximum allowed array length",
        shortArray.length == maxLen);
    try {
      // Assert.assertEquals("We should have corrected the actual length value", len, maxLen);
      for (int i = 0; i < shortArray.length; i++) { // Len should now be maxLen.
        shortArray[i] = 1;
      }
    } catch (IndexOutOfBoundsException e) {
      Assert.fail("We should not go out of the bounds of the array");
    }

    len = getOverAllocLength();
    char[] charArray = new char[len];
    Assert.assertTrue("int[] should not be bigger than the maximum allowed array length",
        charArray.length == maxLen);
    try {
      // Assert.assertEquals("We should have corrected the actual length value", len, maxLen);
      for (int i = 0; i < charArray.length; i++) { // Len should now be maxLen.
        charArray[i] = 1;
      }
    } catch (IndexOutOfBoundsException e) {
      Assert.fail("We should not go out of the bounds of the array");
    }

    len = getOverAllocLength();
    int[] intArray = new int[len];
    Assert.assertTrue("int[] should not be bigger than the maximum allowed array length",
        intArray.length == maxLen);
    try {
      // Assert.assertEquals("We should have corrected the actual length value", len, maxLen);
      for (int i = 0; i < intArray.length; i++) { // Len should now be maxLen.
        intArray[i] = 1;
      }
    } catch (IndexOutOfBoundsException e) {
      Assert.fail("We should not go out of the bounds of the array");
    }

    len = getOverAllocLength();
    long[] longArray = new long[len];
    Assert.assertTrue("int[] should not be bigger than the maximum allowed array length",
        longArray.length == maxLen);
    try {
      // Assert.assertEquals("We should have corrected the actual length value", len, maxLen);
      for (int i = 0; i < longArray.length; i++) { // Len should now be maxLen.
        longArray[i] = 1;
      }
    } catch (IndexOutOfBoundsException e) {
      Assert.fail("We should not go out of the bounds of the array");
    }

    len = getOverAllocLength();
    float[] floatArray = new float[len];
    Assert.assertTrue("int[] should not be bigger than the maximum allowed array length",
        floatArray.length == maxLen);
    try {
      // Assert.assertEquals("We should have corrected the actual length value", len, maxLen);
      for (int i = 0; i < floatArray.length; i++) { // Len should now be maxLen.
        floatArray[i] = 1;
      }
    } catch (IndexOutOfBoundsException e) {
      Assert.fail("We should not go out of the bounds of the array");
    }

    len = getOverAllocLength();
    double[] doubleArray = new double[len];
    Assert.assertTrue("int[] should not be bigger than the maximum allowed array length",
        doubleArray.length == maxLen);
    try {
      // Assert.assertEquals("We should have corrected the actual length value", len, maxLen);
      for (int i = 0; i < doubleArray.length; i++) { // Len should now be maxLen.
        doubleArray[i] = 1;
      }
    } catch (IndexOutOfBoundsException e) {
      Assert.fail("We should not go out of the bounds of the array");
    }
  }
}
