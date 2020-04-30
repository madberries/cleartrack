package pac.test;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Assert;
import org.junit.Test;

import pac.config.BaseConfig;
import pac.config.CleartrackException;

public class ThreadTest {
    private static Lock lock = new ReentrantLock();
    private Random rand = new Random();
    private Object o1 = "Object1";
    private Object o2 = "Object2";
    private Object o3 = "Object3";
    private Exception caughtEx;
    private boolean finished;

    private void join(Thread thread) {
        try {
            thread.join();
        } catch (InterruptedException e) {
            Assert.fail(thread + " was somehow interrupted: " + e);
        }
    }

    // Deadlock Scenario: A -> B -> A
    @Test
    public void deadlockTest1() {
        if (!BaseConfig.getInstance().areDeadlocksEnabled()) {
            System.out.println("Deadlock protection is not enabled, skipping tests...");
            return;
        }
        finished = false;
        caughtEx = null;
        Thread thread1 = new DoubleLockedThread("thread1", o1, o2);
        Thread thread2 = new DoubleLockedThread("thread2", o2, o1);
        thread1.start();
        thread2.start();
        join(thread1);
        join(thread2);
        Assert.assertNotNull("We didn't catch the deadlock in a reasonable time", caughtEx);
        System.out.println("finished deadlockTest()");
    }

    // Deadlock Scenario: A -> B -> C -> A
    @Test
    public void deadlockTest2() {
        if (!BaseConfig.getInstance().areDeadlocksEnabled()) {
            System.out.println("Deadlock protection is not enabled, skipping tests...");
            return;
        }
        finished = false;
        caughtEx = null;
        Thread thread1 = new DoubleLockedThread("thread1", o1, o2);
        Thread thread2 = new DoubleLockedThread("thread2", o2, o3);
        Thread thread3 = new DoubleLockedThread("thread3", o3, o1);
        thread1.start();
        thread2.start();
        thread3.start();
        join(thread1);
        join(thread2);
        join(thread3);
        Assert.assertNotNull("We didn't catch the deadlock in a reasonable time", caughtEx);
        System.out.println("finished deadlockTest()");
    }

    @Test
    public void noDeadlockTest1() {
        finished = false;
        caughtEx = null;
        Thread thread1 = new DoubleLockedThread("thread1", o1, o2);
        Thread thread2 = new DoubleLockedThread("thread2", o1, o2);
        thread1.start();
        thread2.start();
        join(thread1);
        join(thread2);
        Assert.assertNull("We falsely detected a deadlock", caughtEx);
        System.out.println("finished noDeadlockTest1()");
    }

    @Test
    public void noDeadlockTest2() {
        finished = false;
        caughtEx = null;
        Thread thread1 = new DoubleLockedThread("thread1", o2, o1);
        Thread thread2 = new DoubleLockedThread("thread2", o2, o1);
        thread1.start();
        thread2.start();
        join(thread1);
        join(thread2);
        Assert.assertNull("We falsely detected a deadlock", caughtEx);
        System.out.println("finished noDeadlockTest2()");
    }

    @Test
    public void tooManyLocksTest() {
        for (int i = 0; i < 1; i++) {
            finished = false;
            caughtEx = null;
            int x1 = rand.nextInt(10);
            int x2 = rand.nextInt(10);
            if (x1 == x2) {
                x1++;
            } else {
                int temp = Math.min(x1, x2);
                x1 = Math.max(x1, x2);
                x2 = temp;
            }
            Thread t1 = new ImproperlyLockedThread(x2, x1);
            Thread t2 = new ImproperlyLockedThread(0, 1);
            TimeoutThread timer = new TimeoutThread(10);
            timer.start();
            t1.start();
            t2.start();
            try {
                timer.join();
            } catch (InterruptedException e) {
                Assert.fail("we somehow interrupted one of: " + t1 + ", " + t2);
            }
            Assert.assertFalse("we failed to unlock an improperly locked thread", t1.isAlive() || t2.isAlive());
            Assert.assertNull("we somehow caught an IllegalMonitorStateException", caughtEx);
        }
    }

    @Test
    public void tooManyUnlocksTest() {
        for (int i = 0; i < 5; i++) {
            finished = false;
            caughtEx = null;
            int x1 = rand.nextInt(10);
            int x2 = rand.nextInt(10);
            if (x1 == x2) {
                x2++;
            } else {
                int temp = Math.max(x1, x2);
                x1 = Math.min(x1, x2);
                x2 = temp;
            }
            Thread t1 = new ImproperlyLockedThread(x2, x1);
            t1.start();
            try {
                t1.join();
            } catch (InterruptedException e) {
                Assert.fail("we somehow interrupted thread " + t1);
            }
            Assert.assertFalse("somehow this thread is still alive", t1.isAlive());
            Assert.assertNull("we somehow caught an IllegalMonitorStateException", caughtEx);
        }
    }

    @Test
    public void improperThreadStart() {
        Thread thread = new TimeoutThread(4); // timeout is 4 seconds
        long start = System.currentTimeMillis();
        thread.run(); // should return execution immediately if properly calling
                      // Thread.start()
        long end = System.currentTimeMillis();
        boolean fail = (end - start) > 3000;
        if (!fail) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Assert.fail("we somehow interrupted thread " + thread);
            }
        }
        Assert.assertFalse("we failed to properly start thread from Thread.run()", fail);
    }

    @Test
    public void verifyTest() {
        // Just keep this test to ensure that there is no runtime
        // verify error on methods that are synchronized...
        System.out.println("verify test: " + verify1(1, 2));
        System.out.println("verify test: " + verify2(1, 2));
        System.out.println("verify test: " + verify3(1, 2));
        System.out.println("verify test: " + verify4(1, 2));
    }

    private synchronized int verify1(int x, int y) {
        if (x > 2)
            return y;
        return x + y;
    }

    private synchronized int verify2(int x, int y) {
        if (x > 2)
            return y;
        if (y > 5)
            return x;
        return x + y;
    }

    private synchronized int verify3(int x, int y) {
        if (x > 2)
            return y;
        if (y > 5)
            return x;
        if (x + y < 0)
            throw new RuntimeException();
        return x + y;
    }

    private synchronized int verify4(int x, int y) {
        if (x > 2)
            return y;
        if (y > 5)
            return x;
        try {
            if (x + y < 0)
                throw new RuntimeException();
        } catch (Exception e) {

        }
        return x + y;
    }

    @Test
    public void overAllocTest() {
        caughtEx = null;
        OverAllocThread thread = new OverAllocThread();
        try {
            thread.start();
            thread.join();
            Assert.assertNotNull("We should have overallocated memory on " + thread, caughtEx);
        } catch (CleartrackException e) {
            Assert.fail("Exception was thrown on the wrong thread");
        } catch (InterruptedException e) {
            Assert.fail("We should not have interrupted on Thread.join() - " + thread + ": " + e);
        }
    }

    /**
     * Queries the Java runtime for the bytes used by the JVM over the maximum
     * heap space allowed by the currently running JVM.
     * 
     * @param runtime
     *            Runtime instance
     * @return total bytes used by the JVM (as a fraction over the total space)
     */
    private static double getUsedSpace(Runtime runtime) {
        long used = runtime.totalMemory() - runtime.freeMemory();
        BigDecimal percent = new BigDecimal(used).divide(new BigDecimal(runtime.maxMemory()), 5,
                                                         BigDecimal.ROUND_HALF_UP);
        return percent.doubleValue();
    }

    private class DoubleLockedThread extends Thread {
        private Object lock1, lock2;

        public DoubleLockedThread(String name, Object lock1, Object lock2) {
            super(name);
            this.lock1 = lock1;
            this.lock2 = lock2;
        }

        private String createWork(int num) {
            StringWriter buf = new StringWriter();
            for (int i = 0; i < rand.nextInt(num); i++) {
                buf.write("" + i);
            }
            return buf.toString();
        }

        @Override
        public void run() {
            int i = 0;
            try {
                while (!finished) {
                    // make sure we only print every few iterations, so that we
                    // dont get a java heap space error
                    createWork(1000); // create some busy work
                    synchronized (lock1) {
                        createWork(1000); // create some busy work
                        synchronized (lock2) {
                            createWork(1000); // create some busy work
                        }
                    }
                    i++;
                    if (i >= BaseConfig.getInstance().getLoopMaxItersDefinite())
                        break;
                }
            } catch (CleartrackException e) {
                caughtEx = e;
            } finally {
                finished = true;
            }
        }
    }

    private class TimeoutThread extends Thread {
        private int timeout;

        private TimeoutThread(int timeout) {
            this.timeout = timeout;
        }

        @Override
        public void run() {
            int i = 0;
            while (++i < timeout && !finished) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Assert.fail(TimeoutThread.this + " was somehow interrupted: " + e);
                }
            }
            finished = true;
        }
    }

    private class OverAllocThread extends Thread {
        private List<int[]> arrays = new LinkedList<int[]>();

        public OverAllocThread() {
            super("overAllocThread");
        }

        @Override
        public void run() {
            Runtime runtime = Runtime.getRuntime();
            try {
                // Ensure that our memory allocations will eventually
                // run out of memory...
                int i = 0;
                final int timeout = 10;
                final boolean catchGeneric = rand.nextBoolean();
                double used = getUsedSpace(runtime);
                // We need to keep looping past 75%, so that we eventually
                // find that the thread has been interrupted...
                int secs = 0;
                System.out.printf("we will catch %s exceptions on sleep()\n", catchGeneric ? "generic" : "interrupt");
                while (used < .95d && secs < timeout) {
                    if (used < .8f) { // Stop allocating after this point
                        arrays.add(new int[100000]);
                        if (i++ % 100 == 0) {
                            System.out.printf("%s%% of total memory is currently being used\n", used * 100d);
                        }
                        used = getUsedSpace(runtime);
                    } else { // Timeout after 10 seconds...
                        System.out.printf("timeout in %d seconds...\n", (timeout - secs));
                        secs++;
                        if (!catchGeneric || secs == timeout) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                System.out.println("caught interrupt exception: " + e);
                            }
                        } else {
                            try {
                                Thread.sleep(1000);
                            } catch (Exception e) {
                                // Even though we may catch the CleartrackException here,
                                // we will reset the interrupt bit and catch it on the
                                // next iteration.
                                System.out.println("caught generic exception: " + e);
                            }
                        }
                    }
                }
                System.out.printf("%s%% of total memory is currently being used\n", used * 100d);
            } catch (CleartrackException e) {
                caughtEx = e;
                System.out.println("caught cleartrack exception: " + e);
            }
        }
    }

    private class ImproperlyLockedThread extends Thread {
        private int unlocks, locks;

        public ImproperlyLockedThread(int unlocks, int locks) {
            this.unlocks = unlocks;
            this.locks = locks;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < locks; i++)
                    lock.lock();
                for (int i = 0; i < 20; i++) {
                    System.out.println("do something " + i);
                }
            } catch (IllegalMonitorStateException e) {
                caughtEx = e;
            } finally {
                for (int i = 0; i < unlocks; i++)
                    lock.unlock();
            }
        }
    }
}
