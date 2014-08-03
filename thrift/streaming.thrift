namespace java de.completionary.proxy.thrift.services.streaming

include "exceptions.thrift"

/*
 * Including a common.thrift with typedefs only works if you call
 * the type via common.int -> that's ugly so let's define it here
 */
typedef i16 short
typedef i32 int
typedef i64 long

exception UnableToConnectToStreamingClientException {
	1: string message;
}


/**
 * A Container object to transfer realtime statistics of the last second
 * about one suggest index
 * 
 * Even though we write last second in the documentation this could be an
 * average over a time window. The window size is also transferred.
 * 
 * @param numberOfCurrentUsers
 *            how many unique users have been interacting with the api in
 *            the last second
 * @param numberOfQueries
 *            absolute number of queries that have been fulfilled in the
 *            last second
 * @param randomSampleOfCurrentCompletedTerms
 *            : A random sample of successfully completed terms
 * @param numberOfSelectedSuggestions
 *            : absolute number of how many terms have been selected in the
 *            last second
 * @param conversionRate
 *            : relative number of search sessions in which a term has been
 *            selected from the autocompletion
 */
struct StreamedStatisticsField {
	1: int numberOfCurrentUsers;
	2: int numberOfQueries;
	3: list<string> randomSampleOfCurrentCompletedTerms;
	4: int numberOfSelectedSuggestions;
	5: double conversionRate;
	6: int numberOfShownSuggestions
}

/**
 * The StreamingService allows a client to connect to data streams of various 
 * indices. It will receive every second an update with statistics about all 
 * requested indices by using the StreamingClientService
 **/
service StreamingService {
	/**
	 * Allows a client to register an index for a statistics stream. The server
	 * will connect to hostname:port end send all registered statistics via
	 * updateStatistics every second
	 * 
	 * @param index
	 *            The index to be registered
	 * @param hostName
	 *            The hostname of the machine that should receive the stream
	 * @param port
	 *            The port number of the stream receiver at the client side
	 */
	void establishStream(1: string index, 2: string hostName, 3: int port) throws (1: exceptions.IndexUnknownException indexUnkown, 2: UnableToConnectToStreamingClientException unableToConnect);

	/**
	 * Allows a client to remove an index from the statistics stream
	 * 
	 * @param index
	 *            The index to be unregistered
	 * @param hostName
	 *            The hostname of the machine that should receive the stream
	 * @param port
	 *            The port number of the stream receiver at the client side
	 */
	void disconnectStream(1: string index, 2: string hostName, 3: int port);

	/**
	 * Forces the server not to send statistics to the client anymore
	 * TODO: discuss if the set of indexes to which the client was connected will be removed (I would suggest to do so)
	 */
	void disconnectFromStatisticStream(1: string hostName, 2: int port);
}

/**
 * This service must be run on the streaming client side to receive statistics streams
 */
service StreamingClientService {
	/**
	 * Allows the server to push a map with stream items every second to 
	 * the registered client:
	 * Key: Index
	 * Value: statistics of the last second
	 */
	/*oneway*/ void updateStatistics(1: map<string, StreamedStatisticsField> stream);
}