package pac.inst.taint;

import java.sql.Connection;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import pac.config.BaseConfig;
import pac.config.Notify;
import pac.config.NotifyMsg;
import pac.config.RunChecks;
import pac.config.CleartrackException;
import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.util.Ret;
import pac.util.TaintUtils;

@InstrumentationClass(value = "java/sql/Connection", isInterface = true)
public final class ConnectionInstrumentation {

    @InstrumentationMethod
    public static final void setCatalog(Connection connection, String catalog) throws SQLException {
        Notify.enter_check("Connection.setCatalog", catalog);
        try {
            if (!TaintUtils.isTrusted(catalog)) {
                final NotifyMsg notifyMsg = new NotifyMsg("Connection.setCatalog(String)",
                        "Connection(" + catalog + ")", 15); // cwe-15 is "External control of system or configuration setting"
                notifyMsg.addTaintOutput(catalog);
                if (BaseConfig.getInstance().canSetCatalogWithUntrustedData()) {
                    notifyMsg.setAction(RunChecks.LOG_ONLY_ACTION);
                    notifyMsg.append("setCatalog was performed.\n");
                } else {
                    notifyMsg.setExceptionConstructor(CleartrackException.class
                            .getConstructor(new Class[] { String.class })); // NoSuchMethodException
                    notifyMsg.setAction(RunChecks.EXCEPTION_ACTION);
                    notifyMsg.append("setCatalog not performed.\n");
                }

                notifyMsg.append(catalog + " is not trusted.");
                Notify.notifyAndRespond(notifyMsg); // IOException
            }
            connection.setCatalog(catalog); // SQLException

        } catch (NoSuchMethodException ex) {
            throw new SQLException("Attack detected in call to Connection.setCatalog(" + catalog + ")");
        }
    }

    @InstrumentationMethod
    public static final void setClientInfo(Connection connection, Properties properties) throws SQLClientInfoException {
        Notify.enter_check("Connection.setClientInfo-Properties");
        try {
            Enumeration<?> keys = properties.propertyNames();
            StringBuilder strbuf = new StringBuilder();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                String val = PropertiesInstrumentation.getProperty(properties, key, new Ret());
                if (!TaintUtils.isTrusted(key) || !TaintUtils.isTrusted(val)) {
                    strbuf.append("  ");
                    strbuf.append(key);
                    strbuf.append("=");
                    strbuf.append(val);
                }
            }

            if (strbuf.length() > 0) {
                final NotifyMsg notifyMsg = new NotifyMsg("Connection.setClientInfo(Properties)",
                        "setClientInfo(Properties)", 15); // cwe-15 is "External control of system or configuration setting"

                if (BaseConfig.getInstance().canSetClientInfoWithUntrustedProps()) {
                    notifyMsg.setAction(RunChecks.LOG_ONLY_ACTION);
                    notifyMsg.append("setClientInfo was performed with untrusted properties.\n");
                } else {
                    notifyMsg.setExceptionConstructor(CleartrackException.class
                            .getConstructor(new Class[] { String.class })); // NoSuchMethodException
                    notifyMsg.setAction(RunChecks.EXCEPTION_ACTION);
                    notifyMsg.append("setClientInfo not performed.\n");
                }
                notifyMsg.append("Properties contains these untrusted properties.\n");
                notifyMsg.append(strbuf.toString());
                Notify.notifyAndRespond(notifyMsg); // IOException
            }
            connection.setClientInfo(properties); // SQLClientInfoException

        } catch (NoSuchMethodException ex) {
            throw new RuntimeException("Attack detected in call to Connection.setClientInfo(Properties)");
        }
    }

    @InstrumentationMethod
    public static final void setClientInfo(Connection connection, String name, String value)
            throws SQLClientInfoException {
        Notify.enter_check("Connection.setClientInfo", name, value);

        try {
            StringBuilder errMsg = new StringBuilder();

            if (!TaintUtils.isTrusted(name)) {
                errMsg.append("  ");
                errMsg.append(name);
                errMsg.append(" is untrusted.");
            }

            if (!TaintUtils.isTrusted(value)) {
                errMsg.append("  ");
                errMsg.append(value);
                errMsg.append(" is untrusted.");
            }

            if (errMsg.length() > 0) {
                final NotifyMsg notifyMsg = new NotifyMsg("Connection.setClientInfo(String,String)",
                        "Connection(" + name + ", " + value + ")", 15); // cwe-15 is "External control of system or configuration setting"
                notifyMsg.addTaintOutput(name);
                notifyMsg.addTaintOutput(value);
                if (BaseConfig.getInstance().canSetClientInfoWithUntrustedNameOrValue()) {
                    notifyMsg.setAction(RunChecks.LOG_ONLY_ACTION);
                    notifyMsg.append("setClientInfo performed with untrusted data.\n");
                } else {
                    notifyMsg.setExceptionConstructor(CleartrackException.class
                            .getConstructor(new Class[] { String.class })); // NoSuchMethodException
                    notifyMsg.setAction(RunChecks.EXCEPTION_ACTION);
                    notifyMsg.append("setClientInfo not performed.\n");
                }
                notifyMsg.append(errMsg.toString());
                Notify.notifyAndRespond(notifyMsg); // IOException
            }
            connection.setClientInfo(name, value); // SQLClientInfoException

        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(
                    "Attack detected in call to Connection.setClientInfo(" + name + ", " + value + ")");
        }
    }

    @InstrumentationMethod
    public static final Savepoint setSavepoint(Connection connection, String name) throws SQLException {
        Notify.enter_check("Connection.setSavepoint", name);

        Savepoint savePoint = null;

        try {
            String errMsg = (!TaintUtils.isTrusted(name) ? (name + " is untrusted.") : null);

            if (errMsg != null) {
                final NotifyMsg notifyMsg = new NotifyMsg("Connection.setSavepoint(String)", "Connection(" + name + ")",
                        15); // cwe-15 is "External control of system or configuration setting"
                notifyMsg.addTaintOutput(name);
                if (BaseConfig.getInstance().canSetSavePointWithUntrustedName()) {
                    notifyMsg.setAction(RunChecks.LOG_ONLY_ACTION);
                    notifyMsg.append("setSavepoint was performed.\n");
                } else {
                    notifyMsg.setExceptionConstructor(CleartrackException.class
                            .getConstructor(new Class[] { String.class })); // NoSuchMethodException
                    notifyMsg.setAction(RunChecks.EXCEPTION_ACTION);
                    notifyMsg.append("setSavepoint was not performed.\n");
                }

                notifyMsg.append(errMsg);

                Notify.notifyAndRespond(notifyMsg); // IOException
            }
            savePoint = connection.setSavepoint(name); // SQLClientInfoException

        } catch (NoSuchMethodException ex) {
            throw new RuntimeException("Attack detected in call to Connection.setSavepoint(" + name + ")");
        }

        return savePoint; // Returns null if attack was discovered
    }

    @InstrumentationMethod
    public static final void setTypeMap(Connection connection, Map<String, Class<?>> map) throws SQLException {
        Notify.enter_check("Connection.setTypeMap");
        try {
            StringBuilder errmsg = new StringBuilder();
            Set<String> keys = map.keySet();
            Iterator<String> keyIt = keys.iterator();
            while (keyIt.hasNext()) {
                String key = keyIt.next();
                if (!TaintUtils.isTrusted(key)) {
                    errmsg.append("  ");
                    errmsg.append(key);
                    errmsg.append(" is untrusted.");
                }
            }

            if (errmsg.length() > 0) {
                final NotifyMsg notifyMsg = new NotifyMsg("Connection.setTypeMap(Map<String,Class<?>>)",
                        "Connection(Map<String,Class<?>> map)", 15); // cwe-15 is "External control of stystem or configuration setting"
                if (BaseConfig.getInstance().canSetTypeMapWithUntrustedMap()) {
                    notifyMsg.setAction(RunChecks.LOG_ONLY_ACTION);
                    notifyMsg.append("The TypeMap was installed.");
                } else {
                    notifyMsg.setExceptionConstructor(CleartrackException.class
                            .getConstructor(new Class[] { String.class })); // NoSuchMethodException
                    notifyMsg.setAction(RunChecks.EXCEPTION_ACTION);
                    notifyMsg.append("The TypeMap was not installed.");
                }
                notifyMsg.append(" The following keys in Map were not trusted\n");

                notifyMsg.append(errmsg.toString());
                Notify.notifyAndRespond(notifyMsg); // IOException
            }
            connection.setTypeMap(map); // SQLException

        } catch (NoSuchMethodException ex) {
            throw new RuntimeException("Attack detected in call to Connection.setTypeMap(Map<String,Class<?>>)");
        }
    }
    
}
