package de.completionary.proxy.analytics;

import org.rrd4j.DsType;

import static org.rrd4j.DsType.GAUGE;
import static org.rrd4j.DsType.DERIVE;

public enum AnalyticsField {
    CurrentUsers(0, GAUGE),
    Queries(1, DERIVE),
    SelectedSuggestions(2, DERIVE),
    Sessions(3, DERIVE),
    ShownSuggestions(4, DERIVE);

    public final int ID;

    public final DsType dataSourceType;

    private AnalyticsField(
            final int ID,
            final DsType dataSourceType) {
        this.ID = ID;
        this.dataSourceType = dataSourceType;
    }
}
