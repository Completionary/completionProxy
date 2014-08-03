namespace java de.completionary.proxy.thrift.services.admin

/*
 * Including a common.thrift with typedefs only works if you call
 * the type via common.int -> that's ugly so let's define it here
 */
typedef i16 short
typedef i32 int
typedef i64 long

/**
 * Data used to store new terms in the DB
 * 
 * @param ID
 *            Used to reference this field for deletion queries. Must be
 *            unique.
 * @param outputField
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
	2: string outputField;
	// Required
	3: list<string> input;
	//will be a JSON and we have to specify supported base format
	4: string payload;
	// required
	5: int weight;
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
	 * @param outputField
	 *            The String to be returned by a complete request if some of the
	 *            inputs are matching. If this element is NULL the matching
	 *            input string will be used instead.
	 * @param payload
	 *            The payload (e.g. images) stored with the field
	 * @param weight
	 *            The weight of the term
	 * @return Number of milliseconds passed on the server side
	 *
	 * @throws IOException
	 */
	long addSingleTerm(
			1: string index,
			2: string ID,
			3: list<string> inputs,
			4: string outputField,
			5: string payload,
			6: int weight);

	/**
	 * Adds a list of terms in one single transaction (see above)
	 * @return Number of milliseconds passed on the server side
	 */
	long addTerms (1: string index, 2: list<SuggestionField> terms);

	/**
	 * Removes a term from the Database
	 */
	bool deleteSingleTerm(1: string index, 2: string ID);

	/**
	 * Removes several terms from the Database
	 * @return Number of milliseconds passed on the server side
	 */
	long deleteTerms(1: string index, 2: list<string> ID);

	/**
	 * Deletes a whole index
	 */
	bool deleteIndex(1: string index);

	/**
	 * Clears an Index (deletes all fields)
	 * @return The time in milliseconds spend on the server side
	 */
	long truncateIndex(1: string index);
}