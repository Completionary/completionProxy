/**
 * 
 */
package de.completionary.proxy;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import de.completionary.proxy.elasticsearch.SuggestionIndex;
import de.completionary.proxy.helper.ProxyOptions;
import de.completionary.proxy.thrift.services.exceptions.InvalidIndexNameException;
import de.completionary.proxy.thrift.services.exceptions.ServerDownException;

/**
 * @author kunzejo
 * 
 */
public class AnalyticsTest extends SuggestionIndexTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        ProxyOptions.ENABLE_LOGGIN = true;
    }
    
    @Test
    public void IndexSizeTest() throws InterruptedException,
            ExecutionException, IOException, InvalidIndexNameException,
            ServerDownException {
        SuggestionIndex client = getRandomIndex();

        addTerm(client, 1, "term", "payload");

        /*
         * Check if the index size is 1
         */
        Assert.assertEquals(1,
                client.getStatistics().getCurrentStatistics().indexSize);
        /*
         * IndexSize is a COUNTER type so it should not be reset after
         * requesting getCurrentStatistics()
         */
        Assert.assertEquals(1,
                client.getStatistics().getCurrentStatistics().indexSize);

        deleteTerm(client, 1, 20000); // Long timeout to generate rrd DB

        Assert.assertEquals(0,
                client.getStatistics().getCurrentStatistics().indexSize);
    }
}
