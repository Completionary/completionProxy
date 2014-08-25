package de.completionary.proxy.helper;

import java.lang.invoke.MethodHandles;

public class ProxyOptions extends Options {

    public static String ES_CLUSTER_NAME;

    public static int ADMIN_SERVER_PORT;

    public static int ANALYTICS_SERVER_PORT;

    public static String ANALYTICS_STORAGE_DIR;

    /*
     * Definition of the different time resolutions in seconds (;-separated
     * list)
     */
    public static String ANALYTICS_RESOLUTIONS;

    /*
     * Number of rows (entries) of each resolution (;-separated list)
     */
    public static String ANALYTICS_RESOLUTION_STORE_ROWS;

    public static int STREAMING_SERVER_PORT;

    public static int SUGGESTION_SERVER_BINARY_PORT;

    public static int SUGGESTION_SERVER_HTTP_PORT;

    /*
     * Large bulk imports should be split into several bulk requests with the
     * maximum size of [SPLIT_BULK_REQUEST_SIZE]
     */
    public static int SPLIT_BULK_REQUEST_SIZE;

    public static int MAX_NUMBER_OF_SAMPLE_TERMS_IN_STREAM;
    
    public static boolean ENABLE_LOGGIN;

    static {
        try {
            Options.initialize("/etc/completionary/proxyOptions.cfg",
                    MethodHandles.lookup().lookupClass());
        } catch (IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static String[] getStringList(final String semicolonSeparatedList) {
        return semicolonSeparatedList.split(";");
    }

    public static int[] getIntList(final String semicolonSeparatedList) {
        String[] strings = semicolonSeparatedList.split(";");
        int[] ints = new int[strings.length];

        for (int i = 0; i != strings.length; i++) {
            ints[i] = Integer.parseInt(strings[i]);
        }
        return ints;
    }
}
