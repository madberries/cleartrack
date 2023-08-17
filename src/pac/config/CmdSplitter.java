
package pac.config;

import java.awt.Point;
import java.util.Vector;

import pac.util.TaintUtils;

// Use this class to divide the String that is sent to a shell. The string sent to a shell
// is a command and its arguments. But the string may be composed of sub-commands each with
// it's arguments.
// ex: If processMe is "<commandA and args> ; <other stuff>
//   then this class divides processMe into
//   processMeA       "<commmandA and args>"
//   dividerAndSpace  " ; "
//   processMeB       "<other stuff>"
// Note: "<other stuff>" may another command and its arguments
//                    or it may again be two commands and arguments separated by ";"
//                    or it may be a shell with its associated command and arguments
// Note about quotes:
//   Assume processMe is "ls -l "filename;3""  - Note the two internal trusted quotes
//   Here the intent is that the name of the file is:  filename;3
//   Rule: A separator string enclosed in trusted quotes, becomes a normal character

// getA
// getDividerStr
// getB
//
// enclosedInQuotes

public class CmdSplitter {

  private final String processMe;

  /** String to left of trusted command separator. This String will not end with white space. */
  private String processMeA;

  /**
   * The divider string plus all contiguous white space to the left and all contiguous white space
   * to right.
   */
  private String dividerAndSpace;

  /** String to right of trusted command separator will not begin with white space. */
  private String processMeB;

  CmdSplitter(final String processMe) {
    this.processMe = processMe;
  }

  /**
   * @return The string that is to the left of the left-most trusted command separator string. The
   *         string to left (processMeA) will not end with contiguous white space - trusted or not
   *         The string to right (processMeB) will not start with contiguous white space.
   */
  String getA() {
    final String[] dividers = {";", "|", "|&", "&&", "||"}; // command separator strings
    int loBologne = Integer.MAX_VALUE;
    int winningIndex = -1;
    int startSearch = 0;

    // Get list of indexes of quote chars that enclose quoted substrings within processMe.
    // Each quoted substring is represented as a point:
    //  o point.x indexes the open quote
    //  o point.y indexes the closed quote
    final Vector<Point> doubleQuoteStrs = Utils.getUnescapedDoubleQuotes(processMe);
    final Vector<Point> singleQuoteStrs = Utils.getUnescapedSingleQuotes(processMe);

    while (startSearch < processMe.length() - 1) {
      // Loop looking for whichever found command separator string is in the left-most location.
      for (int i = 0; i < dividers.length; i++) {
        final String divider = dividers[i];
        int found = -1;
        for (int index = 0; (found == -1) && ((index =
            TaintUtils.stringIndexOf(processMe, divider, index + startSearch)) != -1); index++) {
          if (TaintUtils.isTrusted(processMe, index, index)) {
            found = index;
          }
        }

        if (found > -1 && found < loBologne) {
          loBologne = found;
          winningIndex = i;
        }
      }

      if (loBologne < Integer.MAX_VALUE) {
        // loBologne indexes the location of divider string.
        // Now test if loBologne is surrounded by either double quotes or single quotes.
        // If loBologne is surrounded by trusted quotes, advance startSearch to the right of the
        // quoted area.
        Point point = enclosedInQuotes(doubleQuoteStrs, loBologne);
        if (point == null) {
          point = enclosedInQuotes(singleQuoteStrs, loBologne);
        }

        if (point == null) {
          startSearch = processMe.length() + 1; // To get out of while loop.

        } else {
          startSearch = point.y;
          loBologne = Integer.MAX_VALUE;
        }
      } else {
        startSearch = processMe.length() + 1; // To get out of while loop.
      }
    }

    if (loBologne < Integer.MAX_VALUE) {
      // loBologne indexes the first char in command separator string that is not surrounded
      // surrounded by trusted quotes.
      int right = loBologne;

      //   loBologne is here ----            or here ---
      //                         |                      |
      //                         v                      v
      // This block        "ls -l;/bin/pwd"     "ls -l  ;/bin/pwd"
      // puts left here      ----^         or here ---^

      // Point left at one character to right of the last non-whitespace char that precedes
      // loBologne.
      int left = loBologne - 1;
      while (left > 0
          && (processMe.substring(left, left + 1).matches(TaintUtils.whiteSpaceRegExp()))) {
        left--;
      }
      if (left < loBologne
          && !processMe.substring(left, left + 1).matches(TaintUtils.whiteSpaceRegExp())) {
        left++;
      }

      // Put right on the first non-whitespace char to right of the command separator string
      // winningIndex tells which divider string that loBologne points to
      // First advance right past the divider chars.
      right += dividers[winningIndex].length(); // Is possible this puts "right" on non-white that
                                                // is adjacent to divider. ex. "ls -l;/bin/pwd

      // This block         "ls -l;/bin/pwd"     "ls -l;  /bin/pwd"
      // puts "right" here      ---^         or here   ---^
      while (right < processMe.length()
          && processMe.substring(right, right + 1).matches(TaintUtils.whiteSpaceRegExp())) {
        right++;
      }

      processMeA = TaintUtils.substring(processMe, 0, left);
      dividerAndSpace = TaintUtils.substring(processMe, left, right);
      processMeB = TaintUtils.substring(processMe, right);
    }

    return processMeA;
  }

  /**
   * @return the divider string plus and white space that was to left and right of the divider
   *         string.
   */
  String getDividerStr() {
    return dividerAndSpace;
  }

  /**
   * @return the string that is to the right of the left most trusted divider string. The returned
   *         string does not contain contiguous white space (trusted or not) to right of the trusted
   *         divider string. If preceding white space is not eliminated, then when caller does
   *         split("\\s+") on the returned string, "" is returned as the first token in the returned
   *         array.
   */
  String getB() {
    return processMeB;
  }

  private Point enclosedInQuotes(final Vector<Point> quotedStrs, final int loBologne) {
    Point enclosed = null;

    for (int i = 0; enclosed == null && i < quotedStrs.size(); i++) {
      final Point point = quotedStrs.elementAt(i);
      if (loBologne > point.x && loBologne < point.y) {
        enclosed = point;
      }
    }

    return enclosed;
  }

}
