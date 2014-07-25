package de.completionary.proxy.thrift.handler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import de.completionary.proxy.elasticsearch.SuggestionIndex;
import de.completionary.proxy.thrift.services.admin.AdminService;
import de.completionary.proxy.thrift.services.suggestion.SuggestionField;

public class AdminHandler implements AdminService.AsyncIface {

    public void addSingleTerm(
            String index,
            String ID,
            List<String> inputs,
            String output,
            String payload,
            int weight,
            final AsyncMethodCallback resultHandler) throws TException {
        try {
            SuggestionIndex.getIndex(index).async_addSingleTerm(ID, inputs,
                    output, payload, weight, new AsyncMethodCallback<Long>() {

                        public void onComplete(Long time) {
                            resultHandler.onComplete(time);
                        }

                        public void onError(Exception e) {
                            resultHandler.onError(e);
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
            resultHandler.onError(e);
        }

    }

    public void addTerms(
            String index,
            List<SuggestionField> terms,
            final AsyncMethodCallback resultHandler) throws TException {
        try {
            SuggestionIndex.getIndex(index).async_addTerms(terms,
                    new AsyncMethodCallback<Long>() {

                        public void onComplete(Long time) {
                            resultHandler.onComplete(time);
                        }

                        public void onError(Exception e) {
                            resultHandler.onError(e);
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
            resultHandler.onError(e);
        }

    }

    public void deleteSingleTerm(
            String index,
            String ID,
            final AsyncMethodCallback resultHandler) throws TException {
        try {
            SuggestionIndex.getIndex(index).async_deleteSingleTerm(ID,
                    new AsyncMethodCallback<Boolean>() {

                        public void onComplete(Boolean b) {
                            resultHandler.onComplete(b);
                        }

                        public void onError(Exception e) {
                            resultHandler.onError(e);
                        }
                    });
        } catch (IOException e) {
            resultHandler.onError(e);
        }
    }

    public void deleteTerms(
            String index,
            List<String> IDs,
            final AsyncMethodCallback resultHandler) throws TException {
        try {
            SuggestionIndex.getIndex(index).async_deleteTerms(IDs,
                    new AsyncMethodCallback<Long>() {

                        public void onComplete(Long b) {
                            resultHandler.onComplete(b);
                        }

                        public void onError(Exception e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            resultHandler.onError(e);
        }

    }

    public void deleteIndex(
            String index,
            final AsyncMethodCallback resultHandler) throws TException {
        try {
            SuggestionIndex.delete(index);
        } catch (InterruptedException e) {
            resultHandler.onError(e);
        } catch (ExecutionException e) {
            resultHandler.onError(e);
        }
    }

    public void truncateIndex(
            String index,
            final AsyncMethodCallback resultHandler) throws TException {
        try {
            SuggestionIndex.getIndex(index).async_truncate(
                    new AsyncMethodCallback<Long>() {

                        public void onError(Exception e) {
                            resultHandler.onError(e);
                        }

                        public void onComplete(Long time) {
                            resultHandler.onComplete(time);
                        }
                    });
        } catch (Exception e) {
            resultHandler.onError(e);
        }
    }
}
