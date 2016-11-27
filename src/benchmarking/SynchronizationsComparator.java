package benchmarking;


public class SynchronizationsComparator {

    private static final int ITERATIONS = 5;

    private static final AtomicTest atomicTest = new AtomicTest();
    private static final LockTest lockTest = new LockTest();
    private static final SpinLockTest spinLockTest = new SpinLockTest();
    private static final SynchronizedTest synchronizedTest = new SynchronizedTest();
    private static final ThreadUnsafeTest threadUnsafeTest = new ThreadUnsafeTest();

    private static void test() {
        System.out.printf("%-12s : %12d\n", "Cycles", Accumulator.getCYCLES());

        lockTest.test();
        spinLockTest.test();
        synchronizedTest.test();
        threadUnsafeTest.test();
        atomicTest.test();

        Accumulator.compare(atomicTest, lockTest);
        Accumulator.compare(atomicTest, spinLockTest);
        Accumulator.compare(atomicTest, synchronizedTest);
        Accumulator.compare(atomicTest, threadUnsafeTest);

        Accumulator.compare(lockTest, spinLockTest);
        Accumulator.compare(lockTest, synchronizedTest);
        Accumulator.compare(lockTest, threadUnsafeTest);

        Accumulator.compare(spinLockTest, synchronizedTest);
        Accumulator.compare(spinLockTest, threadUnsafeTest);

        Accumulator.compare(synchronizedTest, threadUnsafeTest);
    }

    public static void main(String[] args) {
        lockTest.test(); // eliminates cost of starting the thread pool

        for (int i = 0; i < ITERATIONS; i++) {
            test();
            Accumulator.setCYCLES(Accumulator.getCYCLES() * 2);
        }
        Accumulator.stopAccumulating();
    }
}
