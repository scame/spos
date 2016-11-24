package hidden.unixsockets;


import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final File socketFile = new File(new File(System.getProperty("java.io.tmpdir")), "junixsocket-test.sock");

    private static final int BUFF_SIZE = 128;

    private static final double ARG = 3.14;

    private static final int CLIENTS_NUMBER = 2;

    private final ExecutorService serverService = Executors.newSingleThreadExecutor();

    private final ExecutorService resultsProcessingService = Executors.newCachedThreadPool();

    private final ExecutorService clientsExecutor = Executors.newFixedThreadPool(CLIENTS_NUMBER);

    private final ApplicationHandler applicationHandler;

    private final CountDownLatch latch = new CountDownLatch(CLIENTS_NUMBER);

    private final List<Double> resultsList = Collections.synchronizedList(new ArrayList<>());

    private AFUNIXServerSocket server;

    public Server(ApplicationHandler applicationHandler) {
        this.applicationHandler = applicationHandler;
    }

    void runApp() throws IOException {
        resultsProcessingService.submit(this::startResultsProcessing);
        serverService.submit(this::runServer);
    }

    private void runServer() {
        try (AFUNIXServerSocket server = AFUNIXServerSocket.newInstance()) {
            server.bind(new AFUNIXSocketAddress(socketFile));
            this.server = server;
            System.out.println("server: " + server);

            new Thread(applicationHandler::runChildProcesses).start();
            runConnectionListener(server);
        } catch (IOException e) {
            System.out.println(e.getLocalizedMessage());
        }
    }

    private void runConnectionListener(AFUNIXServerSocket server) {
        while (!Thread.interrupted()) {
            System.out.println("Waiting for connection...");
            try {
                Socket sock = server.accept();
                System.out.println("Connected: " + sock);
                clientsExecutor.submit(() -> invokeClientHandler(sock));

            } catch (IOException e) {
                System.out.println("Server is shut down");
                clientsExecutor.shutdownNow();
                closeServerSocket(server);
                System.out.println(e.getLocalizedMessage());
            }
        }
        System.out.println("interrupted");
    }

    private void closeServerSocket(AFUNIXServerSocket serverSocket) {
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.out.println(e.getLocalizedMessage());
        }
    }

    private void startResultsProcessing() {
        try {
            latch.await();
            double res = resultsList.get(0) + resultsList.get(1);
            applicationHandler.printOutput("Result of computation: " + res);

            closeServerSocket(server);
            resultsProcessingService.shutdownNow();
            System.out.println("kinda interrupted");
        } catch (InterruptedException e) {
            System.out.println("Result processing was interrupted");
            System.out.println(e.getLocalizedMessage());
        }
    }

    private void invokeClientHandler(Socket clientSocket) {
        try (InputStream is = clientSocket.getInputStream();
             OutputStream os = clientSocket.getOutputStream()) {

            sendArgument(os);
            processResponse(is);
            latch.countDown();
        } catch (IOException e) {
            System.out.println(e.getLocalizedMessage());
        } finally {
            closeClientSocket(clientSocket);
        }
    }

    private void sendArgument(OutputStream os) throws IOException {
        System.out.println("Sending " + ARG);
        byte[] argWrapper = new byte[BUFF_SIZE];
        ByteBuffer.wrap(argWrapper).putDouble(ARG);
        os.write(argWrapper);
        os.flush();
    }

    private void processResponse(InputStream is) throws IOException {
        byte[] responseWrapper = new byte[BUFF_SIZE];
        int read = is.read(responseWrapper);

        resultsList.add(ByteBuffer.wrap(responseWrapper).getDouble());
        System.out.println(resultsList.get(resultsList.size() - 1));
    }

    private void closeClientSocket(Socket clientSocket) {
        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void stopRunning() {
        if (latch.getCount() != 0) {
            applicationHandler.printOutput("Cancelled");
            closeServerSocket(server);
            resultsProcessingService.shutdownNow();
        }
    }
}
