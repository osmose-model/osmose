
package fr.ird.osmose.process;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import java.util.List;

/**
 *
 * @author pverley
 */
public class LocalReproductionProcess extends AbstractProcess {
    
    final private Species species;
    /*
     * Percentage of female in the population
     */
    private double sexRatio;
    /*
     * Number of eggs per gram of mature female
     */
    private double alpha;
    
    final private ReproductionProcess parent;
    
    public LocalReproductionProcess(ReproductionProcess parent, int replica, Species species) {
        super(replica);
        this.parent = parent;
        this.species = species;
    }

    @Override
    public void init() {
        int index = species.getIndex();
        sexRatio = getConfiguration().sexRatio[index];
        alpha = getConfiguration().alpha[index];
    }

    @Override
    public void run() {
        double SSB = 0;
        List<School> schools = getPopulation().getSchools(species);
        for (School school : schools) {
            if (school.getLength() >= species.getSizeMaturity()) {
                SSB += school.getInstantaneousBiomass();
            }
        }

        double season = parent.getSeason(getSimulation().getIndexTimeSimu(), species);
        double nbEggs = sexRatio * alpha * season * SSB * 1000000;

        /*
         * Making cohorts going up to the upper age class
         * Kill old schools
         */
        for (School school : schools) {
            school.incrementAge();
        }

        //UPDATE AGE CLASS 0
        int nbSchools = getConfiguration().nSchool;
        if (nbEggs == 0.d) {
            // do nothing, zero school
        } else if (nbEggs < nbSchools) {
            School school0 = new School(species, nbEggs);
            getPopulation().add(school0);
        } else if (nbEggs >= nbSchools) {
            for (int i = 0; i < nbSchools; i++) {
                School school0 = new School(species, nbEggs / nbSchools);
                getPopulation().add(school0);
            }
        }
    }
    
}
