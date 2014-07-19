package de.completionary.proxy.server;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.zeromq.ZMQ;

import net.minidev.json.JSONObject;
import de.completionary.proxy.structs.Suggestion;

public class SuggestionsRetrievedListener implements ASuggestionsRetrievedListener {

    final byte[] clientID;

    private static ZMQ.Socket[] sockets = new ZMQ.Socket[1];

    public SuggestionsRetrievedListener(
            final byte[] clientID) {
        this.clientID = clientID;
    }
    
    /**
     * Returns a thread scoped socket to be used to send the response.
     * At the moment an array of sockets is stored and the sockets are stored in
     * this array and later retrieved based on the thread ID.
     * 
     * TODO: Spring Bean does not work yet! The method is called every time.
     * 
     * @return A thread scoped response socket
     */
    @Bean
    @Scope("thread")
    public ZMQ.Socket responseSocket() {
        int threadId = (int) Thread.currentThread().getId();

        ZMQ.Socket outSocket;
        if (threadId < sockets.length) {
            outSocket = sockets[threadId];
            if (outSocket != null) {
                System.out.println(threadId + "\t" + outSocket);
                return outSocket;
            }
        }

        synchronized (sockets) {
            outSocket = CompletionServer.context.socket(ZMQ.PUSH);
            outSocket.connect("tcp://localhost:9242");

            if (sockets.length <= threadId) {
                ZMQ.Socket[] tmp = new ZMQ.Socket[threadId * 2];
                System.arraycopy(sockets, 0, tmp, 0, sockets.length);
                sockets = tmp;
            }
            sockets[threadId] = outSocket;
        }

        System.out.println(threadId + "\t" + outSocket);
        return outSocket;
    }

    public void suggestionsRetrieved(final List<Suggestion> suggestions) {
        JSONObject builder = new JSONObject();
        builder.put("suggestionList", suggestions);

        ZMQ.Socket outSocket = responseSocket();

        System.out.println(outSocket);
        outSocket.send("message", ZMQ.SNDMORE); // Send "message"
        outSocket.send(clientID, ZMQ.SNDMORE); // Send "ID"
        outSocket.send(builder.toJSONString(/* JSONStyle.MAX_COMPRESS */));
    }
}
