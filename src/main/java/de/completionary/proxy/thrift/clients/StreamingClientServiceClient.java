package de.completionary.proxy.thrift.clients;

import org.apache.thrift.async.TAsyncClientManager;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TNonblockingTransport;

import de.completionary.proxy.thrift.services.streaming.StreamingClientService.AsyncClient;

public class StreamingClientServiceClient extends AsyncClient {

	public final String hostname;
	public final int port;

	public StreamingClientServiceClient(TProtocolFactory protocolFactory,
			TAsyncClientManager clientManager, TNonblockingTransport transport,
			String hostname, int port) {
		super(protocolFactory, clientManager, transport );

		this.hostname = hostname;
		this.port = port;
	}

	@Override
	public String toString() {
		return hostname + ":" + port;
	}
}
