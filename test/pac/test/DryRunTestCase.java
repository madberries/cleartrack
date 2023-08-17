package pac.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.junit.Assert;
import org.junit.Test;

import pac.util.TaintUtils;
import pac.wrap.CharArrayTaint;

public class DryRunTestCase {

  @Test
  public void testCharacterTaint() {
    String arg = "1234567890";
    TaintUtils.taint(arg);
    main(new String[] {arg});
  }

  public static void main(String[] args) {
    // Help message.
    if (args.length >= 1
        && (args[0].equals("-h") || args[0].equals("--h") || args[0].equals("help"))) {
      System.out.println("Usage: java -jar ISBNChecker.jar [isbn_number]");
      System.out.println("  [isbn_number] may be any valid ISBN-10 or ISBN-13 number");
      System.exit(1);
    } else if (args.length == 0) {
      try {
        BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
        String isbn = bufferRead.readLine(); // CLEARTRACK:STDIN_terminal

        System.out.println(ISBNChecker.isValidISBN(isbn));
      } catch (IOException e) {
        e.printStackTrace();
      }

    } else if (args.length != 1) {
      System.out.println("Usage: java -jar ISBNChecker.jar [isbn_number]");
      System.out.println("  [isbn_number] may be any valid ISBN-10 or ISBN-13 number");
      System.exit(1);
    } else {
      System.out.println(ISBNChecker.isValidISBN(args[0]));
    }
  }

  static class ISBNChecker {
    // Developer felt there was no reason to have more than 25 characters.
    static byte stringBytes[] = new byte[25];

    public static void passMeValue(char input, int whereAmI) {
      // CLEARTRACK:signed_int // CLEARTRACK:CROSSOVER_POINT if crash = true
      byte overflowMe = (byte) (104 + whereAmI); // CLEARTRACK:TRIGGER_POINT
      if (overflowMe < 0) {
        System.out.println("Overflow occurred");
        System.exit(0);
      }
      stringBytes[whereAmI] = (byte) input;
    }

    /**
     * This method checks the input String and returns whether or not it is a valid ISBN-10 or
     * ISBN-13 number.
     *
     * Reference and formulas from:
     *  http://en.wikipedia.org/wiki/International_Standard_Book_Number#ISBN-10
     *  http://en.wikipedia.org/wiki/International_Standard_Book_Number#ISBN-13
     * 
     * Example ISBN-13: 978-0-306-40615-7
     * Example ISBN-10: 0-306-40615-2
     *
     * @param isbn the input ISBN number to check, as a String.
     * @return whether the input String is a valid ISBN number.
     */
    public static boolean isValidISBN(String isbn) {

      char temp[] = isbn.toCharArray();
      int u = 1;
      for (int j = (u + 50) * 2 / 100 - 1; j < temp.length; j++) { 
        // CLEARTRACK:loop_complexity_initialization
        passMeValue(temp[j], j); // CLEARTRACK:INTERACTION_POINT //CLEARTRACK:pass_by_value
      }

      // Assert that the character region we copied from is the same
      // as where we copied to.
      Assert.assertTrue("Character taint was not preserved correctly.",
          CharArrayTaint.hasEqualTaint(temp, stringBytes));

      // Accept and remove spaces and dashes from input.
      isbn = isbn.replaceAll("[ -]", "");

      if (isbn.length() == 13) {
        // Validate ISBN-13 string.
        // ISBN-13 number must be all digits.
        try {
          Long.parseLong(isbn);
        } catch (NumberFormatException e) {
          return false;
        }

        // Convert String to ints for calculation of check digit.
        int[] isbnInts = new int[13];
        for (int i = 0; i < isbn.length(); i++) {
          isbnInts[i] = Character.getNumericValue(isbn.charAt(i));
        }

        // Validate check digit.
        if (isbnInts[12] == (10 - ((isbnInts[0] + 3 * isbnInts[1] + isbnInts[2] + 3 * isbnInts[3]
            + isbnInts[4] + 3 * isbnInts[5] + isbnInts[6] + 3 * isbnInts[7] + isbnInts[8]
            + 3 * isbnInts[9] + isbnInts[10] + 3 * isbnInts[11]) % 10)) % 10) {
          return true;
        } else {
          return false;
        }

      } else if (isbn.length() == 10) {
        // Validate ISBN-10 string.
        // ISBN-10 number must be 9 digits followed by either another digit or an 'X'.
        try {
          Integer.parseInt(isbn.substring(0, 9));
          if (!(Character.isDigit(isbn.charAt(9)) || isbn.charAt(9) == 'X'
              || isbn.charAt(9) == 'x'))
            throw new NumberFormatException(
                "Last character of ISBN-10 number is not a digit or an 'X'");
        } catch (NumberFormatException e) {
          return false;
        }

        // Convert String to ints for calculation of check digit.
        int[] isbnInts = new int[10];
        for (int i = 0; i < isbn.length(); i++) {
          if (i == 9 && (isbn.charAt(9) == 'X' || isbn.charAt(9) == 'x')) {
            isbnInts[i] = 10;
          } else {
            isbnInts[i] = Character.getNumericValue(isbn.charAt(i));
          }
        }

        // Validate check digit.
        if (isbnInts[9] == (isbnInts[0] + 2 * isbnInts[1] + 3 * isbnInts[2] + 4 * isbnInts[3]
            + 5 * isbnInts[4] + 6 * isbnInts[5] + 7 * isbnInts[6] + 8 * isbnInts[7]
            + 9 * isbnInts[8]) % 11) {
          return true;
        } else {
          return false;
        }
      } else {
        // ISBN is not 10 or 13 characters long.
        return false;
      }
    }
  }
}
