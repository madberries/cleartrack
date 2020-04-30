package pac.test;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import pac.util.TaintUtils;
import pac.util.TaintValues;

public class FileInstrumentationTest {

    public static void main(String[] args) {
        new FileInstrumentationTest().tempFileTest();
    }

    @Test
    public void tempFileTest() {
        File tempFile = null;
        String tmpDir = System.getProperty("java.io.tmpdir");
        System.out.println("tmp dir = \n" + TaintUtils.createTaintDisplayLines(tmpDir));
        String trustPrefix = TaintUtils.trust("trustedPrefix");
        String taintPrefix = TaintUtils.taint("taintedPrefix");
        String trustSuffix = TaintUtils.trust(".tmp-trusted");
        String taintSuffix = TaintUtils.taint(".tmp-tainted");

        // Try all combinations of tainted/trusted prefixes and suffixes...

        try {
            tempFile = File.createTempFile(trustPrefix, trustSuffix);
            tempFile.deleteOnExit();
            ensureTaintEquals(tempFile, tmpDir, trustPrefix, trustSuffix);
        } catch (IOException e) {
            Assert.fail("threw an IOException while try to create file " + tempFile + ": " + e);
        }

        try {
            tempFile = File.createTempFile(trustPrefix, taintSuffix);
            tempFile.deleteOnExit();
            ensureTaintEquals(tempFile, tmpDir, trustPrefix, taintSuffix);
        } catch (IOException e) {
            Assert.fail("threw an IOException while try to create file " + tempFile + ": " + e);
        }

        try {
            tempFile = File.createTempFile(taintPrefix, trustSuffix);
            tempFile.deleteOnExit();
            ensureTaintEquals(tempFile, tmpDir, taintPrefix, trustSuffix);
        } catch (IOException e) {
            Assert.fail("threw an IOException while try to create file " + tempFile + ": " + e);
        }

        try {
            tempFile = File.createTempFile(taintPrefix, taintSuffix);
            tempFile.deleteOnExit();
            ensureTaintEquals(tempFile, tmpDir, taintPrefix, taintSuffix);
        } catch (IOException e) {
            Assert.fail("threw an IOException while try to create file " + tempFile + ": " + e);
        }

        // Now retry the combinations, when specifying a tainted temporary directory...

        File taintedTemp = new File(TaintUtils.trust("/tmp/"));
        taintedTemp = new File(taintedTemp, TaintUtils.taint("ss_tainted/"));
        if (!taintedTemp.exists()) {
            if (!taintedTemp.mkdir()) {
                Assert.fail("unabled to create temporary directory: " + taintedTemp);
            }
        }
        taintedTemp.deleteOnExit();
        tmpDir = taintedTemp.toString() + File.separatorChar;
        System.out.println("tmp dir = \n" + TaintUtils.createTaintDisplayLines(tmpDir));

        try {
            tempFile = File.createTempFile(trustPrefix, trustSuffix, taintedTemp);
            tempFile.deleteOnExit();
            ensureTaintEquals(tempFile, tmpDir, trustPrefix, trustSuffix);
        } catch (IOException e) {
            Assert.fail("threw an IOException while try to create file " + tempFile + ": " + e);
        }

        try {
            tempFile = File.createTempFile(trustPrefix, taintSuffix, taintedTemp);
            tempFile.deleteOnExit();
            ensureTaintEquals(tempFile, tmpDir, trustPrefix, taintSuffix);
        } catch (IOException e) {
            Assert.fail("threw an IOException while try to create file " + tempFile + ": " + e);
        }

        try {
            tempFile = File.createTempFile(taintPrefix, trustSuffix, taintedTemp);
            tempFile.deleteOnExit();
            ensureTaintEquals(tempFile, tmpDir, taintPrefix, trustSuffix);
        } catch (IOException e) {
            Assert.fail("threw an IOException while try to create file " + tempFile + ": " + e);
        }

        try {
            tempFile = File.createTempFile(taintPrefix, taintSuffix, taintedTemp);
            tempFile.deleteOnExit();
            ensureTaintEquals(tempFile, tmpDir, taintPrefix, taintSuffix);
        } catch (IOException e) {
            Assert.fail("threw an IOException while try to create file " + tempFile + ": " + e);
        }
    }

    private void ensureTaintEquals(File tempFile, String tmpDir, String prefix, String suffix) {
        String path = tempFile.toString();
        String pathPrefix = new File(tmpDir, prefix).toString();
        int start = path.indexOf(pathPrefix);
        Assert.assertEquals("path prefix '" + pathPrefix + "' not found, or is not a prefix of '" + path + "'", 0,
                            start);
        int end = path.indexOf(suffix);
        Assert.assertFalse("path suffix '" + suffix + "' not found, or is not a suffix of '" + path + "'",
                           start < 0 || !path.endsWith(suffix));
        String randNum = path.substring(pathPrefix.length(), end);
        String actualPath = pathPrefix + randNum + suffix;
        Assert.assertEquals("strings should equal: " + path + " != " + actualPath, path, actualPath);
        System.out.println("taint of expected path:\n" + TaintUtils.createTaintDisplayLines(path));
        System.out.println("taint of actual path:\n" + TaintUtils.createTaintDisplayLines(actualPath));
        // One of the slashes appears to come from a property, where the other
        // is a string constant (and therefore has no taint source).  So, let's
        // just compare with a taint mask.
        Assert.assertTrue("taint of '" + path + "' and '" + actualPath + "' does not match",
                          TaintUtils.hasEqualTaint(path, actualPath, TaintValues.TRUST_MASK));
    }

    @Test
    public void fileTraversalTest() {
        String str = null;
        File file = null;

        /*
         * The following are tests for valid path
         * traversals according to the new policy set by
         * MITRE...
         */

        final String CWD = new File("").getAbsolutePath();

        // BENIGN PATHS...

        str = "file.txt";
        TaintUtils.taint(str);
        file = new File(str);
        Assert.assertEquals(str, file.getPath());

        str = "benign/file.txt";
        TaintUtils.taint(str);
        file = new File(str);
        Assert.assertEquals(str, file.getPath());

        str = "benign/../file.txt";
        TaintUtils.taint(str);
        file = new File(str);
        Assert.assertEquals(str, file.getPath());

        str = TaintUtils.trust("/some/trusted/path/file.txt");
        str = TaintUtils.mark(str, TaintValues.TAINTED, 19, str.length() - 1);
        file = new File(str);
        Assert.assertEquals(str, file.getPath());

        str = TaintUtils.trust("/some/trusted/path/x/../file.txt");
        str = TaintUtils.mark(str, TaintValues.TAINTED, 19, str.length() - 1);
        file = new File(str);
        Assert.assertEquals(str, file.getPath());

        str = TaintUtils.trust("/some/trusted/path/some/dir/../bob/../bob");
        str = TaintUtils.mark(str, TaintValues.TAINTED, 19, 30);
        file = new File(str);
        Assert.assertEquals(str, file.getPath());

        str = TaintUtils.trust("./janedoe/../config.properties"); // doesn't go above the CWD
        str = TaintUtils.mark(str, TaintValues.TAINTED, 2, 8);
        str = TaintUtils.mark(str, TaintValues.TAINTED, 10, str.length() - 1);
        file = new File(str);
        Assert.assertEquals(str, file.getPath());

        // ATTACK PATHS...

        str = "../file.txt";
        TaintUtils.taint(str);
        file = new File(str);
        Assert.assertEquals("__/file.txt", file.getPath());

        str = TaintUtils.trust(CWD + "/../joe/file.txt");
        str = TaintUtils.mark(str, TaintValues.TAINTED, CWD.length() + 1, str.length() - 1);
        file = new File(str);
        Assert.assertEquals(CWD + "/__/joe/file.txt", file.getPath());

        str = TaintUtils.trust("/some/trusted/path/x/../../joe/file.txt");
        str = TaintUtils.mark(str, TaintValues.TAINTED, 19, str.length() - 1);
        file = new File(str);
        Assert.assertEquals("/some/trusted/path/x/__/__/joe/file.txt", file.getPath());

        str = TaintUtils.trust(CWD + "/some/dir/../bob/../bob/../../..");
        str = TaintUtils.mark(str, TaintValues.TAINTED, CWD.length() + 1, CWD.length() + 11);
        str = TaintUtils.mark(str, TaintValues.TAINTED, CWD.length() + 24, str.length() - 1);
        file = new File(str);
        Assert.assertEquals(CWD + "/some/dir/__/bob/../bob/__/__/__", file.getPath());

        str = TaintUtils.trust("/some/trusted/path/some/dir/../bob/../bob/../..");
        str = TaintUtils.mark(str, TaintValues.TAINTED, 19, 30);
        str = TaintUtils.mark(str, TaintValues.TAINTED, 42, str.length() - 1);
        file = new File(str);
        Assert.assertEquals("/some/trusted/path/some/dir/__/bob/../bob/__/__", file.getPath());

        str = TaintUtils.trust("/some/trusted/path/some/dir/../bob/../bob/..");
        str = TaintUtils.mark(str, TaintValues.TAINTED, 19, 30);
        str = TaintUtils.mark(str, TaintValues.TAINTED, 42, str.length() - 1);
        file = new File(str);
        Assert.assertEquals("/some/trusted/path/some/dir/__/bob/../bob/__", file.getPath());

        if (!CWD.contains(" ")) {
            // spaces in filenames sort of invalidates this test since the whitespace
            // is converted to _, and therefore CWD is no longer CWD...
            String child = new File(CWD).getName();
            str = TaintUtils.trust(CWD + "/../" + child + "/file.txt");
            str = TaintUtils.mark(str, TaintValues.TAINTED, CWD.length(), str.length() - 1);
            file = new File(str);
            Assert.assertEquals(CWD + "/__/" + child + "/file.txt", file.getPath());
        }

        str = TaintUtils.trust("./janedoe/../johndoe/birthcertificate.txt");
        str = TaintUtils.mark(str, TaintValues.TAINTED, 10, str.length() - 1);
        file = new File(str);
        Assert.assertEquals("./janedoe/__/johndoe/birthcertificate.txt", file.getPath());

        str = TaintUtils.trust("./janedoe//../johndoe/birthcertificate.txt");
        str = TaintUtils.mark(str, TaintValues.TAINTED, 10, str.length() - 1);
        file = new File(str);
        Assert.assertEquals("./janedoe/__/johndoe/birthcertificate.txt", file.getPath());

        str = TaintUtils.trust("./janedoe/./../johndoe/birthcertificate.txt");
        str = TaintUtils.mark(str, TaintValues.TAINTED, 10, str.length() - 1);
        file = new File(str);
        Assert.assertEquals("./janedoe/./__/johndoe/birthcertificate.txt", file.getPath());
    }

    @Test
    public void absolutePathTest() {

        // tainted attack paths not within the CWD...

        String path = "/etc/dir1";
        int len = path.length();
        TaintUtils.taint(path);
        Assert.assertEquals("tainted path not in the CWD should have replaced leading slash with _",
                            new File(path).toString(), "_etc/dir1");

        path = TaintUtils.trust("/etc/dir1/file");
        path = TaintUtils.mark(path, TaintValues.TAINTED, 0, len);
        Assert.assertEquals("tainted path not in the CWD should have replaced leading slash with _",
                            new File(path).toString(), "_etc/dir1/file");

        path = TaintUtils.trust("/etc/dir1/dir2/../file");
        path = TaintUtils.mark(path, TaintValues.TAINTED, 0, len);
        Assert.assertEquals("tainted path not in the CWD should have replaced leading slash with _",
                            new File(path).toString(), "_etc/dir1/dir2/../file");

        // tainted benign paths within the CWD...

        path = "dir1/dir2";
        len = path.length();
        TaintUtils.taint(path);
        Assert.assertEquals("tainted path in the CWD should not have replaced leading slash with _",
                            new File(path).toString(), path);

        path = TaintUtils.trust("dir1/dir2/file");
        path = TaintUtils.mark(path, TaintValues.TAINTED, 0, len);
        Assert.assertEquals("tainted path in the CWD should not have replaced leading slash with _",
                            new File(path).toString(), path);

        path = TaintUtils.trust("dir1/dir2/../file");
        path = TaintUtils.mark(path, TaintValues.TAINTED, 0, len);
        Assert.assertEquals("tainted path in the CWD should not have replaced leading slash with _",
                            new File(path).toString(), path);

        final String CWD = new String(new File("").getAbsolutePath().toCharArray());
        TaintUtils.taint(CWD);

        // shame on you if you use whitespace in file names!!!
        if (!CWD.contains(" ")) {
            // spaces in filenames sort of invalidates this test since the whitespace
            // is converted to _, and therefore CWD is no longer CWD...
            path = CWD + TaintUtils.trust("/dir1/dir2");
            Assert.assertEquals("tainted path in the CWD should not have replaced leading slash with _",
                                new File(path).toString(), path);

            path = CWD + TaintUtils.trust("/dir1/dir2/file");
            Assert.assertEquals("tainted path in the CWD should not have replaced leading slash with _",
                                new File(path).toString(), path);

            path = CWD + TaintUtils.trust("/dir1/dir2/../file");
            Assert.assertEquals("tainted path in the CWD should not have replaced leading slash with _",
                                new File(path).toString(), path);

            String suffix = "/dir3/../file.txt";
            TaintUtils.taint(suffix);
            path = CWD + TaintUtils.trust("/dir1/dir2") + suffix;
            Assert.assertEquals("tainted path in the CWD should not have replaced leading slash with _",
                                new File(path).toString(), path);

            suffix = "/dir3/../dir4/dir5/../../file.txt";
            TaintUtils.taint(suffix);
            path = CWD + TaintUtils.trust("/dir1/dir2") + suffix;
            Assert.assertEquals("tainted path in the CWD should not have replaced leading slash with _",
                                new File(path).toString(), path);

            // tainted attack paths within the CWD...

            suffix = "/dir3/../dir4/../../file.txt";
            TaintUtils.taint(suffix);
            path = CWD + TaintUtils.trust("/dir1/dir2") + suffix;
            Assert.assertEquals("tainted path in the CWD should not have replaced leading slash with _",
                                new File(path).toString(), path.replaceAll("\\.\\.", "__"));

            suffix = "/dir3/../../dir4/../../file.txt";
            TaintUtils.taint(suffix);
            path = CWD + TaintUtils.trust("/dir1/dir2") + suffix;
            Assert.assertEquals("tainted path in the CWD should not have replaced leading slash with _",
                                new File(path).toString(), path.replaceAll("\\.\\.", "__"));

            suffix = "/dir3/../../dir4/../file.txt";
            TaintUtils.taint(suffix);
            path = CWD + TaintUtils.trust("/dir1/dir2") + suffix;
            Assert.assertEquals("tainted path in the CWD should not have replaced leading slash with _",
                                new File(path).toString(), path.replaceAll("\\.\\.", "__"));
        }
    }

    @Test
    public void canonicalFileTest() throws IOException {
        String userDir = System.getProperty("user.dir");

        File f = new File(userDir);
        String filepath = f.getPath();
        Assert.assertTrue("file path was not trusted", TaintUtils.isTrusted(filepath, 0, filepath.length() - 1));

        String path = "src";
        TaintUtils.taint(path);
        String testPath = userDir + File.separator + path;

        File f2 = new File(f, path);
        filepath = f2.getPath();
        Assert.assertTrue("file paths metadata did not match expected result",
                          TaintUtils.hasEqualTaint(testPath, filepath, TaintValues.TRUST_MASK));

        File f3 = new File(f2.getCanonicalPath());
        filepath = f3.getPath();
        Assert.assertTrue("file paths metadata did not match expected result",
                          TaintUtils.hasEqualTaint(testPath, filepath, TaintValues.TRUST_MASK));

        File cf = f2.getCanonicalFile();
        filepath = cf.getPath();
        Assert.assertTrue("file paths metadata did not match expected result",
                          TaintUtils.hasEqualTaint(testPath, filepath, TaintValues.TRUST_MASK));

        filepath = cf.getCanonicalPath();
        Assert.assertTrue("file paths metadata did not match expected result",
                          TaintUtils.hasEqualTaint(testPath, filepath, TaintValues.TRUST_MASK));
    }

    @Test
    public void canonicalFileTest2() throws IOException {
        String path = TaintUtils.trust("src");
        TaintUtils.taint(path);

        File f2 = new File(path);
        String filepath = f2.getPath();
        Assert.assertTrue("file path should be tainted", TaintUtils.isTainted(filepath));

        File f3 = new File(f2.getCanonicalPath());
        filepath = f3.getPath();
        Assert.assertTrue("file path based on partially tainted canonical " + "path should at least be tainted",
                          TaintUtils.isTainted(filepath));

        File cf = f2.getCanonicalFile();
        filepath = cf.getPath();
        Assert.assertTrue("file path based on partially tainted canonical " + "path should at least be tainted",
                          TaintUtils.isTainted(filepath));

        filepath = cf.getCanonicalPath();
        Assert.assertTrue("file path based on partially tainted canonical " + "path should at least be tainted",
                          TaintUtils.isTainted(filepath));
    }

    @Test
    public void absoluteFileTest() throws IOException {
        String userDir = System.getProperty("user.dir");

        File f = new File(userDir);
        String filepath = f.getPath();
        Assert.assertTrue("file path was not trusted", TaintUtils.isTrusted(filepath, 0, filepath.length() - 1));

        String path = TaintUtils.trust("/src");
        TaintUtils.taint(path);
        String testPath = userDir + File.separator + path.substring(1);

        File f2 = new File(f, path);
        filepath = f2.getPath();
        Assert.assertTrue("file paths metadata did not match expected result",
                          TaintUtils.hasEqualTaint(testPath, filepath, TaintValues.TRUST_MASK));

        File f3 = new File(f2.getAbsolutePath());
        filepath = f3.getPath();
        Assert.assertTrue("file paths metadata did not match expected result",
                          TaintUtils.hasEqualTaint(testPath, filepath, TaintValues.TRUST_MASK));

        File cf = f2.getAbsoluteFile();
        filepath = cf.getPath();
        Assert.assertTrue("file paths metadata did not match expected result",
                          TaintUtils.hasEqualTaint(testPath, filepath, TaintValues.TRUST_MASK));

        filepath = cf.getAbsolutePath();
        Assert.assertTrue("file paths metadata did not match expected result",
                          TaintUtils.hasEqualTaint(testPath, filepath, TaintValues.TRUST_MASK));
    }

    @Test
    public void absoluteFileTest2() throws IOException {
        String userDir = System.getProperty("user.dir");

        String path = TaintUtils.trust("src");
        TaintUtils.taint(path);
        String testPath = userDir + File.separator + path;

        File f2 = new File(path);
        String filepath = f2.getPath();
        Assert.assertTrue("file path should be tainted", TaintUtils.isTainted(filepath));

        File f3 = new File(f2.getAbsolutePath());
        filepath = f3.getPath();
        Assert.assertTrue("file paths metadata did not match expected result",
                          TaintUtils.hasEqualTaint(testPath, filepath, TaintValues.TRUST_MASK));

        File cf = f2.getAbsoluteFile();
        filepath = cf.getPath();
        Assert.assertTrue("file paths metadata did not match expected result",
                          TaintUtils.hasEqualTaint(testPath, filepath, TaintValues.TRUST_MASK));

        filepath = cf.getAbsolutePath();
        Assert.assertTrue("file paths metadata did not match expected result",
                          TaintUtils.hasEqualTaint(testPath, filepath, TaintValues.TRUST_MASK));
    }

    @Test
    public void fileTest() {

        File file = null, file_2 = null;
        String pathName, pathName_2;
        boolean caughtEx;

        // ***
        // Test constructor File(String)
        // Should replace ../test with __/test
        pathName = TaintUtils.trust("../test");
        TaintUtils.taint(pathName);
        try {
            file = new File(pathName);
        } catch (RuntimeException ex) {
            Assert.fail("File(String) threw unexpected RuntimeException");
        }

        Assert.assertTrue("File(String) The string is tainted but the returned File is not tainted",
                          TaintUtils.isTainted(file.getPath()));
        Assert.assertTrue("File(String) did not replace untrusted attack ../test", file.getPath().equals("__/test"));
        // ***

        // ***
        // Test constructor File("test") - "test" is trusted. Test that returned File is tainted. That File.toString() is tainted
        pathName = TaintUtils.trust("test");
        try {
            file = new File(pathName);
        } catch (RuntimeException ex) {
            Assert.fail("File(String) threw unexpected RuntimeException");
        }

        //        Assert.assertFalse("File(String) The String is trusted but the returned File is tainted",
        //                            TaintValues.isTainted(file));
        Assert.assertFalse("File(String) The String is trusted but file.toString() is tainted",
                           TaintUtils.isTainted(file.toString()));
        // ***

        // ***
        // Must look in config_inst file to insure that File name conversion happened
        // Test that .../.../test => ___/___/test
        //           etc
        pathName = TaintUtils.trust(".../.../test");
        TaintUtils.taint(pathName);
        try {
            file = new File(pathName);
        } catch (RuntimeException ex) {
            Assert.fail("File(String) threw unexpected RuntimeException");
        }
        pathName = file.getPath();
        Assert.assertTrue("File(String) The String is untrusted but the returned File is not tainted",
                          TaintUtils.isTainted(pathName));
        Assert.assertTrue("File(String) attack data \".../.../test was not converted", "___/___/test".equals(pathName));

        // ***

        // ***
        // test constructor File(File, String)
        // file is ___/___/test from the above test
        // "src///csail" is untrusted attack to be replaced with "src___csail"
        pathName = TaintUtils.trust("/src/////csail");
        TaintUtils.taint(pathName);
        try {
            file_2 = new File(file, pathName);
        } catch (RuntimeException ex) {
            Assert.fail("File(File, String) threw unexpected RuntimeException");
        }
        pathName = file_2.getPath();

        Assert.assertTrue("File(File, String) failed to taint File whose path is untrusted",
                          TaintUtils.isTainted(file_2.getPath()));
        Assert.assertTrue("File(File, String) failed to replace untrusted attack path",
                          pathName.equals("___/___/test/src____/csail"));
        // ***

        // ***
        // Test constructor File(File, String) that the returned File is trusted because it refers to untrusted location
        pathName = TaintUtils.trust("/usr/bin");
        file = new File(pathName);
        pathName = TaintUtils.trust("fax");
        try {
            file = new File(file, pathName);
        } catch (RuntimeException ex) {
            Assert.fail("File(File, String) threw unexpected exception");
        }
        Assert.assertFalse("File(File, String) incorrectly tainted File having a trusted path",
                           TaintUtils.isTainted(file.getPath()));
        Assert.assertTrue("File(File, String) incorrectly altered path", "/usr/bin/fax".equals(file.toString()));

        pathName = TaintUtils.trust("/usr/bin");
        TaintUtils.taint(pathName);
        file = new File(pathName);
        pathName = TaintUtils.trust("fax");
        try {
            file = new File(file, pathName);
        } catch (RuntimeException ex) {
            Assert.fail("File(File, String) threw unexpected exception");
        }
        Assert.assertTrue("File(File, String) File param is tainted, so returned File should be tainted",
                          TaintUtils.isTainted(file.getPath()));
        Assert.assertTrue("File(File, String) failed to alter path", "_usr/bin/fax".equals(file.toString()));

        pathName = TaintUtils.trust("/usr/bin");
        file = new File(pathName);
        pathName = TaintUtils.trust("fax");
        TaintUtils.taint(pathName);
        try {
            file = new File(file, pathName);
        } catch (RuntimeException ex) {
            Assert.fail("File(File, String) threw unexpected exception");
        }
        Assert.assertTrue("File(File, String) altered taint of File",
                          TaintUtils.isTrusted(file.toString(), 0, "/usr/bin".length() - 1));
        Assert.assertTrue("File(File, String) String param was tainted. Should remain tainted in returned File.toString()",
                          TaintUtils.isTainted(file.toString(), "/usr/bin/".length(), // start
                                               file.toString().length() - 1)); // end
        Assert.assertTrue("File(File, String) String param is tainted, so returned File should be tainted",
                          TaintUtils.isTainted(file.getPath()));
        Assert.assertTrue("File(File, String) incorrectly altered path", "/usr/bin/fax".equals(file.toString()));
        // ***

        // ***
        // test constructor File(String, String) that no replacements are made
        //                                       that untrusted path caused File to be tainted
        pathName = TaintUtils.trust("~/Public");
        pathName_2 = TaintUtils.trust("/afs/csail");
        TaintUtils.taint(pathName_2);
        try {
            file_2 = new File(pathName, pathName_2);
        } catch (RuntimeException ex) {
            Assert.fail("File(String, String) threw unexpected RuntimeException");
        }
        Assert.assertTrue("File(String, String) failed to taint File of untrusted path",
                          TaintUtils.isTainted(file_2.getPath()));
        Assert.assertTrue("File(String, String) incorrecttly altered path",
                          file_2.getPath().equals("~/Public/afs/csail"));
        // ***

        // ***
        // test constructor File(String, String) that trusted path location causes returned File to be trusted
        pathName = TaintUtils.trust("/usr/bin");
        pathName_2 = TaintUtils.trust("fax");
        caughtEx = false;
        try {
            file = new File(pathName, pathName_2);
        } catch (RuntimeException ex) {
            caughtEx = true;
        }
        Assert.assertFalse("File(String, String) threw unexpected RuntimeException", caughtEx);
        Assert.assertFalse("File(String, String) failed to trust File path location",
                           TaintUtils.isTainted(file.getPath()));
        Assert.assertTrue("File(String, String) incorrecttly altered path", file.getPath().equals("/usr/bin/fax"));
        // ***

        // ***
        // test constructor File(String, String) that untrusted tilde is replaced
        //                                       that untrusted path taints returned File
        pathName = TaintUtils.trust("~/Public");
        TaintUtils.taint(pathName);
        pathName_2 = TaintUtils.trust("/afs/csail");
        TaintUtils.taint(pathName_2);
        try {
            file_2 = new File(pathName, pathName_2);
        } catch (RuntimeException ex) {
            Assert.fail("File(String, String) threw unexpected RuntimeException");
        }
        Assert.assertTrue("File(String, String) failed to taint File of untrusted path",
                          TaintUtils.isTainted(file_2.getPath()));
        Assert.assertTrue("File(String, String) failed to replace untrusted attack paths",
                          file_2.getPath().equals("_/Public/afs/csail"));
        // ***

        // ***
        // Test that taint of File.getParent()
        //                    ______________
        pathName = TaintUtils.trust("/afs/csail/system/common");
        pathName = TaintUtils.taint(pathName, 10, 14);
        file = new File(pathName);
        Assert.assertTrue("File(String) is an untrusted location. But did not return an untrusted File",
                          TaintUtils.isTainted(file.getPath()));
        pathName = file.getParent();
        Assert.assertTrue("File.getParent() is an untrusted location. But did not return an untrusted File",
                          TaintUtils.isTainted(pathName));
        Assert.assertTrue("File.getParent() failed to return a String with the proper trusted chars",
                          TaintUtils.isTrusted(pathName, 0, 9));
        Assert.assertTrue("File.getParent() failed to return a String with the proper trusted chars",
                          TaintUtils.isTainted(pathName, 10, 16));
        Assert.assertTrue("File.getParent() returned incorrect parent String", "/afs/csail/system".equals(pathName));

        pathName = TaintUtils.trust("config_inst");
        TaintUtils.taint(pathName);
        file = new File(pathName);
        pathName = file.getParent();
        Assert.assertTrue("File.parent() should return null as parent of a leaf", pathName == null);

        pathName = TaintUtils.trust("/usr/bin/fax");
        file = new File(pathName);
        Assert.assertFalse("File(String) is a trusted location. But returned a tainted File",
                           TaintUtils.isTainted(file.getPath()));
        file_2 = file.getParentFile();
        Assert.assertFalse("File.getParetnFile() refers to a trusted location. But returned a tainted File",
                           TaintUtils.isTainted(file_2.getPath()));
        // ***

        // ***
        // Test that taint is copied in File.getAbsolutePath()
        //                    ______________
        pathName = TaintUtils.trust("/afs/csail/system/common");
        pathName = TaintUtils.taint(pathName, 12, 12);
        file = new File(pathName);
        pathName = file.getAbsolutePath();
        Assert.assertTrue("File.getAbsolutePath() failed to return a String with the proper trusted chars",
                          TaintUtils.isTrusted(pathName, 0, 9));
        Assert.assertTrue("File.getAbsolutePath() failed to return a String with the proper trusted chars",
                          TaintUtils.isTainted(pathName, 10, 23));

        pathName = TaintUtils.trust("config_inst");
        int pathName_len = pathName.length();
        TaintUtils.taint(pathName);
        file = new File(pathName);
        pathName = file.getAbsolutePath();
        Assert.assertTrue("File.getAbsolutePath() failed to return a String with the proper trusted chars",
                          TaintUtils.isTrusted(pathName, 0, pathName.length() - pathName_len - 2));
        Assert.assertTrue("File.getAbsolutePath() failed to return a String with the proper trusted chars",
                          TaintUtils.isTainted(pathName, pathName.length() - pathName_len - 1, // start
                                               pathName.length() - 1)); // end
        // ***

        // ***
        // test that File.getParentFile() retains taint info
        //                    ______________
        pathName = TaintUtils.trust("/afs/csail/system/common");
        pathName = TaintUtils.taint(pathName, 10, 14);
        file = new File(pathName);
        file_2 = file.getParentFile();
        pathName = file_2.toString();
        Assert.assertTrue("File.getParentFile() failed to retain taint info",
                          TaintUtils.isTrusted(pathName, 0, pathName.length() - 8));
        Assert.assertTrue("File.getParentFile() failed to retain taint info", TaintUtils.isTainted(pathName, 10, 16));
        pathName = "config_inst";
        TaintUtils.taint(pathName);
        file = new File(pathName);
        file_2 = file.getParentFile();
        Assert.assertTrue("File.getParentFile() where file is a leaf failed to return null", file_2 == null);
        // ***

        // ***
        // test File.getCanonicalPath()
        pathName = TaintUtils.trust("config_inst");
        file = new File(pathName);
        try {
            pathName = file.getCanonicalPath(); // IOException
            // show_metadata("getCanonicalPath()", pathName);
            Assert.assertFalse("File(\"trusted_path\").getCanonicalPath should not return untrusted String",
                               TaintUtils.isTainted(file.getPath())); // (pathName, pathName.length()));
        } catch (IOException ex) {
            Assert.fail("File.getCanonicalPath() threw unexpected IOException\n" + ex.getMessage());
        }

        pathName = TaintUtils.trust("bin");
        TaintUtils.taint(pathName);
        file = new File(pathName);
        try {
            pathName = file.getCanonicalPath(); // IOException
            // show_metadata("getCanonicalPath()", pathName);
            Assert.assertTrue("File(\"untrusted_path\").getCanonicalPath should return untrusted String",
                              TaintUtils.isTainted(pathName));
        } catch (IOException ex) {
            Assert.fail("File.getCanonicalPath() threw unexpected IOException\n" + ex.getMessage());
        }
        // ***

        // ***
        pathName_2 = TaintUtils.trust("/afs/csail");
        pathName = TaintUtils.trust("lib"); // Note: Can't user "conifg_inst" or "bin" again. Because getCononicalPath will return a path that already
        file = new File(pathName);
        try {
            file_2 = file.getCanonicalFile(); // IOException
            pathName = file_2.toString();
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
        file = new File(pathName);
        final String[] list = file.list();
        for (i = 0; i < list.length; i++) {
            final String s = list[i];
            Assert.assertTrue("File.list returned a file name string that is not trusted", TaintUtils.isTrusted(s));
        }
        // ***

        // ***
        // Test that File.listFiles()
        pathName = TaintUtils.trust("src");
        TaintUtils.taint(pathName);
        file = new File(pathName);
        File[] files = file.listFiles();
        for (i = 0; i < files.length; i++) {
            file_2 = files[i];
            String toStr = file_2.toString();
            Assert.assertFalse("File.ListFiles returned a File whose toString should be part trusted part untrusted",
                               TaintUtils.isTrusted(toStr));

            toStr = file_2.getAbsolutePath();
            Assert.assertFalse("File.ListFiles returned a File whose getAbsolutePath should be part trusted part untrusted but was not",
                               TaintUtils.isTrusted(toStr));

            toStr = file.getName();
            Assert.assertFalse("new File(\"untrusted_leaf\").ListFiles returned a File whose getName should be untrust but was not",
                               TaintUtils.isTrusted(toStr));
        }

        // Test File.listFiles(FilenameFilter)
        pathName = TaintUtils.trust("src");
        TaintUtils.taint(pathName);
        file = new File(pathName);
        final FnameFilter fnameFilter = new FnameFilter();
        files = file.listFiles(fnameFilter);
        for (i = 0; i < files.length; i++) {
            file_2 = files[i];
            String toStr = file_2.toString();
            Assert.assertFalse("File.ListFiles((FilenameFilter) returned a File whose toString should be part trusted part untrusted",
                               TaintUtils.isTrusted(toStr));

            toStr = file_2.getAbsolutePath();
            Assert.assertFalse("File.ListFiles((FilenameFilter) returned a File whose getAbsolutePath should be part trusted part untrusted but was not",
                               TaintUtils.isTrusted(toStr));

            toStr = file.getName();
            Assert.assertFalse("new File(\"untrusted_leaf\").ListFiles(FilenameFilter) returned a File whose getName should be untrust but was not",
                               TaintUtils.isTrusted(toStr));
        }

        // Test File.listFiles(FileFilter)
        pathName = TaintUtils.trust("src");
        TaintUtils.taint(pathName);
        file = new File(pathName);
        final FFilter fFilter = new FFilter();
        files = file.listFiles(fFilter);
        for (i = 0; i < files.length; i++) {
            file_2 = files[i];
            String toStr = file_2.toString();
            Assert.assertTrue("File.ListFiles(FileFilter) returned a File whose toString is " + toStr + "  "
                    + pathName.substring(0, 3) + " should be tainted.",
                              TaintUtils.isTainted(toStr, 0, pathName.length()));

            Assert.assertTrue("File.ListFiles(FileFilter) returned a File whose toString is " + toStr + "  "
                    + pathName.substring(0, 2) + " should be trusted. The remainder should be untrusted",
                              TaintUtils.isTrusted(toStr, pathName.length(), toStr.length() - 1));

            String toStr_2 = file_2.getAbsolutePath();
            Assert.assertTrue("File.ListFiles(FileFilter).getAbsolutePath() should be a string the first part of which should be trusted",
                              TaintUtils.isTrusted(toStr_2, 0, toStr_2.length() - toStr.length() - 2));
            Assert.assertTrue("File.ListFiles(FileFilter).getAbsolutePath() should be a String that is partly untrusted",
                              TaintUtils.isTainted(toStr_2, 0, toStr_2.length() - 1));

            toStr = file.getName();
            Assert.assertFalse("new File(\"untrusted_leaf\").ListFiles(FileFilter) returned a File whose getName should be untrust but was not",
                               TaintUtils.isTrusted(toStr));
        }
        // ***

        // ***
        // Do test on File("trusted_leaf") - toString(), getName, getPath(), getAbsolutePath(), getAbsoluteFile(), 
        String leaf = TaintUtils.trust("config_inst");
        file = new File(leaf);
        pathName = file.toString();
        Assert.assertTrue("File(\"trusted\").toString should be trusted", TaintUtils.isTrusted(pathName));
        // show_metadata("File.toString", file.toString());

        pathName = file.getName();
        Assert.assertTrue("File(\"trusted\").getName should be trusted", TaintUtils.isTrusted(pathName));
        // show_metadata("File.getName", file.getName());

        pathName = file.getPath();
        Assert.assertTrue("File(\"trusted\").getPath should be trusted", TaintUtils.isTrusted(pathName));
        // show_metadata("File.getPath", file.getPath());

        pathName = file.getAbsolutePath();
        Assert.assertTrue("File(\"trusted\").getAbsolutePath() should be trusted", TaintUtils.isTrusted(pathName));
        // show_metadata("File getAboslutePath", file.getAbsolutePath());

        File absFile = file.getAbsoluteFile();
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
        // Do tests on File("untrusted_leaf") - toString(), getName(), getPath(), getAbsolutePath(), getAbsoluteFile()
        TaintUtils.taint(leaf);
        file = new File(leaf);
        pathName = file.toString();
        Assert.assertFalse("File(\"trusted\").toString should be untrusted", TaintUtils.isTrusted(pathName));
        // show_metadata("File.toString", file.toString());

        pathName = file.getName();
        Assert.assertFalse("File(\"trusted\").getName should be untrusted", TaintUtils.isTrusted(pathName));
        // show_metadata("File.getName", file.getName());

        pathName = file.getPath();
        Assert.assertFalse("File(\"trusted\").getPath should be untrusted", TaintUtils.isTrusted(pathName));
        // show_metadata("File.getPath", file.getPath());

        pathName = file.getAbsolutePath();
        Assert.assertFalse("File(\"trusted\").getAbsolutePath() should be untrusted", TaintUtils.isTrusted(pathName));
        // show_metadata("File getAboslutePath", file.getAbsolutePath());

        absFile = file.getAbsoluteFile();
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
        File file = new File(TaintUtils.trust("build.xml"));
        FileInfo expected = new FileInfo(file);
        try {
            Assert.assertEquals("canWrite() calls should match", expected.canWrite(), file.canWrite());
            Assert.assertEquals("canRead() calls should match", expected.canRead(), file.canRead());
            Assert.assertEquals("canExecute() calls should match", expected.canExecute(), file.canExecute());
            Assert.assertEquals("isDirectory() calls should match", expected.isDirectory(), file.isDirectory());
            Assert.assertEquals("isFile() calls should match", expected.isFile(), file.isFile());
            Assert.assertEquals("exists() calls should match", expected.exists(), file.exists());
            Assert.assertEquals("lastModified() calls should match", expected.lastModified(), file.lastModified());
            Assert.assertEquals("getAbsoluteFile() calls should match", expected.getAbsoluteFile(),
                                file.getAbsoluteFile());
            Assert.assertEquals("getAbsolutePath() calls should match", expected.getAbsolutePath(),
                                file.getAbsolutePath());
            Assert.assertEquals("getCanonicalFile() calls should match", expected.getCanonicalFile(),
                                file.getCanonicalFile());
            Assert.assertEquals("getCanonicalPath() calls should match", expected.getCanonicalPath(),
                                file.getCanonicalPath());
        } catch (IOException e) {
            Assert.fail("unexpected IOException on file '" + file + "': " + e);
        }
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

    // Uninstrument this class, so we can compare Java's answer
    // with our instrumented answer (i.e. fstat).
    class FileInfo {
        File file;

        public FileInfo(File file) {
            this.file = file;
        }

        public boolean canRead() {
            return file.canRead();
        }

        public boolean canWrite() {
            return file.canWrite();
        }

        public boolean canExecute() {
            return file.canExecute();
        }

        public boolean isDirectory() {
            return file.isDirectory();
        }

        public boolean isFile() {
            return file.isFile();
        }

        public boolean exists() {
            return file.exists();
        }

        public long lastModified() {
            return file.lastModified();
        }

        public File getAbsoluteFile() {
            return file.getAbsoluteFile();
        }

        public String getAbsolutePath() {
            return file.getAbsolutePath();
        }

        public File getCanonicalFile() throws IOException {
            return file.getCanonicalFile();
        }

        public String getCanonicalPath() throws IOException {
            return file.getCanonicalPath();
        }
    }
    
}
