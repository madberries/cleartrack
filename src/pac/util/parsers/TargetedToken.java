package pac.util.parsers;

import pac.org.antlr.runtime.CharStream;
import pac.org.antlr.runtime.CommonToken;

public class TargetedToken extends CommonToken {
  private static final long serialVersionUID = 3238640271807018809L;

  private int targets;

  public TargetedToken(CharStream input, int type, int channel, int start, int stop) {
    super(input, type, channel, start, stop);
  }

  public int getTargets() {
    return targets;
  }

  public void setTargets(int targets) {
    this.targets = targets;
  }
}
