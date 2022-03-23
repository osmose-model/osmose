package fr.ird.osmose.util.timeseries;

import fr.ird.osmose.util.OsmoseLinker;

public class SeasonTimeSeries extends OsmoseLinker {

    private String key;
    private String sufix;
    private double[] values;

    public SeasonTimeSeries(String key, String sufix) {
        this.key = key;
        this.sufix = sufix;
    }

    public void init() {

        // key = mortality.additional.larva.rate.seasonality
        // keyfile = mortality.additional.larva.rate.seasonality.file
        String keyFile = String.format("%s.file.%s", key, sufix);
        String keyVal = String.format("%s.%s", key, sufix);

        // if the season can be defined from a file.
        if (!getConfiguration().isNull(keyFile)) {
            String fileName = getConfiguration().getFile(keyFile);
            SingleTimeSeries seasonSeries = new SingleTimeSeries();
            seasonSeries.read(fileName);
            values = seasonSeries.getValues();
        } else {

            int nStepYear = getConfiguration().getNStepYear();
            int nStep = getConfiguration().getNStep();

            values = new double[nStep];

            // if no values is provided, season is forced as a constant
            if (getConfiguration().isNull(keyVal)) {
                for (int i = 0; i < nStep; i++) {
                    values[i] = (1 / getConfiguration().getNStepYear());
                }
            } else {
                double[] tempValues = getConfiguration().getArrayDouble(keyVal);
                if (tempValues.length == nStepYear) {
                    for (int i = 0; i < nStep; i++) {
                        values[i] = tempValues[i % nStepYear];
                    }
                } else {
                    for (int i = 0; i < nStep; i++) {
                        values[i] = tempValues[i];
                    }
                }
            }
        }
    }

    public double[] getValues() {
        return this.values;
    }
}
