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

import de.completionary.proxy.helper.ProxyOptions;
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
	 * List of the active users during the current aggregation priod (1s)
	 */
	private Set<Long> activeUsers = new TreeSet<Long>();

	private AtomicInteger numberOfQueries = new AtomicInteger(0);

	private Set<String> randomSampleOfCurrentCompletedTerms = new TreeSet<String>();

	private AtomicInteger numberOfSelectedSuggestions = new AtomicInteger(0);

	private AtomicInteger numberOfSearchSessions = new AtomicInteger(0);

	private AtomicInteger numberOfShownSuggestions = new AtomicInteger(0);

	private AtomicLong indexSize;

	private AtomicLong numberOfQueriesThisMonth;

	public StatisticsAggregator(long indexSize, long numberOfQueriesThisMonth) {
		this.indexSize = new AtomicLong(indexSize);
		this.numberOfQueriesThisMonth = new AtomicLong(numberOfQueriesThisMonth);
	}

	/**
	 * Must be called every time an search session is finished
	 */
	public void onSearchSessionFinished(long userID) {
		numberOfSearchSessions.incrementAndGet();
	}

	/**
	 * Must be called every time a suggestion query was run
	 * 
	 * @param userID
	 *            The user session ID of the end user sending the query
	 * @param suggestRequest
	 *            The string that was completed
	 * @param suggestions
	 *            The list of suggestions that was sent back to the client
	 */
	public void onQuery(long userID, final String suggestRequest,
			final List<Suggestion> suggestions) {
		activeUsers.add(userID);
		numberOfQueries.incrementAndGet();
		numberOfQueriesThisMonth.incrementAndGet();
		if (!suggestions.isEmpty()) {
			numberOfShownSuggestions.addAndGet(suggestions.size());
			randomSampleOfCurrentCompletedTerms.add(suggestions.get(0)
					.getSuggestion());
		}
	}

	/**
	 * Must be called every time the end user clicks on a suggestion. This
	 * method counts the number of selected terms and stores a sample of these.
	 * 
	 * @param suggestedString
	 *            The string that was selected by the end user
	 */
	public void onSuggestionSelected(final String suggestedString) {
		if (ProxyOptions.MAX_NUMBER_OF_SAMPLE_TERMS_IN_STREAM != randomSampleOfCurrentCompletedTerms
				.size()) {
			randomSampleOfCurrentCompletedTerms.add(suggestedString);
		}
		numberOfSelectedSuggestions.incrementAndGet();
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
		StreamedStatisticsField result = new StreamedStatisticsField(
				activeUsers.size(), numberOfQueries.get(),
				new ArrayList<String>(randomSampleOfCurrentCompletedTerms),
				numberOfSelectedSuggestions.get(),
				numberOfSearchSessions.get(), numberOfShownSuggestions.get(),
				indexSize.get(), numberOfQueriesThisMonth.get());
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
