package pac.config;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;

import pac.inst.taint.ClassInstrumentation;
import pac.inst.taint.SocketInstrumentation.SocketType;
import pac.inst.taint.StringInstrumentation;
import pac.inst.taint.URLDecoderInstrumentation;
import pac.inst.taint.URLEncoderInstrumentation;
import pac.util.LangTester;
import pac.util.Ret;
import pac.util.TaintUtils;
import pac.util.TaintValues;
import pac.util.ThreadMonitor;
import pac.util.ThreadMonitor.Timestamp;
import pac.util.parsers.LangConstants;
import pac.wrap.ByteArrayTaint;

/**
 * This provides the configuration functionality which will interface with the application-specific
 * configuration that was generated as a RuntimeConfig object.
 * 
 * @author jeikenberry
 */
public class BaseConfig extends AbstractConfig {
  protected static BaseConfig INSTANCE;

  protected int[] trustedPorts;

  // The sanitized string at a socket resulting from the backstop confinement. (For testing purpose
  // only.)
  private String sanitizedSocketString;

  protected BaseConfig() {

  }

  /**
   * @return An instance of the application-specific configuration.
   */
  public static final BaseConfig getInstance() {
    if (INSTANCE == null) {
      try {
        INSTANCE = (BaseConfig) Class.forName("pac.config.runtime.RuntimeConfig").newInstance();
      } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
        System.err.println("unable to load the application's cleartrack configuration, "
            + "check to ensure that config.jar is in your bootclasspath");
        System.exit(1);
      }
    }
    return INSTANCE;
  }

  public String getLogFile() {
    return log_file;
  }

  public Chk[] getBeforeChks(final String descStr) {
    Desc d = methodDescMap.get(descStr);
    if (d == null)
      return null;
    return d.getBeforeChks();
  }

  public Chk getAfterChk(final String descStr) {
    Desc d = methodDescMap.get(descStr);
    if (d == null)
      return null;
    return d.getAfterChk();
  }

  // If envp is null search $PATH for the first path where leaf exe is located.
  // If envp is not null search envp for the first path where leaf.
  // Each entry in envp looks like "<key>=<value>".
  //
  // Param exe for charTaint exe is always unmapped.
  // Return first directory found where exe is located.
  // Null if exe is not found in any dir contained in PATH.
  // Note for charTaint, the returned path will be unmapped.
  private String findEnvPathContainingExe(final String exe, final String[] envp) {

    // Get an array of paths from either envp of System.getenv
    String[] paths = null;
    if (envp == null) {
      paths = System.getenv("PATH").split(":");
    } else {
      for (int i = 0; paths == null && i < envp.length; i++) {
        if (envp[i].startsWith("PATH=")) {
          final int index = envp[i].indexOf("=") + 1;
          paths = envp[i].substring(index).split(":");
        }
      }
    }

    // Determine which path in paths contains exe.
    String envPath = null;
    if (paths != null) {
      // Find first path in paths where exe exists.
      for (int i = 0; envPath == null && i < paths.length; i++) {
        String path_at_i = paths[i];
        final File file = new File(path_at_i, exe);
        if (file.exists()) {
          // Remove all trailing '/' chars from paths[i].
          envPath = path_at_i.replaceAll("/+$", "");
        }
      }

    }

    return envPath;
  }

  /**
   * If str is not trusted follow the action specified in config file trusted_exec_paths_action.
   *
   * @param str is the part of a $PATH variable that follows "=".
   */
  public void testPathComponentForTaint(final String str, final NotifyMsg notifyMsg)
      throws IOException {

    if (!TaintUtils.isTrusted(str)) {
      final String msg = "PATH environment variable contains untrusted path component(s).\n"
          + TaintUtils.createTaintDisplayLines(str) + "\n";
      // final String msg = "PATH environment variable (" +
      // str +
      // ") contains untrusted path component(s).\n";

      if (trusted_exec_paths_action == RunChecks.EXCEPTION_ACTION
          || trusted_exec_paths_action == RunChecks.TERMINATE_ACTION) {
        notifyMsg.append("Not executing \"" + notifyMsg.getMessage() + "\".\n");
        notifyMsg.append(msg);
        notifyMsg.prepareForExceptionOrTerminate(trusted_env_paths_exception_constructor,
            trusted_exec_paths_action);
        Notify.notifyAndRespond(notifyMsg); // No return. Either throw or terminate.
                                            // IOException
      } else {
        notifyMsg.setAction(trusted_exec_paths_action);
        notifyMsg.append(msg);
      }
    }
  }

  // Called from Runtime.exe and ProcessBuilder.start().
  //
  // If some part of $PATH is tainted, take the actions specified by config directive
  // "trusted_exec_paths_action".
  //
  // Return an array of environment variables containing only those env vars in envp whose value is
  // trusted.
  public String[] processEnvp(final String[] envp, final String desc, final String cmd)
      throws IOException {

    if (envp == null) {
      return null;
    }

    final NotifyMsg notifyMsg = new NotifyMsg(desc, cmd, 642); // CWE-642 is "External control of
                                                               // critical state data".
    notifyMsg.setAction(RunChecks.REMOVE_ACTION);
    Vector<String> trustedVec = new Vector<String>(); // Temp place to store trusted env vars.

    // Put all trusted env vars in trustedVec.
    for (int i = 0; i < envp.length; i++) {
      final String[] tokens = TaintUtils.split(envp[i], "="); // PatternSyntaxException
      if (tokens.length == 2) {
        final boolean trusted_env_var = TaintUtils.isTrusted(tokens[1]);
        if (trusted_env_var)
          trustedVec.add(envp[i]);
        else if (tokens[0].equals("PATH")) {
          testPathComponentForTaint(tokens[1], notifyMsg); // IOException
        }

      } else {
        Notify.log("odd environment variable %s in processEnvp\n", envp[i]);
      }
    }

    // Copy env vars in trustedVec to trustedArray.
    String[] trustedArray = new String[trustedVec.size()];
    for (int i = 0; i < trustedArray.length; i++) {
      trustedArray[i] = trustedVec.elementAt(i);
    }

    // Any String in envp that is not in trustedVec is not trusted.
    // Output these unstrusted env vars to log file.
    for (int i = 0; i < envp.length; i++) {
      final String str = envp[i];
      if (!trustedVec.contains(str)) {
        if (i == 0) {
          notifyMsg.append("The following environment variables were untrusted and removed:\n");
        }
        notifyMsg.append("  " + str + "\n");
      }
    }

    if (notifyMsg.isMessage()) {
      Notify.notifyAndRespond(notifyMsg); // IOException
    }

    return trustedArray;

  }

  /**
   * @param exe is an executable file. Do nothing if exe is not a leaf (ie contains '/' char).
   *
   *        If the leaf does not live on a path listed in trusted_exec_paths, take the action
   *        specified by trusted_exec_paths_action.
   */
  public void exeIsLeafInLegalDir(final String exe_param, final NotifyMsg notifyMsg,
      final String[] envp) throws IOException {

    String exe = exe_param;
    String envPath;
    if ((exe.indexOf("/") == -1) && ((envPath = findEnvPathContainingExe(exe, envp)) != null)) {
      String dir = null;
      if (".".equals(envPath)) {
        // If exe is in "." - If $PWD is tainted, throw exception.
        if (envp == null) {
          dir = System.getenv("PWD");
        } else {
          for (int i = 0; dir == null && i < envp.length; i++) {
            // Search for the PWD key
            if (envp[i].startsWith("PWD=")) {
              final int index = envp[i].indexOf("=") + 1;
              dir = envp[i].substring(index);
            }
          }
        }
      } else {
        dir = envPath;
      }

      // trustedExecPaths contains mapped Strings
      if (dir != null && !trustedExecPaths.contains(dir)) {
        final String msg = exe + " is located in an untrusted $PATH directory: " + envPath;

        if (trusted_exec_paths_action == RunChecks.EXCEPTION_ACTION
            || trusted_exec_paths_action == RunChecks.TERMINATE_ACTION) {

          notifyMsg.append("Not executing \"" + exe + "\".\n");
          notifyMsg.append(msg);
          notifyMsg.prepareForExceptionOrTerminate(trusted_env_paths_exception_constructor,
              trusted_exec_paths_action);
          Notify.notifyAndRespond(notifyMsg); // No return. Either throw or terminate.
                                              // IOException
        }

        notifyMsg.setAction(trusted_exec_paths_action);
        notifyMsg.append(msg);
      }
    }
  }

  // Shells all are mapped.
  // Leaf can be mapped/unmapped
  // Look for leaf in shells
  // ex. If leaf is "csh" and "/bin/csh" is shells at location n, return n.
  private int containsLeaf(final Vector<String> shells, final String leaf) {

    int retVal = -1;
    String slashLeaf = "/" + leaf;

    for (int i = 0; retVal == -1 && i < shells.size(); i++) {
      final String shell = shells.elementAt(i);
      if (shell.endsWith(slashLeaf)) {
        retVal = i;
      }
    }

    return retVal;
  }

  // Entry point
  // Return
  //   o cmd: If cmd matched a shell listed in config file.
  //   o A String from Config: if cmd is a leaf and it matches the leaf of some shell path found in
  //     the config file.
  //   o null: If cmd is not a shell.
  public String isShell(final String cmd) {
    String retVal = null;
    if (shells != null) {
      int shellsIndex;

      // cmd was listed as a shell in config file.
      if (shells.contains(cmd)) {
        retVal = cmd;

        // If cmd is "relPath/csh it no matter when shells contains. cmd is not a shell.
      } else {
        boolean containsSlash = cmd.contains("/");
        if (!cmd.startsWith("/") && containsSlash) {
          // If cmd is "csh" and shells contains "/bin/csh, convert "csh" to "/bin/csh".
        } else if (!containsSlash && ((shellsIndex = containsLeaf(shells, cmd)) != -1)) {
          retVal = shells.elementAt(shellsIndex);
          // If cmd is "/bin/csh" and shells contains "csh", then "/bin/csh qualifies as a shell.
        } else if (containsSlash) {
          final int index = cmd.lastIndexOf("/");
          final String cmdLeaf = cmd.substring(index + 1);
          if (shells.contains(cmdLeaf)) {
            retVal = cmd;
          }
        }
      }
    }

    return retVal;
  }

  Chk findChkFromName(final String chkName) {
    return configChksMap.get(chkName);
  }

  /**
   * Called when processing a parameter send to a java method. This method identifies the name of
   * the Chk indicated by the description sent by the caller. The caller uses the returned Chk to
   * process the parameter.
   *
   * @param description The api description from code. Must search methoDescs for the matching
   *        description. ex. "Runtime.exec(String, String[]name, String[]".
   * @param descIndex Index in Desc of the parameter being processed.
   * @return The name of the Chk indicated by the description.
   */
  public Chk findChkFromDescription(final String description, final int descIndex) {
    Desc d = getDescriptor(description);
    if (d != null) {
      if (descIndex < 0)
        return d.afterChk;
      if (d.paramChks == null)
        return null;
      return d.paramChks[descIndex];
    }
    return null;
  }

  /**
   * @return maximum number of iterations for unchecked loops.
   */
  public final int getLoopMaxIters() {
    return loopCheckMaxIters;
  }

  /**
   * @return maximum time for unchecked loops.
   */
  public final int getLoopMaxTime() {
    return loopCheckMaxTime;
  }

  /**
   * @return maximum number of iterations for unchecked loops.
   */
  public final int getLoopMaxItersDefinite() {
    return loopCheckMaxItersDef;
  }

  /**
   * @return maximum time for unchecked loops.
   */
  public final int getLoopMaxTimeDefinite() {
    return loopCheckMaxTimeDef;
  }

  public final String getOverflowReplacement() {
    return overflow_print_replace;
  }

  public final String getUnderflowReplacement() {
    return underflow_print_replace;
  }

  public final String getInfinityReplacement() {
    return infinity_print_replace;
  }

  public final int getMaxAllocSize() {
    return max_array_alloc_value;
  }

  public final float getMaxMemory() {
    return max_memory;
  }

  public final int getMaxFiles() {
    return max_files;
  }

  public void handleToctou(String method, String msg) {
    NotifyMsg notifyMsg = new NotifyMsg(method, method, 367);
    notifyMsg.setAction(toctou_action);
    notifyMsg.append(msg);
    if (toctou_action == RunChecks.EXCEPTION_ACTION
        || toctou_action == RunChecks.TERMINATE_ACTION) {
      notifyMsg.prepareForExceptionOrTerminate(toctou_exception_constructor, toctou_action);
    }
    Notify.notifyAndRespond(notifyMsg);
  }

  public int handleMaxAllocation(String method, String desc, int size) throws IOException {
    NotifyMsg notifyMsg = new NotifyMsg(method, desc, 789);
    notifyMsg.setAction(max_array_alloc_action);
    notifyMsg.append("Attemping to allocate an array of more than " + max_array_alloc_value
        + " elements on a tainted dimension(s), as set by the config file: " + size);

    if (max_array_alloc_action == RunChecks.EXCEPTION_ACTION
        || max_array_alloc_action == RunChecks.TERMINATE_ACTION) {
      notifyMsg.prepareForExceptionOrTerminate(max_array_alloc_exception, max_array_alloc_action);
    }
    Notify.notifyAndRespond(notifyMsg);
    if (max_array_alloc_action == RunChecks.LOG_ONLY_ACTION)
      return size;
    return max_array_alloc_value;
  }

  public void handleOverflow(String method, String desc) {
    final NotifyMsg notifyMsg = new NotifyMsg(method, desc);
    notifyMsg.setCweNumber(190);
    notifyMsg.setAction(overflow_action);
    notifyMsg.append("Attempting to perform a dangerous operation on an overflowed value.");

    if (overflow_action == RunChecks.EXCEPTION_ACTION
        || overflow_action == RunChecks.TERMINATE_ACTION) {
      notifyMsg.prepareForExceptionOrTerminate(overflow_exception, overflow_action);
    }
    Notify.notifyAndRespond(notifyMsg);
  }

  public void handleUnderflow(String method, String desc) {
    final NotifyMsg notifyMsg = new NotifyMsg(method, desc);
    notifyMsg.setCweNumber(191);
    notifyMsg.setAction(underflow_action);
    notifyMsg.append("Attempting to perform a dangerous operation on an underflowed value.");

    if (underflow_action == RunChecks.EXCEPTION_ACTION
        || underflow_action == RunChecks.TERMINATE_ACTION) {
      notifyMsg.prepareForExceptionOrTerminate(underflow_exception, underflow_action);
    }
    Notify.notifyAndRespond(notifyMsg);
  }

  public void handleInfinity(String method, String desc) {
    handleInfinity(method, desc, true);
  }

  public void handleInfinity(String method, String desc, boolean dangerousOp) {
    final NotifyMsg notifyMsg = new NotifyMsg(method, desc);
    notifyMsg.setCweNumber(369);

    if (dangerousOp) {
      notifyMsg.setAction(infinity_action);
      notifyMsg.append("Attempting to perform a dangerous operation on a divide-by-zero value.");
    } else {
      notifyMsg.setAction(RunChecks.REPLACE_ACTION);
      notifyMsg
          .append("Attempting to divide a primitive value by zero. Returning MAX_VALUE instead.");
    }

    if (dangerousOp && infinity_action == RunChecks.EXCEPTION_ACTION
        || infinity_action == RunChecks.TERMINATE_ACTION) {
      notifyMsg.prepareForExceptionOrTerminate(infinity_exception, infinity_action);
    }
    Notify.notifyAndRespond(notifyMsg);
  }

  /**
   * Called from a recursive method to check whether the relative stack size is bigger than an
   * amount specified by this config file.
   * 
   * @param count int of the current relative stack size for method.
   * @param method String name of the method.
   * @param ret Ret
   * @return true iff method has exceeded the maximum relative stack size.
   */
  public boolean handleRecursiveCheck(int count, String method, Ret ret) {
    if (count < recursiveMaxStackSize)
      return false;
    final NotifyMsg notifyMsg = new NotifyMsg(method, method);
    notifyMsg.setCweNumber(674);
    notifyMsg.setAction(recursiveMaxStackAction);
    notifyMsg.append("Uncontrolled recursion in method " + method
        + ".  The stack size for this method is bigger than " + recursiveMaxStackSize + ".");
    if (recursiveMaxStackAction == RunChecks.EXCEPTION_ACTION
        || recursiveMaxStackAction == RunChecks.TERMINATE_ACTION) {
      notifyMsg.prepareForExceptionOrTerminate(recursiveMaxStackException, recursiveMaxStackAction);
    }
    Notify.notifyAndRespond(notifyMsg);
    ret.breakFromRecursion = true;
    return true;
  }

  /**
   * Called from tainted (or possible tainted loops) to check whether we have gone over the maximum
   * number of iterations as specified by this configuration. If so, then take whatever action was
   * specified by this configuration.
   * 
   * @param int count the number of iterations so far.
   * @param boolean defTainted true iff the loop containing this check is definitely tainted.
   * @param boolean shouldThrow true iff the loop containing this check is unable to break (and
   *        therefore must throw an exception).
   * @param String method the method that this was called from (i.e. "SomeClass.foo()").
   * @throws IOException
   */
  public boolean handleLoopCheck(int count, boolean defTainted, boolean shouldThrow, String method,
      Timestamp timeObj) throws IOException {
    int maxCount, action;
    Constructor<?> constructor;
    String errorMsg = null;
    if (Thread.interrupted())
      ThreadMonitor.INSTANCE.handleInterrupts();
    if (defTainted) {
      maxCount = loopCheckMaxItersDef;
      if (timeObj != null && timeObj.isFinished()) {
        errorMsg = "Definitely tainted loop exceeded " + loopCheckMaxTimeDef / 1000
            + " seconds, as set by the config file.";
      } else {
        if (count < maxCount)
          return false;
        errorMsg = "Definitely tainted loop exceeded " + maxCount
            + " iterations, as set by the config file.";
      }
      action = loopCheckActionDef;
      constructor = loopCheckExceptionDef;
    } else {
      maxCount = loopCheckMaxIters;
      if (timeObj != null && timeObj.isFinished()) {
        errorMsg = "Possibly tainted loop exceeded " + loopCheckMaxTime / 1000
            + " seconds, as set by the config file.";
      } else {
        if (count < maxCount)
          return false;
        errorMsg = "Possibly tainted loop exceeded " + maxCount
            + " iterations, as set by the config file.";
      }
      action = loopCheckAction;
      constructor = loopCheckException;
    }

    final NotifyMsg notifyMsg = new NotifyMsg(method, method);
    notifyMsg.setCweNumber(606);
    notifyMsg.setAction(loopCheckActionDef);
    notifyMsg.append(errorMsg);

    boolean isBreak = action == RunChecks.BREAK_ACTION;
    if (shouldThrow && isBreak) {
      notifyMsg.append("  We cannot break from this loop, so throw an exception.");
      action = RunChecks.EXCEPTION_ACTION;
      try {
        constructor = CleartrackException.class.getConstructor(String.class);
      } catch (NoSuchMethodException | SecurityException e) {
        constructor = null; // We would never see this...
      }
    }

    if (action == RunChecks.EXCEPTION_ACTION || action == RunChecks.TERMINATE_ACTION) {
      notifyMsg.prepareForExceptionOrTerminate(constructor, action);
    }
    Notify.notifyAndRespond(notifyMsg);

    return isBreak;
  }

  /**
   * Called when Exceptions are encountered inside of a designated server loop, as determined
   * through analyis. This method will log the encountered exception and the server will continue
   * running as normal.
   *
   * @param e Exception that was thrown inside of the server loop.
   */
  public void handleServerException(Throwable e, int cweNumber, String type) {
    final String exceptionClass = e.getClass().getName();
    final NotifyMsg notifyMsg = new NotifyMsg(
        "ConfigFile.handleServerException(" + exceptionClass + ")", e.toString(), cweNumber);
    // Set action to something other than log only, since we are clearly intervening in this case.
    notifyMsg.setAction(RunChecks.REMOVE_ACTION);
    notifyMsg.append("Encountered " + exceptionClass + " in " + type + "...\n");
    notifyMsg.append("message: " + e.toString() + "\n");
    notifyMsg.append("stack track:\n");
    StackTraceElement[] stack = e.getStackTrace();
    for (StackTraceElement ste : stack)
      notifyMsg.append("  " + ste + "\n");
    Notify.notifyAndRespond(notifyMsg);
  }

  /**
   * Called in the finally block of Runnable.run() methods, to handle the cleanup of improperly
   * locked locks (to ensure we don't deadlock other threads).
   */
  public void handleThreadLocks() {
    Set<Lock> locks = Thread.currentThread().ss_lobjs;
    if (locks == null || locks.isEmpty())
      return;

    final NotifyMsg notifyMsg = new NotifyMsg("Thread.run()", "Thread.run()");
    notifyMsg.setCweNumber(764);
    notifyMsg.setAction(RunChecks.REMOVE_ACTION);
    Iterator<Lock> iter = locks.iterator();
    Lock lock = iter.next();
    notifyMsg.append("Thread id " + Thread.currentThread().getId()
        + " still holds the following locks at the end of execution: " + lock);
    // These appear to be the only types of locks that we can check whether it's held by the current
    // thread.
    unlock(lock);
    iter.remove();
    while (iter.hasNext()) {
      lock = iter.next();
      notifyMsg.append(", " + lock);
      unlock(lock);
      iter.remove();
    }

    notifyMsg.append(". Now releasing the aforementioned locks.");
    Notify.notifyAndRespond(notifyMsg);
  }

  /**
   * This method will unlock a ReentrantLock until the current thread no longer holds this lock. If
   * it's not a ReentrantLock, then presume that we only need to unlock it once.
   * 
   * @param lock Lock
   */
  private void unlock(Lock lock) {
    if (lock instanceof ReentrantLock) {
      ReentrantLock reLock = (ReentrantLock) lock;
      while (reLock.getHoldCount() > 0) {
        lock.unlock();
      }
    } else {
      // Assume not reentrant, even through it could be a ReentrantReadWriteLock.
      lock.unlock();
    }

    // TODO: Why no ReadLock.getHoldCount() method??? leave this for now.
    // else if (lock instanceof ReentrantReadWriteLock.ReadLock) {
    //   ReentrantReadWriteLock.ReadLock reLock = (ReentrantReadWriteLock.ReadLock) lock;
    //   while (reLock.getHoldCount() > 0) {
    //     lock.unlock();
    //   }
    // } else if (lock instanceof ReentrantReadWriteLock.WriteLock) {
    //   ReentrantReadWriteLock.WriteLock reLock = (ReentrantReadWriteLock.WriteLock) lock;
    //   while (reLock.getHoldCount() > 0) {
    //     lock.unlock();
    //   }
    // }

  }

  public Object nullCheck(Object obj, String method, Class<?> type)
      throws NoSuchMethodException, SecurityException, IOException {
    if (obj == null) {
      NotifyMsg notifyMsg = new NotifyMsg(method, method, 476);

      boolean throwEx = false;
      try {
        if ((type.getModifiers() & Modifier.FINAL) != Modifier.FINAL) {
          // We may not have a precise type and so any downcast would be dangerous.
          throwEx = true;
        } else {
          obj = ClassInstrumentation.newInstance(type);
          notifyMsg.setAction(RunChecks.LOG_ONLY_ACTION);
          notifyMsg.append(
              "Encountered a null object that may be dereferenced. Returning a new object [ hc: "
                  + System.identityHashCode(obj) + "] = " + obj);
        }
      } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
          | InvocationTargetException e) {
        throwEx = true;
      }

      if (throwEx) {
        notifyMsg.setAction(RunChecks.EXCEPTION_ACTION);
        notifyMsg.append("Unable to instantiate an object of type " + type.getName()
            + ". Instead of returning null, let's throw an exception.");
        notifyMsg.prepareForExceptionOrTerminate(
            CleartrackException.class.getConstructor(String.class), RunChecks.EXCEPTION_ACTION);
      }
      Notify.notifyAndRespond(notifyMsg);
    }
    return obj;
  }

  public int errorCheck(int value, int errorCode, String method) {
    // Just a place-holder, never called from anywhere.
    return value;
  }

  public int errorCheck(int value, int value_t, int errorCode, int errorCode_t, String method,
      Ret ret) throws NoSuchMethodException, SecurityException, IOException {
    if ((value_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED && value == errorCode) {
      NotifyMsg notifyMsg = new NotifyMsg(method, method, 253);
      notifyMsg.setAction(RunChecks.EXCEPTION_ACTION);
      notifyMsg
          .append("Tainted value matched an unhandled error code: " + value + " == " + errorCode);
      notifyMsg.prepareForExceptionOrTerminate(
          CleartrackException.class.getConstructor(String.class), RunChecks.EXCEPTION_ACTION);
      Notify.notifyAndRespond(notifyMsg);
    }
    return value;
  }

  public long errorCheck(long value, long errorCode, String method) {
    // Just a place-holder, never called from anywhere.
    return value;
  }

  public long errorCheck(long value, int value_t, long errorCode, int errorCode_t, String method,
      Ret ret) throws NoSuchMethodException, SecurityException, IOException {
    if ((value_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED && value == errorCode) {
      NotifyMsg notifyMsg = new NotifyMsg(method, method, 253);
      notifyMsg.setAction(RunChecks.EXCEPTION_ACTION);
      notifyMsg
          .append("Tainted value matched an unhandled error code: " + value + " == " + errorCode);
      notifyMsg.prepareForExceptionOrTerminate(
          CleartrackException.class.getConstructor(String.class), RunChecks.EXCEPTION_ACTION);
      Notify.notifyAndRespond(notifyMsg);
    }
    return value;
  }

  public boolean errorCheck(boolean value, boolean errorCode, String method) {
    // Just a place-holder, never called from anywhere.
    return value;
  }

  public boolean errorCheck(boolean value, int value_t, boolean errorCode, int errorCode_t,
      String method, Ret ret) throws NoSuchMethodException, SecurityException, IOException {
    if ((value_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED && value == errorCode) {
      NotifyMsg notifyMsg = new NotifyMsg(method, method, 253);
      notifyMsg.setAction(RunChecks.EXCEPTION_ACTION);
      notifyMsg
          .append("Tainted value matched an unhandled error code: " + value + " == " + errorCode);
      notifyMsg.prepareForExceptionOrTerminate(
          CleartrackException.class.getConstructor(String.class), RunChecks.EXCEPTION_ACTION);
      Notify.notifyAndRespond(notifyMsg);
    }
    return value;
  }

  /**
   * Takes the appropriate action when encountering a file whose type is not allowed by the system.
   * Depending on the configuration file the program will either throw an exception, terminate, or
   * jail the file to a safe directory.
   *
   * @param file the file whose content type is untrusted.
   * @param errorMessage
   * @throws IOException
   */
  public void handleUntrustedFileType(File file, String errorMessage) throws IOException {
    final NotifyMsg notifyMsg = new NotifyMsg("FileOutputStream.close()", file.getName());
    notifyMsg.append(errorMessage);

    int action = unallowed_mimetype_action;
    Constructor<?> constructor = unallowed_mimetype_exception_constructor;

    if (action == RunChecks.JAIL_ACTION) {
      boolean success = file.renameTo(new File(unallowed_mimetype_jail_file, file.getName()));
      notifyMsg
          .append((success) ? (".. File successfully jailed.\n") : (".. Unable to jail file."));
      if (!success) {
        action = RunChecks.REMOVE_ACTION;
      }
    }

    if (action == RunChecks.REMOVE_ACTION) {
      boolean success = file.delete();
      notifyMsg.append(
          (success) ? (".. File successfully removed.\n") : (".. Unable to remove file.\n"));
      if (!success) {
        // If we were unable to remove the file then terminate the application to prevent the
        // application from using the malicious file.
        action = RunChecks.TERMINATE_ACTION;
      }
    }

    notifyMsg.setCweNumber(434);
    notifyMsg.setAction(action);

    if (action == RunChecks.EXCEPTION_ACTION || action == RunChecks.TERMINATE_ACTION) {
      notifyMsg.prepareForExceptionOrTerminate(constructor, action);
    }
    Notify.notifyAndRespond(notifyMsg);
  }

  public void addSuppressed(Throwable origException, Throwable nestedException) throws IOException {
    if (origException == nestedException)
      return;
    NotifyMsg notifyMsg = new NotifyMsg("catch-block", "catch-block", 248);
    notifyMsg.setAction(RunChecks.LOG_ONLY_ACTION);
    notifyMsg.append("Uncaught exception thrown inside of catch-block. "
        + "Re-throwing the original exception.");
    origException.addSuppressed(nestedException);
    Notify.notifyAndRespond(notifyMsg);
  }

  public int reportNullByte(String description, String message) throws IOException {
    final NotifyMsg notifyMsg = new NotifyMsg(description, description);
    notifyMsg.append(message);
    notifyMsg.setCweNumber(626);
    notifyMsg.setAction(null_byte_action);

    if (null_byte_action == RunChecks.EXCEPTION_ACTION
        || null_byte_action == RunChecks.TERMINATE_ACTION) {
      notifyMsg.prepareForExceptionOrTerminate(null_byte_exception_constructor, null_byte_action);
    }

    Notify.notifyAndRespond(notifyMsg);

    return null_byte_replace_char;
  }

  public int getNullByteReplaceChar() {
    return null_byte_replace_char;
  }

  public byte getTrustedExceptionChar() {
    return trusted_exception_output_replace_char;
  }

  public int getTrustedExceptionAction() {
    return trusted_exception_output_action;
  }

  /**
   * Called when entering a catch block. This will push a new NotifyMsg object for the current
   * thread's exception stack.
   * 
   * @param method - name of the method.
   */
  public void enterCatch(String method) {
    Thread curThread = Thread.currentThread();
    if (curThread.ss_estack == null)
      curThread.ss_estack = new Stack<NotifyMsg>();
    String desc = "try-catch in method " + method;
    NotifyMsg notifyMsg = new NotifyMsg(desc, desc, 209);
    notifyMsg.setAction(trusted_exception_output_action);
    curThread.ss_estack.add(notifyMsg);
  }

  /**
   * Called when exiting a catch block. This will pop a NotifyMsg object off the current thread's
   * exception stack and take action only if there was a trusted write on that NotifyMsg.
   * 
   * @throws SecurityException
   * @throws NoSuchMethodException
   */
  public void exitCatch() {
    exitCatch(Thread.currentThread().ss_estack, false);
  }

  public void exitCatch(boolean returnFromFinally) {
    exitCatch(Thread.currentThread().ss_estack, returnFromFinally);
  }

  protected void exitCatch(Stack<NotifyMsg> notifyStack, boolean returnFromFinally) {
    if (notifyStack == null || notifyStack.isEmpty()) // should never happen...
      return;
    NotifyMsg notifyMsg = notifyStack.pop();
    try {
      if (!notifyMsg.isMessage())
        return; // There was no trusted write.
      notifyMsg.prepend("Attempting to write trusted data inside of a catch/finally block:\n");
      Notify.notifyAndRespond(notifyMsg);
    } finally {
      if (returnFromFinally) {
        notifyMsg = new NotifyMsg(notifyMsg.getDescription(), notifyMsg.getCommand(), 584);
        notifyMsg.setAction(RunChecks.EXCEPTION_ACTION);
        notifyMsg.append("Attempting to return from a finally block");
        try {
          notifyMsg.prepareForExceptionOrTerminate(
              CleartrackException.class.getConstructor(String.class), RunChecks.EXCEPTION_ACTION);
        } catch (NoSuchMethodException | SecurityException e) {
          notifyMsg.prepareForExceptionOrTerminate(null, RunChecks.TERMINATE_ACTION);
        } finally {
          Notify.notifyAndRespond(notifyMsg);
        }
      }
    }
  }

  public Map<String, Set<String>> getAllowableMimeTypes() {
    return mimeTypesMap;
  }

  public Set<String> getAllContentTypesAllowed() {
    return contentTypesAllowed;
  }

  public boolean untrustedKeyCanAccessPropertyValue() {
    return untrusted_key_can_access_property_value;
  }

  public boolean taintCommandArgs() {
    return taint_arguments;
  }

  public boolean taintEnvVars() {
    return taint_env_vars;
  }

  public boolean taintDbReads() {
    return taint_db_reads;
  }

  public boolean isPortTrusted(int port) {
    if (trustAllPorts)
      return true;
    synchronized (INSTANCE) {
      if (trustedPortsSet == null) {
        trustedPortsSet = new HashSet<Integer>();
        for (int i = 0; i < trustedPorts.length; i++)
          trustedPortsSet.add(trustedPorts[i]);
      }
    }
    return trustedPortsSet.contains(port);
  }

  public int getServerSocketTimeout() {
    return server_socket_timeout;
  }

  public int getClientSocketTimeout() {
    return client_socket_timeout;
  }

  public boolean isTestHarnessEnabled() {
    return test_harness_enabled;
  }

  public boolean canSetCatalogWithUntrustedData() {
    return can_set_catalog_with_untrusted_data;
  }

  public boolean canSetClientInfoWithUntrustedProps() {
    return can_set_clientinfo_with_untrusted_properties;
  }

  public boolean canSetClientInfoWithUntrustedNameOrValue() {
    return can_set_clientinfo_with_untrusted_name_or_value;
  }

  public boolean canSetSavePointWithUntrustedName() {
    return can_set_savepoint_with_untrusted_name;
  }

  public boolean canSetTypeMapWithUntrustedMap() {
    return can_set_typemap_with_untrusted_map;
  }

  // CALLED ONLY FROM BACKSTOP CONFINEMENT TESTS

  public synchronized void setIsUnitTesting(Writer writer, boolean value) {
    isUnitTesting = value;
    writer.ss_socktype = SocketType.CLIENT_SINK;
  }

  public synchronized String getSanitizedSocketString() {
    return sanitizedSocketString;
  }

  // THE FOLLOWING METHODS ARE ONLY CALLED FROM CONFINEMENT WRAPPERS, USED
  // FOR BACKSTOP CONFINEMENT (I.E. WITH FULL TAINT TRACKING)... IF YOU
  // WANT TO ENABLE BACKSTOP CONFINEMENT WITH CHARTAINT APPROACH, YOU WILL
  // NEED TO REFACTOR THESE METHODS AND CREATE THE APPROPRIATE WRAPPERS.

  /**
   * Identifies the language of the output at a client socket, performs the corresponding language
   * check and returns the resulting sanitized result.
   * 
   * @param desc The API method description WITHOUT return type and WITHOUT parameter identifiers.
   * @param bb The ByteBuffer that contains the socket output to be checked.
   * @return The ByteBuffer that contains the sanitized socket output.
   */
  public ByteBuffer runClientSocketOutputCheck(String desc, ByteBuffer bb, Ret ret) {
    if (bb.hasArray()) {
      ByteArrayTaint bytes = bb.array(ret);
      ByteArrayTaint newBytes = runClientSocketOutputCheck(desc, bytes, ret);
      if (newBytes != bytes) {
        return ByteBuffer.wrap(newBytes, ret);
      }
    }
    return bb;
  }

  /**
   * Identifies the language of the output at a client socket, performs the corresponding language
   * check and returns the resulting sanitized result.
   * 
   * @param desc The API method description WITHOUT return type and WITHOUT parameter identifiers.
   * @param b The byte array that contains the socket output to be checked.
   * @return The byte array that contains the sanitized socket output.
   */
  public ByteArrayTaint runClientSocketOutputCheck(String desc, ByteArrayTaint b, Ret ret) {
    Charset charset = StringInstrumentation.getCharset(b);
    if (charset != null) {
      String processMe = new String(b, charset, ret);
      String newProcessMe = runClientSocketOutputCheck(desc, processMe, ret);
      if (!newProcessMe.equals(processMe)) {
        return newProcessMe.getBytes(charset, ret);
      }
    }
    return b;
  }

  /**
   * Identifies the language of the output at a client socket, performs the corresponding language
   * check and returns the resulting sanitized result.
   * 
   * @param desc The API method description WITHOUT return type and WITHOUT parameter identifiers.
   * @param processMe The socket output string to be checked.
   * @return The sanitized socket output string.
   */
  public String runClientSocketOutputCheck(String desc, String processMe, Ret ret) {
    Chk chk;
    int runningShell = 0;
    try {
      switch (LangTester.theInstance.testLang(processMe)) {
        case LangConstants.COMMAND:
          chk = findChkFromName("execCmdString");
          break;
        case LangConstants.LDAP:
          chk = findChkFromName("ldapChk");
          break;
        case LangConstants.SQL:
          chk = findChkFromName("sqlChk");
          break;
        case LangConstants.XQUERY:
          chk = findChkFromName("xqueryChk");
          break;
        default:
          chk = findChkFromName("numericOrAlphaNumericChk");
      }
      if (chk == null) // A check could not exist in the config file.
        return processMe;
      final NotifyMsg notifyMsg = new NotifyMsg(desc, processMe);
      processMe = chk.runFromChk(processMe, null, runningShell, notifyMsg);
    } catch (MsgException e) {
      Notify.error("Exception: " + e + "\n");
    } catch (IOException e) {
      Notify.error("Exception: " + e + "\n");
    }
    if (isUnitTesting)
      sanitizedSocketString = processMe;
    return processMe;
  }

  /**
   * Run malformed HTTP request check on the input string at a server socket.
   * 
   * @param desc The API method description WITHOUT return type and WITHOUT parameter identifiers.
   * @param processMe The socket input string to be checked.
   * @return The sanitized socket input string.
   * @throws IOException
   */
  public String runServerSocketInputCheck(String desc, String processMe, Ret ret)
      throws IOException {
    Matcher m = HTTPREQUEST_PAT.matcher(processMe);
    // Matcher m = PatternInstrumentation.matcher(HTTPREQUEST_PAT, processMe);
    if (m.matches()) {
      processMe = checkHTTPRequest(desc, processMe, m, ret);
    }
    if (isUnitTesting)
      sanitizedSocketString = processMe;
    return processMe;
  }

  /**
   * Checks the given HTTP resquest string and takes appropriate action according to
   * malformed_http_request_action if the HTTP request string is malformed.
   * 
   * @param str The HTTP request string.
   * @param m The matcher that matches the HTTP request string against the pattern HTTPREQUEST_PAT.
   * @return The sanitized HTTP request string.
   * @throws IOException
   */
  private String checkHTTPRequest(String desc, String processMe, Matcher m, Ret ret)
      throws IOException {
    String meth = m.group(1, TaintValues.TRUSTED, ret);
    String path = m.group(2, TaintValues.TRUSTED, ret);
    String version = m.group(3, TaintValues.TRUSTED, ret);
    // String meth = MatcherInstrumentation.group(m, 1);
    // String path = MatcherInstrumentation.group(m, 2);
    // String version = MatcherInstrumentation.group(m, 3);
    int queryStrStart = path.indexOf('?');
    if (queryStrStart < 0)
      return processMe;
    int fragStart = path.indexOf('#');
    int queryStrEnd = (fragStart < 0) ? path.length() : fragStart;
    String queryStr = path.substring(queryStrStart + 1, TaintValues.TRUSTED, queryStrEnd,
        TaintValues.TRUSTED, ret);
    if (!isLegalQueryString(queryStr)) {
      final NotifyMsg notifyMsg = new NotifyMsg(desc, processMe);
      notifyMsg.setCweNumber(88);
      notifyMsg.append("argument injection or modification: " + "HTTP  request is malformed: ");
      notifyMsg.setAction(malformed_http_request_action);
      if (malformed_http_request_action == RunChecks.EXCEPTION_ACTION
          || malformed_http_request_action == RunChecks.TERMINATE_ACTION) {
        notifyMsg.prepareForExceptionOrTerminate(malformed_http_request_exception_constructor,
            malformed_http_request_action);
      }

      if (malformed_http_request_action == RunChecks.REPLACE_ACTION) {
        String sanitizedQueryStr = sanitizeQueryString(queryStr, ret);
        StringBuilder sb = new StringBuilder(ret);
        sb.append(meth, ret);
        sb.append(path.substring(0, TaintValues.TRUSTED, queryStrStart, TaintValues.TRUSTED, ret),
            ret);
        if (sanitizedQueryStr.length() > 0) {
          sb.append(TaintUtils.trust("?"), ret);
          sb.append(sanitizedQueryStr, ret);
        }
        if (fragStart > 0)
          sb.append(path.substring(fragStart, TaintValues.TRUSTED, ret), ret);
        sb.append(version, ret);
        String newProcessMe = sb.toString(ret);
        notifyMsg.append("path was rewritten\n");
        notifyMsg.append("\"" + processMe + "\" rewritten to \"" + newProcessMe + "\"\n");
        Notify.notifyAndRespond(notifyMsg);
        return newProcessMe;
      }
      notifyMsg.append("\"" + processMe + "\"\n");
      Notify.notifyAndRespond(notifyMsg);
    }
    return processMe;
  }

  /**
   * Returns true if the query string in a URL is legal.
   * 
   * @param queryStr The query string in a URL.
   * @return true if the query string is legal.
   */
  private static boolean isLegalQueryString(String queryStr) {
    String[] arrayParam = queryStr.split("&");
    for (String param : arrayParam) {
      if (!isLegalParamSpec(param))
        return false;
    }
    return true;
  }

  /**
   * Returns true if the given parameter spec in a URL query string is legal.
   * 
   * @param param The parameter spec in a URL query string.
   * @return true if the parameter spec is legal.
   */
  private static boolean isLegalParamSpec(String param) {
    int pos = param.indexOf('=');
    if (pos < 0)
      return false;
    String key = param.substring(0, pos);
    String value = param.substring(pos + 1);
    return isLegalKeyOrValue(key) && isLegalKeyOrValue(value);
  }

  /**
   * Returns true if the given string is a legal key or value in a URL query string.
   * 
   * @param key The key or value in a parameter spec contained in a URL query string.
   * @return true if the given string is a legal key or value.
   */
  private static boolean isLegalKeyOrValue(String key) {
    for (int i = 0; i < key.length(); i++) {
      char c = key.charAt(i);
      if (Character.isLetterOrDigit(c))
        continue;
      switch (c) {
        case '.':
        case '-':
        case '+':
        case '~':
        case '_':
          continue;
        case '%':
          if (i + 2 >= key.length() || !isHEXDigit(key.charAt(i + 1))
              || !isHEXDigit(key.charAt(i + 2)))
            return false;
          else
            continue;
        default:
          return false;
      }
    }
    return true;
  }

  /**
   * Returns true if the given character is a hexadecimal digit.
   * 
   * @param ch The character.
   * @return true if the character is a hexadecial digit.
   */
  private static boolean isHEXDigit(char ch) {
    return Character.isDigit(ch) || ('a' <= ch && ch <= 'f') || ('A' <= ch && ch <= 'F');
  }

  /**
   * Sanitizes the query string in a URL by encoding all the illegal characters in the parameter
   * specs using the URL encoding.
   * 
   * @param str The query string in a URL.
   * @return The sanitized query string.
   * @throws IOException
   */
  private String sanitizeQueryString(String queryStr, Ret ret) throws IOException {
    StringBuilder buf = new StringBuilder(ret);
    String[] arrayParam = queryStr.split("&", ret);
    for (int i = 0; i < arrayParam.length; i++) {
      if (i > 0) {
        buf.append('&', TaintValues.TRUSTED, ret);
      }
      String param = arrayParam[i];
      int pos = param.indexOf('=');
      if (pos < 0) {
        buf.append(urlEncode(param, ret), ret);
      } else {
        String key = param.substring(0, TaintValues.TRUSTED, pos, TaintValues.TRUSTED, ret);
        String value = param.substring(pos + 1, TaintValues.TRUSTED, ret);
        boolean isLegalKey = isLegalKeyOrValue(key);
        boolean isLegalValue = isLegalKeyOrValue(value);
        if (isLegalKey && isLegalValue) {
          buf.append(param, ret);
        } else {
          key = (isLegalKey) ? key : urlEncode(key, ret);
          value = (isLegalValue) ? value : urlEncode(value, ret);
          buf.append(key, ret);
          buf.append('=', TaintValues.TRUSTED, ret);
          buf.append(value, ret);
        }
      }
    }
    return buf.toString(ret);
  }

  /*
   * The given string might be (partially) encoded. So we need to decode it first before performing
   * the URL encoding.
   */
  private String urlEncode(String str, Ret ret) throws IOException {
    return URLEncoderInstrumentation.encode(URLDecoderInstrumentation.decode(str, "UTF-8", ret),
        "UTF-8", ret);
  }

  /**
   * Run tainted HTTP redirect check on the output bytes at a server socket.
   * 
   * @param desc The API method description WITHOUT return type and WITHOUT parameter identifiers.
   * @param b The byte array that contains socket output to be checked.
   * @return The byte array that contains sanitized socket output.
   */
  public ByteArrayTaint runServerSocketOutputCheck(String desc, ByteArrayTaint b, Ret ret) {
    Charset charset = StringInstrumentation.getCharset(b);
    if (charset != null) {
      String processMe;
      try {
        processMe = new String(b, charset, ret);
        String newProcessMe = runServerSocketOutputCheck(desc, processMe, ret);
        if (!newProcessMe.equals(processMe)) {
          return newProcessMe.getBytes(charset, ret);
        }
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return b;
  }

  /**
   * Run tainted HTTP redirect check on the output string at a server socket. redirect response.
   * 
   * @param desc The API method description WITHOUT return type and WITHOUT parameter identifiers.
   * @param processMe The socket output string to be checked.
   * @return The sanitized socket output string.
   */
  public String runServerSocketOutputCheck(String desc, String processMe, Ret ret)
      throws IOException {
    Matcher m = HTTPRESPONSE_PAT.matcher(processMe);
    // Matcher m = PatternInstrumentation.matcher(HTTPRESPONSE_PAT, processMe);
    if (m.matches()) {
      processMe = checkHTTPRedirectResponse(desc, processMe, m, ret);
    }
    if (isUnitTesting)
      sanitizedSocketString = processMe;
    return processMe;
  }

  /**
   * Checks the given HTTP response and takes appropriate action according to
   * tainted_redirect_action if the response contains any tainted/untrusted redirect.
   * 
   * @param desc The API method description WITHOUT return type and WITHOUT parameter identifiers.
   * @param processMe The HTTP response string.
   * @param m The matcher that matches the HTTP response string against the pattern HTTPRESPONSE_PAT.
   * @return The sanitized HTTP response string.
   * @throws IOException if the HTTP response contains a tainted/untrusted redirect and the call to
   *         Notify.notifyAndResponse throws an IOException.
   */
  private String checkHTTPRedirectResponse(String desc, String processMe, Matcher m, Ret ret)
      throws IOException {
    int groupCount = m.groupCount();
    String untrustedRedirectHeader = null;
    String untrustedRedirectLocation = null;
    for (int i = 1; i <= groupCount; i++) {
      String header = m.group(i);
      // String header = MatcherInstrumentation.group(m, i);
      int pos = header.indexOf(':');
      String fieldName = header.substring(0, TaintValues.TRUSTED, pos, TaintValues.TRUSTED, ret);
      if (fieldName.equals("Location")) {
        String location = header.substring(pos + 1, TaintValues.TRUSTED, ret);

        location = location.trim(ret);
        Notify.log("Checking http redirect:\n%s", TaintUtils.createTaintDisplayLines(location));
        if (!TaintUtils.isTrusted(location)) {
          untrustedRedirectHeader = header;
          untrustedRedirectLocation = location;
        }
      }
    }
    if (untrustedRedirectHeader != null) {
      final NotifyMsg notifyMsg = new NotifyMsg(desc, processMe);
      notifyMsg.setCweNumber(601);
      notifyMsg.append(
          "URL redirection to untrusted site: " + "HTTP response contains tainted location: ");
      notifyMsg.setAction(tainted_redirect_action);

      if (tainted_redirect_action == RunChecks.EXCEPTION_ACTION
          || tainted_redirect_action == RunChecks.TERMINATE_ACTION) {
        notifyMsg.prepareForExceptionOrTerminate(tainted_redirect_exception_constructor,
            tainted_redirect_action);
      }

      if (tainted_redirect_action == RunChecks.REMOVE_ACTION) {
        String newProcessMe = processMe.replace(untrustedRedirectHeader, "", ret);
        notifyMsg.append("path was rewritten\n");
        notifyMsg.append("\"" + processMe + "\" rewritten to \"" + newProcessMe + "\"\n");
        Notify.notifyAndRespond(notifyMsg);
        return newProcessMe;
      }
      notifyMsg.append("\"" + untrustedRedirectLocation + "\"\n");
      Notify.notifyAndRespond(notifyMsg);
    }
    return processMe;
  }
}
