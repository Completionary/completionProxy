/**
 * This class is used to aggregate completion statistics to be used for the
 * streaming and analytics services
 */
package de.completionary.proxy.elasticsearch;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import de.completionary.proxy.analytics.AnalyticsLogger;
import de.completionary.proxy.helper.ProxyOptions;
import de.completionary.proxy.thrift.services.streaming.StreamedStatisticsField;
import de.completionary.proxy.thrift.services.suggestion.AnalyticsData;
import de.completionary.proxy.thrift.services.suggestion.Suggestion;

/**
 * @author Jonas Kunze (kunze.jonas@gmail.com)
 *
 *         TODO: This class is not thread safe at the moment. We should
 *         implement a ZMQ communication layer between ES-Index and Aggregator
 */
class StatisticsAggregator {

    /*
     * List of the active users during the current aggregation priod (1s)
     */
    private Set<Long> activeUsers = new TreeSet<Long>();

    private AtomicInteger numberOfQueries = new AtomicInteger(0);

    private Set<String> randomSampleOfCurrentCompletedTerms =
            new TreeSet<String>();

    private AtomicInteger numberOfSelectedSuggestions = new AtomicInteger(0);

    private AtomicInteger numberOfSearchSessions = new AtomicInteger(0);

    private AtomicInteger numberOfShownSuggestions = new AtomicInteger(0);

    private AtomicLong indexSize;

    private AtomicLong numberOfQueriesThisMonth;

    private static final AnalyticsLogger logger = new AnalyticsLogger();

    public StatisticsAggregator(
            long indexSize,
            long numberOfQueriesThisMonth) {
        this.indexSize = new AtomicLong(indexSize);
        this.numberOfQueriesThisMonth =
                new AtomicLong(numberOfQueriesThisMonth);
    }

    /**
     * Must be called every time an search session is finished (suggestion is
     * selected, timeout or query is deleted)
     */
    public void onSearchSessionFinished(AnalyticsData userData) {
        numberOfSearchSessions.incrementAndGet();
        logger.logSessionFinished(userData);
    }

    /**
     * Must be called every time a suggestion query was run
     * 
     * @param userData
     *            End user specific analytics data
     * @param suggestRequest
     *            The string that was completed
     * @param suggestions
     *            The list of suggestions that was sent back to the client
     */
    public void onQuery(
            final AnalyticsData userData,
            final String suggestRequest,
            final List<Suggestion> suggestions) {
        activeUsers.add(userData.userID);
        numberOfQueries.incrementAndGet();
        numberOfQueriesThisMonth.incrementAndGet();
        if (!suggestions.isEmpty()) {
            numberOfShownSuggestions.addAndGet(suggestions.size());
            randomSampleOfCurrentCompletedTerms.add(suggestions.get(0)
                    .getSuggestion());
        }

        logger.logQuery(userData, suggestRequest);
    }

    /**
     * Must be called every time the end user clicks on a suggestion. This
     * method counts the number of selected terms and stores a sample of these.
     * Additionally this method triggers a onSessionFinished
     * 
     * @param suggestionID
     *            The ID of the suggestion string that was selected by the end
     *            user
     * @param suggestionID
     *            The suggestion string that was selected by the end user
     */
    public void onSuggestionSelected(
            final String suggestionID,
            final String suggestionString,
            final AnalyticsData userData) {
        if (ProxyOptions.MAX_NUMBER_OF_SAMPLE_TERMS_IN_STREAM != randomSampleOfCurrentCompletedTerms
                .size()) {
            randomSampleOfCurrentCompletedTerms.add(suggestionString);
        }
        numberOfSelectedSuggestions.incrementAndGet();
        numberOfSearchSessions.incrementAndGet();

        logger.logSuggestionSelected(userData, suggestionID);
    }

    /**
     * Must be called every time a new term was added to the index
     */
    public void onTermAdded() {
        indexSize.incrementAndGet();
    }

    /**
     * Must be called every time a term was deleted from the index
     */
    public void onTermDeleted() {
        indexSize.decrementAndGet();
    }

    /**
     * Returns the statistics aggregated since last call of this method
     * 
     * @return
     */
    public StreamedStatisticsField getCurrentStatistics() {
        StreamedStatisticsField result =
                new StreamedStatisticsField(activeUsers.size(),
                        numberOfQueries.get(), new ArrayList<String>(
                                randomSampleOfCurrentCompletedTerms),
                        numberOfSelectedSuggestions.get(),
                        numberOfSearchSessions.get(),
                        numberOfShownSuggestions.get(), indexSize.get(),
                        numberOfQueriesThisMonth.get());
        reset();
        return result;
    }

    /**
     * Resets all statistics values
     */
    private void reset() {
        activeUsers.clear();
        numberOfQueries.set(0);
        randomSampleOfCurrentCompletedTerms.clear();
        numberOfSelectedSuggestions.set(0);
        numberOfSearchSessions.set(0);
        numberOfShownSuggestions.set(0);
    }
}
