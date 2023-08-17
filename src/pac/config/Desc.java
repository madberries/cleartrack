package pac.config;

import pac.org.objectweb.asm.Opcodes;

/**
 * This class exists to "remember" each api description as it is read in from the config file. Each
 * api description is stored in an instance of Desc and added to a global list.
 * 
 * @author rjk
 */
public class Desc {
  String command;
  Condition descCond;
  Chk afterChk;
  Condition afterChkCond;
  String[] paramTypes;
  Condition[] paramConds;
  Chk[] paramChks;
  int[] paramCwes;

  /**
   * Constructs a new Desc object.
   * 
   * @param command is the java dot notation name being processed. ex. "java.io.File.createTempFile".
   * @param paramTypes The type of each formal parameter in the api description is in this list.
   * @param paramChkNames The optional name found to the right of the paramType in the api
   *        description. The name, if given, is the name of a check, defined elsewhere in the config
   *        file. At run time the check of this name will be used to check the parameter in this
   *        corresponding position.
   */
  private Desc(final String command, final String descCond, final String[] paramTypes,
      final Chk[] paramChks, final Condition[] paramConds, final int[] paramCwes,
      final String afterChk, AbstractConfig config) {
    int idx = command.indexOf('.');
    if (idx > 0 && idx != command.lastIndexOf('.'))
      this.command = toASM(command);
    else
      this.command = command;
    if (descCond != null)
      this.descCond = new Condition(descCond);
    this.paramTypes = paramTypes;
    this.paramChks = paramChks;
    this.paramConds = paramConds;
    this.paramCwes = paramCwes;
    if (afterChk != null) {
      int start = afterChk.indexOf('{') + 1;
      if (start > 0) {
        int end = afterChk.indexOf('}', start);
        afterChkCond = new Condition(afterChk.substring(start, end));
        this.afterChk = config.getCheck(afterChk.substring(end + 1).trim());
      } else {
        this.afterChk = config.getCheck(afterChk);
      }
    }
  }

  public Chk[] getBeforeChks() {
    return paramChks;
  }

  public Chk getAfterChk() {
    return afterChk;
  }

  public String getCommand() {
    return command;
  }

  public Condition getCondition() {
    return descCond;
  }

  public Condition getAfterCondition() {
    return afterChkCond;
  }

  public Condition[] getBeforeConditions() {
    return paramConds;
  }

  /**
   * Called when reading config file and the line starts with the "desc" token.
   * 
   * @param line Is a line from config file. line has been trimmed. Looks like "desc
   *        Runtime.exec(String, int[]check_name)" White space may not be right. This method parses
   *        out "Runtime.exec" and the check_name that may or may not exist for each parameter.
   * @return A filled Desc. Desc.command is "Runtime.exe" Desc.paramChkNames is a Vector<String>
   *         Desc.paramChkNames[n] is null or a check_name according as whether the config file
   *         specified a check_name.
   */
  public static Desc createDesc(String line, AbstractConfig config) {
    boolean hasBeforeChk = false;

    // In "line" change white space around the brackets to make it be easier to parse
    // i.e. change "desc Runtime.exec(float a, String [ ]b int[ ] b)"
    //   to "desc Runtime.exec(float a, String[] b int[] b)"
    // This makes it easier to parse aout a, b, and c
    // i.e. eliminate all white space left of '['.
    // Eliminate all white space between '[' and ']'
    // Ensure exactly one white space is to right of ']'.
    final String line_2 = line.replaceAll(" *\\[ *\\] *", "[] ");

    String descCond = null;
    int startClass = 0;
    if (line_2.charAt(0) == '{') {
      startClass = line_2.indexOf('}');
      descCond = line_2.substring(1, startClass++);
    }

    int start = line_2.indexOf("(");
    // Get "java.lang.Runtime.exec".
    final StringBuilder cmd = new StringBuilder(line_2.substring(startClass, start).trim());
    start++; // Start is one past "(".

    // Point "end" either to ',' or ')'.
    cmd.append("(");

    int end = line_2.indexOf(')');
    String[] params = line_2.substring(start, end).split("\\s*,\\s*");

    String[] paramTypes = new String[params.length];
    Chk[] paramChks = new Chk[params.length];
    Condition[] paramConds = new Condition[params.length];
    int[] paramCwes = new int[params.length];

    for (int i = 0;;) {
      final String param = params[i].trim();
      final String[] tokens = param.split("\\s+");
      String type = tokens[0];

      // Look for conditional:
      int idx = type.indexOf('{');
      if (idx > 0) {
        String conditional = type.substring(idx + 1, type.indexOf('}', idx));
        type = type.substring(0, idx);
        paramConds[i] = new Condition(conditional);
      }

      paramTypes[i] = type;

      // Look for cwe numbers:
      String name = (tokens.length > 1 ? tokens[1] : null);
      if (name != null) {
        idx = name.indexOf('{');
        if (idx > 0) {
          paramCwes[i] =
              Integer.parseInt(name.substring(idx + 1, name.length() - 1).split("\\-")[1]);
          name = name.substring(0, idx);
        }
      }
      paramChks[i] = config.getCheck(name);

      hasBeforeChk |= name != null;
      cmd.append(type);
      if (++i >= params.length)
        break;
      cmd.append(", ");
    }
    cmd.append(")");
    String afterChk = null;
    if (++end < line_2.length())
      afterChk = line_2.substring(end).trim();

    if (!hasBeforeChk)
      paramChks = null;

    final Desc desc = new Desc(cmd.toString(), descCond, paramTypes, paramChks, paramConds,
        paramCwes, afterChk, config);

    return desc;
  }

  private static String toASM(String command) {
    int startArgs = command.indexOf('(');
    int endArgs = command.indexOf(')');
    int endClass = command.lastIndexOf('.', startArgs);
    StringBuilder buf = new StringBuilder(command.substring(0, endClass).replaceAll("\\.", "/"));
    buf.append('.');
    buf.append(command.substring(endClass + 1, startArgs));
    buf.append('(');
    String[] args = command.substring(startArgs + 1, endArgs).split("\\s*,\\s*");
    for (String arg : args) {
      int arrIdx = arg.indexOf('[');
      String type = arg;
      if (arrIdx > 0) {
        for (int i = arrIdx; i < arg.length(); i += 2) {
          buf.append('[');
        }
        type = arg.substring(0, arrIdx);
      }

      if (type.equals("int"))
        buf.append('I');
      else if (type.equals("char"))
        buf.append('C');
      else if (type.equals("byte"))
        buf.append('B');
      else if (type.equals("short"))
        buf.append('S');
      else if (type.equals("float"))
        buf.append('F');
      else if (type.equals("long"))
        buf.append('J');
      else if (type.equals("double"))
        buf.append('D');
      else if (type.equals("boolean"))
        buf.append('Z');
      else
        buf.append("L" + type.replaceAll("\\.", "/") + ";");
    }
    buf.append(')');
    return buf.toString();
  }

  public static class Condition {
    private String string;
    private String method;
    private int op;

    public Condition(String cond) {
      int idx = cond.indexOf('=');
      if (idx == 0) {
        string = cond.substring(idx + 1).trim();
        op = Opcodes.IFEQ;
      } else if (idx > 0) {
        string = cond.substring(idx + 1).trim();
        if (cond.charAt(idx - 1) == '!') {
          op = Opcodes.IFNE;
          method = cond.substring(0, idx - 1).trim();
        } else {
          op = Opcodes.IFEQ;
          method = cond.substring(0, idx).trim();
        }
      } else {
        // Method refers to a static method in ConfigFile that takes no parameters and returns a
        // boolean.
        method = cond;
        op = Opcodes.IFEQ;
      }
    }

    public String getMethod() {
      return method;
    }

    public String getValue() {
      return string;
    }

    public int getOp() {
      return op;
    }
  }
}
