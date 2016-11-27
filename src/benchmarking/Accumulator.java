package benchmarking;


import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class Accumulator {

    protected volatile int index;

    protected volatile long value;

    protected long duration;

    protected String description;

    protected final static int SIZE = 100000;

    protected static final int [] preLoaded = new int[SIZE];

    static {
        Random random = new Random(1);
        for (int i = 0; i < SIZE; i++) {
            preLoaded[i] = random.nextInt();
        }
    }

    private static int CYCLES = 50000;

    private static final int CONTENTION_NUMBER = 5; // readers/writers

    private static final ExecutorService executorService = Executors.newFixedThreadPool(CONTENTION_NUMBER * 2);

    private static final CyclicBarrier cyclicBarrier = new CyclicBarrier(CONTENTION_NUMBER * 2 + 1);

    protected abstract void accumulate();

    protected abstract long read();

    private class Writer implements Runnable {

        @Override
        public void run() {
            for (int i = 0; i < CYCLES; i++) {
                accumulate();
            }
            try {
                cyclicBarrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                System.out.println(e.getLocalizedMessage());
            }
        }
    }

    private class Reader implements Runnable {
        private volatile long consumedVal;

        @Override
        public void run() {
            for (int i = 0; i < CYCLES; i++) {
                consumedVal = read();
            }
            try {
                cyclicBarrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                System.out.println(e.getLocalizedMessage());
            }
        }
    }

    public void test() {
        long start = System.nanoTime();

        for (int i = 0; i < CONTENTION_NUMBER; i++) {
            executorService.submit(new Writer());
            executorService.submit(new Reader());
        }
        try {
            cyclicBarrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            System.out.println(e.getLocalizedMessage());
        }
        duration = System.nanoTime() - start;
        System.out.printf("%-12s: %12d\n", description, duration);
    }

    public static void compare(Accumulator firstAccumulator, Accumulator secondAccumulator) {
        System.out.printf("%-20s: %.2f\n",
                firstAccumulator.description + "/" + secondAccumulator.description,
                (double) firstAccumulator.duration / secondAccumulator.duration
        );
    }

    public static void setCYCLES(int CYCLES) {
        Accumulator.CYCLES = CYCLES;
    }

    public static int getCYCLES() {
        return CYCLES;
    }
}
