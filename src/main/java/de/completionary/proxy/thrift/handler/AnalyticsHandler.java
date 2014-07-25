package de.completionary.proxy.thrift.handler;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import de.completionary.proxy.thrift.services.analytics.AnalyticsService;

public class AnalyticsHandler implements AnalyticsService.AsyncIface {

    public void topQueriesSince(
            int date,
            short k,
            AsyncMethodCallback/* <List<SuggestionField> > */resultHandler)
            throws TException {
        // TODO Auto-generated method stub

    }

}
