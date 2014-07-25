package de.completionary.proxy;

import de.completionary.proxy.thrift.server.AdminServer;
import de.completionary.proxy.thrift.server.AnalyticsServer;
import de.completionary.proxy.thrift.server.StreamingServer;

public class CompletionProxy {

    static boolean running = true;

    public static void main(String[] args) {
        AdminServer adminServer = new AdminServer();
        adminServer.start();

        AnalyticsServer analyticsServer = new AnalyticsServer();
        analyticsServer.start();

        StreamingServer streamingServer = new StreamingServer();
        streamingServer.start();

        try {
            adminServer.join();
            analyticsServer.join();
            streamingServer.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
