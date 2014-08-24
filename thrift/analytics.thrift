namespace java de.completionary.proxy.thrift.services.analytics

include "admin.thrift"

include "streaming.thrift"

/*
 * Including a common.thrift with typedefs only works if you call
 * the type via common.int -> that's ugly so let's define it here
 */
typedef i16 short
typedef i32 int
typedef i64 long

/**
 * Serice to retrieve analytics for API customer
 * 
 * 
 * 
 **/
service AnalyticsService {
	/**
	 * retrieves a list of top asked queries (which have been selected from users)
	 * the weight in the SuggestioField will be the amount of selections
	 */
	list<admin.SuggestionField> topQueriesSince(1: int date, 2: short k);

	// I want a function that enables to display the currently asked queries. This should be a polling http request

	// I want a function to display how much traffic has been used

	// I want a function that displays the current payment plan

	/**
	 * Returns the number of total queries inquired this month
	 */
	long getNumberOfTotalQueriesThisMonth(1: string index);

	/**
	 *  Returns the number of items that are currently stored in the index 
	 */
	int getIndexSize(1: string index);

	/**
	 * Retrieves a list of statistics fields in the lowest time resolution available within
	 * the timeperiod [startTime, endTime]. numberOfQueriesThisMonth and 
	 * randomSampleOfCurrentCompletedTerms will not be set. 
	 */
	list<streaming.StreamedStatisticsField> getAnalytics(1: string index, 2: long startTime, 3: long endTime);
}