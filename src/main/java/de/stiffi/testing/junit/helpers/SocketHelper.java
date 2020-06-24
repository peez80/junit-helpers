package de.stiffi.testing.junit.helpers;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ThreadLocalRandom;

public class SocketHelper {

    private static final int MIN_PORT = 1025;
    private static final int MAX_PORT = 32700;

    public static int findFreePort() {
        return findFreePort(MIN_PORT, MAX_PORT);
    }

    public static int findFreePort(int minPort, int maxPort)  {


        boolean foundFreePort = false;
        for (int i=0; i<2000; i++) {
            int port = ThreadLocalRandom.current().nextInt(minPort, maxPort);
            try {
                ServerSocket s = new ServerSocket(port);
                s.close();
                return port;
            } catch (IOException e) {
                //Do nothing - Socket is just already used
            }

        }
        throw new IllegalStateException("Couldn't find a random free port within 2000 tries.");

    }
}
