/**
 * 
 */
package de.completionary.proxy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import junit.framework.Assert;

import org.junit.Test;

import de.completionary.proxy.elasticsearch.SuggestionIndex;
import de.completionary.proxy.server.ASuggestionsRetrievedListener;
import de.completionary.proxy.structs.Suggestion;

/**
 * @author kunzejo
 * 
 */
public class SuggestionIndexTest {

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
        SuggestionIndex client = new SuggestionIndex("index");
        client.truncate();

        client.addSingleTerm("1", Arrays.asList(new String[] {
            "bla", "blub"
        }), "asdf", "{}", 1);
        final List<Suggestion> results = new ArrayList<Suggestion>();
        client.findSuggestionsFor("b", 10, new ASuggestionsRetrievedListener() {

            public void suggestionsRetrieved(List<Suggestion> suggestions) {
                results.addAll(suggestions);
            }
        });
        client.waitForGreen();

        /*
         * Check if we find what we've stored
         */
        Assert.assertEquals(results.size(), 1);
        Assert.assertEquals(results.get(0).suggestion, "asdf");
        Assert.assertEquals(results.get(0).payload, "{}");

        /*
         * Check if we still find the term after deleting it
         */
        Assert.assertTrue(client.deleteSingleTerm("1"));
        results.clear();
        client.findSuggestionsFor("b", 10, new ASuggestionsRetrievedListener() {

            public void suggestionsRetrieved(List<Suggestion> suggestions) {
                results.addAll(suggestions);
            }
        });
        client.waitForGreen();
        Assert.assertEquals(0, results.size());
    }
}
