package me.bhe.qilim;

/**
 * Created by hitsmaxft on 15/3/14.
 *
 * @author qixiang.mft <qixiang.mft@taobao.com>
 */

import kilim.RingQueue;
import kilim.ShutdownException;
import kilim.Task;

/**
 * 一个伪线程 , 因为 LW task 只会在当前线程中运行
 */
import java.util.concurrent.atomic.AtomicInteger;

public class LWThread {
    Task runningTask;

    RingQueue<Task> tasks      = new RingQueue<Task>(10);
    LWScheduler            scheduler;

    static AtomicInteger gid        = new AtomicInteger();

    public int           numResumes = 0;

    private String name;

    protected LWThread(String id) {
        this.name = id;
    }

    LWThread(LWScheduler ascheduler) {
        this("KilimWorker-" + gid.incrementAndGet());
        scheduler = ascheduler;
    }

    public void run() {
        try {
            while (true) {
                Task t = getNextTask(this); // blocks until task available
                runningTask = t;
                ((LWTask)t)._runExecute(this);
                runningTask = null;
            }
        } catch (ShutdownException se) {
            // nothing to do.
        } catch (OutOfMemoryError ex) {
            System.err.println("Out of memory");
            System.exit(1);
        } catch (Throwable ex) {
            ex.printStackTrace();
            System.err.println(runningTask);
        }
        runningTask = null;
    }

    protected Task getNextTask(LWThread workerThread) throws ShutdownException {
        Task t = null;
        while (true) {
            if (scheduler.isShutdown())
                throw new ShutdownException();

            t = getNextTask();
            if (t != null)
                break;

            // try loading from scheduler
            scheduler.loadNextTask(this);
            synchronized (this) { // ///////////////////////////////////////
                // Wait if still no task to execute.
                t = tasks.get();
                if (t != null)
                    break;

                scheduler.addWaitingThread(this);
                try {
                    wait();
                } catch (InterruptedException ignore) {
                } // shutdown indicator checked above
            } // //////////////////////////////////////////////////////////
        }
        assert t != null : "Returning null task";
        return t;
    }

    public Task getCurrentTask() {
        return runningTask;
    }

    public void addRunnableTask(Task t) {
        //assert t.preferredResumeThread == null || t.preferredResumeThread == this : "Task given to wrong thread";
        tasks.put(t);
        //notify();
    }

    public synchronized boolean hasTasks() {
        return tasks.size() > 0;
    }

    public synchronized Task getNextTask() {
        return tasks.get();
    }

    public synchronized void waitForMsgOrSignal() {
        try {
            if (tasks.size() == 0) {
                wait();
            }
        } catch (InterruptedException ignore) {
        }
    }
}

