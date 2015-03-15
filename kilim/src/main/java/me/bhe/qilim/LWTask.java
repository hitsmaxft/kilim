package me.bhe.qilim;

import kilim.*;

/**
 * Created by hitsmaxft on 15/3/14.
 *
 * @author qixiang.mft <qixiang.mft@taobao.com>
 */
public class LWTask extends Task implements EventSubscriber {

    private LWScheduler lwscheduler;

    @Override
    public void _runExecute(WorkerThread thread) throws NotPausable {
        super._runExecute(thread);
    }

    public void _runExecute(LWThread thread) throws NotPausable {
    }

    @Override
    public boolean resume() {
        if (lwscheduler == null) return false;
        if (done || running) return false;

        running = true;
        lwscheduler.schedule(this);
        return true ;
    }

    /**
     * 触发任务执行(前提是有消息)
     * @return
     */
    public Task start() {
        if (lwscheduler == null) {
            lwscheduler = new LWScheduler();
        }
        resume();
        return this;
    }

    public void onEvent(EventPublisher ep, Event e) {
        resume();
    }
}
