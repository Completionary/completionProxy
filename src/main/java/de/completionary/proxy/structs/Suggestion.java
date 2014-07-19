package de.completionary.proxy.structs;

import java.io.IOException;

import net.minidev.json.JSONStreamAware;

public class Suggestion implements JSONStreamAware {

    public final String suggestion;

    public final String payload;

    /**
     * All strings must be escaped (see e.g. JSONObject.escape())
     * 
     * @param suggestion
     * @param payload
     */
    public Suggestion(
            final String suggestion,
            final String payload) {
        this.suggestion = suggestion;
        this.payload = payload;
    }

    public void writeJSONString(Appendable out) throws IOException {
        out.append('[').append('"');
        out.append(suggestion, 0, suggestion.length());
        out.append('"').append(',').append('"');
        out.append(payload, 0, payload.length());
        out.append('"').append(']');
    }

}
