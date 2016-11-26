package benchmarking;


import java.util.concurrent.atomic.AtomicReference;

public class SpinLock {

    private AtomicReference<Thread> owner = new AtomicReference<>();

    private int count = 0;

    public void lock() {
        Thread currentThread = Thread.currentThread();

        if (currentThread == owner.get()) {
            count++;
            return;
        }

        while (!owner.compareAndSet(null, currentThread)) { }
    }

    public void unlock() {
        Thread currentThread = Thread.currentThread();

        if (owner.get() == currentThread) {
            if (count != 0) {
                count--;
            } else {
                owner.compareAndSet(currentThread, null);
            }
        }
    }
}
