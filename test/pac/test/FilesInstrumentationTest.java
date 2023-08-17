package pac.test;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import pac.util.TaintUtils;
import pac.util.TaintValues;

public class FilesInstrumentationTest {

  @Test
  public void fileTraversalTest() {
    String str = null;
    Path path = null;
    FileSystem fs = FileSystems.getDefault();

    /*
     * The following are tests for valid path traversals according to the new policy set by MITRE...
     */

    final String CWD = new File("").getAbsolutePath();

    // BENIGN PATHS...

    str = "file.txt";
    TaintUtils.taint(str);
    path = fs.getPath(str);
    Assert.assertEquals(str, path.toFile().getPath());

    str = "benign/file.txt";
    TaintUtils.taint(str);
    path = fs.getPath(str);
    Assert.assertEquals(str, path.toFile().getPath());

    str = "benign/../file.txt";
    TaintUtils.taint(str);
    path = fs.getPath(str);
    Assert.assertEquals(str, path.toFile().getPath());

    str = TaintUtils.trust("/some/trusted/path/file.txt");
    str = TaintUtils.mark(str, TaintValues.TAINTED, 19, str.length() - 1);
    path = fs.getPath(str);
    Assert.assertEquals(str, path.toFile().getPath());

    str = TaintUtils.trust("/some/trusted/path/x/../file.txt");
    str = TaintUtils.mark(str, TaintValues.TAINTED, 19, str.length() - 1);
    path = fs.getPath(str);
    Assert.assertEquals(str, path.toFile().getPath());

    str = TaintUtils.trust("/some/trusted/path/some/dir/../bob/../bob");
    str = TaintUtils.mark(str, TaintValues.TAINTED, 19, 30);
    path = fs.getPath(str);
    Assert.assertEquals(str, path.toFile().getPath());

    str = TaintUtils.trust("./janedoe/../config.properties"); // doesn't go above the CWD
    str = TaintUtils.mark(str, TaintValues.TAINTED, 2, 8);
    str = TaintUtils.mark(str, TaintValues.TAINTED, 10, str.length() - 1);
    path = fs.getPath(str);
    Assert.assertEquals(str, path.toFile().getPath());

    // ATTACK PATHS...

    str = "../file.txt";
    TaintUtils.taint(str);
    path = fs.getPath(str);
    Assert.assertEquals("__/file.txt", path.toFile().getPath());

    str = TaintUtils.trust(CWD + "/../joe/file.txt");
    str = TaintUtils.mark(str, TaintValues.TAINTED, CWD.length() + 1, str.length() - 1);
    path = fs.getPath(str);
    Assert.assertEquals(CWD + "/__/joe/file.txt", path.toFile().getPath());

    str = TaintUtils.trust("/some/trusted/path/x/../../joe/file.txt");
    str = TaintUtils.mark(str, TaintValues.TAINTED, 19, str.length() - 1);
    path = fs.getPath(str);
    Assert.assertEquals("/some/trusted/path/x/__/__/joe/file.txt", path.toFile().getPath());

    str = TaintUtils.trust(CWD + "/some/dir/../bob/../bob/../../..");
    str = TaintUtils.mark(str, TaintValues.TAINTED, CWD.length() + 1, CWD.length() + 11);
    str = TaintUtils.mark(str, TaintValues.TAINTED, CWD.length() + 24, str.length() - 1);
    path = fs.getPath(str);
    Assert.assertEquals(CWD + "/some/dir/__/bob/../bob/__/__/__", path.toFile().getPath());

    str = TaintUtils.trust("/some/trusted/path/some/dir/../bob/../bob/../..");
    str = TaintUtils.mark(str, TaintValues.TAINTED, 19, 30);
    str = TaintUtils.mark(str, TaintValues.TAINTED, 42, str.length() - 1);
    path = fs.getPath(str);
    Assert.assertEquals("/some/trusted/path/some/dir/__/bob/../bob/__/__", path.toFile().getPath());

    str = TaintUtils.trust("/some/trusted/path/some/dir/../bob/../bob/..");
    str = TaintUtils.mark(str, TaintValues.TAINTED, 19, 30);
    str = TaintUtils.mark(str, TaintValues.TAINTED, 42, str.length() - 1);
    path = fs.getPath(str);
    Assert.assertEquals("/some/trusted/path/some/dir/__/bob/../bob/__", path.toFile().getPath());

    if (!CWD.contains(" ")) {
      // Spaces in filenames sort of invalidates this test since the whitespace is converted to _,
      // and therefore CWD is no longer CWD.
      String child = new File(CWD).getName();
      str = TaintUtils.trust(CWD + "/../" + child + "/file.txt");
      str = TaintUtils.mark(str, TaintValues.TAINTED, CWD.length(), str.length() - 1);
      path = fs.getPath(str);
      Assert.assertEquals(CWD + "/__/" + child + "/file.txt", path.toFile().getPath());
    }

    str = TaintUtils.trust("./janedoe/../johndoe/birthcertificate.txt");
    str = TaintUtils.mark(str, TaintValues.TAINTED, 10, str.length() - 1);
    path = fs.getPath(str);
    Assert.assertEquals("./janedoe/__/johndoe/birthcertificate.txt", path.toFile().getPath());

    str = TaintUtils.trust("./janedoe//../johndoe/birthcertificate.txt");
    str = TaintUtils.mark(str, TaintValues.TAINTED, 10, str.length() - 1);
    path = fs.getPath(str);
    Assert.assertEquals("./janedoe/__/johndoe/birthcertificate.txt", path.toFile().getPath());

    str = TaintUtils.trust("./janedoe/./../johndoe/birthcertificate.txt");
    str = TaintUtils.mark(str, TaintValues.TAINTED, 10, str.length() - 1);
    path = fs.getPath(str);
    Assert.assertEquals("./janedoe/./__/johndoe/birthcertificate.txt", path.toFile().getPath());
  }

  @Test
  public void absolutePathTest() {

    // Tainted attack paths not within the CWD...

    String path = "/etc/dir1";
    int len = path.length();
    TaintUtils.taint(path);
    Assert.assertEquals("tainted path not in the CWD should have replaced leading slash with _",
        new File(path).toPath().toString(), "_etc/dir1");

    path = TaintUtils.trust("/etc/dir1/file");
    path = TaintUtils.mark(path, TaintValues.TAINTED, 0, len);
    Assert.assertEquals("tainted path not in the CWD should have replaced leading slash with _",
        new File(path).toPath().toString(), "_etc/dir1/file");

    path = TaintUtils.trust("/etc/dir1/dir2/../file");
    path = TaintUtils.mark(path, TaintValues.TAINTED, 0, len);
    Assert.assertEquals("tainted path not in the CWD should have replaced leading slash with _",
        new File(path).toPath().toString(), "_etc/dir1/dir2/../file");

    // Tainted benign paths within the CWD...

    path = "dir1/dir2";
    len = path.length();
    TaintUtils.taint(path);
    Assert.assertEquals("tainted path in the CWD should not have replaced leading slash with _",
        new File(path).toPath().toString(), path);

    path = TaintUtils.trust("dir1/dir2/file");
    path = TaintUtils.mark(path, TaintValues.TAINTED, 0, len);
    Assert.assertEquals("tainted path in the CWD should not have replaced leading slash with _",
        new File(path).toPath().toString(), path);

    path = TaintUtils.trust("dir1/dir2/../file");
    path = TaintUtils.mark(path, TaintValues.TAINTED, 0, len);
    Assert.assertEquals("tainted path in the CWD should not have replaced leading slash with _",
        new File(path).toPath().toString(), path);

    final String CWD = new String(new File("").getAbsolutePath().toCharArray());
    TaintUtils.taint(CWD);

    // Shame on you if you use whitespace in file names!!!
    if (!CWD.contains(" ")) {
      // Spaces in filenames sort of invalidates this test since the whitespace is converted to _,
      // and therefore CWD is no longer CWD.
      path = CWD + TaintUtils.trust("/dir1/dir2");
      Assert.assertEquals("tainted path in the CWD should not have replaced leading slash with _",
          new File(path).toPath().toString(), path);

      path = CWD + TaintUtils.trust("/dir1/dir2/file");
      Assert.assertEquals("tainted path in the CWD should not have replaced leading slash with _",
          new File(path).toPath().toString(), path);

      path = CWD + TaintUtils.trust("/dir1/dir2/../file");
      Assert.assertEquals("tainted path in the CWD should not have replaced leading slash with _",
          new File(path).toPath().toString(), path);

      String suffix = "/dir3/../file.txt";
      TaintUtils.taint(suffix);
      path = CWD + TaintUtils.trust("/dir1/dir2") + suffix;
      Assert.assertEquals("tainted path in the CWD should not have replaced leading slash with _",
          new File(path).toPath().toString(), path);

      suffix = "/dir3/../dir4/dir5/../../file.txt";
      TaintUtils.taint(suffix);
      path = CWD + TaintUtils.trust("/dir1/dir2") + suffix;
      Assert.assertEquals("tainted path in the CWD should not have replaced leading slash with _",
          new File(path).toPath().toString(), path);

      // Tainted attack paths within the CWD...

      suffix = "/dir3/../dir4/../../file.txt";
      TaintUtils.taint(suffix);
      path = CWD + TaintUtils.trust("/dir1/dir2") + suffix;
      Assert.assertEquals("tainted path in the CWD should not have replaced leading slash with _",
          new File(path).toPath().toString(), path.replaceAll("\\.\\.", "__"));

      suffix = "/dir3/../../dir4/../../file.txt";
      TaintUtils.taint(suffix);
      path = CWD + TaintUtils.trust("/dir1/dir2") + suffix;
      Assert.assertEquals("tainted path in the CWD should not have replaced leading slash with _",
          new File(path).toPath().toString(), path.replaceAll("\\.\\.", "__"));

      suffix = "/dir3/../../dir4/../file.txt";
      TaintUtils.taint(suffix);
      path = CWD + TaintUtils.trust("/dir1/dir2") + suffix;
      Assert.assertEquals("tainted path in the CWD should not have replaced leading slash with _",
          new File(path).toPath().toString(), path.replaceAll("\\.\\.", "__"));
    }
  }

  @Test
  public void canonicalFileTest() throws IOException {
    FileSystem fs = FileSystems.getDefault();
    String userDir = System.getProperty("user.dir");

    Path path = fs.getPath(userDir);
    String filepath = path.toFile().getPath();
    Assert.assertTrue("file path was not trusted",
        TaintUtils.isTrusted(filepath, 0, filepath.length() - 1));

    String pathStr = "src";
    TaintUtils.taint(pathStr);
    String testPath = userDir + File.separator + pathStr;

    path = fs.getPath(userDir, pathStr);
    filepath = path.toFile().getPath();
    Assert.assertTrue("file paths metadata did not match expected result",
        TaintUtils.hasEqualTaint(testPath, filepath, TaintValues.TRUST_MASK));

    File f3 = new File(path.toFile().getCanonicalPath());
    filepath = f3.getPath();
    Assert.assertTrue("file paths metadata did not match expected result",
        TaintUtils.hasEqualTaint(testPath, filepath, TaintValues.TRUST_MASK));

    File cf = path.toFile().getCanonicalFile();
    filepath = cf.getPath();
    Assert.assertTrue("file paths metadata did not match expected result",
        TaintUtils.hasEqualTaint(testPath, filepath, TaintValues.TRUST_MASK));

    filepath = cf.getCanonicalPath();
    Assert.assertTrue("file paths metadata did not match expected result",
        TaintUtils.hasEqualTaint(testPath, filepath, TaintValues.TRUST_MASK));
  }

  @Test
  public void canonicalFileTest2() throws IOException {
    FileSystem fs = FileSystems.getDefault();
    String pathStr = TaintUtils.trust("src");
    TaintUtils.taint(pathStr);

    Path path = fs.getPath(pathStr);
    String filepath = path.toFile().getPath();
    Assert.assertTrue("file path should be tainted", TaintUtils.isTainted(filepath));

    File f3 = new File(path.toFile().getCanonicalPath());
    filepath = f3.getPath();
    Assert.assertTrue(
        "file path based on partially tainted canonical " + "path should at least be tainted",
        TaintUtils.isTainted(filepath));

    File cf = path.toFile().getCanonicalFile();
    filepath = cf.getPath();
    Assert.assertTrue(
        "file path based on partially tainted canonical " + "path should at least be tainted",
        TaintUtils.isTainted(filepath));

    filepath = path.toFile().getCanonicalPath();
    Assert.assertTrue(
        "file path based on partially tainted canonical " + "path should at least be tainted",
        TaintUtils.isTainted(filepath));
  }

  @Test
  public void absoluteFileTest() throws IOException {
    FileSystem fs = FileSystems.getDefault();
    String userDir = System.getProperty("user.dir");

    Path path = fs.getPath(userDir);
    String filepath = path.toFile().getPath();
    Assert.assertTrue("file path was not trusted",
        TaintUtils.isTrusted(filepath, 0, filepath.length() - 1));

    String pathStr = TaintUtils.trust("/src");
    TaintUtils.taint(pathStr);
    String testPath = userDir + File.separator + pathStr.substring(1);

    path = fs.getPath(userDir, pathStr);
    filepath = path.toFile().getPath();
    Assert.assertTrue("file paths metadata did not match expected result",
        TaintUtils.hasEqualTaint(testPath, filepath, TaintValues.TRUST_MASK));

    File f3 = new File(path.toFile().getAbsolutePath());
    filepath = f3.getPath();
    Assert.assertTrue("file paths metadata did not match expected result",
        TaintUtils.hasEqualTaint(testPath, filepath, TaintValues.TRUST_MASK));

    File cf = path.toFile().getAbsoluteFile();
    filepath = cf.getPath();
    Assert.assertTrue("file paths metadata did not match expected result",
        TaintUtils.hasEqualTaint(testPath, filepath, TaintValues.TRUST_MASK));

    filepath = cf.getAbsolutePath();
    Assert.assertTrue("file paths metadata did not match expected result",
        TaintUtils.hasEqualTaint(testPath, filepath, TaintValues.TRUST_MASK));
  }

  @Test
  public void absoluteFileTest2() throws IOException {
    FileSystem fs = FileSystems.getDefault();
    String userDir = System.getProperty("user.dir");

    String pathStr = TaintUtils.trust("src");
    TaintUtils.taint(pathStr);
    String testPath = userDir + File.separator + pathStr;

    Path path = fs.getPath(pathStr);
    String filepath = path.toFile().getPath();
    Assert.assertTrue("file path should be tainted", TaintUtils.isTainted(filepath));

    File f3 = new File(path.toFile().getAbsolutePath());
    filepath = f3.getPath();
    Assert.assertTrue("file paths metadata did not match expected result",
        TaintUtils.hasEqualTaint(testPath, filepath, TaintValues.TRUST_MASK));

    File cf = path.toFile().getAbsoluteFile();
    filepath = cf.getPath();
    Assert.assertTrue("file paths metadata did not match expected result",
        TaintUtils.hasEqualTaint(testPath, filepath, TaintValues.TRUST_MASK));

    filepath = cf.getAbsolutePath();
    Assert.assertTrue("file paths metadata did not match expected result",
        TaintUtils.hasEqualTaint(testPath, filepath, TaintValues.TRUST_MASK));
  }

  @Test
  public void fileTest() {
    FileSystem fs = FileSystems.getDefault();

    Path path = null, path_2 = null;
    String pathName, pathName_2;
    boolean caughtEx;

    // ***
    // Test constructor File(String)
    // Should replace ../test with __/test
    pathName = TaintUtils.trust("../test");
    TaintUtils.taint(pathName);
    try {
      path = fs.getPath(pathName);
    } catch (RuntimeException ex) {
      Assert.fail("File(String) threw unexpected RuntimeException");
    }

    Assert.assertTrue("File(String) The string is tainted but the returned File is not tainted",
        TaintUtils.isTainted(path.toFile().getPath()));
    Assert.assertTrue("File(String) did not replace untrusted attack ../test",
        path.toFile().getPath().equals("__/test"));
    // ***

    // ***
    // Test constructor File("test") - "test" is trusted. Test that returned File is tainted. That
    // File.toString() is tainted.
    pathName = TaintUtils.trust("test");
    try {
      path = fs.getPath(pathName);
    } catch (RuntimeException ex) {
      Assert.fail("File(String) threw unexpected RuntimeException");
    }

    // Assert.assertFalse("File(String) The String is trusted but the returned File is tainted",
    // TaintValues.isTainted(file));
    Assert.assertFalse("File(String) The String is trusted but path.toFile().toString() is tainted",
        TaintUtils.isTainted(path.toFile().toString()));
    // ***

    // ***
    // Must look in config_inst file to insure that File name conversion happened
    // Test that .../.../test => ___/___/test
    // etc
    pathName = TaintUtils.trust(".../.../test");
    TaintUtils.taint(pathName);
    try {
      path = fs.getPath(pathName);
    } catch (RuntimeException ex) {
      Assert.fail("File(String) threw unexpected RuntimeException");
    }
    pathName = path.toFile().getPath();
    Assert.assertTrue("File(String) The String is untrusted but the returned File is not tainted",
        TaintUtils.isTainted(pathName));
    Assert.assertTrue("File(String) attack data \".../.../test was not converted",
        "___/___/test".equals(pathName));

    // ***

    // ***
    // test constructor File(File, String)
    // file is ___/___/test from the above test
    // "src///csail" is untrusted attack to be replaced with "src___csail"
    pathName = TaintUtils.trust("/src/////csail");
    TaintUtils.taint(pathName);
    try {
      path_2 = path.resolve(pathName);
    } catch (RuntimeException ex) {
      Assert.fail("File(File, String) threw unexpected RuntimeException");
    }
    pathName = path_2.toFile().getPath();

    Assert.assertTrue("File(File, String) failed to taint File whose path is untrusted",
        TaintUtils.isTainted(path_2.toFile().getPath()));
    Assert.assertTrue("File(File, String) failed to replace untrusted attack path",
        pathName.equals("_src/csail"));
    // ***

    // ***
    // Test constructor File(File, String) that the returned File is trusted because it refers to
    // untrusted location.
    pathName = TaintUtils.trust("/usr/bin");
    path = fs.getPath(pathName);
    pathName = TaintUtils.trust("fax");
    try {
      path = path.resolve(pathName);
    } catch (RuntimeException ex) {
      Assert.fail("File(File, String) threw unexpected exception");
    }
    Assert.assertFalse("File(File, String) incorrectly tainted File having a trusted path",
        TaintUtils.isTainted(path.toFile().getPath()));
    Assert.assertTrue("File(File, String) incorrectly altered path",
        "/usr/bin/fax".equals(path.toFile().toString()));

    pathName = TaintUtils.trust("/usr/bin");
    TaintUtils.taint(pathName);
    path = fs.getPath(pathName);
    pathName = TaintUtils.trust("fax");
    try {
      path = path.resolve(pathName);
    } catch (RuntimeException ex) {
      Assert.fail("File(File, String) threw unexpected exception");
    }
    Assert.assertTrue(
        "File(File, String) File param is tainted, so returned File should be tainted",
        TaintUtils.isTainted(path.toFile().getPath()));
    Assert.assertTrue("File(File, String) failed to alter path",
        "_usr/bin/fax".equals(path.toFile().toString()));

    pathName = TaintUtils.trust("/usr/bin");
    path = fs.getPath(pathName);
    pathName = TaintUtils.trust("fax");
    TaintUtils.taint(pathName);
    try {
      path = path.resolve(pathName);
    } catch (RuntimeException ex) {
      Assert.fail("File(File, String) threw unexpected exception");
    }
    Assert.assertTrue("File(File, String) altered taint of File",
        TaintUtils.isTrusted(path.toFile().toString(), 0, "/usr/bin".length() - 1));
    Assert.assertTrue(
        "File(File, String) String param was tainted. Should remain tainted in returned File.toString()",
        TaintUtils.isTainted(path.toFile().toString(), "/usr/bin/".length(), // start
            path.toFile().toString().length() - 1)); // end
    Assert.assertTrue(
        "File(File, String) String param is tainted, so returned File should be tainted",
        TaintUtils.isTainted(path.toFile().getPath()));
    Assert.assertTrue("File(File, String) incorrectly altered path",
        "/usr/bin/fax".equals(path.toFile().toString()));
    // ***

    // ***
    // Test constructor File(String, String) that no replacements are made that untrusted path
    // caused File to be tainted
    pathName = TaintUtils.trust("~/Public");
    pathName_2 = TaintUtils.trust("/afs/csail");
    TaintUtils.taint(pathName_2);
    try {
      path_2 = fs.getPath(pathName, pathName_2);
    } catch (RuntimeException ex) {
      Assert.fail("File(String, String) threw unexpected RuntimeException");
    }
    Assert.assertTrue("File(String, String) failed to taint File of untrusted path",
        TaintUtils.isTainted(path_2.toFile().getPath()));
    Assert.assertTrue("File(String, String) incorrecttly altered path",
        path_2.toFile().getPath().equals("~/Public/afs/csail"));
    // ***

    // ***
    // Test constructor File(String, String) that trusted path location causes returned File to be
    // trusted.
    pathName = TaintUtils.trust("/usr/bin");
    pathName_2 = TaintUtils.trust("fax");
    caughtEx = false;
    try {
      path = fs.getPath(pathName, pathName_2);
    } catch (RuntimeException ex) {
      caughtEx = true;
    }
    Assert.assertFalse("File(String, String) threw unexpected RuntimeException", caughtEx);
    Assert.assertFalse("File(String, String) failed to trust File path location",
        TaintUtils.isTainted(path.toFile().getPath()));
    Assert.assertTrue("File(String, String) incorrecttly altered path",
        path.toFile().getPath().equals("/usr/bin/fax"));
    // ***

    // ***
    // Test constructor File(String, String) that untrusted tilde is replaced that untrusted path
    // taints returned File.
    pathName = TaintUtils.trust("~/Public");
    TaintUtils.taint(pathName);
    pathName_2 = TaintUtils.trust("/afs/csail");
    TaintUtils.taint(pathName_2);
    try {
      path_2 = fs.getPath(pathName, pathName_2);
    } catch (RuntimeException ex) {
      Assert.fail("File(String, String) threw unexpected RuntimeException");
    }
    Assert.assertTrue("File(String, String) failed to taint File of untrusted path",
        TaintUtils.isTainted(path_2.toFile().getPath()));
    Assert.assertTrue("File(String, String) failed to replace untrusted attack paths",
        path_2.toFile().getPath().equals("_/Public/afs/csail"));
    // ***

    // ***
    // Test that taint of File.getParent():
    //                                     ______________
    pathName = TaintUtils.trust("/afs/csail/system/common");
    pathName = TaintUtils.taint(pathName, 10, pathName.length() - 1);
    path = fs.getPath(pathName);
    Assert.assertTrue("File(String) is an untrusted location. But did not return an untrusted File",
        TaintUtils.isTainted(path.toFile().getPath()));
    pathName = path.toFile().getParent();
    Assert.assertTrue(
        "File.getParent() is an untrusted location. But did not return an untrusted File",
        TaintUtils.isTainted(pathName));
    Assert.assertTrue("File.getParent() failed to return a String with the proper trusted chars",
        TaintUtils.isTrusted(pathName, 0, 9));
    Assert.assertTrue("File.getParent() failed to return a String with the proper trusted chars",
        TaintUtils.isTainted(pathName, 10, 16));
    Assert.assertTrue("File.getParent() returned incorrect parent String",
        "/afs/csail/system".equals(pathName));

    pathName = TaintUtils.trust("config_inst");
    TaintUtils.taint(pathName);
    path = fs.getPath(pathName);
    pathName = path.toFile().getParent();
    Assert.assertTrue("File.parent() should return null as parent of a leaf", pathName == null);

    pathName = TaintUtils.trust("/usr/bin/fax");
    path = fs.getPath(pathName);
    Assert.assertFalse("File(String) is a trusted location. But returned a tainted File",
        TaintUtils.isTainted(path.toFile().getPath()));
    path_2 = path.toFile().getParentFile().toPath();
    Assert.assertFalse(
        "File.getParetnFile() refers to a trusted location. But returned a tainted File",
        TaintUtils.isTainted(path_2.toFile().getPath()));
    // ***

    // ***
    // Test that taint is copied in File.getAbsolutePath():
    //                                       ____________
    pathName = TaintUtils.trust("/afs/csail/system/common");
    pathName = TaintUtils.taint(pathName, 12, pathName.length() - 1);
    path = fs.getPath(pathName);
    pathName = path.toFile().getAbsolutePath();
    Assert.assertTrue(
        "File.getAbsolutePath() failed to return a String with the proper trusted chars",
        TaintUtils.isTrusted(pathName, 0, 9));
    Assert.assertTrue(
        "File.getAbsolutePath() failed to return a String with the proper trusted chars",
        TaintUtils.isTainted(pathName, 10, 23));

    pathName = TaintUtils.trust("config_inst");
    int pathName_len = pathName.length();
    TaintUtils.taint(pathName);
    path = fs.getPath(pathName);
    pathName = path.toFile().getAbsolutePath();
    Assert.assertTrue(
        "File.getAbsolutePath() failed to return a String with the proper trusted chars",
        TaintUtils.isTrusted(pathName, 0, pathName.length() - pathName_len - 2));
    Assert.assertTrue(
        "File.getAbsolutePath() failed to return a String with the proper trusted chars",
        TaintUtils.isTainted(pathName, pathName.length() - pathName_len - 1, // start
            pathName.length() - 1)); // end
    // ***

    // ***
    // Test that File.getParentFile() retains taint info:
    //                                     ______________
    pathName = TaintUtils.trust("/afs/csail/system/common");
    pathName = TaintUtils.taint(pathName, 10, pathName.length() - 1);
    path = fs.getPath(pathName);
    path_2 = path.toFile().getParentFile().toPath();
    pathName = path_2.toFile().toString();
    Assert.assertTrue("File.getParentFile() failed to retain taint info",
        TaintUtils.isTrusted(pathName, 0, pathName.length() - 8));
    Assert.assertTrue("File.getParentFile() failed to retain taint info",
        TaintUtils.isTainted(pathName, 10, 16));
    pathName = "config_inst";
    TaintUtils.taint(pathName);
    path = fs.getPath(pathName);
    // path_2 = path.toFile().getParentFile().toPath();
    // Assert.assertTrue("File.getParentFile() where file is a leaf failed to return null",
    // path_2 == null);
    // ***

    // ***
    // Test File.getCanonicalPath():
    pathName = TaintUtils.trust("config_inst");
    path = fs.getPath(pathName);
    try {
      pathName = path.toFile().getCanonicalPath(); // IOException
      // show_metadata("getCanonicalPath()", pathName);
      Assert.assertFalse(
          "File(\"trusted_path\").getCanonicalPath should not return untrusted String",
          TaintUtils.isTainted(path.toFile().getPath())); // (pathName, pathName.length()));
    } catch (IOException ex) {
      Assert.fail("File.getCanonicalPath() threw unexpected IOException\n" + ex.getMessage());
    }

    pathName = TaintUtils.trust("bin");
    TaintUtils.taint(pathName);
    path = fs.getPath(pathName);
    try {
      pathName = path.toFile().getCanonicalPath(); // IOException
      // show_metadata("getCanonicalPath()", pathName);
      Assert.assertTrue("File(\"untrusted_path\").getCanonicalPath should return untrusted String",
          TaintUtils.isTainted(pathName));
    } catch (IOException ex) {
      Assert.fail("File.getCanonicalPath() threw unexpected IOException\n" + ex.getMessage());
    }
    // ***

    // ***
    pathName_2 = TaintUtils.trust("/afs/csail");
    pathName = TaintUtils.trust("lib");
    path = fs.getPath(pathName);
    try {
      path_2 = path.toFile().getCanonicalFile().toPath(); // IOException
      pathName = path_2.toFile().toString();
      Assert.assertTrue("File(\"trusted_path\").getCanonicalFile should return trusted String",
          TaintUtils.isTrusted(pathName));
      // show_metadata("getCononicalFile", pathName);
    } catch (IOException ex) {
      Assert.fail("File.getCanonicalFile() threw unexpected IOException\n" + ex.getMessage());
    }
    // ***

    // ***
    int i;
    pathName = TaintUtils.trust("/usr");
    path = fs.getPath(pathName);
    final String[] list = path.toFile().list();
    for (i = 0; i < list.length; i++) {
      final String s = list[i];
      Assert.assertTrue("File.list returned a file name string that is not trusted",
          TaintUtils.isTrusted(s));
    }
    // ***

    // ***
    // Test that File.listFiles():
    pathName = TaintUtils.trust("src");
    TaintUtils.taint(pathName);
    path = fs.getPath(pathName);
    File[] files = path.toFile().listFiles();
    for (i = 0; i < files.length; i++) {
      path_2 = files[i].toPath();
      String toStr = path_2.toFile().toString();
      Assert.assertFalse(
          "File.ListFiles returned a File whose toString should be part trusted part untrusted",
          TaintUtils.isTrusted(toStr));

      toStr = path_2.toFile().getAbsolutePath();
      Assert.assertFalse(
          "File.ListFiles returned a File whose getAbsolutePath should be part trusted part untrusted but was not",
          TaintUtils.isTrusted(toStr));

      toStr = path.toFile().getName();
      Assert.assertFalse(
          "new File(\"untrusted_leaf\").ListFiles returned a File whose getName should be untrust but was not",
          TaintUtils.isTrusted(toStr));
    }

    // Test File.listFiles(FilenameFilter):
    pathName = TaintUtils.trust("src");
    TaintUtils.taint(pathName);
    path = fs.getPath(pathName);
    final FnameFilter fnameFilter = new FnameFilter();
    files = path.toFile().listFiles(fnameFilter);
    for (i = 0; i < files.length; i++) {
      path_2 = files[i].toPath();
      String toStr = path_2.toFile().toString();
      Assert.assertFalse(
          "File.ListFiles((FilenameFilter) returned a File whose toString should be part trusted part untrusted",
          TaintUtils.isTrusted(toStr));

      toStr = path_2.toFile().getAbsolutePath();
      Assert.assertFalse(
          "File.ListFiles((FilenameFilter) returned a File whose getAbsolutePath should be part trusted part untrusted but was not",
          TaintUtils.isTrusted(toStr));

      toStr = path.toFile().getName();
      Assert.assertFalse(
          "new File(\"untrusted_leaf\").ListFiles(FilenameFilter) returned a File whose getName should be untrust but was not",
          TaintUtils.isTrusted(toStr));
    }

    // Test File.listFiles(FileFilter):
    pathName = TaintUtils.trust("src");
    TaintUtils.taint(pathName);
    path = fs.getPath(pathName);
    final FFilter fFilter = new FFilter();
    files = path.toFile().listFiles(fFilter);
    for (i = 0; i < files.length; i++) {
      path_2 = files[i].toPath();
      String toStr = path_2.toFile().toString();
      Assert.assertTrue(
          "File.ListFiles(FileFilter) returned a File whose toString is " + toStr + "  "
              + pathName.substring(0, 3) + " should be tainted.",
          TaintUtils.isTainted(toStr, 0, pathName.length()));

      Assert.assertTrue(
          "File.ListFiles(FileFilter) returned a File whose toString is " + toStr + "  "
              + pathName.substring(0, 2) + " should be trusted. The remainder should be untrusted",
          TaintUtils.isTrusted(toStr, pathName.length(), toStr.length() - 1));

      String toStr_2 = path_2.toFile().getAbsolutePath();
      Assert.assertTrue(
          "File.ListFiles(FileFilter).getAbsolutePath() should be a string the first part of which should be trusted",
          TaintUtils.isTrusted(toStr_2, 0, toStr_2.length() - toStr.length() - 2));
      Assert.assertTrue(
          "File.ListFiles(FileFilter).getAbsolutePath() should be a String that is partly untrusted",
          TaintUtils.isTainted(toStr_2, 0, toStr_2.length() - 1));

      toStr = path.toFile().getName();
      Assert.assertFalse(
          "new File(\"untrusted_leaf\").ListFiles(FileFilter) returned a File whose getName should be untrust but was not",
          TaintUtils.isTrusted(toStr));
    }
    // ***

    // ***
    // Do test on File("trusted_leaf") - toString(), getName, getPath(), getAbsolutePath(),
    // getAbsoluteFile().
    String leaf = TaintUtils.trust("config_inst");
    path = fs.getPath(leaf);
    pathName = path.toFile().toString();
    Assert.assertTrue("File(\"trusted\").toString should be trusted",
        TaintUtils.isTrusted(pathName));
    // show_metadata("File.toString", path.toFile().toString());

    pathName = path.toFile().getName();
    Assert.assertTrue("File(\"trusted\").getName should be trusted",
        TaintUtils.isTrusted(pathName));
    // show_metadata("File.getName", path.toFile().getName());

    pathName = path.toFile().getPath();
    Assert.assertTrue("File(\"trusted\").getPath should be trusted",
        TaintUtils.isTrusted(pathName));
    // show_metadata("File.getPath", path.toFile().getPath());

    pathName = path.toFile().getAbsolutePath();
    Assert.assertTrue("File(\"trusted\").getAbsolutePath() should be trusted",
        TaintUtils.isTrusted(pathName));
    // show_metadata("File getAboslutePath", path.toFile().getAbsolutePath());

    File absFile = path.toFile().getAbsoluteFile();
    pathName = absFile.getPath();
    Assert.assertTrue("File(\"trusted\").getAbsoluteFile().getPath() should be trusted",
        TaintUtils.isTrusted(pathName));
    // show_metadata("File.getPath", absFile.getPath());

    pathName = absFile.getParent();
    Assert.assertTrue("File(\"trusted\").getAbosluteFile().getParent() should be trusted",
        TaintUtils.isTrusted(pathName));
    // show_metadata("File.getParent", absFile.getParent());
    // ***

    // ****
    // Do tests on File("untrusted_leaf") - toString(), getName(), getPath(), getAbsolutePath(),
    // getAbsoluteFile().
    TaintUtils.taint(leaf);
    path = fs.getPath(leaf);
    pathName = path.toFile().toString();
    Assert.assertFalse("File(\"trusted\").toString should be untrusted",
        TaintUtils.isTrusted(pathName));
    // show_metadata("File.toString", path.toFile().toString());

    pathName = path.toFile().getName();
    Assert.assertFalse("File(\"trusted\").getName should be untrusted",
        TaintUtils.isTrusted(pathName));
    // show_metadata("File.getName", path.toFile().getName());

    pathName = path.toFile().getPath();
    Assert.assertFalse("File(\"trusted\").getPath should be untrusted",
        TaintUtils.isTrusted(pathName));
    // show_metadata("File.getPath", path.toFile().getPath());

    pathName = path.toFile().getAbsolutePath();
    Assert.assertFalse("File(\"trusted\").getAbsolutePath() should be untrusted",
        TaintUtils.isTrusted(pathName));
    // show_metadata("File getAboslutePath", path.toFile().getAbsolutePath());

    absFile = path.toFile().getAbsoluteFile();
    pathName = absFile.getPath();
    Assert.assertFalse("File(\"trusted\").getAbosluteFile().getPath() should be untrusted",
        TaintUtils.isTrusted(pathName));
    // show_metadata("File.getPath", absFile.getPath());

    pathName = absFile.getParent();
    Assert.assertTrue("File(\"trusted\").getAbosluteFile().getParent() should be trusted",
        TaintUtils.isTrusted(pathName));
    // show_metadata("File.getParent", absFile.getParent());
    // ***

  }

  @Test
  public void sanityTest() {
    Path path = FileSystems.getDefault().getPath(TaintUtils.trust("build.xml"));
    FileInfo expected = new FileInfo(path);
    try {
      Assert.assertEquals("isWritable() calls should match", expected.isWritable(),
          Files.isWritable(path));
      Assert.assertEquals("isReadable() calls should match", expected.isReadable(),
          Files.isReadable(path));
      Assert.assertEquals("isExecutable() calls should match", expected.isExecutable(),
          Files.isExecutable(path));
      Assert.assertEquals("isDirectory() calls should match", expected.isDirectory(),
          Files.isDirectory(path));
      Assert.assertEquals("isRegularFile() calls should match", expected.isRegularFile(),
          Files.isRegularFile(path));
      Assert.assertEquals("exists() calls should match", expected.exists(), Files.exists(path));
      Assert.assertEquals("notExists() calls should match", expected.notExists(),
          Files.notExists(path));
      Assert.assertEquals("isSymbolicLink() calls should match", expected.isSymbolicLink(),
          Files.isSymbolicLink(path));
      Assert.assertEquals("getLastModifiedTime() calls should match",
          expected.getLastModifiedTime(), Files.getLastModifiedTime(path));
      Assert.assertEquals("getPosixFilePermissions() calls should match",
          expected.getPosixFilePermissions(), Files.getPosixFilePermissions(path));
      checkAttribute(expected, path, "dev");
      checkAttribute(expected, path, "rdev");
      checkAttribute(expected, path, "ino");
      checkAttribute(expected, path, "mode");
      // TODO: We need to handle uid and gid first, before I can enable these checks.
      // checkAttribute(expected, path, "uid");
      // checkAttribute(expected, path, "gid");
      checkAttribute(expected, path, "size");
      checkAttribute(expected, path, "ctime");
      checkAttribute(expected, path, "nlink");
    } catch (IOException e) {
      Assert.fail("unexpected IOException on file '" + path + "': " + e);
    }
  }

  private void checkAttribute(FileInfo expected, Path path, String attribute) throws IOException {
    attribute = "unix:" + attribute;
    Assert.assertEquals("getAttribute(\"" + attribute + "\") calls should match",
        expected.getAttribute(attribute), Files.getAttribute(path, attribute));
  }

  private class FFilter implements FileFilter {

    @Override
    public boolean accept(File file) {
      final String fname = file.getName();
      boolean ans = "pac".equals(fname);
      return ans;
    }

  }

  private class FnameFilter implements FilenameFilter {

    @Override
    public boolean accept(File dir, String name) {
      boolean ans = "pac".equals(name);
      return ans;
    }

  }

  // Leave this class uninstrumented so that we can compare Java's answer with our instrumented
  // answer (i.e. fstat).
  class FileInfo {
    Path path;

    public FileInfo(Path path) {
      this.path = path;
    }

    public boolean isReadable() {
      return Files.isReadable(path);
    }

    public boolean isWritable() {
      return Files.isWritable(path);
    }

    public boolean isExecutable() {
      return Files.isExecutable(path);
    }

    public boolean isDirectory() {
      return Files.isDirectory(path);
    }

    public boolean isRegularFile() {
      return Files.isRegularFile(path);
    }

    public boolean exists() {
      return Files.exists(path);
    }

    public boolean notExists() {
      return Files.notExists(path);
    }

    public boolean isSymbolicLink() {
      return Files.isSymbolicLink(path);
    }

    public FileTime getLastModifiedTime() throws IOException {
      return Files.getLastModifiedTime(path);
    }

    public Set<PosixFilePermission> getPosixFilePermissions() throws IOException {
      return Files.getPosixFilePermissions(path);
    }

    public Object getAttribute(String attribute) throws IOException {
      return Files.getAttribute(path, attribute);
    }
  }

}
