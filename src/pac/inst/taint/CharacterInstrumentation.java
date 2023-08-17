package pac.inst.taint;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.inst.InvocationType;
import pac.util.Ret;

@InstrumentationClass("java/lang/CleartrackCharacter")
public final class CharacterInstrumentation {

  @InstrumentationMethod(invocationType = InvocationType.STATIC, name = "valueOf",
      descriptor = "(CILpac/util/Ret;)Ljava/lang/Character;")
  public static final CleartrackCharacter valueOf(char c, int c_t, Ret ret) {
    return new CleartrackCharacter(c, c_t, ret);
  }

}
