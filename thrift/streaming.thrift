namespace java de.completionary.proxy.thrift.services

typedef i16 short
typedef i32 int
typedef i65 long

/**
 * A Container object to transfer realtime statistics of the last second about one suggest index
 * 
 * Even though we write last second in the documentation this could be an average over a time window. The window size is also transferred.
 * 
 * @param numberOfCcurrentUsers: how many unique users have been interacting with the api in the last second
 * @param numberOfQueries: absolute number of queries that have been fulfilled in the last second
 * @param randomSampleOfCurrentCompletedTerms: A random sample of successfully completed terms 
 * @param indexSize: number of items that are currently stored in the index (this is never averaged)
 * @param numberOfSelectedSuggestions: absolute number of how many terms have been selected in the last second 
 * @param conversionRate: relative number of search sessions in which a term has been selected from the autocompletion
 * @param numberOfTotalQueriesThisMonth: number of total queries used in this month. (this is never averaged)
 * @param windowSize: the windowsize in milliseconds from which the statistics are averaged.
 */
struct StreamedStatisticsField {
	1: int numberOfCcurrentUsers;
	2: int numberOfQueries;
	3: list<string> randomSampleOfCurrentCompletedTerms;
	4: int indexSize;
	5: int numberOfSelectedSuggestions;
	6: double conversionRate;
	7: int numberOfTotalQueriesThisMonth;
	8: int windowSize;
}
/**
* The StreamingService allows a client to connect to data streams of various 
* indices and will receive every second an update with statistics about all 
* requested indices
**/
service StreamingService {
	/**
	 * adds current statistics of index to the overall statistics stream
	 */
	establishStream(1: string index),
	
	/**
	 * removes current statistics of index from the overall statistics stream 
	 */
	disconnectStream(1: string index),


	/**
	 * this method pushes a map with stream items of (maybe aggregated statistics) every second to the api client
	 */
	map<String, StreamedStatisticField> connectToStatisticStream(),
	
	/**
	 * forces the server not to send statistics to the client anymore
	 * TODO: discuss if the set of indexes to which the client was connected will be removed (I would suggest to do so)
	 */
	disconnectFromStatisticStream(),
}
