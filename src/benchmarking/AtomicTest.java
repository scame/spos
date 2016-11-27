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
        value.getAndAdd(preLoaded[i]);
        if (++i >= SIZE) {
            index.set(0);
        }
    }

    @Override
    protected long read() {
        return value.get();
    }
}
