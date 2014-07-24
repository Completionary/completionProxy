package de.completionary.proxy.server;

import org.zeromq.ZMQ;

import de.completionary.proxy.elasticsearch.SuggestionIndex;

public class CompletionServer {

    public static ZMQ.Context context = ZMQ.context(1);

    static boolean running = true;

    SuggestionIndex index = SuggestionIndex.getIndex("index");

    ZMQ.Socket inSocket = context.socket(ZMQ.PULL);

    public CompletionServer() {
        inSocket.connect("tcp://localhost:9241");
    }

    public void run() {

        while (running) {

            /*
             * All messages are 3-part messages in the form of: message_type
             * session_id data
             */
            byte[] msgBuffer = inSocket.recv(0);
            String msg = new String(msgBuffer);
            byte[] idBuffer = inSocket.recv(0);
            //long id = new BigInteger(idBuffer).longValue();
            String data = new String(inSocket.recv(0));
            System.out.println("Received " + msg);

            if (msg.equals("message")) {
                index.async_findSuggestionsFor(data, 15,
                        new SuggestionsRetrievedListener(idBuffer));
            }
        }
        inSocket.close();
        context.term();
    }
}
