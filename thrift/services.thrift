namespace java de.completionary.proxy.thrift

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
 * @param output In case this term matches a suggestion query, this string will be displayed
 * @param input List of strings used for the completion index triggering this field
 * @param payload Additional data stored with this term
 * @param weight Weight of the term
 */
struct SuggestionField {
	1: string output;
	2: list<string> input;
	//will be a JSON and we have to specify supported base format
	3: string payload;
	4: int weight;
}


service SuggestionService {
        list<Suggestion> findSuggestionsFor(1: string query, 2: short size),
}

/**
* Interface for Users of the API to administrate the service
* 
* TODO: what happens if a customer wants to have multiple indexes for several autocompletions (this is not reflected by our API and would yield the need to create several accounts)
**/
service AdminService {
	//adds a single term to the suggest Index
    void addSingleTerm(	1: list<string> inputs,
		 		2: string output,
            	3: string payload,
				4: int weight),

	void addSingleTerm(1: term SuggestionField),

	void addTerms (1: list<SuggestionField> terms),
	
	void deleteTerm(1: term SuggestionField),
	
	void deleteIndex(),	
}
/**
* Serice to retrieve analytics for API customer
* 
* 
* 
**/
service AnalyticsService {
	// retrieves a list of top asked queries (which have been selected from users) the weight in the SuggestioField will be the amount of selections
	list<SuggestionField> topQueriesSince(1: Date, 2: short k),
	
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