namespace java de.completionary.proxy.thrift.services.admin

include "exceptions.thrift"

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
 *            Additional data stored with this term. The standard format
 *            is the following JSON:
 *            {"href":"$URL","image":"$URL_or_BASE64Image"}
 * @param weight
 *            Weight of the term
 */
struct SuggestionField {
	1: long ID;
	// May be null
	2: string outputField;
	// Required
	3: list<string> input;
	// Additinoal data to be stored
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
 	 * @param apiToken
	 *			Must be given in any call to verify the identity of the api user
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
			1: string apiToken,
			2: string index,
			3: long ID,
			4: list<string> inputs,
			5: string outputField,
			6: string payload,
			7: int weight) throws (1: exceptions.InvalidIndexNameException invalidIndexException, 2: exceptions.ServerDownException serverDownException);

	/**
	 * Adds a list of terms in one single transaction (see above)
	 * @return Number of milliseconds passed on the server side
	 */
	long addTerms (1: string apiToken, 2: string index, 3: list<SuggestionField> terms) throws (1: exceptions.InvalidIndexNameException invalidIndexException, 2: exceptions.ServerDownException serverDownException);

	/**
	 * Removes a term from the Database
	 */
	bool deleteSingleTerm(1: string apiToken, 2: string index, 3: long ID) throws (1: exceptions.InvalidIndexNameException invalidIndexException, 2: exceptions.ServerDownException serverDownException);

	/**
	 * Removes several terms from the Database
	 * @return Number of milliseconds passed on the server side
	 */
	long deleteTerms(1: string apiToken, 2: string index, 3: list<long> ID) throws (1: exceptions.InvalidIndexNameException invalidIndexException, 2: exceptions.ServerDownException serverDownException);

	/**
	 * Deletes a whole index
	 */
	bool deleteIndex(1: string apiToken, 2: string index) throws (1: exceptions.InvalidIndexNameException invalidIndexException, 2: exceptions.ServerDownException serverDownException);

	/**
	 * Clears an Index (deletes all fields)
	 * @return The time in milliseconds spend on the server side
	 */
	long truncateIndex(1: string apiToken, 2: string index) throws (1: exceptions.InvalidIndexNameException invalidIndexException, 2: exceptions.ServerDownException serverDownException);
}