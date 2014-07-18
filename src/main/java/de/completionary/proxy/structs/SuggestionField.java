package de.completionary.proxy.structs;

import java.util.List;

public class SuggestionField {

    public final String output;

    public final List<String> input;

    public final String payload;

    public final int weight;

    /**
     * All Strings (output, payload and all input strings) must be escaped:
     * JSONObject.escape()
     * 
     * @param output
     * @param input
     * @param payload
     * @param weight
     */
    public SuggestionField(
            final String output,
            final List<String> input,
            final String payload,
            final int weight) {
        this.output = output;
        this.input = input;
        this.payload = payload;
        this.weight = weight;
    }
}
