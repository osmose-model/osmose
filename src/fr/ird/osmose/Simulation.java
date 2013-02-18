package fr.ird.osmose;

import fr.ird.osmose.grid.IGrid;
import fr.ird.osmose.ltl.LTLForcing;
import fr.ird.osmose.output.Indicators;
import fr.ird.osmose.process.AbstractProcess;
import fr.ird.osmose.process.PopulatingProcess;
import fr.ird.osmose.step.AbstractStep;
import fr.ird.osmose.step.ConcomitantMortalityStep;
import fr.ird.osmose.step.SequentialMortalityStep;
import java.util.*;

public class Simulation {

///////////////////////////////
// Declaration of the constants
///////////////////////////////
    public enum Version {
        /*
         * SCHOOL2012 stands for SCHOOLBASED processes, in sequential order
         * (just like in WS2009).
         * Similarily to WS2009 and conversely to SCHOOL2012_PROD, plankton
         * concentration are read like production.
         */

        SCHOOL2012_PROD,
        /*
         * SCHOOL2012 stands for SCHOOLBASED processes, in sequential order
         * (just like in WS2009).
         * Difference from WS2009 comes from plankton concentration that is read
         * directly as a biomass.
         */
        SCHOOL2012_BIOM,
        /*
         * CASE1
         * > It is assumed that every cause is independant and concomitant.
         * > No stochasticity neither competition within predation process: every
         * predator sees preys as they are at the begining of the time-step.
         * > Synchromous updating of school biomass.
         */
        CASE1,
        /*
         * CASE2
         * > It is assumed that every cause is independant and concomitant.
         * > Stochasticity and competition within predation process: prey and
         * predator biomass are being updated on the fly virtually (indeed the
         * update is not effective outside the predation process,
         * it is just temporal).
         * > Synchronous updating of school biomass.
         */
        CASE2,
        /*
         * CASE3
         * > It is assumed that every cause compete with each other.
         * > Stochasticity and competition within predation process.
         * > Asynchronous updating of school biomass (it means biomass are updated
         * on the fly).
         */
        CASE3;
    }
    /*
     * Choose the version of Osmose tu run.
     * @see enum Version for details.
     */
    public static final Version VERSION = Version.CASE3;
///////////////////////////////
// Declaration of the variables
///////////////////////////////
    private Population population;
    /*
     * Forcing with Biogeochimical model.
     */
    private LTLForcing forcing;
    /*
     * Number of time-steps in one year
     */
    private int nTimeStepsPerYear;
    /*
     * Number of years of simulation
     */
    private int nYear;
    /*
     * Time of the simulation in [year]
     */
    private int year;
    /*
     * Time step of the current year
     */
    private int i_step_year;
    /*
     * Time step of the simulation
     */
    private int i_step_simu;
    /*
     * Array of the species of the simulation
     */
    private Species[] species;
    /*
     * What should be done within one time step
     */
    private AbstractStep step;

///////////////////////////////
// Definition of the functions
///////////////////////////////
    /**
     * Initialize the simulation
     */
    public void init() {

        // Create a new population, empty at the moment
        population = new Population();

        // Reset time variables
        year = 0;
        i_step_year = 0;
        i_step_simu = 0;
        nTimeStepsPerYear = getOsmose().nbDtMatrix;
        nYear = getOsmose().simulationTimeTab;

        // Create the species
        species = new Species[getOsmose().nbSpeciesTab];
        for (int i = 0; i < species.length; i++) {
            species[i] = new Species(i);
            // Initialize species
            species[i].init();
        }

        // Instantiate the Step
        switch (VERSION) {
            case SCHOOL2012_PROD:
            case SCHOOL2012_BIOM:
                step = new SequentialMortalityStep();
                break;
            case CASE1:
            case CASE2:
            case CASE3:
                step = new ConcomitantMortalityStep();
        }
        // Intialize the step
        step.init();

        // Initialize the population
        AbstractProcess populatingProcess = new PopulatingProcess();
        populatingProcess.init();
        populatingProcess.run();
        
        // Initialize the indicators
        Indicators.init();
    }

    /**
     * Print the progress of the simulation in text console
     */
    private void progress() {
        // screen display to check the period already simulated
        if (year % 5 == 0) {
            System.out.println("year " + year + " | CPU time " + new Date());   // t is annual
        } else {
            System.out.println("year " + year);
        }
    }

    private void setupMPA() {
        if ((getOsmose().thereIsMPATab) && (year == getOsmose().MPAtStartTab)) {
            //RS = (double) getOsmose().tabMPAiMatrix.length / ((getGrid().getNbLines()) * getGrid().getNbColumns());
            for (int index = 0; index < getOsmose().tabMPAiMatrix.length; index++) {
                getGrid().getCell(getOsmose().tabMPAiMatrix[index], getOsmose().tabMPAjMatrix[index]).setMPA(true);
            }
        } else if ((!getOsmose().thereIsMPATab) || (year > getOsmose().MPAtEndTab)) {
            //RS = 0;
            for (int index = 0; index < getOsmose().tabMPAiMatrix.length; index++) {
                getGrid().getCell(getOsmose().tabMPAiMatrix[index], getOsmose().tabMPAjMatrix[index]).setMPA(false);
            }
        }
    }

    public void updateStages() {
        for (School school : population) {
            int i = school.getSpeciesIndex();
            school.updateFeedingStage(species[i].sizeFeeding, species[i].nbFeedingStages);
            school.updateAccessStage(getOsmose().accessStageThreshold[i], getOsmose().nbAccessStage[i]);
            school.updateDietOutputStage(species[i].dietStagesTab, species[i].nbDietStages);
        }
    }

    /*
     * save fish biomass before any mortality process for diets data (last
     * column of predatorPressure output file in Diets/)
     */
    public void saveBiomassBeforeMortality() {

        // update biomass
        if (getOsmose().dietsOutputMatrix && (year >= getOsmose().timeSeriesStart)) {

//            for (School school : getPopulation().getPresentSchools()) {
//                Indicators.biomPerStage[school.getSpeciesIndex()][school.dietOutputStage] += school.getBiomass();
//            }
//            getForcing().saveForDiet();
        }
        forcing.savePlanktonBiomass(getOsmose().planktonBiomassOutputMatrix);
    }

    public void run() {

        while (year < nYear) {

            // Print progress in console
            progress();

            // Calculate relative size of MPA
            setupMPA();

            // Loop over the year
            while (i_step_year < nTimeStepsPerYear) {
                // Run a new step
                step.step();
                // Increment time step
                i_step_year++;
                i_step_simu++;
            }
            // End of the year
            i_step_year = 0;
            // Go to following year
            year++;
        }
    }

    public Population getPopulation() {
        return population;
    }

    public int getNumberSpecies() {
        return species.length;
    }

    /**
     * Get a species
     *
     * @param index, the index of the species
     * @return species[index]
     */
    public Species getSpecies(int index) {
        return species[index];
    }

    public LTLForcing getForcing() {
        return forcing;
    }
    
    public void setForcing(LTLForcing forcing) {
        this.forcing = forcing;
    }

    public int getNumberTimeStepsPerYear() {
        return nTimeStepsPerYear;
    }

    public int getYear() {
        return year;
    }

    public int getIndexTimeYear() {
        return i_step_year;
    }

    public int getIndexTimeSimu() {
        return i_step_simu;
    }

    public int getNumberYears() {
        return nYear;
    }
    
    private IGrid getGrid() {
        return Osmose.getInstance().getGrid();
    }

    private Osmose getOsmose() {
        return Osmose.getInstance();
    }
}
