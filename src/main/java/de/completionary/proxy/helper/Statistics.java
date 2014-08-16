package de.completionary.proxy.helper;

public class Statistics {

    public static double calculateAverage(long[] values) {
        long sum = 0;
        for (Long l : values) {
            sum += l / 1000;
        }
        return sum / (double) values.length * 1000.;
    }

    public static double calculateStandardDeviation(long[] values) {
        double av = calculateAverage(values);
        double sum = 0;

        for (long value : values) {
            sum += (value - av) * (value - av);
        }

        return Math.sqrt(sum / (values.length - 1));
    }
}
