package de.completionary.proxy.structs;

import java.util.List;

public class SuggestionField {

    public final String ID;

    public final String output;

    public final List<String> input;

    public final String payload;

    public final int weight;

    /**
     * Data used to store new terms in the DB
     * All Strings (output, payload and all input strings) must be escaped:
     * JSONObject.escape()
     * 
     * @param ID
     *            Used to reference this field for deletion queries.
     *            If <ID> is null <output> will be used instead
     * @param output
     *            In case this term matches a suggestion query, this string will
     *            be displayed
     * @param input
     *            List of strings used for the completion index triggering this
     *            field
     * @param payload
     *            Additional data stored with this term
     * @param weight
     *            Weight of the term
     */
    public SuggestionField(
            final String ID,
            final String output,
            final List<String> input,
            final String payload,
            final int weight) {
        this.ID = ID;
        this.output = output;
        this.input = input;
        this.payload = payload;
        this.weight = weight;
    }
}
