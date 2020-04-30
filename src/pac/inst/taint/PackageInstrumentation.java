package pac.inst.taint;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.util.TaintUtils;

@InstrumentationClass("java/lang/Package")
public final class PackageInstrumentation {

    @InstrumentationMethod
    public static final String getName(Package p) {
        String str = p.getName();
        TaintUtils.trust(str);
        return str;
    }

}
