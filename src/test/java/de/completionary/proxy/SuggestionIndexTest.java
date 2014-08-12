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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.completionary.proxy.elasticsearch.SuggestionIndex;
import de.completionary.proxy.thrift.services.exceptions.InvalidIndexNameException;
import de.completionary.proxy.thrift.services.exceptions.ServerDownException;
import de.completionary.proxy.thrift.services.suggestion.AnalyticsData;
import de.completionary.proxy.thrift.services.suggestion.Suggestion;

/**
 * @author kunzejo
 * 
 */
public class SuggestionIndexTest {

    protected String index = "";

    protected CountDownLatch lock = new CountDownLatch(1);

    @Before
    public void setUp() {
        Random r = new Random();
        index = "testindex" + r.nextInt();
    }

    @After
    public void tearDown() throws Exception {
        SuggestionIndex.delete(index);
    }

    @Test
    public void Test() throws InterruptedException, ExecutionException,
            IOException, InvalidIndexNameException, ServerDownException {
        SuggestionIndex client = SuggestionIndex.getIndex(index);

        /*
         * Add a term
         */
        client.async_addSingleTerm(1, Arrays.asList(new String[] {
            "bla", "blub"
        }), "output", "payload", 1, new AsyncMethodCallback<Long>() {

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
                lock.await(2000, TimeUnit.MILLISECONDS));

        /*
         * Find that term
         */
        checkIfEntryIsFound(client, "b", "output", "payload");

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
                lock.await(200000, TimeUnit.MILLISECONDS));

        /*
         * Check if it's deleted
         */
        checkIfEntryIsDeleted(client, "b");
    }

    void checkIfEntryIsDeleted(SuggestionIndex client, String query)
            throws InterruptedException {
        lock = new CountDownLatch(1);

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
                lock.await(2000, TimeUnit.MILLISECONDS));
        Assert.assertEquals(0, results.size());
    }

    /**
     * Sends a standard completion query and checks if the response contains the
     * given output and payload
     */
    void checkIfEntryIsFound(
            SuggestionIndex client,
            String query,
            String output,
            String payload) throws InterruptedException {
        lock = new CountDownLatch(1);

        final List<Suggestion> results = new ArrayList<Suggestion>();
        client.async_findSuggestionsFor(query, 10, new AnalyticsData(),
                new AsyncMethodCallback<List<Suggestion>>() {

                    public void onError(Exception e) {
                        e.printStackTrace();
                        Assert.fail("An Error has occured (see above)");
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
                lock.await(200, TimeUnit.MILLISECONDS));

        /*
         * Check if we find what we've stored
         */
        Assert.assertEquals(1, results.size());
        Assert.assertEquals(output, results.get(0).suggestion);
        Assert.assertEquals(payload, results.get(0).payload);
    }
}
