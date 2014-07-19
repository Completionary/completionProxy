/**
 * 
 */
package de.completionary.proxy;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import de.completionary.proxy.elasticsearch.SuggestionIndex;
import de.completionary.proxy.server.ASuggestionsRetrievedListener;
import de.completionary.proxy.structs.Suggestion;

/**
 * @author kunzejo
 * 
 */
public class SuggestionIndexTest {

    @Test
    public void speedTest() {
        SuggestionIndex client = new SuggestionIndex("index");

        Random r = new Random();

        final int numberOfQueries = 1000;
        for (int i = 0; i < 1000; i++) { // heat up cpu
            r.nextInt();
        }

        final long randomStartTime = System.currentTimeMillis();
        for (int i = 0; i < numberOfQueries; i++) {
            String query = "" + (char) ('a' + Math.abs(r.nextInt()) % 25);
        }
        final long randomTime = (System.currentTimeMillis() - randomStartTime);

        final AtomicInteger numberOfWaitingRequests =
                new AtomicInteger(numberOfQueries);
        final long startTime = System.currentTimeMillis();
        for (int i = 0; i < numberOfQueries; i++) {
            String query = "" + (char) ('a' + Math.abs(r.nextInt()) % 25);
            client.findSuggestionsFor(query, 15,
                    new ASuggestionsRetrievedListener() {

                        public void suggestionsRetrieved(
                                List<Suggestion> suggestions) {
                            if (numberOfWaitingRequests.decrementAndGet() == 0) {
                                float time =
                                        (System.currentTimeMillis() - startTime - randomTime)
                                                / (float) numberOfQueries;

                                System.out.println(time + " ms per query");
                            }
                        }
                    });
        }
        while (numberOfWaitingRequests.get() != 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
