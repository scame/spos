package hidden.unixsockets;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ApplicationHandler {

    private static final String CLASS_PATH = "/home/scame/IdeaProjects/spos/out/production/spos:/home/scame/programming/junixsocket-1.3/dist/junixsocket-1.3.jar";

    private static final int SCHEDULING_PERIOD = 3;
    private static final int INITIAL_DELAY = 3;

    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);

    private final ExecutorService daemonService = Executors.newFixedThreadPool(1, runnable -> {
        Thread thread = Executors.defaultThreadFactory().newThread(runnable);
        thread.setDaemon(true);
        return thread;
    });

    private Server server;

    private final Lock mutex = new ReentrantLock(true);

    private enum CancellationMode {KEY_PRESS, PROMPT}

    private CancellationMode cancellationMode;

    private void run() throws IOException {
        runInteractor();
        server = new Server(this);
        server.runApp();
    }

    void printOutput(String output) {
        try {
            mutex.lockInterruptibly();
            try {
                System.out.println(output);
                scheduledExecutor.shutdownNow();
            } finally {
                mutex.unlock();
            }
        } catch (InterruptedException e) {
            System.out.println(e.getLocalizedMessage());
        }
    }

    private void runInteractor() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("cancellation (1 - btn press, 2 - prompt): ");
        cancellationMode = (Objects.equals(scanner.nextInt(), 1) ? CancellationMode.KEY_PRESS : CancellationMode.PROMPT);
    }

    private void runCancellationModeHandler() {
        if (Objects.equals(cancellationMode, CancellationMode.KEY_PRESS)) {
            runKeyPressListener();
        } else if (Objects.equals(cancellationMode, CancellationMode.PROMPT)) {
            runPromptScheduler();
        }
    }

    void runChildProcesses() {
        runChildProcess("hidden/unixsockets/FirstProcess");
        runChildProcess("hidden/unixsockets/SecondProcess");
        runCancellationModeHandler();
    }

    private void runKeyPressListener() {
        daemonService.submit(new Runnable() {
            @Override
            public void run() {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

                String message = null;
                while (true) {
                    try {
                        message = bufferedReader.readLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (message != null && message.equals("c")) {
                        stopServer();
                        break;
                    }
                }
            }
        });
    }

    private void stopServer() {
        if (mutex.tryLock()) {
            try {
                server.stopRunning();
            } finally {
                mutex.unlock();
            }
        }
    }

    private void runPromptScheduler() {
        scheduledExecutor.scheduleWithFixedDelay(this::displayPrompt, INITIAL_DELAY, SCHEDULING_PERIOD, TimeUnit.SECONDS);
    }

    private void runChildProcess(String processSource) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("java", "-classpath", CLASS_PATH, processSource, "java.io.tmpdir", "junixsocket-test.sock");
        try {
            processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void displayPrompt() {
        if (mutex.tryLock()) {
            try {
                Scanner scanner = new Scanner(System.in);
                System.out.print("continue(1), continue without prompt(2), cancel(3): ");
                int readValue = scanner.nextInt();

                switch (readValue) {
                    case 1:
                        break;
                    case 2:
                        scheduledExecutor.shutdownNow();
                        break;
                    case 3:
                        server.stopRunning();
                        break;
                    default:
                        System.out.println("input mismatch, be careful next time");
                }
            } finally {
                mutex.unlock();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new ApplicationHandler().run();
    }
}
