package lab1;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.*;

import static lab1.ConnectionConstants.*;

public class Server {

    private ExecutorService taskExecutor = Executors.newCachedThreadPool();

    private ConcurrentLinkedQueue<Future<Integer>> futuresHolder = new ConcurrentLinkedQueue<>();

    private void run() {

        try(AsynchronousServerSocketChannel asyncServerSocket = AsynchronousServerSocketChannel.open()) {
            if (asyncServerSocket.isOpen()) {

                asyncServerSocket.bind(new InetSocketAddress(IP, PORT));

                while (true) {
                    System.out.println("waiting for connections...");
                    Future<AsynchronousSocketChannel> asyncSocket = asyncServerSocket.accept();

                    try {
                        futuresHolder.add(taskExecutor.submit(createWorkerTask(asyncSocket.get())));
                    } catch (InterruptedException | ExecutionException e) {

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
    }

    private Callable<Integer> createWorkerTask(AsynchronousSocketChannel asyncSocket) {
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

    public static void main(String[] args) {
        new Server().run();
    }
}
