package de.completionary.proxy.streaming;

public class IndexAndSampleSize {

    public final String index;

    public final int sampleSize;

    public IndexAndSampleSize(
            String index,
            int sampleSize) {
        this.index = index;
        this.sampleSize = sampleSize;
    }

    @Override
    public int hashCode() {
        return index.hashCode() + sampleSize;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        IndexAndSampleSize other = (IndexAndSampleSize) obj;
        if (sampleSize != other.sampleSize)
            return false;
        if (!index.equals(other.index))
            return false;
        return true;
    }
}
