package de.completionary.proxy.elasticsearch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.completionary.proxy.thrift.services.suggestion.AnalyticsData;

/**
 * This class is the central logging handler for any kind of analytics related
 * logs The log is printed to a file with the following format:
 * [logType]\t[optionalLogSpecificFields\t][endUserData] <br/>
 * logType meanings:<br/>
 * 
 * Q: A suggestion query was executed. The query string is the first field after
 * Q<br/>
 * 
 * S: A suggestion was selected. The ID of the selected suggestion is the first
 * field after S<br/>
 * 
 * F: A completion session was finished. [logSpecificFields] is empty in this
 * case
 * 
 * @author Jonas Kunze (kunze.jonas@gmail.com)s
 */

public class AnalyticsLogger {

    final static Logger logger = LoggerFactory.getLogger(AnalyticsLogger.class);

    public AnalyticsLogger() {
        logger.info("Entering application.");
    }

    public void logQuery(
            final AnalyticsData userData,
            final String suggestRequest) {
        StringBuilder builder = new StringBuilder();
        builder.append("Q\t");
        builder.append(suggestRequest);
        builder.append('\t');
        appendUserData(userData, builder);

        logger.info(builder.toString());
    }

    /**
     * 
     * @param suggestionID
     *            The ID of the selected suggestion
     */
    public void logSuggestionSelected(
            final AnalyticsData userData,
            final String suggestionID) {
        StringBuilder builder = new StringBuilder();
        builder.append("S\t");
        builder.append(suggestionID);
        builder.append('\t');
        appendUserData(userData, builder);

        logger.info(builder.toString());
    }

    /**
     * 
     * @param suggestionID
     *            The ID of the selected suggestion
     */
    public void logSessionFinished(final AnalyticsData userData) {
        StringBuilder builder = new StringBuilder();
        builder.append("F\t");
        builder.append('\t');
        appendUserData(userData, builder);

        logger.info(builder.toString());
    }

    private void appendUserData(
            final AnalyticsData userData,
            final StringBuilder builder) {
        builder.append(userData.userID);
        builder.append('\t');
        builder.append(userData.userAgent);
    }
}
