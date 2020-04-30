package pac.inst.taint;

import pac.config.Notify;
import pac.config.NotifyMsg;
import pac.config.RunChecks;
import pac.config.CleartrackException;
import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationLocation;
import pac.inst.InstrumentationMethod;
import pac.inst.InvocationType;
import pac.util.ThreadMonitor;

@InstrumentationClass("java/lang/Thread")
public final class ThreadInstrumentation {

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP)
    public static final void join(Thread t) throws InterruptedException {
        Thread curThread = null;
        try {
            // mark t as the thread curThread is joining with, so we
            // may catch the deadlock in case t blocks indefinitely.
            curThread = Thread.currentThread();
            curThread.ss_join = t;
            t.join();
        } catch (InterruptedException e) {
            try {
                ThreadMonitor.INSTANCE.handleInterrupts();
            } catch (CleartrackException se) {
                // need to set the bit back in case they catch the
                // CleartrackException
                Thread.currentThread().interrupt();
                throw se;
            }
            throw e; // not our interrupt, so throw it
        } finally {
            // we have properly completed the join on t, so unset the
            // current threads join thread.
            if (curThread != null)
                curThread.ss_join = null;
        }
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP)
    public static final void join(Thread t, long millis) throws InterruptedException {
        Thread curThread = null;
        try {
            if (millis == 0) {
                curThread = Thread.currentThread();
                // mark t as the thread curThread is joining with, so we
                // may catch the deadlock in case t blocks indefinitely.
                // we only need to do this if we are not waiting
                // indefinitely.
                curThread.ss_join = t;
            }
            t.join(millis);
        } catch (InterruptedException e) {
            try {
                ThreadMonitor.INSTANCE.handleInterrupts();
            } catch (CleartrackException se) {
                // need to set the bit back in case they catch the
                // CleartrackException
                Thread.currentThread().interrupt();
                throw se;
            }
            throw e; // not our interrupt, so throw it
        } finally {
            // we have properly completed the join on t, so unset the
            // current threads join thread.
            if (curThread != null)
                curThread.ss_join = null;
        }
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP)
    public static final void join(Thread t, long millis, int nanos) throws InterruptedException {
        Thread curThread = null;
        try {
            if (nanos == 0 && millis == 0) {
                // mark t as the thread curThread is joining with, so we
                // may catch the deadlock in case t blocks indefinitely.
                // we only need to do this if we are not waiting
                // indefinitely.
                curThread = Thread.currentThread();
                curThread.ss_join = t;
            }
            t.join(millis, nanos);

        } catch (InterruptedException e) {
            try {
                ThreadMonitor.INSTANCE.handleInterrupts();
            } catch (CleartrackException se) {
                // need to set the bit back in case they catch the
                // CleartrackException
                Thread.currentThread().interrupt();
                throw se;
            }
            throw e; // not our interrupt, so throw it
        } finally {
            // we have properly completed the join on t, so unset the
            // current threads join thread.
            if (curThread != null)
                curThread.ss_join = null;
        }
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP, invocationType = InvocationType.STATIC)
    public static final void sleep(long millis) throws InterruptedException {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            try {
                ThreadMonitor.INSTANCE.handleInterrupts();
            } catch (CleartrackException se) {
                // need to set the bit back in case they catch the
                // CleartrackException
                Thread.currentThread().interrupt();
                throw se;
            }
            throw e; // not our interrupt, so throw it
        }
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP, invocationType = InvocationType.STATIC)
    public static final void sleep(long millis, int nanos) throws InterruptedException {
        try {
            Thread.sleep(millis, nanos);
        } catch (InterruptedException e) {
            try {
                ThreadMonitor.INSTANCE.handleInterrupts();
            } catch (CleartrackException se) {
                // need to set the bit back in case they catch the
                // CleartrackException
                Thread.currentThread().interrupt();
                throw se;
            }
            throw e; // not our interrupt, so throw it
        }
    }

    // TODO Why on earth can I not set canExtend=true in the annotation???
    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP)
    public static final void run(Thread thread) {
        if (thread.isAlive()) {
            // Just in case run() can recursively call itself or super.run() is called.
            // This should not loop recursively, since this replacement is done only
            // on instrumented application methods.  So, ChildThread.run(ret) will be
            // replaced with ThreadInstrumentation.run(ret) -> ChildThread.run() 
            // -> ChildThread.run(new Ret()).
            thread.run();
        } else {
            // Assume that the programmer meant to call Thread.start()
            thread.start();

            final NotifyMsg notifyMsg = new NotifyMsg("Thread.run()", "Thread.run()");
            notifyMsg.setCweNumber(572);
            notifyMsg.setAction(RunChecks.REPLACE_ACTION);
            notifyMsg.append("Thread " + thread + " improperly invoked Thread.run() when it should have"
                    + " invoked Thread.start().");
            Notify.notifyAndRespond(notifyMsg);
        }
    }

}
