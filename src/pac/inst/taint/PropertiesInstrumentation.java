package pac.inst.taint;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import pac.config.Notify;
import pac.config.NotifyMsg;
import pac.config.RunChecks;
import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.util.Ret;
import pac.util.TaintUtils;
import pac.util.TaintValues;

@InstrumentationClass("java/util/Properties")
public final class PropertiesInstrumentation {

    // INSTANCE METHODS

    @InstrumentationMethod
    public static final void load(Properties props, InputStream inStream, Ret ret) throws IOException {
        // keep around existing properties so we can easily tell what
        // properties had changed.
        Map<?, ?> oldProps = (Map<?, ?>) props.clone();

        props.load(inStream, ret); // IOException

        // for each new or altered property, trust or taint
        // based on the taint of the stream.
        Enumeration<?> keys = props.propertyNames();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();

            // ORIG FIXED
            String val = props.getProperty(key);
            Object oldVal = oldProps.get(key);
            if (val != null && !val.equals(oldVal) && inStream.ss_hasUniformTaint) {
                if ((inStream.ss_taint & TaintValues.TRUST_MASK) == TaintValues.TRUSTED) {
                    TaintUtils.trust(key, TaintValues.PROPERTY);
                    TaintUtils.trust(val, TaintValues.PROPERTY);
                } else {
                    TaintUtils.taint(key, TaintValues.PROPERTY);
                    TaintUtils.taint(val, TaintValues.PROPERTY);
                }
            }
        }
    }

    @InstrumentationMethod
    public static final void load(Properties props, Reader reader, Ret ret) throws IOException {
        // keep around existing properties so we can easily tell what
        // properties had changed.
        Map<?, ?> oldProps = (Map<?, ?>) props.clone();

        props.load(reader, ret);

        // for each new or altered property, trust or taint
        // based on the taint of the stream.
        Enumeration<?> keys = props.propertyNames();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();

            // ORIG FIXED
            String val = props.getProperty(key);
            Object oldVal = oldProps.get(key);
            if (val != null && !val.equals(oldVal) && reader.ss_hasUniformTaint) {
                if ((reader.ss_taint & TaintValues.TRUST_MASK) == TaintValues.TRUSTED) {
                    TaintUtils.trust(key, TaintValues.PROPERTY);
                    TaintUtils.trust(val, TaintValues.PROPERTY);
                } else {
                    TaintUtils.taint(key, TaintValues.PROPERTY);
                    TaintUtils.taint(val, TaintValues.PROPERTY);
                }
            }
        }
    }

    /**
     * Sets the taint on the acquired property. Properties are trusted if they
     * match their correct (gold) value or if the config file trusts them
     */
    @InstrumentationMethod
    public static final String getProperty(Properties props, String key, Ret ret) {
        String res = props.getProperty(key, ret);
        if (res == null)
            return null;

        // Always trust these and prevent recursion
        if (key.equals("line.separator") || key.equals("path.separator")) {
            TaintUtils.trust(res, TaintValues.PROPERTY);
            return res;
        }

        Notify.enter_check("Properties.getProperty", key, res);

        // Calling notifyAndRespond causes infinite recursion: appending to log
        // file calls getProperty
        final NotifyMsg notifyMsg = new NotifyMsg("Properties.getProperty(String)",
                "Properties.getProperty(" + key + ")");

        // Notify.log ("%s istracked = %b\n", key,
        // TaintValues.isTracked(res));
        if (!TaintUtils.isTrusted(res)) {
            RunChecks.checkLegalProperty(key, res, notifyMsg);
        }

        Notify.log("result taint:\n%s\n", TaintUtils.createTaintDisplayLines(res));
        if (TaintUtils.isTainted(res))
            Notify.log("properties result tainted\n");

        /*
         * try {
         * 
         * // ORIG FIXED if (! TaintValues.isTracked(res)) {
         * RunChecks.checkLegalProperty(key, res, notifyMsg); // MssgException
         * if (notifyMsg.isMessage()) { // Output message to log file only.
         * Because cwe is 0, message will not go to test harness
         * Notify.notifyAndRespond(notifyMsg); // IOException } } // ORIG FIXED
         */
        /*
         * ORIG if (!RunChecks.checkLegalProperty(key, res, notifyMsg)) {
         * notifyMsg.append("Setting property value to tainted.\n");
         * 
         * // Output message to log file only. Because cwe is 0, message will
         * not go to test harness Notify.notifyAndRespond(notifyMsg); //
         * IOException }
         */

        /*
         * } catch (MsgException ex) { Notify.error("Exception: " + ex + "\n");
         * } catch (IOException ex) { Notify.error("Exception: " + ex + "\n"); }
         */

        return res;
    }

    @InstrumentationMethod
    public static final String getProperty(Properties props, String key, String defaultValue, Ret ret) {
        String res = props.getProperty(key, defaultValue);
        if (res == null)
            return null;

        // Must check in_application first, because enter_check generates
        // recursive calls.
        Notify.enter_check("Properties.getProperty-def", key, defaultValue, res);

        // Calling notifyAndRespond causes infinite recursion: appending to log
        // file calls getProperty
        final NotifyMsg notifyMsg = new NotifyMsg("Properties.getProperty(String, String)",
                "Properties.getProperty(" + key + ", " + defaultValue + ")");

        // GOLD
        if (!TaintUtils.isTrusted(res)) {
            RunChecks.checkLegalProperty(key, res, notifyMsg);
        }

        Notify.log("result taint:\n%s\n", TaintUtils.createTaintDisplayLines(res));
        if (TaintUtils.isTainted(res))
            Notify.log("properties result tainted\n");
        return res;

        /*
         * try { // ORIG FIXED if (! TaintValues.isTracked(res)) {
         * RunChecks.checkLegalProperty(key, res, notifyMsg); if
         * (notifyMsg.isMessage()) { // Output message to log file only. Because
         * cwe is 0, message will not go to test harness
         * Notify.notifyAndRespond(notifyMsg); // IOException } } // ORIG FIXED
         */
        /*
         * ORIG if (!RunChecks.checkLegalProperty(key, res, notifyMsg)) {
         * notifyMsg.append("Setting property value to tainted.\n");
         * 
         * // Output message to log file only. Because cwe is 0, message will
         * not go to test harness Notify.notifyAndRespond(notifyMsg); //
         * IOException }
         */

        /*
         * } catch (MsgException ex) { Notify.error("Exception: " + ex + "\n");
         * } catch (IOException ex) { Notify.error("Exception: " + ex + "\n"); }
         * return res;
         */
    }
    
}
