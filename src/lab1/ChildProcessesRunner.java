package lab1;


import java.io.File;
import java.io.IOException;

import static lab1.Constants.CLIENT_PATH;
import static lab1.Constants.JAVA;
import static lab1.Constants.PROJECT_DIR;

class ChildProcessesRunner {

    void runProcesses(int clientsNumber, int maxDuration, int argument) {
        ProcessBuilder processBuilder = constructProcessBuilder(maxDuration, argument);

        for (int i = 0; i < clientsNumber; i++) {
            try {
                processBuilder.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private ProcessBuilder constructProcessBuilder(int maxDuration, int argument) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(new File(PROJECT_DIR));
        processBuilder.command(JAVA, CLIENT_PATH, String.valueOf(argument), String.valueOf(maxDuration));

        return processBuilder;
    }
}
