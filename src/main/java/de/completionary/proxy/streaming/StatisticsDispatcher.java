package de.completionary.proxy.streaming;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import de.completionary.proxy.thrift.services.streaming.StreamingService;

public class StatisticsDispatcher extends TimerTask {

    /*
     * Time between two statistics updates sent to the clients
     */
    private static final int SendingDelay = 1000;

    private final Map<StreamingService.Client, Set<IndexAndSampleSize>> clientsAndIndices =
            new HashMap<StreamingService.Client, Set<IndexAndSampleSize>>();

    private final Map<String, StreamingService.Client> clientsByHostAndPort =
            new HashMap<String, StreamingService.Client>();

    public StatisticsDispatcher() {
        Timer timer = new Timer();
        timer.schedule(this, SendingDelay, SendingDelay);
        System.out.println("StatisticsDispatcher started");
    }

    /**
     * Registers a new client to receive a statistics stream
     * 
     * @param index
     *            Index of the statistics to be sent
     * @param hostName
     *            Hostname of the client
     * @param port
     *            Port number at which the StreamingServer at the client side is
     *            listening to
     * @param sampleSize
     *            Number of random sample queries within the statistics stream
     * @param resultHandler
     *            Callback to be called as soon as the client is registered
     */
    public synchronized void registerClient(
            String index,
            String hostName,
            int port,
            int sampleSize,
            AsyncMethodCallback<Void> resultHandler) {
        System.out.println("Connecting to streaming client " + hostName + ":"
                + port);
        /*
         * Check if we already know the client
         */
        final String hostAndPortKey = hostName + port;

        StreamingService.Client client =
                clientsByHostAndPort.get(hostAndPortKey);

        if (client != null) {
            registerIndex(client, index, sampleSize);
            resultHandler.onComplete(null);
            return;
        }

        /*
         * Connect to the client as it's an unknown one
         */
        TTransport transport =
                new TFramedTransport(new TSocket(hostName, port));
        TProtocol protocol = new TCompactProtocol(transport);

        client = new StreamingService.Client(protocol);
        for (int i = 0; i < 3; i++) {
            try {
                transport.open();
                clientsByHostAndPort.put(hostAndPortKey, client);
                registerIndex(client, index, sampleSize);
                resultHandler.onComplete(null);
                return;
            } catch (TTransportException e) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                }
            }
        }

        resultHandler
                .onError(new Exception(
                        "Unable to connect to "
                                + hostName
                                + ":"
                                + port
                                + " which should be the address of a listening streaming server."));
    }

    /**
     * Registers a client with index and sampleSize
     * 
     * @param client
     *            The client the statistics stream should be sent to
     * @param index
     *            Index of the statistics to be sent
     * @param sampleSize
     *            Number of random sample queries within the statistics stream
     */
    private void registerIndex(
            StreamingService.Client client,
            String index,
            int sampleSize) {

        Set<IndexAndSampleSize> indices = clientsAndIndices.get(client);
        if (indices == null) {
            indices = new HashSet<IndexAndSampleSize>();
            clientsAndIndices.put(client, indices);
        }
        indices.add(new IndexAndSampleSize(index, sampleSize));
    }

    /**
     * The thread sends the statistics to all clients every second
     */
    @Override
    public void run() {
        System.out.println("now!");
    }
}
