package pac.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ConcurrentlyAccessedFieldsTest {
    private static final int NUM_ITERATIONS = 100000000;
    private static final int EXPECTED_RESULT = 2 * NUM_ITERATIONS;
    private static int count1 = 0;
    private static int count2 = 0;
    private static int count3 = 0;

    @Test
    public void emptyTest() {
        // Need at least one JUnit test here, or JUnit is not
        // happy.
    }

    private void join(Thread thread) {
        try {
            thread.join();
        } catch (InterruptedException e) {
            Assert.fail(thread + " was somehow interrupted: " + e);
        }
    }

    static class UnsynchronizedIncrementer extends Thread {
        public void run() {
            for (int i = 0; i < NUM_ITERATIONS; i++) {
                int x = count1;
                count1 = x + 1;
            }
        }
    }

    static class UnsynchronizedDecrementer extends Thread {
        public void run() {
            for (int i = 0; i < NUM_ITERATIONS; i++) {
                int x = count1;
                count1 = x - 1;
            }
        }
    }

    static class MethodSynchronizedIncrementer extends Thread {
        public synchronized void run() {
            for (int i = 0; i < NUM_ITERATIONS; i++) {
                int x = count2;
                count2 = x + 1;
            }
        }
    }

    static class MethodSynchronizedDecrementer extends Thread {
        public synchronized void run() {
            for (int i = 0; i < NUM_ITERATIONS; i++) {
                int x = count2;
                count2 = x - 1;
            }
        }
    }

    static class ObjectSynchronizedIncrementer extends Thread {
        private Object localLock = null;

        public ObjectSynchronizedIncrementer(Object o) {
            localLock = o;
        }

        public void run() {
            for (int i = 0; i < NUM_ITERATIONS; i++) {
                synchronized (localLock) {
                    int x = count3;
                    count3 = x + 1;
                }
            }
        }
    }

    static class Counter {
        private int counter = 0;
        private int nIllegalAccesses = 0;

        public void increment() {
            int x = counter;
            counter = x + 1;
        }

        public synchronized void decrement() {
            if (counter <= 0) {
                nIllegalAccesses++;
                counter = 0;
            } else {
                counter = counter - 1;
            }
        }

        public int getCount() {
            return counter;
        }

        public int getIllegalAccesses() {
            return nIllegalAccesses;
        }
    }

    static class UnsynchronizedFieldIncrementer extends Thread {
        private Counter counter = null;

        public UnsynchronizedFieldIncrementer(Counter c) {
            counter = c;
        }

        public void run() {
            for (int i = 0; i < NUM_ITERATIONS; i++) {
                counter.increment();
            }
        }
    }

    static class ProducerConsumer extends Thread {
        private Counter counter = null;
        private boolean isConsumer = false;

        public ProducerConsumer(Counter c) {
            counter = c;
        }

        public ProducerConsumer(Counter c, boolean isC) {
            counter = c;
            isConsumer = isC;
        }

        public void run() {
            if (isConsumer) {
                consume();
            } else {
                produce();
            }
        }

        private void produce() {
            for (int i = 0; i < NUM_ITERATIONS; i++) {
                counter.increment();
                yield();
            }
        }

        private void consume() {
            for (int i = 0; i < NUM_ITERATIONS; i++) {
                if (counter.getCount() > 0) {
                    counter.decrement();
                    yield();
                }
            }
        }
    }

    @Before
    public void initialize() {
        count1 = 0;
        count2 = 0;
        count3 = 0;
    }

    @Ignore
    public void noSynchronizationIncTest() {
        Thread t1 = new UnsynchronizedIncrementer();
        Thread t2 = new UnsynchronizedIncrementer();
        t1.start();
        t2.start();
        join(t1);
        join(t2);
        Assert.assertTrue("failed to synchronize updating static field; count1 = " + count1 + " (expected: "
                + EXPECTED_RESULT + ")", count1 == EXPECTED_RESULT);
    }

    @Ignore
    public void noSynchronizationIncDecTest() {
        Thread t1 = new UnsynchronizedIncrementer();
        Thread t2 = new UnsynchronizedDecrementer();
        t1.start();
        t2.start();
        join(t1);
        join(t2);
        Assert.assertTrue("failed to synchronize updating static field; count1 = " + count1 + " (expected: 0)",
                          count2 == 0);
    }

    @Ignore
    public void methodSynchronizationIncTest() {
        Thread t1 = new MethodSynchronizedIncrementer();
        Thread t2 = new MethodSynchronizedIncrementer();
        t1.start();
        t2.start();
        join(t1);
        join(t2);
        Assert.assertTrue("failed to synchronize updating static field: count2 = " + count2 + " (expected: "
                + EXPECTED_RESULT + ")", count2 == EXPECTED_RESULT);
    }

    @Ignore
    public void methodSynchronizationIncDecTest() {
        Thread t1 = new MethodSynchronizedIncrementer();
        Thread t2 = new MethodSynchronizedDecrementer();
        t1.start();
        t2.start();
        join(t1);
        join(t2);
        Assert.assertTrue("failed to synchronize updating static field: count2 = " + count2 + " (expected: 0)",
                          count2 == 0);
    }

    @Ignore
    public void objectSynchronizationIncTest() {
        Thread t1 = new ObjectSynchronizedIncrementer(new Object());
        Thread t2 = new ObjectSynchronizedIncrementer(new Object());
        t1.start();
        t2.start();
        join(t1);
        join(t2);
        Assert.assertTrue("failed to synchronize updating static field: count3 = " + count3 + " (expected: "
                + EXPECTED_RESULT + ")", count3 == EXPECTED_RESULT);
    }

    @Ignore
    public void fieldIncTest() {
        Counter c = new Counter();
        Thread t1 = new UnsynchronizedFieldIncrementer(c);
        Thread t2 = new UnsynchronizedFieldIncrementer(c);
        t1.start();
        t2.start();
        join(t1);
        join(t2);
        int result = c.getCount();
        Assert.assertTrue("failed to synchronized updating object field: result = " + result + " (expected: "
                + EXPECTED_RESULT + ")", result == EXPECTED_RESULT);
    }

    @Ignore
    public void producerConsumerTest() {
        Counter c = new Counter();
        Thread t1 = new ProducerConsumer(c);
        Thread[] consumers = new Thread[100];
        for (int i = 0; i < 100; i++) {
            consumers[i] = new ProducerConsumer(c, true);
        }
        t1.start();
        for (int i = 0; i < 100; i++) {
            consumers[i].start();
        }
        join(t1);
        for (int i = 0; i < 100; i++) {
            join(consumers[i]);
        }
        int result = c.getIllegalAccesses();
        Assert.assertTrue("failed to synchronize on checking shared object value: result = " + result
                + " (expected: 0)", result == 0);
    }
}
