package de.completionary.proxy.elasticsearch;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import org.apache.thrift.async.AsyncMethodCallback;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.mapping.delete.DeleteMappingResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;

import de.completionary.proxy.thrift.services.admin.SuggestionField;
import de.completionary.proxy.thrift.services.exceptions.InvalidIndexNameException;
import de.completionary.proxy.thrift.services.streaming.StreamedStatisticsField;
import de.completionary.proxy.thrift.services.suggestion.Suggestion;

public class SuggestionIndex {

	/*
	 * The es index identifying this suggestion index
	 */
	private final String index;

	private final StatisticsAggregator statisticsAggregator;

	private static Client esClient = null;

	/*
	 * Constants
	 */
	private static final String NAME_FIELD = "name";

	private static final String SUGGEST_FIELD = "suggest";

	private static final String TYPE = "t";

	private static Map<String, SuggestionIndex> indices = new TreeMap<String, SuggestionIndex>();

	static {
		boolean isClient = true;

		final Node node = nodeBuilder().clusterName("completionaryCluster")
				.client(isClient).node();
		esClient = node.client();

		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				System.out.println("Shutting down ES client");
				node.close();
			}
		});
	}

	/**
	 * Factory methode to create SuggestionIndex objects
	 * 
	 * @param index
	 *            The ID of the index
	 * @return A new Index or null in case of an error
	 */
	public static SuggestionIndex getIndex(final String index) {
		SuggestionIndex instance = indices.get(index);
		if (instance != null) {
			return instance;
		}

		synchronized (esClient) {
			try {
				instance = new SuggestionIndex(index);
			} catch (ExecutionException | InterruptedException | IOException e) {
				e.printStackTrace();
				return null;
			}
			indices.put(index, instance);
		}
		return instance;
	}

	private SuggestionIndex(String indexID) throws ExecutionException,
			InterruptedException, IOException {
		this.index = indexID;

		/*
		 * Create the ES index if it does not exist yet
		 */
		final boolean indexExists = indexExists(indexID);

		if (!indexExists) {
			createIndexIfNotExists();
			addMapping(TYPE);
		}

		statisticsAggregator = new StatisticsAggregator();
	}

	/**
	 * Find out if the index already exists in the ES database
	 * 
	 * @param index
	 * @return
	 */
	public static boolean indexExists(final String index) {
		if (index.equals("")) {
			return false;
		}
		return esClient.admin().indices()
				.exists(new IndicesExistsRequest(index)).actionGet().isExists();
	}

	/**
	 * Adds a list of SuggestionFields (terms) to the DB within one single bulk
	 * request and refreshes the index afterwards.
	 * 
	 * @param terms
	 *            The suggestion fields to be added
	 * @param listener
	 *            Callback that will be passed the number of milliseconds the
	 *            async ES call took
	 * @throws IOException
	 */
	public void async_addTerms(final List<SuggestionField> terms,
			final AsyncMethodCallback<Long> listener) throws IOException {

		BulkRequestBuilder bulkRequest = esClient.prepareBulk();
		for (SuggestionField field : terms) {
			bulkRequest.add(esClient.prepareIndex(index, TYPE, field.ID)
					.setSource(
							generateFieldJS(field.input, field.outputField,
									field.payload, field.weight)));
		}

		ListenableActionFuture<BulkResponse> future = bulkRequest.setRefresh(
				true).execute();

		future.addListener(new ActionListener<BulkResponse>() {

			public void onResponse(BulkResponse response) {
				listener.onComplete(response.getTookInMillis());
			}

			public void onFailure(Throwable e) {
				listener.onError(new Exception(e.getMessage()));
			}
		});
	}

	/**
	 * Adds a single term (SuggestionField) to the DB and refreshes the index.
	 * 
	 * @param ID
	 *            Used to reference this field for deletion queries. Must be
	 *            unique.
	 * @param input
	 *            The strings used to build the suggestion index.
	 * @param output
	 *            The String to be returned by a complete request if some of the
	 *            inputs are matching. If this element is NULL the matching
	 *            input string will be used instead. This string will also be
	 *            used to define the ID of the new field
	 * @param payload
	 *            The payload (e.g. images) stored with the field
	 * @param weight
	 *            The weight of the term
	 * @param listener
	 *            Callback that will be passed the number of milliseconds the
	 *            async ES call took
	 * @throws IOException
	 */
	public void async_addSingleTerm(final String ID, final List<String> inputs,
			final String output, final String payload, final int weight,
			final AsyncMethodCallback<Long> listener) throws IOException {

		ListenableActionFuture<IndexResponse> future = esClient
				.prepareIndex(index, TYPE, ID)
				.setSource(generateFieldJS(inputs, output, payload, weight))
				.setRefresh(true).execute();

		final long start = System.currentTimeMillis();

		future.addListener(new ActionListener<IndexResponse>() {

			public void onResponse(IndexResponse response) {
				listener.onComplete(System.currentTimeMillis() - start);
			}

			public void onFailure(Throwable e) {
				listener.onError(new Exception(e.getMessage()));
			}
		});

	}

	/**
	 * Deletes a single term (SuggestionField) and refreshes the index.
	 * 
	 * @param ID
	 *            ID of the field to be deleted
	 * @param listener
	 *            Callback to be called after executing the command. The
	 *            returned Boolean will store if the element was found.
	 * @return <true> in case the element has been found and deleted
	 * @throws IOException
	 */
	public void async_deleteSingleTerm(final String ID,
			final AsyncMethodCallback<Boolean> listener) throws IOException {

		ListenableActionFuture<DeleteResponse> future = esClient
				.prepareDelete(index, TYPE, ID).setRefresh(true).execute();

		future.addListener(new ActionListener<DeleteResponse>() {

			public void onResponse(DeleteResponse response) {
				listener.onComplete(response.isFound());
			}

			public void onFailure(Throwable e) {
				listener.onError(new Exception(e.getMessage()));
			}
		});
	}

	/**
	 * Deletes a list of terms in the DB within one single bulk request and
	 * refreshes the index afterwards.
	 * 
	 * @param ID
	 *            ID of the field to be deleted
	 * @param listener
	 *            Callback to be called after executing the command. The
	 *            returned Boolean will store if the element was found.
	 * @return <true> in case the element has been found and deleted
	 * @throws IOException
	 */
	public void async_deleteTerms(final List<String> IDs,
			final AsyncMethodCallback<Long> listener) throws IOException {

		BulkRequestBuilder bulkRequest = esClient.prepareBulk();
		for (String ID : IDs) {
			bulkRequest.add(esClient.prepareDelete(index, TYPE, ID));
		}

		ListenableActionFuture<BulkResponse> future = bulkRequest.setRefresh(
				true).execute();

		future.addListener(new ActionListener<BulkResponse>() {

			public void onResponse(BulkResponse response) {
				listener.onComplete(response.getTookInMillis());
			}

			public void onFailure(Throwable e) {
				listener.onError(new Exception(e.getMessage()));
			}
		});

	}

	/**
	 * Creates a jason node for a suggestion field
	 * 
	 * @see SuggestionIndex.addSingleTerm
	 * @return A new XContentBuilder with the field created
	 * @throws IOException
	 */
	private XContentBuilder generateFieldJS(final List<String> inputs,
			final String output, final String payload, final int weight)
			throws IOException {
		String name = output;
		if (name == null) {
			if (inputs.size() == 0) {
				return null;
			}
			name = inputs.get(0);
		}
		XContentBuilder b = jsonBuilder().startObject().field(NAME_FIELD, name)
				.startObject(SUGGEST_FIELD).startArray("input");

		for (String input : inputs) {
			b.value(input);
		}
		b.endArray();
		if (output != null) {
			b.field("output", output);
		}
		b.field("payload", payload).field("weight", weight).endObject()
				.endObject();
		return b;
	}

	/**
	 * 
	 * @param suggestRequest
	 *            The string to be completed
	 * @param size
	 *            The maximum number of suggestions to be returned
	 * 
	 * @param listener
	 *            Callback to be called as soon as the completion result has
	 *            arrived
	 */
	public void async_findSuggestionsFor(final String suggestRequest,
			final int size, final AsyncMethodCallback<List<Suggestion>> listener) {

		CompletionSuggestionBuilder compBuilder = new CompletionSuggestionBuilder(
				SUGGEST_FIELD).field(SUGGEST_FIELD).text(suggestRequest);

		ListenableActionFuture<SuggestResponse> future = esClient
				.prepareSuggest(index).addSuggestion(compBuilder).execute();

		future.addListener(new ActionListener<SuggestResponse>() {

			public void onResponse(SuggestResponse response) {
				List<Suggestion> suggestions = generateSuggestionsFromESResponse(
						response, size);
				listener.onComplete(suggestions);

				statisticsAggregator.onQuery(suggestRequest, suggestions);
			}

			public void onFailure(Throwable e) {
				listener.onError(new Exception(e.getMessage()));
			}
		});
	}

	/**
	 * Synchroneous way to acquire suggestions from a String
	 * 
	 * @param suggestRequest
	 *            The string to be completed
	 * @param size
	 *            The maximum number of suggestions to be returned
	 * @return A list of Suggestions fitting the requested string
	 */
	public List<Suggestion> findSuggestionsFor(final String suggestRequest,
			final int size) {

		CompletionSuggestionBuilder compBuilder = new CompletionSuggestionBuilder(
				SUGGEST_FIELD).field(SUGGEST_FIELD).text(suggestRequest);

		SuggestRequestBuilder suggestRequestBuilder = esClient.prepareSuggest(
				index).addSuggestion(compBuilder);

		SuggestResponse response = suggestRequestBuilder.execute().actionGet();

		List<Suggestion> suggestions = generateSuggestionsFromESResponse(
				response, size);
		statisticsAggregator.onQuery(suggestRequest, suggestions);

		return suggestions;
	}

	/**
	 * Splits an ES SuggestResponse into a list of suggestions
	 * 
	 * @param response
	 *            The response coming from the ES server
	 * @param size
	 *            The number of suggestion string in the response
	 * @return The generated List of suggestion containing all suggestions in
	 *         the given response
	 */
	private List<Suggestion> generateSuggestionsFromESResponse(
			final SuggestResponse response, final int size) {

		CompletionSuggestion compSuggestion = response.getSuggest()
				.getSuggestion(SUGGEST_FIELD);

		if (compSuggestion != null) {
			List<CompletionSuggestion.Entry> entryList = compSuggestion
					.getEntries();

			if (entryList != null) {
				// We request only 1 completion -> get(0)
				CompletionSuggestion.Entry entry = entryList.get(0);
				List<CompletionSuggestion.Entry.Option> options = entry
						.getOptions();
				/*
				 * Loop through all suggestions
				 */
				List<Suggestion> suggestions = new ArrayList<Suggestion>(
						options.size());

				for (CompletionSuggestion.Entry.Option option : options) {
					suggestions.add(new Suggestion(option.getText().toString(),
							option.getPayloadAsString()));
				}
				return suggestions;
			}
		}
		return new ArrayList<Suggestion>(0);
	}

	/**
	 * Stores a new mapping. A json similar to following is generated: {"type" :
	 * { "properties" : { "name" : { "type" : "string" }, "suggest" : { "type" :
	 * "completion", "index_analyzer" : "simple", "search_analyzer" : "simple",
	 * "payloads" : true }}}}
	 * 
	 * @param index
	 *            ES index the new mapping should be added to
	 * @param type
	 *            The type of the mapping
	 * @throws IOException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private void addMapping(String type) throws IOException,
			InterruptedException, ExecutionException {
		XContentBuilder js = jsonBuilder().startObject().startObject(type)
				.startObject("properties").startObject("name")
				.field("type", "string").endObject().startObject(SUGGEST_FIELD)
				.field("type", "completion").field("index_analyzer", "simple")
				.field("search_analyzer", "simple").field("payloads", true)
				.endObject().endObject().endObject().endObject();

		esClient.admin().indices().preparePutMapping(index).setType(type)
				.setSource(js).get();
	}

	private void async_addMapping(String type,
			final AsyncMethodCallback<Boolean> listener) throws IOException,
			InterruptedException, ExecutionException {

		XContentBuilder js = jsonBuilder().startObject().startObject(type)
				.startObject("properties").startObject("name")
				.field("type", "string").endObject().startObject(SUGGEST_FIELD)
				.field("type", "completion").field("index_analyzer", "simple")
				.field("search_analyzer", "simple").field("payloads", true)
				.endObject().endObject().endObject().endObject();

		ListenableActionFuture<PutMappingResponse> future = esClient.admin()
				.indices().preparePutMapping(index).setType(type).setSource(js)
				.execute();

		future.addListener(new ActionListener<PutMappingResponse>() {

			public void onResponse(PutMappingResponse response) {

				listener.onComplete(response.isAcknowledged());
			}

			public void onFailure(Throwable e) {
				listener.onError(new Exception(e.getMessage()));
			}
		});

	}

	/**
	 * Creates a new ES index if it does not exists yet
	 * 
	 * @param index
	 *            The index to be created
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private void createIndexIfNotExists() throws ExecutionException,
			InterruptedException {
		try {
			CreateIndexResponse response = esClient.admin().indices()
					.create(new CreateIndexRequest(index)).get();

			if (!response.isAcknowledged()) {
				System.err.println("Unable to create index " + index);
			}
		} catch (ExecutionException e) {
			// Ignore IndexAlreadyExistsExceptions
			if (!((e.getCause().getCause()) instanceof IndexAlreadyExistsException)) {
				throw e;
			}
		}
	}

	/**
	 * Deletes the whole ES index
	 * 
	 * @param index
	 *            The index to be deleted
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public static void delete(final String index) throws InterruptedException,
			ExecutionException {
		final DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(
				index);
		try {
			esClient.admin().indices().delete(deleteIndexRequest).get();
		} catch (ExecutionException e) {
			// Ignore IndexMissingException
			if (!((e.getCause().getCause()) instanceof IndexMissingException)) {
				throw e;
			}
		}
		indices.remove(index);
	}

	/**
	 * Empties the index (all fields are deleted)
	 * 
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws IOException
	 */
	public void truncate() throws InterruptedException, ExecutionException,
			IOException {
		esClient.admin().indices().prepareDeleteMapping(index).setType(TYPE)
				.execute().actionGet();
		waitForYellow();
		addMapping(TYPE);
	}

	/**
	 * Empties the index (all fields are deleted)
	 * 
	 * @param listener
	 *            Callback that will be passed the number of milliseconds the
	 *            async ES call took
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws IOException
	 */
	public void async_truncate(final AsyncMethodCallback<Long> listener)
			throws InterruptedException, ExecutionException, IOException {

		final long start = System.currentTimeMillis();

		ListenableActionFuture<DeleteMappingResponse> future = esClient.admin()
				.indices().prepareDeleteMapping(index).setType(TYPE).execute();

		/*
		 * Execute async truncate
		 */
		future.addListener(new ActionListener<DeleteMappingResponse>() {

			public void onResponse(DeleteMappingResponse response) {

				/*
				 * Execute async waitForYellow
				 */
				async_waitForYellow(new AsyncMethodCallback<Object>() {

					public void onComplete(Object o) {
						try {

							/*
							 * Recreate the mapping asynchronously
							 */
							async_addMapping(TYPE,
									new AsyncMethodCallback<Boolean>() {

										public void onComplete(Boolean b) {
											if (b) {
												listener.onComplete(System
														.currentTimeMillis()
														- start);
											} else {
												listener.onError(new Exception(
														"Unable to recreate Mapping after truncation"));
											}
										}

										public void onError(Exception e) {
											listener.onError(new Exception(e
													.getMessage()));
										}
									});

						} catch (Exception e) {
							listener.onError(e);
						}
					}

					public void onError(Exception e) {
						listener.onError(new Exception(e.getMessage()));
					}
				});
			}

			public void onFailure(Throwable e) {
				listener.onError(new Exception(e.getMessage()));
			}
		});
	}

	/**
	 * 
	 * @param listener
	 *            Callback that will be called after success with null
	 */
	public void async_waitForYellow(final AsyncMethodCallback<Object> listener) {
		ListenableActionFuture<ClusterHealthResponse> future = esClient.admin()
				.cluster().prepareHealth().setIndices(index)
				.setWaitForYellowStatus().execute();

		future.addListener(new ActionListener<ClusterHealthResponse>() {

			public void onResponse(ClusterHealthResponse response) {
				listener.onComplete(null);
			}

			public void onFailure(Throwable e) {
				listener.onError(new Exception(e.getMessage()));
			}
		});
	}

	/**
	 * Returns the statistics aggregated since last call of this method
	 * 
	 * @return
	 */
	public StreamedStatisticsField getCurrentStatistics() {
		return statisticsAggregator.getCurrentStatistics();
	}

	public void waitForYellow() {
		esClient.admin().cluster().prepareHealth().setIndices(index).execute()
				.actionGet();
	}

	public void waitForGreen() {
		esClient.admin().cluster().prepareHealth().setIndices(index)
				.setWaitForGreenStatus().execute().actionGet();
	}

	private void expungeDeletes() {
		esClient.admin().indices().prepareOptimize(index)
				.setOnlyExpungeDeletes(true).setWaitForMerge(true).execute()
				.actionGet();
	}

	/**
	 * Checks if an index is valid or malformed
	 * 
	 * @param index
	 *            The index to be checked
	 * @return <true> if the given index is valid
	 */
	public static void checkIndexValidity(String index) throws InvalidIndexNameException {
		if (!index.toLowerCase().equals(index)) {
			throw new InvalidIndexNameException("An index must be lowercase");
		}
	}
}
