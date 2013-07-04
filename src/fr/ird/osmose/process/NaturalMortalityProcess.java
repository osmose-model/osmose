package fr.ird.osmose.process;

import au.com.bytecode.opencsv.CSVReader;
import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import fr.ird.osmose.util.GridMap;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

/**
 *
 * @author pverley
 */
public class NaturalMortalityProcess extends AbstractProcess {

    /**
     * Natural mortality rates (time step)^-1.
     */
    private float D[][];
    /**
     * Spatial factor for natural mortality [0, 1]
     */
    private GridMap[] spatialD;
    /**
     * Larval mortality rates, (time step)^-1.
     */
    private float[][] Dlarva;

    public NaturalMortalityProcess(int indexSimulation) {
        super(indexSimulation);
    }

    @Override
    public void init() {

        int nsteps = getConfiguration().getNStepYear() * getConfiguration().getNYear();

        D = new float[getNSpecies()][nsteps];
        if (!getConfiguration().isNull("mortality.natural.rate.file")) {
            // Seasonal or interannual natural mortality rates
            D = readCSVRates(getConfiguration().getFile("mortality.natural.rate.file"));
        } else {
            // Annual mortality rates
            for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
                float Ds = getConfiguration().getFloat("mortality.natural.rate.sp" + iSpec) / getConfiguration().getNStepYear();
                for (int iStep = 0; iStep < nsteps; iStep++) {
                    D[iSpec][iStep] = Ds;
                }
            }
        }

        Dlarva = new float[getNSpecies()][nsteps];
        if (!getConfiguration().isNull("mortality.natural.larva.rate.file")) {
            // Seasonal or interannual natural mortality rates
            Dlarva = readCSVRates(getConfiguration().getFile("mortality.natural.larva.rate.file"));
        } else {
            for (int iSpec = 0; iSpec < getConfiguration().getNSpecies(); iSpec++) {
                float Ds = getConfiguration().getFloat("mortality.natural.larva.rate.sp" + iSpec);
                for (int iStep = 0; iStep < nsteps; iStep++) {
                    Dlarva[iSpec][iStep] = Ds;
                }
            }
        }

        spatialD = new GridMap[getNSpecies()];
        List<String> keys = getConfiguration().findKeys("mortality.natural.spatial.distrib.file.sp*");
        if (keys != null && !keys.isEmpty()) {
            for (int iSpec = 0; iSpec < getConfiguration().getNSpecies(); iSpec++) {
                if (!getConfiguration().isNull("mortality.natural.spatial.distrib.file.sp" + iSpec)) {
                    spatialD[iSpec] = readCSVMap(getConfiguration().getFile("mortality.natural.spatial.distrib.file.sp" + iSpec));
                }
            }
        }
    }

    @Override
    public void run() {
        // Natural mortality (due to other predators)
        for (School school : getSchoolSet()) {
            double M = getInstantaneousRate(school, 1);
            double nDead = school.getInstantaneousAbundance() * (1.d - Math.exp(-M));
            if (nDead > 0.d) {
                school.setNdeadNatural(nDead);
            }
        }
    }

    /**
     * For all species, D is due to other predators (seals, seabirds) for
     * migrating species, we add mortality because absents during a time step so
     * they don't undergo mortalities due to predation and starvation Additional
     * mortalities for ages 0: no-fecundation of eggs, starvation more
     * pronounced than for sup ages (rel to CC), predation by other species are
     * not explicit.
     */
    public double getInstantaneousRate(School school, int subdt) {
        double M;
        Species spec = school.getSpecies();
        if (school.getAgeDt() == 0) {
            M = (Dlarva[spec.getIndex()][getSimulation().getIndexTimeSimu()]) / (float) subdt;
        } else {
            if (null != spatialD[spec.getIndex()] && !school.isUnlocated()) {
                M = (spatialD[spec.getIndex()].getValue(school.getCell()) * D[spec.getIndex()][getSimulation().getIndexTimeSimu()]) / (float) subdt;
            } else {
                M = (D[spec.getIndex()][getSimulation().getIndexTimeSimu()]) / (float) (subdt);
            }
        }
        return M;
    }

    /*
     * The annual mortality rate is calculated as the annual average of
     * the larval mortality rates over the years.
     */
    public double getLarvalAnnualRate(Species species) {

        double rate = 0.d;
        int iSpec = species.getIndex();
        for (int iStep = 0; iStep < Dlarva[iSpec].length; iStep++) {
            rate += Dlarva[iSpec][iStep];
        }
        rate /= Dlarva[iSpec].length;
        return rate;
    }

    /*
     * The annual mortality rate is calculated as the annual average of
     * the natural mortality rates over the years.
     */
    public double getAnnualRate(Species species) {
        double rate = 0.d;
        int iSpec = species.getIndex();
        for (int iStep = 0; iStep < D[iSpec].length; iStep++) {
            rate += D[iSpec][iStep];
        }
        rate /= D[iSpec].length;
        return rate;
    }

    private float[][] readCSVRates(String filename) {

        int nspecies = getNSpecies();
        int nStepYear = getConfiguration().getNStepYear();
        int nStepSimu = nStepYear * getConfiguration().getNYear();
        float[][] rates = new float[nspecies][nStepSimu];
        try {
            // 1. Open CSV file
            CSVReader reader = new CSVReader(new FileReader(filename), ';');
            List<String[]> lines = reader.readAll();
            // 2. Read natural mortality rates
            int nTimeSerie = lines.size() - 1;
            for (int t = 0; t < nTimeSerie; t++) {
                String[] line = lines.get(t + 1);
                for (int i = 0; i < nspecies; i++) {
                    rates[i][t] = Float.valueOf(line[i + 1]);
                }
            }
            // 3. Check the length of the time serie
            if (nTimeSerie % nStepYear != 0) {
                // Either the time serie is less than a year or it is not a 
                // multiple of number of time step per year.
                throw new IOException("Found " + nTimeSerie + " time steps in the time serie. It must be a multiple of the number of time steps per year.");
            } else if (nTimeSerie < nStepSimu) {
                // There is less season in the file than number of years of the
                // simulation.
                int t = nTimeSerie;
                while (t < nStepSimu) {
                    for (int k = 0; k < nTimeSerie; k++) {
                        for (int i = 0; i < nspecies; i++) {
                            rates[i][t] = rates[i][k];
                        }
                        t++;
                        if (t == nStepSimu) {
                            break;
                        }
                    }
                }
                getLogger().log(Level.WARNING, "Time serie in file {0} only contains {1} steps out of {2}. Osmose will loop over it.", new Object[]{filename, nTimeSerie, nStepSimu});
            } else if (nTimeSerie > nStepSimu) {
                getLogger().log(Level.WARNING, "Time serie in file {0} contains {1} steps out of {2}. Osmose will ignore the exceeding years.", new Object[]{filename, nTimeSerie, nStepSimu});
            }
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Error reading CSV file " + filename, ex);
            System.exit(1);
        }

        return rates;
    }

    private GridMap readCSVMap(String csvFile) {

        GridMap map = null;
        try {
            /*
             * Read the CSV file
             */
            CSVReader reader = new CSVReader(new FileReader(csvFile), ';');
            List<String[]> lines = reader.readAll();
            /*
             * Initialize the map
             */
            map = new GridMap();
            /*
             * Read the map
             */
            int ny = getGrid().get_ny();
            for (int l = 0; l < lines.size(); l++) {
                String[] line = lines.get(l);
                int j = ny - l - 1;
                for (int i = 0; i < line.length; i++) {
                    float val = Float.valueOf(line[i]);
                    if (val > 0.f) {
                        map.setValue(i, j, val);
                    }
                }
            }
            reader.close();
        } catch (IOException ex) {
            getLogger().log(Level.SEVERE, "Error reading natural mortality rate file " + csvFile, ex);
        }
        return map;
    }
}
