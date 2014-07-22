package de.completionary.proxy.server;

import java.util.List;

import de.completionary.proxy.thrift.services.Suggestion;

public interface ISuggestionsRetrievedListener {

    public void suggestionsRetrieved(final List<Suggestion> suggestions);
}
