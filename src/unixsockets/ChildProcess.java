package unixsockets;


import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.newsclub.net.unix.AFUNIXSocketException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class ChildProcess {

    protected static final int PROPERTY_INDEX = 0;
    protected static final int SOCKET_INDEX = 1;

    protected final File socketFile;

    public ChildProcess(File socketFile) {
        this.socketFile = socketFile;
    }

    protected void runProcess(ComputationalFunc func) throws IOException {
        try (AFUNIXSocket sock = AFUNIXSocket.newInstance()) {
            try {
                sock.connect(new AFUNIXSocketAddress(socketFile));
            } catch (AFUNIXSocketException e) {
                System.out.println("Cannot connect to server. Have you started it?");
                System.out.flush();
                throw e;
            }
            System.out.println("Connected");

            startExchanging(sock, func);
        }

        System.out.println("End of communication.");
    }

    protected void startExchanging(AFUNIXSocket socket, ComputationalFunc func) throws IOException {
        try (InputStream is = socket.getInputStream();
             OutputStream os = socket.getOutputStream()) {

            byte[] buf = new byte[128];

            int read = is.read(buf);
            System.out.println("Server says: " + new String(buf, 0, read));
            double computationResult = func.compute(ByteBuffer.wrap(buf).getDouble());

            byte[] responseBuffer = new byte[128];
            ByteBuffer.wrap(responseBuffer).putDouble(computationResult);
            os.write(responseBuffer);
            os.flush();
        }
    }
}
