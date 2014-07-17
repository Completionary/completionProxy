package de.completionary.proxy;

//import static org.elasticsearch.common.xcontent.XContentFactory.*;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;

public class Main {
	static Client client;
	static final String INDEX = "test9";
	static final String TYPE = "type";
	static final String NAME_FIELD = "name";
	static final String SUGGEST_FIELD = "suggest";

	static final String USER_MAPPING = "currentMapping";

	public static void main(String[] args) throws ElasticsearchException,
			IOException, InterruptedException, ExecutionException {
		boolean isClient = false;
		Node node = nodeBuilder().clusterName("completionaryCluster")
				.client(isClient).node();
		client = node.client();
		/*******************************************************************/
		// createIndex(INDEX);
		deleteIndex(INDEX);

		client.admin()
				.indices()
				.prepareCreate(INDEX)
				.addMapping(
						TYPE,
						jsonBuilder().startObject().startObject(TYPE)
								.startObject("properties")
								.startObject(SUGGEST_FIELD)
								.field("type", "completion")
								.field("index_analyzer", "simple")
								.field("search_analyzer", "simple")
								.field("payloads", true).endObject()
								.endObject().endObject().endObject()).get();

		// PutMappingResponse putMappingResponse = client
		// .admin()
		// .indices()
		// .preparePutMapping(INDEX)
		// .setType(TYPE)
		// .setSource(
		// jsonBuilder().startObject().startObject(TYPE)
		// .startObject("properties")
		// .startObject(NAME_FIELD)
		// .field("type", "string")
		// .field("path", "just_name")
		// .startObject("fields").startObject(NAME_FIELD)
		// .field("type", "string").endObject()
		// .startObject("SUGGEST_FIELD")
		// .field("type", "completion")
		// .field("index_analyzer", "simple")
		// .field("search_analyzer", "simple").endObject()
		// .endObject().endObject().endObject()
		// .endObject().endObject()).get();

		String[] input = { "Foo", "fla" };

		for (int i = 0; i < input.length; i++) {

			XContentBuilder b = jsonBuilder().startObject()
					.field(NAME_FIELD, input[i]).startObject(SUGGEST_FIELD)
					.startArray("input").value(input[i]).endArray()
					.field("output", input[i])
					.field("payload", input[i] + "payload").field("weight", 1)
					.endObject().endObject();

			System.out.println(b.string());
			client.prepareIndex(INDEX, TYPE, "" + i).setSource(b).execute()
					.actionGet();
		}

		findSuggestionsFor("F", 5);

		/*******************************************************************/
		// on shutdown
		node.close();
	}

	/**
	 * 
	 * @param suggestRequest
	 *            String to be completed
	 * @param size
	 *            Maximum number of suggestion strings
	 * @return
	 */
	public static List<String> findSuggestionsFor(String suggestRequest,
			int size) {

		CompletionSuggestionBuilder compBuilder = new CompletionSuggestionBuilder(
				SUGGEST_FIELD);
		compBuilder.text(suggestRequest);
		compBuilder.field(SUGGEST_FIELD);

		SuggestRequestBuilder suggestRequestBuilder = client.prepareSuggest(
				INDEX).addSuggestion(compBuilder);

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
			if (options != null) {
				CompletionSuggestion.Entry.Option option = options.get(0);
				String toReturn = option.getText().string();
				System.out.println(toReturn);
			}
		}

		return null;
	}

	static void createIndex(String index) {
		try {
			client.admin().indices().create(Requests.createIndexRequest(index))
					.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (ExecutionException e) {
			// Ignore IndexAlreadyExistsExceptions
			if (!((e.getCause().getCause()) instanceof IndexAlreadyExistsException)) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	static void deleteIndex(String index) {
		try {
			client.admin().indices().delete(Requests.deleteIndexRequest(index))
					.get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
