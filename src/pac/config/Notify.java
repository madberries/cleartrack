package pac.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import pac.agent.CleartrackAgent;
import pac.inst.taint.PrintWriterInstrumentation;
import pac.inst.taint.SystemInstrumentation;
import pac.util.Overflows;
import pac.util.Ret;
import pac.util.SSBuild;
import pac.util.SSVersion;
import pac.util.TaintUtils;
import pac.util.TaintValues;
import pac.util.ThreadMonitor;
import pac.wrap.ByteArrayTaint;

// notifyAndRespond
// setLogFile
// getApplicationStackTrace
// appendToLogFile
// sendMsgToTestHarness
public class Notify {
    private static PrintWriter bWriter;
    private static boolean logAll;

    public static boolean initialized;
    public static boolean confinementOn;
    private static long startMillis;

    public static boolean isLogAll() {
        return logAll;
    }

    /**
     * Write an error message to the log file
     */
    public static void error(String format, Object... args) {
        log(format, false, args);
    }

    /**
     * Write a debug message to the log file
     */
    public static void log(String format, Object... args) {
        log(format, true, args);
    }

    /**
     * Write cleartrack info such as version number and configuration information
     * to the log file
     */
    public static void info(String format, Object... args) {
        log(format, false, args);
    }

    private static void log(String format, boolean stdLog, Object... args) {
        if ((!logAll && stdLog) || bWriter == null)
            return;

        String out = format;
        for (Object arg : args) {
            int indx = out.indexOf("%");
            String prefix = out.substring(0, indx) + arg;
            if (indx + 2 < out.length())
                out = prefix + out.substring(indx + 2);
            else
                out = prefix;
        }

        // The cleartrack log file should clear after every execution.
        synchronized (Notify.class) {
            if (bWriter != null) {
                bWriter.write(out);
                bWriter.flush();
            }
        }
    }

    private static boolean getBooleanOption(Map<String, Object> optionMap, String optionName) {
        Boolean option = (Boolean) optionMap.get(optionName);
        if (option == null)
            return false;
        return option.booleanValue();
    }

    /**
     * Convert the instrumentation options map to something a little more
     * readable.
     */
    private static String toArgString(Map<String, Object> optionMap) {
        Iterator<Entry<String, Object>> iter = optionMap.entrySet().iterator();
        StringBuilder buf = new StringBuilder("[ ");
        while (iter.hasNext()) {
            boolean added = false;
            Entry<String, Object> entry = iter.next();
            Object value = entry.getValue();
            if (value instanceof Boolean) {
                if ((Boolean) value) {
                    buf.append(entry.getKey());
                    added = true;
                }
            } else if (value instanceof EnumSet<?>) {
                buf.append(entry.getKey());
                buf.append("={");
                Iterator<?> enumIter = ((EnumSet<?>) value).iterator();
                while (enumIter.hasNext()) {
                    buf.append(enumIter.next().toString());
                    if (enumIter.hasNext())
                        buf.append(",");
                }
                buf.append("}");
                added = true;
            }

            if (added && iter.hasNext()) {
                buf.append(", ");
            }
        }
        buf.append(" ]");
        return buf.toString();
    }

    /**
     * This method is called from the application main() method.
     *
     * @param optionMap map of instrumentation options used.
     */
    public synchronized static void setup(Map<String, Object> optionMap) {
        if (initialized)
            return;
        startMillis = System.currentTimeMillis();
        // register the shutdown hook (for application cleanup and 
        // flushing of the log file).
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());

        confinementOn = getBooleanOption(optionMap, "CONFINEMENT");

        PrintWriterInstrumentation.setHtmlFormatHack(Boolean.getBoolean("pac.cleartrack.htmlFormatHack"));
        logAll = Boolean.getBoolean("pac.cleartrack.logAll");
        if (logAll)
            optionMap.put("LOG_ALL", true);
        if (CleartrackAgent.agentLoaded)
            optionMap.put("AGENT", true);

        if (Boolean.getBoolean("pac.cleartrack.trustUnknown")) {
            TaintUtils.setTrustUnknown(true);
            optionMap.put("TRUST_UNKNOWN", true);
        } else {
            TaintUtils.setTrustUnknown(false);
            optionMap.put("TAINT_UNKNOWN", true);
        }

        // Open log file for writing...
        String logFile = BaseConfig.getInstance().getLogFile();
        try {
            bWriter = new PrintWriter(new FileOutputStream(logFile), true);
        } catch (FileNotFoundException e1) {
            System.err.println("ERROR: unable to write to logfile: " + logFile);
            System.exit(1);
        }

        // Write out an initial message to the log
        info("ClearTrack Agent. Version information: ");
        info("Built by %s at %s\n", SSBuild.build_user, SSBuild.build_time);
        for (String ver_line : SSVersion.version_info)
            info("    %s\n", ver_line);
        info("Configuration parsed from %s\n", BaseConfig.getInstance().getPath());
        info("Agent arguments = %s\n", toArgString(optionMap));
        info("CleartrackAgent loaded\n");

        if (!confinementOn) {
            initialized = true;
            return;
        }

        ThreadMonitor.INSTANCE.start(Thread.currentThread().getId());

        TaintUtils.trust(System.getProperty("java.version"), TaintValues.PROPERTY);

        // taint stdin...
        SystemInstrumentation.taintConsole(System.in);

        // No need to Initialize valid (gold) invoirnment vars and properties
        // SystemInstrumentation.initProperties(System.getProperties());

        initialized = true;
    }

    public synchronized static void notifyAndRespond(final NotifyMsg notifyMsg) {
        int action = notifyMsg.getAction();
        if (action == RunChecks.IGNORE_ACTION)
            return; // do nothing, log nothing, report nothing...

        final boolean executing = (action != RunChecks.EXCEPTION_ACTION && action != RunChecks.TERMINATE_ACTION);
        final StringBuilder buf = new StringBuilder();

        // Make the first line a good synopsis of the error and
        // differentiate between errors and things we want to log
        String logType;
        if (action == RunChecks.LOG_ONLY_ACTION) {
            logType = "LOG";
        } else if (executing) {
            logType = "WARN";
        } else {
            logType = "ERR";
        }

        buf.append(logType);
        buf.append(": ");
        int cweNum = notifyMsg.getCweNumber();
        if (cweNum > 0) {
            buf.append("CWE-");
            buf.append(cweNum);
            buf.append(": ");
        }
        buf.append(notifyMsg.getMessage());

        buf.append(executing ? "\nE" : "\nNot e");
        buf.append("xecuting: ");
        buf.append(notifyMsg.getDescription());
        buf.append("\n");

        // buf.append ("Checked string and its taint status:\n");
        buf.append(notifyMsg.getTaintOutput());

        buf.append("Action: ");
        try {
            buf.append(RunChecks.actionIntToString(notifyMsg.getAction())); // MsgException
            final Constructor<?> constructor = notifyMsg.getExceptionConstructor();
            if (constructor != null) {
                buf.append("  (");
                buf.append(constructor.getName());
                buf.append(")");
            }
            buf.append("\n");

        } catch (MsgException ex) {
            buf.append(ex.getMessage());
            buf.append("\n");
        }

        if (action == RunChecks.EXCEPTION_ACTION) {
            final Constructor<?> exceptionConstructor = notifyMsg.getExceptionConstructor();
            // May want to add the CWE # back to the cleartrack exception, but
            // for the sake of running the MITRE tests let's leave it out for
            // now...
            String exc_msg = notifyMsg.getCommand() + ": " + notifyMsg.getMessage();
            if (exceptionConstructor == null) {
                buf.append("Exception action was specified, but null is the exception subclass.\n");
                buf.append("Will throw a CleartrackException.\n");
                appendToLogFile(buf, notifyMsg.getCweNumber());
                throw new CleartrackException(
                        BaseConfig.getInstance().isTestHarnessEnabled() ? "Message elided for Evaluation" : exc_msg); // throw a CleartrackException by
                                                                                                                      // default
            }

            try {
                final RuntimeException ex = Utils.newException(exceptionConstructor,
                                                               BaseConfig.getInstance().isTestHarnessEnabled()
                                                                       ? "Message elided for Evaluation"
                                                                       : exc_msg);
                appendToLogFile(buf, notifyMsg.getCweNumber());
                throw ex;

            } catch (MsgException ex) {
                buf.append("Unable to create the correct RuntimeException: " + exceptionConstructor.getName() + "\n"
                        + ex.getMessage());
                appendToLogFile(buf, notifyMsg.getCweNumber());
                throw new CleartrackException(exc_msg); // throw a
                                                        // CleartrackException by
                                                        // default
            }

        } else if (action == RunChecks.TERMINATE_ACTION) {
            appendToLogFile(buf, notifyMsg.getCweNumber());
            System.exit(1);
            // System.exit(CleartrackAgent.CONTROLLED_EXIT);
        }

        appendToLogFile(buf, notifyMsg.getCweNumber());
    }

    private static String getApplicationStackTrace() {
        StringBuffer strbuf = new StringBuffer();
        StackTraceElement trace[] = Thread.currentThread().getStackTrace();
        // The 0th element is Thread.getStackTrace
        for (int i = 1; i < trace.length; i++) {
            final StackTraceElement s = trace[i];
            final String className = s.getClassName();
            if (true /* || !className.startsWith("pac.") */) {
                strbuf.append("  ");
                strbuf.append(className);
                strbuf.append(".");
                strbuf.append(s.getMethodName());
                strbuf.append("  line:");
                strbuf.append(s.getLineNumber());

                strbuf.append("\n");
            }
        }
        return strbuf.toString();
    }

    /**
     *
     * @param msg
     *            The message that will be sent to log file and to test harness
     * @param cweNumber
     *            For test harness only. If cweNumber is 0, do not send the
     *            message to the test harness
     */
    public static void appendToLogFile(final String msg, final int cweNumber) {
        if (!logAll && cweNumber == 0)
            return;

        synchronized (Notify.class) {
            if (bWriter != null) {
                bWriter.write("\n***\n"); // IOException

                final DateFormat df = new SimpleDateFormat("HH:mm:ss MM/dd/yyyy");
                final Date date = new Date(); // date now
                final String dateStr = df.format(date);
                bWriter.write(dateStr); // IOException
                bWriter.write("\n"); // IOException

                bWriter.write(msg); // IOException

                bWriter.write(getApplicationStackTrace());
                bWriter.flush();
            }
        }
    }

    private static void appendToLogFile(final StringBuilder msgBuf, final int cweNumber) {
        appendToLogFile(msgBuf.toString(), cweNumber);
    }

    private static int ec_cnt = 0;

    /**
     * Logs information about the entry to a check function (only if log all is
     * enabled).
     *
     * @param func
     * @param args for chartaint both mapped and unmapped
     */
    public static void enter_check(String func, Object... args) {
        if (!logAll)
            return;

        StringBuffer out = new StringBuffer();
        out.append("Checking " + func + "(");
        for (int i = 0; i < args.length; i++) {
            if (i > 0)
                out.append(",");
            Object arg = args[i];
            if (arg instanceof String[]) {
                String str = Arrays.toString((String[]) arg);
                out.append(str);
            } else {
                out.append(arg);
            }
        }
        out.append(")");
        out.append(" id=" + ec_cnt + "\n");
        ec_cnt++;

        for (Object arg : args) {
            if (arg instanceof String) {
                int hc = System.identityHashCode(arg);
                out.append("arg hashcode = ");
                out.append(hc);
                out.append("\n");
                // out.append ("arg hashcode = " + hc + "\n");
                out.append(TaintUtils.createTaintDisplayLines((String) arg));
            } else if (arg instanceof File) {
                File f = (File) arg;
                out.append("arg filepath = ");
                out.append(f.toString());
                out.append("\n");
                out.append(TaintUtils.createTaintDisplayLines(f));
            }
        }

        // Print the application stack for debugging purposes
        if (true) {
            StackTraceElement trace[] = Thread.currentThread().getStackTrace();
            out.append("stack trace:\n");
            for (StackTraceElement e : trace) {
                out.append("  st: ");
                out.append(e.getClassName());

                // If "." is not unmapped, a mapped char appears
                // in TC_Java_191/P_v01/repeat-run-1-?.cleartrack.log
                // Don't understand this??
                out.append(".");
                out.append(e.getMethodName());

                out.append("(");
                out.append(e.getFileName());
                out.append(":");
                out.append(e.getLineNumber());
                out.append(")\n");
                // out.append ("  st: " + e + "\n");
            }
        }

        Notify.log("\n%s", out.toString());
    }

    /**
     * Called prior to checked methods (as defined in the configuration file).
     *
     * @param args
     *            Object[]
     * @param desc
     *            String of the method call desc (as specified in the
     *            configuration file).
     * @return Object[] of sanitized arguments
     */
    @SuppressWarnings("unchecked")
    public static synchronized Object[] run_checks(Object[] args, String desc) {
        if (args == null)
            return args;
        Notify.enter_check(desc, args);

        if (args.length == 2 && args[0] != null && args[1] != null && !args[0].toString().equals("")
                && desc.startsWith("java/io/File.<init>")) {
            args[1] = TaintUtils.replaceFirst((String) args[1], "/*", "");
        }

        Ret ret = new Ret();

        Desc d = BaseConfig.getInstance().getDescriptor(desc);
        for (int i = 0; i < args.length; i++) {
            try {
                Object arg = args[i];
                Chk chk = BaseConfig.getInstance().findChkFromDescription(desc, i);
                if (chk == null)
                    continue;

                // Use descriptions CWE number over the checks CWE number...
                int cweNumber = d.paramCwes[i];
                if (cweNumber <= 0)
                    cweNumber = chk.cweNumber;

                // argument could be a string, input stream, or reader...
                String argStr = null;
                String[] argArr = null;
                List<String> argList = null;
                if (arg instanceof String) {
                    argStr = (String) arg;
                    if (argStr == null || TaintUtils.isTrusted(argStr))
                        continue;
                    if ((desc.startsWith("java/lang/Class.forName")
                            || desc.startsWith("java/lang/ClassLoader.loadClass")) && TaintUtils.hasUnknown(argStr))
                        continue; // assume that this was invoked by the JVM
                } else if (arg instanceof InputStream) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream(ret);
                    TaintUtils.copyTaint((InputStream) arg, out, ret);
                    argStr = out.toString(ret);
                    if (TaintUtils.isTrusted(argStr))
                        continue;
                } else if (arg instanceof Reader) {
                    StringWriter writer = new StringWriter(ret);
                    TaintUtils.copyTaint((Reader) arg, writer, ret);
                    argStr = writer.toString(ret);
                    if (TaintUtils.isTrusted(argStr))
                        continue;
                } else if (arg instanceof String[]) {
                    argArr = (String[]) arg;
                } else if (arg instanceof List) {
                    argList = (List<String>) arg;
                    argArr = argList.toArray(new String[0]);
                } else if (arg instanceof URL[]) {
                    URL[] urls = (URL[]) arg;
                    if (urls == null || urls.length == 0)
                        continue;

                    // piece all of the URL paths together, since the
                    // check is merely going to check for any tainted
                    // data...
                    StringBuilder sb = TaintUtils.newStringBuilder();
                    for (int k = 0; k < urls.length; k++) {
                        TaintUtils.append(sb, TaintUtils.trustConstant("url["));
                        TaintUtils.append(sb, TaintUtils.trust("" + k));
                        TaintUtils.append(sb, TaintUtils.trustConstant("] = "));
                        TaintUtils.append(sb, urls[k].getPath());
                        if (i < urls.length - 1)
                            TaintUtils.append(sb, TaintUtils.trustConstant(", "));
                    }
                    argStr = TaintUtils.toString(sb);
                } else {
                    continue;
                }

                if (argArr != null) {
                    if (argArr.length == 0)
                        continue;
                    final String commandStr = Utils.arrayToString(argArr);
                    final NotifyMsg notifyMsg = new NotifyMsg(desc, commandStr, cweNumber);
                    notifyMsg.addTaintOutput(argArr);
                    RunChecks.runFromDescArray(argArr, notifyMsg, i, null); // no
                                                                            // env
                                                                            // vars
                    if (argList != null) {
                        for (int j = 0; j < argArr.length; j++) {
                            argList.set(j, argArr[j]);
                        }
                    }
                } else {
                    final NotifyMsg notifyMsg = new NotifyMsg(desc, argStr, cweNumber);
                    notifyMsg.addTaintOutput(argStr);
                    String newArg = RunChecks.runFromDesc(argStr, notifyMsg, 0, i, null); // no env vars
                    if (argStr != newArg) {
                        Object argObj;
                        if (arg instanceof InputStream) {
                            ByteArrayTaint res = newArg.getBytes(Charset.defaultCharset(), ret);
                            argObj = new ByteArrayInputStream(res, ret);
                        } else if (arg instanceof Reader) {
                            argObj = new StringReader(newArg, ret);
                        } else {
                            argObj = (String) newArg;
                        }
                        args[i] = argObj; // replace the argument with
                                          // sanitized argument
                    }

                    if (notifyMsg.isMessage()) {
                        Notify.notifyAndRespond(notifyMsg); // IOException
                    }
                }
            } catch (IOException e) {
            }
        }
        return args;
    }

    /**
     * Called after the checked method (as defined in the configuration file).
     *
     * @param obj
     *            Object of the return value.
     * @param desc
     *            String of the method call desc (as specified in the
     *            configuration file).
     */
    public static synchronized Object run_after_check(Object obj, String desc) {
        if (obj == null)
            return null;
        String arg;
        if (obj instanceof URL)
            arg = ((URL) obj).getPath(new Ret());
        else if (obj instanceof URI)
            arg = ((URI) obj).getPath(new Ret());
        else if (obj instanceof Path)
            arg = ((Path) obj).toString(new Ret());
        else
            return obj;

        try {
            Notify.enter_check(desc, arg);
            Chk chk = BaseConfig.getInstance().findChkFromDescription(desc, -1);
            if (chk == null)
                return obj;
            final NotifyMsg notifyMsg = new NotifyMsg(desc, arg, chk.cweNumber);
            notifyMsg.addTaintOutput(arg);
            if (arg != null && !TaintUtils.isTrusted(arg)) {
                String newArg = RunChecks.runFromDesc(arg, notifyMsg, 0, -1, null);
                if (newArg != arg && obj instanceof Path) {
                    return FileSystems.getDefault().getPath(newArg);
                }
            }
            if (notifyMsg.isMessage()) {
                Notify.notifyAndRespond(notifyMsg); // IOException
            }
        } catch (IOException e) {

        }
        return obj;
    }

    /**
     * Shutdown hook responsible for finalizing the application.  This will clear
     * out any catch block information for all threads (by exitting the catch).
     * Also, print the runtime and flush the log file.  This should always get
     * called, so long that Java receives a healthy kill/interrupt signal.
     * 
     * @author jeikenberry
     */
    private static class ShutdownHook extends Thread {

        @Override
        public void run() {
            // Ensure that we have flushed the log file
            // prior to exitting the application.
            synchronized (Notify.class) {
                if (bWriter != null) {
                    //#if log_overflow==true
                    try {
                        if (confinementOn)
                            Overflows.print(bWriter);
                    } catch (IOException e) {
                    }
                    //#endif

                    info("\nTotal Execution Time: %s seconds",
                         ((double) (System.currentTimeMillis() - startMillis)) / 1000.0d);
                    bWriter.flush();
                    bWriter.close();
                    bWriter = null;
                }
            }
        }

    }
} // class Notify
