package lab1;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static lab1.Constants.IP;
import static lab1.Constants.PORT;

public class Client {

    private ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);

    private static int ARGUMENT_VAL;

    private void run() {

        try(AsynchronousSocketChannel asyncSocketChannel = AsynchronousSocketChannel.open()) {

            if (asyncSocketChannel.isOpen()) {
                Void connect = asyncSocketChannel.connect(new InetSocketAddress(IP, PORT)).get();

                if (connect == null) {

                    TimeUnit.SECONDS.sleep(new Random().nextInt(10));
                    IntBuffer intBuffer = byteBuffer.asIntBuffer();
                    intBuffer.put(new Random().nextInt(ARGUMENT_VAL + 15));

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
        if (args.length > 0) {
            ARGUMENT_VAL = Integer.valueOf(args[0]);
        }

        new Client().run();
    }
}
