package me.bhe.qilim;

import kilim.RingQueue;
import kilim.ShutdownException;
import kilim.Task;

import java.util.LinkedList;

/**
 * Created by hitsmaxft on 15/3/14.
 *
 * 一个轻量级的单线程 fiber 实现
 *
 * @author qixiang.mft <qixiang.mft@taobao.com>
 */
public class LWScheduler {
    public static int defaultNumberThreads;

    public LinkedList<LWThread> allThreads = new LinkedList<LWThread>();

    public RingQueue<LWThread> waitingThreads = new RingQueue<LWThread>(10);

    protected boolean shutdown = false;

    public RingQueue<Task> runnableTasks = new RingQueue<Task>(100);

    static {
        String s = System.getProperty("kilim.Scheduler.numThreads");
        if (s != null) {
            try {
                defaultNumberThreads = Integer.parseInt(s);
            } catch(Exception e) {}
        }
        if (defaultNumberThreads == 0) {
            defaultNumberThreads = Runtime.getRuntime().availableProcessors();
        }
    }
    protected LWScheduler() {}

    public LWScheduler(int numThreads) {
        // 永远都只有一个 worker thread
        LWThread wt = new LWThread(this);
        allThreads.add(wt);
        addWaitingThread(wt);
    }

    void addWaitingThread(LWThread wt) {
        waitingThreads.put(wt);
    }

    LWThread getWaitingThread() {
        return waitingThreads.get();
    }

    /**
     * Schedule a task to run. It is the task's job to ensure that
     * it is not scheduled when it is runnable.
     */
    public void schedule(Task t) {
        LWThread wt = null;
        runnableTasks.put(t);

        wt = getWaitingThread();
    }

    public void shutdown() {
        shutdown = true;
        for (LWThread wt: allThreads) {
        }
    }

    public boolean isShutdown() {
        return shutdown;
    }

    /**
     * This is called in the LWThread's stack. It transfers a runnable task to the given worker thread's
     * list of runnables. If the task prefers a different worker thread, then the search continues (after notifying
     * the other thread that it has a task to execute).
     *
     * @return
     */
    void loadNextTask(LWThread wt) throws ShutdownException {
        while (true) {
            Task t = null;
            LWThread prefThread = null;

            if (shutdown) throw new ShutdownException();

            t = runnableTasks.get();
            if (t == null) {
                // LWThread will add itself to waitingThreads in LWThread.getNextTask()
                break;
            } else {
                prefThread = t.preferredResumeThread;
                if (prefThread == null || prefThread == wt) {
                    wt.addRunnableTask(t);
                    break; // Supplied worker thread has work to do
                } else {
                    // The task states a preferred thread which is not the supplied worker thread
                    // Enqueue it and continue searching.
                    prefThread.addRunnableTask(t);
                }
            }
        }
    }

    public void dump() {
        System.out.println(runnableTasks);
    }
}
