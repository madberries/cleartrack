package pac.inst.taint;

import java.io.BufferedInputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import pac.config.BaseConfig;
import pac.config.Notify;
import pac.config.NotifyMsg;
import pac.config.RunChecks;
import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.inst.InvocationType;
import pac.util.EnvInfo;
import pac.util.Ret;
import pac.util.TaintUtils;
import pac.util.TaintValues;
import pac.wrap.TaintableArray;

@InstrumentationClass("java/lang/System")
public final class SystemInstrumentation {
    private static boolean loadedEnv = false;

    // STATIC METHODS
    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final void setIn(InputStream in, Ret ret) {
        System.setIn(in, ret);
        taintConsole(in);
    }

    public static final void taintConsole(InputStream in) {
        try {
            in.ss_hasUniformTaint = true;
            in.ss_taint = TaintValues.CONSOLE | TaintValues.TAINTED;
            if (in instanceof BufferedInputStream) {
                InputStream in2 = ((BufferedInputStream) in).in;
                in2.ss_hasUniformTaint = true;
                in2.ss_taint = TaintValues.CONSOLE | TaintValues.TAINTED;
                if (in2 instanceof FileInputStream) {
                    FileDescriptor fd = ((FileInputStream) in2).getFD();
                    fd.fd_t = TaintValues.CONSOLE | TaintValues.TAINTED;
                }
            }
        } catch (IOException e) {
            Notify.error("Unexpected exception tainting stdin: " + e);
        }
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final long currentTimeMillis(Ret ret) {
        long result = System.currentTimeMillis();
        ret.taint = TaintValues.TRUSTED;
        return result;
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final String getProperty(String key, Ret ret) {
        Notify.enter_check("System.getProperty", key);

        String propertyVal = null;
        if (!BaseConfig.getInstance().untrustedKeyCanAccessPropertyValue() && !TaintUtils.isTrusted(key)) {
            final NotifyMsg notifyMsg = new NotifyMsg("System.getProperty(String)", "System.getProperty(" + key + ")",
                    642);
            notifyMsg.addTaintOutput(key);
            notifyMsg.setAction(RunChecks.REMOVE_ACTION);
            propertyVal = "";
            notifyMsg.append("Key " + key + " is not trusted. Returning empty string as Property value.");
            Notify.notifyAndRespond(notifyMsg); // IOException

        } else {
            propertyVal = System.getProperty(key, ret);
        }

        Notify.log("Property Value:\n%s",
                   (propertyVal == null) ? "Null\n" : TaintUtils.createTaintDisplayLines(propertyVal));
        return propertyVal;
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final String getProperty(String key, String def, Ret ret) {
        Notify.enter_check("System.getProperty", key, def);

        String propertyVal = null;
        if (!BaseConfig.getInstance().untrustedKeyCanAccessPropertyValue() && !TaintUtils.isTrusted(key)) {
            final NotifyMsg notifyMsg = new NotifyMsg("System.getProperty(String,String)",
                    "System.getProperty(" + key + ", " + def + ")", 642);
            notifyMsg.addTaintOutput(key);
            notifyMsg.setAction(RunChecks.REMOVE_ACTION);

            boolean defIsTrusted = def == null || TaintUtils.isTrusted(def);
            propertyVal = (defIsTrusted ? def : "");

            notifyMsg.append("Key " + key + " is not trusted.\n");
            notifyMsg.append("Default is " + (defIsTrusted ? "" : "not") + " trusted.\n");
            notifyMsg.append("Returning Property value: " + (defIsTrusted ? def : "empty String."));

            Notify.notifyAndRespond(notifyMsg); // IOException

        } else {
            propertyVal = System.getProperty(key, def, ret);
        }

        Notify.log("Property Value:\n%s",
                   (propertyVal == null) ? "Null\n" : TaintUtils.createTaintDisplayLines(propertyVal));

        return propertyVal;
    }

    // Make a more thorough test for this method
    //
    // System.getenv() returns an unmodifiable Map.
    // Set trust level of each key and value entry to "untrusted"
    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final Map<String, String> getenv(Ret ret) {
        Notify.enter_check("System.getenv()");

        // GOLD
        Map<String, String> map = System.getenv(ret); // Returned map can not be
                                                      // altered
                                                      // Map<String,String> mapClone = new LinkedHashMap<String,String>();
        HashMap<String, String> goldMap = EnvInfo.get_env_map();

        // the environment cannot change, so we only need to do this once.
        synchronized (SystemInstrumentation.class) {
            if (!loadedEnv && map != null) {
                loadedEnv = true;
                if (BaseConfig.getInstance().taintEnvVars()) {
                    for (Entry<String, String> entry : map.entrySet()) {
                        String key = entry.getKey(); // Get system key
                        String envValue = map.get(key); // Get system value
                        String goldValue = goldMap.get(key); // Get gold
                        if (goldValue == null) {
                            TaintUtils.taint(envValue, TaintValues.ENV_VAR);
                            TaintUtils.taint(key);
                        } else if (goldValue == EnvInfo.ANY_VALUE) {
                            TaintUtils.trust(envValue, TaintValues.ENV_VAR);
                            TaintUtils.trust(key);
                        } else { // we have a valid gold value
                            if (envValue.equals(goldValue)) {
                                TaintUtils.trust(envValue, TaintValues.ENV_VAR);
                                TaintUtils.trust(key);
                            } else {
                                TaintUtils.taint(envValue, TaintValues.ENV_VAR);
                                TaintUtils.taint(key);
                                Notify.log("Not trusting std env var with odd value %s=%s\n", key, envValue);
                            }
                            TaintUtils.trust(key);
                        }
                    }
                } else { // environment variables are trusted
                    for (Entry<String, String> entry : map.entrySet()) {
                        TaintUtils.trust(entry.getKey());
                        TaintUtils.trust(entry.getValue(), TaintValues.ENV_VAR);
                    }
                }
            }
        }

        return (map);
    }

    /**
     * Set the taint on environment variables according to the config file If
     * environment variables are trusted, just trust the current value. If env
     * vars are not trusted and they match a good value, trust them. otherwise
     * taint them. Note that we never change a value here (ie, we don't repair
     */
    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final String getenv(String name, Ret ret) {
        return getenv(ret).get(name);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC)
    public static final void arraycopy(Object src, int srcPos, int srcPos_t, Object dest, int destPos, int destPos_t,
                                       int length, int length_t, Ret d) {
        if (!(src instanceof TaintableArray) || !(dest instanceof TaintableArray)) {
            System.arraycopy(src, srcPos, dest, destPos, length);
        } else {
            TaintableArray srcArr = (TaintableArray) src;
            TaintableArray destArr = (TaintableArray) dest;
            System.arraycopy(srcArr.getValue(), srcPos, destArr.getValue(), destPos, length);
            System.arraycopy(srcArr.getTaint(), srcPos, destArr.getTaint(), destPos, length);
        }
    }
}
