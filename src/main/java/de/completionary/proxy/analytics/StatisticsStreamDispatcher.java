package de.completionary.proxy.analytics;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.async.TAsyncClientManager;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TNonblockingSocket;
import org.apache.thrift.transport.TTransportException;

import de.completionary.proxy.elasticsearch.SuggestionIndex;
import de.completionary.proxy.thrift.clients.StreamingClientServiceClient;
import de.completionary.proxy.thrift.services.exceptions.IndexUnknownException;
import de.completionary.proxy.thrift.services.exceptions.InvalidIndexNameException;
import de.completionary.proxy.thrift.services.exceptions.ServerDownException;
import de.completionary.proxy.thrift.services.streaming.StreamedStatisticsField;

public class StatisticsStreamDispatcher extends TimerTask {

    /*
     * Time between two statistics updates sent to the clients
     */
    private static final int SendingDelay = 1000;

    private final Map<StreamingClientServiceClient, Set<String>> indicesByClient =
            new HashMap<StreamingClientServiceClient, Set<String>>();

    private final Map<String, StreamingClientServiceClient> clientsByHostAndPort =
            new HashMap<String, StreamingClientServiceClient>();

    public StatisticsStreamDispatcher() {
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
     * @param resultHandler
     *            Callback to be called as soon as the client is registered
     */
    public synchronized void registerClient(
            String index,
            String hostName,
            int port,
            AsyncMethodCallback<Void> resultHandler) {
        System.out.println("Connecting to streaming client " + hostName + ":"
                + port);

        if (!SuggestionIndex.indexExists(index)) {
            resultHandler
                    .onError(new IndexUnknownException(
                            "You tried to register for a statistics stream of index '"
                                    + index
                                    + "' even thought this index is not stored in our database"));
            return;
        }

        /*
         * Check if we already know the client
         */
        final String hostAndPortKey = hostName + port;

        StreamingClientServiceClient client =
                clientsByHostAndPort.get(hostAndPortKey);

        if (client != null) {
            registerIndex(client, index);
            resultHandler.onComplete(null);
            return;
        }

        /*
         * Connect to the client as it's an unknown one
         */
        try {
            TNonblockingSocket transport =
                    new TNonblockingSocket(hostName, port);
            transport.setTimeout(10);

            TProtocol protocol = new TBinaryProtocol(transport);

            TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();
            TAsyncClientManager clientManager = new TAsyncClientManager();
            // new TBinaryProtocol.Factory(), new TAsyncClientManager(),
            // transport,

            client =
                    new StreamingClientServiceClient(protocolFactory,
                            clientManager, transport, hostName, port);

            clientsByHostAndPort.put(hostAndPortKey, client);
            registerIndex(client, index);
            resultHandler.onComplete(null);

            // for (int i = 0; i < 3; i++) {
            // try {
            // transport.open();
            // clientsByHostAndPort.put(hostAndPortKey, client);
            // registerIndex(client, index);
            // resultHandler.onComplete(null);
            // return;
            // } catch (TTransportException e) {
            // try {
            // Thread.sleep(100);
            // } catch (InterruptedException e1) {
            // }
            // }
            // }
            //
            // resultHandler
            // .onError(new UnableToConnectToStreamingClientException(
            // "Unable to connect to "
            // + hostName
            // + ":"
            // + port
            // +
            // " which should be the address of a listening streaming server."));
        } catch (IOException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }

    }

    /**
     * Registers a client with an index
     * 
     * @param client
     *            The client the statistics stream should be sent to
     * @param index
     *            Index of the statistics to be sent
     */
    private void
        registerIndex(StreamingClientServiceClient client, String index) {

        Set<String> indices = indicesByClient.get(client);
        if (indices == null) {
            indices = new HashSet<String>();
            indices.add(index);
            indicesByClient.put(client, indices);
        }
        indices.add(index);
    }

    /**
     * Unregisters a client from receiving a statistics stream
     * 
     * @param index
     *            Index of the statistics of the stream
     * @param hostName
     *            Hostname of the client
     * @param port
     *            Port number at which the StreamingServer at the client side is
     *            listening to
     * @param resultHandler
     *            Callback to be called as soon as the client is unregistered
     */
    public synchronized void unregisterIndex(
            String index,
            String hostName,
            int port,
            AsyncMethodCallback<Void> resultHandler) {
        final String hostAndPortKey = hostName + port;

        StreamingClientServiceClient client =
                clientsByHostAndPort.get(hostAndPortKey);

        if (client != null) {
            Set<String> indices = indicesByClient.get(client);
            indices.remove(index);

            if (indices.isEmpty()) {
                clientsByHostAndPort.remove(hostAndPortKey);
                client = null;
            }
        }
        resultHandler.onComplete(null);
    }

    /**
     * Disconnects all streams to the given client
     * 
     * @param client
     *            The client which is to be disconnected
     */
    public synchronized void disconnectClient(
            StreamingClientServiceClient client) {
        if (client != null) {
            System.out.println("Disconnecting from streaming client " + client);
            indicesByClient.remove(client);
            clientsByHostAndPort.remove(client.hostname + client.port);
        }
    }

    /**
     * Disconnects all streams to the given client
     * 
     * @param client
     *            The client which is to be disconnected
     */
    public synchronized void disconnectClient(
            String hostName,
            int port,
            AsyncMethodCallback resultHandler) {
        final String hostAndPortKey = hostName + port;

        StreamingClientServiceClient client =
                clientsByHostAndPort.get(hostAndPortKey);
        if (client != null) {
            System.out.println("Disconnecting streaming client " + client);
            indicesByClient.remove(client);
            clientsByHostAndPort.remove(client.hostname + client.port);
        }
        resultHandler.onComplete(null);
    }

    /**
     * This method is called every second to send all the new statistics to all
     * clients
     */
    @Override
    public void run() {
        System.out.println("Sending statistics to " + indicesByClient.size()
                + " clients");
        /*
         * Iterate through all connected clients
         */
        for (Map.Entry<StreamingClientServiceClient, Set<String>> entry : indicesByClient
                .entrySet()) {
            StreamingClientServiceClient client = entry.getKey();
            Set<String> indeces = entry.getValue();

            Map<String, StreamedStatisticsField> streamForClient =
                    new HashMap<String, StreamedStatisticsField>();

            Map<String, StreamedStatisticsField> retrievedStats =
                    new HashMap<String, StreamedStatisticsField>();
            /*
             * Fill the stream with all index related data
             */
            for (String index : indeces) {
                StreamedStatisticsField field = retrievedStats.get(index);
                if (field == null) {
                    try {
                        field =
                                SuggestionIndex.getIndex(index)
                                        .getCurrentStatistics();
                    } catch (InvalidIndexNameException | ServerDownException e) {
                        e.printStackTrace();
                    }

                    retrievedStats.put(index, field);
                }
                streamForClient.put(index, field);
            }

            /*
             * Send the stream to the client
             */
            final StreamingClientServiceClient theClient = client;
            try {
                theClient.updateStatistics(streamForClient,
                        new AsyncMethodCallback<Object>() {

                            @Override
                            public void onComplete(Object o) {
                            }

                            @Override
                            public void onError(Exception e) {
                                System.err
                                        .println("Unable to send stats to client "
                                                + theClient
                                                + ": "
                                                + e.getMessage());
                                disconnectClient(theClient);
                            }
                        });
            } catch (TTransportException | IllegalStateException e) {
                System.err.println("Unable to send stats to client " + client
                        + ": " + e.getMessage());
                disconnectClient(client);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
