package lab1;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class ApplicationManager implements ServerListener {

    private static final int SCHEDULER_PERIOD = 3;
    private static final int INITIAL_DELAY = 3;

    private final ChildProcessesRunner processesRunner = new ChildProcessesRunner();

    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);

    // used to lock computation output till the prompt is closed
    private final Lock promptLock = new ReentrantLock(true);

    private enum PromptOptions { CONTINUE, CONTINUE_WITHOUT_PROMPT, CANCEL }

    private enum CancellationMode { KEY_PRESS, PROMPT}

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

        promptLock.lock();
        try {
            if (isShortCircuited) {
                System.out.println("result (short-circuit): " + result);
            } else {
                System.out.println("result: " + result);
            }
        } finally {
            scheduledExecutor.shutdownNow();
            promptLock.unlock();
        }
    }

    @Override
    public void onFailureReported(String cause) {

        promptLock.lock();
        try {
            System.out.println("failure caused by: " + cause);
        } finally {
            scheduledExecutor.shutdownNow();
            promptLock.unlock();
        }
    }

    private void runServer() {
        server = new Server(this, clientsNumber);
        server.runServer();
    }

    // gets called when the server was successfully started
    private void runClients() {
       processesRunner.runProcesses(clientsNumber, 30, argumentVal);
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
                    // it's definitely bad if scheduler decides to reschedule here
                    // because output lock happens only inside stopServer method
                    // (trying to lock right after readLine procedure creates possibility of losing the result)
                    server.stopServer();
                    break;
                }
            }
        });

        thread.setDaemon(true);
        thread.start();
    }

    private void runPromptScheduler() {
        scheduledExecutor.scheduleWithFixedDelay(this::displayPrompt, INITIAL_DELAY, SCHEDULER_PERIOD, TimeUnit.SECONDS);
    }

    private void displayPrompt() {

        promptLock.lock();
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
            promptLock.unlock();
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
