package de.completionary.proxy.streaming;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import StreamingClientService.StreamingClientServiceClient;
import de.completionary.proxy.elasticsearch.SuggestionIndex;
import de.completionary.proxy.thrift.services.exceptions.IndexUnknownException;
import de.completionary.proxy.thrift.services.exceptions.InvalidIndexNameException;
import de.completionary.proxy.thrift.services.exceptions.ServerDownException;
import de.completionary.proxy.thrift.services.streaming.StreamedStatisticsField;
import de.completionary.proxy.thrift.services.streaming.UnableToConnectToStreamingClientException;

public class StatisticsDispatcher extends TimerTask {

	/*
	 * Time between two statistics updates sent to the clients
	 */
	private static final int SendingDelay = 1000;

	private final Map<StreamingClientServiceClient, Set<String>> indicesByClient = new HashMap<StreamingClientServiceClient, Set<String>>();

	private final Map<String, StreamingClientServiceClient> clientsByHostAndPort = new HashMap<String, StreamingClientServiceClient>();

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
	public synchronized void registerClient(String index, String hostName,
			int port, AsyncMethodCallback<Void> resultHandler) {
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

		StreamingClientServiceClient client = clientsByHostAndPort
				.get(hostAndPortKey);

		if (client != null) {
			registerIndex(client, index);
			resultHandler.onComplete(null);
			return;
		}

		/*
		 * Connect to the client as it's an unknown one
		 */
		TTransport transport = new TFramedTransport(new TSocket(hostName, port));
		TProtocol protocol = new TBinaryProtocol(transport);

		client = new StreamingClientServiceClient(protocol, hostName, port);
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
				.onError(new UnableToConnectToStreamingClientException(
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
	private void registerIndex(StreamingClientServiceClient client, String index) {

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
	public synchronized void unregisterIndex(String index, String hostName,
			int port, AsyncMethodCallback<Void> resultHandler) {
		/*
		 * Check if we already know the client
		 */
		final String hostAndPortKey = hostName + port;

		StreamingClientServiceClient client = clientsByHostAndPort
				.get(hostAndPortKey);

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
		System.out.println("Disconnecting from streaming client "
				+ client.hostname + ":" + client.port);
		if (client != null) {
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
	public synchronized void disconnectClient(String hostName, int port,
			AsyncMethodCallback resultHandler) {
		final String hostAndPortKey = hostName + port;

		StreamingClientServiceClient client = clientsByHostAndPort
				.get(hostAndPortKey);
		if (client != null) {
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

			Map<String, StreamedStatisticsField> streamForClient = new HashMap<String, StreamedStatisticsField>();

			Map<String, StreamedStatisticsField> retrievedStats = new HashMap<String, StreamedStatisticsField>();
			/*
			 * Fill the stream with all index related data
			 */
			for (String index : indeces) {
				StreamedStatisticsField field = retrievedStats.get(index);
				if (field == null) {
					try {
                        field = SuggestionIndex.getIndex(index)
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
			try {
				client.updateStatistics(streamForClient);
			} catch (TTransportException te) {
				disconnectClient(client);
			} catch (TException e) {
				e.printStackTrace();
			}
		}
	}
}
