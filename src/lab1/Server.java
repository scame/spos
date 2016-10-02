package lab1;


import jcip.ThreadSafe;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

import static lab1.Constants.*;

class Server {

    private static final int SHORT_CIRCUIT_CONDITION = 0;

    private static final Integer INTERRUPTION_VALUE = null;

    static final Semaphore mutex = new Semaphore(1);

    private final List<Integer> valuesContainer = new ArrayList<>();

    private final ExecutorService taskExecutor = Executors.newCachedThreadPool();

    private final CompletionService<Integer> completionService = new ExecutorCompletionService<>(taskExecutor);

    private ServerListener serverListener;

    private Future<Void> serverFuture;

    private Thread futuresConsumer;

    private int clientsNumber;


    Server(ServerListener serverListener, int clientsNumber) {
        this.clientsNumber = clientsNumber;
        this.serverListener = serverListener;
    }

    void runServer() {
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
            prepareBuffer(asyncSocket, byteBuffer);
        } catch (InterruptedException | ExecutionException e) {
            return INTERRUPTION_VALUE;
        } finally {
            closeClientSocket(asyncSocket);
        }

        return byteBuffer.getInt();
    }

    private void closeClientSocket(AsynchronousSocketChannel asyncSocket) {
        try {
            asyncSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void prepareBuffer(AsynchronousSocketChannel socket, ByteBuffer byteBuffer)
            throws InterruptedException, ExecutionException {
        socket.read(byteBuffer).get();
        byteBuffer.flip();
    }

    private void runFuturesConsumer() {
        futuresConsumer = new Thread(() -> {
            try {
                while (true) {
                    Future<Integer> consumedFuture = completionService.take();
                    Integer consumedValue = consumedFuture.get();

                    if (Objects.equals(consumedValue, INTERRUPTION_VALUE)) {
                        break;  // clients were interrupted, stop futures consuming
                    }           // happens after cancelServerFuture procedure call

                    valuesContainer.add(consumedValue);

                    if (consumedValue == SHORT_CIRCUIT_CONDITION) {
                        transferResult(true);
                        break;
                    }

                    if (valuesContainer.size() == clientsNumber) {
                        transferResult(false);
                        break;
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                e.getLocalizedMessage();
            }
        });

        futuresConsumer.start();
    }

    private void transferResult(boolean isShortCircuited) {
        try {
            mutex.acquire();
            try {
                if (isShortCircuited) {
                    serverListener.onCompletedComputation(SHORT_CIRCUIT_CONDITION, true);
                } else {
                    serverListener.onCompletedComputation(valuesContainer.stream()
                            .reduce(1, (accumulator, elem) -> accumulator * elem), false);
                }
            } finally {
                cancelServerFuture();
                mutex.release();
            }
        } catch (InterruptedException e) {
            e.getLocalizedMessage();
        }
    }

    void stopServer() {
        if (!mutex.hasQueuedThreads() && !serverFuture.isCancelled()) {
            serverListener.onFailureReported("stopped before the completion");

            cancelServerFuture(); // cancels socketHandlerTasks if there are any running
            futuresConsumer.interrupt(); // required when a mutex was acquired in transferResult,
                                         // but stopServer check is already in the past
        }
    }


    @ThreadSafe // executor's implementation of Future interface guarantees thread-safety
    private void cancelServerFuture() {
        if (!serverFuture.isCancelled()) {
            serverFuture.cancel(true);
        }
    }
}
