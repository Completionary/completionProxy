package de.completionary.proxy.elasticsearch;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;

import de.completionary.proxy.server.ASuggestionsRetrievedListener;
import de.completionary.proxy.structs.Suggestion;
import de.completionary.proxy.structs.SuggestionField;

public class SuggestionIndex {

    /*
     * The es index identifying this suggestion index
     */
    private final String index;

    private static Node node = null;

    private static Client client = null;

    private static final String NAME_FIELD = "name";

    private static final String SUGGEST_FIELD = "suggest";

    private static final String TYPE = "t";

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                System.out.println("Shutting down ES client");
                node.close();
            }
        });

        boolean isClient = true;
        node =
                nodeBuilder().clusterName("completionaryCluster")
                        .client(isClient).node();
        client = node.client();
    }

    public SuggestionIndex(
            String indexID) {
        this.index = indexID;

        /*
         * Create the ES index if it does not exist yet
         */
        try {
            final boolean indexExists =
                    client.admin().indices()
                            .exists(new IndicesExistsRequest(index))
                            .actionGet().isExists();
            if (!indexExists) {
                createIndexIfNotExists();
                addMapping(TYPE);
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a list of SuggestionFields (terms) to the DB within one single bulk
     * request and refreshes the index afterwards.
     * If the ID of a term is null, the output string will be used.
     * 
     * @throws IOException
     */
    public void addTerms(final List<SuggestionField> terms) throws IOException {
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        for (SuggestionField field : terms) {
            bulkRequest.add(client.prepareIndex(index, TYPE,
                    field.ID == null ? field.output : field.ID).setSource(
                    generateFieldJS(field.input, field.output, field.payload,
                            field.weight)));
        }
        BulkResponse bulkResponse =
                bulkRequest.setRefresh(true).execute().actionGet();

        if (bulkResponse.hasFailures()) {
            for (BulkItemResponse item : bulkResponse.getItems()) {
                System.err.println(item.getFailureMessage());
            }
        }
    }

    /**
     * Adds a single term (SuggestionField) to the DB and refreshes the index.
     * 
     * @param inputs
     *            The strings used to build the suggestion index
     * @param output
     *            The String to be returned by a complete request if some of the
     *            inputs are matching. If this element is NULL the matching
     *            input string will be used instead. This string will also be
     *            used to define the ID of the new field
     * @param payload
     *            The payload (e.g. images) stored with the field
     * @param weight
     *            The weight of the term
     * 
     * @throws IOException
     */
    public void addSingleTerm(
            final String ID,
            final List<String> inputs,
            final String output,
            final String payload,
            final int weight) throws IOException {
        client.prepareIndex(index, TYPE, ID == null ? output : ID)
                .setSource(generateFieldJS(inputs, output, payload, weight))
                .setRefresh(true).execute().actionGet();
    }

    /**
     * Deletes a single term (SuggestionField) and refreshes the index.
     * 
     * @param ID
     *            ID of the field to be deleted
     * @return <true> in case the element has been found and deleted
     * @throws IOException
     */
    public boolean deleteSingleTerm(final String ID) throws IOException {
        System.out.println("Deleting " + ID);
        DeleteResponse response =
                client.prepareDelete(index, TYPE, ID).setRefresh(true)
                        .execute().actionGet();
        return response.isFound();
    }

    /**
     * Creates a jason node for a suggestion field
     * 
     * @see SuggestionIndex.addSingleTerm
     * @return A new XContentBuilder with the field created
     * @throws IOException
     */
    private XContentBuilder generateFieldJS(
            final List<String> inputs,
            final String output,
            final String payload,
            final int weight) throws IOException {
        String name = output;
        if (name == null) {
            if (inputs.size() == 0) {
                return null;
            }
            name = inputs.get(0);
        }
        XContentBuilder b =
                jsonBuilder().startObject().field(NAME_FIELD, name)
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
     *            String to be completed
     * @param size
     *            Maximum number of suggestion strings
     * @return
     */
    public void findSuggestionsFor(
            final String suggestRequest,
            final int size,
            final ASuggestionsRetrievedListener listener) {

        CompletionSuggestionBuilder compBuilder =
                new CompletionSuggestionBuilder(SUGGEST_FIELD).field(
                        SUGGEST_FIELD).text(suggestRequest);

        SuggestRequestBuilder suggestRequestBuilder =
                client.prepareSuggest(index).addSuggestion(compBuilder);

        ListenableActionFuture<SuggestResponse> future =
                suggestRequestBuilder.execute();

        /*
         * Trigger listener.suggestionsRetrieved as soon as we've received the
         * answer from ES
         */
        future.addListener(new ActionListener<SuggestResponse>() {

            public void onResponse(SuggestResponse response) {
                List<Suggestion> suggestionStrings =
                        new ArrayList<Suggestion>(size);
                CompletionSuggestion compSuggestion =
                        response.getSuggest().getSuggestion(SUGGEST_FIELD);

                List<CompletionSuggestion.Entry> entryList =
                        compSuggestion.getEntries();
                if (entryList != null) {
                    // We request only 1 completion -> get(0)
                    CompletionSuggestion.Entry entry = entryList.get(0);
                    List<CompletionSuggestion.Entry.Option> options =
                            entry.getOptions();
                    /*
                     * Loop through all suggestions
                     */
                    boolean first = true;
                    for (CompletionSuggestion.Entry.Option option : options) {
                        suggestionStrings.add(new Suggestion(option.getText()
                                .toString(), option.getPayloadAsString()));
                        if (first) {
                            try {
                                deleteSingleTerm(option.getText().toString());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            first = false;
                        }
                    }
                }
                listener.suggestionsRetrieved(suggestionStrings);
            }

            public void onFailure(Throwable e) {
                // TODO To be implemented
            }
        });
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
    public void addMapping(String type) throws IOException {
        XContentBuilder js =
                jsonBuilder().startObject().startObject(type)
                        .startObject("properties").startObject("name")
                        .field("type", "string").endObject()
                        .startObject(SUGGEST_FIELD).field("type", "completion")
                        .field("index_analyzer", "simple")
                        .field("search_analyzer", "simple")
                        .field("payloads", true).endObject().endObject()
                        .endObject().endObject();

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
    private void createIndexIfNotExists() throws ExecutionException,
            InterruptedException {
        try {
            CreateIndexResponse response =
                    client.admin().indices()
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
    public void delete() throws InterruptedException, ExecutionException {

        final DeleteIndexRequest deleteIndexRequest =
                new DeleteIndexRequest(index);
        try {
            client.admin().indices().delete(deleteIndexRequest).get();
        } catch (ExecutionException e) {
            // Ignore IndexMissingException
            if (!((e.getCause().getCause()) instanceof IndexMissingException)) {
                throw e;
            }
        }
    }

    /**
     * Empties the index (all fields are deleted)
     * 
     * @param index
     *            The index to be deleted
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws IOException
     */
    public void truncate() throws InterruptedException, ExecutionException,
            IOException {
        delete();
        waitForGreen();
        createIndexIfNotExists();
        addMapping(TYPE);
    }

    private void waitForYellow() {
        client.admin().cluster().prepareHealth().setIndices(index);
    }

    private void waitForGreen() {
        client.admin().cluster().prepareHealth().setIndices(index)
                .setWaitForGreenStatus().execute().actionGet();
    }
}
