package pac.inst.taint;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import com.sun.security.auth.module.UnixSystem;

import pac.config.Notify;
import pac.config.NotifyMsg;
import pac.config.RunChecks;
import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.inst.InvocationType;
import pac.util.Ret;
import pac.util.TaintUtils;
import pac.util.TaintValues;

@InstrumentationClass("java/io/FileInputStream")
public final class FileInputStreamInstrumentation extends InputStreamInstrumentation {
    //	private static String cur_username;
    private static long cur_uid;
    private static Set<Long> cur_gids;

    static {
        UnixSystem u = new UnixSystem();
        cur_uid = u.getUid();
        cur_gids = new HashSet<Long>();
        for (long gid : u.getGroups()) {
            cur_gids.add(gid);
        }
    }

    private static final long getAttr(Path path, String name) throws IOException {
        return ((Integer) Files.getAttribute(path, "unix:" + name)).longValue();
    }

    public static final boolean shouldTrustContent(File file) {
        return shouldTrustContent(file, new Ret());
    }

    /**
     * New MITRE policy for trusting file content.
     * 
     * @param file
     * @return true iff one of the uid or gid matches that of the current
     * 			user and the filename is trusted
     */
    public static final boolean shouldTrustContent(File file, Ret ret) {
        Path path = file.toPath();
        Notify.log("considering trust of %s\n", file);
        try {
            long uid = getAttr(path, "uid");
            long gid = getAttr(path, "gid");
            // trust this file if the filename is trusted and
            // the user id and group id matches that of the
            // current user...
            Notify.log("  file uid = %d, cur_uid = %d\n", uid, cur_uid);
            if ((cur_uid == uid || cur_gids.contains(gid)) && TaintUtils.isTrusted(file.getPath(ret))) {
                Notify.log("  File is trusted\n");
                return true;
            }
        } catch (IOException e) {
            // we were unable to obtain file attributes, so conservatively
            // assume that the file content should not be trusted
            Notify.log("  Could not determine the files uig/gid due to " + e);
        }
        Notify.log("  File is untrusted\n");
        return false;
    }

    // CONSTRUCTORS

    /**
    * Mark the returned FileInputStream tainted if config file lists the canonical path of File as trusted
    * If this method marks FileInputStream tainted, send message to log, and not to test harness
    */
    @InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR)
    public static final FileInputStream init(File file, Ret ret) throws Exception {
        FileInputStream fis;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException fne) {
            // Perform an existential TOU check on file
            FileInstrumentation.timeOfUse("FileInputStream.<init>(Ljava/io/File;)", file, null);
            throw fne;
        }

        Notify.enter_check("FileInputStream-File", file);

        // mark FileInputStream tainted only if file is not a path listed in config file as trusted

        final NotifyMsg notifyMsg = new NotifyMsg("FileInputStream(File)", "FileInputStream(" + file.toString() + ")",
                0); // cwe-0 signals to not output error message to test harness
        // If canonical path of file is untrusted, then taint fis object
        FileDescriptor fd = fis.getFD();
        try {
            // Perform an existential TOU check on file with the open FD
            FileInstrumentation.timeOfUse("FileInputStream.<init>(Ljava/io/File;)", file, fd);
        } catch (Exception e) {
            // Be sure to close the random access file we opened, since
            // we are throwing an exception...
            fis.close();
            throw e;
        }

        // If we fail the file content check defined in the config file
        // the content will be trusted if and only if it passes the new
        // MITRE policy.
        if (!RunChecks.checkLegalFileName(fd, file, notifyMsg) && shouldTrustContent(file, ret)) {
            fd.fd_t = TaintValues.TRUSTED | TaintValues.FILE;
        }
        Notify.log("The contents of file '%s' are %s (user application)\n", file,
                   TaintUtils.toTrustString(fd.fd_t));
        fis.ss_hasUniformTaint = true;
        fis.ss_taint = fd.fd_t;

        return fis;
    }

    @InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR)
    public static final FileInputStream init(FileDescriptor filedesc, Ret ret) {
        FileInputStream fis = new FileInputStream(filedesc);
        fis.ss_hasUniformTaint = true;
        fis.ss_taint = filedesc.fd_t;
        return fis;
    }

    /**
    * Mark the returned FileInputStream tainted if config file lists the canonical path of filename as trusted.
    * If this method marks FileInputStream tainted, send message to log, and not to test harness
    */
    @InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR)
    public static final FileInputStream init(String filename, Ret ret) throws Exception {
        File file = filename == null ? null : new File(filename);
        return FileInputStreamInstrumentation.init(file, ret);
    }
}
