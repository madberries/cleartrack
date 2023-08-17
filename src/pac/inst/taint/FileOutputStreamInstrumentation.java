package pac.inst.taint;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.Set;

import pac.config.BaseConfig;
import pac.config.Notify;
import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.inst.InvocationType;
import pac.util.Ret;
import pac.util.FileStat;
import pac.util.TaintUtils;
import pac.util.ThreadMonitor;
import pac.wrap.ByteArrayTaint;

@InstrumentationClass("java/io/FileOutputStream")
public final class FileOutputStreamInstrumentation extends OutputStreamInstrumentation {

  // CONSTRUCTORS

  @SuppressWarnings("resource")
  @InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR)
  public static final FileOutputStream init(File file, Ret ret)
      throws IOException, NoSuchMethodException, SecurityException {
    // We do not need to worry about a resource leak, presuming that the application properly closes
    // this FileOutputStream. This is because both streams share the same file descriptor.
    RandomAccessFile raf = new RandomAccessFile(file, "rw");
    FileDescriptor fd = raf.getFD();
    try {
      FileInstrumentation.timeOfUse("FileOutputStream.<init>(Ljava/io/File;)", file, fd);
    } catch (Exception e) {
      // Be sure to close the random access file we opened, since we are throwing an exception.
      raf.close();
      throw e;
    }
    raf.setLength(0);
    FileOutputStream out = new FileOutputStream(fd);
    if (TaintUtils.isTracked(file.getPath())) {
      fd.ss_file = file;
    }
    ThreadMonitor.INSTANCE.addCloseable(out);
    return out;
  }

  @SuppressWarnings("resource")
  @InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR)
  public static final FileOutputStream init(File file, boolean append, int append_t, Ret ret)
      throws IOException, NoSuchMethodException, SecurityException {
    final String methodDesc = "FileOutputStream.<init>(Ljava/io/File;Z)";
    if (append) {
      // We are appending, so we don't need to worry about overwriting the file.
      FileOutputStream out = new FileOutputStream(file, append);
      FileDescriptor fd = out.getFD();
      try {
        FileInstrumentation.timeOfUse(methodDesc, file, fd);
      } catch (Exception e) {
        // Be sure to close the random access file we opened, since we are throwing an exception.
        out.close();
        throw e;
      }
      if (TaintUtils.isTracked(file.getPath())) {
        fd.ss_file = file;
      }
      ThreadMonitor.INSTANCE.addCloseable(out);
      return out;
    } else {
      // We do not need to worry about a resource leak, presuming that the application properly
      // closes this FileOutputStream. This is because both streams share the same file descriptor.
      RandomAccessFile raf = new RandomAccessFile(file, "rw");
      FileDescriptor fd = raf.getFD();
      try {
        FileInstrumentation.timeOfUse(methodDesc, file, fd);
      } catch (Exception e) {
        // Be sure to close the random access file we opened, since we are throwing an exception.
        raf.close();
        throw e;
      }
      raf.setLength(0);
      FileOutputStream out = new FileOutputStream(fd);
      if (TaintUtils.isTracked(file.getPath())) {
        fd.ss_file = file;
      }
      ThreadMonitor.INSTANCE.addCloseable(out);
      return out;
    }
  }

  // @InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR)
  // public static final FileOutputStream init(FileDescriptor fdObj) throws FileNotFoundException {
  //   FileOutputStream out = new FileOutputStream(fdObj);
  //   File file = fileDescMap.get(fdObj);
  //   if (file != null && Notify.in_application() && !Notify.in_jdk ()) {
  //     FileTaintValues ftm = (FileTaintValues) FileTaintValues.getMetadata(out);
  //     ftm.setFile(file);
  //   }
  //   return out;
  // }

  @SuppressWarnings("resource")
  @InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR)
  public static final FileOutputStream init(String name, Ret ret)
      throws IOException, NoSuchMethodException, SecurityException {
    File file = name == null ? null : new File(name);
    String methodDesc = "java/io/FileOutputStream.<init>(Ljava/lang/String;)";

    // We do not need to worry about a resource leak, presuming that the application properly closes
    // this FileOutputStream. This is because both streams share the same file descriptor.
    RandomAccessFile raf = new RandomAccessFile(file, "rw");
    FileDescriptor fd = raf.getFD();
    try {
      FileInstrumentation.timeOfUse(methodDesc, file, fd);
    } catch (Exception e) {
      // Be sure to close the random access file we opened, since we are throwing an exception.
      raf.close();
      throw e;
    }
    raf.setLength(0);
    FileOutputStream out = new FileOutputStream(fd);
    if (TaintUtils.isTracked(file.getPath())) {
      fd.ss_file = file;
    }
    ThreadMonitor.INSTANCE.addCloseable(out);
    return out;
  }

  @SuppressWarnings("resource")
  @InstrumentationMethod(invocationType = InvocationType.CONSTRUCTOR)
  public static final FileOutputStream init(String name, boolean append, int append_t, Ret ret)
      throws IOException, NoSuchMethodException, SecurityException {
    File file = name == null ? null : new File(name);
    String methodDesc = "java/io/FileOutputStream.<init>(Ljava/lang/String;Z)";

    if (append) {
      // We are appending, so we don't need to worry about overwriting the file.
      FileOutputStream out = new FileOutputStream(file, append);
      FileDescriptor fd = out.getFD();
      try {
        FileInstrumentation.timeOfUse(methodDesc, file, fd);
      } catch (Exception e) {
        // Be sure to close the random access file we opened, since we are throwing an exception.
        out.close();
        throw e;
      }
      if (TaintUtils.isTracked(file.getPath())) {
        fd.ss_file = file;
      }
      ThreadMonitor.INSTANCE.addCloseable(out);
      return out;
    } else {
      // We do not need to worry about a resource leak, presuming that the application properly
      // closes this FileOutputStream. This is because both streams share the same file descriptor.
      RandomAccessFile raf = new RandomAccessFile(file, "rw");
      FileDescriptor fd = raf.getFD();
      try {
        FileInstrumentation.timeOfUse(methodDesc, file, fd);
      } catch (Exception e) {
        // Be sure to close the random access file we opened, since we are throwing an exception.
        raf.close();
        throw e;
      }
      raf.setLength(0);
      FileOutputStream out = new FileOutputStream(fd);
      if (TaintUtils.isTracked(file.getPath())) {
        fd.ss_file = file;
      }
      ThreadMonitor.INSTANCE.addCloseable(out);
      return out;
    }
  }

  // INSTANCE METHODS

  @InstrumentationMethod
  public static final void close(FileOutputStream out, Ret ret) throws IOException {
    FileDescriptor fd;
    try {
      fd = out.getFD();
      try {
        out.close();
        handleFileClose(fd.ss_file);
      } catch (IOException e1) {

      } finally {
        fd.ss_file = null;
        ThreadMonitor.INSTANCE.removeCloseable(out);
      }
    } catch (IOException e) {
      out.close();
    }
  }

  @InstrumentationMethod
  public static final void write(FileOutputStream fos, ByteArrayTaint b, Ret ret)
      throws IOException {
    if (RuntimeInstrumentation.isShellOutputStream(fos)) {
      String s = RuntimeInstrumentation.checkCommand(new String(b, ret), ret);
      fos.write((byte[]) s.getBytes(ret).value);
    } else {
      fos.write((byte[]) b.value);
    }
  }

  @InstrumentationMethod
  public static final void write(FileOutputStream fos, ByteArrayTaint b, int off, int off_t,
      int len, int len_t, Ret ret) throws IOException {
    if (RuntimeInstrumentation.isShellOutputStream(fos)) {
      String s =
          RuntimeInstrumentation.checkCommand(new String(b, off, off_t, len, len_t, ret), ret);
      fos.write((byte[]) s.getBytes(ret).value);
    } else {
      fos.write((byte[]) b.value, off, len);
    }
  }

  public static final void handleFileClose(File file) throws IOException {
    /*
     * FIXME: Not sure what we want to allow if it there are no read permissions, but let it through
     * for now. If there are no permissions to read this file, then we will get an error when
     * obtaining the content type as determined by the file command.
     */
    if (file == null || !file.exists() || !file.canRead() || TaintUtils.isTrusted(file.toString()))
      return;

    String extension = getFileExtension(file);
    String contentType = null;
    try {
      contentType = FileStat.getMimetype(file);
    } catch (IOException e) {
      Notify.error("Unable obtain MIME type from file: " + e + "\n");
    }

    // If we were unable to determine file type, for whatever reason, then there isn't much we can
    // do.
    //
    // TODO: Maybe we should assume the file is bad???
    if (contentType == null)
      return;

    Map<String, Set<String>> mimeTypes = BaseConfig.getInstance().getAllowableMimeTypes();
    if (mimeTypes != null) {
      Set<String> allTypesAllowed = BaseConfig.getInstance().getAllContentTypesAllowed();
      if (allTypesAllowed.contains(contentType)) {
        Set<String> contentTypes = mimeTypes.get(extension);
        if (contentTypes == null) {
          contentTypes = mimeTypes.get("*");
          if (!contentTypes.contains(contentType)) {
            BaseConfig.getInstance().handleUntrustedFileType(file, "file '" + file.getName()
                + "' has MIME type '" + contentType
                + "' when it is expecting some other content type based on the extension of the file.");
          }
        } else if (!contentTypes.contains(contentType)) {
          BaseConfig.getInstance().handleUntrustedFileType(file,
              "file '" + file.getName() + "' has MIME type '" + contentType
                  + "' when it is expecting one of: " + contentTypes);
        }
      } else {
        BaseConfig.getInstance().handleUntrustedFileType(file,
            "file '" + file.getName() + "' has MIME type '" + contentType
                + "' which is not allowed according to the configuration file.");
      }
    }
  }

  private static final String getFileExtension(File file) {
    String name = file.getName();
    int idx = name.lastIndexOf('.');
    if (idx < 0)
      return "";
    return name.substring(idx + 1).toLowerCase();
  }

}
