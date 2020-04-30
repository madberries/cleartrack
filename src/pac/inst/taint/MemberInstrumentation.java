package pac.inst.taint;

import java.lang.reflect.Member;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.util.TaintUtils;

@InstrumentationClass(value = "java/lang/reflect/Member", isInterface = true)
public class MemberInstrumentation {

    @InstrumentationMethod(canExtend = true)
    public static final String getName(Member member) {
        String str = member.getName();
        TaintUtils.trust(str);
        return str;
    }

}
