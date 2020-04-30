package pac.inst.taint;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import pac.config.Notify;
import pac.config.NotifyMsg;
import pac.config.RunChecks;
import pac.config.Utils;
import pac.config.BaseConfig;
import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationLocation;
import pac.inst.InstrumentationMethod;
import pac.util.Ret;

@InstrumentationClass("java/lang/Runtime")
public final class RuntimeInstrumentation {

    protected synchronized static final boolean isShellOutputStream(FileOutputStream fos) throws IOException {
        return fos.getFD().ss_shell;
    }

    protected synchronized static final void addShellOutputStream(Process p) throws IOException {
        OutputStream os = p.getOutputStream();
        if (os instanceof FilterOutputStream) {
            OutputStream os2 = ((FilterOutputStream) os).out;
            if (os2 instanceof FileOutputStream)
                ((FileOutputStream) os2).getFD().ss_shell = true;
        }
    }

    public static final String checkCommand(String cmd, Ret ret) throws IOException {

        Notify.enter_check("Runtime.checkCommand-String", cmd);

        final NotifyMsg notifyMsg = new NotifyMsg("Runtime.exec(String)", cmd, 78);
        notifyMsg.addTaintOutput(cmd);

        String newCmd;
        newCmd = RunChecks.runFromDesc(cmd, notifyMsg, RunChecks.RUNNING_EXEC, 0, null); // no env vars
        if (notifyMsg.isMessage()) {
            Notify.notifyAndRespond(notifyMsg); // IOException
        }

        return newCmd;
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP)
    public static final Process exec(final Runtime runtime, final String cmd) throws IOException {

        Notify.enter_check("Runtime.exec-String", cmd);

        // cwe-78 is "command injection"
        final NotifyMsg notifyMsg = new NotifyMsg("Runtime.exec(String)", cmd, 78);
        notifyMsg.addTaintOutput(cmd);

        // GOLD
        // Get a system env vars and convert them from Map to Array
        Ret ret = new Ret();
        String[] envp = getSysEnvArray(ret);
        // Get an array of only trusted env vars
        String[] newEnvp = BaseConfig.getInstance().processEnvp(envp, "Runtime.exec(String)", cmd); // IOException
        // GOLD

        String newCmd;
        newCmd = RunChecks.runFromDesc(cmd, notifyMsg, RunChecks.RUNNING_EXEC, 0, null); // no env vars
        if (notifyMsg.isMessage()) {
            Notify.notifyAndRespond(notifyMsg); // IOException
        }

        // For exec'ing shell commands is no need to guard against user injecting attacks.
        //   exec("<shell-cmd> <user-cmd>"))      Is illegal. First param to shell-cmd must be -c.
        //   exec("<shell-cmd> -c <cmd> <param>") -c will accept only a single argument.
        Process proc = runtime.exec(newCmd, newEnvp); // IOException  GOLD
        if (BaseConfig.getInstance().isShell(newCmd) != null)
            addShellOutputStream(proc);

        return proc;
    }

    // GOLD
    // Get system env vars. Convert them from Map to Array
    private static final String[] getSysEnvArray(Ret ret) {
        final Map<String, String> map = SystemInstrumentation.getenv(ret);
        String[] envp;

        if (map == null) {
            envp = new String[0];
        } else {
            Set<Map.Entry<String, String>> set = map.entrySet();
            envp = new String[set.size()];

            int index = 0;
            for (Entry<String, String> entry : map.entrySet()) {
                final String key = entry.getKey();
                final String value = entry.getValue();
                StringBuilder buf = new StringBuilder(ret);
                buf.append(key, ret).append("=", ret).append(value, ret);
                envp[index++] = buf.toString(ret);
            }
        }

        return envp;
    } // GOLD

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP)
    public static final Process exec(Runtime runtime, String cmd, String[] envp) throws IOException {

        Notify.enter_check("Runtime.exec-String,String[]", cmd, "String[]envp");

        // cwe-78 is "command injection"
        final NotifyMsg notifyMsg = new NotifyMsg("Runtime.exec(String, String[])", cmd, 78);
        notifyMsg.addTaintOutput(cmd);
        notifyMsg.addTaintOutput(envp);

        // GOLD
        // Return array of only those env vars in envp whose value is trusted
        final String[] newEnvp = BaseConfig.getInstance().processEnvp(envp, "Runtime.exec(String, String[])", cmd); // IOException

        String newCmd = RunChecks.runFromDesc(cmd, notifyMsg, RunChecks.RUNNING_EXEC, 0, newEnvp); // IOException

        if (notifyMsg.isMessage()) {
            Notify.notifyAndRespond(notifyMsg); // IOException
        }

        final Process proc = runtime.exec(newCmd, newEnvp); // IOException GOLD
        if (BaseConfig.getInstance().isShell(newCmd) != null)
            addShellOutputStream(proc);
        return proc;
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP)
    public static final Process exec(Runtime runtime, String cmd, String[] envp, File dir) throws IOException {

        Notify.enter_check("Runtime.exec-String,String[],File", cmd, "String[]envp", dir);

        // cwe-78 is "command injection"
        final NotifyMsg notifyMsg = new NotifyMsg("Runtime.exec(String, String[], File)", cmd, 78);

        notifyMsg.addTaintOutput(cmd);
        notifyMsg.addTaintOutput(envp);

        final String[] newEnvp = BaseConfig.getInstance().processEnvp(envp, "Runtime.exec(String, String[], File)",
                                                                      cmd); // IOException

        String newCmd;
        newCmd = RunChecks.runFromDesc(cmd, notifyMsg, RunChecks.RUNNING_EXEC, 0, envp);

        if (notifyMsg.isMessage()) {
            Notify.notifyAndRespond(notifyMsg); // IOException
        }

        final Process proc = runtime.exec(newCmd, newEnvp, dir); // IOException GOLD
        if (BaseConfig.getInstance().isShell(newCmd) != null)
            addShellOutputStream(proc);
        return proc;
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP)
    public static final Process exec(Runtime runtime, String[] cmd) throws IOException {

        Notify.enter_check("Runtime.exec-String[]", (java.lang.Object[]) cmd);

        // magic check: if shell_list is defined in config file, check if cmd[0]
        // is a shell and alter cmd[0] to be complete path to shell if needed
        // Ignore return value
        // More magic: if cmd[0] is a shell and if check execShellOnElementTwo is
        // defined, then execShellOnElementTwo is applied to cmd[2]
        final String command = Utils.arrayToString(cmd);
        // cwe-78 is "command injection"
        final NotifyMsg notifyMsg = new NotifyMsg("Runtime.exec(String[])", command, 78);
        notifyMsg.addTaintOutput(cmd);

        // Return an array of System env vars.
        Ret ret = new Ret();
        String[] envp = getSysEnvArray(ret);
        String[] newEnvp = null;

        RunChecks.runFromDescArray(cmd, notifyMsg, 0, null); // IOException

        if (notifyMsg.isMessage()) {
            Notify.notifyAndRespond(notifyMsg); // IOException
        }

        newEnvp = BaseConfig.getInstance().processEnvp(envp, "Runtime.exec(String[])", command); // IOException

        final Process proc = runtime.exec(cmd, newEnvp); // IOException GOLD
        if (BaseConfig.getInstance().isShell(cmd[0]) != null)
            addShellOutputStream(proc);
        return proc;
    } // Process exec

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP)
    public static final Process exec(Runtime runtime, String[] cmd, String[] envp) throws IOException {

        Notify.enter_check("Runtime.exec-String[],String[]", cmd[0], "...", "String[]envp");

        final String command = Utils.arrayToString(cmd);
        // cwe-78 is "command injection"
        final NotifyMsg notifyMsg = new NotifyMsg("Runtime.exec(String[], String[])", command, 78);
        notifyMsg.addTaintOutput(cmd);
        notifyMsg.addTaintOutput(envp);

        final String[] newEnvp = BaseConfig.getInstance().processEnvp(envp, "Runtime.exec(String[], String[])",
                                                                      command); // IOException

        RunChecks.runFromDescArray(cmd, notifyMsg, 0, newEnvp); // IOException - attack with action=exception

        if (notifyMsg.isMessage()) {
            Notify.notifyAndRespond(notifyMsg);
        }

        final Process proc = runtime.exec(cmd, newEnvp); // IOException
        if (BaseConfig.getInstance().isShell(cmd[0]) != null)
            addShellOutputStream(proc);
        return proc;
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP)
    public static final Process exec(Runtime runtime, String[] cmd, String[] envp, File dir) throws IOException {

        Notify.enter_check("Runtime.exec-String[],String[],File", cmd[0], "...", "String[]envp", dir);

        final String command = Utils.arrayToString(cmd);
        // cwe-78 is "command injection"
        final NotifyMsg notifyMsg = new NotifyMsg("Runtime.exec(String[], String[], File)", command, 78);
        notifyMsg.addTaintOutput(cmd);
        notifyMsg.addTaintOutput(envp);

        final String[] newEnvp = BaseConfig.getInstance().processEnvp(envp, "Runtime.exec(String[], String[], File)",
                                                                      command); // IOException

        RunChecks.runFromDescArray(cmd, notifyMsg, 0, newEnvp); // IOException - attack with action=exception

        if (notifyMsg.isMessage()) {
            Notify.notifyAndRespond(notifyMsg);
        }

        final Process proc = runtime.exec(cmd, newEnvp, dir); // IOException
        if (BaseConfig.getInstance().isShell(cmd[0]) != null)
            addShellOutputStream(proc);
        return proc;
    }

} // class RuntimeInstrumentation
