package de.completionary.proxy.thrift.handler;

import java.util.Map;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import de.completionary.proxy.streaming.StatisticsDispatcher;
import de.completionary.proxy.thrift.services.streaming.StreamedStatisticsField;
import de.completionary.proxy.thrift.services.streaming.StreamingService;

public class StreamingHandler implements StreamingService.AsyncIface {

    private final StatisticsDispatcher dispatcher;

    public StreamingHandler() {
        super();
        dispatcher = new StatisticsDispatcher();
    }

    @Override
    public void establishStream(
            String index,
            String hostName,
            int port,
            int sampleSize,
            AsyncMethodCallback resultHandler) throws TException {
        
        dispatcher.registerClient(index, hostName, port, sampleSize, resultHandler);
    }

    @Override
    public void disconnectStream(
            String index,
            String hostName,
            int port,
            AsyncMethodCallback resultHandler) throws TException {
        resultHandler.onComplete(null);
    }

    @Override
    public void updateStatistics(
            Map<String, StreamedStatisticsField> stream,
            AsyncMethodCallback resultHandler) throws TException {
        resultHandler.onComplete(null);

    }

    @Override
    public void
        disconnectFromStatisticStream(AsyncMethodCallback resultHandler)
                throws TException {
        resultHandler.onComplete(null);
    }
}
