package dekker;


import jcip.ThreadSafe;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class FixnumLockImpl implements FixnumLock {

    private final int threadsNumber;

    private final Lock lock = new ReentrantLock(true);

    protected final List<Boolean> threadsIdList;

    private ThreadLocal<Integer> threadId = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return -1;
        }
    };

    public FixnumLockImpl(int threadsNumber) {
        this.threadsNumber = threadsNumber;
        this.threadsIdList = new ArrayList<>(threadsNumber);
        initIdsList();
    }

    private void initIdsList() {
        for (int i = 0; i < threadsNumber; i++) {
            threadsIdList.add(false);
        }
    }

    @Override
    public int getId() {
        return threadId.get();
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
            threadId.set(getFreeId());
            return threadsIdList.set(threadId.get(), true);
        }
        return false;
    }

    private boolean freeIdExists() {
        boolean freeIdFlag = false;

        for (boolean idFlag : threadsIdList) {
            if (!idFlag) {
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

    /**
     * should be called in the end of critical section
     * API user ought to use this method only after a successful register call
     */

    @ThreadSafe
    @Override
    public void unregister() {
        lock.lock();
        try {
            checkRegistration();
            threadsIdList.set(threadId.get(), false);
            resetId();
        } finally {
            lock.unlock();
        }
    }

    private void checkRegistration() {
        if (threadId.get() == -1) {
            try {
                throw new IllegalAccessException("thread wasn't registered");
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void resetId() {
        threadId.set(-1);
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
}
