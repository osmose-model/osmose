package fr.ird.osmose.background;

import java.util.Iterator;
import java.util.List;

import fr.ird.osmose.process.AbstractProcess;
import fr.ird.osmose.util.GridMap;

public class BackgroundProcess extends AbstractProcess {

    /** Map distribution objects (one for each background species) */
    BackgroundMapDistribution[] mapDistribution;

    // Initialize the
    int[] nSchools;

    public BackgroundProcess(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        // Initialize the map distributions
        mapDistribution = new BackgroundMapDistribution[this.getNBkgSpecies()];
        int cpt = 0;
        for(int iSpeciesFile : getBackgroundIndex()) {
            mapDistribution[cpt] = new BackgroundMapDistribution(iSpeciesFile, cpt);
            mapDistribution[cpt].init();
            cpt++;
        }

        nSchools = new int[this.getNBkgSpecies()];
        cpt = 0;
        for (int iSpeciesFile : getBackgroundIndex()) {
            // if (this.getConfiguration().isNull("simulation.nschool.sp" + iSpeciesFile)) {
            //     nSchools[cpt] = -999;
            // } else {
                nSchools[cpt] = this.getConfiguration().getInt("simulation.nschool.sp" + iSpeciesFile);
            // }
            cpt++;
        }

        this.initializeSchools();

    }

    @Override
    public void run() {

        int iStepSimu = this.getSimulation().getIndexTimeSimu();

        //  Loop over all the background species.
        for (int iSpecies = 0; iSpecies < getNBkgSpecies(); iSpecies++) {
            BackgroundSpecies bkgSpecies = this.getBkgSpecies(iSpecies);
            int nClass = bkgSpecies.getNClass();

            // Loop over all the size classes
            for (int iClass = 0; iClass < nClass; iClass++) {
                if (!mapDistribution[iSpecies].mapIsUnchanged(iStepSimu, iClass)) {
                    // if the map change, the existing schools are killed and
                    // new schools are created
                    this.redistributeSchools(iSpecies, iClass);
                } else {
                    // If the map does not change
                    this.moveSchools(iSpecies, iClass);
                }
           }
        }

        getBkgSchoolSet().updateSchoolMap();

    }

    private void initializeSchools() {

        int iStepSimu = this.getSimulation().getIndexTimeSimu();

        for (int iSpecies = 0; iSpecies < getNBkgSpecies(); iSpecies++) {

            BackgroundSpecies species = getBkgSpecies(iSpecies);
            int nClass = species.getNClass();

            for (int iClass = 0; iClass < nClass; iClass++) {
                // get the biomass for the given size-class and the given time
                double biomass = this.getBkgSpecies(iSpecies).getBiomass(iStepSimu, iClass);

                int nschool = nSchools[iSpecies];

                double biomassPerSchool = biomass / nschool;

                for (int i = 0; i < nschool; i++) {
                    BackgroundSchool bkgSchool = new BackgroundSchool(species, iClass);
                    bkgSchool.setBiomass(biomassPerSchool);
                    getBkgSchoolSet().add(bkgSchool);
                }
            }
        }
    }

    private void redistributeSchools(int iSpecies, int iClass) {

        int iStepSimu = this.getSimulation().getIndexTimeSimu();

        // get the biomass for the given size-class and the given time
        double biomass = this.getBkgSpecies(iSpecies).getBiomass(iStepSimu, iClass);

        int nschool = nSchools[iSpecies];

        // If the number of schools is fixed over time (Ricardo's request).
        // the biomass is updated based on the number of schools and schools are moved
        if (nschool > 0) {
            // list the background schools that belong to the given species and given
            // size-class
            List<BackgroundSchool> schoolSet = this.getSimulation().getBkgSchoolSet()
                    .getSchools(this.getBkgSpecies(iSpecies), iClass, true);
            double biomassPerSchool = biomass / nschool;

            // For each school, we reinitialize the biomass, and we move the schools
            for (BackgroundSchool bkgSchool : schoolSet) {
                bkgSchool.setBiomass(biomassPerSchool);
                mapDistribution[iSpecies].move(bkgSchool, iStepSimu);
            }

        } else {
            // if the number of schools is not provided, old schools are removed.
            // then we recreate the schools, with one school per movement cell.

            // removing of schools
            this.getSimulation().getBkgSchoolSet().removeSchools(iSpecies, iClass);

            // recover the distribution map for the given time-step
            GridMap map = mapDistribution[iSpecies].getGridMap(iClass, iStepSimu);

        }
    }

    /** Method that aims at moving background schools and at resetting the biomass. */
    private void moveSchools(int iSpecies, int iClass) {

        int iStepSimu = this.getSimulation().getIndexTimeSimu();

        // get the biomass for the given size-class and the given time
        double biomass = this.getBkgSpecies(iSpecies).getBiomass(iStepSimu, iClass);

        // list the background schools that belong to the given species and given size-class
        List<BackgroundSchool> schoolSet = this.getSimulation().getBkgSchoolSet().getSchools(this.getBkgSpecies(iSpecies), iClass, true);

        // get the number of schools and extract the biomass per schools
        int nSchools = schoolSet.size();
        double biomassPerSchool = biomass / nSchools;

        // For each school, we reinitialize the biomass, and we move the schools
        for(BackgroundSchool bkgSchool :schoolSet) {
            bkgSchool.setBiomass(biomassPerSchool);
            mapDistribution[iSpecies].move(bkgSchool, iStepSimu);
        }
    }

}
