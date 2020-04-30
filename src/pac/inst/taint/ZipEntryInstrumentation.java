package pac.inst.taint;

import java.util.zip.ZipEntry;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.util.Ret;
import pac.util.TaintUtils;

@InstrumentationClass("java/util/zip/ZipEntry")
public final class ZipEntryInstrumentation {

    /*
     * TODO we need to revisit this... i'm not sure if we really want to
     * trust all paths inside of a zip/jar file. but this is necessary for
     * jmeter to work properly.
     */

    @InstrumentationMethod(canExtend = true)
    public static final String toString(ZipEntry zipEntry, Ret ret) {
        String str = zipEntry.toString(ret);
        TaintUtils.trust(str);
        return str;
    }

    @InstrumentationMethod(canExtend = true)
    public static final String getName(ZipEntry zipEntry, Ret ret) {
        String str = zipEntry.getName(ret);
        TaintUtils.trust(str);
        return str;
    }

}
