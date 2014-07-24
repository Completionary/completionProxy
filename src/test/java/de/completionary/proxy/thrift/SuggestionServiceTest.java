package de.completionary.proxy.thrift;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import junit.framework.Assert;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.completionary.proxy.elasticsearch.SuggestionIndex;
import de.completionary.proxy.thrift.server.AdminServer;
import de.completionary.proxy.thrift.services.Suggestion;
import de.completionary.proxy.thrift.services.SuggestionService;

public class SuggestionServiceTest {

    static TTransport transport;

    static SuggestionService.Client client;

    @BeforeClass
    public static void setUpBeforeClass() {
        AdminServer.startNewInstance();

        transport = new TFramedTransport(new TSocket("localhost", 9090));
        TProtocol protocol = new TBinaryProtocol(transport);

        client = new SuggestionService.Client(protocol);
        while (true) {
            try {
                transport.open();
                break;
            } catch (TTransportException e) {
                e.printStackTrace();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                }
            }
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        transport.close();
    }

    @Test
    public void perform() throws TException {
        final String indexID = "index";
        final String term = "asdf";
        final String query = "as";
        final String payload = "{}";

        SuggestionIndex index = SuggestionIndex.getIndex(indexID);
        try {
            index.truncate();

            /*
             * Check if the index is really truncated
             */
            List<Suggestion> suggestions =
                    client.findSuggestionsFor(indexID, query, (short) 10);
            Assert.assertTrue(suggestions.isEmpty());

            /*
             * Add an element and check if we find it in a query
             */
            index.addSingleTerm("1", Arrays.asList(new String[] {
                term
            }), null, payload, 1);
            suggestions = client.findSuggestionsFor(indexID, query, (short) 10);
            Assert.assertEquals(suggestions.size(), 1);
            Assert.assertEquals(suggestions.get(0).suggestion, term);
            Assert.assertEquals(suggestions.get(0).payload, payload);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
