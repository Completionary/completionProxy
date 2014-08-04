namespace java de.completionary.proxy.thrift.services.suggestion

/*
 * Including a common.thrift with typedefs only works if you call
 * the type via common.int -> that's ugly so let's define it here
 */
typedef i16 short
typedef i32 int
typedef i64 long

/*
 * Data sent back from the suggestion service
 * @param suggestion 
 *			  The suggested string (equals 'output' of SuggestionField)
 * @param payload 
 *			  Additional data stored with this suggestion term. The standard
 *			  format is the following JSON:
 *            {"href":"$URL","image":"$URL_or_BASE64Image"}
 */
struct Suggestion {
	1: string suggestion;
	2: string payload;
}

/*
 * Data used to send with every RPC call for analytics purposes
 */
struct AnalyticsData {
	/* 
	 * The unique ID of the end user. If the user is logged in you
	 * should use the user ID within your database. If not you could
	 * use the session ID.
	 */
	1: long userID
	
	/*
	 * The user-agent data specifying the browser of the end user
	 */
	2: string userAgent;
}

service SuggestionService {
	/**
	 * Returns the top k completions of <query> in the given index
	 * 
	 * @param index
	 *            The index to be used to generate the suggestions
	 * @param query
	 *            The string to be completed
	 * @param numberOfSuggestions
	 *            The maximum number of suggestions to be returned
	 */
	list<Suggestion> findSuggestionsFor(1: string index, 2: string query, 3: short numberOfSuggestions, 4: AnalyticsData userData);
	
	oneway void onSearchSessionFinished(1: string index, 2: AnalyticsData userData);
}