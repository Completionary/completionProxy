package de.completionary.proxy;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.zeromq.ZMQ;

import de.completionary.proxy.elasticsearch.SuggestionIndex;
import de.completionary.proxy.structs.Suggestion;

public class Main {

    public static void main(String[] args) {

        SuggestionIndex client = new SuggestionIndex("index");

        ZMQ.Context context = ZMQ.context(1);

        //  Socket to talk to server
        System.out.println("Connecting to hello world server…");

        ZMQ.Socket inSocket = context.socket(ZMQ.PULL);
        inSocket.connect("tcp://localhost:9241");

        ZMQ.Socket outSocket = context.socket(ZMQ.PUSH);
        outSocket.connect("tcp://localhost:9242");

        while (true) {

            /*
             * All messages are 3-part messages in the form of:
             * message_type
             * session_id
             * data
             */
            byte[] msgBuffer = inSocket.recv(0);
            String msg = new String(msgBuffer);
            byte[] idBuffer = inSocket.recv(0);
            long id = new BigInteger(idBuffer).longValue();
            String data = new String(inSocket.recv(0));
            System.out.println("Received " + msg);

            if (msg.equals("message")) {
                List<Suggestion> suggestions =
                        client.findSuggestionsFor(data, 15);

                try {
                    XContentBuilder json =
                            jsonBuilder().startObject().startArray(
                                    "suggestionList");

                    for (Suggestion s : suggestions) {
                        json.value(s.suggestion);
                    }
                    json.endArray().endObject();
                    outSocket.send(msgBuffer, ZMQ.SNDMORE); // Send "message"
                    outSocket.send(idBuffer, ZMQ.SNDMORE); // Send "ID"
                    outSocket.send(json.string());
                    System.out.println(json.string());

                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

            }
        }
        inSocket.close();
        context.term();

    }
}
