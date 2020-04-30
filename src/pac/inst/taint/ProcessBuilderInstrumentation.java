package pac.inst.taint;

import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

@InstrumentationClass("java/lang/ProcessBuilder")
public final class ProcessBuilderInstrumentation {

    // CUT AND PASTE from RuntimeInstrumentation
    protected synchronized static final void addShellOutputStream(Process p) throws IOException {
        OutputStream os = p.getOutputStream();
        if (os instanceof FilterOutputStream) {
            OutputStream os2 = ((FilterOutputStream) os).out;
            if (os2 instanceof FileOutputStream)
                ((FileOutputStream) os2).getFD().ss_shell = true;
        }
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP)
    public static final Process start(ProcessBuilder pb) throws IOException {
        // Get command from ProcessBuilder. Convert command to String[]
        final List<String> cmd_list = pb.command();
        final String[] cmd_ray = cmd_list.toArray(new String[0]);

        Notify.enter_check("ProcessBuilder.start-String[]", (java.lang.Object[]) cmd_ray);
        final String cmd_str = Utils.arrayToString(cmd_ray);
        final NotifyMsg notifyMsg = new NotifyMsg("ProcessBuilder.start(String[])", cmd_str, 78);

        // In order for pb.environment() to return a properly tainted Map,
        // must first call SystemInstrumentation.getenv()
        Ret ret = new Ret();
        SystemInstrumentation.getenv(ret);
        final Map<String, String> env_map = pb.environment();

        final Set<String> key_set = env_map.keySet();
        final Iterator<String> it = key_set.iterator();
        int i = 0;
        final String[] env_array = new String[env_map.size()];
        while (it.hasNext()) {
            final String key = it.next();
            final String value = env_map.get(key);

            // Perform instrumented: "env_array[i] = key + "=" + value;"
            StringBuilder buf = new StringBuilder(ret);
            buf.append(key, ret).append("=", ret).append(value, ret);
            env_array[i++] = buf.toString(ret);
        }

        // The return array contains only those env vars in envp_array
        // whose value is trusted
        final String[] new_env_array = BaseConfig.getInstance().processEnvp(env_array, "ProcessBuilder.start(String[])",
                                                                            cmd_str); // IOException
        RunChecks.runFromDescArray(cmd_ray, notifyMsg, 0, new_env_array); // IOException

        // Convert possibly altered cmd_ray to List<String>
        final List<String> new_cmd_list = Arrays.asList(cmd_ray);
        pb.command(new_cmd_list);

        final Process proc = pb.start();

        if (BaseConfig.getInstance().isShell(cmd_list.get(0)) != null)
            addShellOutputStream(proc);

        return proc;
    }

} // class ProcessBuilderInstrumentation
