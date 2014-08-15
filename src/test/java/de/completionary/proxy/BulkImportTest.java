/**
 * 
 */
package de.completionary.proxy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.apache.thrift.async.AsyncMethodCallback;
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

        client.async_addTerms(terms, new AsyncMethodCallback<Long>() {

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }

            @Override
            public void onComplete(Long response) {
                System.out.println("I'ts working!");
            }
        });
    }
}
