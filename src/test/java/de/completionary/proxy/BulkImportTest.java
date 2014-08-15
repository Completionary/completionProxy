/**
 * 
 */
package de.completionary.proxy;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.thrift.async.AsyncMethodCallback;
import org.junit.Assert;
import org.junit.Test;

import de.completionary.proxy.elasticsearch.SuggestionIndex;
import de.completionary.proxy.thrift.services.admin.SuggestionField;
import de.completionary.proxy.thrift.services.exceptions.InvalidIndexNameException;
import de.completionary.proxy.thrift.services.exceptions.ServerDownException;
import de.completionary.proxy.thrift.services.suggestion.Suggestion;

/**
 * @author kunzejo
 * 
 */
public class BulkImportTest extends SuggestionIndexTest {

    private static CountDownLatch lock = new CountDownLatch(1);

    public static void FillDB(String index, int numberOfTermsToAdd)
            throws InvalidIndexNameException, ServerDownException, IOException,
            InterruptedException {
        SuggestionIndex client = SuggestionIndex.getIndex(index);

        /*
         * Generate a list of random SuggestionFields
         */
        List<SuggestionField> terms = new ArrayList<SuggestionField>();

        for (int i = 0; i != numberOfTermsToAdd; i++) {
            String s = RandomStringGenerator.getNextString(10);
            String payload = RandomStringGenerator.getNextString(1);
            SuggestionField field =
                    new SuggestionField(i, s, Arrays.asList(new String[] {
                        s
                    }), payload, i);
            terms.add(field);
        }

        /*
         * Push these SuggestionFields to the DB
         */
        lock = new CountDownLatch(1);
        client.async_addTerms(terms, new AsyncMethodCallback<Long>() {

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }

            @Override
            public void onComplete(Long response) {
                lock.countDown();
            }
        });
        Assert.assertTrue("async_addTerms has timed out",
                lock.await(numberOfTermsToAdd, TimeUnit.MILLISECONDS));

        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            SuggestionField field = terms.get(random.nextInt(terms.size()));
            checkIfEntryIsFound(client, field.getOutputField(),
                    field.getOutputField(), field.getPayload(), 2000);
        }
    }

    @Test
    public void PerformanceTest() throws InterruptedException,
            ExecutionException, IOException, InvalidIndexNameException,
            ServerDownException {
        final int numberOfThreads = 8;
        final int numberOfReuqests = 100000;

        final String index = "bulkperformanceindex";
        if (!SuggestionIndex.indexExists(index)) {
            FillDB(index, 1000000);
        }

        Thread[] threads = new Thread[numberOfThreads];
        for (int thread = 0; thread != numberOfThreads; thread++) {

            threads[thread] = new Thread() {

                @Override
                public void run() {
                    final Random random = new SecureRandom();
                    try {
                        long start = System.nanoTime();
                        SuggestionIndex client =
                                SuggestionIndex.getIndex(index);
                        int numberOfSuggestionsFound = 0;
                        for (int i = 0; i < numberOfReuqests; i++) {
                            String query =
                                    RandomStringGenerator.getNextString(random
                                            .nextInt(10) + 1);
                            List<Suggestion> l =
                                    findSuggestionFor(client, query, 200000);
                            numberOfSuggestionsFound += l.size();
                        }
                        double timePerRequestInms =
                                (System.nanoTime() - start)
                                        / (double) numberOfReuqests / 1E6;
                        System.out.println("Took " + timePerRequestInms
                                + " ms per completion request (found "
                                + numberOfSuggestionsFound + " suggestions in "
                                + numberOfReuqests + " requests)");
                    } catch (InvalidIndexNameException | ServerDownException
                            | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            threads[thread].start();
        }

        for (int thread = 0; thread != numberOfThreads; thread++) {
            threads[thread].join();
        }
        System.exit(0);
    }
}
