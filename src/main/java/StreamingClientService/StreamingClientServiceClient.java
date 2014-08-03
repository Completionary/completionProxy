package StreamingClientService;

import org.apache.thrift.protocol.TProtocol;

import de.completionary.proxy.thrift.services.streaming.StreamingClientService.Client;

public class StreamingClientServiceClient extends Client {
	public final String hostname;
	public final int port;

	public StreamingClientServiceClient(TProtocol prot, String hostname,
			int port) {
		super(prot);
		this.hostname = hostname;
		this.port = port;
	}

}
