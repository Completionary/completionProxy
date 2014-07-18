package de.completionary.proxy.elasticsearch;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.indices.IndexMissingException;
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
				boolean isClient = true;
				node = nodeBuilder().clusterName("completionaryCluster")
						.client(isClient).node();
				client = node.client();
				// waitForGreen();
			}
		}
		try {
			createIndexIfNotExists(index);
			addMapping(index, TYPE);
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void test() throws ElasticsearchException, IOException,
			InterruptedException, ExecutionException {

		deleteIndex(index);
		createIndexIfNotExists(index);

		addMapping(index, TYPE);
		String[] input = { "NI", "NA" };

		for (String s : input) {
			addSingleTerm(Arrays.asList(s), s, s, 1);
		}

		/*******************************************************************/
		// on shutdown
		node.close();
	}

	/**
	 * Adds a list of SuggestionFields (terms) to the DB within one single bulk
	 * request and refreshes the index afterwards.
	 * 
	 * @throws IOException
	 */
	public void addTerms(final List<SuggestionField> terms) throws IOException {
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		for (SuggestionField field : terms) {
			bulkRequest.add(client.prepareIndex(index, TYPE).setSource(
					generateFieldJS(field.input, field.output, field.payload,
							field.weight)));
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
	}

	/**
	 * Adds a single term (SuggestionField) to the DB and refreshes the index.
	 * 
	 * @throws IOException
	 */
	public void addSingleTerm(final List<String> inputs, final String output,
			final String payload, final int weight) throws IOException {
		client.prepareIndex(index, TYPE)
				.setSource(generateFieldJS(inputs, output, payload, weight))
				.setRefresh(true).execute().actionGet();
	}

	private XContentBuilder generateFieldJS(final List<String> inputs,
			final String output, final String payload, final int weight)
			throws IOException {
		XContentBuilder b = jsonBuilder().startObject()
				.field(NAME_FIELD, output).startObject(SUGGEST_FIELD)
				.startArray("input");

		for (String input : inputs) {
			b.value(input);
		}
		b.endArray().field("output", output).field("payload", payload)
				.field("weight", weight).endObject().endObject();
		return b;
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
		List<String> suggestions = new ArrayList<String>(size);
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
				suggestions.add(option.getText().toString());
			}
		}

		return suggestions;
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
			CreateIndexResponse response = client.admin().indices()
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

		try {
			client.admin().indices().delete(deleteIndexRequest).get();
		} catch (ExecutionException e) {
			// Ignore IndexMissingException
			if (!((e.getCause().getCause()) instanceof IndexMissingException)) {
				throw e;
			}
		}
	}

	private void waitForYellow() {
		client.admin().cluster().prepareHealth().setIndices(index);
	}

	private void waitForGreen() {
		client.admin().cluster().prepareHealth().setIndices(index)
				.setWaitForGreenStatus().execute().actionGet();
	}
}
