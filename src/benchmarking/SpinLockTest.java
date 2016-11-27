package benchmarking;


public class SpinLockTest extends Accumulator {

    {
        description = "SpinLockTest";
    }

    private final SpinLock spinLock = new SpinLock();

    @Override
    protected void accumulate() {
        spinLock.lock();
        try {
            value += preLoaded[index++];
            if (index >= SIZE) {
                index = 0;
            }
        } finally {
            spinLock.unlock();
        }
    }

    @Override
    protected long read() {
        spinLock.lock();
        try {
            return value;
        } finally {
            spinLock.unlock();
        }
    }
}
