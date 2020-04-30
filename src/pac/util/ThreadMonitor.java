package pac.util;

import java.io.Closeable;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.sun.management.ThreadMXBean;

import pac.config.BaseConfig;
import pac.config.Notify;
import pac.config.NotifyMsg;
import pac.config.RunChecks;
import pac.config.CleartrackException;

/**
 * This thread will monitor all java threads and interrupt any misbehaved
 * threads.  It works by spawning a very low-priority daemon thread that queries
 * the JVM for possible deadlock conditions.  If a deadlock is found, we simply
 * interrupt an arbitrary thread (in the cycle of the deadlocked threads).
 * This interrupted thread will then throw a CleartrackException at the point
 * where the lock() was attempted.  All other threads will continue as normal.
 * 
 * In addition to this, the daemon will also perform memory checks when the heap
 * is over 75% full.  If that is the case, the thread that has allocated the
 * most bytes will be interrupted (provided that it is not this thread or the
 * main thread).  Once this thread is found, the daemon will invoke the garbage
 * collector until we are back under 75% heap occupancy.
 * 
 * @author jeikenberry
 */
public class ThreadMonitor extends Thread {
    public static final ThreadMonitor INSTANCE = new ThreadMonitor();

    private static final int FAST_TASK_SLEEP = 50;
    private static final int SLOW_TASK_SLEEP = 5000;
    private static final int SLEEP_SCALE = SLOW_TASK_SLEEP / FAST_TASK_SLEEP;

    /**
     * The maximum allowed memory that can be used by the 
     * application (as a fraction)
     */
    private final float maxMemRatio;

    /**
     * The maximum number of open file handles that can be
     * open at one time, by the application
     */
    private final int maxFiles;

    /**
     * Maximum loop time for possibly/definitely tainted 
     * loops (respectively).
     */
    private final int maxLoopTime, maxLoopTimeDef;

    /**
     * Current timestamps created by the daemon thread for 
     * possibly/definitely tainted loops (respectively).
     */
    public Timestamp curTimestamp, curTimestampDef;

    /**
     * Queue of all timestamps created by the daemon thread for 
     * possibly/definitely tainted loops (respectively).
     */
    private LinkedList<Timestamp> timestamps, timestampsDef;

    /** Map of all objects to their respective locks **/
    private Map<Object, Lock> lockMap = new WeakIdentityHashMap<Object, Lock>();

    /**
     * Map of all objects to their respective condition (derived from the lock)
     */
    private Map<Object, Condition> condMap = new WeakIdentityHashMap<Object, Condition>();

    /** List of all thread ids that have deadlocked **/
    private List<Long> deadlockedThreadIds = new LinkedList<Long>();

    /** Thread that we have interrupted due to a deadlock condition **/
    private Thread interruptedThread = null;

    /** Total number of memory allocations of the offending thread **/
    private long memAlloc = -1;

    /** Minimum amount of free memory that should be available to the application **/
    private long maxMemory;

    /** The thread id for the main thread **/
    private long mainTid;

    /** Count of the total number of open files used by the JVM **/
    private int totalOpenFiles;

    /** Count of the number of open files of the offending thread **/
    private int openFiles = -1;

    /** Lock used to synchronize the calls to add/remove open files **/
    private Object openFileLock = new Object();

    private ThreadMXBean threadMB;

    private ThreadMonitor() {
        super("SS-Thread-Monitor");
        setDaemon(true);
        setPriority(MIN_PRIORITY);
        BaseConfig configFile = BaseConfig.getInstance();
        this.maxMemRatio = configFile.getMaxMemory();
        this.maxFiles = configFile.getMaxFiles();

        this.maxLoopTime = configFile.getLoopMaxTime();
        this.maxLoopTimeDef = configFile.getLoopMaxTimeDefinite();

        this.curTimestamp = new Timestamp(maxLoopTime);
        this.timestamps = new LinkedList<Timestamp>();
        this.timestamps.add(curTimestamp);

        this.curTimestampDef = new Timestamp(maxLoopTimeDef);
        this.timestampsDef = new LinkedList<Timestamp>();
        this.timestampsDef.add(curTimestampDef);

        this.threadMB = (ThreadMXBean) ManagementFactory.getThreadMXBean();
    }

    ThreadGroup getRootThreadGroup() {
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        ThreadGroup ptg;
        while ((ptg = tg.getParent()) != null)
            tg = ptg;
        return tg;
    }

    Thread[] getAllThreads() {
        final ThreadGroup root = getRootThreadGroup();
        int nAlloc = threadMB.getThreadCount();
        int n = 0;
        Thread[] threads;
        do {
            nAlloc *= 2;
            threads = new Thread[nAlloc];
            n = root.enumerate(threads, true);
        } while (n == nAlloc);
        return java.util.Arrays.copyOf(threads, n);
    }

    long[] getAllThreadIds() {
        final Thread[] threads = getAllThreads();
        long[] threadIds = new long[threads.length];
        for (int i = 0; i < threads.length; i++)
            threadIds[i] = threads[i].getId();
        return threadIds;
    }

    Thread getThread(final long id) {
        final Thread[] threads = getAllThreads();
        for (Thread thread : threads)
            if (thread.getId() == id)
                return thread;
        return null;
    }

    public synchronized void start(long mainTid) {
        if (isAlive())
            return;
        this.mainTid = mainTid;
        this.maxMemory = (long) (((double) Runtime.getRuntime().maxMemory()) * maxMemRatio);
        this.threadMB.setThreadAllocatedMemoryEnabled(true);
        start();
    }

    @Override
    public void run() {
        Runtime runtime = Runtime.getRuntime();
        long tid = getId();
        int iters = 0;
        while (true) {
            if (iters++ == 0) {
                // Determine whether a loop has extended beyond the alloted 
                // time limit.  Mark all such loops as finished and remove
                // them from the queue.
                curTimestamp = new Timestamp(maxLoopTime);
                curTimestampDef = new Timestamp(maxLoopTimeDef);
                Iterator<Timestamp> iter = timestamps.iterator();
                while (iter.hasNext()) {
                    Timestamp timestamp = iter.next();
                    if (timestamp.update(SLOW_TASK_SLEEP))
                        iter.remove();
                }
                Iterator<Timestamp> iterDef = timestampsDef.iterator();
                while (iterDef.hasNext()) {
                    Timestamp timestamp = iterDef.next();
                    if (timestamp.update(SLOW_TASK_SLEEP))
                        iterDef.remove();
                }
                timestamps.addLast(curTimestamp);
                timestampsDef.addLast(curTimestampDef);

                synchronized (deadlockedThreadIds) {
                    long threadIds[] = threadMB.findDeadlockedThreads();
                    if (threadIds != null) { // this covers cyclic deadlocks
                        this.memAlloc = -1;
                        deadlockedThreadIds.clear();
                        for (long id : threadIds) {
                            deadlockedThreadIds.add(id);
                        }
                        interruptedThread = getThread(deadlockedThreadIds.get(0));
                        interruptedThread.interrupt();
                    } else { // this covers more complicated deadlocks
                        // TODO we might want to make this run less often, since it
                        // is more expensive
                        Thread[] threads = getAllThreads();
                        for (Thread b : threads) {
                            if (b.getState() == State.WAITING) {
                                for (Thread a : threads) {
                                    if (a == b || a.ss_lobjs == null || a.ss_join != b || a.ss_lobjs.isEmpty())
                                        continue;
                                    // B is waiting, so look for another thread A that holds the lock
                                    // that B is waiting on.
                                    boolean foundMatch = false;
                                    ThreadInfo threadInfo = threadMB.getThreadInfo(b.getId());
                                    int lockId = threadInfo.getLockInfo().getIdentityHashCode();
                                    for (Lock lock : a.ss_lobjs) {
                                        if (!(lock instanceof ReentrantLock))
                                            continue;
                                        if (System.identityHashCode((((ReentrantLock) lock).sync)) == lockId) {
                                            foundMatch = true;
                                            break;
                                        }
                                    }
                                    if (foundMatch) {
                                        // Let's interrupt the thread that is waiting, and
                                        // not the thread that we are joining on.
                                        this.memAlloc = -1;
                                        deadlockedThreadIds.clear();
                                        deadlockedThreadIds.add(a.getId());
                                        deadlockedThreadIds.add(b.getId());
                                        interruptedThread = b;
                                        interruptedThread.interrupt();
                                    }
                                }
                            }
                        }
                    }
                }
                // we need to reclaim memory from the thread that was
                // killed
                if (memAlloc > 0) {
                    System.gc();
                }
            }
            if (iters > SLEEP_SCALE)
                iters = 0;

            final long usedMem = runtime.totalMemory() - runtime.freeMemory();
            if (memAlloc > 0 && usedMem < maxMemory) {
                // wait until we fall back under the memory threshold
                // before we start interrupting threads.
                memAlloc = -1;
            } else if (memAlloc < 0 && usedMem >= maxMemory) {
                long[] threadIds = getAllThreadIds();
                long[] memAllocs = threadMB.getThreadAllocatedBytes(threadIds);
                long killTid = -1;
                long maxAlloc = Long.MIN_VALUE;
                for (int i = 0; i < memAllocs.length; i++) {
                    // Do not kill the main thread or this one...
                    long id = threadIds[i];
                    if (id == mainTid || id == tid)
                        continue;
                    if (memAllocs[i] > maxAlloc) {
                        killTid = threadIds[i];
                        maxAlloc = memAllocs[i];
                    }
                }
                if (killTid >= 0) {
                    synchronized (deadlockedThreadIds) {
                        memAlloc = maxAlloc;
                        interruptedThread = getThread(killTid);
                        interruptedThread.interrupt();
                    }
                }
            }

            try {
                // TODO may consider making the sleep depend on how close we 
                // are to maxMemory
                Thread.sleep(FAST_TASK_SLEEP);
            } catch (InterruptedException e) {

            }

        }
    }

    /**
     * Mimic's a MONITORENTER instruction using a ReentrantLock.
     * 
     * @param o
     *            Object to lock
     */
    public void monitorenter(Object o) {
        Lock lock;
        synchronized (lockMap) {
            lock = lockMap.get(o);
            if (lock == null) {
                lock = new ReentrantLock();
                lockMap.put(o, lock);
            }
        }

        monitorenter(lock);
    }

    public void monitorenter(Lock lock) {
        while (true) {
            try {
                lock.lockInterruptibly();
                break;
            } catch (InterruptedException e) {
                handleInterrupts();
            }
        }
    }

    public void handleInterrupts() {
        String errorMsg = null;
        Thread curThread = Thread.currentThread();
        int cwe = 0;
        synchronized (deadlockedThreadIds) {
            // make sure that the current thread that's interrupted
            // is the one that caused the deadlock.
            if (interruptedThread == curThread) {
                if (memAlloc > 0) {
                    errorMsg = "Interrupting " + curThread + " due to a memory over-allocation: " + memAlloc + " bytes";
                    cwe = 400;
                } else if (openFiles > 0) {
                    errorMsg = "Interrupting " + curThread + " due to too many open files: " + openFiles
                            + " files open.";
                    cwe = 774;
                    // clean up the file handles and update the total number
                    // of open files...
                    synchronized (openFileLock) {
                        openFiles = -1;
                        Iterator<Closeable> closeables = interruptedThread.ss_openfiles.iterator();
                        while (closeables.hasNext()) {
                            try {
                                closeables.next().close();
                            } catch (IOException e) {
                                // ignore the exception
                            } finally {
                                closeables.remove();
                                totalOpenFiles--;
                            }
                        }
                    }
                } else if (BaseConfig.getInstance().areDeadlocksEnabled() && !deadlockedThreadIds.isEmpty()) {
                    errorMsg = "Interrupting " + curThread + " due to a detected deadlock condition on thread ids: "
                            + deadlockedThreadIds;
                    cwe = 833;
                    deadlockedThreadIds.clear();
                    interruptedThread = null;
                }
            }
        }
        if (errorMsg != null) {
            NotifyMsg notifyMsg = new NotifyMsg("DeadlockDetector.monitorenter()", "DeadlockDetector.monitorenter()",
                    cwe);
            notifyMsg.setAction(RunChecks.EXCEPTION_ACTION);
            notifyMsg.append(errorMsg);
            try {
                notifyMsg.prepareForExceptionOrTerminate(CleartrackException.class.getConstructor(String.class),
                                                         RunChecks.EXCEPTION_ACTION);
            } catch (NoSuchMethodException | SecurityException e2) {
                // Would never see this
            }
            Notify.notifyAndRespond(notifyMsg);
        }
    }

    /**
     * Mimic's a MONITOREXIT instruction using a ReentrantLock.
     * 
     * @param o
     *            Object to unlock
     */
    public void monitorexit(Object o) {
        Lock lock;
        synchronized (lockMap) {
            lock = lockMap.get(o);
            if (lock == null) {
                throw new RuntimeException("The lock should not be null");
            }
        }
        lock.unlock();
    }

    /**
     * Mimic's Object.wait() calls, using the ReentrantLock's condition that was
     * generated.
     * 
     * @param obj
     *            Object to wait on
     * @throws InterruptedException
     */
    public void _wait(Object obj) throws InterruptedException {
        if (!BaseConfig.getInstance().areDeadlocksEnabled()) {
            obj.wait();
            return;
        }
        Condition cond;
        synchronized (condMap) {
            cond = condMap.get(obj);
            if (cond == null) {
                synchronized (lockMap) {
                    Lock lock = lockMap.get(obj);
                    if (lock == null) {
                        throw new RuntimeException("The lock should not be null");
                    }
                    cond = lock.newCondition();
                    condMap.put(obj, cond);
                }
            }
        }
        cond.await();
    }

    /**
     * Mimic's Object.wait() calls, using the ReentrantLock's condition that was
     * generated.
     * 
     * @param obj
     *            Object to wait on
     * @param timeout
     *            long of the timeout (in milliseconds).
     * @throws InterruptedException
     */
    public void _wait(Object obj, long timeout) throws InterruptedException {
        if (!BaseConfig.getInstance().areDeadlocksEnabled()) {
            obj.wait(timeout);
            return;
        }
        Condition cond;
        synchronized (condMap) {
            cond = condMap.get(obj);
            if (cond == null) {
                synchronized (lockMap) {
                    Lock lock = lockMap.get(obj);
                    if (lock == null) {
                        throw new RuntimeException("The lock should not be null");
                    }
                    cond = lock.newCondition();
                    condMap.put(obj, cond);
                }
            }
        }
        cond.await(timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * Mimic's Object.wait() calls, using the ReentrantLock's condition that was
     * generated.
     * 
     * @param obj
     *            Object to wait on
     * @param timeout
     *            long of the timeout (in milliseconds).
     * @param nanos
     *            int of additional timeout (in nanoseconds).
     * @throws InterruptedException
     */
    public void _wait(Object obj, long timeout, int nanos) throws InterruptedException {
        if (!BaseConfig.getInstance().areDeadlocksEnabled()) {
            obj.wait(timeout, nanos);
            return;
        }
        Condition cond;
        synchronized (condMap) {
            cond = condMap.get(obj);
            if (cond == null) {
                synchronized (lockMap) {
                    Lock lock = lockMap.get(obj);
                    if (lock == null) {
                        throw new RuntimeException("The lock should not be null");
                    }
                    cond = lock.newCondition();
                    condMap.put(obj, cond);
                }
            }
        }
        if (!cond.await(timeout, TimeUnit.MILLISECONDS))
            cond.awaitNanos(nanos);
    }

    /**
     * Mimic's Object.notify() calls, using the ReentrantLock's condition that
     * was generated.
     * 
     * @param obj
     *            Object to notify on
     */
    public void _notify(Object obj) {
        if (!BaseConfig.getInstance().areDeadlocksEnabled()) {
            obj.notify();
            return;
        }
        Condition cond;
        synchronized (condMap) {
            cond = condMap.get(obj);
            if (cond == null) {
                synchronized (lockMap) {
                    Lock lock = lockMap.get(obj);
                    if (lock == null) {
                        throw new RuntimeException("The lock should not be null");
                    }
                    cond = lock.newCondition();
                    condMap.put(obj, cond);
                }
            }
        }
        cond.signal();
    }

    /**
     * Mimic's Object.notifyAll() calls, using the ReentrantLock's condition
     * that was generated.
     * 
     * @param obj
     *            Object to notify all on
     */
    public void _notifyAll(Object obj) {
        if (!BaseConfig.getInstance().areDeadlocksEnabled()) {
            obj.notifyAll();
            return;
        }
        Condition cond;
        synchronized (condMap) {
            cond = condMap.get(obj);
            if (cond == null) {
                synchronized (lockMap) {
                    Lock lock = lockMap.get(obj);
                    if (lock == null) {
                        throw new RuntimeException("The lock should not be null");
                    }
                    cond = lock.newCondition();
                    condMap.put(obj, cond);
                }
            }
        }
        cond.signalAll();
    }

    /**
     * This method should be called when creating a closeable that is
     * backed by an open file descriptor.  If we exceed some limit
     * on the number of open files (at set by the config file) we
     * will interrupt the offending thread.  Once this interrupt is
     * detected, a CleartrackException will be raised.
     * 
     * @param c Closeable
     */
    public void addCloseable(Closeable c) {
        synchronized (openFileLock) {
            Thread curThread = Thread.currentThread();
            if (totalOpenFiles >= maxFiles) {
                // interrupt the thread with the most number of open files.
                Thread[] threads = getAllThreads();
                Thread offendingThread = null;
                int maxSize = -1;
                for (Thread b : threads) {
                    Set<Closeable> openfileSet = b.ss_openfiles;
                    if (openfileSet == null) {
                        continue;
                    }
                    int size = openfileSet.size();
                    if (maxSize < size) {
                        offendingThread = b;
                        maxSize = size;
                    }
                }
                if (offendingThread == null) // don't think this should ever happen
                    offendingThread = curThread;
                openFiles = maxSize;
                interruptedThread = offendingThread;
                if (offendingThread == curThread) {
                    // don't forget to close the pending closeable
                    try {
                        c.close();
                    } catch (IOException e) {
                        // ignore the exception
                    }
                    // throw the CleartrackException here, don't bother with an interrupt.
                    handleInterrupts();
                } else {
                    interruptedThread.interrupt();
                }
            }
            if (curThread.ss_openfiles == null) {
                curThread.ss_openfiles = new HashSet<Closeable>();
            }
            if (curThread.ss_openfiles.add(c))
                totalOpenFiles++;
        }
    }

    /**
     * This method should be called once a closeable's close() method has
     * been called.
     * 
     * @param c Closeable
     */
    public void removeCloseable(Closeable c) {
        synchronized (openFileLock) {
            Thread curThread = Thread.currentThread();
            if (curThread.ss_openfiles == null) {
                curThread.ss_openfiles = new HashSet<Closeable>();
                return;
            }
            if (curThread.ss_openfiles.remove(c))
                totalOpenFiles--;
        }
    }

    /**
     * Metadata that we keep for active loops.  The update should be maintained
     * by the daemon thread and should be reasonably accurate, by passing the
     * time that the daemon thread sleeps (per iteration).
     * 
     * @author jeikenberry
     */
    public class Timestamp {
        private long time;
        private int timeout;
        private boolean finished;

        Timestamp(int timeout) {
            this.time = 0;
            this.timeout = timeout;
            this.finished = false;
        }

        /**
         * Updates this Timestamp, and determines whether this Timestamp
         * is still alive.
         * 
         * @param time int of the time increment (in milliseconds)
         * @return true if this Timestamp object is stale.
         */
        private boolean update(int time) {
            this.time += time;
            if (this.time >= timeout)
                finished = true;
            return finished;
        }

        public boolean isFinished() {
            return finished;
        }
    }
}
