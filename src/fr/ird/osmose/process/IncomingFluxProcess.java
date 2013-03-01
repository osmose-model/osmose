package fr.ird.osmose.process;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;

/**
 *
 * @author pverley
 */
public class IncomingFluxProcess extends AbstractProcess {

    private Species species;
    /*
     * Annual flux of incoming biomass in tons
     */
    private float biomassFluxIn;
    /*
     * Mean length of incomimg fish
     */
    private float meanLengthIn;
    /*
     * Mean weight of incoming fish
     */
    private int ageMeanIn;
    final private ReproductionProcess parent;

    public IncomingFluxProcess(ReproductionProcess parent, int replica, Species species) {
        super(replica);
        this.parent = parent;
        this.species = species;
    }

    @Override
    public void init() {
        int index = species.getIndex();
        biomassFluxIn = getConfiguration().biomassFluxIn[index];
        meanLengthIn = getConfiguration().meanLengthFishIn[index];
        ageMeanIn = (int) Math.round(getConfiguration().meanAgeFishIn[index] * getConfiguration().getNumberTimeStepsPerYear());
    }

    @Override
    public void run() {
        /*
         * Making cohorts going up to the upper age class
         * Kill old schools
         */
        for (School school : getPopulation().getSchools(species)) {
            school.incrementAge();
        }

        /*
         * Incoming flux
         */
        double season = parent.getSeason(getSimulation().getIndexTimeSimu(), species);
        double biomassIn = biomassFluxIn * season;
        float meanWeigthIn = (float) species.computeWeight(meanLengthIn);
        long abundanceIn = (long) Math.round(biomassIn * 1000000.d / meanWeigthIn);
        int nbSchools = getConfiguration().nSchool;
        if (abundanceIn > 0 && abundanceIn < nbSchools) {
            getPopulation().add(new School(species, abundanceIn, meanLengthIn, meanWeigthIn, ageMeanIn));
        } else if (abundanceIn >= nbSchools) {
            int mod = (int) (abundanceIn % nbSchools);
            int abdSchool = (int) (abundanceIn / nbSchools);
            for (int i = 0; i < nbSchools; i++) {
                abdSchool += (i < mod) ? 1 : 0;
                getPopulation().add(new School(species, abdSchool, meanLengthIn, meanWeigthIn, ageMeanIn));
            }
        }
    }
}
