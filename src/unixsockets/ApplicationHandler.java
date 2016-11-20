package unixsockets;


import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ApplicationHandler {

    private Server server;

    private final Lock mutex = new ReentrantLock(true);

    private void run() throws IOException {
        server = new Server(this);
        server.runServer();
    }

    void printOutput(String output) {
        mutex.lock();
        try {
            System.out.println(output);
        } finally {
            mutex.unlock();
        }
    }

    void runChildProcesses() {

    }

    public static void main(String[] args) throws IOException {
        new ApplicationHandler().run();
    }
}
