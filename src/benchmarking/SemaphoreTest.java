package benchmarking;


import java.util.concurrent.Semaphore;

public class SemaphoreTest extends Accumulator {

    {
        description = "semaphoreTest";
    }

    private final Semaphore semaphore = new Semaphore(1);

    @Override
    protected void accumulate() {
        try {
            semaphore.acquire();
            try {
                updateValue();
                updateIndex();
            } finally {
                semaphore.release();
            }
        } catch (InterruptedException e) {
            System.out.println(e.getLocalizedMessage());
        }
    }

    private void updateValue() {
        value += preLoaded[index++];
    }

    private void updateIndex() {
        if (index >= SIZE) {
            index = 0;
        }
    }

    @Override
    protected long read() {
        return 0;
    }
}
