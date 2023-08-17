package pac.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import pac.config.CleartrackException;

/**
 * Example code for my blog post "The TOCTTOU attack", available at
 * http://out-println.appspot.com/posts/tocttou.
 */
public class TOCTTOUTest {
  private static boolean finished = false;
  private static String failMsg = null;
  private static final Lock lock = new ReentrantLock();
  private static final Random rand = new Random();

  @AfterClass
  public static void cleanup() {
    new File("file1").delete();
    new File("file2").delete();
  }

  static class WriteThread extends Thread {
    private File writeFile;

    public WriteThread(File writeFile) {
      super("writer thread");
      this.writeFile = writeFile;
    }

    @Override
    public void run() {
      while (!finished) {
        try {
          boolean newFileCreated = false;
          boolean isSymLink = false;

          // Create an empty file and check to see if it's a sym link.
          lock.lock();
          try {
            if (writeFile.exists()) {
              if (!writeFile.delete()) {
                failMsg = "ERROR[" + getName() + "] - unable to remove existing file: " + writeFile;
                finished = true;
                break;
              }
            }
            newFileCreated = writeFile.createNewFile();
            isSymLink = Files.isSymbolicLink(writeFile.toPath());
          } finally {
            lock.unlock();
          }

          if (!newFileCreated) { // The attack can occur after this check and before the new
                                 // FileWriter.
            failMsg = "ERROR[" + getName() + "] - unable to create new file: " + writeFile;
            finished = true;
            break;
          } else if (!isSymLink) { // TOC-to-TOU vulnerability
            // Delay here so we have a better chance of exploiting the weakness.
            try {
              Thread.sleep(rand.nextInt(1000) + 200);
            } catch (InterruptedException e1) {

            }

            // Make sure we lock the attack file to prevent attack thread from reading this file as
            // we write to it.
            lock.lock();
            try {
              try (FileWriter out = new FileWriter(writeFile)) {
                out.write("write something");
              } catch (CleartrackException e) {
                finished = true;
                System.out.println("We were able to prevent a TOU-to-TOC attack: " + e);
                return;
              }
            } finally {
              lock.unlock();
            }
          }
        } catch (IOException e) {
          failMsg =
              "ERROR[" + getName() + "] - exception occured while writing to file: " + writeFile;
          finished = true;
          break;
        }
      }
    }
  }

  // NOTE: DO NOT INSTRUMENT THE ATTACK THREAD SINCE IT"S ASSUMED TO BE AN ATTACK
  // OUTSIDE THE APPLICATION (i.e. add this class to the classes.skip file)
  static class AttackThread extends Thread {
    private File writeFile, attackFile;
    private Path writePath, attackPath;

    public AttackThread(File writeFile, File attackFile) {
      super("attacker thread");
      this.writeFile = writeFile;
      this.attackFile = attackFile;
      this.writePath = writeFile.toPath();
      this.attackPath = attackFile.toPath();
    }

    @Override
    public void run() {
      int attackCount = 0;
      while (!finished) {
        try {
          // Remove any copy that was left around and create a brand new symlink from writeFile ->
          // attackFile.
          lock.lock();
          try {
            // Delete the existing file.
            if (writeFile.exists()) {
              if (!writeFile.delete()) {
                failMsg = "ERROR[" + getName() + "] - unable to remove existing file: " + writeFile;
                finished = true;
                break;
              }
            }

            // Attempt to create the malicious symlink from the file that WriteThread is writing
            // to.
            Files.createSymbolicLink(writePath, attackPath);
          } finally {
            lock.unlock();
          }

          // Delay here so we have a better chance of exploiting the weakness.
          try {
            Thread.sleep(rand.nextInt(1000) + 200);
          } catch (InterruptedException e1) {

          }

          // Block until the write thread has finished writing output to writeFile. If attackFile
          // contains bytes, then we must have successfully exploited the TOC-to-TOU weakness.
          lock.lock();
          try {
            try (FileInputStream attackIn = new FileInputStream(attackFile)) {
              if (attackIn.read() >= 0) {
                failMsg = "TOC-to-TOU ATTACK SUCCESSFUL!";
                finished = true;
                break;
              }
            }
          } finally {
            lock.unlock();
          }

          System.out.println("attack " + (++attackCount));
        } catch (IOException e1) {
          // The attacker may miss his opportunity for the attack, in which case we will not be able
          // to create the symbolic link.
          System.out.println("attack " + (++attackCount) + " - CANNOT CREATE SYMLINK: " + e1);
        }
      }
    }

  }

  @Test
  public void timeOfCheckToTimeOfUseTest() throws IOException, InterruptedException {
    main(new String[] {"file1", "file2"});
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    // Remove and touch attack file.
    File writeFile = new File(args[0]);
    File attackFile = new File(args[1]);
    if (attackFile.exists()) {
      if (!attackFile.delete()) {
        Assert.fail("ERROR - unable to remove existing file: " + attackFile);
        return;
      }
    }

    // Setup the attack file (initially empty).
    if (!attackFile.createNewFile()) {
      Assert.fail("ERROR - unable to create new file: " + attackFile);
      return;
    }

    Thread thread1 = new AttackThread(writeFile, attackFile);
    Thread thread2 = new WriteThread(writeFile);
    thread1.start();
    thread2.start();

    // Block on each thread.
    thread1.join();
    thread2.join();

    if (failMsg != null) {
      Assert.fail(failMsg);
    }
  }
}
