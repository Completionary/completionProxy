namespace java de.completionary.proxy.thrift.services.analytics

include "admin.thrift"

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
}