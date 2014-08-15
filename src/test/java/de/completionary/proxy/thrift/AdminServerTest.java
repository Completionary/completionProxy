package de.completionary.proxy.thrift;

import java.util.Arrays;
import java.util.Random;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.completionary.proxy.CompletionProxy;
import de.completionary.proxy.elasticsearch.SuggestionIndex;
import de.completionary.proxy.helper.ProxyOptions;
import de.completionary.proxy.thrift.services.admin.AdminService;

public class AdminServerTest {

    private static String index = "";

    private AdminService.Client client;

    @BeforeClass
    public static void setUpBeforeClass() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                CompletionProxy.main(null);
            }
        }).start();
    }

    @Before
    public void setUp() throws Exception {
        Random r = new Random();
        index = "testindex" + r.nextInt();

        TTransport transport =
                new TFramedTransport(new TSocket("localhost",
                        ProxyOptions.ADMIN_SERVER_PORT));
        TProtocol protocol = new TBinaryProtocol(transport);

        client = new AdminService.Client(protocol);
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

    @After
    public void tearDown() throws Exception {
        SuggestionIndex.delete(index);
    }

    @Test
    public void test() throws TException {
        client.addSingleTerm("", index, 1, Arrays.asList(new String[] {
            "bla", "blub"
        }), "bla blub", "payload", 3);

    }
}
