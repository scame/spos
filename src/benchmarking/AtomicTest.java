package benchmarking;


import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

// isn't reliable, but gives rough performance estimate
public class AtomicTest extends Accumulator {

    {
        description = "AtomicTest";
    }

    private final AtomicInteger index = new AtomicInteger(0);
    private final AtomicLong value = new AtomicLong(0);

    @Override
    protected void accumulate() {
        int i = index.getAndIncrement();
        if (i >= SIZE / 2) {
            index.set(0);
            i = index.getAndIncrement();
        }
        value.getAndAdd(preLoaded[i]);
    }

    @Override
    protected long read() {
        return value.get();
    }
}
