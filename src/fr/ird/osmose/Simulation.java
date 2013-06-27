/*
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, natural and
 * starvation mortalities, reproduction and migration) and fishing mortalities
 * (Shin and Cury 2001, 2004).
 * 
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */
package fr.ird.osmose;

import fr.ird.osmose.output.SchoolSetSnapshot;
import fr.ird.osmose.process.AbstractProcess;
import fr.ird.osmose.process.PopulatingProcess;
import fr.ird.osmose.step.AbstractStep;
import fr.ird.osmose.step.ConcomitantMortalityStep;
import fr.ird.osmose.step.SequentialMortalityStep;
import fr.ird.osmose.util.SimulationLogFormatter;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.nc2.NetcdfFile;

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
    /*
     * Object that is able to create NetCDF restart file
     */
    private SchoolSetSnapshot snapshot;
    /*
     * Record frequency of the restart file
     */
    private int restartFrequency;
    /*
     * 
     */
    private boolean restart;

///////////////////////////////
// Definition of the functions
///////////////////////////////
    /**
     * Initialize the simulation
     */
    public void init() {

        // Create a new population, empty at the moment
        schoolSet = new SchoolSet();

        // Trick to force the model into running only one time step
        boolean oneStep = false;
        if (getConfiguration().canFind("simulation.onestep")) {
            oneStep = getConfiguration().getBoolean("simulation.onestep");
        }

        // Reset time variables
        n_steps_simu = oneStep
                ? 1
                : getConfiguration().getNYear() * getConfiguration().getNStepYear();
        year = 0;
        i_step_year = 0;
        i_step_simu = 0;
        restart = false;
        if (!getConfiguration().isNull("simulation.restart.file")) {
            String ncfile = getConfiguration().getFile("simulation.restart.file") + "." + index;
            i_step_simu = 0;
            try {
                NetcdfFile nc = NetcdfFile.open(ncfile);
                i_step_simu = Integer.valueOf(nc.findGlobalAttribute("step").getStringValue()) + 1;
                if (oneStep) {
                    n_steps_simu = i_step_simu + 1;
                }
                int nStepYear = getConfiguration().getNStepYear();
                year = i_step_simu / nStepYear;
                i_step_year = i_step_simu % nStepYear;
                getLogger().log(Level.INFO, "Restarting simulation from year {0} step {1}", new Object[]{year, i_step_year});
                restart = true;
            } catch (IOException ex) {
                getLogger().log(Level.WARNING, "Failed to open restart file " + ncfile, ex);
            }
        }

        // Create the species
        species = new Species[getConfiguration().getNSpecies()];
        for (int i = 0; i < species.length; i++) {
            species[i] = new Species(i);
        }

        // Init plankton groups
        ltlGroups = new Plankton[getConfiguration().getNPlankton()];
        for (int p = 0; p < ltlGroups.length; p++) {
            if (getConfiguration().canFind("plankton.biomass.total.plk" + p)) {
                ltlGroups[p] = new UniformPlankton(p,
                        getConfiguration().getString("plankton.name.plk" + p),
                        getConfiguration().getFloat("plankton.size.min.plk" + p),
                        getConfiguration().getFloat("plankton.size.max.plk" + p),
                        getConfiguration().getFloat("plankton.tl.plk" + p),
                        getConfiguration().getFloat("plankton.accessibility2fish.plk" + p),
                        getConfiguration().getFloat("plankton.biomass.total.plk" + p));
            } else {
                ltlGroups[p] = new Plankton(p,
                        getConfiguration().getString("plankton.name.plk" + p),
                        getConfiguration().getFloat("plankton.size.min.plk" + p),
                        getConfiguration().getFloat("plankton.size.max.plk" + p),
                        getConfiguration().getFloat("plankton.tl.plk" + p),
                        getConfiguration().getFloat("plankton.conversion2tons.plk" + p),
                        1, // prod2biom parameter that is not used anymore
                        getConfiguration().getFloat("plankton.accessibility2fish.plk" + p));
            }
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

        // Initialize the restart maker
        snapshot = new SchoolSetSnapshot(index);
        restartFrequency = Integer.MAX_VALUE;
        if (!getConfiguration().isNull("simulation.restart.recordfrequency.ndt")) {
            restartFrequency = getConfiguration().getInt("simulation.restart.recordfrequency.ndt");
        }
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

    public boolean isRestart() {
        return restart;
    }

    public void run() {
        
        while (i_step_simu < n_steps_simu) {
            year = i_step_simu / getConfiguration().getNStepYear();
            i_step_year = i_step_simu % getConfiguration().getNStepYear();

            // Print progress in console
            //progress();

            // Run a new step
            step.step(i_step_simu);
            fr.ird.osmose.util.SimulationUI.step(year, i_step_year);

            // Create a restart file
            if ((i_step_simu + 1) % restartFrequency == 0) {
                snapshot.makeSnapshot(i_step_simu);
            }

            // Increment time step
            i_step_simu++;
            }
        step.end();

        // create a restart at the end of the simulation
        snapshot.makeSnapshot(i_step_simu - 1);
    }
    
    public void makeSnapshot() {
        snapshot.makeSnapshot(i_step_simu);
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
