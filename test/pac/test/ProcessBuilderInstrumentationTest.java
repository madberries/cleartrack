package pac.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import pac.util.TaintUtils;

public class ProcessBuilderInstrumentationTest {

    // Use this method when there are three parameters  eg "csh -c /bin/ls
    //
    // Test both ProcessBuilder.command(String...)
    //       and ProcessBuilder.command(String[])
    // with the same data.
    //
    private void doBothCommandShellCalls(final ArrayList<String> cmdRay, final String catchExErrMsg,
                                         final boolean expectThrow, final String cmdRayTwoErrMsg,
                                         final String testString, final boolean cmdRayTwoErrBool) {
        String saveCmdRay2 = new String(cmdRay.get(2));
        boolean caughtEx;
        ProcessBuilder procBuilder = new ProcessBuilder();

        for (int i = 0; i < 2; i++) {
            List<String> cmd_list = null;
            caughtEx = false;
            try {
                if (i == 0) {
                    procBuilder.command(cmdRay.get(0), cmdRay.get(1), cmdRay.get(2));
                } else {
                    // Is 2nd time thru loop cmdRay(2) may have been altered the 1st time thru.
                    // So restore cmdRay(2) to its original String
                    cmdRay.add(saveCmdRay2);
                    procBuilder.command(cmdRay);
                }

                try {
                    procBuilder.start(); // IOException
                    cmd_list = procBuilder.command();
                } catch (IOException ex) {
                    Assert.fail("Unexpected IOExceptioin " + ex.getMessage());
                }

            } catch (RuntimeException ex) {
                caughtEx = true;
            }

            if (catchExErrMsg != null) {
                final String whichCommand = "ProcessBuilder.start() " + (i == 0 ? "(String...)" : "(List)");

                if (expectThrow) {
                    Assert.assertTrue(whichCommand + " " + catchExErrMsg, caughtEx);
                } else {
                    Assert.assertFalse(whichCommand + " " + catchExErrMsg, caughtEx);
                }
            }
            if (cmdRayTwoErrMsg != null) {
                final String whichCommand = "ProcessBuilder.start() " + (i == 0 ? "(String...)" : "(List)");

                if (cmdRayTwoErrBool) {
                    // If i == 0, is no way to check if Instrumented code altered the third arg
                    if (i != 0) {
                        Assert.assertTrue(whichCommand + " " + cmdRayTwoErrMsg, testString.equals(cmd_list.get(2)));
                    }
                } else {
                    if (cmdRayTwoErrBool) {
                        Assert.assertFalse(whichCommand + " " + cmdRayTwoErrMsg, testString.equals(cmd_list.get(2)));
                    }
                }
            }

            cmdRay.remove(2);
        }
    }

    @Test
    public void nonShellTest() {

        ArrayList<String> cmdRay = new ArrayList<String>();
        boolean catchEx;
        List<String> cmd_list = null;

        // ***
        // Run "/bin/ls -l ../file"
        cmdRay.add("/bin/ls");
        cmdRay.add("-l");
        //          _______
        cmdRay.add("../file");
        TaintUtils.trust(cmdRay.get(0));
        TaintUtils.trust(cmdRay.get(1));
        TaintUtils.taint(cmdRay.get(2));
        ProcessBuilder pb = new ProcessBuilder(cmdRay);
        try {
            pb.start(); // IOException
            cmd_list = pb.command();
        } catch (IOException ex) {
            Assert.fail("Unexpected IOExceptioin " + ex.getMessage());
        }
        Assert.assertTrue("ProcessBuilder.start() failed to replace ../file __/file",
                          "__/file".equals(cmd_list.get(2)));
        cmdRay.clear();
        // ***

        // ***
        // Run "bin/ls -l ../file"
        cmdRay.add("/bin/ls");
        cmdRay.add("-l");
        //          _______
        cmdRay.add("../file");
        TaintUtils.trust(cmdRay.get(0));
        TaintUtils.trust(cmdRay.get(1));
        TaintUtils.taint(cmdRay.get(2));
        doBothCommandShellCalls(cmdRay, "unexpected exception", false, // false means should not throw exception
                                "failed to replace untrusted attack characters", // null means do not check for altering cmdRay(2)
                                "__/file", true); // cmdRay(2) should equal the above string
        cmdRay.clear();
        // ***

        // ***
        // Test that taint anywhere in $PATH is an attack
        cmdRay.clear();
        cmdRay.add("/bin/ls");

        ProcessBuilder pb_2 = new ProcessBuilder(cmdRay);
        final Map<String, String> map_2 = pb_2.environment();
        String path = map_2.get("PATH");
        final String tainted_path_component = ":/User/rjk/bin";
        path += tainted_path_component;
        final int index = path.lastIndexOf(":");
        path = TaintUtils.taint(path, index, path.length() - 1);

        /* Bug
        // Taint disappears from tainted_path_component
        TaintUtils.taint(tainted_path_component);
        StringBuffer sb = new StringBuffer(path);
        sb.append(tainted_path_component);
        path = sb.toString();
        */

        /* BUG
        // Taint disappears from tainted_path_component
        TaintUtils.taint(tainted_path_component);
        path += tainted_path_component;
        */

        map_2.remove("PATH");
        map_2.put("PATH", path);
        catchEx = false;
        try {
            pb_2.start(); // IOExcetpion
        } catch (IOException ex) {
            Assert.fail("Unexpected IOExceptioin " + ex.getMessage());
        } catch (RuntimeException ex) {
            catchEx = true;
        }
        Assert.assertTrue("ProcessBuilder.start() failed throw RuntimeException from detecting taint $PATH component(s).",
                          catchEx);
    }

    @Test
    public void shellTest() {
        ArrayList<String> cmdRay = new ArrayList<String>();
        List<String> cmd_list = null;
        boolean catchEx;

        cmdRay.add("csh");
        TaintUtils.trust(cmdRay.get(0));
        cmdRay.add("-c");
        TaintUtils.trust(cmdRay.get(1));

        // ***
        // Run "csh -c $abc"
        // where $abc expands to "ls *"
        cmdRay.add("$abc");
        TaintUtils.taint(cmdRay.get(2));
        ProcessBuilder pb_1 = null;
        catchEx = false;
        try {
            pb_1 = new ProcessBuilder(cmdRay);
            final Map<String, String> map_1 = pb_1.environment();
            map_1.put("abc", "ls *"); // "abc" and "ls *" will both tainted
            pb_1.start(); // IOException
        } catch (IOException ex) {
            Assert.fail("Unexpected IOExceptioin " + ex.getMessage());
        } catch (RuntimeException ex) {
            // unable to tokenize entirely tainted data, so treat this as an attack
            catchEx = true;
        }
        Assert.assertTrue("ProcessBuilder.start() failed throw RuntimeException from leaf $abc not in a safe directory",
                          catchEx);
        cmdRay.remove(2);
        pb_1 = null;
        // ***

        // ***
        // Run "csh -c /bin/pwd"
        // Test constructor with shell command
        cmdRay.add("/bin/pwd");
        TaintUtils.trust(cmdRay.get(2));
        ProcessBuilder pb_2 = new ProcessBuilder(cmdRay);
        try {
            pb_2.start(); // IOException
        } catch (IOException ex) {
            Assert.fail("Unexpected IOExceptioin " + ex.getMessage());
        } catch (RuntimeException ex) {
            Assert.fail("Unexpected RuntimeException");
        }
        final List<String> cmd_ray = pb_2.command();
        Assert.assertTrue("ProcessBuilder.start() failed to replace csh with /bin/csh",
                          "/bin/csh".equals(cmd_ray.get(0)));
        cmdRay.remove(2);
        // ***

        // **
        // Run "csh -c /bin/ls ; /bin/pwd"
        // Test constructor with shell comand
        //                  __________
        cmdRay.add("/bin/ls ; /bin/pwd");
        TaintUtils.trust(cmdRay.get(2));
        TaintUtils.taint(cmdRay.get(2), 8, cmdRay.get(2).length() - 1);
        catchEx = false;
        try {
            final ProcessBuilder pb_3 = new ProcessBuilder(cmdRay);
            pb_3.start(); // IOException
        } catch (RuntimeException ex) {
            // altered line to "/bin/ls ;_/bin/pwd"
            // check filename_check failed to match: ";"
            catchEx = true;
        } catch (IOException ex) {
            Assert.fail("Unexpected IOExceptioin " + ex.getMessage());
        }
        Assert.assertTrue("ProcessBuilder.starg() failed to catch untrusted attack semicolon", catchEx);
        cmdRay.remove(2);
        // ***

        // ***
        // Run "csh -c pac"
        // "pac" exists in "bin/"
        // "bin/" is a trusted path component of $PATH that is not listed in config file trusted_exec_paths
        ArrayList<String> cmdRay_2 = new ArrayList<String>();
        cmdRay_2.add("pac"); // is a directory in bin
        TaintUtils.trust(cmdRay_2.get(0));
        ProcessBuilder pb_5 = new ProcessBuilder(cmdRay_2);
        Map<String, String> procEnv = pb_5.environment();
        if (procEnv.containsKey("PATH")) {
            procEnv.remove("PATH");
            procEnv.put("PATH", "bin:.:/bin");
            catchEx = false;
            try {
                pb_5.start();
            } catch (RuntimeException ex) {
                // "pac is located in an untrusted $PATH directory: bin"
                catchEx = true;
            } catch (IOException ex) {
                Assert.fail("Unexpected IOExceptioin " + ex.getMessage());
            }
            Assert.assertTrue("ProcessBuilder.start() failed to get expected Exception from dangerous \"bin\" path in $PATH",
                              catchEx);
        }
        // ***

        // ***
        // Test constructor with non-shell command
        // Run "csh -c /bin/ls ../a/../b/../c/.."
        cmdRay.add("/bin/ls ../a/../b/../c/..");
        TaintUtils.trust(cmdRay.get(2));
        TaintUtils.taint(cmdRay.get(2), 8, 9);
        TaintUtils.taint(cmdRay.get(2), 13, 14);
        TaintUtils.taint(cmdRay.get(2), 18, 19);
        TaintUtils.taint(cmdRay.get(2), 23, 24);
        catchEx = false;
        try {
            ProcessBuilder pb_4 = new ProcessBuilder(cmdRay);
            pb_4.start(); // IOException
            cmd_list = pb_4.command();
        } catch (RuntimeException ex) {
            catchEx = true;
        } catch (IOException ex) {
            Assert.fail("Unexpected IOExceptioin " + ex.getMessage());
        }

        Assert.assertTrue("ProcessBuilder(List<String) did not change shell from csh to /bin/csh",
                          "/bin/csh".equals(cmd_list.get(0)));
        Assert.assertFalse("ProcessBuilder.start() unexpected exception " + cmd_list.get(2), catchEx);
        Assert.assertTrue("ProcessBuilder.start() Untrusted blacklisted parameters were not sanitized.",
                          "/bin/ls __/a/__/b/__/c/__".equals(cmd_list.get(2)));
        cmdRay.remove(2);
        // ***

        // ***
        // Run "csh -c /bin/ls ; /bin/pwd"
        //                  __________
        cmdRay.add("/bin/ls ; /bin/pwd");
        TaintUtils.trust(cmdRay.get(2));
        TaintUtils.taint(cmdRay.get(2), 8, cmdRay.get(2).length() - 1);

        doBothCommandShellCalls(cmdRay,
                                "failed to throw RuntimeException while shell runs trusted command with dangerous data.\n"
                                        + cmdRay.get(2),
                                true, // true means should throw
                                null, // Err msg if test string no match
                                null, // String that cmdRay(2) should match or not match
                                true); // unused - Says
        // ***

        // ***
        // Run "csh -c /bin/ls -l"
        //          _______
        cmdRay.add("/bin/ls -l");
        TaintUtils.trust(cmdRay.get(2));
        TaintUtils.taint(cmdRay.get(2), 0, 6);
        doBothCommandShellCalls(cmdRay, "failed to throw exeption from shell's running untrusted command.", true, // true means should throw exception
                                null, // null means do not check for altering cmdRay(2)
                                null, true); // Unused
        // ***

        // ***
        // Run "csh -c /bin/ls ../file"
        //                  _______
        cmdRay.add("/bin/ls ../file");
        TaintUtils.trust(cmdRay.get(2));
        TaintUtils.taint(cmdRay.get(2), 8, 14);
        doBothCommandShellCalls(cmdRay, "failed to throw exeption from shell's running untrusted command.", false, // false means should NOT throw exception
                                "failed to replace untrusted ../file with __/file", "/bin/ls __/file", true); // Unused
        // ***

        // ***
        // Run "csh -c /bin/ls ; /bin/ls /etc/hosts"
        // User is injecting ';'
        //                  ___________________
        cmdRay.add("/bin/ls ; /bin/ls /etc/hosts");
        TaintUtils.trust(cmdRay.get(2));
        TaintUtils.taint(cmdRay.get(2), 8, 16);
        doBothCommandShellCalls(cmdRay,
                                "failed to throw IOException while shell runs trusted command with dangerous data.\n"
                                        + cmdRay.get(2),
                                true, // true means should throw exception
                                null, null, true); // Unused
        // ***

        // ***
        cmdRay.clear();
        cmdRay.add("/bin/sh");
        TaintUtils.trust(cmdRay.get(0));
        cmdRay.add("-c");
        TaintUtils.trust(cmdRay.get(1));

        // Run "/bin/sh -c /bin/ls \"-l\" ; \"/bin/pwd\""
        //                    _________________
        cmdRay.add("/bin/ls \"-l\" ; \"/bin/pwd\"");
        TaintUtils.trust(cmdRay.get(2));
        TaintUtils.taint(cmdRay.get(2), 9, cmdRay.get(2).length() - 2);
        doBothCommandShellCalls(cmdRay, "failed to throw exception from whitelist.\n" + cmdRay.get(2), true, // true means should throw exception
                                null, null, true); // Unused
        // ***

        // ***
        // Run "/bin/sh -c /bin/pwd"
        cmdRay.add("/bin/pwd");
        TaintUtils.trust(cmdRay.get(2));
        doBothCommandShellCalls(cmdRay, "thew exception for no reason.\n" + cmdRay.get(2), false, // true means should throw exception
                                null, null, true); // Unused
        // ***

        // ***
        // Run "/bin/sh -c /bin/ls ../a/../b/../c/.."
        cmdRay.add("/bin/ls ../a/../b/../c/..");
        TaintUtils.trust(cmdRay.get(2));
        TaintUtils.taint(cmdRay.get(2), 8, 9);
        TaintUtils.taint(cmdRay.get(2), 13, 14);
        TaintUtils.taint(cmdRay.get(2), 18, 19);
        TaintUtils.taint(cmdRay.get(2), 23, 24);
        doBothCommandShellCalls(cmdRay, "unexpected exception", false, // should not throw
                                "Untrusted blacklisted parameters to trusted commands were not sanitized.",
                                "/bin/ls __/a/__/b/__/c/__", true); // true means cmdRay(2) should equal the above param
        // ***

        // ***
        // Run "/bin/sh -c /bin/ls ../a/../b/  /c/.."
        cmdRay.add("/bin/ls ../a/../b/  /c/..");
        TaintUtils.trust(cmdRay.get(2));
        TaintUtils.taint(cmdRay.get(2), 8, 24);
        doBothCommandShellCalls(cmdRay, "unexpected exception", false, // should not throw
                                "Untrusted blacklisted parameters to trusted commands were not sanitized.",
                                "/bin/ls __/a/__/b/__/c/__", // cmdRay.get(2) must equal this
                                true); // true means cmdRay(2) should equal the above param
        // ***

        // ***
        // Run "/bin/sh -c /bin/ls ../zzz ; /bin/pwd"
        //                  ______   ________
        cmdRay.add("/bin/ls ../zzz ; /bin/pwd");
        TaintUtils.trust(cmdRay.get(2));
        TaintUtils.taint(cmdRay.get(2), 8, 13);
        TaintUtils.taint(cmdRay.get(2), 17, 24);
        doBothCommandShellCalls(cmdRay, "failed to throw RuntimeException from running untrusted command after \";\".",
                                true, null, // unused
                                null, // unused
                                true); // unused
        // ***

        // ***
        // Run "/bin/sh -c /bin/ls ../xyz |csh -c /bin/pwd;  /bin/ls ~/_tricks"
        // shell command pipes to a second shell
        //                  ______                            _________
        cmdRay.add("/bin/ls ../xyz |csh -c /bin/pwd;  /bin/ls ~/_tricks");
        TaintUtils.trust(cmdRay.get(2));
        TaintUtils.taint(cmdRay.get(2), 8, 13);
        TaintUtils.taint(cmdRay.get(2), 42, 50);
        doBothCommandShellCalls(cmdRay, "erroneously threw RuntimeException", false, // should be no exception
                                "failed to change untrusted replace illegal prefix blacklist ~/_tricks",
                                "/bin/ls __/xyz |/bin/csh -c /bin/pwd;  /bin/ls _/_tricks", true); // cmdRay(2) should equal the above param
        // ***
    }

} // class ProcessBuilderINstrumentationTest
