/**
 * 
 */
package de.completionary.proxy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
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

/**
 * @author kunzejo
 * 
 */
public class BulkImportTest extends SuggestionIndexTest {

    private CountDownLatch lock = new CountDownLatch(1);

    @Test
    public void Test() throws InterruptedException, ExecutionException,
            IOException, InvalidIndexNameException, ServerDownException {
        SuggestionIndex client = SuggestionIndex.getIndex(index);
        int numberOfTermsToAdd = 10000;

        List<SuggestionField> terms = new ArrayList<SuggestionField>();

        for (int i = 0; i != numberOfTermsToAdd; i++) {
            String s = UUID.randomUUID().toString();
            SuggestionField field =
                    new SuggestionField(i, s, Arrays.asList(new String[] {
                        s
                    }), s, i);
            terms.add(field);
        }

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
                lock.await(1000, TimeUnit.SECONDS));

        for (int i = 0; i < 10; i++) {
            SuggestionField field = terms.get(i);
            checkIfEntryIsFound(client, field.getOutputField(),
                    field.getOutputField(), field.getPayload());
        }
    }
}
