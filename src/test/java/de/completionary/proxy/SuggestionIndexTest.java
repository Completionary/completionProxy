/**
 * 
 */
package de.completionary.proxy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.apache.thrift.async.AsyncMethodCallback;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.completionary.proxy.analytics.LoggingHandler;
import de.completionary.proxy.elasticsearch.SuggestionIndex;
import de.completionary.proxy.helper.Options;
import de.completionary.proxy.helper.ProxyOptions;
import de.completionary.proxy.thrift.services.exceptions.IndexAlreadyExistsException;
import de.completionary.proxy.thrift.services.exceptions.InvalidIndexNameException;
import de.completionary.proxy.thrift.services.exceptions.ServerDownException;
import de.completionary.proxy.thrift.services.suggestion.AnalyticsData;
import de.completionary.proxy.thrift.services.suggestion.Suggestion;

/**
 * @author kunzejo
 * 
 */
public class SuggestionIndexTest {

    static protected CountDownLatch lock = new CountDownLatch(1);

    private static List<String> randomIndices = new ArrayList<String>();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ProxyOptions.ENABLE_LOGGIN = false;
    }

    public static SuggestionIndex getRandomIndex()
            throws InvalidIndexNameException, ServerDownException {
        Random r = new Random();
        String index = "testindex" + r.nextInt();
        randomIndices.add(index);
        try {
            return SuggestionIndex.generateIndex(index);
        } catch (IndexAlreadyExistsException e) {
            e.printStackTrace();
        }
        return null;
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        for (String index : randomIndices) {
            SuggestionIndex.delete(index);
        }
    }

    @SuppressWarnings("unused")
    @Test
    public void TestBadIndexName() throws ServerDownException,
            IndexAlreadyExistsException {
        try {
            // Test with an empty string
            SuggestionIndex client = SuggestionIndex.generateIndex("");
            Assert.fail();
        } catch (InvalidIndexNameException e) {
        }

        try {
            SuggestionIndex client =
                    SuggestionIndex.generateIndex("white space");
            Assert.fail();
        } catch (InvalidIndexNameException e) {
        }

        try {
            SuggestionIndex client = SuggestionIndex.generateIndex("UpperCase");
            Assert.fail();
        } catch (InvalidIndexNameException e) {
        }
    }

    @Test
    public void Test() throws InterruptedException, ExecutionException,
            IOException, InvalidIndexNameException, ServerDownException {
        SuggestionIndex client = getRandomIndex();

        addTerm(client, 1, "output", Arrays.asList(new String[] {
            "blub"
        }), "payload");
        /*
         * Find that term
         */
        checkIfEntryIsFound(client, "b", "output", "payload", 200);

        /*
         * Delete The term again
         */
        lock = new CountDownLatch(1);

        client.async_deleteSingleTerm(1, new AsyncMethodCallback<Boolean>() {

            public void onError(Exception e) {
                e.printStackTrace();
                Assert.fail("An Error has occured (see above)");
                lock.countDown();
            }

            public void onComplete(Boolean b) {
                Assert.assertTrue(b);
                lock.countDown();
            }
        });

        Assert.assertTrue("async_deleteSingleTerm has timed out",
                lock.await(200, TimeUnit.MILLISECONDS));

        /*
         * Check if it's deleted
         */
        deleteTerm(client, 1, 200000);
        checkIfEntryIsDeleted(client, "b");
    }

    static void addTerm(
            SuggestionIndex client,
            long ID,
            String term,
            String payload) throws IOException, InterruptedException {
        addTerm(client, ID, term, Arrays.asList(new String[] {
            term
        }), payload);
    }

    static void addTerm(
            SuggestionIndex client,
            long ID,
            String output,
            List<String> inputs,
            String payload) throws IOException, InterruptedException {
        /*
         * Add a term
         */
        client.async_addSingleTerm(1, inputs, output, payload, 1,
                new AsyncMethodCallback<Long>() {

                    public void onError(Exception e) {
                        e.printStackTrace();
                        Assert.fail("An Error has occured (see above)");
                        lock.countDown();
                    }

                    public void onComplete(Long arg0) {
                        lock.countDown();
                    }
                });

        Assert.assertTrue("async_addSingleTerm has timed out",
                lock.await(20000, TimeUnit.MILLISECONDS));

    }

    static void deleteTerm(
            SuggestionIndex client,
            long ID,
            int timeOutMilliSeconds) throws IOException, InterruptedException {
        /*
         * Delete The term again
         */
        lock = new CountDownLatch(1);

        final boolean[] foundElemnt = new boolean[1];

        client.async_deleteSingleTerm(ID, new AsyncMethodCallback<Boolean>() {

            public void onError(Exception e) {
                e.printStackTrace();
                Assert.fail("An Error has occured (see above)");
                foundElemnt[0] = false;
                lock.countDown();
            }

            public void onComplete(Boolean b) {
                foundElemnt[0] = b;
                lock.countDown();
            }
        });

        Assert.assertTrue("async_deleteSingleTerm has timed out",
                lock.await(timeOutMilliSeconds, TimeUnit.MILLISECONDS));

        Assert.assertTrue(foundElemnt[0]);
    }

    static void checkIfEntryIsDeleted(SuggestionIndex client, String query)
            throws InterruptedException {
        final CountDownLatch lock = new CountDownLatch(1);

        final List<Suggestion> results = new ArrayList<Suggestion>();
        client.async_findSuggestionsFor(query, 10, new AnalyticsData(),
                new AsyncMethodCallback<List<Suggestion>>() {

                    public void onError(Exception e) {
                        e.printStackTrace();
                        Assert.fail("An Error has occured (see above)");
                        lock.countDown();

                    }

                    public void onComplete(List<Suggestion> suggestions) {
                        results.addAll(suggestions);
                        lock.countDown();
                    }
                });
        Assert.assertTrue("async_findSuggestionsFor has timed out",
                lock.await(200, TimeUnit.MILLISECONDS));
        Assert.assertEquals(0, results.size());
    }

    static List<Suggestion> findSuggestionFor(
            final SuggestionIndex client,
            String query,
            int timeOutMilliSeconds) throws InterruptedException {

        final CountDownLatch lock = new CountDownLatch(1);

        final List<Suggestion> results = new ArrayList<Suggestion>();
        client.async_findSuggestionsFor(query, 10, new AnalyticsData(),
                new AsyncMethodCallback<List<Suggestion>>() {

                    public void onError(Exception e) {
                        Assert.fail(e.getLocalizedMessage());
                        lock.countDown();
                    }

                    public void onComplete(List<Suggestion> suggestions) {
                        Assert.assertNotNull("An Error has occured",
                                suggestions);
                        results.addAll(suggestions);
                        lock.countDown();
                    }
                });
        Assert.assertTrue("async_findSuggestionsFor has timed out",
                lock.await(timeOutMilliSeconds, TimeUnit.MILLISECONDS));
        return results;
    }

    /**
     * Sends a standard completion query and checks if the response contains the
     * given output and payload
     */
    static void checkIfEntryIsFound(
            SuggestionIndex client,
            String query,
            String output,
            String payload,
            int timeOutMilliSeconds) throws InterruptedException {

        final List<Suggestion> results =
                findSuggestionFor(client, query, timeOutMilliSeconds);

        if (results.size() > 1) {
            String message =
                    "Searching for " + query + " Returned multiple results:";
            for (Suggestion s : results) {
                message += s.suggestion + ":" + s.payload;
            }
            Assert.fail(message);
        }

        /*
         * Check if we find what we've stored
         */
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(output, results.get(0).suggestion);
        Assert.assertEquals(payload, results.get(0).payload);
    }
}
