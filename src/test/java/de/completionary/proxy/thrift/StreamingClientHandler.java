package de.completionary.proxy.thrift;

import java.util.Map;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import de.completionary.proxy.thrift.services.streaming.StreamedStatisticsField;
import de.completionary.proxy.thrift.services.streaming.StreamingClientService;

public class StreamingClientHandler implements
        StreamingClientService.AsyncIface {

    @Override
    public void updateStatistics(
            Map<String, StreamedStatisticsField> stream,
            AsyncMethodCallback resultHandler) throws TException {
        // TODO Auto-generated method stub

    }
}