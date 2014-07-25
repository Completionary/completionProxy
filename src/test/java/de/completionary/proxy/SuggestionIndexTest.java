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

import de.completionary.proxy.elasticsearch.SuggestionIndex;
import de.completionary.proxy.thrift.services.Suggestion;

/**
 * @author kunzejo
 * 
 */
public class SuggestionIndexTest {

    private static String index = "";

    @BeforeClass
    public static void setUpBeforeClass() {
        Random r = new Random();
        index = "testindex" + r.nextInt();
        index = "testindex1978505353";
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        //SuggestionIndex.delete(index);
    }

    private CountDownLatch lock = new CountDownLatch(1);

    //    @Test
    //    public void speedTest() {
    //        SuggestionIndex client = new SuggestionIndex("index");
    //
    //        Random r = new Random();
    //
    //        final int numberOfQueries = 1000;
    //        for (int i = 0; i < 1000; i++) { // heat up cpu
    //            r.nextInt();
    //        }
    //
    //        final long randomStartTime = System.currentTimeMillis();
    //        for (int i = 0; i < numberOfQueries; i++) {
    //            String query = "" + (char) ('a' + Math.abs(r.nextInt()) % 25);
    //        }
    //        final long randomTime = (System.currentTimeMillis() - randomStartTime);
    //
    //        final float times[] = new float[numberOfQueries];
    //        final long totalTimeStart = System.currentTimeMillis();
    //        for (int i = 0; i < numberOfQueries; i++) {
    //            final int queryID = i;
    //            String query = "" + (char) ('a' + Math.abs(r.nextInt()) % 25);
    //            final long startTime = System.currentTimeMillis();
    //            client.findSuggestionsFor(query, 15,
    //                    new ASuggestionsRetrievedListener() {
    //
    //                        public void suggestionsRetrieved(
    //                                List<Suggestion> suggestions) {
    //                            float time =
    //                                    (System.currentTimeMillis() - startTime);
    //                            times[queryID] = time;
    //                        }
    //                    });
    //        }
    //
    //        while (times[numberOfQueries - 1] == 0.0) {
    //            try {
    //                Thread.sleep(1);
    //            } catch (InterruptedException e) {
    //                e.printStackTrace();
    //            }
    //        }
    //        float time =
    //                (System.currentTimeMillis() - totalTimeStart - randomTime)
    //                        * 1000 / (float) numberOfQueries;
    //        System.out.println("Average per query time: " + time + " µs");
    //        for (float f : times) {
    //            System.out.println(f);
    //        }
    //    }

    @Test
    public void Test() throws InterruptedException, ExecutionException,
            IOException {
        SuggestionIndex client = SuggestionIndex.getIndex(index);

        /*
         * Add a term
         */
        client.async_addSingleTerm("1", Arrays.asList(new String[] {
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
        client.waitForGreen();

        /*
         * Find that term
         */
        final List<Suggestion> results = new ArrayList<Suggestion>();
        client.async_findSuggestionsFor("b", 10,
                new AsyncMethodCallback<List<Suggestion>>() {

                    public void onError(Exception e) {
                        e.printStackTrace();
                        Assert.fail("An Error has occured (see above)");
                        lock.countDown();
                    }

                    public void onComplete(List<Suggestion> suggestions) {
                        Assert.assertNotNull("An Error has occured", results);
                        results.addAll(suggestions);
                        lock.countDown();
                    }
                });
        Assert.assertTrue("async_findSuggestionsFor has timed out",
                lock.await(2000, TimeUnit.MILLISECONDS));

        /*
         * Check if we find what we've stored
         */
        Assert.assertEquals(1, results.size());
        Assert.assertEquals("output", results.get(0).suggestion);
        Assert.assertEquals("payload", results.get(0).payload);

        /*
         * Check if we still find the term after deleting it
         */
        client.async_deleteSingleTerm("1", new AsyncMethodCallback<Boolean>() {

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
                lock.await(2000, TimeUnit.MILLISECONDS));

        results.clear();
        client.async_findSuggestionsFor("b", 10,
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
}
