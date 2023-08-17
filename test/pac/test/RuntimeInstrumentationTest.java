package pac.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.owasp.esapi.ESAPI;

import pac.config.CleartrackException;
import pac.util.TaintUtils;
import pac.util.TaintValues;

public class RuntimeInstrumentationTest {

  @AfterClass
  public static void cleanup() {
    new File("File_Name").delete();
    new File("File_Name.err").delete();
  }

  @Test
  public void nonShellExecCommandArrayTest() {
    String input = "good-01";
    TaintUtils.mark(input, TaintValues.UNKNOWN, 0, input.length() - 1);
    String ext = ".jar";
    TaintUtils.mark(ext, TaintValues.UNKNOWN, 0, ext.length() - 1);
    String path = "/opt/cleartrack/TH-workspace/testData/" + input + "/jar/HelloWorld" + ext;
    String[] javaCmd = new String[] {"/usr/lib/jvm/jdk1.7.0_21/jre/bin/java", "-jar", path};
    try {
      Runtime.getRuntime().exec(javaCmd, null, null);
    } catch (CleartrackException e) {
      Assert.fail("We should not see this, since java is not a shell: " + e);
    } catch (IOException e) {
      // This is only a test to ensure that we are not applying must_trust on the arguments to the
      // java command... This executable doesn't actually exist.
    }
  }

  @Test
  public void pipedExecTest() throws IOException {
    Process p = Runtime.getRuntime().exec(TaintUtils.trust("/bin/bash"));
    PrintWriter pOut = new PrintWriter(p.getOutputStream());
    BufferedReader pIn = new BufferedReader(new InputStreamReader(p.getInputStream()));
    String path = "../StaticAnalyzer/README.txt";
    TaintUtils.taint(path);
    String cmd = TaintUtils.trust("ls -la ") + path;
    pOut.println(cmd);// CLEARTRACK:TRIGGER_POINT
    pOut.flush();
    pOut.close();

    try {
      int exitStatus = p.waitFor();
      Assert.assertTrue("command piped to process should return non-zero exit status",
          exitStatus != 0);
    } catch (InterruptedException e) {
      Assert.fail("process has been interrupted");
    }

    String line;
    while ((line = pIn.readLine()) != null) {
      Assert.assertTrue("string read from process should be entirely tainted",
          TaintUtils.isAllTainted(line, 0, line.length() - 1));
    }

    p = Runtime.getRuntime().exec(TaintUtils.trust("/bin/bash"));
    pOut = new PrintWriter(p.getOutputStream());
    path = TaintUtils.trust("build.xml");
    TaintUtils.taint(path);
    cmd = TaintUtils.trust("ls -la ") + path;
    pOut.println(cmd);// CLEARTRACK:TRIGGER_POINT
    pOut.flush();
    pOut.close();

    try {
      int exitStatus = p.waitFor();
      Assert.assertTrue("command piped to process should return zero exit status", exitStatus == 0);
    } catch (InterruptedException e) {
      Assert.fail("process has been interrupted");
    }

    try {
      p = Runtime.getRuntime().exec(TaintUtils.trust("/bin/bash"));
      pOut = new PrintWriter(p.getOutputStream());
      cmd = "ls -la";
      TaintUtils.taint(cmd);
      pOut.println(cmd);// CLEARTRACK:TRIGGER_POINT
      pOut.flush();
      pOut.close();

      p.waitFor();
      Assert.fail("command should not even properly execute since entire command is untrusted");
    } catch (InterruptedException e) {
      Assert.fail("process has been interrupted");
    } catch (CleartrackException e) {
      return;
    }
    Assert.fail("entirely untrusted command should have thrown a CleartrackException");
  }

  @Test
  public void testInappropriateEncoding() throws IOException {
    Runtime runtime = Runtime.getRuntime();
    String cmd;

    String cmd1 = ESAPI.encoder().encodeForLDAP(TaintUtils.trust("../zzz"));
    String cmd2 = "/bin/pwd";
    TaintUtils.taint(cmd1);
    TaintUtils.taint(cmd2);
    cmd = new String("/bin/ls " + cmd1 + " ; " + cmd2);
    boolean caughtEx = false;
    try {
      runtime.exec(cmd); // IOException
    } catch (RuntimeException ex) {
      caughtEx = true;
    }
    Assert.assertTrue(
        "Runtime.exec(String) failed to throw RuntimeException due to inappropriate encoding.",
        caughtEx);
  }

  @Test
  public void execTest() {
    try {
      Runtime runtime = Runtime.getRuntime();

      // Test that System.getenv(<key>) returns tainted data (and not unknown)
      final String test = System.getenv(TaintUtils.trust("HOME"));
      Assert.assertFalse(
          "System.getenv(\"HOME\") returned a String having a trust level of UNKNOWN.",
          TaintUtils.hasUnknown(test));

      String cmd;

      // ***
      // exec(String)
      // If programmer supplies "<shell-cmd> <command>"         shell fails
      //                        "<shell-cmd> -c <command)"      shell executes command
      //                        "<shell-cmd> -c <command> param Any params, injected or not, are
      //                                                        ignored
      String cmdRay[] = new String[3];

      cmdRay[0] = "csh"; // Config lists "/bin/csh" as a shell.
                         // Instrumentation will change cmdRay[0] from "csh" to "/bin/csh".
      TaintUtils.trust(cmdRay[0]);
      cmdRay[1] = "-c";
      TaintUtils.trust(cmdRay[1]);

      // ***
      // Config lists "/bin/csh as shell
      // Uses the special: execShellArrayElementTwo
      // white_failure_response is exception
      boolean caughtEx;

      // ***
      // Test "/bin/bash -c ls -l $HOME" $HOME is expanded to some tainted /home/dir/...
      // The first / is replaced with _
      //                                  _____
      cmdRay[2] = TaintUtils.trust("ls -l $HOME");
      TaintUtils.taint(cmdRay[2], 6, 10);
      caughtEx = false;
      try {
        runtime.exec(cmdRay); // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertFalse("Runtime.exec(String[]) threw unexpected RuntimeException", caughtEx);
      Assert.assertTrue(
          "Runtime.exec(String[]); String[2] is \"ls -l $HOME\"; $HOME expands to a tainted String starting wih slash. Slash is replaced by underscore",
          cmdRay[2].startsWith("ls -l _"));

      // ***
      // Test "/bin/bash -c ls -l '$HOME'" $HOME is NOT expanded because it is surrounded by single
      // quotes.
      // $HOME is a legal string
      //                                   _____
      cmdRay[2] = TaintUtils.trust("ls -l '$HOME'");
      TaintUtils.taint(cmdRay[2], 7, 11);
      caughtEx = false;
      try {
        runtime.exec(cmdRay); // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertFalse("Runtime.exec(String[]) threw unexpected RuntimeException", caughtEx);
      Assert.assertTrue(
          "Runtime.exec(String[]); String[2] is \"ls -l '$HOME'\"; $HOME is surrounded by single quotes and so should not expand.",
          cmdRay[2].equals("ls -l '$HOME'"));

      // ***
      // Test "/bin/bash -c ls -l '$HOME'" $HOME is NOT expanded because it is surrounded by single
      // quotes.
      // /User/something is untrusted and is altered to _User/something
      //                                  _______
      cmdRay[2] = TaintUtils.trust("ls -l '$HOME'");
      TaintUtils.taint(cmdRay[2], 6, 12);
      caughtEx = false;
      try {
        runtime.exec(cmdRay); // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertFalse("Runtime.exec(String[]) threw unexpected RuntimeException", caughtEx);
      Assert.assertTrue(
          "Runtime.exec(String[]); String[2] is \"ls -l '$HOME'\"; $HOME is surrounded by single quotes and so should not expand.",
          cmdRay[2].equals("ls -l '$HOME'"));

      // ***
      // Test some bizarre parameters combinations that can possibly be sent to a shell:
      //   /bin/bash command exec /bin/ls -l file
      //   /bin/bash command exec csh command /bin/ls -l file
      //   /bin/bash command exec csh -c /bin/ls -l file
      //   /bin/bash command exec csh script
      //   /bin/bash command bash command exec /bin/ls -l
      //   /bin/bash exec /bin/csh -l file
      //   /bin/bash command csh -noprofile -c -f /bin/ls -l file
      //
      // ***
      // "-l src/META-INF" is untrusted. White space is replaced and should not throw.
      //     _______________
      cmd = "-l src/META-INF";
      TaintUtils.taint(cmd);
      cmd = TaintUtils.trust("/bin/bash command exec /bin/ls ") + cmd;
      caughtEx = false;
      try {
        runtime.exec(cmd); // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertFalse("Runtime.exec(String) threw unexpected RuntimeException", caughtEx);
      // ***

      // ***
      //                          _______
      cmdRay[2] = "/bin/csh -c ls O'Leary";
      TaintUtils.trust(cmdRay[2]);
      TaintUtils.taint(cmdRay[2], 15, 21);
      caughtEx = false;
      try {
        runtime.exec(cmdRay); // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertTrue(
          "Runtime.exec(String[]) expected to throw RuntimeException. Illegal untrusted name O\'Leary",
          caughtEx);
      // ***

      // ***
      // Nonsense input
      //                              _______________
      cmd = new String("/bin/bash -fc -l\" ; \"/bin/pwd");
      TaintUtils.trust(cmd);
      TaintUtils.taint(cmd, 14, 28);
      caughtEx = false;
      try {
        runtime.exec(cmd); // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertTrue(
          "Runtime.exec(String) failed to throw RuntimeException when semicolon \";\" command was not found in a trusted path",
          caughtEx);
      // ***

      // ***
      // Test that suffix_black blacklist chars are replaced.
      // attach_chars lists all the illegal suffix chars in config file suffix_black.
      final String[] attack_chars = {" ", "/",
          // "\\", // Don't check this. Whitespace catches it first.
          "+", "."
          // "\0" Whitespace catches this first.
      };

      for (String str : attack_chars) {
        cmdRay[2] = "/bin/ls badFile" + str;
        //                      ________
        // cmdRay[2] = "/bin/ls badFileX";
        TaintUtils.trust(cmdRay[2]);
        TaintUtils.taint(cmdRay[2], 8, 15);
        caughtEx = false;
        try {
          runtime.exec(cmdRay); // IOException
        } catch (RuntimeException ex) {
          caughtEx = true;
        }
        Assert.assertTrue(
            "Runtime.exec(String[]) failed to replace attacking suffix blacklisted attack char",
            cmdRay[2].equals("/bin/ls badFile_"));
        Assert.assertFalse("Runtime.exec(String[]) threw unexpected RuntimeException", caughtEx);
      }
      // ***

      // ***
      // Test for path beginning with "./".
      //                   _____________
      cmdRay[2] = "/bin/ls ./config_inst";
      TaintUtils.trust(cmdRay[2]);
      TaintUtils.taint(cmdRay[2], 8, 20);
      caughtEx = false;
      try {
        runtime.exec(cmdRay); // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertTrue("Runtime.exec(String[]) failed to detect untrusted attack",
          cmdRay[2].equals("/bin/ls __config_inst"));
      Assert.assertFalse("Runtime.exec(String[]) threw unexpected RuntimeException", caughtEx);
      // ***

      // ***
      // Test that prefix_black blacklist chars are replaced.
      final String[] chars = {" ", "/", "~"}; // These three chars come from config file
                                              // prefix_black in check filename_check.
      for (String str : chars) {
        cmdRay[2] = "/bin/ls \"" + str + "badFile\"";
        //                       ________
        // cmdRay[2] = "/bin/ls "XbadFile"";
        TaintUtils.trust(cmdRay[2]);
        TaintUtils.taint(cmdRay[2], 9, 16);
        caughtEx = false;
        try {
          runtime.exec(cmdRay); // IOException
        } catch (RuntimeException ex) {
          caughtEx = true;
        }
        Assert.assertTrue(
            "Runtime.exec(String[]) failed to replace attacking suffix blacklisted attack char",
            cmdRay[2].equals("/bin/ls \"_badFile\""));
        Assert.assertFalse("Runtime.exec(String[]) threw unexpected RuntimeException", caughtEx);
      }

      // ***

      // ***
      // Test conversion of env var within double quotes to an untrusted illegal path.
      //                     _____
      cmdRay[2] = "/bin/ls \"$HOME\"";
      TaintUtils.trust(cmdRay[2]);
      TaintUtils.taint(cmdRay[2], 9, 13);
      caughtEx = false;
      try {
        runtime.exec(cmdRay); // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      String home = System.getenv("HOME");
      home = home.replaceFirst("/", "_");
      String newStr = "/bin/ls \"" + home + "\"";
      Assert.assertTrue("Runtime.exec(String[]) failed to detect $HOME is illegal",
          newStr.equals(cmdRay[2]));
      // ***

      // ***
      // Test conversion of env var to an untrusted illegal path.
      //                   _____
      cmdRay[2] = "/bin/ls $HOME";
      TaintUtils.trust(cmdRay[2]);
      TaintUtils.taint(cmdRay[2], 8, 12);
      caughtEx = false;
      try {
        runtime.exec(cmdRay); // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      home = System.getenv("HOME");
      home = home.replaceFirst("/", "_");
      newStr = "/bin/ls " + home;
      Assert.assertFalse("Runtime.exec(String[]) unexpectedly threw RuntimeException", caughtEx);
      Assert.assertTrue("Runtime.exec(String[]) failed to detect $HOME is illegal",
          newStr.equals(cmdRay[2]));
      // ***

      // ***
      // Test that $HOME is not converted because it is within single quotes.
      //                     _____
      cmdRay[2] = "/bin/ls \'$HOME\'";
      TaintUtils.trust(cmdRay[2]);
      TaintUtils.taint(cmdRay[2], 9, 13);
      caughtEx = false;
      try {
        runtime.exec(cmdRay); // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertFalse("Runtime.exec(String[]) unexpectedly threw RuntimeException", caughtEx);
      Assert.assertTrue("Runtime.exec(String[]) failed to detect $HOME is illegal",
          "/bin/ls \'$HOME\'".equals(cmdRay[2]));
      // ***

      // ***
      // $HOME is expanded. But '/Users/rjk' contains illegal single quote chars.
      // RuntimeException is thrown before the $HOME expanded altered line is copied into cmdRay[2].
      //                   _____________
      cmdRay[2] = "/bin/ls \"\'$HOME\'\"";
      // _____________________
      // cmdRay[2] = "/bin/ls \"-l\" ; ls \"\'$HOME\'\"";
      // cmdRay[2] = "/bin/ls \"-l\" ; ls \"$HOME\"\"";
      // cmdRay[2] = "/bin/ls \"-l;\"$HOME\"\"";
      TaintUtils.trust(cmdRay[2]);
      TaintUtils.taint(cmdRay[2], 8, 16);
      caughtEx = false;
      try {
        runtime.exec(cmdRay); // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertTrue(
          "Runtime.exec(String[]). Expected RuntimeException from untrusted single quotes ",
          caughtEx);
      // ***

      // ***
      // $HOME should be converted to an illegal and untrusted path.
      // Is no way to check this here. Can look at /tmp/cleartrack.log file.
      //                                                   _____
      cmd = new String("/bin/bash -noprofile -c -f /bin/ls $HOME");
      TaintUtils.trust(cmd);
      TaintUtils.taint(cmd, 35, 39);
      caughtEx = false;
      try {
        runtime.exec(cmd); // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertFalse("Runtime.exec(String) unexpectedly threw RuntimeException", caughtEx);
      // ***

      // ***
      // "-l src/META-INF" is untrusted. White space is replaced. should not throw.
      //           _______________
      cmdRay[2] = "-l src/META-INF";
      TaintUtils.taint(cmdRay[2]);
      cmdRay[2] = "/bin/bash command exec /bin/ls " + cmdRay[2];
      caughtEx = false;
      try {
        runtime.exec(cmdRay); // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertFalse("Runtime.exec(String) threw unexpected RuntimeException", caughtEx);
      Assert.assertTrue("Runtime.exec(String) unexpectedly altered parameters",
          "/bin/bash command exec /bin/ls -l_src/META-INF".equals(cmdRay[2]));
      // ***

      // ***
      //                                        _____________________________
      // /bin/bash command csh -noprofile -c -f /bin/ls -l ../Instrumentation
      cmd = new String("/bin/bash command csh -noprofile -c -f /bin/ls -l ../Instrumentation");
      TaintUtils.trust(cmd);
      TaintUtils.taint(cmd, 39, 67);
      caughtEx = false;
      try {
        runtime.exec(cmd); // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertTrue(
          "Runtime.exec(String) failed to detect shell running untrusted executable: /bin/ls",
          caughtEx);
      // ***

      // ***
      //                              _________
      cmdRay[2] = new String("/bin/ls ; /bin/pwd");
      TaintUtils.trust(cmdRay[2]);
      TaintUtils.taint(cmdRay[2], 8, cmdRay[2].length() - 1);
      caughtEx = false;
      try {
        runtime.exec(cmdRay); // IOException
        // "/bin/ls ; /bin/pwd"
        // Assert.assertTrue("exec failed to remove injected semi-colon (1)",
        // cmdRay[2].equals("/bin/ls __________"));
        // Assert.assertTrue("exec failed to change \"csh\" to full config file specification
        // \"/bin/csh\"", "/bin/csh".equals(cmdRay[0]));
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertTrue(
          "Runtime.exec(String[]) failed to throw RuntimeException while shell runs trusted command with dangerous data.\n"
              + cmdRay[2],
          caughtEx);
      // ***

      // ***
      // Uses the special: execShellArrayElementTwo.
      // Token 0 must_trust - this should cause a thrown exception.
      //                      _______
      cmdRay[2] = new String("/bin/ls -l");
      TaintUtils.trust(cmdRay[2]);
      TaintUtils.taint(cmdRay[2], 0, 6);
      caughtEx = false;
      try {
        runtime.exec(cmdRay); // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertTrue(
          "Runtime.exec(String[]) failed to throw exeption from shell's running untrusted command.",
          caughtEx);
      // ***

      // ***
      // Uses the special: execShellArrayElementTwo.
      // Insternal_black has [.]{2} should catch the ".." file name.
      // black_failure_response is replace.
      //                              _______
      cmdRay[2] = new String("/bin/ls ../file");
      TaintUtils.trust(cmdRay[2]);
      TaintUtils.taint(cmdRay[2], 8, 14);
      caughtEx = false;
      try {
        runtime.exec(cmdRay); // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertTrue("Runtime.exec(String[]) failed to replace csh with /bin/csh",
          "/bin/csh".equals(cmdRay[0]));
      Assert.assertTrue("Runtime.exec(String[]) failed to replace untrusted ../file with __/file",
          "/bin/ls __/file".equals(cmdRay[2]));
      Assert.assertFalse(
          "Runtime.exec(String[]) threw inapropriate RuntimeException while shell runs trusted command with dangerous data.\n"
              + cmdRay[2],
          caughtEx);
      // ***

      // ***
      // Uses the special: execShellArrayElementTwo.
      // white_failure_response is exception.
      // _________
      cmdRay[2] = new String("/bin/ls ; /bin/ls /etc/hosts");
      TaintUtils.trust(cmdRay[2]);
      TaintUtils.taint(cmdRay[2], 8, 16);
      caughtEx = false;
      try {
        runtime.exec(cmdRay); // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertTrue(
          "Runtime.exec(String[]) failed to throw IOException while shell runs trusted command with dangerous data.\n"
              + cmdRay[2],
          caughtEx);
      // ***

      // ***
      // Uses the special: execShellArrayElementTwo.
      // Config file specifies "sh": "/bin/sh" passes as a shell.
      // whitelist fails.
      // qstring escapes untrusted quotes.
      // white_failure_action exception.
      cmdRay[0] = "/bin/sh";
      TaintUtils.trust(cmdRay[0]);
      //                                _________________
      cmdRay[2] = new String("/bin/ls \"-l\" ; \"/bin/pwd\"");
      TaintUtils.trust(cmdRay[2]);
      TaintUtils.taint(cmdRay[2], 9, cmdRay[2].length() - 2);
      caughtEx = false;
      try {
        runtime.exec(cmdRay); // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertTrue(
          "Runtime.exec(String[]) failed to throw exception from whitelist.\n" + cmdRay[2],
          caughtEx);
      // ***

      // ***
      //                                _________________
      cmdRay[2] = new String("/bin/ls \"-l\" ; \"/bin/pwd\" /etc/hosts");
      TaintUtils.trust(cmdRay[2]);
      TaintUtils.taint(cmdRay[2], 9, 23);
      caughtEx = false;
      try {
        runtime.exec(cmdRay); // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertTrue(
          "Runtime.exec(String[]) failed to throw exception from exception from whitelist not matching \";\"\n"
              + cmdRay[2],
          caughtEx);
      // ***

      // ***
      // Config file execShellArrayElementTwo calls check must_trust to throw on untrusted token[0].
      // Tested exec runs untrusted data.
      // Check must_trust throws an exception.
      //                _______
      cmd = new String("/bin/ls");
      TaintUtils.taint(cmd);
      caughtEx = false;
      try {
        runtime.exec(cmd); // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertTrue(
          "Runtime.exec() failed to throw IOException trying to run untrusted command.", caughtEx);
      // ***

      // ***
      // "dangerous_exe" is presumably a dangerous executable.
      // It does not exist in any of the safe directories listed in the config file.
      // The attacker may have manipulated environment variable PATH to contain the path where "dangerous_exe" exists.
      // TODO: Other tests:
      //    o Where cmd exists in a safe dir listed in config file.
      //      But does dot exist in PATH.
      //    o Where cmd exists in a safe dir listed in config file.
      //      The first dir in PATH where cmd exists is not the same safe dir as listed in config file.
      cmd = "dangerous_exe";
      TaintUtils.trust(cmd);
      caughtEx = false;
      try {
        runtime.exec(cmd); // IOEception is thrown because dangerous_exe does not exist in $PATH.
                           // dangersou_ex exists neither in trusted exe dir nor in PATH, so
                           // assume dangerous_exe is an internal command.
      } catch (RuntimeException ex) {
        Assert.fail(
            "Runtime.exec() threw unexpect RuntimeException on leaf that is neither in trusted dir nor $PATH.");
      } catch (IOException ex) {
        caughtEx = true;
      }
      Assert.assertTrue(
          "Runtime.exec() failed to throw IOException on leaf executable that not in trusted directory nor in $PATH.",
          caughtEx);
      // ***

      /* 
       * TODO: Perhaps add this...
      // ***
      // Unrealistic situation. Where program is allowing user to execute any command he wants.
      //             ____________
      cmd = "/bin/csh -c /bin/pwd";
      TaintUtils.trust(cmd);
      TaintUtils.taint(cmd).markRegion(CharArrayTaint.TrustLevel.TRUSTED, 8, cmd.length() - 1);
      caughtEx = false;
      try {
        runtime.exec(cmd);  // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertTrue("Runtime.exec(String) failed to throw RuntimeException from running a shell running an untrusted shell.",
                         caughtEx);
      // ***
      */

      // ***
      // Config file must have: EXEC_SHELL_ON_UNTRUSTED_CMD_RESPONSE = throw_exception.
      // exec shell runs untrusted command.
      // ________
      cmd = new String("/bin/csh -c /bin/pwd");
      TaintUtils.trust(cmd);
      TaintUtils.taint(cmd, 12, 19);
      caughtEx = false;
      try {
        runtime.exec(cmd); // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertTrue(
          "Runtime.exec(String) failed to throw RuntimeException from running a shell running an untrusted command.",
          caughtEx);
      // ***

      // ***
      // Config file lists "/bin/csh" - so csh is altered to /bin/csh
      cmd = new String("csh -c /bin/pwd");
      TaintUtils.trust(cmd);
      caughtEx = false;
      try {
        runtime.exec(cmd);
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertFalse("Runtime.exe(String) threw RuntimeException for unknown reason", caughtEx);
      // ***

      // ***
      // exec shell where shell runs trusted command with untrusted param.
      // Config file FILENAME_WHITE must have '-'.
      // Config file EXEC_SHELL_CMD_PARAMS_ATTACK_RESPONSE must be sanitize.
      cmdRay[0] = new String("csh");
      TaintUtils.trust(cmdRay[0]);
      cmdRay[1] = "-c";
      TaintUtils.trust(cmdRay[1]);
      // ___
      cmdRay[2] = "/bin/ls -l";
      TaintUtils.trust(cmdRay[2]);
      runtime.exec(cmdRay);
      Assert.assertTrue("Runtime.exec(String) did not change csh to /bin/csh",
          "/bin/csh".equals(cmdRay[0]));
      Assert.assertTrue("Runtime.exec(String) mistakenly sanitized untrusted \"-l\" to be a threat",
          "/bin/ls -l".equals(cmdRay[2]));
      // ***

      // ***
      // Same test as previous but with frequent alterations between untrusted/trusted sections.
      // exec shell where shell runs trusted command with untrusted blacklisted command.
      // Config file EXEC_SHELL_CMD_PARAMS_ATTACK_RESPONSE must be sanitize.
      // Config file FILENAME_BLACK must contain [.]{2}
      //                   __   __   __   __
      cmdRay[2] = "/bin/ls ../a/../b/../c/..";
      TaintUtils.trust(cmdRay[2]);
      TaintUtils.taint(cmdRay[2], 8, 9);
      TaintUtils.taint(cmdRay[2], 13, 14);
      TaintUtils.taint(cmdRay[2], 18, 19);
      TaintUtils.taint(cmdRay[2], 23, 24);
      runtime.exec(cmdRay); // IOException
      Assert.assertTrue("Runtime.exec(String) did not change csh to /bin/csh",
          "/bin/csh".equals(cmdRay[0]));
      Assert.assertTrue(
          "Runtime.exec(String). untrusted blacklisted parameters to trusted commands were not sanitized.",
          cmdRay[2].equals("/bin/ls __/a/__/b/__/c/__"));
      // ***

      // ***
      // Same test as previous but all black list chars are within the same untrusted block of chars
      // exec shell where shell runs trusted command with untrusted blacklisted command.
      // Config file EXEC_SHELL_CMD_PARAMS_ATTACK_RESPONSE must be sanitize.
      // Config file FILENAME_BLACK must contain [.]{2}
      // Config check filename_check must have token_whitespace replace _
      //                   _________________
      cmdRay[2] = "/bin/ls ../a/../b/  /c/..";
      TaintUtils.trust(cmdRay[2]);
      TaintUtils.taint(cmdRay[2], 8, 24);
      runtime.exec(cmdRay); // IOException
      Assert.assertTrue("Runtime.exec(String) did not change csh to /bin/csh",
          "/bin/csh".equals(cmdRay[0]));
      Assert.assertTrue(
          "Runtime.exec(String). untrusted blacklisted parameters to trusted commands were not sanitized.",
          cmdRay[2].equals("/bin/ls __/a/__/b/__/c/__"));
      // ***

      // ***
      // Test that tainted PATH component is detected.
      cmd = TaintUtils.trust("/bin/pwd");
      Map<String, String> map = System.getenv();
      String[] envp = null;
      if (map != null) {
        // ***
        // Test that tainted PATH components are detected.
        envp = new String[map.size() + 2];
        final Set<String> keys = map.keySet();
        int i = 0;
        for (Iterator<String> it = keys.iterator(); it.hasNext();) {
          final String key = it.next();
          envp[i] = key + "=" + map.get(key);
          i++;
        }

        // Replace PATH in env var with a PATH having taint.
        int index = -1;
        int j;
        for (j = 0; index == -1 && j < envp.length; j++) {
          if (envp[j].startsWith("PATH=")) {
            index = j;
          }
        }
        if (index == -1) {
          //              __________
          envp[i] = "PATH=/afs/csail:/usr/bin/:/usr/local/bin";
          TaintUtils.trust(envp[i]);
          TaintUtils.taint(envp[i], 5, 14);
          i++;
        } else {
          //                  __________
          envp[index] = "PATH=/afs/csail:/usr/bin/:/usr/local/bin";
          TaintUtils.trust(envp[index]);
          TaintUtils.taint(envp[index], 5, 14);
        }

        caughtEx = false;
        try {
          runtime.exec(cmd, envp); // IOException
        } catch (RuntimeException ex) {
          caughtEx = true;
        }
        Assert.assertTrue("Runtime.exec(String, String[]) failed to detect taint in $PATH",
            caughtEx);
      }
      // ***

      /*
       * cmdRay[2] = new String("/bin/pwd");
       * final String[]envp_2=new String[2];
       * envp_2[0]="PATH=/afs/csail:/usr/bin/:/usr/local/bin";
       * TaintUtils.trust(envp_2[0]);
       * TaintUtils.taint(envp_2[0], 0, 3);
       * envp_2[1]="ANOTHER=../youplace";
       * TaintUtils.trust(envp_2[1]);
       * TaintUtils.taint(envp_2[1], 0, 6);
       * runtime.exec(cmdRay, envp_2);
       */
      // ***

      // ***
      //                              ______   ________
      cmdRay[2] = new String("/bin/ls ../zzz ; /bin/pwd");
      TaintUtils.trust(cmdRay[2]);
      TaintUtils.taint(cmdRay[2], 8, 13);
      TaintUtils.taint(cmdRay[2], 17, 24);
      caughtEx = false;
      try {
        runtime.exec(cmdRay); // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertTrue(
          "Runtime.exec(String[]) failed to throw RuntimeException from running untrusted command after \";\".",
          caughtEx);
      // ***

      // ***
      // Three commands sent to shell.
      //                              ______
      cmdRay[2] = new String("/bin/ls ../zzz | /bin/pwd ; echo hello");
      TaintUtils.trust(cmdRay[2]);
      TaintUtils.taint(cmdRay[2], 8, 13);
      caughtEx = false;
      try {
        runtime.exec(cmdRay); // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertTrue("Runtime.exec(String[]) failed to change untrusted ../zzz to __/zzz ",
          "/bin/ls __/zzz | /bin/pwd ; echo hello".equals(cmdRay[2]));
      // ***

      // ***
      // Shell command pipes to a second shell.
      //                              ______                            _________
      cmdRay[2] = new String("/bin/ls ../xyz |csh -c /bin/pwd;  /bin/ls ~/_tricks");
      TaintUtils.trust(cmdRay[2]);
      TaintUtils.taint(cmdRay[2], 8, 13);
      TaintUtils.taint(cmdRay[2], 42, 50);
      caughtEx = false;
      try {
        runtime.exec(cmdRay); // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertTrue("Runtime.exec(String[]) failed to change untrusted ../xyz to __/xyz ",
          cmdRay[2].startsWith("/bin/ls __/xyz"));
      Assert.assertTrue("Runtime.exec(String[]) failed to change shell from csh to /bin/csh",
          cmdRay[2].startsWith("/bin/ls __/xyz |/bin/csh"));
      Assert.assertTrue(
          "Runtime.exec(String[]) failed to change untrusted replace illegal prefix blacklist ~/_tricks",
          "/bin/ls __/xyz |/bin/csh -c /bin/pwd;  /bin/ls _/_tricks".equals(cmdRay[2]));
      Assert.assertFalse("Runtime.exec(String[]) erroneously threw RuntimeException", caughtEx);
      // ***

      // ***
      //                                _____
      cmdRay[2] = new String("/bin/ls \"../ps ; nofile\"");
      TaintUtils.trust(cmdRay[2]);
      TaintUtils.taint(cmdRay[2], 9, 13);
      caughtEx = false;
      try {
        runtime.exec(cmdRay); // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertTrue("Runtime.exec(String[]) failed to change untrusted ../ps to __/ps ",
          cmdRay[2].equals("/bin/ls \"__/ps ; nofile\""));
      Assert.assertFalse("Runtime.exec(String[]) erroneously threw RuntimeException", caughtEx);
      // ***

      // ***
      //                                _____
      cmdRay[2] = new String("/bin/ls \'../ps ; nofile\'");
      TaintUtils.trust(cmdRay[2]);
      TaintUtils.taint(cmdRay[2], 9, 13);
      caughtEx = false;
      try {
        runtime.exec(cmdRay); // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertTrue("Runtime.exec(String[]) failed to change untrusted ../ps to __/ps ",
          cmdRay[2].equals("/bin/ls \'__/ps ; nofile\'"));
      Assert.assertFalse("Runtime.exec(String[]) erroneously threw RuntimeException", caughtEx);
      // ***

      // ***
      // Test that "config_inst (including the escaped quote) is the tested untrusted string.
      // Test the escaped quote attack character.
      //                 _____________
      cmdRay[2] = "ls -l \"config_inst";
      TaintUtils.trust(cmdRay[2]);
      TaintUtils.taint(cmdRay[2], 6, 17);
      caughtEx = false;
      try {
        runtime.exec(cmdRay); // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertTrue(
          "Runtime.exec(String[] erroneously altered the single untrusted escaped quote file name",
          cmdRay[2].equals("ls -l \"config_inst"));
      Assert.assertTrue("Runtime.exec(String[]) Did not detect escaped attack quote", caughtEx);
      // ***

      // ***
      //                 ___________________
      cmdRay[2] = "ls -l \"\"\"config_inst\"";
      TaintUtils.trust(cmdRay[2]);
      TaintUtils.taint(cmdRay[2], 6, 20);
      caughtEx = false;
      try {
        runtime.exec(cmdRay); // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertTrue("Runtime.exec(String[] failed to reassemble quoted tokens",
          cmdRay[2].equals("ls -l \"\"\"config_inst\""));
      Assert.assertFalse("Runtime.exec(String[]) erroneously threw RuntimeException", caughtEx);
      // ***

      // ***
      //                 __________________________
      cmdRay[2] = "ls -l \"config_inst\" \'\'common";
      TaintUtils.trust(cmdRay[2]);
      TaintUtils.taint(cmdRay[2], 6, 27);
      caughtEx = false;
      try {
        runtime.exec(cmdRay); // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertTrue(
          "Runtime.exec(String[] either failed to reassemble tokens or failed to detect whitespace",
          cmdRay[2].equals("ls -l \"config_inst\"_\'\'common"));
      Assert.assertFalse("Runtime.exec(String[]) erroneously threw RuntimeException", caughtEx);
      // ***

      // ***
      //
      cmdRay[0] = "/bin/ls";
      TaintUtils.trust(cmdRay[0]);
      cmdRay[1] = "lib";
      TaintUtils.trust(cmdRay[1]);
      cmdRay[2] = "/home"; // <== Contains untrusted prefix contains blacklist chars.
      TaintUtils.taint(cmdRay[2], 0, 4);
      caughtEx = false;
      try {
        runtime.exec(cmdRay); // IOException
      } catch (RuntimeException ex) {
        caughtEx = true;
      }
      Assert.assertTrue("Runtime.exec(String[]) did not replace untrusted blacklisted \"/home\"",
          "_home".equals(cmdRay[2]));
      Assert.assertFalse("Runtime.exec(String[]) erroneously threw RuntimeException", caughtEx);
      // ***

    } catch (IOException ex) {
      Assert.fail("error performing exec: " + ex.getMessage());
    }
  } // execTest

  void execCommands(String cmd[]) {
    try {
      Process p = Runtime.getRuntime().exec(cmd);
      StreamConsumer out = new StreamConsumer(p.getInputStream());
      out.start();
      StreamConsumer err = new StreamConsumer(p.getErrorStream());
      err.start();
      out.join();
      err.join();

      int exitvalue = p.waitFor();
      System.out.println("Exit value: " + exitvalue);

    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    System.out.println("----------------------");
    // try {
    //   Thread.sleep(3000);
    // } catch (InterruptedException e) {
    //   // TODO Auto-generated catch block
    //   e.printStackTrace();
    // }
  }

  @Test
  public void perlCommandInjection() {
    String[] cmd = new String[3];

    cmd[0] = "/usr/bin/perl";// Can be ruby, python, etc...
    cmd[1] = "-e";
    String trusted = "'`print touch ";
    TaintUtils.trust(trusted);

    String fname = "/tmp/hellostone.txt";
    TaintUtils.taint(fname);

    String untrusted = ";ls\u0020-l\u0020" + fname;
    TaintUtils.taint(untrusted);

    System.out.println("Executing commands using perl");
    cmd[2] = trusted + fname + "`'";
    System.out.println("Executing shell command: " + cmd[2]);
    execCommands(cmd);
    cmd[2] = trusted + fname + untrusted + "`'";
    boolean caughte = false;
    try {
      System.out.println("Executing shell command: " + cmd[2]);
      execCommands(cmd);
    } catch (CleartrackException e) {
      caughte = true;
      // System.out.println(e.getMessage());
    }
    Assert.assertTrue("Tainted data having untrusted space should be caught", caughte);

  }

  /*
   * @Test
   * public void testFileNameWithSpaces() {
   *   String cmd[] = new String[3];
   *   String command = "cat ";
   *   cmd[0] = "/bin/bash";
   *   cmd[1] = "-c";
   *   String untrusted = "file\\ with\\ spaces.txt";
   * 
   *   System.out.println("Testing command: " + command + untrusted);
   *   TaintUtils.taint(untrusted);
   *   cmd[2] = command + untrusted;
   * 
   *   boolean caught = false;
   *   try {
   *     execCommands(cmd);
   *   } catch(CleartrackException e) {
   *     caught = true;
   *   }
   *   Assert.assertFalse(
   *     "A valid file name containing spaces should not be intervened by cleartrack",
   *     caught);
   * }
   */

  @Test
  public void Temp() {
    String spaceAsHex = String.format("tt%s;", "\u0020");
    System.out.println(spaceAsHex);
    String cmd = String.format("ls -l %s", "$HOME;cat\u0020config_inst");
    System.out.println(cmd);
  }

  @Test
  public void testHexSpace() {
    String cmd[] = new String[3];
    String command = "ls -l ";
    //
    cmd[0] = "/bin/bash";
    cmd[1] = "-c";

    String untrusted = String.format("%s", "$HOME;cat\u0020config_inst_t");
    TaintUtils.taint(untrusted);
    System.out.println("Testing command: " + command + " " + untrusted);

    cmd[2] = command + untrusted;
    boolean caught = false;
    try {
      execCommands(cmd);
    } catch (CleartrackException e) {
      caught = true;
      System.out.println(e.getMessage());
    }
    Assert.assertTrue("Commands separated by hex encoded space should not run.", caught);

    untrusted = "$HOME;cat\u0020config_inst";
    TaintUtils.taint(untrusted);
    cmd[2] = command + untrusted;
    try {
      execCommands(cmd);
    } catch (CleartrackException e) {
      caught = true;
    }
    Assert.assertTrue("Commands separated by hex encoded space should not run.", caught);
  }

  @Test
  public void testHOMEVAR() {
    String cmd[] = new String[3];
    String command = "ls -l ";

    cmd[0] = "/bin/bash";
    cmd[1] = "-c";

    // ***
    // Test that tainted $HOME is converted to tainted /Users/... which is an attack and
    // is sanitized to _Users/...
    String untrusted = String.format("%s", "$HOME");
    TaintUtils.taint(untrusted);

    cmd[2] = command + untrusted;
    boolean caught = false;
    try {
      System.out.println("Testing command: " + command + untrusted);
      execCommands(cmd);
    } catch (CleartrackException e) {
      caught = true;
      System.out.println(e.getMessage());
    }
    Assert.assertTrue(
        "$HOME environment variable is tainted. Should be sanitized or caught by cleartrack",
        cmd[2].startsWith("ls -l _") || caught);

    // untrusted = "$HOME";
    // TaintUtils.taint(untrusted);
    caught = false;
    try {
      // cmd[2] = command + untrusted;
      System.out.println("Testing command: " + command + untrusted);
      execCommands(cmd);
    } catch (CleartrackException e) {
      caught = true;
      System.out.println(e.getMessage());
    }
    Assert.assertFalse("$HOME environment variable variable be sanitized or caught by cleartrack",
        cmd[2].startsWith("ls -l /") && !caught);
  }

  String[] getCommandArray(int arrSize) {
    String cmd[] = new String[arrSize];
    cmd[0] = "/bin/bash";
    cmd[1] = "-c";
    return cmd;
  }

  public String[] execCommand(String command) {
    String cmd[] = getCommandArray(3);
    cmd[2] = command;
    System.out.println("Executing command: " + command);
    execCommands(cmd);
    return cmd;
  }

  public void executeTaintedCommand(String command) {
    String cmd[] = getCommandArray(3);
    cmd[2] = command;
    System.out.println("Executing command: " + command);
    boolean caught = false;
    try {
      execCommands(cmd);
    } catch (CleartrackException e) {
      caught = true;
      System.out.println("Caught tainted command: " + command + "\n" + e.getMessage());
    }
    Assert.assertTrue("Tainted data should be caught", caught);
  }

  @Test
  public void invalidCommands() {
    String tainted_file_name = "";
    tainted_file_name = "File_Name&grep File_Name File_Name";
    TaintUtils.taint(tainted_file_name);
    String command = "echo " + tainted_file_name;
    executeTaintedCommand(command);

    tainted_file_name = "\u003Bls";
    TaintUtils.taint(tainted_file_name);
    command = "echo " + tainted_file_name;
    executeTaintedCommand(command);

    tainted_file_name = "|\\||ls";
    TaintUtils.taint(tainted_file_name);
    command = "echo " + tainted_file_name;
    executeTaintedCommand(command);

    tainted_file_name = "\\|||ls";
    TaintUtils.taint(tainted_file_name);
    command = "echo " + tainted_file_name;
    executeTaintedCommand(command);

    tainted_file_name = "|||ls";
    TaintUtils.taint(tainted_file_name);
    command = "echo " + tainted_file_name;
    executeTaintedCommand(command);

    tainted_file_name = "||\\|ls";
    TaintUtils.taint(tainted_file_name);
    command = "echo " + tainted_file_name;
    executeTaintedCommand(command);

    // \|\||ls
    tainted_file_name = "\\|\\||ls";
    TaintUtils.taint(tainted_file_name);
    command = "echo " + tainted_file_name;
    executeTaintedCommand(command);
    // | sed -e "s/[aeio]/u/g"
    tainted_file_name = "|sed -e \"s/[aeio]/u/g\"";
    TaintUtils.taint(tainted_file_name);
    command = "echo " + tainted_file_name;
    executeTaintedCommand(command);

  }

  @Test
  public void validCommands() {
    String file_name = "File_Name";
    TaintUtils.taint(file_name);
    {

      String cmd[] = getCommandArray(3);
      cmd[2] = "touch " + file_name;
      execCommands(cmd);
    }

    String command = String.format("echo %s > %s;ls %s&&cat %s|grep %s", file_name, file_name,
        file_name, file_name, file_name);
    execCommand(command);

    ////////////////////////////////////////////////////////////////////////////////////////////
    command = String.format("echo %s >> %s;ls %s&cat %s|grep %s", file_name, file_name, file_name,
        file_name, file_name);
    execCommand(command);

    command = String.format("echo %s > %s 2> File_Name.err ;ls %s&cat %s|grep %s", file_name,
        file_name, file_name, file_name, file_name);
    execCommand(command);

    command = String.format("echo %s 2>&1 %s;ls %s&cat %s|grep %s", file_name, file_name, file_name,
        file_name, file_name);
    execCommand(command);

    command = String.format("echo %s|sed -e \"s/[aeio]/u/g\"", file_name);
    execCommand(command);

    file_name = "~";
    TaintUtils.taint(file_name);
    command = "ls " + file_name;
    String cmd[] = null;

    cmd = execCommand(command);

    System.out.println(cmd[2]);
    Assert.assertTrue("~ should be replaced with _", cmd[2].equalsIgnoreCase("ls _"));

    /////////////////////////////////////////////////////////////
    boolean caught = false;
    file_name = "Y";
    TaintUtils.taint(file_name);
    command = String.format("date +%%%s |sed -e \"s/[aeio]/u/g\"", file_name);
    try {
      execCommand(command);
    } catch (CleartrackException e) {
      caught = true;
      System.out.println(e.getMessage());
    }
    Assert.assertFalse(
        "% is a trusted character. Only character Y is tainted and this command should be allowed to execute",
        caught);

    caught = false;
    // touch < echo `date +%Y`
    file_name = "Y";
    TaintUtils.taint(file_name);
    command = String.format("touch < echo `date +%%%s`", file_name);
    try {
      execCommand(command);
    } catch (CleartrackException e) {
      caught = true;
      System.out.println(e.getMessage());
    }
    Assert.assertFalse(
        "% is a trusted character. Only character Y is tainted and this command should be allowed to execute",
        caught);
  }

  /**
   * Other Commands and some combinations Vars. PATH, TERM, SHELL, USER, PWD, DISPLAY
   *
   * echo "File_Name" > File_Name;ls File_Name&&cat File_Name|grep File_Name echo "File_Name" >
   * File_Name;ls File_Name&cat File_Name|grep File_Name
   */
  @Test
  public void env_slash() {
    String cmd[] = new String[3];
    String command = "echo ";

    cmd[0] = "/bin/bash";
    cmd[1] = "-c";

    // ***
    ArrayList<String> should_replace_with_underscore = new ArrayList<String>();
    should_replace_with_underscore.add("$HOME");
    // GOLD PATH is ok
    // ORIG should_replace_with_underscore.add("$PATH");
    // should_replace_with_underscore.add("$TERM");
    should_replace_with_underscore.add("$SHELL");
    // should_replace_with_underscore.add("$USER");
    // should_replace_with_underscore.add("$PWD");
    // should_replace_with_underscore.add("$DISPLAY");
    for (String v : should_replace_with_underscore) {
      String untrusted = String.format("%s", v);
      TaintUtils.taint(untrusted);

      cmd[2] = command + untrusted;
      boolean caught = false;
      try {
        System.out.println("Testing command: " + command + untrusted);
        execCommands(cmd);
      } catch (CleartrackException e) {
        caught = true;
        System.out.println(e.getMessage());
      }
      Assert.assertTrue(v + " environment variable should be sanitized or caught by cleartrack",
          cmd[2].startsWith(command + "_") || caught);
    }
  }

  @Test
  public void testTickCommand() {
    String cmd[] = new String[3];
    String command = "ls -l ";
    //
    cmd[0] = "/bin/bash";
    cmd[1] = "-c";
    String untrusted = "`pwd`";
    TaintUtils.taint(untrusted);
    System.out.println("Testing command: " + command + " " + untrusted);
    cmd[2] = command + untrusted;
    // TaintUtils.taint(untrusted);// Is setting tainted data after cmd[2] has been initialized
    // cheating?
    boolean caught = false;
    try {
      execCommands(cmd);
    } catch (CleartrackException e) {
      caught = true;
    }
    Assert.assertTrue("Tainted data in ticks should be caught", caught);
  }

  class StreamConsumer extends Thread {
    BufferedReader br = null;

    public StreamConsumer(InputStream is) {
      br = new BufferedReader(new InputStreamReader(is));
    }

    @Override
    public void run() {
      // System.out.println("Thread Started");
      String line = "";
      while (line != null) {
        System.out.println(line);
        try {
          line = br.readLine();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
      // System.out.println("Thread Finished");
    }
  }

} // class RuntimeInstrumentationTest
