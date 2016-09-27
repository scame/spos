package lab1;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.*;

import static lab1.Constants.*;

class Server {

    private ExecutorService taskExecutor = Executors.newCachedThreadPool();

    private ConcurrentLinkedQueue<Future<Integer>> futuresHolder = new ConcurrentLinkedQueue<>();

    private ServerListener serverListener;

    private Future<Void> serverFuture;

    @FunctionalInterface
    interface ServerListener {

        void onServerStarted();
    }

    Server(ServerListener serverListener) {
        this.serverListener = serverListener;
    }

    void run() {
       serverFuture = taskExecutor.submit(this::handleServerSocket);
    }

    private Void handleServerSocket() {
        try(AsynchronousServerSocketChannel asyncServerSocket = AsynchronousServerSocketChannel.open()) {
            if (asyncServerSocket.isOpen()) {

                asyncServerSocket.bind(new InetSocketAddress(IP, PORT));
                serverListener.onServerStarted();

                while (true) {
                    Future<AsynchronousSocketChannel> asyncSocket = asyncServerSocket.accept();

                    try {
                        futuresHolder.add(taskExecutor.submit(createSocketHandlerTask(asyncSocket.get())));
                    } catch (InterruptedException | ExecutionException e) {
                        System.out.println("Interrupted/exec exception");
                        taskExecutor.shutdown();
                        while (!taskExecutor.isTerminated()) { }

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

    private Integer handleSocketChannel(AsynchronousSocketChannel asyncSocket)
            throws InterruptedException, ExecutionException, IOException {

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);

        while (asyncSocket.read(byteBuffer).get() != -1) {
            byteBuffer.flip();
            System.out.println("successful read: " + byteBuffer.getInt());
            byteBuffer.rewind();
        }

        asyncSocket.close();

        return byteBuffer.getInt();
    }

    void stop() {
        System.out.println("gonna stop");
    }
}
