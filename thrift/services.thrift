namespace java de.completionary.proxy.thrift.services

typedef i16 short
typedef i32 int

/*
 * Data sent back from the suggestion service
 * @param suggestion The suggested string (equals 'output' of SuggestionField)
 * @param payload The additional data stored with the suggested term
 */
struct Suggestion {
	1: string suggestion;
	//will be a JSON and we have to specify supported base format
	2: string payload;
}

/**
 * Data used to store new terms in the DB
 * 
 * @param ID
 *            Used to reference this field for deletion queries. Must be
 *            unique.
 * @param output
 *            If not null, this string will be displayed returned in case
 *            this term matches a suggestion query
 * @param input
 *            List of strings used for the completion index triggering this
 *            field
 * @param payload
 *            Additional data stored with this term
 * @param weight
 *            Weight of the term
 */
struct SuggestionField {
	1: string ID;
	// May be null
	2: string output;
	// Required
	3: list<string> input;
	//will be a JSON and we have to specify supported base format
	4: string payload;
	// required
	5: int weight;
}

service SuggestionService {
	/**
	 * Returns the top k completions of <query>
	 */
	list<Suggestion> findSuggestionsFor(1: string index, 2: string query, 3: short k),
}

/**
 * Interface for Users of the API to administrate the service
 * 
 * TODO: what happens if a customer wants to have multiple indexes for several autocompletions (this is not reflected by our API and would yield the need to create several accounts)
 **/
service AdminService {
	/**
	 * Adds a single term (SuggestionField) to the DB and refreshes the index.
	 * 
	 * @param ID
	 * 			  Used to reference this field for deletion queries.
	 * @param inputs
	 *            The strings used to build the suggestion index
	 * @param output
	 *            The String to be returned by a complete request if some of the
	 *            inputs are matching. If this element is NULL the matching
	 *            input string will be used instead.
	 * @param payload
	 *            The payload (e.g. images) stored with the field
	 * @param weight
	 *            The weight of the term
	 * 
	 * @throws IOException
	 */
	void addSingleTerm(
			1: string index,
			2: string ID,
			3: list<string> inputs,
			4: string output,
			5: string payload,
			6: int weight),

	/**
	 * Adds a list of terms in one single transaction (see above)
	 */
	void addTerms (1: string index, 2: list<SuggestionField> terms),

	/**
	 * Removes a term from the Database
	 */
	bool deleteTerm(1: string index, 2: string ID),

	/**
	 * Deletes a whole index
	 */
	bool deleteIndex(1: string index),

	/**
	 * Clears an Index (deletes all fields)
	 */
	bool truncateIndex(1: string index),
}
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
	list<SuggestionField> topQueriesSince(1: int date, 2: short k),

	// I want a function that enables to display the currently asked queries. This should be a polling http request

	// I want a function to display how much traffic has been used

	// I want a function that displays the current payment plan
}

/**
 * basic service that enables user login 
 * 
 * 
 **/
service AuthenticationService {
// register account
// delete account
// login
// forgotPassword
}

/**
 * 
 * 
 * 
 **/
service PaymentService {
// buySubscription(),
// cancleSubscription(),
// upgradeSubscription(),
// warnUser(), // e.g. queries start increase over subscription limit (does this maybe have to be in a monitoring service?)
}
