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
     * Natural mortality rates year-1.
     */
    private float D[];
    /**
     *
     */
    private GridMap[] spatialD;
    /**
     * Larval mortality rates, timestep-1.
     */
    private float[][] larvalMortalityRates;

    public NaturalMortalityProcess(int indexSimulation) {
        super(indexSimulation);
    }

    @Override
    public void init() {

        D = new float[getNSpecies()];
        for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
            D[iSpec] = getConfiguration().getFloat("mortality.natural.rate.sp" + iSpec);
        }

        larvalMortalityRates = new float[getNSpecies()][getConfiguration().getNStepYear() * getConfiguration().getNYear()];
        for (int iSpec = 0; iSpec < getConfiguration().getNSpecies(); iSpec++) {
            for (int iStep = 0; iStep < larvalMortalityRates[iSpec].length; iStep++) {
                larvalMortalityRates[iSpec][iStep] = getConfiguration().getFloat("mortality.natural.larva.rate.sp" + iSpec);
            }
        }

        spatialD = new GridMap[getNSpecies()];
        List<String> keys = getConfiguration().findKeys("mortality.natural.rate.file.sp*");
        if (keys != null && !keys.isEmpty()) {
            for (int iSpec = 0; iSpec < getConfiguration().getNSpecies(); iSpec++) {
                if (!getConfiguration().isNull("mortality.natural.rate.file.sp" + iSpec)) {
                    spatialD[iSpec] = readCSVMap(getConfiguration().getFile("mortality.natural.rate.file.sp" + iSpec));
                }
            }
        }
    }

    @Override
    public void run() {
        // Natural mortality (due to other predators)
        for (School school : getSchoolSet()) {
            double M = getNaturalMortalityRate(school, 1);
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
    public double getNaturalMortalityRate(School school, int subdt) {
        double M;
        Species spec = school.getSpecies();
        if (school.getAgeDt() == 0) {
            M = (larvalMortalityRates[spec.getIndex()][getSimulation().getIndexTimeSimu()]) / (float) subdt;
        } else {
            if (null != spatialD[spec.getIndex()] && !school.isUnlocated()) {
                M = (spatialD[spec.getIndex()].getValue(school.getCell())) / (float) (getConfiguration().getNStepYear() * subdt);
            } else {
                M = (D[spec.getIndex()]) / (float) (getConfiguration().getNStepYear() * subdt);
            }
        }
        return M;
    }

    /*
     * The annual mortality rate is calculated as the annual average of
     * the larval mortality rates over the years.
     */
    public double getLarvalMortalityRate(Species species) {

        double rate = 0.d;
        int iSpec = species.getIndex();
        for (int iStep = 0; iStep < larvalMortalityRates[iSpec].length; iStep++) {
            rate += larvalMortalityRates[iSpec][iStep];
        }
        rate /= larvalMortalityRates[iSpec].length;
        return rate;
    }

    public double getNaturalMortalityRate(Species species) {
        return D[species.getIndex()];
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
