package de.completionary.proxy;

import java.math.BigInteger;
import java.util.List;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONStyle;

import org.zeromq.ZMQ;

import de.completionary.proxy.elasticsearch.SuggestionIndex;
import de.completionary.proxy.structs.Suggestion;

public class Main {

    static boolean running = true;

    public static void main(String[] args) {

        SuggestionIndex client = new SuggestionIndex("index");

        ZMQ.Context context = ZMQ.context(1);

        // Socket to talk to server
        System.out.println("Connecting to hello world server…");

        ZMQ.Socket inSocket = context.socket(ZMQ.PULL);
        inSocket.connect("tcp://localhost:9241");

        ZMQ.Socket outSocket = context.socket(ZMQ.PUSH);
        outSocket.connect("tcp://localhost:9242");

        while (running) {

            /*
             * All messages are 3-part messages in the form of: message_type
             * session_id data
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
                // List<Suggestion> suggestions = new ArrayList<Suggestion>();
                // suggestions.add(new Suggestion("string", "payload"));
                // suggestions.add(new Suggestion("string2", "payload2"));

                JSONObject builder = new JSONObject();
                builder.put("suggestionList", suggestions);

                outSocket.send(msgBuffer, ZMQ.SNDMORE); // Send "message"
                outSocket.send(idBuffer, ZMQ.SNDMORE); // Send "ID"
                outSocket.send(builder
                        .toJSONString(/* JSONStyle.MAX_COMPRESS */));
                System.out
                        .println(builder.toJSONString(JSONStyle.MAX_COMPRESS));

            }
        }
        inSocket.close();
        context.term();
    }
}
