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
 *			  The additional data stored with the suggested term
 */
struct Suggestion {
	1: string suggestion;
	//will be a JSON and we have to specify supported base format
	2: string payload;
}

service SuggestionService {
	/**
	 * Returns the top k completions of <query>
	 */
	list<Suggestion> findSuggestionsFor(1: string index, 2: string query, 3: short k);
}