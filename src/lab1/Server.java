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

    private static final int NO_RUNNING_THREADS = 0;

    private static final int SHORT_CIRCUIT_CONDITION = 0;

    private static final Integer POISON_KILL_VALUE = null;

    static final Semaphore binarySemaphore = new Semaphore(1);

    private final List<Integer> valuesContainer;

    private final ExecutorService taskExecutor;

    private final CompletionService<Integer> completionService;

    private final ExecutorService consumerService;

    private final ExecutorService serverService;

    private ServerListener serverListener;

    private int clientsNumber;

    Server(ServerListener serverListener, int clientsNumber) {
        valuesContainer = new ArrayList<>();
        taskExecutor = Executors.newCachedThreadPool();
        completionService = new ExecutorCompletionService<>(taskExecutor);
        consumerService = Executors.newFixedThreadPool(1);
        serverService = Executors.newFixedThreadPool(1);

        this.clientsNumber = clientsNumber;
        this.serverListener = serverListener;
    }

    void runServer() {
        if (Objects.equals(((ThreadPoolExecutor) serverService).getActiveCount(), NO_RUNNING_THREADS)) {
            serverService.submit(this::handleServerSocket);
            runFuturesConsumer();
        }
    }

    private void handleServerSocket() {
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
    }

    private Callable<Integer> createSocketHandlerTask(AsynchronousSocketChannel asyncSocket) {
        return () -> handleSocketChannel(asyncSocket);
    }

    private Integer handleSocketChannel(AsynchronousSocketChannel asyncSocket) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        try {
            prepareBuffer(asyncSocket, byteBuffer);
        } catch (InterruptedException | ExecutionException e) {
            return POISON_KILL_VALUE;
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
        if (Objects.equals(((ThreadPoolExecutor) consumerService).getActiveCount(), NO_RUNNING_THREADS)) {
            consumerService.submit(() -> {
                try {
                    while (true) {
                        Future<Integer> consumedFuture = completionService.take();
                        Integer consumedValue = consumedFuture.get();

                        if (Objects.equals(consumedValue, POISON_KILL_VALUE)) {
                            break;
                        } else {
                            valuesContainer.add(consumedValue);
                        }

                        if (Objects.equals(consumedValue, SHORT_CIRCUIT_CONDITION)) {
                            transferResult(true);
                            break;
                        }
                        if ((Objects.equals(valuesContainer.size(), clientsNumber))) {
                            transferResult(false);
                            break;
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.getLocalizedMessage();
                }
            });
        }
    }

    private void transferResult(boolean isShortCircuited) {
        try {
            binarySemaphore.acquire();
            try {
                if (isShortCircuited) {
                    serverListener.onCompletedComputation(SHORT_CIRCUIT_CONDITION, true);
                } else {
                    serverListener.onCompletedComputation(valuesContainer.stream()
                            .reduce(1, (accumulator, elem) -> accumulator * elem), false);
                }
            } finally {
                shutdownExecutors();
                binarySemaphore.release();
            }
        } catch (InterruptedException e) {
            e.getLocalizedMessage();
        }
    }

    void stopServer() {
        if (!binarySemaphore.hasQueuedThreads()) {
            shutdownExecutors();
            serverListener.onCancellationReported();
        }
    }

    @ThreadSafe
    private void shutdownExecutors() {
        if (!serverService.isShutdown()) serverService.shutdownNow();
        if (!consumerService.isShutdown()) consumerService.shutdownNow();
    }
}
