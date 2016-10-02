package lab1;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class ApplicationManager implements ServerListener {

    private static final int SCHEDULER_PERIOD = 3;
    private static final int INITIAL_DELAY = 3;

    private final ChildProcessesRunner processesRunner = new ChildProcessesRunner();

    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);

    private enum PromptOptions {CONTINUE, CONTINUE_WITHOUT_PROMPT, CANCEL}

    private enum CancellationMode {KEY_PRESS, PROMPT}

    private CancellationMode cancellationMode;

    private int clientsNumber;

    private int argumentVal;

    private Server server;

    public static void main(String[] args) throws IOException, InterruptedException {
        ApplicationManager appManager = new ApplicationManager();

        appManager.runInteractor();
        appManager.runServer();
        appManager.runCancellationModeHandler();
    }

    @Override
    public void onServerStarted() {
        runClients();
    }

    @Override
    public void onCompletedComputation(int result, boolean isShortCircuited) {
        if (isShortCircuited) {
            System.out.print("result (short-circuit): " + result);
        } else {
            System.out.print("result: " + result);
        }

        shutdownPromptExecutor();
    }

    @Override
    public void onFailureReported(String cause) {
        System.out.print("failure caused by: " + cause);
        shutdownPromptExecutor();
    }

    private void shutdownPromptExecutor() {
        if (!scheduledExecutor.isShutdown()){
            scheduledExecutor.shutdownNow();
        }
    }

    private void runServer() {
        server = new Server(this, clientsNumber);
        server.runServer();
    }

    // gets called when the server was successfully started
    private void runClients() {
        processesRunner.runProcesses(clientsNumber, 10, argumentVal);
    }

    private void runInteractor() {
        Scanner scanner = new Scanner(System.in);

        System.out.printf("cancellation mode (1 - btn press, 2 - prompt): ");
        cancellationMode = (scanner.nextInt() == 1 ? CancellationMode.KEY_PRESS : CancellationMode.PROMPT);
        scanner.nextLine();
        System.out.print("number of functions: ");
        clientsNumber = scanner.nextInt();
        scanner.nextLine();
        System.out.print("argument: ");
        argumentVal = scanner.nextInt();
    }

    private void runCancellationModeHandler() {
        if (cancellationMode == CancellationMode.KEY_PRESS) {
            runKeyPressDaemon();
        } else if (cancellationMode == CancellationMode.PROMPT) {
            runPromptScheduler();
        }
    }

    private void runKeyPressDaemon() {
        Thread thread = new Thread(() -> {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

            String message = null;
            while (true) {
                try {
                    message = bufferedReader.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (message != null && message.equals("q")) {
                    stopServer();
                    break;
                }
            }
        });

        thread.setDaemon(true);
        thread.start();
    }

    private void stopServer() {
        if (Server.mutex.tryAcquire()) {
            try {
                server.stopServer();
            } finally {
                Server.mutex.release();
            }
        }
    }

    private void runPromptScheduler() {
        scheduledExecutor.scheduleWithFixedDelay(this::displayPrompt, INITIAL_DELAY, SCHEDULER_PERIOD, TimeUnit.SECONDS);
    }

    private void displayPrompt() {

        if (Server.mutex.tryAcquire()) {
            try {
                Scanner scanner = new Scanner(System.in);

                System.out.print("continue(1), continue without prompt(2), cancel(3): ");
                int readValue = scanner.nextInt();

                switch (readValue) {
                    case 1:
                        handlePrompt(PromptOptions.CONTINUE);
                        break;
                    case 2:
                        handlePrompt(PromptOptions.CONTINUE_WITHOUT_PROMPT);
                        break;
                    case 3:
                        handlePrompt(PromptOptions.CANCEL);
                        break;
                }
            } finally {
                Server.mutex.release();
            }
        }
    }

    private void handlePrompt(PromptOptions promptOptions) {
        switch (promptOptions) {
            case CONTINUE:
                break;
            case CONTINUE_WITHOUT_PROMPT:
                scheduledExecutor.shutdownNow();
                break;
            case CANCEL:
                server.stopServer();
                break;
        }
    }
}
