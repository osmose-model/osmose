
package fr.ird.osmose;

import java.util.List;

/**
 *
 * @author pverley
 */
public class LocalReproductionProcess extends AbstractProcess {
    
    private Species species;
    
    public LocalReproductionProcess(Species species) {
        this.species = species;
    }

    @Override
    public void loadParameters() {
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

        double season = species.seasonSpawning.length > getSimulation().getNbTimeStepsPerYear()
                ? species.seasonSpawning[getSimulation().getIndexTimeSimu()]
                : species.seasonSpawning[getSimulation().getIndexTimeYear()];
        double nbEggs = species.sexRatio * species.alpha * season * SSB * 1000000;

        /*
         * Making cohorts going up to the upper age class
         * Kill old schools
         */
        for (School school : schools) {
            school.age += 1;
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
