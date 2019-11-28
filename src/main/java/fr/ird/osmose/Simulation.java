/* 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
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
import fr.ird.osmose.populator.PopulatingProcess;
import fr.ird.osmose.process.genet.Trait;
import fr.ird.osmose.resource.ResourceCaching;
import fr.ird.osmose.resource.ResourceForcing;
import fr.ird.osmose.util.OsmoseLinker;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import ucar.nc2.NetcdfFile;

/**
 * This class is in charge of running one instance of the simulation. Let's
 * clarify: a {@code Configuration} is a set of parameters. The parameter
 * <i>simulation.nsimu</i> controls how many simulations with the same set of
 * parameters are to be run. Every replicated simulation is an instance of this
 * object {@code Simulation}.<br>
 * The {@code Simulation} initialises all the required components for running
 * the simulation such as
 * {@link fr.ird.osmose.step.AbstractStep}, {@link fr.ird.osmose.ltl.LTLForcing}
 * or {@link fr.ird.osmose.populator.PopulatingProcess} and then controls the
 * loop over time.
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0b 2013/09/01
 */
public class Simulation extends OsmoseLinker {

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
    private ResourceForcing[] resourceForcing;
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
     * The object that controls what should be done during one time step.
     */
    private SimulationStep step;
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
    /**
     * Whether the restart files should be written or not
     */
    private boolean writeRestart;
    /**
     * Whether to keep track of prey records during the simulation
     */
    private boolean preyRecord;
    /**
     * Year to start writing the outputs
     */
    private int yearOutput;

    /**
     * List of evolving trait.
     */
    private List<Trait> evolvingTrait;
    private int n_evolving_trait;

//////////////
// Constructor
//////////////
    /**
     * Creates a new simulation with given rank.
     *
     * @param rank, the rank of the simulation
     */
    public Simulation(int rank) {
        this.rank = rank;
    }

    /**
     * Removes all the schools and delete forcing, species, ltlgroups, etc.
     */
    public void destroy() {

        schoolSet.clear();
        step = null;
        resourceForcing = null;
        snapshot = null;
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
                : getConfiguration().getNStep();
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

        if (this.getConfiguration().isGeneticEnabled()) {
            List<String> genet_keys = this.getConfiguration().findKeys("*.trait.mean");
            this.n_evolving_trait = genet_keys.size();
            this.evolvingTrait = new ArrayList<>();
            for (int p = 0; p < this.n_evolving_trait; p++) {
                String key = genet_keys.get(p);
                // recovers the trait prefix
                String prefix = key.replace(".trait.mean", "");
                Trait trait = new Trait(this.rank, prefix);
                trait.init();
                this.evolvingTrait.add(trait);
            }
        }

        // Init resource forcing
        initResourceForcing();

        // By default do not make prey records as it is memory expensive
        /**
         * @warning - phv 2014/01/25 this must be done before calling
         * step.init() that will in turn call outputManager.init() which may
         * request prey record.
         */
        preyRecord = false;

        // Instantiate the Step
        step = new SimulationStep(rank);

        // Intialize the step
        step.init();

        // Initialize the population
        PopulatingProcess populatingProcess = new PopulatingProcess(rank);
        populatingProcess.init();
        populatingProcess.run();

        // Initialize the restart maker
        snapshot = new SchoolSetSnapshot(rank);
        restartFrequency = Integer.MAX_VALUE;
        if (!getConfiguration().isNull("output.restart.recordfrequency.ndt")) {
            restartFrequency = getConfiguration().getInt("output.restart.recordfrequency.ndt");
        }
        spinupRestart = 0;
        if (!getConfiguration().isNull("output.restart.spinup")) {
            spinupRestart = getConfiguration().getInt("output.restart.spinup") - 1;
        }
        writeRestart = true;
        if (!getConfiguration().isNull("output.restart.enabled")) {
            writeRestart = getConfiguration().getBoolean("output.restart.enabled");
        } else {
            warning("Could not find parameter 'output.restart.enabled'. Osmose assumes it is true and a NetCDF restart file will be created at the end of the simulation (or more, depending on parameters 'simulation.restart.recordfrequency.ndt' and 'simulation.restart.spinup').");
        }

        // Year to start writing the outputs
        yearOutput = getConfiguration().getInt("output.start.year");

    }

    /**
     * Initializes resources forcing.
     */
    private void initResourceForcing() {

        int nRsc = getConfiguration().getNRscSpecies();
        resourceForcing = new ResourceForcing[nRsc];
        for (int iRsc = 0; iRsc < nRsc; iRsc++) {
            resourceForcing[iRsc] = new ResourceForcing(iRsc, ResourceCaching.ALL);
            resourceForcing[iRsc].init();
        }
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
                info("year {0}", year);
            }

            // Run a new step
            step.step(i_step_simu);
            //fr.ird.osmose.util.SimulationUI.step(year, i_step_year);

            // Create a restart file
            if (writeRestart && (year >= spinupRestart) && ((i_step_simu + 1) % restartFrequency == 0)) {
                snapshot.makeSnapshot(i_step_simu);
            }

            // Increment time step
            i_step_simu++;
        }
        step.end();

        // Create systematically a restart file at the end of the simulation
        if (writeRestart) {
            snapshot.makeSnapshot(i_step_simu - 1);
        }
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
     * Whether to keep track of prey records during the simulation. The prey
     * records starts one year before the start year of saving. It is arbitrary
     * and just to make sure that the trophic levels are fully stabilised before
     * saving the outputs.
     *
     * @return true if prey records should be activated
     */
    public boolean isPreyRecord() {
        return preyRecord && (year >= (yearOutput - 1));
    }

    public void requestPreyRecord() {
        preyRecord = true;
    }

    /**
     * Returns the {@code ResourceForcing} instance for specified resource.
     *
     * @param index, the index of the resource
     * @return the {@code ResourceForcing} instance for specified resource.
     */
    public ResourceForcing getResourceForcing(int index) {
        return resourceForcing[index];
    }

    /**
     * Returns the ith evolving trait for the given simulation.
     *
     * @param i Index of the evolving trait
     * @return A Trait object
     */
    public Trait getEvolvingTrait(int i) {
        return this.evolvingTrait.get(i);
    }

    /**
     * Returns the total number of evolving trait for the given simulation.
     *
     * @return The number of evolving trait
     */
    public int getNEvolvingTraits() {
        return this.n_evolving_trait;
    }

}
