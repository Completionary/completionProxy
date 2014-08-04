package de.completionary.proxy.thrift.handler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.thrift.async.AsyncMethodCallback;

import de.completionary.proxy.elasticsearch.SuggestionIndex;
import de.completionary.proxy.thrift.services.admin.AdminService;
import de.completionary.proxy.thrift.services.admin.SuggestionField;
import de.completionary.proxy.thrift.services.exceptions.InvalidIndexNameException;
import de.completionary.proxy.thrift.services.exceptions.ServerDownException;

public class AdminHandler implements AdminService.AsyncIface {

	@Override
	public void addSingleTerm(String apiToken, String index, long ID,
			List<String> inputs, String output, String payload, int weight,
			final AsyncMethodCallback resultHandler) {

		try {
			SuggestionIndex.checkIndexValidity(index);
		} catch (InvalidIndexNameException e) {
			resultHandler.onError(e);
			return;
		}

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
			resultHandler.onError(new ServerDownException(e.getMessage()));
		}
	}

	@Override
	public void addTerms(String apiToken, String index,
			List<SuggestionField> terms, final AsyncMethodCallback resultHandler) {
		try {
			SuggestionIndex.checkIndexValidity(index);
		} catch (InvalidIndexNameException e) {
			resultHandler.onError(e);
			return;
		}

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
			resultHandler.onError(new ServerDownException(e.getMessage()));
		}
	}

	@Override
	public void deleteSingleTerm(String apiToken, String index, long ID,
			final AsyncMethodCallback resultHandler) {
		try {
			SuggestionIndex.getIndex(index).async_deleteSingleTerm(
					Long.toString(ID), new AsyncMethodCallback<Boolean>() {

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
	public void deleteTerms(String apiToken, String index, List<String> IDs,
			final AsyncMethodCallback resultHandler) {
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
			resultHandler.onError(new ServerDownException(e.getMessage()));
		}

	}

	@Override
	public void deleteIndex(String apiToken, String index,
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
	public void truncateIndex(String apiToken, String index,
			final AsyncMethodCallback resultHandler) {
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
			resultHandler.onError(new ServerDownException(e.getMessage()));
		}
	}
}
