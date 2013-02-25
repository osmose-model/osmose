package fr.ird.osmose.process;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;

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

    @Override
    public void init() {
        isFishingInterannual = getOsmose().fishingRates[0].length > getOsmose().getNumberTimeStepsPerYear();
        fishingRates = new float[getOsmose().getNumberSpecies()][];
        System.arraycopy(getOsmose().fishingRates, 0, fishingRates, 0, getOsmose().getNumberSpecies());
    }

    @Override
    public void run() {
        for (School school : getPopulation().getPresentSchools()) {
            if (school.getAbundance() != 0.d) {
                double nDead = computeFishingMortality(school, 1);
                if (nDead != 0.d) {
                    school.setAbundance(school.getAbundance() - nDead);
                    if (school.getAbundance() < 1.d) {
                        //nDead = school.getAbundance();
                        school.setAbundance(0.d);
                    }
                    //school.nDeadFishing = nDead;
                }
            }
        }
    }

    public double computeFishingMortality(School school, int subdt) {

        double F = getFishingMortalityRate(school, subdt);
        double nDead = 0;
        if (F > 0) {
            nDead = school.getInstantaneousAbundance() * (1 - Math.exp(-F));
        }
        return nDead;
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
        return (school.getAgeDt() >= school.getSpecies().recruitAge)
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
            F /= getOsmose().getNumberYears();
        }
        return F;
    }
}
