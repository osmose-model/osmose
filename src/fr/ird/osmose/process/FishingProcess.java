package fr.ird.osmose.process;

import au.com.bytecode.opencsv.CSVReader;
import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pverley
 */
public class FishingProcess extends AbstractProcess {

    /*
     * Whether fishing rates are the same every year or change throughout the
     * years of simulation
     */
    private boolean isFishingInterannual;
    /*
     * Fishing mortality rates
     */
    private float[][] fishingRates;

    public FishingProcess(int replica) {
        super(replica);
    }

    @Override
    public void init() {
        isFishingInterannual = false;
        int nStepYear = getConfiguration().getNumberTimeStepsPerYear();
        fishingRates = new float[getConfiguration().getNSpecies()][nStepYear];
        for (int i = 0; i < getNSpecies(); i++) {
            double F = getConfiguration().getFloat("mortality.fishing.rate.sp" + i);
            String filename = getConfiguration().resolveFile(getConfiguration().getString("mortality.fishing.season.distrib.file.sp" + i));
            CSVReader reader;
            try {
                reader = new CSVReader(new FileReader(filename), ';');
                List<String[]> lines = reader.readAll();
                if ((lines.size() - 1) % nStepYear != 0) {
                    // @TODO throw error
                }
                for (int t = 0; t < fishingRates[i].length; t++) {
                    double season = Double.valueOf(lines.get(t + 1)[1]);
                    fishingRates[i][t] = (float) (F * season / 100.d);
                    //System.out.println("spec " + i + " f: " + fishingRates[i][t]);
                }
            } catch (IOException ex) {
                getLogger().log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void run() {
        for (School school : getPopulation().getPresentSchools()) {
            if (school.getAbundance() != 0.d) {
                double F = getFishingMortalityRate(school, 1);
                double nDead = school.getInstantaneousAbundance() * (1 - Math.exp(-F));
                if (nDead > 0.d) {
                    school.setNdeadFishing(nDead);
                }
            }
        }
    }

    public double getFishingMortalityRate(School school, int subdt) {
        if (isFishable(school)) {
            int iStep = isFishingInterannual
                    ? getSimulation().getIndexTimeSimu()
                    : getSimulation().getIndexTimeYear();
            return fishingRates[school.getSpeciesIndex()][iStep] / subdt;
        } else {
            return 0.d;
        }
    }

    private boolean isFishable(School school) {
        // Test whether fishing applies to this school
        // 1. School is recruited
        // 2. School is catchable (no MPA)
        // 3. School is not out of simulated domain
        return (school.getAgeDt() >= school.getSpecies().getRecruitmentAge())
                && school.isCatchable()
                && !school.isUnlocated();
    }

    /*
     * F the annual mortality rate is calculated as the annual average
     * of the fishing rates over the years. 
     */
    public double getFishingMortalityRate(Species species) {
        double F = 0;
        int iSpec = species.getIndex();
        for (int iStep = 0; iStep < fishingRates[iSpec].length; iStep++) {
            F += fishingRates[iSpec][iStep];
        }

        if (isFishingInterannual) {
            F /= getConfiguration().getNYear();
        }
        return F;
    }
}
