package benchmarking;


public class ThreadUnsafeTest extends Accumulator {

    {
        description = "ThreadUnsafeTest";
    }

    @Override
    protected void accumulate() {
        value += preLoaded[index++];
        if (index >= SIZE) {
            index = 0;
        }
    }

    @Override
    protected long read() {
        return value;
    }
}
