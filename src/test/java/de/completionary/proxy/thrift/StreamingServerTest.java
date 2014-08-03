package de.completionary.proxy.thrift;

import java.util.Random;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
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
import de.completionary.proxy.thrift.services.exceptions.IndexUnknownException;
import de.completionary.proxy.thrift.services.streaming.StreamingClientService;
import de.completionary.proxy.thrift.services.streaming.StreamingService;

public class StreamingServerTest {

	private static String index = "";

	private StreamingService.Client client;

	private StreamingClientHandler clientHandler;

	private static final int streamReceiverPort = 6538;

	@BeforeClass
	public static void setUpBeforeClass() {
		/*
		 * Start the streaming server
		 */
		new Thread(new Runnable() {

			@Override
			public void run() {
				CompletionProxy.main(null);
			}
		}).start();
	}

	@Before
	public void setUp() throws Exception {
		/*
		 * Connect to the server
		 */
		Random r = new Random();
		index = "testindex" + r.nextInt();

		TTransport transport = new TFramedTransport(new TSocket("localhost",
				ProxyOptions.STREAMING_SERVER_PORT));
		TProtocol protocol = new TBinaryProtocol(transport);

		client = new StreamingService.Client(protocol);
		while (true) {
			try {
				transport.open();
				break;
			} catch (TTransportException e) {
				System.err.println("Unable to connect to StreamingService. Retrying...");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
				}
			}
		}
		System.out.println("Connection to StreamingService established.");

		/*
		 * Start the streaming client receiver
		 */
		try {
			TNonblockingServerTransport trans = new TNonblockingServerSocket(
					streamReceiverPort);
			TNonblockingServer.Args args = new TNonblockingServer.Args(trans);
			args.transportFactory(new TFramedTransport.Factory());
			args.protocolFactory(new TBinaryProtocol.Factory());
			clientHandler = new StreamingClientHandler();
			args.processor(new StreamingClientService.AsyncProcessor<StreamingClientService.AsyncIface>(
					clientHandler));
			final TServer server = new TNonblockingServer(args);
			(new Thread() {

				@Override
				public void run() {
					server.serve();
				}
			}).start();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@After
	public void tearDown() throws Exception {
		SuggestionIndex.delete(index);
	}

	@Test
	public void test() throws InterruptedException {
		try {
			client.establishStream("wikipediaindex", "localhost", streamReceiverPort);
		} catch (IndexUnknownException e) {
			e.printStackTrace();
		} catch (TException e) {
			e.printStackTrace();
		}

		Thread.sleep(100000);
	}
}
