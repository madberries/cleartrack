package pac.inst.taint;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import pac.config.BaseConfig;
import pac.config.Notify;
import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationLocation;
import pac.inst.InstrumentationMethod;
import pac.inst.InvocationType;
import pac.util.FileStat;
import pac.util.OS;
import pac.util.Ret;
import pac.util.TaintUtils;
import pac.util.TaintValues;

// These methods retain taint info: File.getPath()
// File.toString()
// File.getName()
// getAbsoluteFile()
// getAbsolutePath()
// getParent()
// getParentFile()
// getCanonicalPath()
// getCanonicalFile()
// list()
// listFiles()
//
// private
// listFilesCommon
//
// TODO
// listRoots
// toURL
// toURI File.toURI().toURL() is a common sequence
// File.toURI() looses taint info

@InstrumentationClass("java/io/File")
public final class FileInstrumentation {
  public static final int PATH_MAX = 256;

  protected static final Map<String, FileStat> attrMap =
      Collections.synchronizedMap(new WeakHashMap<String, FileStat>());

  /**
   * Stat file and store the results.
   * 
   * @param file File
   * @return FileStat
   * @throws IOException
   */
  protected static final FileStat timeOfCheck(File file) throws IOException {
    return timeOfCheck(file, false, false);
  }

  /**
   * Stat file and store the results, for the purpose of a TOC-to-TOU check.
   * 
   * @param file File to stat.
   * @param symbolic boolean set to true iff we should perform an lstat on file.
   * @param exists boolean set to true iff this is an existential check.
   * @return FileStat
   * @throws IOException
   */
  protected static final FileStat timeOfCheck(File file, boolean symbolic, boolean exists)
      throws IOException {
    try {
      // Make sure that a NullPointerException is thrown in the event that file is null. Otherwise a
      // native call on a null object could crash the JVM.
      String key = file.getPath();
      FileStat fstat = symbolic ? FileStat.lstat(file) : FileStat.stat(file);
      attrMap.put(key, fstat);
      return fstat;
    } catch (IOException e) {
      if (exists) { // We will map a null value for exists check (where the file does not exist).
        attrMap.put(file.getPath(), null);
        return null;
      }
      throw e;
    }
  }

  /**
   * Called to ensure that the file we are using is in a consistent state with the file object that
   * was checked prior to this usage.
   * 
   * @param method String of the method name and descriptor that was called.
   * @param file File of the used file.
   * @param fd FileDescriptor of the opened file.
   * @throws IOException
   * @throws NoSuchMethodException
   * @throws SecurityException
   */
  protected static void timeOfUse(String method, File file, FileDescriptor fd)
      throws IOException, NoSuchMethodException, SecurityException {
    String errorMsg = null;
    String path = file.getPath();
    FileStat statAtCheck = attrMap.get(path);
    if (statAtCheck != null) {
      if (fd == null) {
        // This was checked to exists, but no longer exists...
        errorMsg = "the file '" + file + "' has been checked to exist, but no longer exists:"
            + "    " + statAtCheck.toString();
      } else {
        FileStat stat = FileStat.fstat(fd);
        if (!stat.equals(statAtCheck)) {
          errorMsg = "the file '" + file + "' has an inconsistent inode state:\n" + "    "
              + statAtCheck.toString() + "\n" + "    " + stat.toString(file);
        }
      }
    }

    // throw an exception if we found any inconsistencies.
    if (errorMsg != null)
      BaseConfig.getInstance().handleToctou(method, errorMsg);
  }

  @InstrumentationMethod
  public static final boolean canRead(File file, Ret ret) {
    try {
      return timeOfCheck(file).isReadable();
    } catch (IOException e) {
      return false;
    }
  }

  @InstrumentationMethod
  public static final boolean canWrite(File file, Ret ret) {
    try {
      return timeOfCheck(file).isWritable();
    } catch (IOException e) {
      return false;
    }
  }

  @InstrumentationMethod
  public static final boolean canExecute(File file, Ret ret) {
    try {
      return timeOfCheck(file).isExecutable();
    } catch (IOException e) {
      return false;
    }
  }

  @InstrumentationMethod
  public static final boolean isDirectory(File file, Ret ret) {
    try {
      return timeOfCheck(file).isDirectory();
    } catch (IOException e) {
      return false;
    }
  }

  @InstrumentationMethod
  public static final boolean isFile(File file, Ret ret) {
    try {
      return timeOfCheck(file).isRegular();
    } catch (IOException e) {
      return false;
    }
  }

  @InstrumentationMethod
  public static final long lastModified(File file, Ret ret) {
    try {
      // TODO: First of all, we assume the else case is Linux as we have not
      //       tested this on Windows, for example.  Also, it's unclear why the
      //       two stat formats are necessarily different here, but maybe there
      //       is some difference in syscalls between the two OSes??
      if (OS.get().isMac())
        return timeOfCheck(file).lastModified();
      return timeOfCheck(file).lastModifiedPrecise();
    } catch (IOException e) {
      return 0;
    }
  }

  /**
   * Recursively copies taint from the original path to the resolved canonical path by starting from
   * the root and working up.
   * 
   * @param path File object of the path to resolve.
   * @return File representing the canonicalized path.
   * @throws IOException
   */
  private static final File canonicalize(File path, Ret ret) throws IOException {
    File parent = path.getParentFile(ret);
    File newPath;
    if (parent != null)
      newPath = canonicalize(parent, ret);
    else
      return path;
    newPath = new File(newPath.toString(ret), path.getName(ret), ret);
    File realPath = newPath.getCanonicalFile();
    if (!realPath.equals(newPath)) {
      String unresolved = newPath.toString(ret);
      String resolved = realPath.toString(ret);
      resolved = TaintUtils.copyTaintPath(unresolved, resolved);
      return new File(resolved, ret);
    }
    return newPath;
  }

  @InstrumentationMethod
  public static final boolean mkdir(File file, Ret ret) {
    boolean result = file.mkdir();
    ret.taint = file.exists() ? TaintValues.TRUSTED : TaintValues.TAINTED;
    return result;
  }

  @InstrumentationMethod
  public static final boolean mkdirs(File file, Ret ret) {
    boolean result = file.mkdirs();
    ret.taint = file.exists() ? TaintValues.TRUSTED : TaintValues.TAINTED;
    return result;
  }

  @InstrumentationMethod
  public static final boolean delete(File file, Ret ret) {
    boolean result = file.delete();
    ret.taint = file.exists() ? TaintValues.TAINTED : TaintValues.TRUSTED;
    attrMap.put(file.getPath(), null);
    return result;
  }

  @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP)
  public static final boolean renameTo(File srcFile, File destFile, Ret ret) throws IOException {
    boolean result = srcFile.renameTo(destFile);
    FileInstrumentation.attrMap.put(srcFile.getPath(), null);
    try {
      FileInstrumentation.timeOfCheck(destFile, false, true);
    } catch (IOException e) {
      if (result) // We somehow succeeded, but failed on the stat?
        throw e;
    }
    return result;
  }

  @InstrumentationMethod
  public static final boolean createNewFile(File file, Ret ret) throws IOException {
    boolean result = file.createNewFile();
    ret.taint = file.exists() ? TaintValues.TRUSTED : TaintValues.TAINTED;
    return result;
  }

  @InstrumentationMethod(invocationType = InvocationType.STATIC)
  public static final File createTempFile(String prefix, String suffix, File directory, Ret ret)
      throws IOException {
    File result = File.createTempFile(prefix, suffix, directory, ret);
    result.deleteOnExit(); // Ensure we do proper cleanup when we exit the application.
    return result;
  }

  @InstrumentationMethod
  public static final boolean exists(File file, Ret ret) throws IOException {
    return timeOfCheck(file, false, true) != null;
  }

  @InstrumentationMethod
  public static final String getAbsolutePath(File file, Ret ret)
      throws NoSuchMethodException, SecurityException {
    String absPath = file.getAbsolutePath(ret);
    String path = file.getPath();
    synchronized (attrMap) {
      if (attrMap.containsKey(path)) {
        attrMap.put(absPath, attrMap.get(path));
      }
    }
    return absPath;
  }

  @InstrumentationMethod
  public static final File getAbsoluteFile(File file, Ret ret)
      throws NoSuchMethodException, SecurityException {
    File absFile = file.getAbsoluteFile(ret);
    String path = file.getPath();
    synchronized (attrMap) {
      if (attrMap.containsKey(path)) {
        attrMap.put(absFile.getPath(), attrMap.get(path));
      }
    }
    return absFile;
  }

  /**
   * @param file If this file's file.canonicalFile() is tracked, return it. If this file's
   *        file.canonicalFile() is not tracked, taint or trust the String based on file.toString()
   *        trust/taint, and return it.
   * @return A File whose toString() is tracked and whose trust/untrust is derived from input file.
   * @throws IOException
   * @throws SecurityException
   * @throws NoSuchMethodException
   */
  @InstrumentationMethod
  public static final String getCanonicalPath(File file, Ret ret)
      throws IOException, NoSuchMethodException, SecurityException {
    File canonFile = getCanonicalFile(file, ret);
    FileStat canonStat, fileStat;
    try {
      canonStat = timeOfCheck(canonFile);
    } catch (IOException e) {
      canonStat = null;
    }
    try {
      fileStat = timeOfCheck(file);
    } catch (IOException e) {
      // It's possible that the canonical path exists, but the original path does not resolve at
      // some point (i.e. nonexisting_dir/..). So, let's presume this is ok.
      return canonFile.toString(ret);
    }
    if (canonStat == null && fileStat == null) {
      ; // No check needed
    } else if (canonStat == null) {
      if (!fileStat.equals(canonStat)) {
        BaseConfig.getInstance().handleToctou("Files.getCanonicalPath()",
            "file stat " + fileStat + " does not match canonical file stat " + canonStat);
      }
    } else if (!canonStat.equals(fileStat)) {
      BaseConfig.getInstance().handleToctou("Files.getCanonicalPath()",
          "file stat " + fileStat + " does not match canonical file stat " + canonStat);
    }
    return canonFile.toString(ret);
  }

  /**
   * @param file If this file's file.canonicalFile().toString() is tracked, return it. If this
   *        file's file.canonicalFile().toString() is not tracked, taint or trust the canonical
   *        String based on file.toString() trust/taint, create a File from the canonical String,
   *        and return that File.
   * @return A File whose toString() is tracked and whose trust/untrust is derived from input file.
   * @throws IOException
   * @throws SecurityException
   * @throws NoSuchMethodException
   */
  @InstrumentationMethod
  public static final File getCanonicalFile(File file, Ret ret)
      throws IOException, NoSuchMethodException, SecurityException {
    Notify.enter_check("getCanonicalFile", file.getPath());

    File canonFile = canonicalize(file.getAbsoluteFile(ret), ret);
    FileStat canonStat, fileStat;
    try {
      canonStat = timeOfCheck(canonFile);
    } catch (IOException e) {
      canonStat = null;
    }
    try {
      fileStat = timeOfCheck(file);
    } catch (IOException e) {
      // It's possible that the canonical path exists, but the original path does not resolve at
      // some point (i.e. nonexisting_dir/..). So, let's presume this is ok.
      return canonFile;
    }
    if (canonStat == null && fileStat == null) {
      ; // No check needed.
    } else if (canonStat == null) {
      if (!fileStat.equals(canonStat)) {
        BaseConfig.getInstance().handleToctou("Files.getCanonicalPath()",
            "file stat " + fileStat + " does not match canonical file stat " + canonStat);
      }
    } else if (!canonStat.equals(fileStat)) {
      BaseConfig.getInstance().handleToctou("Files.getCanonicalPath()",
          "file stat " + fileStat + " does not match canonical file stat " + canonStat);
    }
    return canonFile;
  }

  // Return an array of trusted leaf names.
  @InstrumentationMethod
  public static final String[] list(File file, Ret ret) {
    final String[] leafRay = file.list(ret);
    if (leafRay == null)
      return null;

    for (String leaf : leafRay) { // Mark each path as trusted.
      TaintUtils.trust(leaf);
    }

    return leafRay;
  }

  @InstrumentationMethod
  public static final String[] list(File file, FilenameFilter filter, Ret ret) {
    final String[] leafRay = file.list(filter, ret);
    if (leafRay == null)
      return null;

    for (String leaf : leafRay) { // Mark each path as trusted.
      TaintUtils.trust(leaf);
    }

    return leafRay;
  }

  private static final File[] listFilesCommon(final File[] fileRay, final File file) {
    File[] retRay = null;

    if (fileRay != null) {
      File[] newFileRay = new File[fileRay.length];
      final String fileStr = file.toString();
      final int fileStrLen = fileStr.length();

      for (int i = 0; i < fileRay.length; i++) {
        final File f = fileRay[i];
        String childPath = f.toString();

        int trustStart;
        if (childPath.startsWith(fileStr)) {
          childPath = TaintUtils.copyTaint(fileStr, childPath, fileStr.length());
          trustStart = fileStrLen;
        } else {
          trustStart = 0;
        }

        // The child leaf part of childPath will be trusted.
        TaintUtils.mark(childPath, TaintValues.TRUSTED, trustStart, childPath.length() - 1);

        newFileRay[i] = new File(childPath);
      }

      retRay = newFileRay;
    }

    return retRay;
  }

  @InstrumentationMethod
  public static final File[] listFiles(File file, Ret ret) {
    final File[] fileRay = file.listFiles(ret);
    final File[] retRay = listFilesCommon(fileRay, file);
    return retRay;
  }

  @InstrumentationMethod
  public static final File[] listFiles(File file, FileFilter filter, Ret ret) {
    final File[] fileRay = file.listFiles(filter, ret);
    final File[] retRay = listFilesCommon(fileRay, file);
    return retRay;
  }

  @InstrumentationMethod
  public static final File[] listFiles(File file, FilenameFilter filter, Ret ret) {
    final File[] fileRay = file.listFiles(filter, ret);
    final File[] retRay = listFilesCommon(fileRay, file);
    return retRay;
  }
}
