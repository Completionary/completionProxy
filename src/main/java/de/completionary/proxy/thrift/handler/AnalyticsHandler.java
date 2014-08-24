package de.completionary.proxy.thrift.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import de.completionary.proxy.elasticsearch.SuggestionIndex;
import de.completionary.proxy.thrift.services.admin.SuggestionField;
import de.completionary.proxy.thrift.services.analytics.AnalyticsService;
import de.completionary.proxy.thrift.services.exceptions.InvalidIndexNameException;
import de.completionary.proxy.thrift.services.exceptions.ServerDownException;
import de.completionary.proxy.thrift.services.streaming.StreamedStatisticsField;

public class AnalyticsHandler implements AnalyticsService.AsyncIface {

    public void topQueriesSince(
            int date,
            short k,
            AsyncMethodCallback/* <List<SuggestionField> > */resultHandler)
            throws TException {
        // TODO Auto-generated method stub
        List<SuggestionField> l = new ArrayList<SuggestionField>();
        l.add(new SuggestionField(1, "output string", Arrays
                .asList(new String[] {
                    "output", "string"
                }), "payload", 1));
        resultHandler.onComplete(l);
    }

    @Override
    public void getNumberOfTotalQueriesThisMonth(
            String index,
            AsyncMethodCallback resultHandler) throws TException {
        // TODO Auto-generated method stub

    }

    @Override
    public void getIndexSize(String index, AsyncMethodCallback resultHandler)
            throws TException {
        // TODO Auto-generated method stub

    }

    @Override
    public void getAnalytics(
            String index,
            long startTime,
            long endTime,
            AsyncMethodCallback resultHandler) {
        List<StreamedStatisticsField> list;
        try {
            list =
                    SuggestionIndex.getIndex(index).getStatistics()
                            .getStatistics(startTime, endTime);
            resultHandler.onComplete(list);
        } catch (IOException e) {
            resultHandler.onError(new ServerDownException());
        } catch (InvalidIndexNameException | ServerDownException e) {
            resultHandler.onError(e);
        }
    }
}
