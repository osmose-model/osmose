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

    public IncomingFluxProcess(Species species) {
        this.species = species;
    }

    @Override
    public void init() {
        int numSerie = getOsmose().numSerie;
        int index = species.getIndex();
        biomassFluxIn = getOsmose().biomassFluxInTab[numSerie][index];
        meanLengthIn = getOsmose().meanLengthFishInTab[numSerie][index];
        ageMeanIn = (int) Math.round(getOsmose().meanAgeFishInTab[numSerie][index] * getSimulation().getNumberTimeStepsPerYear());
    }

    @Override
    public void run() {
        /*
         * Making cohorts going up to the upper age class
         * Kill old schools
         */
        for (School school : getSimulation().getSchools(species)) {
            school.incrementAge();
            if (school.getAgeDt() > (species.getLongevity() - 1)) {
                school.kill();
            }
        }

        /*
         * Incoming flux
         */
        double biomassIn = biomassFluxIn * species.seasonSpawning[getSimulation().getIndexTimeYear()];
        float meanWeigthIn = (float) species.computeWeight(meanLengthIn);
        long abundanceIn = (long) Math.round(biomassIn * 1000000.d / meanWeigthIn);
        int nbSchools = getOsmose().nbSchools[getOsmose().numSerie];
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
