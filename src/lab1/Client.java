package lab1;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;

import static lab1.ConnectionConstants.IP;
import static lab1.ConnectionConstants.PORT;

public class Client {

    private ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);

    private void run() {

        try(AsynchronousSocketChannel asyncSocketChannel = AsynchronousSocketChannel.open()) {

            if (asyncSocketChannel.isOpen()) {
                Void connect = asyncSocketChannel.connect(new InetSocketAddress(IP, PORT)).get();

                if (connect == null) {
                    IntBuffer intBuffer = byteBuffer.asIntBuffer();
                    intBuffer.put(10);

                    asyncSocketChannel.write(byteBuffer).get();
                } else {
                    System.out.println("Connection can't be established");
                }
            } else {
                System.out.println("Async socket can't be opened");
            }

        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Client().run();
    }
}
