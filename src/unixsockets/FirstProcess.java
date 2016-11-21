package unixsockets;


import java.io.File;
import java.io.IOException;

public class FirstProcess extends ChildProcess {

    private FirstProcess(File socketFile) {
        super(socketFile);
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 0) {
            new FirstProcess(new File(new File(System.getProperty(args[PROPERTY_INDEX])), args[SOCKET_INDEX])).runProcess(Math::cos);
        }
    }
}