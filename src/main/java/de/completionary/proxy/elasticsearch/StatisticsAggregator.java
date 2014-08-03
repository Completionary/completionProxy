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
	private AtomicInteger numberOfCurrentUsers = new AtomicInteger(0);

	private AtomicInteger numberOfQueries = new AtomicInteger(0);

	private Set<String> randomSampleOfCurrentCompletedTerms = new TreeSet<String>();

	private AtomicInteger numberOfSelectedSuggestions = new AtomicInteger(0);

	private double conversionRate = 1.0;

	private AtomicInteger numberOfShownSuggestions = new AtomicInteger(0);

	private AtomicLong indexSize;

	private AtomicLong numberOfQueriesThisMonth;

	public StatisticsAggregator(long indexSize, long numberOfQueriesThisMonth) {
		this.indexSize = new AtomicLong(indexSize);
		this.numberOfQueriesThisMonth = new AtomicLong(numberOfQueriesThisMonth);
	}

	/**
	 * Must be called every time a suggestion query was run
	 * 
	 * @param suggestRequest
	 *            The string that was completed
	 * @param suggestions
	 *            The list of suggestions that was sent back to the client
	 */
	public void onQuery(final String suggestRequest,
			final List<Suggestion> suggestions) {
		numberOfQueries.incrementAndGet();
		numberOfQueriesThisMonth.incrementAndGet();
		if (!suggestions.isEmpty()) {
			numberOfShownSuggestions.addAndGet(suggestions.size());
			randomSampleOfCurrentCompletedTerms.add(suggestions.get(0)
					.getSuggestion());
		}
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
				numberOfCurrentUsers.get(), numberOfQueries.get(),
				new ArrayList<String>(randomSampleOfCurrentCompletedTerms),
				numberOfSelectedSuggestions.get(), conversionRate,
				numberOfShownSuggestions.get(), indexSize.get(),
				numberOfQueriesThisMonth.get());
		reset();
		return result;
	}

	/**
	 * Resets all statistics values
	 */
	private void reset() {
		numberOfQueries.set(0);
		randomSampleOfCurrentCompletedTerms = new TreeSet<String>();
		numberOfSelectedSuggestions.set(0);
		conversionRate = 1.0;
		numberOfShownSuggestions.set(0);
	}
}
