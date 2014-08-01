package de.completionary.proxy.thrift;

import java.util.Map;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import de.completionary.proxy.thrift.services.streaming.StreamedStatisticsField;
import de.completionary.proxy.thrift.services.streaming.StreamingService;

public class StreamingClientHandler implements StreamingService.AsyncIface {

    @Override
    public void establishStream(
            String index,
            String hostName,
            int port,
            int sampleSize,
            AsyncMethodCallback resultHandler) throws TException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void disconnectStream(
            String index,
            String hostName,
            int port,
            AsyncMethodCallback resultHandler) throws TException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void updateStatistics(
            Map<String, StreamedStatisticsField> stream,
            AsyncMethodCallback resultHandler) throws TException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void
        disconnectFromStatisticStream(AsyncMethodCallback resultHandler)
                throws TException {
        // TODO Auto-generated method stub
        
    }

}
