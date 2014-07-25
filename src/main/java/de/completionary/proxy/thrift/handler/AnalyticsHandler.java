package de.completionary.proxy.thrift.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import de.completionary.proxy.thrift.services.admin.SuggestionField;
import de.completionary.proxy.thrift.services.analytics.AnalyticsService;

public class AnalyticsHandler implements AnalyticsService.AsyncIface {

    public void topQueriesSince(
            int date,
            short k,
            AsyncMethodCallback/* <List<SuggestionField> > */resultHandler)
            throws TException {
        List<SuggestionField> l = new ArrayList<SuggestionField>();
        l.add(new SuggestionField("ID1", "output string", Arrays
                .asList(new String[] {
                    "output", "string"
                }), "payload", 1));
        resultHandler.onComplete(l);
    }
}
