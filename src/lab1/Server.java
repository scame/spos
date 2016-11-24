package lab1;


import jcip.ThreadSafe;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Objects;
import java.util.concurrent.*;

import static lab1.Constants.*;

class Server {

    private static final int NO_RUNNING_THREADS = 0;

    private static final int SHORT_CIRCUIT_CONDITION = 0;

    static final Semaphore binarySemaphore = new Semaphore(1);

    private final ExecutorService taskExecutor;

    private final ExecutorService consumerService;

    private final ExecutorService serverService;

    private final BlockingQueue<Integer> resultsQueue;

    private ServerListener serverListener;

    private int clientsNumber;

    Server(ServerListener serverListener, int clientsNumber) {
        taskExecutor = Executors.newCachedThreadPool();
        consumerService = Executors.newFixedThreadPool(1);
        serverService = Executors.newFixedThreadPool(1);
        resultsQueue = new ArrayBlockingQueue<>(clientsNumber);

        this.clientsNumber = clientsNumber;
        this.serverListener = serverListener;
    }

    void runServer() {
        if (Objects.equals(((ThreadPoolExecutor) serverService).getActiveCount(), NO_RUNNING_THREADS)) {
            serverService.submit(this::handleServerSocket);
            runQueueConsumer();
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
                        taskExecutor.submit(handleSocketChannel(asyncSocket.get()));
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

    private Runnable handleSocketChannel(AsynchronousSocketChannel asyncSocket) {
        return () -> prepareBuffer(asyncSocket);
    }

    private void closeClientSocket(AsynchronousSocketChannel asyncSocket) {
        try {
            asyncSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void prepareBuffer(AsynchronousSocketChannel socket) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        socket.read(byteBuffer, null, new CompletionHandler<Integer, Object>() {
            @Override
            public void completed(Integer integer, Object o) {
                try {
                    byteBuffer.flip();
                    resultsQueue.put(byteBuffer.getInt());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    closeClientSocket(socket);
                }
            }

            @Override
            public void failed(Throwable throwable, Object o) {
                System.out.println(throwable.getLocalizedMessage());
                closeClientSocket(socket);
            }
        });
    }

    // I remember that the original idea was to use canonical pooling (to follow a principle of asynchronous I/O)
    // to avoid blocking in resultsQueue.take() we can simply replace blockingQueue by ConcurrentLinkedQueue
    // and then use pool method in a while loop (I leave it as it is because there is no much sense to change the implementation)
    private void runQueueConsumer() {
        if (Objects.equals(((ThreadPoolExecutor) consumerService).getActiveCount(), NO_RUNNING_THREADS)) {
            consumerService.submit(() -> {
                try {
                    for (int i = 0, accumulator = 1; i < clientsNumber; i++) {
                        int producedValue = resultsQueue.take();

                        if (Objects.equals(producedValue, SHORT_CIRCUIT_CONDITION)) {
                            transferResult(true, 0);
                            break;
                        } else {
                            accumulator *= producedValue;
                        }

                        if ((Objects.equals(i, clientsNumber - 1))) {
                            transferResult(false, accumulator);
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    e.getLocalizedMessage();
                }
            });
        }
    }

    private void transferResult(boolean isShortCircuited, int accumulator) {
        try {
            binarySemaphore.acquire();
            try {
                if (isShortCircuited) {
                    serverListener.onCompletedComputation(SHORT_CIRCUIT_CONDITION, true);
                } else {
                    serverListener.onCompletedComputation(accumulator, false);
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
