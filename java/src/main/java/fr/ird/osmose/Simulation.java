/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine Ecosystems)
 * http://www.osmose-model.org
 * 
 * Copyright (C) IRD (Institut de Recherche pour le Développement) 2009-2020
 * 
 * Osmose is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, additional and
 * starvation mortalities, reproduction and migration) and fishing mortalities
 * (Shin and Cury 2001, 2004).
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License). Full description
 * is provided on the LICENSE file.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * 
 */

package fr.ird.osmose;

import fr.ird.osmose.background.BackgroundSchoolSet;
import fr.ird.osmose.output.SchoolSetSnapshot;
import fr.ird.osmose.populator.PopulatingProcess;
import fr.ird.osmose.process.genet.Trait;
import fr.ird.osmose.process.mortality.FishingGear;
import fr.ird.osmose.resource.ResourceForcing;
import fr.ird.osmose.util.OsmoseLinker;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import ucar.nc2.NetcdfFile;

/**
 * This class is in charge of running one instance of the simulation. Let's
 * clarify: a {@code Configuration} is a set of parameters. The parameter
 * <i>simulation.nsimu</i> controls how many simulations with the same set of
 * parameters are to be run. Every replicated simulation is an instance of this
 * object {@code Simulation}.<br>
 * The {@code Simulation} initialises all the required components for running
 * the simulation such as {@link fr.ird.osmose.step.AbstractStep},
 * {@link fr.ird.osmose.ltl.LTLForcing} or
 * {@link fr.ird.osmose.populator.PopulatingProcess} and then controls the loop
 * over time.
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

    private BackgroundSchoolSet backSchoolSet;

    /**
     * The low trophic level forcing class. Indexes from [0, nbkg -1] are the
     * forcings for bkg species. Indexes from [nbkg, nbkg + nrsc - 1] are for LTL.
     */
    private ResourceForcing[] resourceForcing;
    
    /** Recover the list of fishing Gears **/   
    private FishingGear[] fishingGear;
    
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
     * Object that is able to take a snapshot of the set of schools and write it in
     * a NetCDF file. Osmose will be able to restart on such a file.
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
    private int nEvolvingTrait;

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

        // barrier.n: init a set of background schools (used to save fisheries and
        // discards)
        this.backSchoolSet = new BackgroundSchoolSet();
        this.backSchoolSet.init();

        // Option for running only one time step and stops
        boolean oneStep = false;
        if (getConfiguration().canFind("simulation.onestep")) {
            oneStep = getConfiguration().getBoolean("simulation.onestep");
        }

        // Initialize time variables
        n_steps_simu = oneStep ? 1 : getConfiguration().getNStep();
        year = 0;
        i_step_year = 0;
        i_step_simu = 0;
    
        // initialisation of fishing gears.
        this.initFishingGear();

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
                info("Restarting simulation from year {0} step {1}", new Object[] { year, i_step_year });
                restart = true;
            } catch (IOException ex) {
                error("Failed to open restart file " + ncfile, ex);
            }
        }
 
        if (this.getConfiguration().isGeneticEnabled()) {

            // List all the trait mean parameters 
            List<String> genet_keys = this.getConfiguration().findKeys("*.trait.mean.sp*");
            
            // Remove the "sp#"
            List<String> output_keys = new ArrayList<>();
            for(String strOut : genet_keys) { 
                String prefix = strOut.substring(0, strOut.indexOf(".sp"));
                output_keys.add(prefix);    
            }
            
            // Removes duplicates.    
            List<String> prefix_keys = output_keys.stream().distinct().collect(Collectors.toList());
        
            // Init the arrays
            this.nEvolvingTrait = prefix_keys.size();
            this.evolvingTrait = new ArrayList<>();
            
            for (int p = 0; p < this.nEvolvingTrait; p++) {
                String key = prefix_keys.get(p);
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
         * @warning - phv 2014/01/25 this must be done before calling step.init() that
         *          will in turn call outputManager.init() which may request prey
         *          record.
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
     * Initializes resources forcing. The ResourceForcing array contains first the
     * background species, then the resource species.
     */
    private void initResourceForcing() {

        int nTot = this.getNBkgSpecies() + this.getNRscSpecies();
        resourceForcing = new ResourceForcing[nTot];

        int resourceIndex = 0;

        // Init resources for background species
        for (int fileIndex : this.getConfiguration().getBackgroundIndex()) {
            ResourceForcing resForcing = new ResourceForcing(fileIndex, resourceIndex);
            try {
                resForcing.init();
            } catch (IOException ex) {
                Logger.getLogger(Simulation.class.getName()).log(Level.SEVERE, null, ex);
            }
            resourceForcing[resourceIndex] = resForcing;
            resourceIndex++;
            // Name must contain only alphanumerical characters
        }

        for (int fileIndex : this.getConfiguration().getResourceIndex()) {
            ResourceForcing resForcing = new ResourceForcing(fileIndex, resourceIndex);
            try {
                resForcing.init();
            } catch (IOException ex) {
                Logger.getLogger(Simulation.class.getName()).log(Level.SEVERE, null, ex);
            }
            resourceForcing[resourceIndex] = resForcing;
            resourceIndex++;
            // Name must contain only alphanumerical characters
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
            // fr.ird.osmose.util.SimulationUI.step(year, i_step_year);

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
     * Whether to keep track of prey records during the simulation. The prey records
     * starts one year before the start year of saving. It is arbitrary and just to
     * make sure that the trophic levels are fully stabilised before saving the
     * outputs.
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
     * Returns the {@code ResourceForcing} instance for specified resource.
     *
     * @param index, the index of the resource
     * @return the {@code ResourceForcing} instance for specified resource.
     */
    public ResourceForcing[] getResourceForcing() {
        return resourceForcing;
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
        return this.nEvolvingTrait;
    }

    public BackgroundSchoolSet getBkgSchoolSet() {
        return this.backSchoolSet;
    }

    public FishingGear[] getFishingGear() {
        return this.fishingGear;
    }

    private void initFishingGear() {
        
        int nfishery = getConfiguration().getNFishery();
        
        fishingGear = new FishingGear[nfishery];

        // Recovers the index of fisheries
        int[] fisheryIndex = this.getConfiguration().findKeys("fisheries.name.fsh*").stream()
                .mapToInt(rgKey -> Integer.valueOf(rgKey.substring(rgKey.lastIndexOf(".fsh") + 4))).sorted().toArray();

        if (fisheryIndex.length != nfishery) {
            String message = "The number of fishery is not consistant with the number of fisheries name.";
            error(message, new Exception());
        }

        int cpt = 0;
        for (int index : fisheryIndex) {
            fishingGear[cpt] = new FishingGear(rank, index);
            fishingGear[cpt].init();
            cpt++;
        }
    }
    
}
