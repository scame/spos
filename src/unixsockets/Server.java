package unixsockets;


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

    private final ExecutorService connectionsListenerService = Executors.newSingleThreadExecutor();

    private final ExecutorService resultsProcessingService = Executors.newCachedThreadPool();

    private final ExecutorService clientsExecutor = Executors.newFixedThreadPool(CLIENTS_NUMBER);

    private final ApplicationHandler applicationHandler;

    private final CountDownLatch latch = new CountDownLatch(CLIENTS_NUMBER);

    private final List<Double> resultsList = Collections.synchronizedList(new ArrayList<>());

    public Server(ApplicationHandler applicationHandler) {
        this.applicationHandler = applicationHandler;
    }

    void runServer() throws IOException {
        try (AFUNIXServerSocket server = AFUNIXServerSocket.newInstance()) {
            server.bind(new AFUNIXSocketAddress(socketFile));
            System.out.println("server: " + server);
            applicationHandler.runChildProcesses();

            connectionsListenerService.submit(() -> runConnectionListener(server));
            resultsProcessingService.submit(this::startResultsProcessing);
        }
    }

    private void runConnectionListener(AFUNIXServerSocket server) {
        while (true) {
            System.out.println("Waiting for connection...");
            try {
                try (Socket sock = server.accept()) {
                    System.out.println("Connected: " + sock);

                    clientsExecutor.submit(() -> invokeClientHandler(sock));
                }
            } catch (IOException e) {
                clientsExecutor.shutdownNow();
                closeServerSocket(server);
                System.out.println(e.getLocalizedMessage());
            }
        }
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
            applicationHandler.printOutput("Result of computation: " + resultsList.get(0) + resultsList.get(1));
        } catch (InterruptedException e) {
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
            closeClientSocket(clientSocket);
            System.out.println(e.getLocalizedMessage());
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
        System.out.println("Client's response: " + new String(responseWrapper, 0, read));

        resultsList.add(ByteBuffer.wrap(responseWrapper).getDouble());
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
            connectionsListenerService.shutdownNow();
            resultsProcessingService.shutdownNow();
        }
    }
}
