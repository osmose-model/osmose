/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process.naturalmortality;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import fr.ird.osmose.process.AbstractMortalityScenario;
import fr.ird.osmose.util.ByClassTimeSeries;
import java.util.logging.Level;

/**
 * Natural mortality rates provided in a time series and 
 * @author pverley
 */
public class ByDtByAgeSizeNaturalMortalityScenario extends AbstractMortalityScenario {

    /**
     * Natural mortality rates by time step and by age/size class.
     */
    private float[][] m;
    /**
     * Size thresholds in centimeter. Size stage k means {@code threshold[k] <= age < threshold[k+1]}
     */
    private float[] sizeThreshold;
    /**
     * Age thresholds in time steps. It is provided in year in the input file
     * and converted in the {@code init} function in number of time steps. Age
     * stage k means {@code threshold[k] <= ageDt < threshold[k+1]}
     */
    private int[] ageThreshold;

    public ByDtByAgeSizeNaturalMortalityScenario(int iSimulation, Species species) {
        super(iSimulation, species);
    }

    @Override
    public void init() {
        int iSpec = getIndexSpecies();
        if (!getConfiguration().isNull("mortality.natural.rate.byDt.byAge.file.sp" + iSpec)) {
            ByClassTimeSeries timeSerieByAge = new ByClassTimeSeries(getIndexSimulation());
            timeSerieByAge.read(getConfiguration().getFile("mortality.natural.rate.byDt.byAge.file.sp" + iSpec));
            m = timeSerieByAge.getValues();
            ageThreshold = new int[timeSerieByAge.getNClass() - 1];
            for (int k = 0; k < ageThreshold.length; k++) {
                // Converts age in year into number of time steps
                ageThreshold[k] = (int) Math.round(timeSerieByAge.getThreshold(k) * getConfiguration().getNStepYear());
            }
        } else if (!getConfiguration().isNull("mortality.natural.rate.byDt.bySize.file.sp" + iSpec)) {
            ByClassTimeSeries timeSerieBySize = new ByClassTimeSeries(getIndexSimulation());
            timeSerieBySize.read(getConfiguration().getFile("mortality.natural.rate.byDt.bySize.file.sp" + iSpec));
            m = timeSerieBySize.getValues();
            sizeThreshold = timeSerieBySize.getThresholds();
        } else {
            getLogger().log(Level.SEVERE, "Could not found parameters mortality.natural.rate.byDt.byAge/bySize.file.sp{0}", iSpec);
        }
    }

    @Override
    public float getInstantaneousRate(School school) {
        
        int iSimu = getSimulation().getIndexTimeSimu();
        if (null != sizeThreshold) {
            // By size class
            float length = school.getLength();
            // 1. Length < value of the first size threshold, it means there is
            // no value provided. Osmose assume it is zero.
            if (length < sizeThreshold[0]) {
                return 0.f;
            }
            // 2. Normal case thresold[k] <= length < threshold[k+1]
            for (int k = 0; k < sizeThreshold.length - 1; k++) {
                if ((sizeThreshold[k] <= length) && (length < sizeThreshold[k + 1])) {
                    return m[iSimu][k];
                }
            }
            // 3. length >= threshold[last]
            return m[iSimu][sizeThreshold.length - 1];
        } else if (null != ageThreshold) {
            // By age class
            float age = school.getAgeDt();
            // 1. age < threshold[0] it means there is no value provided.
            // Osmose assume it is zero.
            if (age < ageThreshold[0]) {
                return 0.f;
            }
            // 2. Normal case thresold[k] <= age < threshold[k+1]
            for (int k = 0; k < ageThreshold.length - 1; k++) {
                if ((ageThreshold[k] <= age) && (age < ageThreshold[k + 1])) {
                    return m[iSimu][k];
                }
            }
            // 3. age >= threshold[last]
            return m[iSimu][ageThreshold.length - 1];
        }
        // We should never reach that stage. If we do it is because there is
        // something wrong in the thresholds and then we return a NaN value.
        return Float.NaN;
    }

    @Override
    public float getAnnualRate() {
        double D = 0;
        for (int iStep = 0; iStep < m.length; iStep++) {
            for (int k = 0; k < m[iStep].length; k++) {
                D += m[iStep][k];
            }
        }
        D = D / (m.length * getConfiguration().getNYear());
        return (float) D;
    }
}
