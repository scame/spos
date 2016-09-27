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
 * represents a server entity
 * makes invocation of client processes */

public class ApplicationManager {

    private enum DialogOptions { CONTINUE, CONTINUE_WITHOUT_PROMPT, CANCEL }

    private enum CancellationMode { KEY_PRESS, POP_UP_DIALOG }

    private static final int SCHEDULER_PERIOD = 5;
    private static final int INITIAL_DELAY = 5;

    private ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);

    private static int CLIENTS_NUMBER;

    private static int ARGUMENT_VAL;

    private CancellationMode cancellationMode;

    private Server server;

    public static void main(String[] args) throws IOException, InterruptedException {
        ApplicationManager appManager = new ApplicationManager();

        appManager.runUserInteractor();
        appManager.runServer();
        appManager.runCancellationModeHandler();
    }

    private void runServer() {
        server = new Server(this::runClients);
        server.run();
    }

    // gets called when the server was successfully started
    private void runClients() {
        ProcessBuilder processBuilder = constructProcessBuilder();

        for (int i = 0; i < CLIENTS_NUMBER; i++) {
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
        processBuilder.command(JAVA, CLIENT_PATH, String.valueOf(ARGUMENT_VAL));

        return processBuilder;
    }

    private void runUserInteractor() {
        Scanner scanner = new Scanner(System.in);

        System.out.printf("cancellation mode (1 - btn press, 2 - dialog window): ");
        cancellationMode = (scanner.nextInt() == 1 ? CancellationMode.KEY_PRESS : CancellationMode.POP_UP_DIALOG);
        scanner.nextLine();
        System.out.print("number of functions: ");
        CLIENTS_NUMBER = scanner.nextInt();
        scanner.nextLine();
        System.out.print("argument: ");
        ARGUMENT_VAL = scanner.nextInt();
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
        scheduledExecutor.scheduleWithFixedDelay(() -> {
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
        }, INITIAL_DELAY, SCHEDULER_PERIOD, TimeUnit.SECONDS);
    }

    private void handlePopupMessage(DialogOptions dialogOptions) {
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
