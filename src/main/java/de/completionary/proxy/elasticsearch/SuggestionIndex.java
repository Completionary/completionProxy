package de.completionary.proxy.elasticsearch;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.completionary.proxy.analytics.AStatisticsAggregator;
import de.completionary.proxy.analytics.StatisticsAggregator_RrdDb;
import de.completionary.proxy.helper.ProxyOptions;
import de.completionary.proxy.thrift.services.admin.SuggestionField;
import de.completionary.proxy.thrift.services.exceptions.IndexUnknownException;
import de.completionary.proxy.thrift.services.exceptions.InvalidIndexNameException;
import de.completionary.proxy.thrift.services.exceptions.ServerDownException;
import de.completionary.proxy.thrift.services.suggestion.AnalyticsData;
import de.completionary.proxy.thrift.services.suggestion.Suggestion;

public class SuggestionIndex {

    final static Logger logger = LoggerFactory.getLogger(SuggestionIndex.class);

    /*
     * The es index identifying this suggestion index
     */
    private final String index;

    private final String payloadIndex;

    private final AStatisticsAggregator statisticsAggregator;

    private static Client esClient = null;

    /*
     * Constants
     */
    private static final String NAME_FIELD = "name";

    private static final String SUGGEST_FIELD = "suggest";

    private static final String PAYLOAD_FIELD = "p";

    private static final String TYPE = "t";

    private static final String index_and_search_analyzer = "whitespace";

    private static Map<String, SuggestionIndex> indices =
            new TreeMap<String, SuggestionIndex>();

    static {
        boolean isClient = true;

        final Node node =
                nodeBuilder().clusterName("completionaryCluster")
                        .client(isClient).node();
        esClient = node.client();

        // Settings settings = ImmutableSettings.settingsBuilder()
        // .put("cluster.name", "completionaryCluster").build();
        //
        // esClient = new TransportClient(settings)
        // .addTransportAddress(new
        // InetSocketTransportAddress("metalcon2.physik.uni-mainz.de", 9300));

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                System.out.println("Shutting down ES client");
                // node.close();
                esClient.close();
            }
        });
    }

    /**
     * Factory methode to get SuggestionIndex objects
     * 
     * @param index
     *            The ID of the index
     * @return A new Index or null in case of an error
     */
    public static SuggestionIndex getIndex(final String index)
            throws IndexUnknownException, InvalidIndexNameException,
            ServerDownException {
        if (index.equals("")) {
            throw new InvalidIndexNameException("The index must not be empty");
        }
        SuggestionIndex instance = indices.get(index);
        if (instance != null) {
            return instance;
        }

        if (indexExists(index)) {
            return generateInstance(index);
        }

        throw new IndexUnknownException("Index " + index + " does not exist");
    }

    /**
     * Factory method to generate a non existing index
     * 
     * @param index
     *            The ID of the index to be generated
     * @return the newly generated index
     */
    public static
        SuggestionIndex
        generateIndex(final String index)
                throws InvalidIndexNameException,
                ServerDownException,
                de.completionary.proxy.thrift.services.exceptions.IndexAlreadyExistsException {
        checkIndexValidity(index);

        /*
         * Check if the index already exists in the cache or at least in the DB
         */
        SuggestionIndex instance = indices.get(index);
        if (instance == null && !indexExists(index)) {
            /*
             * Generate a new Instance only if the index did not already exists
             */
            return generateInstance(index);
        }

        throw new de.completionary.proxy.thrift.services.exceptions.IndexAlreadyExistsException(
                "The index " + index + " already exists");
    }

    /**
     * Generates a new Index instance and initializes the DB if the index did
     * not exists yet
     * 
     * @param index
     *            The ID of the index to be generated
     * @return The generated index
     */
    private static SuggestionIndex generateInstance(final String index)
            throws ServerDownException {

        synchronized (esClient) {
            try {
                SuggestionIndex instance = new SuggestionIndex(index);
                indices.put(index, instance);
                return instance;
            } catch (ExecutionException | InterruptedException | IOException e) {
                throw new ServerDownException(e.getMessage());
            }
        }
    }

    /**
     * Constructor initializing the DB if the index does not yet exists
     * 
     * @param indexID
     *            The ID of the index
     */
    private SuggestionIndex(
            String indexID) throws ExecutionException, InterruptedException,
            IOException {
        this.index = indexID;
        this.payloadIndex = generatePayloadIndex(indexID);

        /*
         * Create the ES index if it does not exist yet
         */
        if (!indexExists(indexID)) {
            createIndexIfNotExists();
            addMapping(TYPE);
        }
        if (ProxyOptions.ENABLE_LOGGIN) {
            statisticsAggregator =
                    new StatisticsAggregator_RrdDb(indexID, getIndexSize(), /*
                                                                             * TODO:
                                                                             * get
                                                                             * queriesThisMonth
                                                                             */
                    0);
        } else {
            statisticsAggregator = null;
        }
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
    public void async_addTerms(
            final List<SuggestionField> terms,
            final AsyncMethodCallback<Long> listener) throws IOException {
        async_addTerms(terms, 0, 0, listener);
    }

    /**
     * This is a recursive method limiting the maximum number of add requests
     * within one bulkRequest
     * 
     * @param terms
     * @param startPos
     * @param timeMillis
     * @param listener
     * @throws IOException
     */
    public void async_addTerms(
            final List<SuggestionField> terms,
            final int startPos,
            final long timeMillis,
            final AsyncMethodCallback<Long> listener) throws IOException {
        logger.info("[" + index + "] Bulk import: " + terms.size()
                + " terms, starting with " + startPos);

        final int endPos =
                Math.min(terms.size(), startPos
                        + ProxyOptions.SPLIT_BULK_REQUEST_SIZE);

        BulkRequestBuilder bulkRequest = esClient.prepareBulk();

        for (int i = startPos; i != endPos; i++) {
            SuggestionField field = terms.get(i);

            bulkRequest.add(esClient.prepareIndex(index, TYPE,
                    Long.toString(field.ID)).setSource(
                    generateFieldJS(field.input, field.outputField, field.ID,
                            field.weight)));

            bulkRequest.add(esClient.prepareIndex(payloadIndex, TYPE,
                    Long.toString(field.ID)).setSource(
                    jsonBuilder().startObject()
                            .field(PAYLOAD_FIELD, field.payload).endObject()));
        }

        ListenableActionFuture<BulkResponse> future =
                bulkRequest.setRefresh(true).execute();

        if (endPos != terms.size()) {
            /*
             * Start recursive call of this method
             */
            future.addListener(new ActionListener<BulkResponse>() {

                public void onResponse(BulkResponse response) {
                    try {
                        async_addTerms(terms, endPos,
                                timeMillis + response.getTookInMillis(),
                                listener);
                    } catch (IOException e) {
                        listener.onError(new Exception(e.getMessage()));
                    }
                }

                public void onFailure(Throwable e) {
                    logger.error("[" + index + "] Bulk import error: "
                            + e.getMessage());
                    listener.onError(new Exception(e.getMessage()));
                    if (statisticsAggregator != null) {
                        statisticsAggregator.onTermsAdded(getIndexSize());
                    }
                }
            });
        } else {
            future.addListener(new ActionListener<BulkResponse>() {

                public void onResponse(BulkResponse response) {
                    logger.info("[" + index + "] Finished Bulk import: "
                            + terms.size() + " terms");
                    listener.onComplete(timeMillis + response.getTookInMillis());
                    if (statisticsAggregator != null) {
                        statisticsAggregator.onTermsAdded(getIndexSize());
                    }
                }

                public void onFailure(Throwable e) {
                    logger.error("[" + index + "] Bulk import error: "
                            + e.getMessage());
                    listener.onError(new Exception(e.getMessage()));
                    if (statisticsAggregator != null) {
                        statisticsAggregator.onTermsAdded(getIndexSize());
                    }
                }
            });
        }
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
            final long ID,
            final List<String> inputs,
            final String output,
            final String payload,
            final int weight,
            final AsyncMethodCallback<Long> listener) throws IOException {

        BulkRequestBuilder bulkRequest = esClient.prepareBulk();
        bulkRequest.add(esClient.prepareIndex(index, TYPE, Long.toString(ID))
                .setSource(generateFieldJS(inputs, output, ID, weight)));

        bulkRequest.add(esClient.prepareIndex(payloadIndex, TYPE,
                Long.toString(ID)).setSource(
                jsonBuilder().startObject().field(PAYLOAD_FIELD, payload)
                        .endObject()));

        ListenableActionFuture<BulkResponse> future =
                bulkRequest.setRefresh(true).execute();

        future.addListener(new ActionListener<BulkResponse>() {

            public void onResponse(BulkResponse response) {
                listener.onComplete(response.getTookInMillis());
                if (statisticsAggregator != null) {
                    statisticsAggregator.onTermAdded(getIndexSize());
                }
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
    public void async_deleteSingleTerm(
            final long ID,
            final AsyncMethodCallback<Boolean> listener) throws IOException {

        ListenableActionFuture<DeleteResponse> future =
                esClient.prepareDelete(index, TYPE, Long.toString(ID))
                        .setRefresh(true).execute();

        future.addListener(new ActionListener<DeleteResponse>() {

            public void onResponse(DeleteResponse response) {
                listener.onComplete(response.isFound());
                if (response.isFound()) {
                    if (statisticsAggregator != null) {
                        statisticsAggregator.onTermDeleted();
                    }
                }
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
    public void async_deleteTerms(
            final List<Long> IDs,
            final AsyncMethodCallback<Long> listener) throws IOException {

        BulkRequestBuilder bulkRequest = esClient.prepareBulk();
        for (Long ID : IDs) {
            bulkRequest.add(esClient.prepareDelete(index, TYPE,
                    Long.toString(ID)));
        }

        ListenableActionFuture<BulkResponse> future =
                bulkRequest.setRefresh(true).execute();

        future.addListener(new ActionListener<BulkResponse>() {

            public void onResponse(BulkResponse response) {
                listener.onComplete(response.getTookInMillis());
                if (statisticsAggregator != null) {
                    statisticsAggregator.onTermsDeleted(getIndexSize());
                }
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
            final long payloadID,
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
        b.field("payload", payloadID).field("weight", weight).endObject()
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
            final AnalyticsData userData,
            final AsyncMethodCallback<List<Suggestion>> listener) {

        CompletionSuggestionBuilder compBuilder =
                new CompletionSuggestionBuilder(SUGGEST_FIELD).field(
                        SUGGEST_FIELD).text(suggestRequest);

        ListenableActionFuture<SuggestResponse> future =
                esClient.prepareSuggest(index).addSuggestion(compBuilder)
                        .execute();

        /*
         * Async AS request finding suggestions
         */
        future.addListener(new ActionListener<SuggestResponse>() {

            public void onResponse(SuggestResponse response) {
                /*
                 * Async requests getting payload data
                 */
                generateSuggestionsFromESResponse(response, size,
                        new AsyncMethodCallback<List<Suggestion>>() {

                            @Override
                            public void
                                onComplete(List<Suggestion> suggestions) {
                                listener.onComplete(suggestions);
                                if (statisticsAggregator != null) {
                                    statisticsAggregator.onQuery(userData,
                                            suggestRequest, suggestions);
                                }
                            }

                            @Override
                            public void onError(Exception e) {
                                listener.onError(e);
                            }
                        });
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
            final int size,
            final AnalyticsData userData) throws ServerDownException {

        CompletionSuggestionBuilder compBuilder =
                new CompletionSuggestionBuilder(SUGGEST_FIELD).field(
                        SUGGEST_FIELD).text(suggestRequest);

        SuggestRequestBuilder suggestRequestBuilder =
                esClient.prepareSuggest(index).addSuggestion(compBuilder);

        SuggestResponse response = suggestRequestBuilder.execute().actionGet();

        /*
         * Call the async function generateSuggestionsFromESResponse and
         * synchronize it
         */
        final CountDownLatch lock = new CountDownLatch(1);

        final List<Suggestion> suggestions = new ArrayList<Suggestion>();
        generateSuggestionsFromESResponse(response, size,
                new AsyncMethodCallback<List<Suggestion>>() {

                    @Override
                    public void onComplete(List<Suggestion> s) {
                        suggestions.addAll(s);
                        lock.countDown();
                    }

                    @Override
                    public void onError(Exception arg0) {
                        arg0.printStackTrace();
                        lock.countDown();
                    }
                });

        try {
            if (!lock.await(2000, TimeUnit.MILLISECONDS)) {
                throw new ServerDownException(
                        "Timeout while retrieving payloads");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (statisticsAggregator != null) {
            statisticsAggregator.onQuery(userData, suggestRequest, suggestions);
        }
        return suggestions;
    }

    /**
     * Returns the payload of the suggestion defined by [suggestionID]
     * 
     * @param suggestionID
     *            The ID of the suggestion which payload is to be returned
     * @return The requested payload or [null] if the s does not exist
     */
    public void getPayload(
            long suggestionID,
            final AsyncMethodCallback<String> callback) {
        ListenableActionFuture<GetResponse> future =
                esClient.prepareGet(payloadIndex, TYPE,
                        Long.toString(suggestionID)).execute();

        future.addListener(new ActionListener<GetResponse>() {

            public void onResponse(GetResponse response) {
                /*
                 * FIXME: This is so uggly! Isn't there a way to store a string
                 * as it is and not within a json... As an alternative we should
                 * at least tune this with something like
                 * response.getSourceAsString().substring(X).substring(0,Y);
                 */
                callback.onComplete((String) response.getSourceAsMap().get(
                        PAYLOAD_FIELD));
            }

            public void onFailure(Throwable e) {
                callback.onError(new ServerDownException(e.getMessage()));
            }
        });
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
    private void generateSuggestionsFromESResponse(
            final SuggestResponse response,
            final int size,
            final AsyncMethodCallback<List<Suggestion>> callback) {

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

                if (!options.isEmpty()) {
                    /*
                     * Loop through all suggestions
                     */
                    // List<Suggestion> suggestions = new ArrayList<Suggestion>(
                    // options.size());
                    final Suggestion[] suggestions =
                            new Suggestion[options.size()];

                    /*
                     * Used to count how many async requests have already been
                     * responded
                     */
                    final AtomicInteger remainingRequests =
                            new AtomicInteger(suggestions.length);

                    for (int i = 0; i != suggestions.length; i++) {
                        final int suggestionNumber = i;
                        CompletionSuggestion.Entry.Option option =
                                options.get(i);
                        suggestions[suggestionNumber] =
                                new Suggestion(option.getText().toString(),
                                        ""/*
                                           * empty payload for now
                                           */, option.getPayloadAsLong());

                        getPayload(option.getPayloadAsLong(),
                                new AsyncMethodCallback<String>() {

                                    @Override
                                    public void onError(Exception e) {
                                        if (remainingRequests.decrementAndGet() == 0) {
                                            callback.onError(e);
                                        }
                                    }

                                    @Override
                                    public void onComplete(String payload) {
                                        suggestions[suggestionNumber].payload =
                                                payload;
                                        if (remainingRequests.decrementAndGet() == 0) {
                                            // the last request is finished
                                            callback.onComplete(Arrays
                                                    .asList(suggestions));
                                        }
                                    }
                                });
                    }
                } else {
                    callback.onComplete(new ArrayList<Suggestion>(0));
                }
            } else { // entryList == null
                callback.onComplete(new ArrayList<Suggestion>(0));
            }
        } else { // compSuggestion == null
            callback.onComplete(new ArrayList<Suggestion>(0));
        }
    }

    /**
     * Stores a new mapping. A json similar to following is generated: {"type" :
     * { "properties" : { "name" : { "type" : "string" }, "suggest" : { "type" :
     * "completion", "index_analyzer" : "simple", "search_analyzer" : "simple",
     * "payloads" : true }}}}
     * 
     * @param randomIndex
     *            ES index the new mapping should be added to
     * @param type
     *            The type of the mapping
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private void addMapping(String type) throws IOException,
            InterruptedException, ExecutionException {
        XContentBuilder js =
                jsonBuilder().startObject().startObject(type)
                        .startObject("properties").startObject("name")
                        .field("type", "string").endObject()
                        .startObject(SUGGEST_FIELD).field("type", "completion")
                        .field("index_analyzer", index_and_search_analyzer)
                        .field("search_analyzer", index_and_search_analyzer)
                        .field("payloads", true).endObject().endObject()
                        .endObject().endObject();

        esClient.admin().indices().preparePutMapping(index).setType(type)
                .setSource(js).get();
    }

    private void async_addMapping(
            String type,
            final AsyncMethodCallback<Boolean> listener) throws IOException,
            InterruptedException, ExecutionException {

        XContentBuilder js =
                jsonBuilder().startObject().startObject(type)
                        .startObject("properties").startObject("name")
                        .field("type", "string").endObject()
                        .startObject(SUGGEST_FIELD).field("type", "completion")
                        .field("index_analyzer", "simple")
                        .field("search_analyzer", "simple")
                        .field("payloads", true).endObject().endObject()
                        .endObject().endObject();

        ListenableActionFuture<PutMappingResponse> future =
                esClient.admin().indices().preparePutMapping(index)
                        .setType(type).setSource(js).execute();

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
     * @param randomIndex
     *            The index to be created
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private void createIndexIfNotExists() throws ExecutionException,
            InterruptedException {
        try {
            CreateIndexResponse response =
                    esClient.admin().indices()
                            .create(new CreateIndexRequest(index)).get();

            if (!response.isAcknowledged()) {
                System.err.println("Unable to create index " + index);
                return;
            }

            response =
                    esClient.admin().indices()
                            .create(new CreateIndexRequest(payloadIndex)).get();
            if (!response.isAcknowledged()) {
                System.err.println("Unable to create index " + payloadIndex);
                return;
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
        try {
            esClient.admin().indices().delete(new DeleteIndexRequest(index))
                    .get();

            esClient.admin()
                    .indices()
                    .delete(new DeleteIndexRequest(generatePayloadIndex(index)))
                    .get();
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

        ListenableActionFuture<DeleteMappingResponse> future =
                esClient.admin().indices().prepareDeleteMapping(index)
                        .setType(TYPE).execute();

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
        ListenableActionFuture<ClusterHealthResponse> future =
                esClient.admin().cluster().prepareHealth().setIndices(index)
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
     * Returns the number of terms stored in the index
     * 
     * @return The number of terms stored in the index
     */
    public long getIndexSize() {
        CountResponse countResponse =
                esClient.prepareCount(index).setTypes(TYPE).execute()
                        .actionGet();

        return countResponse.getCount();
    }

    public void waitForYellow() {
        esClient.admin().cluster().prepareHealth().setIndices(index).execute()
                .actionGet();
    }

    public void waitForGreen() {
        esClient.admin().cluster().prepareHealth().setIndices(index)
                .setWaitForGreenStatus().execute().actionGet();
    }

    public void optimize() {
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
    private static void checkIndexValidity(String index)
            throws InvalidIndexNameException {
        if (index.equals("")) {
            throw new InvalidIndexNameException("An index must not be empty");
        }

        if (!index.toLowerCase().equals(index)) {
            throw new InvalidIndexNameException("An index must be lowercase");
        }

        Pattern pattern = Pattern.compile("\\s");
        Matcher matcher = pattern.matcher(index);
        if (matcher.find()) {
            throw new InvalidIndexNameException(
                    "An index may not contain any whitespace character");
        }
    }

    /**
     * Must be called every time a search session is finished (suggestion is
     * selected, timeout or query is deleted)
     */
    public void onSearchSessionFinished(AnalyticsData userData) {
        if (statisticsAggregator != null) {
            statisticsAggregator.onSearchSessionFinished(userData);
        }
    }

    /**
     * Must be called every time a suggestion was selected
     */
    public void onSuggestionSelected(
            String suggestionID,
            String suggestionString,
            AnalyticsData userData) {
        if (statisticsAggregator != null) {
            statisticsAggregator.onSuggestionSelected(suggestionID,
                    suggestionString, userData);
        }
    }

    /**
     * Returns the StatisticsAggregator to access all kind of analytics data
     * 
     * @return
     */
    public AStatisticsAggregator getStatistics() {
        return statisticsAggregator;
    }

    public static String generatePayloadIndex(String index) {
        return index + "-payload";
    }
}
