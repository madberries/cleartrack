package pac.inst.taint;

import java.io.File;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationLocation;
import pac.inst.InstrumentationMethod;
import pac.inst.InstrumentationType;
import pac.inst.InvocationType;
import pac.util.EnvInfo;
import pac.util.TaintUtils;
import pac.util.TaintValues;

@InstrumentationClass("sun/misc/VM")
public final class VMInstrumentation {

    @InstrumentationMethod(invocationType = InvocationType.STATIC,
            instrumentationLocation = InstrumentationLocation.COMPAT,
            instrumentationType = InstrumentationType.INSERT_AFTER,
            name = "saveAndRemoveProperties", descriptor = "(Ljava/util/Properties;)V")
    public static final void saveAndRemoveProperties() {
        // Setup the valid environment vars and properties
        EnvInfo.init_env_info(System.getProperties());

        TaintUtils.trust(File.pathSeparator, TaintValues.PROPERTY);
        TaintUtils.trust(File.separator, TaintValues.PROPERTY);
        File.pathSeparatorChar_t = TaintValues.TRUSTED | TaintValues.PROPERTY;
        File.separatorChar_t = TaintValues.TRUSTED | TaintValues.PROPERTY;
        //		if (File.fs instanceof UnixFileSystem) {
        //			UnixFileSystem fs = (UnixFileSystem) File.fs;
        //			fs.colon_t = TaintValues.TRUSTED | TaintValues.PROPERTY;
        //			fs.slash_t = TaintValues.TRUSTED | TaintValues.PROPERTY;
        //		}
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC,
            instrumentationLocation = InstrumentationLocation.COMPAT,
            instrumentationType = InstrumentationType.INSERT_AFTER,
            name = "saveAndRemoveProperties", descriptor = "(Ljava/util/Properties;Lpac/util/Ret;)V")
    public static final void saveAndRemoveProperties2() {
        // Setup the valid environment vars and properties
        EnvInfo.init_env_info(System.getProperties());

        TaintUtils.trust(File.pathSeparator, TaintValues.PROPERTY);
        TaintUtils.trust(File.separator, TaintValues.PROPERTY);
        File.pathSeparatorChar_t = TaintValues.TRUSTED | TaintValues.PROPERTY;
        File.separatorChar_t = TaintValues.TRUSTED | TaintValues.PROPERTY;
        //		if (File.fs instanceof UnixFileSystem) {
        //			UnixFileSystem fs = (UnixFileSystem) File.fs;
        //			fs.colon_t = TaintValues.TRUSTED | TaintValues.PROPERTY;
        //			fs.slash_t = TaintValues.TRUSTED | TaintValues.PROPERTY;
        //		}
    }

}
