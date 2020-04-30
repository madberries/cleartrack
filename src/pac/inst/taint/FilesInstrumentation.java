package pac.inst.taint;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.Set;

import pac.config.BaseConfig;
import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationLocation;
import pac.inst.InstrumentationMethod;
import pac.inst.InvocationType;
import pac.util.Ret;
import pac.util.FileStat;

@InstrumentationClass("java/nio/file/Files")
public class FilesInstrumentation {

    private static final boolean shouldFollowLinks(LinkOption[] options) {
        if (options == null)
            return true;
        for (int i = 0; i < options.length; i++) {
            if (options[i] == LinkOption.NOFOLLOW_LINKS)
                return false;
        }
        return true;
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC, instrumentationLocation = InstrumentationLocation.APP)
    public static FileTime getLastModifiedTime(Path path, LinkOption[] options, Ret ret) throws IOException {
        boolean followLinks = shouldFollowLinks(options);
        long microsecs = FileInstrumentation.timeOfCheck(path.toFile(), !followLinks, false).lastModifiedMicro();
        return FileTime.from(microsecs, TimeUnit.MICROSECONDS);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC, instrumentationLocation = InstrumentationLocation.APP)
    public static Set<PosixFilePermission> getPosixFilePermissions(Path path, LinkOption[] options, Ret ret)
            throws IOException {
        boolean followLinks = shouldFollowLinks(options);
        return FileInstrumentation.timeOfCheck(path.toFile(), !followLinks, false).getPermissions();
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC, instrumentationLocation = InstrumentationLocation.APP)
    public static final boolean isSymbolicLink(Path path, Ret ret) throws IOException {
        FileStat fstat = FileInstrumentation.timeOfCheck(path.toFile(), true, false);
        return fstat.isSymbolic();
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC, instrumentationLocation = InstrumentationLocation.APP)
    public static final void delete(Path path, Ret ret) throws IOException {
        String key = path.toFile().getPath();
        Files.delete(path);
        FileInstrumentation.attrMap.put(key, null);
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC, instrumentationLocation = InstrumentationLocation.APP)
    public static final boolean deleteIfExists(Path path, Ret ret) throws IOException {
        String key = path.toFile().getPath();
        boolean result = Files.deleteIfExists(path);
        FileInstrumentation.attrMap.put(key, null);
        return result;
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC, instrumentationLocation = InstrumentationLocation.APP)
    public static final boolean exists(Path path, LinkOption[] options, Ret ret) {
        boolean followLinks = shouldFollowLinks(options);
        try {
            return FileInstrumentation.timeOfCheck(path.toFile(), !followLinks, true) != null;
        } catch (IOException e) {
            return false;
        }
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC, instrumentationLocation = InstrumentationLocation.APP)
    public static final boolean notExists(Path path, LinkOption[] options, Ret ret) {
        boolean followLinks = shouldFollowLinks(options);
        try {
            return FileInstrumentation.timeOfCheck(path.toFile(), !followLinks, true) == null;
        } catch (IOException e) {
            return false;
        }
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC, instrumentationLocation = InstrumentationLocation.APP)
    public static final boolean isReadable(Path path, Ret ret) {
        try {
            return FileInstrumentation.timeOfCheck(path.toFile(), false, false).isReadable();
        } catch (IOException e) {
            return false;
        }
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC, instrumentationLocation = InstrumentationLocation.APP)
    public static final boolean isWritable(Path path, Ret ret) {
        try {
            return FileInstrumentation.timeOfCheck(path.toFile(), false, false).isWritable();
        } catch (IOException e) {
            return false;
        }
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC, instrumentationLocation = InstrumentationLocation.APP)
    public static final boolean isExecutable(Path path, Ret ret) {
        try {
            return FileInstrumentation.timeOfCheck(path.toFile(), false, false).isExecutable();
        } catch (IOException e) {
            return false;
        }
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC, instrumentationLocation = InstrumentationLocation.APP)
    public static final boolean isDirectory(Path path, LinkOption[] options, Ret ret) {
        boolean followLinks = shouldFollowLinks(options);
        try {
            return FileInstrumentation.timeOfCheck(path.toFile(), !followLinks, false).isDirectory();
        } catch (IOException e) {
            return false;
        }
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC, instrumentationLocation = InstrumentationLocation.APP)
    public static final boolean isRegularFile(Path path, LinkOption[] options, Ret ret) {
        boolean followLinks = shouldFollowLinks(options);
        try {
            return FileInstrumentation.timeOfCheck(path.toFile(), !followLinks, false).isRegular();
        } catch (IOException e) {
            return false;
        }
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC, instrumentationLocation = InstrumentationLocation.APP)
    public static final boolean isSameFile(Path path1, Path path2, Ret ret)
            throws IOException, NoSuchMethodException, SecurityException {
        Boolean expected = Files.isSameFile(path1, path2);
        FileStat fstat1 = FileInstrumentation.timeOfCheck(path1.toFile());
        FileStat fstat2 = FileInstrumentation.timeOfCheck(path2.toFile());
        boolean actual = fstat1.equals(fstat2);
        if (actual != expected) {
            BaseConfig.getInstance().handleToctou("Files.isSameFile()",
                                                  "inconsistent results when determining if " + path1 + " == " + path2);
        }
        return expected;
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC, instrumentationLocation = InstrumentationLocation.APP)
    public static final Path move(Path source, Path target, CopyOption[] options, Ret ret) throws IOException {
        Path result = Files.move(source, target, options);
        FileInstrumentation.attrMap.put(source.toFile().getPath(), null);
        FileInstrumentation.timeOfCheck(target.toFile(), false, true);
        return result;
    }

    private static final void checkValue(String method, String attrName, Object value, Object statValue)
            throws NoSuchMethodException, SecurityException {
        if (!value.equals(statValue)) {
            BaseConfig.getInstance()
                    .handleToctou(method,
                                  "file attribute mismatch on '" + attrName + "': " + value + " != " + statValue);
        }
    }

    @SuppressWarnings("unchecked")
    private static final void checkAttribute(String method, String key, Object value, FileStat stat)
            throws NoSuchMethodException, SecurityException {
        if (value instanceof FileTime) {
            long millis = ((FileTime) value).toMillis();
            if (key.equals("ctime") || key.equals("creationTime")) {
                checkValue(method, key, millis, stat.creationTimePrecise());
            } else if (key.equals("lastModifiedTime")) {
                checkValue(method, key, millis, stat.lastModifiedPrecise());
            } else if (key.equals("lastAccessTime")) {
                checkValue(method, key, millis, stat.lastAccessedPrecise());
            }
        } else if (value instanceof Set) {
            if (key.equals("permissions")) {
                if (!stat.matchesPermissions((Set<PosixFilePermission>) value)) {
                    BaseConfig.getInstance()
                            .handleToctou(method, "file attribute mismatch on 'permissions': " + value + " != " + stat);
                }
            }
        } else if (value instanceof Boolean) {
            if (key.equals("isDirectory")) {
                checkValue(method, key, value, stat.isDirectory());
            } else if (key.equals("isSymbolicLink")) {
                checkValue(method, key, value, stat.isSymbolic());
            } else if (key.equals("isRegularFile")) {
                checkValue(method, key, value, stat.isRegular());
            } else if (key.equals("isOther")) {
                checkValue(method, key, value, stat.isOther());
            }
        } else if (value instanceof Integer) {
            if (key.equals("mode")) {
                checkValue(method, key, value, stat.getMode());
            } else if (key.equals("uid")) {
                checkValue(method, key, value, stat.getUid());
            } else if (key.equals("gid")) {
                checkValue(method, key, value, stat.getGid());
            } else if (key.equals("nlink")) {
                checkValue(method, key, value, stat.getNlink());
            }
        } else if (value instanceof Long) {
            if (key.equals("dev")) {
                checkValue(method, key, value, stat.getDev());
            } else if (key.equals("rdev")) {
                checkValue(method, key, value, stat.getRdev());
            } else if (key.equals("ino")) {
                checkValue(method, key, value, stat.getInode());
            } else if (key.equals("size")) {
                checkValue(method, key, value, stat.getSize());
            }
        } else if (value instanceof UserPrincipal) {
            // TODO not sure how to obtain the uid from this
        } else if (value instanceof GroupPrincipal) {
            // TODO not sure how to obtain the gid from this
        } else if (key.equals("fileKey")) {
            String statResult = String.format("(dev=%s,ino=%s)", Long.toHexString(stat.getDev()), stat.getInode());
            checkValue(method, key, value.toString(), statResult);
        }
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC, instrumentationLocation = InstrumentationLocation.APP)
    public static final Object getAttribute(Path path, String attribute, LinkOption[] options, Ret ret)
            throws IOException, NoSuchMethodException, SecurityException {
        Object value = Files.getAttribute(path, attribute, options);
        FileStat stat = FileInstrumentation.timeOfCheck(path.toFile());
        int idx = attribute.indexOf(':');
        String name;
        if (idx > 0) {
            name = attribute.substring(idx + 1);
        } else {
            name = attribute;
        }
        checkAttribute("Files.getAttribute()", name, value, stat);
        return value;
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC, instrumentationLocation = InstrumentationLocation.APP)
    public static final Map<String, Object> readAttributes(Path path, String attributes, LinkOption[] options, Ret ret)
            throws IOException, NoSuchMethodException, SecurityException {
        Map<String, Object> attrMap = Files.readAttributes(path, attributes, options, ret);
        FileStat stat = FileInstrumentation.timeOfCheck(path.toFile());
        String method = "Files.readAttributes()";
        for (Entry<String, Object> entry : attrMap.entrySet()) {
            checkAttribute(method, entry.getKey(), entry.getValue(), stat);
        }
        return attrMap;
    }

    @InstrumentationMethod(invocationType = InvocationType.STATIC, instrumentationLocation = InstrumentationLocation.APP)
    public static <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption[] options,
                                                                   Ret ret)
            throws IOException, NoSuchMethodException, SecurityException {
        A result = Files.readAttributes(path, type, options, ret);
        FileStat stat = FileInstrumentation.timeOfCheck(path.toFile());
        String method = "Files.readAttributes()";
        checkValue(method, "isDirectory", result.isDirectory(), stat.isDirectory());
        checkValue(method, "isRegularFile", result.isRegularFile(), stat.isRegular());
        checkValue(method, "isSymbolicLink", result.isSymbolicLink(), stat.isSymbolic());
        checkValue(method, "isOther", result.isOther(), stat.isOther());
        if (result instanceof PosixFileAttributes) {
            PosixFileAttributes fileAttr = (PosixFileAttributes) result;
            checkValue(method, "creationTime", fileAttr.creationTime().toMillis(), stat.creationTimePrecise());
            checkValue(method, "lastAccessTime", fileAttr.lastAccessTime().toMillis(), stat.lastAccessedPrecise());
            checkValue(method, "lastModifiedTime", fileAttr.lastModifiedTime().toMillis(), stat.lastModifiedPrecise());
            if (!stat.matchesPermissions(fileAttr.permissions())) {
                BaseConfig.getInstance()
                        .handleToctou(method, "file attribute mismatch on 'permissions': " + fileAttr + " != " + stat);
            }
            // TODO need to check uid/guid... how?
            checkValue(method, "size", fileAttr.size(), stat.getSize());
            String statResult = String.format("(dev=%s,ino=%s)", Long.toHexString(stat.getDev()), stat.getInode());
            checkValue(method, "fileKey", fileAttr.fileKey().toString(), statResult);
        }
        return result;
    }

}
