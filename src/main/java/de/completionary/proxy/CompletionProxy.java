package de.completionary.proxy;

import de.completionary.proxy.thrift.server.AdminServer;
import de.completionary.proxy.thrift.server.AnalyticsServer;
import de.completionary.proxy.thrift.server.StreamingServer;

public class CompletionProxy {

    static boolean running = true;

    public static void main(String[] args) {
        System.out.println("Starting AdminServer");
        AdminServer adminServer = new AdminServer();
        adminServer.start();

        System.out.println("Starting AnalyticsServer");
        AnalyticsServer analyticsServer = new AnalyticsServer();
        analyticsServer.start();

        System.out.println("Starting StreamingServer");
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
