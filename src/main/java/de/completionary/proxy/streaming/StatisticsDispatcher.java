package de.completionary.proxy.streaming;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import de.completionary.proxy.elasticsearch.SuggestionIndex;
import de.completionary.proxy.thrift.services.exceptions.IndexUnknownException;
import de.completionary.proxy.thrift.services.streaming.StreamedStatisticsField;
import de.completionary.proxy.thrift.services.streaming.StreamingClientService;

public class StatisticsDispatcher extends TimerTask {

    /*
     * Time between two statistics updates sent to the clients
     */
    private static final int SendingDelay = 1000;

    private final Map<StreamingClientService.Client, Set<String>> indicesByClient =
            new HashMap<StreamingClientService.Client, Set<String>>();

    private final Map<String, StreamingClientService.Client> clientsByHostAndPort =
            new HashMap<String, StreamingClientService.Client>();

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

        StreamingClientService.Client client =
                clientsByHostAndPort.get(hostAndPortKey);

        if (client != null) {
            registerIndex(client, index);
            resultHandler.onComplete(null);
            return;
        }

        /*
         * Connect to the client as it's an unknown one
         */
        TTransport transport =
                new TFramedTransport(new TSocket(hostName, port));
        TProtocol protocol = new TCompactProtocol(transport);

        client = new StreamingClientService.Client(protocol);
        for (int i = 0; i < 3; i++) {
            try {
                transport.open();
                clientsByHostAndPort.put(hostAndPortKey, client);
                registerIndex(client, index);
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
     * Registers a client with an index
     * 
     * @param client
     *            The client the statistics stream should be sent to
     * @param index
     *            Index of the statistics to be sent
     */
    private void registerIndex(
            StreamingClientService.Client client,
            String index) {

        Set<String> indices = indicesByClient.get(client);
        if (indices == null) {
            indices = new HashSet<String>();
            indices.add(index);
            indicesByClient.put(client, indices);
        }
        indices.add(index);
    }

    /**
     * This method is called every second to send all the new statistics to all
     * clients
     */
    @Override
    public void run() {
        /*
         * Iterate through all connected clients
         */
        for (Map.Entry<StreamingClientService.Client, Set<String>> entry : indicesByClient
                .entrySet()) {
            StreamingClientService.Client client = entry.getKey();
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
                    field =
                            SuggestionIndex.getIndex(index)
                                    .getCurrentStatistics();
                    retrievedStats.put(index, field);
                }
                streamForClient.put(index, field);
            }

            /*
             * Send the stream to the client
             */
            try {
                client.updateStatistics(streamForClient);
            } catch (TException e) {
                e.printStackTrace();
            }
        }
    }
}
