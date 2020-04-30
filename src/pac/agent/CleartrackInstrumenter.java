package pac.agent;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.Deflater;

import pac.agent.hierarchy.ClassHierarchy;
import pac.org.objectweb.asm.ClassReader;
import pac.org.objectweb.asm.ClassVisitor;
import pac.org.objectweb.asm.ClassWriter;
import pac.org.objectweb.asm.Opcodes;
import pac.org.objectweb.asm.Type;
import pac.org.objectweb.asm.commons.splitlarge.SplitMethodWriterDelegate;
import pac.org.objectweb.asm.tree.ClassNode;
import pac.util.Ansi;
import pac.util.FixedClassWriter;

/**
 * General class for transforming class, JAR, WAR, etc... into their instrumenting
 * version given a chain of class visitors.
 * 
 * @author jeikenberry
 */
public abstract class CleartrackInstrumenter {
    public static final ClassHierarchy hierarchy = ClassHierarchy.getInstance();

    // instrumentation flags
    public static final int CONFINEMENT = 1;
    public static final int PREFIX = 2;
    public static final int OVERWRITE = 4;

    private static final String JAVA_HOME = System.getProperty("java.home");
    private static final String USER_HOME = System.getProperty("user.home");

    private static final Set<String> knownInvalidJars = arrayToSet(new String[] { "icu4j-2.6.1.jar" });
    private static final Set<String> skippedJars = new TreeSet<String>();

    private Map<File, File> renameOriginals, renameTransformed;
    protected final Set<File> jdkJars, extJars;
    private final File outputDir, outputDirJdk;
    private final int flags, threads;
    private final boolean hideProgress;

    protected static AtomicInteger splitMethodsJdk, splitMethodsApp, verifyErrorsJdk, verifyErrorsApp;

    static {
        // initialize all atomic counters
        splitMethodsJdk = new AtomicInteger(0);
        splitMethodsApp = new AtomicInteger(0);
        verifyErrorsJdk = new AtomicInteger(0);
        verifyErrorsApp = new AtomicInteger(0);

        ClassLoader cl = CleartrackInstrumenter.class.getClassLoader();
        boolean asAgent = cl == null; // If cl is null then we must be running
                                      // with dynamic agent

        try {
            String jarSkipFile = "META-INF/jars.skip";
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(asAgent ? ClassLoader.getSystemResourceAsStream(jarSkipFile)
                            : cl.getResourceAsStream(jarSkipFile)));
            String method;
            while ((method = in.readLine()) != null) {
                String jarName = method.trim();
                if (jarName.startsWith("#") || jarName.equals(""))
                    continue;
                skippedJars.add(jarName);
            }
            in.close();
        } catch (IOException e) {
            // this file should always be in the cleartrack.jar file, but
            // for the sake of being thorough...
            Ansi.error("unable to locate META-INF/jars.skip from cleartrack.jar", null);
            System.exit(1);
        }
    }

    private static <E> Set<E> arrayToSet(E array[]) {
        Set<E> set = new HashSet<E>();
        for (E e : array) {
            set.add(e);
        }
        return set;
    }

    public CleartrackInstrumenter(File outputDir, int flags, int threads, boolean hideProgress) {
        this.outputDir = outputDir;
        this.flags = flags;
        this.threads = threads;
        this.hideProgress = hideProgress;
        jdkJars = new HashSet<File>();
        extJars = new HashSet<File>();
        renameOriginals = new TreeMap<File, File>();
        renameTransformed = new TreeMap<File, File>();

        // Write out instrumented JDK to a global location under ~/.cleartrack
        // unless we are running in test and evaluation mode.
        outputDirJdk = new File(USER_HOME, ".cleartrack");
        if (!outputDirJdk.exists()) {
            if (!outputDirJdk.mkdir()) {
                Ansi.error("unable to make directory: %s", null, outputDirJdk);
                System.exit(1);
            }
        }
    }

    public boolean isFlagSet(int flag) {
        return (flags & flag) == flag;
    }

    private static void copyFile(File sourceFile, File destFile) throws IOException {
        File parent = destFile.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        try (FileInputStream source = new FileInputStream(sourceFile);
                FileOutputStream destination = new FileOutputStream(destFile)) {
            destination.getChannel().transferFrom(source.getChannel(), 0, source.getChannel().size());
        }
    }

    /**
     * This method only exists because of the insanity that is the MITRE test harness.
     */
    public void renameFiles() {
        renameFiles(renameOriginals);
        renameFiles(renameTransformed);
    }

    private static void renameFiles(Map<File, File> renaming) {
        for (File from : renaming.keySet()) {
            File to = renaming.get(from);
            System.out.println("moving " + from + " to " + to);
            // Delete first, just to be safe.
            if (to.exists()) {
                to.delete();
            }
            // Do renaming as copy then delete, in case the renaming is across filesystems.
            try {
                copyFile(from, to);
            } catch (IOException e) {
                e.printStackTrace();
            }
            from.delete();
        }
    }

    private boolean isKnownInvalidJar(File file) {
        return knownInvalidJars.contains(file.getName());
    }

    private boolean isCompatibilityJar(String filename) {
        // first check to see if there is a direct match
        if (skippedJars.contains(filename))
            return true;

        // now check for patterns with wildcards...
        for (String skippedJar : skippedJars) {
            int idx = 0;

            // split on wildcards
            String[] substrs = skippedJar.split("\\*");
            for (int i = 0; i < substrs.length; i++) {

                // if the last character of the pattern is a wildcard
                // then we have a match, otherwise, there are more
                // characters to match.  So, continue...
                if (substrs[i].equals("")) {
                    if (i == substrs.length - 1)
                        return true;
                    continue;
                }

                idx = filename.indexOf(substrs[i], idx);
                if (idx < 0) // we didn't find a substring match
                    break;
                idx += substrs[i].length();
            }

            // we found a match iff the last idx equals the length
            // of the filename.
            if (idx == filename.length())
                return true;
        }

        return false;
    }

    public void instrumentFiles(String[] classFiles, File outputDir, boolean preProcess) {
        instrumentFiles(null, null, classFiles, outputDir, preProcess);
    }

    public void instrumentFiles(File jdkDir, String[] classFiles, File outputDir, boolean preProcess) {
        instrumentFiles(jdkDir, null, classFiles, outputDir, preProcess);
    }

    public void instrumentFiles(File jdkDir, File extDirs[], String[] classFiles, File outputDir,
                                final boolean preProcess) {
        if (preProcess) {
            System.out.println("********************************************************************************");
            System.out.println("PREPROCESSING CLASS FILES");
            System.out.println("********************************************************************************");
        } else {
            System.out.println("********************************************************************************");
            System.out.println("TRANSFORMING CLASS FILES");
            System.out.println("********************************************************************************");
        }

        ExecutorService executor = threads <= 1 ? null : Executors.newFixedThreadPool(8);

        if (jdkDir != null) {
            // analyze JDK...
            for (final File jar : jdkDir.listFiles()) {
                if (jar.isDirectory() || !jar.getName().endsWith(".jar"))
                    continue;
                instrumentFile(executor, jar, true, preProcess);
            }
        }

        if (extDirs != null) {
            for (File extDir : extDirs) {
                if (!extDir.exists() || !extDir.isDirectory()) {
                    continue;
                }
                final boolean inJdk = extDir.getPath().startsWith(JAVA_HOME);
                for (final File jar : extDir.listFiles()) {
                    if (jar.isDirectory() || !jar.getName().endsWith(".jar"))
                        continue;
                    instrumentFile(executor, jar, inJdk, true, preProcess);
                }
            }
        }

        // analyze user class files...
        for (final String filepath : classFiles) {
            instrumentFile(executor, new File(filepath), false, preProcess);
        }

        if (threads > 1) {
            executor.shutdown();
            while (!executor.isTerminated()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    private void instrumentFile(ExecutorService executor, File file, boolean inJdk, boolean preProcess) {
        instrumentFile(executor, file, inJdk, false, preProcess);
    }

    private void instrumentFile(ExecutorService executor, final File file, final boolean inJdk, final boolean isExt,
                                final boolean preProcess) {
        if (!file.exists()) {
            Ansi.warn("file %s does not exist.", null, file);
            return;
        }

        if (file.isDirectory()) {
            if (file.getName().equals(".") || file.getName().equals(".."))
                return;
            File outFile = new File(outputDir, file.getPath());
            outFile.mkdir();
            File children[] = file.listFiles();
            if (children == null) {
                Ansi.warn("got null for listing of contents of directory %s", null, file);
                return;
            }
            for (File child : children) {
                instrumentFile(executor, child, inJdk, preProcess);
            }
        } else {
            File outputDir = inJdk ? outputDirJdk : this.outputDir;

            // assume it's java code (i.e. jar, class, etc...)
            final String fileName = file.getName();
            final FileType fileType = FileType.getFileType(fileName);

            switch (fileType) {
            case JAR:
            case WAR:
            case SAR:
                if (threads > 1) {
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            instrumentJarFile(file, inJdk, isExt, isCompatibilityJar(fileName), preProcess, fileType);
                        }
                    });
                } else {
                    instrumentJarFile(file, inJdk, isExt, isCompatibilityJar(fileName), preProcess, fileType);
                }
                break;
            case CLASS:
                if (threads > 1) {
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            instrumentClassFile(file, inJdk, preProcess);
                        }
                    });
                } else {
                    instrumentClassFile(file, inJdk, preProcess);
                }
                break;
            default:
                if (!preProcess) {
                    try {
                        copyFile(file, new File(outputDir, file.getPath()));
                    } catch (IOException e) {
                        Ansi.warn("unable to copy file to output directory: %s: %s", null, file, e);
                    }
                }
                break;
            }
        }
    }

    private void instrumentClassFile(final File file, boolean inJdk, boolean preProcess) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(file);
            File outFile = new File(outputDir, file.getPath());
            outFile.getParentFile().mkdirs();
            fos = new FileOutputStream(outFile);
            instrumentClass(null, fis, fos, false, false, preProcess);
            if (isFlagSet(OVERWRITE) && !inJdk) {
                renameOriginals.put(file, new File(file.getPath() + "-saved"));
                renameTransformed.put(outFile, file);
            }
            if (!preProcess)
                Ansi.trans("transformed %s\n", true, file.getPath());
        } catch (IOException e) {
            Ansi.warn("unable to instrument classfile %s: %s", null, file.getPath(), e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void instrumentJarFile(File file, boolean inJdk, boolean isExt, boolean isCompatibility, boolean preProcess,
                                   FileType fileType) {
        File outputDir = inJdk ? outputDirJdk : this.outputDir;

        File outJar;
        if (isExt && inJdk) {
            File j = new File("jre/ext", file.getName());
            extJars.add(j);
            outJar = new File(outputDir, j.getPath());
            if (outJar.exists()) {
                return;
            }
        } else if (isExt) {
            File j = new File("ext", file.getName());
            extJars.add(j);
            outJar = new File(outputDir, j.getPath());
            if (outJar.exists()) {
                return;
            }
        } else if (inJdk) {
            File j = new File("jre", file.getName());
            outJar = new File(outputDir, j.getPath());
            if (!preProcess) {
                jdkJars.add(j);
                if (outJar.exists())
                    return;
            }
        } else {
            outJar = new File(outputDir, file.getPath());
        }
        outJar.getParentFile().mkdirs();

        if (isKnownInvalidJar(file)) {
            try {
                copyFile(file, outJar);
                Ansi.warn("using uninstremented version of known-invalid jar: %s", null, file.getName());
            } catch (IOException e) {
                Ansi.warn("could not copy file %s to %s: %s", null, file, outJar, e);
            }
            return;
        }

        // create jar object from file
        JarFile jar = null;
        FileInputStream fis = null;
        OutputStream out = null;
        try {

            jar = new JarFile(file);

            // count the number of entries in this jar...
            float total = 0;
            for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements(); total++, entries
                    .nextElement())
                ;

            if (!preProcess)
                out = new FileOutputStream(outJar);
            fis = new FileInputStream(file);
            instrumentJarFile(file, fis, out, total, inJdk, isCompatibility, preProcess, fileType);
            if (isFlagSet(OVERWRITE) && !inJdk) {
                renameOriginals.put(file, new File(file.getPath() + "-saved"));
                renameTransformed.put(outJar, file);
            }
        } catch (IOException e) {
            Ansi.warn("unable to open jarfile: %s: %s", null, file, e);
        } finally {
            if (!preProcess && !hideProgress) {
                Ansi.trans("\rtransformed %s: %s [%%100] \n", true, fileType, file);
            }
            try {
                if (jar != null) {
                    jar.close();
                }
            } catch (IOException e) {
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void instrumentJarFile(File file, InputStream in, OutputStream out, float total, boolean inJdk,
                                   boolean isCompatibility, boolean preProcess, FileType fileType)
            throws IOException {
        JarInputStream jarIn = new JarInputStream(in);
        JarOutputStream jarOut = null;
        try {
            if (!preProcess) {
                Manifest manifest = jarIn.getManifest();
                if (manifest != null)
                    jarOut = new JarOutputStream(out, manifest);
                else
                    jarOut = new JarOutputStream(out);
                jarOut.setLevel(Deflater.BEST_SPEED);

                if (total >= 0 && !hideProgress) {
                    Ansi.trans("transforming %s: %s [%%0]", false, fileType, file);
                }
            }

            Set<String> entries = new HashSet<String>();
            float processed = 1;
            for (JarEntry jarEntry = jarIn.getNextJarEntry(); jarEntry != null; processed++, jarEntry = jarIn
                    .getNextJarEntry()) {
                String entryName = jarEntry.getName();
                // Do not add digital signatures or jar index lists since
                // the order of classes may have changed and therefore
                // contain invalid indices.
                if (entries.contains(entryName) || entryName.matches("META-INF/[^.]+\\.(SF|RSA)")
                        || entryName.equals("META-INF/INDEX.LIST"))
                    continue;
                entries.add(entryName);

                try {
                    JarEntry newJarEntry = new JarEntry(entryName);
                    newJarEntry.setTime(jarEntry.getTime());
                    if (!preProcess)
                        jarOut.putNextEntry(newJarEntry);

                    if (jarEntry.isDirectory())
                        continue;

                    FileType entryType = FileType.getFileType(entryName);
                    switch (entryType) {
                    case CLASS:
                        byte[] subClassBytes = instrumentClass(file.getName(), jarIn, jarOut, inJdk, isCompatibility,
                                                               preProcess);
                        if (subClassBytes != null) {
                            newJarEntry = new JarEntry(ClassHierarchy.getDangerousSublassName(entryName));
                            newJarEntry.setTime(jarEntry.getTime());
                            if (!preProcess) {
                                jarOut.putNextEntry(newJarEntry);
                                jarOut.write(subClassBytes);
                            }
                        }
                        break;
                    case JAR:
                        // treat as a compatibility jar if either the main jar or nest jar
                        // is a compatibility jar (this fixes cases where a compatibility
                        // jar is buried in a war file).
                        boolean compatibleJar = isCompatibility || isCompatibilityJar(new File(entryName).getName());
                        if (preProcess) {
                            instrumentJarFile(file, jarIn, null, -1, inJdk, compatibleJar, preProcess, entryType);
                        } else {
                            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                            instrumentJarFile(file, jarIn, byteOut, -1, inJdk, compatibleJar, preProcess, entryType);
                            jarOut.write(byteOut.toByteArray());
                        }
                        break;
                    default:
                        if (!preProcess) {
                            int read;
                            byte[] buf = new byte[512];
                            while ((read = jarIn.read(buf)) > 0)
                                jarOut.write(buf, 0, read);
                        }
                        break;
                    }

                    if (!preProcess && total >= 0 && !hideProgress) {
                        Ansi.trans("\rtransforming %s: %s [%%%d]", false, fileType, file,
                                   (int) (100 * (processed + 1) / total));
                    }
                } catch (RuntimeException re) {
                    Ansi.error("unable to instrument class in %s due to exception:\n\t%s", null, jarEntry, re);
                    re.printStackTrace();
                    System.exit(1);
                }
            }

            try {
                if (!preProcess)
                    jarOut.close();
            } catch (IOException e) {
            }
        } finally {
            if (total >= 0)
                jarIn.close();
            if (!preProcess && hideProgress)
                Ansi.trans("transformed %s: %s\n", true, fileType, file);
        }
    }

    private static String parseChecksum(byte[] checksum) {
        StringBuilder buf = new StringBuilder(32);
        BigInteger bigInt = new BigInteger(1, checksum);
        String hexValue = bigInt.toString(16);

        // Pad with zeros if less than 32 characters...
        for (int i = hexValue.length(); i < 32; i++) {
            buf.append('0');
        }
        buf.append(hexValue);
        return buf.toString();
    }

    // FIXME note that className may not be fully qualified if it did not come from a jar.
    private byte[] instrumentClass(final String jarName, InputStream is, OutputStream out, final boolean inJdk,
                                   final boolean isCompatibility, boolean preProcess)
            throws IOException {
        File outputDir = inJdk ? outputDirJdk : this.outputDir;

        byte[] subClassBytes = null;

        // Pass the input stream to a digest input stream first, so that
        // we may acquire the MD5 checksum from the bytes read.
        DigestInputStream md5Stream = null;
        try {
            md5Stream = new DigestInputStream(is, MessageDigest.getInstance("MD5"));
        } catch (NoSuchAlgorithmException e1) {
            Ansi.error("unable to compute md5 checksum due to: %s", null, e1.toString());
            System.exit(1);
        }

        // Remove JSR/RET instructions prior to instrumenting, so that we maybe:
        //   a) recompute the stack frames successfully, and
        //   b) are able to split methods that are too large.
        // We are presuming that classes that are analyzed are > JDK 1.7 to
        // ensure that our byte offsets will not unexpectedly change.
        ClassReader cr = new ClassReader(md5Stream);
        final String checksum = parseChecksum(md5Stream.getMessageDigest().digest());
        cr = new ClassReader(FixedClassWriter.fixJSRInlining(cr));

        if (preProcess) {
            cr.accept(new ClassNode(Opcodes.ASM5) {
                @Override
                public void visitEnd() {
                    hierarchy.process(this, inJdk, isCompatibility, checksum, jarName);
                }
            }, 0);
        } else {
            // Instrument the class and split any methods that are too large...
            SplitMethodWriterDelegate splitter = new SplitMethodWriterDelegate();
            CleartrackClassWriter cw = new CleartrackClassWriter(ClassWriter.COMPUTE_FRAMES, splitter);
            ClassVisitor visitor = instrumentClass(cw, checksum, inJdk);
            cr.accept(visitor, 0);

            // Write out the class file and verify
            byte[] outBytes = cw.toByteArray();
            verifyClass(outputDir, outBytes, cr.getClassName(), inJdk);
            out.write(outBytes);

            if (inJdk)
                splitMethodsJdk.getAndAdd(splitter.getNumberOfSplitMethods());
            else
                splitMethodsApp.getAndAdd(splitter.getNumberOfSplitMethods());

            ClassNode classNode = getInstrumentedSubclass(visitor);
            if (classNode != null) {
                splitter = new SplitMethodWriterDelegate();
                cw = new CleartrackClassWriter(ClassWriter.COMPUTE_FRAMES, splitter);
                visitor = instrumentSubclass(cw);
                classNode.accept(visitor);

                subClassBytes = cw.toByteArray();
                verifyClass(outputDir, subClassBytes, classNode.name, inJdk);

                if (inJdk)
                    splitMethodsJdk.getAndAdd(splitter.getNumberOfSplitMethods());
                else
                    splitMethodsApp.getAndAdd(splitter.getNumberOfSplitMethods());
            }
        }

        return subClassBytes;
    }

    /**
     * This method provides the basic implementation for making transformations
     * to a class that will result in the fully-instrumented class.  Note, if your
     * instrumenter can create subclasses, you will also need to implement
     * getInstrumentedSubclass() and instrumentSubclass().
     * 
     * @param visitor ClassVisitor of the original unmodified class.
     * @param checksum String of the checksum from the class file.
     * @param inJdk true iff this class came from a JDK jar.
     * @return ClassVisitor of the instrumented class.
     */
    protected abstract ClassVisitor instrumentClass(ClassVisitor visitor, String checksum, boolean inJdk);

    /**
     * Override this method if your instrumentation happens to create
     * a dangerous subclass.
     * 
     * @param visitor ClassVisitor of where to obtain the subclass.
     * @return ClassNode of the subclass.
     */
    protected ClassNode getInstrumentedSubclass(ClassVisitor visitor) {
        return null;
    }

    /**
     * Override this method if your instrumentation happens to create
     * a dangerous subclass, and you wish to make transformations to
     * the class prior to writing the bytes.
     * 
     * @param visitor ClassVisitor of the subclass.
     * @return ClassVisitor of the transformed subclass.
     */
    protected ClassVisitor instrumentSubclass(ClassVisitor visitor) {
        throw new RuntimeException("CleartrackInstrumenter.instrumentSubclass() needs"
                + " to be implemented for cases where there is an instrumented subclass.");
    }

    /**
     * Verifies a class file (encoded as a byte array) and outputs all errors to
     * a file located within the directory ".errors" under the directory
     * specified by outputDir. If the ".errors" directory does not exists, it
     * will be created.
     *
     * @param outputDir
     *            File of the output directory
     * @param outBytes
     *            byte[] of the class file
     * @param className
     *            String of internal class name
     */
    public static void verifyClass(File outputDir, byte[] classBytes, String className, boolean inJdk) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        try {
            CleartrackCheckClassAdapter.verify(new ClassReader(classBytes), null, false, pw);
        } catch (Exception e) {
            e.printStackTrace(pw);
        }

        String err = sw.toString();
        if (err.length() != 0) {
            if (inJdk)
                verifyErrorsJdk.incrementAndGet();
            else
                verifyErrorsApp.incrementAndGet();
            Ansi.error("verify error in class %s", null, className);
            File errorDir = new File(outputDir, ".errors");
            if (!errorDir.exists()) {
                if (!errorDir.mkdir()) {
                    Ansi.error("unable to make directory: %s", null, errorDir);
                    System.exit(1);
                }
            }

            File errorFile = new File(errorDir, classToErrorFileName(className));
            try (PrintWriter errorOut = new PrintWriter(errorFile)) {
                errorOut.println(err);
            } catch (FileNotFoundException e) {
                Ansi.error("unable to write file: %s", null, errorFile);
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    /**
     * Converts a class name (in internal form) to an filename where verify
     * errors will be written to.
     *
     * @param className
     *            String of the form some/class/file.class
     * @return String of the form some_class_file.errors
     */
    private static String classToErrorFileName(String className) {
        String filename = className;
        if (className.endsWith(".class")) {
            filename = filename.substring(0, filename.length() - ".class".length());
        }
        return filename.replace('/', '_') + ".errors";
    }

    public static String filterNonErrors(String err) {
        BufferedReader r = new BufferedReader(new StringReader(err));

        StringBuilder buf = new StringBuilder();
        boolean skipError = false;
        boolean firstLine = true;
        String line;

        try {
            while ((line = r.readLine()) != null) {
                if (firstLine) {
                    if (line.contains("java.lang.ClassNotFoundException:")
                            || line.contains("expected Ljava/lang/Number;, but found Ljava/lang/CleartrackByte;")
                            || line.contains("expected Ljava/lang/Number;, but found Ljava/lang/CleartrackShort;")
                            || line.contains("expected Ljava/lang/Number;, but found Ljava/lang/CleartrackInteger;")
                            || line.contains("expected Ljava/lang/Number;, but found Ljava/lang/CleartrackFloat;")
                            || line.contains("expected Ljava/lang/Number;, but found Ljava/lang/CleartrackLong;")
                            || line.contains("expected Ljava/lang/Number;, but found Ljava/lang/CleartrackDouble;")
                            || line.contains("expected Ljava/lang/Number;, but found Ljava/lang/Object;")) {
                        skipError = true;
                    } else {
                        int start = line.indexOf(": expected ");
                        if (start >= 0) {
                            start += ": expected ".length();
                            int end = line.indexOf(',', start);
                            if (end > start) {
                                String type = line.substring(start, end);
                                Type typeObj = Type.getType(type);
                                // dangerous types will not correctly match the type it's expecting.
                                // we need to filter these out.
                                if (hierarchy.isDangerousClass(typeObj)) {
                                    String subtype = ClassHierarchy.getDangerousSublassName(type);
                                    String matchStr = String.format(": expected %s, but found %s", type, subtype);
                                    skipError = line.contains(matchStr);
                                    if (!skipError) {
                                        // it's possible that both a Integer and CleartrackInteger are
                                        // used in a stack merge. merging in this case will result in an
                                        // Object, instead of Integer
                                        matchStr = String.format(": expected %s, but found Ljava/lang/Object;", type);
                                        skipError = line.contains(matchStr);
                                    }
                                }
                            }
                        }
                    }
                }
                if (!skipError) {
                    buf.append(line + "\n");
                }
                firstLine = false;
                if (line.trim().equals("")) {
                    firstLine = true;
                    skipError = false;
                }
            }
        } catch (IOException e) {
            // should never get this in a string reader
            e.printStackTrace();
        }
        return buf.toString();
    }

    public Set<File> getJdkJarFiles() {
        return jdkJars;
    }

    public Set<File> getExtJarFiles() {
        return extJars;
    }

    private enum FileType {
        JAR, WAR, SAR, CLASS, OTHER;

        public static FileType getFileType(String fileName) {
            if (fileName.endsWith(".class"))
                return CLASS;
            if (fileName.endsWith(".jar"))
                return JAR;
            if (fileName.endsWith(".war"))
                return WAR;
            if (fileName.endsWith(".sar"))
                return SAR;
            return OTHER;
        }
    }
}
