package lab1;


import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import static lab1.Constants.*;

/**
 * this class represents a server entity
 * and makes invocation of clients processes
 */

public class ApplicationManager {

    private static int CLIENTS_NUMBER;

    private static int ARGUMENT_VAL;

    public static void main(String[] args) throws IOException, InterruptedException {
        ApplicationManager appManager = new ApplicationManager();

        appManager.interact();
        appManager.runServer();
    }

    private void runServer() {
        Server server = new Server(this::runClients);
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
        System.out.println(processBuilder.command());

        return processBuilder;
    }

    private void interact() {
        Scanner scanner = new Scanner(System.in);

        System.out.print("number of functions: ");
        CLIENTS_NUMBER = scanner.nextInt();
        scanner.nextLine();
        System.out.print("argument: ");
        ARGUMENT_VAL = scanner.nextInt();
    }
}
