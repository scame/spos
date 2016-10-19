package lab1;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.*;

import static lab1.Server.binarySemaphore;

public class ApplicationManager implements ServerListener {

    private static final int NO_RUNNING_THREADS = 0;

    private static final int MAX_DURATION = 25; // just for testing

    private static final int SCHEDULING_PERIOD = 3;
    private static final int INITIAL_DELAY = 3;

    private final ChildProcessesRunner processesRunner;

    private final ScheduledExecutorService scheduledExecutor;

    private final ExecutorService daemonService;

    private enum CancellationMode {KEY_PRESS, PROMPT}

    private CancellationMode cancellationMode;

    private int clientsNumber;

    private int argumentVal;

    private Server server;

    public ApplicationManager() {
        daemonService = Executors.newFixedThreadPool(1, runnable -> {
            Thread thread = Executors.defaultThreadFactory().newThread(runnable);
            thread.setDaemon(true);
            return thread;
        });

        scheduledExecutor = Executors.newScheduledThreadPool(1);
        processesRunner = new ChildProcessesRunner();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ApplicationManager appManager = new ApplicationManager();

        appManager.runInteractor();
        appManager.runServer();
    }

    @Override
    public void onServerStarted() {
        processesRunner.runChildProcesses(clientsNumber, MAX_DURATION, argumentVal);
        runCancellationModeHandler();
    }

    @Override
    public void onCompletedComputation(int result, boolean isShortCircuited) {
        if (isShortCircuited) {
            System.out.print("result (short-circuit): " + result);
        } else {
            System.out.print("result: " + result);
        }

        scheduledExecutor.shutdownNow();
    }

    @Override
    public void onCancellationReported() {
        System.out.print("Successfully cancelled execution");
        scheduledExecutor.shutdownNow();
    }

    private void runServer() {
        server = new Server(this, clientsNumber);
        server.runServer();
    }

    // incorrect input isn't handled
    private void runInteractor() {
        Scanner scanner = new Scanner(System.in);

        System.out.print("cancellation mode (1 - btn press, 2 - prompt): ");
        cancellationMode = (Objects.equals(scanner.nextInt(), 1) ? CancellationMode.KEY_PRESS : CancellationMode.PROMPT);
        scanner.nextLine();
        System.out.print("number of functions: ");
        clientsNumber = scanner.nextInt();
        scanner.nextLine();
        System.out.print("argument: ");
        argumentVal = scanner.nextInt();
    }

    private void runCancellationModeHandler() {
        if (Objects.equals(cancellationMode, CancellationMode.KEY_PRESS)) {
            runKeyPressDaemon();
        } else if (Objects.equals(cancellationMode, CancellationMode.PROMPT)) {
            runPromptScheduler();
        }
    }

    private void runKeyPressDaemon() {
        if (Objects.equals(((ThreadPoolExecutor)daemonService).getActiveCount(), NO_RUNNING_THREADS)) {
            daemonService.submit(this::runKeyPressListener);
        }
    }

    private void runKeyPressListener() {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

        String message = null;
        while (true) {
            try {
                message = bufferedReader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (message != null && message.equals("q")) {
                // dangerous place, but a user simply will be thinking that he was too slow
                stopServer();
                break;
            }
        }
    }

    private void stopServer() {
        if (binarySemaphore.tryAcquire()) {
            try {
                server.stopServer();
            } finally {
                binarySemaphore.release();
            }
        }
    }

    private void runPromptScheduler() {
        if (Objects.equals(((ThreadPoolExecutor) scheduledExecutor).getActiveCount(), NO_RUNNING_THREADS)) {
            scheduledExecutor.scheduleWithFixedDelay(this::displayPrompt, INITIAL_DELAY,
                    SCHEDULING_PERIOD, TimeUnit.SECONDS);
        }
    }

    // incorrect input isn't handled
    private void displayPrompt() {
        if (binarySemaphore.tryAcquire()) {
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
                        server.stopServer();
                        break;
                    default:
                        System.out.println("input mismatch, be careful next time");
                }
            } finally {
                binarySemaphore.release();
            }
        }
    }
}
