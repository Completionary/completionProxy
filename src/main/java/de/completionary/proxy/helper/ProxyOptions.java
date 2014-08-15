package de.completionary.proxy.helper;

import java.lang.invoke.MethodHandles;

public class ProxyOptions extends Options {

    public static String ES_CLUSTER_NAME;

    public static int ADMIN_SERVER_PORT;

    public static int ANALYTICS_SERVER_PORT;

    public static int STREAMING_SERVER_PORT;

    public static int SUGGESTION_SERVER_BINARY_PORT;
    
    public static int SUGGESTION_SERVER_HTTP_PORT;

    /*
     * Large bulk imports should be split into several bulk requests with the
     * maximum size of [SPLIT_BULK_REQUEST_SIZE]
     */
    public static int SPLIT_BULK_REQUEST_SIZE;

    public static int MAX_NUMBER_OF_SAMPLE_TERMS_IN_STREAM;

    static {
        try {
            Options.initialize("/etc/completionary/proxyOptions.cfg",
                    MethodHandles.lookup().lookupClass());
        } catch (IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
