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

    public FishingProcess(int replica) {
        super(replica);
    }

    @Override
    public void init() {
        isFishingInterannual = getConfiguration().fishingRates[0].length > getConfiguration().getNumberTimeStepsPerYear();
        fishingRates = new float[getConfiguration().getNSpecies()][];
        System.arraycopy(getConfiguration().fishingRates, 0, fishingRates, 0, getConfiguration().getNSpecies());
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
