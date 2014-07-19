package de.completionary.proxy.server;

import java.util.List;

import de.completionary.proxy.structs.Suggestion;

public interface ASuggestionsRetrievedListener {

    public void suggestionsRetrieved(final List<Suggestion> suggestions);
}
