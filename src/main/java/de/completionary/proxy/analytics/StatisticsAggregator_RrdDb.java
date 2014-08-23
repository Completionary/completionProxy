package de.completionary.proxy.analytics;

import static de.completionary.proxy.analytics.AnalyticsField.CurrentUsers;
import static de.completionary.proxy.analytics.AnalyticsField.Queries;
import static de.completionary.proxy.analytics.AnalyticsField.SelectedSuggestions;
import static de.completionary.proxy.analytics.AnalyticsField.Sessions;
import static de.completionary.proxy.analytics.AnalyticsField.ShownSuggestions;
import static org.rrd4j.ConsolFun.AVERAGE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.rrd4j.core.FetchData;
import org.rrd4j.core.FetchRequest;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;

import de.completionary.proxy.helper.ProxyOptions;
import de.completionary.proxy.thrift.services.streaming.StreamedStatisticsField;

public class StatisticsAggregator_RrdDb extends AStatisticsAggregator {

    private final String index;

    private final RrdDb rrdDb;

    private final Sample sample;

    private static List<StatisticsAggregator_RrdDb> Instances =
            new ArrayList<>();

    static {
        final int[] resolutions =
                ProxyOptions.getIntList(ProxyOptions.ANALYTICS_RESOLUTIONS);

        /*
         * Run a timer to update the RRD DB a bit more often then the minimum
         * time resolution. The smallest resolution equals the heart beat time
         * which means we have to updated at least once between two heart beats
         * or we'll have NAN (unknown) values
         */
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                for (StatisticsAggregator_RrdDb aggregator : Instances) {
                    try {
                        aggregator.storeCurrentValuesInDB();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, resolutions[0] * 900, resolutions[0] * 900);
        System.out.println("StatisticsDispatcher started");
    }

    public StatisticsAggregator_RrdDb(
            final String index,
            long indexSize,
            long numberOfQueriesThisMonth) {
        super(indexSize, numberOfQueriesThisMonth);

        this.index = index;
        try {
            this.rrdDb = openDB();
            this.sample = rrdDb.createSample();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }

        Instances.add(this);
    }

    /**
     * Opens the RrDb storing Analytics data for the index
     * 
     * @return A new RrDb object connected to the file storing the analytics
     *         history for the index
     * @throws IOException
     */
    private RrdDb openDB() throws IOException {
        final RrdDef rrdDef = generateRrdDef();
        System.out.println("Opening RRD file");
        RrdDb rrdDb = new RrdDb(rrdDef);
        if (rrdDb.getRrdDef().equals(rrdDef)) {
            System.out.println("Checking RRD file structure... OK");
        } else {
            throw new IOException("Invalid RRD file created.");
        }

        return rrdDb;
    }

    /**
     * Generates a RrDef storing all settings corresponding to the Settings from
     * ProxyOptions
     * 
     * @return A new RrdDef object storing all settings needed to create a new
     *         RrdDb accessing the rrd file for this index
     */
    private RrdDef generateRrdDef() {
        final int[] resolutions =
                ProxyOptions.getIntList(ProxyOptions.ANALYTICS_RESOLUTIONS);
        final int[] rows =
                ProxyOptions
                        .getIntList(ProxyOptions.ANALYTICS_RESOLUTION_STORE_ROWS);

        if (resolutions.length != rows.length) {
            throw new RuntimeException(
                    "The number of fields in ANALYTICS_RESOLUTIONS and ANALYTICS_RESOLUTION_STORE_TIMES must be euqual");
        }

        RrdDef rrdDef = new RrdDef(generateFilePath(), resolutions[0]);
        rrdDef.setVersion(2);

        /*
         * Define all data sources (AnalyticsFields)
         */
        for (AnalyticsField field : AnalyticsField.values()) {
            rrdDef.addDatasource(field.toString(), field.dataSourceType,
                    resolutions[0] /* heart beat */, 0, Double.NaN);
        }

        /*
         * Define the time resolutions
         * 1s step means that we have to updated at least once a second.
         * Otherwise we'll have a NAN stored (unknown value)
         * see http://oss.oetiker.ch/rrdtool/doc/rrdcreate.en.html
         */
        for (int i = 0; i != resolutions.length; i++) {
            rrdDef.addArchive(AVERAGE, 0.5, resolutions[i] /*
                                                            * number of seconds
                                                            * per step
                                                            */, rows[i]);
        }

        System.out.println("Estimated file size: " + rrdDef.getEstimatedSize());
        return rrdDef;
    }

    /**
     * Generates the path to the rrd file to be used for this DB
     */
    private String generateFilePath() {
        return ProxyOptions.ANALYTICS_STORAGE_DIR + "/" + index + ".rrd";
    }

    /**
     * Writes all values stored in [currentValues] to the rrd DB and resets the
     * current values
     * 
     * @throws IOException
     */
    public void storeCurrentValuesInDB() throws IOException {
        sample.setTime(System.currentTimeMillis() / 1000L);

        sample.setValue(CurrentUsers.ID, activeUsers.size());
        sample.setValue(Queries.ID, numberOfQueries.getAndSet(0));
        sample.setValue(SelectedSuggestions.ID,
                numberOfSelectedSuggestions.getAndSet(0));
        sample.setValue(Sessions.ID, numberOfSearchSessions.getAndSet(0));
        sample.setValue(ShownSuggestions.ID,
                numberOfShownSuggestions.getAndSet(0));

        sample.update();
    }
    
    public List<StreamedStatisticsField> getStatistics(){
//        FetchRequest request = rrdDb.createFetchRequest(AVERAGE, start, end);
//        println(request.dump());
//        FetchData fetchData = request.fetchData();
//        println("== Data fetched. " + fetchData.getRowCount()
//                + " points obtained");
//        println(fetchData.toString());
//        println("== Fetch completed");
        return null;
    }
}
