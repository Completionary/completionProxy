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
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.thrift.async.AsyncMethodCallback;
import org.junit.Assert;
import org.junit.Test;

import de.completionary.proxy.elasticsearch.SuggestionIndex;
import de.completionary.proxy.helper.RandomStringGenerator;
import de.completionary.proxy.helper.Statistics;
import de.completionary.proxy.thrift.services.admin.SuggestionField;
import de.completionary.proxy.thrift.services.exceptions.InvalidIndexNameException;
import de.completionary.proxy.thrift.services.exceptions.ServerDownException;
import de.completionary.proxy.thrift.services.suggestion.AnalyticsData;
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
                lock.await(numberOfTermsToAdd * 100, TimeUnit.MILLISECONDS));

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
       
        
        final String index = "wikipediaindex";
        SuggestionIndex client = SuggestionIndex.getIndex(index);
        client.findSuggestionsFor("a", 10, new AnalyticsData());
        //        if (!SuggestionIndex.indexExists(index)) {
        //            FillDB(index, 10000);
        //        }
        

        for (int sleepTime = 0; sleepTime < 200; sleepTime *= 2) {

            final int numberOfReuqests = 1000;

            final Random random = new SecureRandom();

            final AtomicInteger numberOfSuggestionsFound = new AtomicInteger(0);
            final long[] times = new long[numberOfReuqests];

            final CountDownLatch lock = new CountDownLatch(numberOfReuqests);

            final long start = System.nanoTime();
            double requestsPerSecond = 0;

            for (int i = 0; i < numberOfReuqests; i++) {
                final int request = i;
                String query =
                        RandomStringGenerator
                                .getNextString(random.nextInt(10) + 1);

                final long requestStart = System.nanoTime();
                client.async_findSuggestionsFor(query, 10, new AnalyticsData(),
                        new AsyncMethodCallback<List<Suggestion>>() {

                            public void onError(Exception e) {
                                Assert.fail(e.getLocalizedMessage());
                                lock.countDown();
                            }

                            public void
                                onComplete(List<Suggestion> suggestions) {
                                Assert.assertNotNull("An Error has occured",
                                        suggestions);
                                numberOfSuggestionsFound.addAndGet(suggestions
                                        .size());
                                lock.countDown();
                                times[request] =
                                        System.nanoTime() - requestStart;
                            }
                        });
                requestsPerSecond =
                        (double) (request + 1) / (System.nanoTime() - start)
                                * 1E9;
                //                if (request % 100 == 0) {
                //                    System.out.println("Request rate: " + requestsPerSecond
                //                            + " Hz");
                //                }

                for(int j=0; j<sleepTime*1E4; j++){
                    
                }
            }

            lock.await();

            double averageTimePerRequestms =
                    Statistics.calculateAverage(times) / 1E6;
            double standardDeviation =
                    Statistics.calculateStandardDeviation(times) / 1E6;

            //        System.out.println("Request rate: " + requestsPerSecond + " Hz");
            //        System.out.println("found " + numberOfSuggestionsFound
            //                + " suggestions in " + numberOfReuqests + " requests) ");
            //        System.out.println("(" + averageTimePerRequestms + "+-"
            //                + standardDeviation + ") ms per request average");

            System.out.println(sleepTime + "\t" + averageTimePerRequestms
                    + "\t" + standardDeviation + "\t" + requestsPerSecond);

            if (sleepTime == 0) {
                sleepTime = 1;
            }
        }
    }
}
