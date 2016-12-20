package dekker;


import jcip.ThreadSafe;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class FixnumLockImpl implements FixnumLock {

    private int threadsNumber = 2;

    private final List<Boolean> threadsIdList = new ArrayList<>(threadsNumber);

    private final Lock lock = new ReentrantLock(true);

    private int threadId = -1;

    @Override
    public int getId() {
        return threadId;
    }

    @ThreadSafe
    @Override
    public boolean register() {
        lock.lock();
        try {
            return tryRegister();
        } finally {
            lock.unlock();
        }
    }

    private boolean tryRegister() {
        if (freeIdExists()) {
            threadId = getFreeId();
            return threadsIdList.set(threadId, true);
        }
        return false;
    }

    private boolean freeIdExists() {
        boolean freeIdFlag = false;

        for (boolean idFlag : threadsIdList) {
            if (idFlag) {
                freeIdFlag = true;
            }
        }
        return freeIdFlag;
    }

    private int getFreeId() {
        int freeId = -1;

        for (int i = 0; i < threadsNumber; i++) {
            if (!threadsIdList.get(i)) {
                freeId = i;
            }
        }
        return freeId;
    }

    @ThreadSafe
    @Override
    public void unregister() {
        lock.lock();
        try {
            threadsIdList.set(threadId, false);
            resetId();
        } finally {
            lock.unlock();
        }
    }

    private void resetId() {
        threadId = -1;
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        throw new UnsupportedOperationException("lock interruptibly is unsupported");
    }

    @Override
    public boolean tryLock() {
        throw new UnsupportedOperationException("try lock is unsupported");
    }

    @Override
    public boolean tryLock(long l, TimeUnit timeUnit) throws InterruptedException {
        throw new UnsupportedOperationException("try lock is unsupported");
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException("condition variables aren't supported");
    }

    public int getThreadsNumber() {
        return threadsNumber;
    }

    public void setThreadsNumber(int threadsNumber) {
        this.threadsNumber = threadsNumber;
    }
}
