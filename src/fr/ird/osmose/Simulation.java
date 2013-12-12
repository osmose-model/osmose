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

import fr.ird.osmose.ltl.LTLForcing;
import fr.ird.osmose.util.logging.OLogger;
import fr.ird.osmose.output.SchoolSetSnapshot;
import fr.ird.osmose.process.PopulatingProcess;
import fr.ird.osmose.step.AbstractStep;
import fr.ird.osmose.step.ConcomitantMortalityStep;
import fr.ird.osmose.step.SequentialMortalityStep;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import ucar.nc2.NetcdfFile;

/**
 * This class is in charge of running one instance of the simulation. Let's
 * clarify: a {@code Configuration} is a set of parameters. The parameter
 * <i>simulation.nsimu</i> controls how many simulations with the same set of
 * parameters are to be run. Every replicated simulation is an instance of this
 * object {@code Simulation}.<br>
 * The {@code Simulation} initializes all the required components for running
 * the simulation such as
 * {@link fr.ird.osmose.step.AbstractStep}, {@link fr.ird.osmose.ltl.LTLForcing}
 * or {@link fr.ird.osmose.process.PopulatingProcess} and then controls the loop
 * over time.
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0b 2013/09/01
 */
public class Simulation extends OLogger {

///////////////////////////////
// Declaration of the variables
///////////////////////////////
    /**
     * The rank of the simulation. (among replicated simulations)
     */
    private final int rank;
    /**
     * The set of schools.
     */
    private SchoolSet schoolSet;
    /**
     * The low trophic level forcing class.
     */
    private LTLForcing forcing;
    /**
     * Current year of the simulation.
     */
    private int year;
    /**
     * Time step in the current year. {@code i_step_year = i_step_simu / nyear}
     */
    private int i_step_year;
    /**
     * Time step of the simulation.
     */
    private int i_step_simu;
    /**
     * Total number of time steps of the simulation.
     */
    private int n_steps_simu;
    /**
     * Array of the species of the simulation.
     */
    private Species[] species;
    /**
     * Array of the LTL groups of the simulation.
     */
    private Plankton[] ltlGroups;
    /**
     * The object that controls what should be done during one time step.
     */
    private AbstractStep step;
    /**
     * Object that is able to take a snapshot of the set of schools and write it
     * in a NetCDF file. Osmose will be able to restart on such a file.
     */
    private SchoolSetSnapshot snapshot;
    /**
     * Record frequency for writing restart files, in number of time step.
     */
    private int restartFrequency;
    /**
     * Indicates whether the simulation starts from a restart file.
     */
    private boolean restart;
    /**
     * Number of years before writing restart files.
     */
    private int spinupRestart;

//////////////
// Constructor
//////////////
    /**
     * Creates a new simulation with given rank.
     *
     * @param rank, the rank of the simulation
     */
    public Simulation(int rank) {
        super(rank);
        this.rank = rank;
    }

///////////////////////////////
// Definition of the functions
///////////////////////////////
    /**
     * Initialize the simulation.
     */
    public void init() {

        // Create a new school set, empty at the moment
        schoolSet = new SchoolSet();

        // Option for running only one time step and stops
        boolean oneStep = false;
        if (getConfiguration().canFind("simulation.onestep")) {
            oneStep = getConfiguration().getBoolean("simulation.onestep");
        }

        // Initialize time variables
        n_steps_simu = oneStep
                ? 1
                : getConfiguration().getNYear() * getConfiguration().getNStepYear();
        year = 0;
        i_step_year = 0;
        i_step_simu = 0;

        // Look for restart file
        restart = false;
        if (!getConfiguration().isNull("simulation.restart.file")) {
            String ncfile = getConfiguration().getFile("simulation.restart.file") + "." + rank;
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
                info("Restarting simulation from year {0} step {1}", new Object[]{year, i_step_year});
                restart = true;
            } catch (IOException ex) {
                error("Failed to open restart file " + ncfile, ex);
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
                ltlGroups[p] = new UniformPlankton(rank, p);
            } else {
                ltlGroups[p] = new Plankton(rank, p);
            }
            ltlGroups[p].init();
        }

        // Init LTL forcing
        initForcing();

        // Instantiate the Step
        //step = new SequentialMortalityStep(rank);
        step = new ConcomitantMortalityStep(rank);

        // Intialize the step
        step.init();

        // Initialize the population
        PopulatingProcess populatingProcess = new PopulatingProcess(rank);
        populatingProcess.init();
        populatingProcess.run();

        // Initialize the restart maker
        snapshot = new SchoolSetSnapshot(rank);
        restartFrequency = Integer.MAX_VALUE;
        if (!getConfiguration().isNull("simulation.restart.recordfrequency.ndt")) {
            restartFrequency = getConfiguration().getInt("simulation.restart.recordfrequency.ndt");
        }
        spinupRestart = 0;
        if (!getConfiguration().isNull("simulation.restart.spinup")) {
            spinupRestart = getConfiguration().getInt("simulation.restart.spinup") - 1;
        }
    }

    /**
     * Creates a specific instance of {@link fr.ird.osmose.ltl.LTLForcing}.
     */
    private void initForcing() {

        String ltlClassName = getConfiguration().getString("ltl.java.classname");
        String errMsg = "Failed to create new LTLForcing instance";
        try {
            info("LTLForcing: " + ltlClassName);
            forcing = (LTLForcing) Class.forName(ltlClassName).getConstructor(Integer.TYPE).newInstance(rank);
        } catch (ClassNotFoundException ex) {
            error(errMsg, ex);
        } catch (IllegalAccessException ex) {
            error(errMsg, ex);
        } catch (IllegalArgumentException ex) {
            error(errMsg, ex);
        } catch (InstantiationException ex) {
            error(errMsg, ex);
        } catch (NoSuchMethodException ex) {
            error(errMsg, ex);
        } catch (SecurityException ex) {
            error(errMsg, ex);
        } catch (InvocationTargetException ex) {
            error(errMsg, ex);
        }

        forcing.init();
    }

    /**
     * Checks whether the simulation started from a restart file.
     *
     * @return {@code true} if the simulation started from a restart file
     */
    public boolean isRestart() {
        return restart;
    }

    /**
     * Runs the simulation. It controls the loop over time.
     */
    public void run() {

        while (i_step_simu < n_steps_simu) {
            year = i_step_simu / getConfiguration().getNStepYear();
            i_step_year = i_step_simu % getConfiguration().getNStepYear();

            // Print progress in console at the beginning of the year
            if (i_step_simu % getConfiguration().getNStepYear() == 0) {
                debug("year {0}", year);
            }

            // Run a new step
            step.step(i_step_simu);
            //fr.ird.osmose.util.SimulationUI.step(year, i_step_year);

            // Create a restart file
            if ((year >= spinupRestart) && ((i_step_simu + 1) % restartFrequency == 0)) {
                snapshot.makeSnapshot(i_step_simu);
            }

            // Increment time step
            i_step_simu++;
        }
        step.end();

        // Create systematically a restart file at the end of the simulation
        snapshot.makeSnapshot(i_step_simu - 1);
    }

    /**
     * Returns the {@code SchoolSet} associated to this simulation.
     *
     * @return the school set
     */
    public SchoolSet getSchoolSet() {
        return schoolSet;
    }

    /**
     * Get a species
     *
     * @param index, the index of the species
     * @return the species at index {@code index}
     */
    public Species getSpecies(int index) {
        return species[index];
    }

    /**
     * Gets the specified plankton group.
     *
     * @param index, the index of the plankton group.
     * @return the plankton group number iPlankton.
     */
    public Plankton getPlankton(int index) {
        return ltlGroups[index];
    }

    /**
     * Returns the current year of the simulation.
     *
     * @return the current year of the simulation
     */
    public int getYear() {
        return year;
    }

    /**
     * Returns the time step in the current year.
     *
     * @return the time step in the current year
     */
    public int getIndexTimeYear() {
        return i_step_year;
    }

    /**
     * Returns the current time step of the simulation.
     *
     * @return the current time step of the simulation
     */
    public int getIndexTimeSimu() {
        return i_step_simu;
    }

    /**
     * Returns an instance of the {@code Configuration}.
     *
     * @return the current {@code Configuration}.
     */
    private Configuration getConfiguration() {
        return Osmose.getInstance().getConfiguration();
    }

    /**
     * Returns the {@code LTLForcing} instance.
     *
     * @return the {@code LTLForcing} instance
     */
    public LTLForcing getForcing() {
        return forcing;
    }
}
