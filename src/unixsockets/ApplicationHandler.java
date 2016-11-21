package unixsockets;


import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ApplicationHandler {

    private static final String CLASS_PATH = "/home/scame/IdeaProjects/spos/out/production/spos:/home/scame/programming/junixsocket-1.3/dist/junixsocket-1.3.jar";

    private Server server;

    private final Lock mutex = new ReentrantLock(true);

    private void run() throws IOException {
        server = new Server(this);
        server.runApp();
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
        runChildProcess("unixsockets/FirstProcess");
        runChildProcess("unixsockets/SecondProcess");
    }

    private void runChildProcess(String processSource) {
        System.out.println(processSource);
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("java", "-classpath", CLASS_PATH, processSource, "java.io.tmpdir", "junixsocket-test.sock");
        try {
            processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        new ApplicationHandler().run();
    }
}
