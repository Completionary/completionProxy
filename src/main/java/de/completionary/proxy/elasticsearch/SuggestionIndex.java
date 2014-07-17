package de.completionary.proxy.elasticsearch;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;

public class SuggestionIndex {
	/*
	 * The es index identifying this suggestion index
	 */
	private final String index;

	final static Object nodeMutex = new Object();
	private static Node node = null;
	private static Client client = null;

	private static final String NAME_FIELD = "name";
	private static final String SUGGEST_FIELD = "suggest";
	private static final String TYPE = "t";

	public SuggestionIndex(String indexID) {
		this.index = indexID;

		synchronized (nodeMutex) {
			if (client == null) {
				boolean isClient = false;
				node = nodeBuilder().clusterName("completionaryCluster")
						.client(isClient).node();
				client = node.client();
				waitForGreen();
			}
		}
	}

	public void test() throws ElasticsearchException, IOException,
			InterruptedException, ExecutionException {

		deleteIndex(index);
		createIndexIfNotExists(index);

		addMapping(index, TYPE);
		String[] input = { "NI" };

		BulkRequestBuilder bulkRequest = client.prepareBulk();
		for (int i = 0; i < input.length; i++) {

			XContentBuilder b = jsonBuilder().startObject()
					.field(NAME_FIELD, input[i]).startObject(SUGGEST_FIELD)
					.startArray("input").value(input[i]).endArray()
					.field("output", input[i])
					.field("payload", input[i] + "payload").field("weight", 1)
					.endObject().endObject();

			System.out.println(b.string());
			bulkRequest.add(client.prepareIndex(index, TYPE).setSource(b));
		}
		BulkResponse bulkResponse = bulkRequest.setRefresh(true).execute()
				.actionGet();

		if (bulkResponse.hasFailures()) {
			for (BulkItemResponse item : bulkResponse.getItems()) {
				System.err.println(item.getFailureMessage());
			}
		} else {
			findSuggestionsFor("n", 5);
		}

		/*******************************************************************/
		// on shutdown
		node.close();
	}

	public void addSingleTerm(String term) throws IOException {
		XContentBuilder b = jsonBuilder().startObject().field(NAME_FIELD, term)
				.startObject(SUGGEST_FIELD).startArray("input").value(term)
				.endArray().field("output", term)
				.field("payload", term + "payload").field("weight", 1)
				.endObject().endObject();

		System.out.println(b.string());
		client.prepareIndex(index, TYPE).setSource(b).setRefresh(true)
				.execute().actionGet();
	}

	/**
	 * 
	 * @param suggestRequest
	 *            String to be completed
	 * @param size
	 *            Maximum number of suggestion strings
	 * @return
	 */
	public List<String> findSuggestionsFor(String suggestRequest, int size) {

		CompletionSuggestionBuilder compBuilder = new CompletionSuggestionBuilder(
				SUGGEST_FIELD).field(SUGGEST_FIELD).text(suggestRequest);

		SuggestRequestBuilder suggestRequestBuilder = client.prepareSuggest(
				index).addSuggestion(compBuilder);

		SuggestResponse suggestResponse = suggestRequestBuilder.execute()
				.actionGet();
		CompletionSuggestion compSuggestion = suggestResponse.getSuggest()
				.getSuggestion(SUGGEST_FIELD);

		List<CompletionSuggestion.Entry> entryList = compSuggestion
				.getEntries();
		if (entryList != null) {
			CompletionSuggestion.Entry entry = entryList.get(0);
			List<CompletionSuggestion.Entry.Option> options = entry
					.getOptions();
			for (CompletionSuggestion.Entry.Option option : options) {
				String toReturn = option.getText().toString();
				System.out.println(toReturn);
			}
		}

		return null;
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
	 */
	public static void addMapping(String index, String type) throws IOException {
		XContentBuilder js = jsonBuilder().startObject().startObject(type)
				.startObject("properties").startObject("name")
				.field("type", "string").endObject().startObject(SUGGEST_FIELD)
				.field("type", "completion").field("index_analyzer", "simple")
				.field("search_analyzer", "simple").field("payloads", true)
				.endObject().endObject().endObject().endObject();

		client.admin().indices().preparePutMapping(index).setType(type)
				.setSource(js).get();
	}

	/**
	 * Creates a new ES index if it does not exists yet
	 * 
	 * @param index
	 *            The index to be created
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private void createIndexIfNotExists(String index)
			throws ExecutionException, InterruptedException {
		try {
			client.admin().indices().create(new CreateIndexRequest(index)).get();
			Thread.sleep(1000);
			waitForYellow();
		} catch (ExecutionException e) {
			// Ignore IndexAlreadyExistsExceptions
			if (!((e.getCause().getCause()) instanceof IndexAlreadyExistsException)) {
				throw e;
			}
		}
	}

	/**
	 * Deletes an ES index
	 * 
	 * @param index
	 *            The index to be deleted
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private void deleteIndex(String index) throws InterruptedException,
			ExecutionException {

		final DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(
				index);

		final DeleteIndexResponse deleteIndexResponse = this.client.admin()
				.indices().delete(deleteIndexRequest).actionGet();

		if (!deleteIndexResponse.isAcknowledged()) {
			System.err.println("Index " + index + " not deleted");
		} else {
			System.err.println("Index " + index + " deleted");
		}

//		client.admin().indices().delete(Requests.deleteIndexRequest(index))
//				.get();
		waitForYellow();
	}

	private void waitForYellow() {
		client.admin().cluster().prepareHealth().setIndices(index)
				.setWaitForYellowStatus().execute().actionGet();
	}

	private void waitForGreen() {
		client.admin().cluster().prepareHealth().setIndices(index)
				.setWaitForYellowStatus().execute().actionGet();
	}
}
