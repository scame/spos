package dekker;


import jcip.NotThreadSafe;

public class RaceConditionDemonstration {

    private static final int NUMBER_OF_ITERATIONS = 1_000_00;

    private volatile long counter;

    private Thread firstThread;

    private Thread secondThread;

    private DekkerLock dekkerLock = new DekkerLock();

    @NotThreadSafe
    private void increment() throws InterruptedException {
        ++counter;
    }

    private void manipulate() {
        for (int i = 0; i < NUMBER_OF_ITERATIONS; i++) {
            try {
                increment();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void runDemonstration() throws InterruptedException {
        createThreads();
        runThreads();
        waitForThreadsToFinish();
        report();
    }

    private void report() {
        System.out.println("Expected value: " + (NUMBER_OF_ITERATIONS * 2)  + "\n" +
                            "Real value: " + counter);
    }

    private void createThreads() {
        firstThread = new Thread(this::manipulate);
        secondThread = new Thread(this::manipulate);
    }

    private void runThreads() {
        firstThread.start();
        secondThread.start();
    }

    private void waitForThreadsToFinish() throws InterruptedException {
        firstThread.join();
        secondThread.join();
    }

    public static void main(String[] args) throws InterruptedException {
        new RaceConditionDemonstration().runDemonstration();
    }
}
