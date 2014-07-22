package de.completionary.proxy.thrift.server;

import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;

import de.completionary.proxy.thrift.impl.SuggestionHandler;
import de.completionary.proxy.thrift.services.SuggestionService;

public class SuggestionServer {

    public static void startNewInstance() {
        try {
            Runnable simple = new Runnable() {

                public void run() {
                    simple();
                }
            };

            Thread t = new Thread(simple);
            t.start();
            Thread.sleep(100);
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    private static void simple() {

        try {
            TServerSocket serverTransport = new TServerSocket(9090);

            SuggestionService.AsyncProcessor<SuggestionHandler> processor =
                    new SuggestionService.AsyncProcessor<SuggestionHandler>(
                            new SuggestionHandler());

            TServer server =
                    new TThreadPoolServer(new TThreadPoolServer.Args(
                            serverTransport).processor(processor));
            System.out.println("Starting thrift server ...");
            server.serve();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
