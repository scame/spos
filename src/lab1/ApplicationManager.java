package lab1;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static lab1.Constants.*;

/**
 * server process
 * makes invocation of client processes */

public class ApplicationManager implements Server.ServerListener {

    private static final int SCHEDULER_PERIOD = 3;
    private static final int INITIAL_DELAY = 3;

    private enum DialogOptions { CONTINUE, CONTINUE_WITHOUT_PROMPT, CANCEL }

    private enum CancellationMode { KEY_PRESS, POP_UP_DIALOG }

    private CancellationMode cancellationMode;

    private ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);

    private int clientNumber;

    private int argumentVal;

    private Server server;

    public static void main(String[] args) throws IOException, InterruptedException {
        ApplicationManager appManager = new ApplicationManager();

        appManager.runUserInteractor();
        appManager.runServer();
        appManager.runCancellationModeHandler();
    }

    @Override
    public void onServerStarted() {
        runClients();
    }

    @Override
    synchronized public void onCompletedComputation(int result, boolean shortCircuited) {
        if (shortCircuited) {
            System.out.println("result (short-circuit): " + result);
        } else {
            System.out.println("result: " + result);
        }

        scheduledExecutor.shutdownNow();
    }

    @Override
    synchronized public void onFailReported(String cause) {
        System.out.println("fail caused by: " + cause);
        scheduledExecutor.shutdownNow();
    }

    private void runServer() {
        server = new Server(this, clientNumber);
        server.run();
    }

    // gets called when the server was successfully started
    private void runClients() {
        ProcessBuilder processBuilder = constructProcessBuilder();

        for (int i = 0; i < clientNumber; i++) {
            try {
                processBuilder.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private ProcessBuilder constructProcessBuilder() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(new File(PROJECT_DIR));
        processBuilder.command(JAVA, CLIENT_PATH, String.valueOf(argumentVal));

        return processBuilder;
    }

    private void runUserInteractor() {
        Scanner scanner = new Scanner(System.in);

        System.out.printf("cancellation mode (1 - btn press, 2 - dialog window): ");
        cancellationMode = (scanner.nextInt() == 1 ? CancellationMode.KEY_PRESS : CancellationMode.POP_UP_DIALOG);
        scanner.nextLine();
        System.out.print("number of functions: ");
        clientNumber = scanner.nextInt();
        scanner.nextLine();
        System.out.print("argument: ");
        argumentVal = scanner.nextInt();
    }

    private void runCancellationModeHandler() {
        if (cancellationMode.equals(CancellationMode.KEY_PRESS)) {
            runKeyPressDaemon();
        } else if (cancellationMode.equals(CancellationMode.POP_UP_DIALOG)) {
            runPopupsScheduler();
        }
    }

    // runs in the background waiting for someone to enter "q"
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
                    server.stop();
                    break;
                }
            }
        });

        thread.setDaemon(true);
        thread.start();
    }

    private void runPopupsScheduler() {
        scheduledExecutor.scheduleWithFixedDelay(this::displayPopup, INITIAL_DELAY, SCHEDULER_PERIOD, TimeUnit.SECONDS);
    }

    synchronized private void displayPopup() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("continue(1), continue without prompt(2), cancel(3)");
        int readValue = scanner.nextInt();

        switch (readValue) {
            case 1:
                handlePopupMessage(DialogOptions.CONTINUE);
                break;
            case 2:
                handlePopupMessage(DialogOptions.CONTINUE_WITHOUT_PROMPT);
                break;
            case 3:
                handlePopupMessage(DialogOptions.CANCEL);
                break;
        }
    }

    synchronized private void handlePopupMessage(DialogOptions dialogOptions) {
        switch (dialogOptions) {
            case CONTINUE:
                // do nothing
                break;
            case CONTINUE_WITHOUT_PROMPT:
                scheduledExecutor.shutdownNow();
                break;
            case CANCEL:
                scheduledExecutor.shutdownNow();
                server.stop();
                break;
        }
    }
}
