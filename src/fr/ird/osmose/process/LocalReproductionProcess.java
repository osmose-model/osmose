
package fr.ird.osmose.process;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import java.util.List;

/**
 *
 * @author pverley
 */
public class LocalReproductionProcess extends AbstractProcess {
    
    private Species species;
    /*
     * Percentage of female in the population
     */
    private double sexRatio;
    /*
     * Number of eggs per gram of mature female
     */
    private double alpha;
    
    public LocalReproductionProcess(Species species) {
        this.species = species;
    }

    @Override
    public void init() {
        int numSerie = getOsmose().numSerie;
        int index = species.getIndex();
        sexRatio = getOsmose().sexRatioMatrix[numSerie][index];
        alpha = getOsmose().alphaMatrix[numSerie][index];
    }

    @Override
    public void run() {
        double SSB = 0;
        List<School> schools = getSimulation().getSchools(species);
        for (School school : schools) {
            if (school.getLength() >= species.sizeMat) {
                SSB += school.getBiomass();
            }
        }

        double season = species.seasonSpawning.length > getSimulation().getNumberTimeStepsPerYear()
                ? species.seasonSpawning[getSimulation().getIndexTimeSimu()]
                : species.seasonSpawning[getSimulation().getIndexTimeYear()];
        double nbEggs = sexRatio * alpha * season * SSB * 1000000;

        /*
         * Making cohorts going up to the upper age class
         * Kill old schools
         */
        for (School school : schools) {
            school.incrementAge();
            if (school.getAgeDt() > (species.getLongevity() - 1)) {
                school.kill();
            }
        }

        //UPDATE AGE CLASS 0
        int nbSchools = getOsmose().nbSchools[getOsmose().numSerie];
        if (nbEggs == 0.d) {
            // do nothing, zero school
        } else if (nbEggs < nbSchools) {
            School school0 = new School(species, nbEggs, species.eggSize, species.eggWeight, 0);
            getPopulation().add(school0);
        } else if (nbEggs >= nbSchools) {
            for (int i = 0; i < nbSchools; i++) {
                School school0 = new School(species, nbEggs / nbSchools, species.eggSize, species.eggWeight, 0);
                getPopulation().add(school0);
            }
        }
    }
    
}
