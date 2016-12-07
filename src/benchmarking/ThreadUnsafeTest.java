package benchmarking;

// isn't reliable (increment in java isn't Atomic, indexing problem, addition also isn't atomic)
// but gives rough performance estimate
public class ThreadUnsafeTest extends Accumulator {

    {
        description = "ThreadUnsafeTest";
    }

    @Override
    protected void accumulate() {
        if (index >= SIZE / 2) {
            index = 0;
        }
        value += preLoaded[index++];
    }

    @Override
    protected long read() {
        return value;
    }
}
