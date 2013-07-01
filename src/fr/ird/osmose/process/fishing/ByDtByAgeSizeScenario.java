/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process.fishing;

import au.com.bytecode.opencsv.CSVReader;
import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author pverley
 */
public class ByDtByAgeSizeScenario extends AbstractFishingScenario {

    /**
     * Fishing mortality rates by timestep and by age/size class.
     */
    private float[][] f;
    /**
     * Size thresholds in centimeter. Size stage k means thresold[k] <= age <
     * threshold[k+1]
     */
    private float[] sizeThreshold;
    /**
     * Age thresholds in time steps. It is provided in year in the input file
     * and converted in the init function in number of time steps. Age stage k
     * means thresold[k] <= ageDt < threshold[k+1]
     */
    private int[] ageThreshold;

    public ByDtByAgeSizeScenario(int iSimulation, Species species) {
        super(iSimulation, species);
    }

    @Override
    public void init() {
        int iSpec = getIndexSpecies();
        if (!getConfiguration().isNull("mortality.fishing.byDt.byAge.file.sp" + iSpec)) {
            readCSVFile(getConfiguration().getFile("mortality.fishing.byDt.byAge.file.sp" + iSpec), "byAge");
        } else if (!getConfiguration().isNull("mortality.fishing.byDt.bySize.file.sp" + iSpec)) {
            readCSVFile(getConfiguration().getFile("mortality.fishing.byDt.bySize.file.sp" + iSpec), "bySize");
        } else {
            getLogger().log(Level.SEVERE, "Could not found parameters mortality.fishing.byDt.byAge/bySize.file.sp{0}", iSpec);
        }
    }

    private void readCSVFile(String filename, String criterion) {

        try {
            // 1. Open the CSV file
            CSVReader reader = new CSVReader(new FileReader(filename), ';');
            List<String[]> lines = reader.readAll();
            // 2. Read the threshold values
            String[] threshold = lines.get(0);
            if (criterion.equalsIgnoreCase("byAge")) {
                // 2.1 By age
                ageThreshold = new int[threshold.length - 1];
                for (int k = 0; k < ageThreshold.length; k++) {
                    // Converts age in year into number of time steps
                    ageThreshold[k] = (int) Math.round(Float.valueOf(threshold[k + 1]) * getConfiguration().getNStepYear());
                }
            } else if (criterion.equalsIgnoreCase("bySize")) {
                // 2.2 By size
                sizeThreshold = new float[threshold.length - 1];
                for (int k = 0; k < sizeThreshold.length; k++) {
                    sizeThreshold[k] = Float.valueOf(threshold[k + 1]);
                }
            }
            // 3. Read the mortality rates
            f = new float[lines.size() - 1][];
            for (int t = 0; t < lines.size() - 1; t++) {
                f[t] = new float[threshold.length - 1];
                String[] fval = lines.get(t + 1);
                for (int k = 0; k < f[t].length; k++) {
                    f[t][k] = Float.valueOf(fval[k + 1]);
                }
            }
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Error reading CSV file " + filename, ex);
        }
    }

    @Override
    public float getInstantaneousRate(School school) {
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
                if ((sizeThreshold[k] < length) && (length < sizeThreshold[k + 1])) {
                    return f[getSimulation().getIndexTimeSimu()][k];
                }
            }
            // 3. length >= threshold[last]
            return f[getSimulation().getIndexTimeSimu()][sizeThreshold.length - 1];
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
                if ((ageThreshold[k] < age) && (age < ageThreshold[k + 1])) {
                    return f[getSimulation().getIndexTimeSimu()][k];
                }
            }
            // 3. age >= threshold[last]
            return f[getSimulation().getIndexTimeSimu()][ageThreshold.length - 1];
        }
        // We should never reach that stage. If we do it is because there is
        // something wrong in the thresholds and then we return a NaN value.
        return Float.NaN;
    }

    @Override
    public float getAnnualRate() {
        double F = 0;
        for (int k = 0; k < f.length; k++) {
            for (int iStep = 0; iStep < f[k].length; iStep++) {
                F += f[k][iStep];
            }
        }
        F = F / (f.length * getConfiguration().getNYear());
        return (float) F;
    }
}
