package de.completionary.proxy.thrift.server;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;

import de.completionary.proxy.thrift.handler.AdminHandler;
import de.completionary.proxy.thrift.services.admin.AdminService;

public class AdminServer {

    public static void startNewInstance() {
        try {
            Runnable simple = new Runnable() {

                public void run() {
                    server();
                }
            };

            Thread t = new Thread(simple);
            t.start();
            Thread.sleep(100);
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    private static void server() {
        try {

            TNonblockingServerTransport trans =
                    new TNonblockingServerSocket(9090);
            TNonblockingServer.Args args = new TNonblockingServer.Args(trans);
            args.transportFactory(new TFramedTransport.Factory());
            args.protocolFactory(new TBinaryProtocol.Factory());
            args.processor(new AdminService.AsyncProcessor<AdminService.AsyncIface>(
                    new AdminHandler()));
            TServer server = new TNonblockingServer(args);
            server.serve();

            //            
            //            TServerSocket serverTransport = new TServerSocket(9090);
            //
            //            SuggestionService.AsyncProcessor<SuggestionHandler> processor =
            //                    new SuggestionService.AsyncProcessor<SuggestionHandler>(
            //                            new SuggestionHandler());
            //
            //            TServer server =
            //                    new TThreadPoolServer(new TThreadPoolServer.Args(
            //                            serverTransport).processor(processor));
            //            System.out.println("Starting thrift server ...");
            //            server.serve();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
