package de.completionary.proxy.thrift.handler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.thrift.async.AsyncMethodCallback;

import de.completionary.proxy.elasticsearch.SuggestionIndex;
import de.completionary.proxy.thrift.services.admin.AdminService;
import de.completionary.proxy.thrift.services.admin.SuggestionField;
import de.completionary.proxy.thrift.services.exceptions.IndexUnknownException;
import de.completionary.proxy.thrift.services.exceptions.InvalidIndexNameException;
import de.completionary.proxy.thrift.services.exceptions.ServerDownException;

public class AdminHandler implements AdminService.AsyncIface {

    @Override
    @SuppressWarnings("rawtypes")
    public void addSingleTerm(
            String apiToken,
            String index,
            long ID,
            List<String> inputs,
            String output,
            String payload,
            int weight,
            final AsyncMethodCallback resultHandler)
            throws InvalidIndexNameException, ServerDownException,
            IndexUnknownException {

        try {
            SuggestionIndex.checkIndexValidity(index);
        } catch (InvalidIndexNameException e) {
            resultHandler.onError(e);
            return;
        }

        try {
            SuggestionIndex.getIndex(index).async_addSingleTerm(ID, inputs,
                    output, payload, weight, new AsyncMethodCallback<Long>() {

                        @SuppressWarnings("unchecked")
                        public void onComplete(Long time) {
                            resultHandler.onComplete(time);
                        }

                        public void onError(Exception e) {
                            resultHandler.onError(e);
                        }
                    });
        } catch (IOException e) {
            resultHandler.onError(new ServerDownException(e.getMessage()));
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void addTerms(
            String apiToken,
            String index,
            List<SuggestionField> terms,
            final AsyncMethodCallback resultHandler)
            throws InvalidIndexNameException, ServerDownException,
            IndexUnknownException {
        try {
            SuggestionIndex.checkIndexValidity(index);
        } catch (InvalidIndexNameException e) {
            resultHandler.onError(e);
            return;
        }

        try {
            SuggestionIndex.getIndex(index).async_addTerms(terms,
                    new AsyncMethodCallback<Long>() {

                        @SuppressWarnings("unchecked")
                        public void onComplete(Long time) {
                            resultHandler.onComplete(time);
                        }

                        public void onError(Exception e) {
                            resultHandler.onError(e);
                        }
                    });
        } catch (IOException e) {
            resultHandler.onError(new ServerDownException(e.getMessage()));
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void deleteSingleTerm(
            String apiToken,
            String index,
            long ID,
            final AsyncMethodCallback resultHandler)
            throws InvalidIndexNameException, ServerDownException,
            IndexUnknownException {
        try {
            SuggestionIndex.getIndex(index).async_deleteSingleTerm(ID,
                    new AsyncMethodCallback<Boolean>() {

                        @SuppressWarnings("unchecked")
                        public void onComplete(Boolean b) {
                            resultHandler.onComplete(b);
                        }

                        public void onError(Exception e) {
                            resultHandler.onError(e);
                        }
                    });
        } catch (IOException e) {
            resultHandler.onError(new ServerDownException(e.getMessage()));
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void deleteTerms(
            String apiToken,
            String index,
            List<Long> IDs,
            final AsyncMethodCallback resultHandler)
            throws InvalidIndexNameException, ServerDownException,
            IndexUnknownException {
        try {
            SuggestionIndex.getIndex(index).async_deleteTerms(IDs,
                    new AsyncMethodCallback<Long>() {

                        @SuppressWarnings("unchecked")
                        public void onComplete(Long b) {
                            resultHandler.onComplete(b);
                        }

                        public void onError(Exception e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            resultHandler.onError(new ServerDownException(e.getMessage()));
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void deleteIndex(
            String apiToken,
            String index,
            final AsyncMethodCallback resultHandler) {
        try {
            SuggestionIndex.delete(index);
        } catch (InterruptedException e) {
            resultHandler.onError(new ServerDownException(e.getMessage()));
        } catch (ExecutionException e) {
            resultHandler.onError(new ServerDownException(e.getMessage()));
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void truncateIndex(
            String apiToken,
            String index,
            final AsyncMethodCallback resultHandler)
            throws IndexUnknownException, InvalidIndexNameException,
            ServerDownException {
        try {
            SuggestionIndex.getIndex(index).async_truncate(

            new AsyncMethodCallback<Long>() {

                public void onError(Exception e) {
                    resultHandler.onError(e);
                }

                @SuppressWarnings("unchecked")
                public void onComplete(Long time) {
                    resultHandler.onComplete(time);
                }
            });
        } catch (InterruptedException | ExecutionException | IOException e) {
            resultHandler.onError(new ServerDownException(e.getMessage()));
        }
    }
}
