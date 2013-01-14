package fr.ird.osmose;

/**
 *
 * @author pverley
 */
public class FishingProcess extends AbstractProcess {

    /*
     * Whether fishing rates are the same every year or change throughout the
     * years of simulation
     */
    static public boolean isFishingInterannual;
    /*
     * Fishing mortality rates
     */
    static private float[][] fishingRates;

    @Override
    public void loadParameters() {
        isFishingInterannual = getOsmose().fishingRates[0].length > getSimulation().getNbTimeStepsPerYear();
        System.arraycopy(getOsmose().fishingRates, 0, fishingRates, 0, getSimulation().getNbSpecies());
    }

    @Override
    public void run() {
        for (School school : getSimulation().getPresentSchools()) {
            if (school.getAbundance() != 0.d) {
                double nDead = computeFishingMortality(school, 1);
                if (nDead != 0.d) {
                    school.setAbundance(school.getAbundance() - nDead);
                    if (school.getAbundance() < 1.d) {
                        //nDead = school.getAbundance();
                        school.setAbundance(0);
                        school.kill();
                    }
                    //school.nDeadFishing = nDead;
                }
            }
        }
    }

    public static double computeFishingMortality(School school, int subdt) {

        double F = getFishingMortalityRate(school, subdt);
        double nDead = 0;
        if (F > 0) {
            nDead = getSimulation().getAbundance(school) * (1 - Math.exp(-F));
        }
        return nDead;
    }

    public static double getFishingMortalityRate(School school, int subdt) {
        if (isFishable(school)) {
            int iStep = isFishingInterannual
                    ? getSimulation().getIndexTimeSimu()
                    : getSimulation().getIndexTimeYear();
            return fishingRates[school.getSpeciesIndex()][iStep] / subdt;
        } else {
            return 0.d;
        }
    }

    private static boolean isFishable(School school) {
        // Test whether fishing applies to this school
        // 1. School is recruited
        // 2. School is catchable (no MPA and no out of zone)
        return (school.getAgeDt() >= school.getSpecies().recruitAge)
                && school.isCatchable();
    }
    
    /*
     * F the annual mortality rate is calculated as the annual average
     * of the fishing rates over the years. 
     */
    public static double getFishingMortalityRate(Species species) {
        double F = 0;
        int iSpec = species.getIndex();
        for (int iStep = 0; iStep < fishingRates[iSpec].length; iStep++) {
            F += fishingRates[iSpec][iStep];
        }
        
        if (isFishingInterannual) {
            F /= getOsmose().simulationTimeTab[getOsmose().numSerie];
        }
        return F;
    }
}
