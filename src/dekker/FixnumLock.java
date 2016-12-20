package dekker;


import java.util.concurrent.locks.Lock;

public interface FixnumLock extends Lock {

    int getId();

    boolean register();

    void unregister();
}
