package fr.ird.osmose;

import fr.ird.osmose.process.AbstractProcess;
import fr.ird.osmose.process.PopulatingProcess;
import fr.ird.osmose.step.AbstractStep;
import fr.ird.osmose.step.ConcomitantMortalityStep;
import fr.ird.osmose.step.SequentialMortalityStep;
import fr.ird.osmose.util.SimulationLogFormatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    public static final Version VERSION = Version.CASE1;
    /*
     * The index of the replicated simulation
     */
    private final int index;
    /**
     * The application logger
     */
    private final Logger logger;

///////////////////////////////
// Constructor
///////////////////////////////    
    public Simulation(int index) {
        this.index = index;
        // setup the logger
        logger = Logger.getLogger(Simulation.class.getName() + "#" + index);
        Handler[] handlers = logger.getHandlers();
        for (Handler handler : handlers) {
            logger.removeHandler(handler);
        }
        logger.setUseParentHandlers(false);
        SimulationLogFormatter formatter = new SimulationLogFormatter(index);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(formatter);
        logger.addHandler(handler);
    }
///////////////////////////////
// Declaration of the variables
///////////////////////////////
    private SchoolSet schoolSet;
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
     * Total number of time steps in one simulation
     */
    private int n_steps_simu;
    /*
     * Array of the species of the simulation
     */
    private Species[] species;
    /**
     * Array of the LTL groups of the simulation
     */
    private Plankton[] ltlGroups;
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
        schoolSet = new SchoolSet();

        // Reset time variables
        n_steps_simu = getConfiguration().getNYear() * getConfiguration().getNStepYear();
        year = 0;
        i_step_year = 0;
        i_step_simu = 0;

        // Create the species
        species = new Species[getConfiguration().getNSpecies()];
        for (int i = 0; i < species.length; i++) {
            species[i] = new Species(i);
        }

        // Init plankton groups
        ltlGroups = new Plankton[getConfiguration().getNPlankton()];
        for (int p = 0; p < ltlGroups.length; p++) {
            ltlGroups[p] = new Plankton(p,
                    getConfiguration().getString("plankton.name.plk" + p),
                    getConfiguration().getFloat("plankton.size.min.plk" + p),
                    getConfiguration().getFloat("plankton.size.max.plk" + p),
                    getConfiguration().getFloat("plankton.tl.plk" + p),
                    getConfiguration().getFloat("plankton.conversion2tons.plk" + p),
                    1, // prod2biom parameter that is not used anymore
                    getConfiguration().getFloat("plankton.accessibility2fish.plk" + p));
            ltlGroups[p].init();
        }

        // Instantiate the Step
        switch (VERSION) {
            case SCHOOL2012_PROD:
            case SCHOOL2012_BIOM:
                step = new SequentialMortalityStep(index);
                break;
            case CASE1:
            case CASE2:
            case CASE3:
                step = new ConcomitantMortalityStep(index);
        }
        // Intialize the step
        step.init();

        // Initialize the population
        AbstractProcess populatingProcess = new PopulatingProcess(index);
        populatingProcess.init();
        populatingProcess.run();
    }

    /**
     * Print the progress of the simulation in text console
     */
    private void progress() {
        // screen display to check the period already simulated
        if (i_step_simu % getConfiguration().getNStepYear() == 0) {
            logger.log(Level.INFO, "year {0}", year);
        }
    }

    public void run() {

        while (i_step_simu < n_steps_simu) {
            year = i_step_simu / getConfiguration().getNStepYear();
            i_step_year = i_step_simu % getConfiguration().getNStepYear();

            // Print progress in console
            //progress();

            // Run a new step
            step.step(i_step_simu);

            // Increment time step
            i_step_simu++;
        }
    }

    public SchoolSet getSchoolSet() {
        return schoolSet;
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

    /**
     * Gets the specified plankton group.
     *
     * @param iPlankton, the index of the plankton group.
     * @return the plankton group number iPlankton.
     */
    public Plankton getPlankton(int index) {
        return ltlGroups[index];
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

    private Configuration getConfiguration() {
        return Osmose.getInstance().getConfiguration();
    }

    public final int getReplica() {
        return index;
    }

    final public Logger getLogger() {
        return logger;
    }
}
