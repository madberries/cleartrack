package pac.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import pac.config.CleartrackException;
import pac.util.TaintUtils;

public class FileOutputStreamInstrumentationTest {
    private String errorMsg;

    @Test
    public void fileTest() {
        File file;
        String pathName;
        boolean caughtEx;
        FileOutputStream fileOutputStream = null;

        // ***
        // Test that FileOutputStream(pathName) alters attack string "..junk" to
        // __junk and then creates and opens __junk
        pathName = "..junk";
        TaintUtils.taint(pathName);
        caughtEx = false;
        try {
            fileOutputStream = new FileOutputStream(pathName); // FileNotFoundException
        } catch (RuntimeException ex) {
            caughtEx = true;
        } catch (FileNotFoundException ex) {
            caughtEx = true;
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close(); // IOException
                }
            } catch (Exception ex) {
            }
        }
        file = new File(pathName);
        Assert.assertFalse("FileOutputStream(String) threw unexpected exception", caughtEx);
        Assert.assertTrue("FileOutputStream(String) did not alter path to __junk and create __junk", file.exists());
        // ***

        // ***
        // Test that FileOutputStream(pathName, true) alters attack string
        // "..junk" to __junk
        // and then opens pre-existing __junk for appending
        pathName = "..junk";
        TaintUtils.taint(pathName);
        caughtEx = false;
        try {
            fileOutputStream = new FileOutputStream(pathName, true); // FileNotFoundException
        } catch (RuntimeException ex) {
            caughtEx = true;
        } catch (FileNotFoundException ex) {
            caughtEx = true;
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close(); // IOException
                }
            } catch (Exception ex) {
            }
        }
        file = new File(pathName); // File Constructor changes pathName ..junk
                                   // to __junk
        Assert.assertFalse("FileOutputStream(String, boolean) threw unexpected exception", caughtEx);
        Assert.assertTrue("FileOutputStream(String, boolean) did not alter path to __junk and append to __junk",
                          file.exists());
        file.delete();
        // ***
    }

    @Test
    public void tooManyFilesTest() {
        Exception caughtEx = null;
        List<FileOutputStream> files = new LinkedList<FileOutputStream>();
        int iters = TaintUtils.taint(2501);
        try {
            for (int i = 0; i < iters; i++)
                files.add(new FileOutputStream("testfile" + i + ".txt"));
        } catch (CleartrackException e) {
            caughtEx = e;
        } catch (Throwable t) {
            Assert.fail("unexpected exception thrown: " + t);
        } finally {
            int i = 0;

            // close all remaining streams
            for (FileOutputStream fos : files) {
                try {
                    fos.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                i++;
            }

            // clean up all files that were written
            for (i = 0; i < iters; i++) {
                File file = new File("testfile" + i + ".txt");
                if (file.exists())
                    file.delete();
            }
        }
        Assert.assertNotNull("we should have thrown an exception when opening too many files", caughtEx);
    }

    public static void main(String[] args) {
        new FileOutputStreamInstrumentationTest().tooManyFilesTest2();
    }

    @Test
    public synchronized void tooManyFilesTest2() {
        FileThread fileThread = new FileThread();
        fileThread.start();

        // wait for the file thread to notify the main thread
        // that it has opened maxFiles minus 1 files.
        try {
            wait();
        } catch (InterruptedException e) {
            Assert.fail("we somehow interrupted the wait(): " + e);
        }

        // now the straw that breaks the camel's back
        try (FileOutputStream fos = new FileOutputStream("overthetop.txt");) {

        } catch (Throwable e) {
            Assert.fail("unexpected exception in main thread: " + e);
        } finally {
            File file = new File("overthetop.txt");
            if (file.exists())
                file.delete();
        }

        // now wait again for the file thread to be interrupted (or
        // timeout)...
        try {
            fileThread.join();
        } catch (InterruptedException e) {
            Assert.fail("we somehow interrupted the join(): " + e);
        }

        // ensure that file thread did not encounter an error
        Assert.assertNull(errorMsg, errorMsg);
    }

    public class FileThread extends Thread {
        
        @Override
        public void run() {
            errorMsg = "we did not appropriately throw an exception in the right thread";
            List<FileOutputStream> files = new LinkedList<FileOutputStream>();
            int iters = TaintUtils.taint(2500);
            try {
                for (int i = 0; i < iters; i++)
                    files.add(new FileOutputStream("testfile" + i + ".txt"));

                // wake up the main thread that is waiting...
                synchronized (FileOutputStreamInstrumentationTest.this) {
                    FileOutputStreamInstrumentationTest.this.notify();
                }

                // now wait here until either their are too many files open
                // or the main thread had interrupted this thread
                synchronized (FileThread.this) {
                    try {
                        FileThread.this.wait(1000);
                        errorMsg = "we were unable to interrupt the thread before the timeout period";
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            } catch (CleartrackException e) {
                errorMsg = null;
            } catch (Throwable t) {
                errorMsg = "unexpected exception thrown: " + t;
            } finally {
                int i = 0;
                // close all remaining streams and clean up the files created
                for (FileOutputStream fos : files) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    i++;
                }

                // clean up all files that were written
                for (i = 0; i < iters; i++) {
                    File file = new File("testfile" + i + ".txt");
                    if (file.exists())
                        file.delete();
                }
            }
        }
        
    }
}
