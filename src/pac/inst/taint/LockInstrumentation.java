package pac.inst.taint;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import pac.config.BaseConfig;
import pac.config.Notify;
import pac.config.NotifyMsg;
import pac.config.RunChecks;
import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationLocation;
import pac.inst.InstrumentationMethod;
import pac.util.ThreadMonitor;

@InstrumentationClass(value = "java/util/concurrent/locks/Lock", isInterface = true)
public class LockInstrumentation {

  // Note: we only need to instrument lock() since unlock() remains the same, and the other "try
  // lock" mechanisms we assume the programmer will interrupt the locks himself and do the right
  // thing.
  //
  // If the programmer does not do the right thing and a deadlock were to occur, our daemon thread
  // will still catch it, but it would just not be reported as a CWE. We could write individual
  // wrappers for the methods, if at some point we feel the need to.

  @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP, canExtend = true)
  public static final void lock(Lock lock) {
    if (!BaseConfig.getInstance().areDeadlocksEnabled()) {
      lock.lock();
    } else {
      ThreadMonitor.INSTANCE.monitorenter(lock);
    }
    Thread curThread = Thread.currentThread();
    if (curThread.ss_lobjs == null)
      curThread.ss_lobjs = new HashSet<Lock>();
    curThread.ss_lobjs.add(lock);
  }

  @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP, canExtend = true)
  public static final boolean tryLock(Lock lock) {
    boolean result = lock.tryLock();
    Thread curThread = Thread.currentThread();
    if (curThread.ss_lobjs == null)
      curThread.ss_lobjs = new HashSet<Lock>();
    curThread.ss_lobjs.add(lock);
    return result;
  }

  @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP, canExtend = true)
  public static final boolean tryLock(Lock lock, long time, TimeUnit unit)
      throws InterruptedException {
    boolean result = lock.tryLock(time, unit);
    Thread curThread = Thread.currentThread();
    if (curThread.ss_lobjs == null)
      curThread.ss_lobjs = new HashSet<Lock>();
    curThread.ss_lobjs.add(lock);
    return result;
  }

  @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP, canExtend = true)
  public static final void lockInterruptibly(Lock lock) throws InterruptedException {
    lock.lockInterruptibly();
    Thread curThread = Thread.currentThread();
    if (curThread.ss_lobjs == null)
      curThread.ss_lobjs = new HashSet<Lock>();
    curThread.ss_lobjs.add(lock);
  }

  @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP, canExtend = true)
  public static final void unlock(Lock lock) {
    if (lock instanceof ReentrantLock) {
      // If this lock had not been previously lock, then presume this was not the application
      // intended and ignore the call.
      ReentrantLock reLock = (ReentrantLock) lock;
      Thread curThread = Thread.currentThread();
      if (!reLock.isHeldByCurrentThread()) {
        final NotifyMsg notifyMsg = new NotifyMsg("Lock.unlock()", "Lock.unlock()");
        notifyMsg.setCweNumber(765);
        notifyMsg.setAction(RunChecks.REMOVE_ACTION);
        notifyMsg.append("Thread id " + curThread.getId() + " does not currently own lock " + lock
            + " and therefore this call will be ignored.");
        Notify.notifyAndRespond(notifyMsg);
        return;
      }
      lock.unlock();
      if (curThread.ss_lobjs != null) {
        // If this thread no longer holds this lock, then we may safely remove it from the list.
        if (!reLock.isHeldByCurrentThread())
          curThread.ss_lobjs.remove(lock);
      }
    } else {
      // This may not always work, but assume that other locks are non-reentrant and we really have
      // no way of checking to see if the lock is held by the current thread, for general locks.
      lock.unlock();
      Thread curThread = Thread.currentThread();
      if (curThread.ss_lobjs != null) {
        curThread.ss_lobjs.remove(lock);
      }
    }
  }

}
