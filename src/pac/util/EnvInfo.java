package pac.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Properties;

import com.sun.security.auth.module.UnixSystem;

/**
 * Finds out standard information about the current environment without trusting environment
 * variables or properties. Currently presumes Unix and some aspects of our test system.
 */
public class EnvInfo {
  public static String ANY_VALUE = "-1";
  private static HashMap<String, String> env = new LinkedHashMap<String, String>();

  private static String user_name = null;
  private static String home_dir = null;
  private static String cur_dir = null;
  private static String hostname = null;

  private static boolean isMac() {
    String os = System.getProperty("os.name").toLowerCase();
    return (os.indexOf("mac") >= 0);
  }

  public static void init_env_info(Properties sysProps) {

    // Get the current directory.
    try {
      cur_dir = new java.io.File(".").getCanonicalPath();
    } catch (Exception e) {
    }

    // User name
    UnixSystem us = new UnixSystem();
    user_name = us.getUsername();
    String pw_us = user_name + ":";
    LineNumberReader lr = null;
    try {
      lr = new LineNumberReader(new FileReader("/etc/passwd"));
      for (String line = lr.readLine(); line != null; line = lr.readLine()) {
        if (line.startsWith(pw_us)) {
          String[] arr = line.split(":");
          home_dir = arr[5];
        }
      }
    } catch (Exception e) {
    } finally {
      try {
        if (lr != null)
          lr.close();
      } catch (IOException e) {
      }
    }

    // Guess at home dir if we didn't find one Default guess.
    if (home_dir == null) {
      if (cur_dir.contains("/Users") || isMac())
        home_dir = "/Users/" + user_name;
      else
        home_dir = "/home/" + user_name;
    }

    // Get hostname.
    try {
      InetAddress addr = InetAddress.getLocalHost();
      // byte[] ipAddr = addr.getAddress();
      hostname = addr.getHostName();
    } catch (Exception e) {
    }

    // Fill in the env table.
    env.put("USER", user_name);
    env.put("LOGNAME", user_name);
    if (cur_dir != null)
      env.put("PWD", cur_dir);
    env.put("HOME", home_dir);
    if (hostname != null)
      env.put("HOST", hostname);

    // Let's just trust these to be any value.
    env.put("JAVA_HOME", ANY_VALUE);
    env.put("OSTYPE", ANY_VALUE);
    env.put("MACHTYPE", ANY_VALUE);
    env.put("LD_LIBRARY_PATH", "");

    // We check that all execs take from the standard path, so we don't care what this path value
    // is. Arguably we should put the standard paths at the beginning of this.
    env.put("PATH", ANY_VALUE);
    // There doesn't seem to be any danger in PRINTER.
    env.put("PRINTER", ANY_VALUE);
    // What X display is used seems to be inherently under the users control.
    env.put("DISPLAY", ANY_VALUE);
    // Too much trouble not to trust this.
    env.put("CLASSPATH", ANY_VALUE);

    File propertyGoalFile = new File(home_dir, ".cleartrack/properties.goal");
    if (!propertyGoalFile.exists()) // If it doesn't exist we must be in T&E, so look in CWD.
      propertyGoalFile = new File("properties.goal");

    try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(propertyGoalFile));) {
      Properties goalProps = (Properties) in.readObject();
      for (Entry<Object, Object> entry : sysProps.entrySet()) {
        Object key = entry.getKey();
        Object value = entry.getValue();
        if (!(key instanceof String) || !(value instanceof String))
          continue;
        String goalVal = goalProps.getProperty((String) key);
        if (goalVal == null)
          continue; // any user defined property should be specified by the config file
        String actualVal = (String) value;
        // Always trust the key if it came from the properties.goal
        TaintUtils.trust((String) key, TaintValues.PROPERTY);
        /*
         * FIXME: MITRE tests are instrumented and executed on different machines, so we will need a
         * good solution for this. Let's always trust base properties for the time being...
         * if (actualVal.equals(goalVal) || key.equals("user.dir") || key.equals("java.class.path") ||
         * key.equals("sun.boot.class.path") || key.equals("java.ext.paths"))
         */
        TaintUtils.trust(actualVal, TaintValues.PROPERTY);
      }
    } catch (IOException | ClassNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /**
   * Returns a map with valid environment variable information. Variables in the map have either
   * correct information or ANY_VALUE as a value. If they have ANY_VALUE, then any setting for that
   * name is acceptable (and can be thought of as trusted) Variables in the environment and not in
   * the map should be considered untrusted. If used as part of an exec() call, they should be
   * removed or set to a harmless value.
   */
  public static HashMap<String, String> get_env_map() {
    return env;
  }

  public static void main(String[] args) throws IOException {
    init_env_info(System.getProperties());

    HashMap<String, String> test_env = get_env_map();
    System.out.println("Env vars:");
    for (String key : test_env.keySet()) {
      System.out.println(key + " = " + test_env.get(key));
    }
  }
}
