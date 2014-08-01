package de.completionary.proxy.thrift.handler;

import java.util.Map;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import de.completionary.proxy.thrift.services.streaming.StreamedStatisticsField;
import de.completionary.proxy.thrift.services.streaming.StreamingService;

public class StreamingHandler implements StreamingService.AsyncIface {

    @Override
    public void establishStream(
            String index,
            String hostName,
            int port,
            AsyncMethodCallback resultHandler) throws TException {
        resultHandler.onComplete(null);
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
