package pac.inst.taint;

import java.util.Map;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.util.TaintUtils;
import pac.util.TaintValues;

@InstrumentationClass(value = "java/util/Map", isInterface = true)
public final class MapInstrumentation {

  @InstrumentationMethod(canExtend = true)
  public static final boolean containsKey(Map<?, ?> map, Object key) {
    boolean result = map.containsKey(key);
    if (result && key instanceof String) {
      TaintUtils.trust((String) key, TaintValues.EQUALS);
    }
    return result;
  }

}
