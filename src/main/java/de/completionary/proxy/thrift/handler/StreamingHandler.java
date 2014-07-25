package de.completionary.proxy.thrift.handler;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import de.completionary.proxy.thrift.services.streaming.*;

public class StreamingHandler implements StreamingService.AsyncIface {

    @Override
    public void
        establishStream(String index, AsyncMethodCallback resultHandler)
                throws TException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void
        disconnectStream(String index, AsyncMethodCallback resultHandler)
                throws TException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void connectToStatisticStream(AsyncMethodCallback resultHandler)
            throws TException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void
        disconnectFromStatisticStream(AsyncMethodCallback resultHandler)
                throws TException {
        // TODO Auto-generated method stub
        
    }
}
