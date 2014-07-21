package de.completionary.proxy;

import de.completionary.proxy.server.CompletionServer;

public class CompletionProxy {

    static boolean running = true;

    public static void main(String[] args) {

        CompletionServer server = new CompletionServer();
        server.run();
    }
}
