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
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.optimize.OptimizeRequest;
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

import de.completionary.proxy.thrift.services.Suggestion;
import de.completionary.proxy.thrift.services.SuggestionField;

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

    private static Map<String, SuggestionIndex> indices =
            new TreeMap<String, SuggestionIndex>();

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

    /**
     * Factory methode to create SuggestionIndex objects
     * 
     * @param index
     * @return
     */
    public static SuggestionIndex getIndex(final String index) {
        SuggestionIndex instance = indices.get(index);
        if (instance == null) {
            instance = new SuggestionIndex(index);
            indices.put(index, instance);
        }
        return instance;
    }

    private SuggestionIndex(
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
     * @param terms
     *            The suggestion fields to be added
     * @param listener
     *            Callback that will be passed the number of milliseconds the
     *            async ES call took
     * @throws IOException
     */
    public void async_addTerms(
            final List<SuggestionField> terms,
            final AsyncMethodCallback<Long> listener) throws IOException {

        BulkRequestBuilder bulkRequest = client.prepareBulk();
        for (SuggestionField field : terms) {
            bulkRequest.add(client.prepareIndex(index, TYPE, field.ID)
                    .setSource(
                            generateFieldJS(field.input, field.output,
                                    field.payload, field.weight)));
        }

        ListenableActionFuture<BulkResponse> future =
                bulkRequest.setRefresh(true).execute();

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
    public void async_addSingleTerm(
            final String ID,
            final List<String> inputs,
            final String output,
            final String payload,
            final int weight,
            final AsyncMethodCallback<Long> listener) throws IOException {
        client.prepareIndex(index, TYPE, ID)
                .setSource(generateFieldJS(inputs, output, payload, weight))
                .setRefresh(true).execute().actionGet();

        ListenableActionFuture<IndexResponse> future =
                client.prepareIndex(index, TYPE, ID)
                        .setSource(
                                generateFieldJS(inputs, output, payload, weight))
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
     * @return <true> in case the element has been found and deleted
     * @throws IOException
     */
    public void async_deleteSingleTerm(
            final String ID,
            final AsyncMethodCallback<Boolean> listener) throws IOException {

        ListenableActionFuture<DeleteResponse> future =
                client.prepareDelete(index, TYPE, ID).setRefresh(true)
                        .execute();

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
     *            The string to be completed
     * @param size
     *            The maximum number of suggestions to be returned
     * 
     * @param listener
     *            Callback to be called as soon as the completion result has
     *            arrived
     */
    public void async_findSuggestionsFor(
            final String suggestRequest,
            final int size,
            final AsyncMethodCallback<List<Suggestion>> listener) {

        CompletionSuggestionBuilder compBuilder =
                new CompletionSuggestionBuilder(SUGGEST_FIELD).field(
                        SUGGEST_FIELD).text(suggestRequest);

        SuggestRequestBuilder suggestRequestBuilder =
                client.prepareSuggest(index).addSuggestion(compBuilder);

        ListenableActionFuture<SuggestResponse> future =
                suggestRequestBuilder.execute();

        future.addListener(new ActionListener<SuggestResponse>() {
            public void onResponse(SuggestResponse response) {

                listener.onComplete(generateSuggestionsFromESRespone(response,
                        size));
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
    public List<Suggestion> findSuggestionsFor(
            final String suggestRequest,
            final int size) {

        CompletionSuggestionBuilder compBuilder =
                new CompletionSuggestionBuilder(SUGGEST_FIELD).field(
                        SUGGEST_FIELD).text(suggestRequest);

        SuggestRequestBuilder suggestRequestBuilder =
                client.prepareSuggest(index).addSuggestion(compBuilder);

        SuggestResponse response = suggestRequestBuilder.execute().actionGet();

        return generateSuggestionsFromESRespone(response, size);
    }

    /**
     * 
     * @param response
     * @return
     */
    private List<Suggestion> generateSuggestionsFromESRespone(
            final SuggestResponse response,
            final int size) {
        List<Suggestion> suggestions = new ArrayList<Suggestion>(size);

        CompletionSuggestion compSuggestion =
                response.getSuggest().getSuggestion(SUGGEST_FIELD);

        if (compSuggestion != null) {

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
                for (CompletionSuggestion.Entry.Option option : options) {
                    suggestions.add(new Suggestion(option.getText().toString(),
                            option.getPayloadAsString()));
                }
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
    private void addMapping(String type) throws IOException {
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
        client.admin().indices().prepareDeleteMapping(index).setType(TYPE)
                .execute().actionGet();
        waitForYellow();
        addMapping(TYPE);
    }

    public void waitForYellow() {
        client.admin().cluster().prepareHealth().setIndices(index);
    }

    public void waitForGreen() {
        //        client.admin().cluster().prepareHealth().setIndices(index)
        //                .setWaitForGreenStatus().execute().actionGet();
        client.admin().indices().optimize(new OptimizeRequest(index))
                .actionGet();
    }

    private void expungeDeletes() {
        client.admin().indices().prepareOptimize(index)
                .setOnlyExpungeDeletes(true).setWaitForMerge(true).execute()
                .actionGet();
    }
}
