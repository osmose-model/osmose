
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
        sexRatio = getConfiguration().getDouble("species.sexratio.sp" + index);
        alpha = getConfiguration().getDouble("species.relativefecundity.sp" + index);
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
        int nSchool = getConfiguration().getSeed();
        if (nbEggs == 0.d) {
            // do nothing, zero school
        } else if (nbEggs < nSchool) {
            School school0 = new School(species, nbEggs);
            getPopulation().add(school0);
        } else if (nbEggs >= nSchool) {
            for (int i = 0; i < nSchool; i++) {
                School school0 = new School(species, nbEggs / nSchool);
                getPopulation().add(school0);
            }
        }
    }
    
}
