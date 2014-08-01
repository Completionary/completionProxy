/**
 * This class is used to aggregate completion statistics to be used for the
 * streaming and analytics services
 */
package de.completionary.proxy.elasticsearch;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import de.completionary.proxy.thrift.services.streaming.StreamedStatisticsField;
import de.completionary.proxy.thrift.services.suggestion.Suggestion;

/**
 * @author Jonas Kunze (kunze.jonas@gmail.com)
 *
 *         TODO: This class is not thread safe at the moment. We should
 *         implement a ZMQ communication layer between ES-Index and Aggregator
 */
class StatisticsAggregator {

    /*
     * The completion index this aggregator stores statistics of
     */
    private int numberOfCurrentUsers = 0;

    private int numberOfQueries = 0;

    private Set<String> randomSampleOfCurrentCompletedTerms;

    private int numberOfSelectedSuggestions = 0;

    private double conversionRate = 1.0;

    private int numberOfShownSuggestions = 0;

    public StatisticsAggregator() {
    }

    /**
     * Must be called every time a suggestion query was run
     * 
     * @param suggestRequest
     *            The string that was completed
     * @param suggestions
     *            The list of suggestions that was sent back to the client
     */
    public void onQuery(
            final String suggestRequest,
            final List<Suggestion> suggestions) {

        numberOfQueries++;
        if (!suggestions.isEmpty()) {
            numberOfShownSuggestions += suggestions.size();
            randomSampleOfCurrentCompletedTerms.add(suggestions.get(0)
                    .getSuggestion());
        }
    }

    /**
     * Returns the statistics aggregated since last call of this method
     * 
     * @return
     */
    public StreamedStatisticsField getCurrentStatistics() {
        StreamedStatisticsField result =
                new StreamedStatisticsField(numberOfCurrentUsers,
                        numberOfQueries, new ArrayList<String>(
                                randomSampleOfCurrentCompletedTerms),
                        numberOfSelectedSuggestions, conversionRate,
                        numberOfShownSuggestions);
        reset();
        return result;
    }

    /**
     * Resets all statistics values
     */
    private void reset() {
        numberOfCurrentUsers = 0;
        numberOfQueries = 0;
        randomSampleOfCurrentCompletedTerms = new TreeSet<String>();
        numberOfSelectedSuggestions = 0;
        conversionRate = 1.0;
        numberOfShownSuggestions = 0;
    }
}
