package de.completionary.proxy.thrift.handler;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import de.completionary.proxy.streaming.StatisticsDispatcher;
import de.completionary.proxy.thrift.services.streaming.StreamingService;

public class StreamingHandler implements StreamingService.AsyncIface {

	private final StatisticsDispatcher dispatcher;

	public StreamingHandler() {
		super();
		dispatcher = new StatisticsDispatcher();
	}

	@Override
	public void establishStream(String index, String hostName, int port,
			AsyncMethodCallback resultHandler) throws TException {

		dispatcher.registerClient(index, hostName, port, resultHandler);
	}

	@Override
	public void disconnectStream(String index, String hostName, int port,
			AsyncMethodCallback resultHandler) throws TException {
		dispatcher.unregisterIndex(index, hostName, port, resultHandler);
	}

	@Override
	public void disconnectFromStatisticStream(String hostName, int port,
			AsyncMethodCallback resultHandler) throws TException {
		dispatcher.disconnectClient(hostName, port, resultHandler);
	}
}
