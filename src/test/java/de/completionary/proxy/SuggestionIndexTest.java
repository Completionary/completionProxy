/**
 * 
 */
package de.completionary.proxy;

import java.util.Random;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.completionary.proxy.elasticsearch.SuggestionIndex;

/**
 * @author kunzejo
 * 
 */
public class SuggestionIndexTest {

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void test() {
        SuggestionIndex client = new SuggestionIndex("index");

        Random r = new Random();

        int numberOfQueries = 1000;
        for (int i = 0; i < 1000; i++) { // heat up cpu
            r.nextInt();
        }

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < numberOfQueries; i++) {
            String query = "" + (char) ('a' + Math.abs(r.nextInt()) % 25);
        }
        long randomTime = (System.currentTimeMillis() - startTime);

        startTime = System.currentTimeMillis();
        for (int i = 0; i < numberOfQueries; i++) {
            String query = "" + (char) ('a' + Math.abs(r.nextInt()) % 25);
            client.findSuggestionsFor(query, 15);
        }
        float time =
                (System.currentTimeMillis() - startTime - randomTime)
                        / (float) numberOfQueries;

        System.out.println(time + " ms per query");
    }

}
