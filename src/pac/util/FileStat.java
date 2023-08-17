package pac.util;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

/**
 * Native wrapper for fstat. This class can also be used to acquire the mimetype of a file (natively
 * through libmagic).
 * 
 * @author jeikenberry
 */
public class FileStat {

  public enum StatType {
    STAT, LSTAT, FSTAT
  }

  // Mode constants
  public static final int S_IFMT = 0170000; /* type of file */
  public static final int S_IFIFO = 0010000; /* named pipe (fifo) */
  public static final int S_IFCHR = 0020000; /* character special */
  public static final int S_IFDIR = 0040000; /* directory */
  public static final int S_IFBLK = 0060000; /* block special */
  public static final int S_IFREG = 0100000; /* regular */
  public static final int S_IFLNK = 0120000; /* symbolic link */
  public static final int S_IFSOCK = 0140000; /* socket */
  public static final int S_IFWHT = 0160000; /* whiteout */
  public static final int S_ISUID = 0004000; /* set user id on execution */
  public static final int S_ISGID = 0002000; /* set group id on execution */
  public static final int S_ISVTX = 0001000; /* save swapped text even after use */
  public static final int S_IRUSR = 0000400; /* read permission, owner */
  public static final int S_IWUSR = 0000200; /* write permission, owner */
  public static final int S_IXUSR = 0000100; /* execute/search permission, owner */
  public static final int S_IRGRP = 0000040; /* read permission, group */
  public static final int S_IWGRP = 0000020; /* write permission, group */
  public static final int S_IXGRP = 0000010; /* execute/search permission, group */
  public static final int S_IROTH = 0000004; /* read permission, others */
  public static final int S_IWOTH = 0000002; /* write permission, others */
  public static final int S_IXOTH = 0000001; /* execute/search permission, others */

  private long dev, rdev, inode, size, atime, atime_nsec, mtime, mtime_nsec, ctime, ctime_nsec;
  private int mode, uid, gid, nlink;

  /**
   * The name of the check/use associated with this file stat.
   */
  private String check;

  /**
   * The absolute path of the checked file.
   */
  private String checkedPath;

  /**
   * The canonical path of the checked file.
   */
  private String checkedRealpath;

  /**
   * The type of stat representing this object (i.e. stat, fstat, lstat).
   */
  private StatType type;

  static {
    System.loadLibrary("fstat");
  }

  private FileStat() {
    // Stat objects should only be created natively.
  }

  /**
   * Finds the check/use method call in the stack trace.
   * 
   * @return String of the method call.
   */
  private static String determineCheck() {
    String check = null;
    StackTraceElement[] eles = Thread.currentThread().getStackTrace();
    for (int i = 3; i < eles.length; i++) {
      String className = eles[i].getClassName();
      if (!className.startsWith("pac.inst.taint."))
        break;
      check = className + "." + eles[i].getMethodName() + "()";
    }
    return check;
  }

  /**
   * Determines the canonicalized path of a file. This native routine is necessary since Java
   * apparently caches canonicalized file paths by default. They do eventually expire, however.
   * 
   * @param filepath String of the file path to canonicalize.
   * @return String canonicalized path of filepath.
   * @throws IOException
   */
  private static native String realpath0(String filepath) throws IOException;

  /**
   * Obtains file stat information about a given file descriptor, through a native call to fstat.
   * 
   * @param fd FileDescriptor of the file to stat.
   * @return FileStat object of the given file descriptor.
   * @throws IOException if fstat returned an error code.
   */
  private static native FileStat fstat0(FileDescriptor fd) throws IOException;

  /**
   * Obtains file stat information about a given file descriptor, through a native call to fstat.
   * 
   * @param fd FileDescriptor of the file to stat.
   * @return FileStat object of the given file descriptor.
   * @throws IOException if fstat returned an error code.
   */
  public static FileStat fstat(FileDescriptor fd) throws IOException {
    FileStat fstat = fstat0(fd);
    fstat.check = determineCheck();
    fstat.type = StatType.FSTAT;
    return fstat;
  }

  /**
   * Obtains file lstat information about a given file descriptor, through a native call to lstat.
   * 
   * @param filename String of the path to the file.
   * @return FileStat object of the given file descriptor.
   * @throws IOException if stat returned an error code.
   */
  private static native FileStat lstat0(String filename) throws IOException;

  /**
   * Obtains file lstat information about a given file descriptor, through a native call to lstat.
   * 
   * @param file File to stat.
   * @return FileStat object of the given file descriptor.
   * @throws IOException if stat returned an error code.
   */
  public static FileStat lstat(File file) throws IOException {
    // if (file == null || !file.exists())
    // return null;
    FileStat fstat = lstat0(file.getAbsolutePath());
    fstat.check = determineCheck();
    fstat.checkedPath = file.getAbsolutePath();
    fstat.checkedRealpath = realpath0(fstat.checkedPath);
    fstat.type = StatType.LSTAT;
    return fstat;
  }

  /**
   * Obtains file stat information about a given file descriptor, through a native call to stat.
   * 
   * @param filename String of the path to the file.
   * @return FileStat object of the given file descriptor.
   * @throws IOException if stat returned an error code.
   */
  private static native FileStat stat0(String filename) throws IOException;

  /**
   * Obtains file stat information about a given file descriptor, through a native call to stat.
   * 
   * @param file File to stat.
   * @return FileStat object of the given file descriptor.
   * @throws IOException if stat returned an error code.
   */
  public static FileStat stat(File file) throws IOException {
    // if (file == null || !file.exists())
    //   return null;
    FileStat fstat = stat0(file.getAbsolutePath());
    fstat.check = determineCheck();
    fstat.checkedPath = file.getAbsolutePath();
    fstat.checkedRealpath = realpath0(fstat.checkedPath);
    fstat.type = StatType.STAT;
    return fstat;
  }

  private static native String getMimetype0(String filename) throws IOException;

  /**
   * Obtains the mimetype of the supplied file, through a native call to libmagic.
   * 
   * @param file File to query.
   * @return String of {@code file}'s mimetype.
   * @throws IOException if there was an error in loading the magic database.
   */
  public static String getMimetype(File file) throws IOException {
    return getMimetype0(file.getAbsolutePath());
  }

  /**
   * toString() method for time-of-check.
   */
  public String toString() {
    return "time-of-check: in method " + check + "\n        filename=" + checkedPath
        + "\n        realpath=" + checkedRealpath + "\n        inode: " + inode + ", dev: " + dev
        + ", ctime: " + ctime + "." + ctime_nsec + ", type: " + type;
  }

  /**
   * toString() method for time-of-use.
   */
  public String toString(File file) {
    String absPath = file.getAbsolutePath();
    try {
      return "time-of-use: in method " + check + "\n        filename=" + absPath
          + "\n        realpath=" + realpath0(absPath) + "\n        inode: " + inode + ", dev: "
          + dev + ", ctime: " + ctime + "." + ctime_nsec + ", type: " + type;
    } catch (IOException e) {
      return "time-of-use: in method " + check + "\n        filename=" + absPath
          + "\n        realpath=N/A" + "\n        inode: " + inode + ", dev: " + dev + ", ctime: "
          + ctime + "." + ctime_nsec + ",type: " + type;
    }
  }

  public boolean isSymbolic() {
    return (mode & S_IFLNK) == S_IFLNK;
  }

  public boolean isRegular() {
    return (mode & S_IFREG) == S_IFREG;
  }

  public boolean isDirectory() {
    return (mode & S_IFDIR) == S_IFDIR;
  }

  public boolean isReadable() {
    return (mode & S_IRUSR) == S_IRUSR;
  }

  public boolean isWritable() {
    return (mode & S_IWUSR) == S_IWUSR;
  }

  public boolean isExecutable() {
    return (mode & S_IXUSR) == S_IXUSR;
  }

  public boolean isOther() {
    return !isSymbolic() && !isRegular() && !isDirectory();
  }

  /**
   * Determines whether the set of permissions matches the mode of this FileStat object by or-ing
   * the bits of each value in this set and comparing it with the mode of this FileStat object
   * (masked with 0777).
   * 
   * @param permissions Set&lt;PosixFilePermission&gt;
   * @return true iff these values match.
   */
  public boolean matchesPermissions(Set<PosixFilePermission> permissions) {
    int expecting = mode & 0777;
    int actual = 0;
    for (PosixFilePermission perm : permissions) {
      switch (perm) {
        case OWNER_READ:
          actual |= S_IRUSR;
          break;
        case OWNER_WRITE:
          actual |= S_IWUSR;
          break;
        case OWNER_EXECUTE:
          actual |= S_IXUSR;
          break;
        case GROUP_READ:
          actual |= S_IRGRP;
          break;
        case GROUP_WRITE:
          actual |= S_IWGRP;
          break;
        case GROUP_EXECUTE:
          actual |= S_IXGRP;
          break;
        case OTHERS_READ:
          actual |= S_IROTH;
          break;
        case OTHERS_WRITE:
          actual |= S_IWOTH;
          break;
        case OTHERS_EXECUTE:
          actual |= S_IXOTH;
          break;
      }
    }
    return expecting == actual;
  }

  /**
   * Constructs a set of PosixFilePermissions from the mode of this FileStat object.
   * 
   * @return Set&lt;PosixFilePermission&gt;
   */
  public Set<PosixFilePermission> getPermissions() {
    Set<PosixFilePermission> perms = EnumSet.noneOf(PosixFilePermission.class);
    int permVal = mode & 0777;
    if ((permVal & S_IRUSR) == S_IRUSR)
      perms.add(PosixFilePermission.OWNER_READ);
    if ((permVal & S_IWUSR) == S_IWUSR)
      perms.add(PosixFilePermission.OWNER_WRITE);
    if ((permVal & S_IXUSR) == S_IXUSR)
      perms.add(PosixFilePermission.OWNER_EXECUTE);
    if ((permVal & S_IRGRP) == S_IRGRP)
      perms.add(PosixFilePermission.GROUP_READ);
    if ((permVal & S_IWGRP) == S_IWGRP)
      perms.add(PosixFilePermission.GROUP_WRITE);
    if ((permVal & S_IXGRP) == S_IXGRP)
      perms.add(PosixFilePermission.GROUP_EXECUTE);
    if ((permVal & S_IROTH) == S_IROTH)
      perms.add(PosixFilePermission.OTHERS_READ);
    if ((permVal & S_IWOTH) == S_IWOTH)
      perms.add(PosixFilePermission.OTHERS_WRITE);
    if ((permVal & S_IXOTH) == S_IXOTH)
      perms.add(PosixFilePermission.OTHERS_EXECUTE);
    return perms;
  }

  public int getMode() {
    return mode;
  }

  public int getUid() {
    return uid;
  }

  public int getGid() {
    return gid;
  }

  public int getNlink() {
    return nlink;
  }

  public long getDev() {
    return dev;
  }

  public long getRdev() {
    return rdev;
  }

  public long getInode() {
    return inode;
  }

  public long getSize() {
    return size;
  }

  // The stat is in seconds, and we need to put this in milliseconds.
  public long lastModified() {
    return mtime * 1000L;
  }

  public long lastAccessed() {
    return atime * 1000L;
  }

  public long creationTime() {
    return ctime * 1000L;
  }

  public long lastModifiedPrecise() {
    return mtime * 1000L + mtime_nsec / 1000000L;
  }

  public long lastAccessedPrecise() {
    return atime * 1000L + atime_nsec / 1000000L;
  }

  public long creationTimePrecise() {
    return ctime * 1000L + ctime_nsec / 1000000L;
  }

  public long lastModifiedMicro() {
    return mtime * 1000000L + mtime_nsec / 1000L;
  }

  public long lastAccessedMicro() {
    return atime * 1000000L + atime_nsec / 1000L;
  }

  public long creationTimeMicro() {
    return ctime * 1000000L + ctime_nsec / 1000L;
  }

  @Override
  public boolean equals(Object obj) {
    // System.out.println("does (" + obj + ") = (" + this + ")?");
    if (obj instanceof FileStat) {
      FileStat fs = (FileStat) obj;
      return this.dev == fs.dev && this.inode == fs.inode && this.ctime == fs.ctime
          && this.ctime_nsec == fs.ctime_nsec;
    }
    return false;
  }

}
