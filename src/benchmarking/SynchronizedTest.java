package benchmarking;


public class SynchronizedTest extends Accumulator {

    {
        description = "SynchronizedTest";
    }

    @Override
    protected synchronized void accumulate() {
        value += preLoaded[index++];
        if (index >= SIZE) {
            index = 0;
        }
    }

    @Override
    protected synchronized long read() {
        return value;
    }
}
