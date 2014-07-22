package de.completionary.proxy.thrift.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import de.completionary.proxy.elasticsearch.SuggestionIndex;
import de.completionary.proxy.thrift.services.Suggestion;
import de.completionary.proxy.thrift.services.SuggestionService;

public class SuggestionHandler implements SuggestionService.AsyncIface {

    public void findSuggestionsFor(
            String index,
            String query,
            short k,
            AsyncMethodCallback resultHandler) throws TException {

        //      SuggestionIndex.getIndex(index).findSuggestionsFor(query, k, null);
        List<Suggestion> suggestions = new ArrayList<Suggestion>();
        
        resultHandler.onComplete(suggestions);

    }

}
