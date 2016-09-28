package lab1;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static lab1.Constants.*;

class Server {

    private static final int SHORT_CIRCUIT_CONDITION = 0;

    private CopyOnWriteArrayList<Integer> valuesContainer = new CopyOnWriteArrayList<>();

    private ExecutorService taskExecutor = Executors.newCachedThreadPool();

    private CompletionService<Integer> completionService = new ExecutorCompletionService<>(taskExecutor);

    private Lock lock = new ReentrantLock();

    private ServerListener serverListener;

    private Future<Void> serverFuture;

    private int clientsNumber;

    interface ServerListener {

        void onServerStarted();

        void onFailReported(String cause);

        void onCompletedComputation(int result, boolean shortCircuited);
    }

    Server(ServerListener serverListener, int clientsNumber) {
        this.clientsNumber = clientsNumber;
        this.serverListener = serverListener;
    }

    void run() {
        runFuturesConsumer();
        serverFuture = taskExecutor.submit(this::handleServerSocket);
    }

    private Void handleServerSocket() {
        try (AsynchronousServerSocketChannel asyncServerSocket = AsynchronousServerSocketChannel.open()) {
            if (asyncServerSocket.isOpen()) {

                asyncServerSocket.bind(new InetSocketAddress(IP, PORT));
                serverListener.onServerStarted();

                while (true) {
                    Future<AsynchronousSocketChannel> asyncSocket = asyncServerSocket.accept();

                    try {
                        completionService.submit(createSocketHandlerTask(asyncSocket.get()));
                    } catch (InterruptedException | ExecutionException e) {
                        taskExecutor.shutdownNow();
                        break;
                    }
                }

            } else {
                System.out.println("server socket channel can't be opened");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private Callable<Integer> createSocketHandlerTask(AsynchronousSocketChannel asyncSocket) {
        return () -> handleSocketChannel(asyncSocket);
    }

    private Integer handleSocketChannel(AsynchronousSocketChannel asyncSocket) {

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

        try {
            asyncSocket.read(byteBuffer).get();
            byteBuffer.flip();
        } catch (InterruptedException | ExecutionException e) {
            return null;
        } finally {
            try {
                asyncSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return byteBuffer.getInt();
    }

    private void runFuturesConsumer() {
        new Thread(() -> {
            try {
                while (true) {
                    Future<Integer> consumedFuture = completionService.take();
                    Integer consumedValue = consumedFuture.get();

                    if (consumedValue == null) {
                        break; // clients were interrupted, stop consuming
                    }

                    valuesContainer.add(consumedValue);

                    if (consumedValue == SHORT_CIRCUIT_CONDITION) {
                        cancelServerFuture(); // says clients to stop computations
                        transferResult(true, null);
                        break;
                    }

                    if (valuesContainer.size() == clientsNumber) {
                        transferResult(false, null);
                        break;
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }).start();
    }

    void stop() {
        cancelServerFuture();

        if (clientsNumber == valuesContainer.size()) {
            transferResult(false, null);
        } else {
            transferResult(false, "stopped before the completion");
        }
    }

    private void transferResult(boolean shortCircuited, String failReport) {

        // simple lock/sync will produce a deadlock (if computations were completed during the prompt)
        if (lock.tryLock()) {
            try {
                if (failReport != null) {
                    serverListener.onFailReported(failReport);
                    return;
                }

                if (shortCircuited) {
                    serverListener.onCompletedComputation(SHORT_CIRCUIT_CONDITION, true);
                } else {
                    serverListener.onCompletedComputation(valuesContainer.stream()
                            .reduce(1, (accumulator, elem) -> accumulator * elem), false);
                }
            } finally {
                cancelServerFuture();
                lock.unlock();
            }
        }
    }

    private void cancelServerFuture() {
        if (!serverFuture.isCancelled()) {
            serverFuture.cancel(true);
        }
    }
}
