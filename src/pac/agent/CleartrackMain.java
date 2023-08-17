package pac.agent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Date;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import pac.agent.hierarchy.ClassHierarchy;
import pac.com.google.common.io.NullOutputStream;
import pac.org.apache.commons.cli.CommandLine;
import pac.org.apache.commons.cli.CommandLineParser;
import pac.org.apache.commons.cli.HelpFormatter;
import pac.org.apache.commons.cli.Options;
import pac.org.apache.commons.cli.ParseException;
import pac.org.apache.commons.cli.PosixParser;
import pac.org.objectweb.asm.ClassVisitor;
import pac.org.objectweb.asm.ClassWriter;
import pac.org.objectweb.asm.tree.ClassNode;
import pac.util.Ansi;
import pac.util.SSBuild;
import pac.util.SSVersion;

public class CleartrackMain {
  public static final ClassHierarchy hierarchy = ClassHierarchy.getInstance();

  public static final String SEPARATOR = System.getProperty("path.separator");
  public static final String JAVA_HOME = System.getProperty("java.home");
  public static final String DEFAULT_EXT_DIRS = System.getProperty("java.ext.dirs");
  public static final String DEFAULT_OUTPUT_DIR = "output-inst";

  // Entry point for offline transformation.
  public static void main(String[] args) throws ParseException, IOException, URISyntaxException {
    // Build all command-line options.
    CommandLineParser parser = new PosixParser();
    Options options = getCommandLineOptions();
    CommandLine cmdLine = parser.parse(options, args);

    // Parse command line flags.
    boolean agentOnly = cmdLine.hasOption("a") || cmdLine.hasOption("agent");
    boolean quiet = cmdLine.hasOption("q") || cmdLine.hasOption("quiet");
    boolean overwrite = cmdLine.hasOption("ow") || cmdLine.hasOption("overwrite");
    boolean confinementOff = cmdLine.hasOption("c") || cmdLine.hasOption("confinement-off");
    boolean prefix = cmdLine.hasOption("p") || cmdLine.hasOption("prefix");
    boolean version = cmdLine.hasOption("V") || cmdLine.hasOption("version");

    int threads = 1;
    if (cmdLine.hasOption("t") || cmdLine.hasOption("threads")) {
      threads = Integer.parseInt(cmdLine.getOptionValue("t"));
    }

    // Set output options.
    boolean hideProgress = quiet || System.console() == null || threads > 1;
    boolean verbose = !quiet && (cmdLine.hasOption("v") || cmdLine.hasOption("verbose"));
    boolean editMode = cmdLine.hasOption("i") || cmdLine.hasOption("interactive");

    // Check command-line options that don't actually run the instrumenter.
    if (cmdLine.hasOption("h") || cmdLine.hasOption("help")) {
      HelpFormatter format = new HelpFormatter();
      format.printHelp("cleartrack [OPTION]... [FILE]...", options);
      System.exit(0);
    } else if (version || verbose) {
      System.out.printf("Cleartrack version %s (built by %s on %s)%n", SSVersion.revision,
          SSBuild.build_user, SSBuild.build_time);
      if (version)
        System.exit(0);
    }

    // Do not run with confinement if we are generating prefixed libraries.
    if (prefix)
      confinementOff = true;

    int flags = 0;
    flags |= prefix ? CleartrackInstrumenter.PREFIX : 0;
    flags |= confinementOff ? 0 : CleartrackInstrumenter.CONFINEMENT;
    flags |= overwrite ? CleartrackInstrumenter.OVERWRITE : 0;

    // Set the output directory (or leave the default).
    String outputDir;
    if (cmdLine.hasOption("o") || cmdLine.hasOption("output")) {
      outputDir = cmdLine.getOptionValue("o");
    } else {
      outputDir = DEFAULT_OUTPUT_DIR;
    }

    File extDirs[] = getExtensionDirs(cmdLine);
    File jdkDir = new File(JAVA_HOME, "/lib");
    File outputDirFile = new File(outputDir);

    // Print command line options that were set:
    if (verbose) {
      Ansi answerAnsi = new Ansi(Ansi.Attribute.BRIGHT, Ansi.Color.BLUE, null);
      System.out.printf("Overwrite bytecode and jars? %s\n", answerAnsi.colorize("" + overwrite));
      boolean noExtDirs =
          extDirs == null || extDirs.length == 0 || Arrays.toString(extDirs).equals("[]");
      System.out.printf("Extension directories set? %s\n", noExtDirs ? answerAnsi.colorize("false")
          : answerAnsi.colorize("true " + Arrays.toString(extDirs)));
      System.out.printf("Instrumentation output directory is: %s\n",
          answerAnsi.colorize(outputDirFile.getAbsolutePath()));
    }

    // Make output directory:
    if (!outputDirFile.exists() && !outputDirFile.mkdir()) {
      System.err.println("ERROR: unable to create output directory: " + outputDirFile);
      System.exit(1);
    }

    if (quiet) {
      // Be quiet from here on out. Note that we are essentially redirecting the output to /dev/null
      // here, but maybe we shouldn't be entirely quiet!
      System.setOut(new PrintStream(new NullOutputStream()));
    }

    MainInstrumenter instrumenter =
        new MainInstrumenter(outputDirFile, flags, threads, hideProgress, editMode);

    // Process the files specified from the command line.
    String[] files = cmdLine.getArgs();
    if (prefix) {
      instrumenter.instrumentFiles(jdkDir, extDirs, files, outputDirFile, true);
      instrumenter.instrumentFiles(files, outputDirFile, false);
    } else {
      // Pre-process and instrument the JDK and files specified at the command line.
      instrumenter.instrumentFiles(jdkDir, extDirs, files, outputDirFile, true);
      hierarchy.postProcess(outputDirFile, "pac.inst.taint");
      if (!agentOnly)
        instrumenter.instrumentFiles(jdkDir, extDirs, files, outputDirFile, false);

      // Serialize trusted properties goal file.
      // The ~/.cleartrack directory must exist at this point.
      File cleartrackDir = new File(System.getProperty("user.home"), ".cleartrack");
      File propertyFile = new File(cleartrackDir, "properties.goal");
      try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(propertyFile))) {
        out.writeObject(System.getProperties());
      } catch (IOException e) {
        Ansi.error("unable to serialize java properties due to exception: %s", null, e);
        e.printStackTrace();
        System.exit(1);
      }

      //#if bootstrap==false
      if (files.length > 0) {
        // We only need to generate the configuration class file if there is an application to
        // instrument.
        ClassNode configuration = pac.config.ConfigFile.getInstance().toClassNode(!confinementOff);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        configuration.accept(cw);
        byte[] classBytes = cw.toByteArray();
        CleartrackInstrumenter.verifyClass(outputDirFile, classBytes, "RuntimeConfig", false);
        JarOutputStream configOut =
            new JarOutputStream(new FileOutputStream(new File(outputDirFile, "ss-config.jar")));
        JarEntry configEntry = new JarEntry("pac/config/runtime/RuntimeConfig.class");
        configEntry.setTime(new Date().getTime());
        configOut.putNextEntry(configEntry);
        configOut.write(classBytes);
        JarEntry hierarchyEntry = new JarEntry("hierarchy.obj");
        hierarchyEntry.setTime(new Date().getTime());
        configOut.putNextEntry(hierarchyEntry);
        configOut.write(ClassHierarchy.saveInstance());
        configOut.close();
      }
      //#endif
    }

    System.out.println(
        "********************************************************************************");
    System.out.printf("Verify Errors:       jdk = %7d     application = %7d\n",
        CleartrackInstrumenter.verifyErrorsJdk.intValue(),
        CleartrackInstrumenter.verifyErrorsApp.intValue());
    System.out.printf("Split Methods:       jdk = %7d     application = %7d\n",
        CleartrackInstrumenter.splitMethodsJdk.intValue(),
        CleartrackInstrumenter.splitMethodsApp.intValue());
    System.out.println(
        "********************************************************************************");
  }

  private static Options getCommandLineOptions() {
    Options options = new Options();
    options.addOption("v", "verbose", false, "run in verbose mode");
    options.addOption("V", "version", false, "print out version information");
    options.addOption("a", "agent", false,
        "only preprocess classes for the purpose of running as an agent");
    options.addOption("t", "threads", true,
        "instrument allocating a specified number of threads in the thread pool");
    options.addOption("q", "quiet", false, "run in silent mode");
    options.addOption("c", "confinement-off", false, "instrument without confinement");
    options.addOption("p", "prefix", false,
        "prefix packages (intended for libraries required by cleartrack)");
    options.addOption("h", "help", false, null);
    options.addOption("o", "output", true, "output directory");
    options.addOption("exts", "extension-dirs", true, "extension directories");
    options.addOption("ow", "overwrite", false,
        "overwrite application bytecode and jars with instrumented versions");
    options.addOption("i", "interactive", false,
        "interactively edit bytecode when encountering a verify error");

    // set the argument names for options with arguments
    options.getOption("t").setArgName("NUMBER");
    options.getOption("o").setArgName("DIRECTORY");
    options.getOption("exts").setArgName("DIRECTORY");

    return options;
  }

  private static File[] getExtensionDirs(CommandLine cmdLine) {
    String extPath;
    if (cmdLine.hasOption("exts") || cmdLine.hasOption("extension-dirs")) {
      extPath = cmdLine.getOptionValue("exts");
    } else {
      extPath = DEFAULT_EXT_DIRS;
    }
    if (extPath == null)
      return null;
    String extPaths[] = extPath.split(SEPARATOR);
    File extDirs[] = new File[extPaths.length];
    for (int i = 0; i < extPaths.length; i++) {
      extDirs[i] = new File(extPaths[i]);
    }
    return extDirs;
  }

  /**
   * Custom instrumenter used to define how classes are to be instrumented.
   * 
   * @author jeikenberry
   */
  private static class MainInstrumenter extends CleartrackInstrumenter {
    private boolean editMode;

    public MainInstrumenter(File outputDir, int flags, int threads, boolean hideProgress,
        boolean editMode) {
      super(outputDir, flags, threads, hideProgress);
      this.editMode = editMode;
    }

    @Override
    protected ClassVisitor instrumentClass(ClassVisitor visitor, String checksum, boolean inJdk) {
      if (isFlagSet(PREFIX)) {
        return new CleartrackPackagePrefixAdapter(visitor);
      } else {
        if (isFlagSet(CONFINEMENT)) {
          visitor = new CleartrackInstrumentedClassAdapter(visitor, new String[] {"CONFINEMENT"});
          visitor = new CleartrackTaintClassAdapter(new CleartrackInstrumentationAdapter(visitor),
              editMode, false);
        } else {
          visitor = new CleartrackInstrumentedClassAdapter(visitor, new String[0]);
          visitor = new CleartrackBaseInstrumentationAdapter(visitor, editMode);
        }
        return visitor;
      }
    }

    @Override
    protected ClassNode getInstrumentedSubclass(ClassVisitor visitor) {
      if (isFlagSet(CONFINEMENT)) {
        return ((CleartrackTaintClassAdapter) visitor).getInstrumentedSubclass();
      }
      return null;
    }

    @Override
    protected ClassVisitor instrumentSubclass(ClassVisitor visitor) {
      // This will only get called if getInstrumentedSubclass() returns a non-null value.
      return new CleartrackInstrumentationAdapter(visitor);
    }
  }
}
