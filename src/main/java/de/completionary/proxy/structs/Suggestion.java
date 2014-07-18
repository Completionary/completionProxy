package de.completionary.proxy.structs;

public class Suggestion {

    public final String suggestion;

    public final String payload;

    public Suggestion(
            final String suggestion,
            final String payload) {
        this.suggestion = suggestion;
        this.payload = payload;
    }
}
