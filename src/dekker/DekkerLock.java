package dekker;


import java.util.Arrays;
import java.util.List;

public class DekkerLock extends FixnumLockImpl {

    private static final int numberOfThreads = 2;

    private final List<Boolean> wantCS = Arrays.asList(false, false);

    private int turn = 1;

    public DekkerLock() {
        super(numberOfThreads);
    }

    @Override
    public void lock() {
        if (register()) {
            requestCS(getId());
        }
    }

    @Override
    public void unlock() {
        releaseCS(getId());
        unregister();
    }

    private void requestCS(int i) {
        int j = 1 - i;
        wantCS.set(i, true);

        while (wantCS.get(j)) {
            if (turn == j) {
                wantCS.set(i, false);
                while (turn == j) { Thread.yield(); }
                wantCS.set(i, true);
            }
        }
    }

    private void releaseCS(int i) {
        turn = 1 - i;
        wantCS.set(i, false);
    }
}
