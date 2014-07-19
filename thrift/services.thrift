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
	3: string payload;
	4: int weight;
}


service SuggestionService {
        list<Suggestion> findSuggestionsFor(1: string query, 2: short size),
}

service AdminService {
        void addSingleTerm(	1: list<string> inputs,
		 		2: string output,
            			3: string payload,
				4: int weight),

	void addTerms (1: list<SuggestionField> terms),
}