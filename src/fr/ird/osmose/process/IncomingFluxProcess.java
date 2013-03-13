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
        biomassFluxIn = getConfiguration().getFloat("flux.incoming.biomass.sp" + index);
        meanLengthIn = getConfiguration().getFloat("flux.incoming.size.sp" + index);
        ageMeanIn = (int) Math.round(getConfiguration().getFloat("flux.incoming.age.sp" + index) * getConfiguration().getNumberTimeStepsPerYear());
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
        int nSchool = getConfiguration().getSeed();
        if (abundanceIn > 0 && abundanceIn < nSchool) {
            getPopulation().add(new School(species, abundanceIn, meanLengthIn, meanWeigthIn, ageMeanIn));
        } else if (abundanceIn >= nSchool) {
            int mod = (int) (abundanceIn % nSchool);
            int abdSchool = (int) (abundanceIn / nSchool);
            for (int i = 0; i < nSchool; i++) {
                abdSchool += (i < mod) ? 1 : 0;
                getPopulation().add(new School(species, abdSchool, meanLengthIn, meanWeigthIn, ageMeanIn));
            }
        }
    }
}
