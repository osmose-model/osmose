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
 * Nicolas BARRIER (nicolas.barrier@ird.fr)
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

import fr.ird.osmose.background.BackgroundProcess;
import fr.ird.osmose.output.OutputManager;
import fr.ird.osmose.process.GrowthProcess;
import fr.ird.osmose.process.IncomingFluxProcess;
import fr.ird.osmose.process.MortalityProcess;
import fr.ird.osmose.process.MovementProcess;
import fr.ird.osmose.process.ReproductionProcess;
import fr.ird.osmose.process.bioen.BioenReproductionProcess;
import fr.ird.osmose.process.bioen.EnergyBudget;
import fr.ird.osmose.util.SimulationLinker;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import fr.ird.osmose.resource.ResourceForcing;

/**
 *
 * @author pverley
 */
public class SimulationStep extends SimulationLinker {


    /* Background process. */
    private BackgroundProcess backgroundProcess;

    /*
     * Growth process
     */
    private GrowthProcess growthProcess;

    /*
     * Reproduction process
     */
    private ReproductionProcess reproductionProcess;
    /*
     * Incoming flux of biomass
     */
    private IncomingFluxProcess incomingFLuxProcess;
    /*
     * Generic mortality process that encompasses all mortality processes
     */
    private MortalityProcess mortalityProcess;
    /*
     * Movement process
     */
    private MovementProcess movementProcess;
    /*
     * List of indicators
     */
    private OutputManager indicators;
    /*
     * Record time step 0 (initial state) in the outputs
     */
    private boolean recordStep0;

    /** Adding a class for the management of bioenergetic module */
    private EnergyBudget bioenProcess;

    int yearMaxSeeding;

    public SimulationStep(int rank) {
        super(rank);
    }

    /**
     * Initialization of the step.
     * It is called once at the beginning of the simulation.
     */
    public void init() {


        // Seeding duration (expressed in number of time steps)
        yearMaxSeeding = 0;
        if (!getConfiguration().isNull("population.seeding.year.max")) {
            yearMaxSeeding = getConfiguration().getInt("population.seeding.year.max")
                    * getConfiguration().getNStepYear();
        } else {
            if(!getConfiguration().getBoolean("population.initialization.relativebiomass.enabled")) {
                for (int i = 0; i < getConfiguration().getNSpecies(); i++) {
                    yearMaxSeeding = Math.max(yearMaxSeeding, getSpecies(i).getLifespanDt());
                }
                warning("Did not find parameter population.seeding.year.max. Osmose set it to "
                        + ((float) yearMaxSeeding / getConfiguration().getNStepYear())
                        + " years, the lifespan of the longest-lived species.");
            }
        }

        // Initialize general mortality process
        mortalityProcess = new MortalityProcess(getRank());
        mortalityProcess.init();

        backgroundProcess = new BackgroundProcess(getRank());
        backgroundProcess.init();

        // If the bioen module is activated, no more use of the
        // GrowthProcess class, use of the EnergyBudget module instead.
        if (!getConfiguration().isBioenEnabled()) {
            // initiliaza growth process
            growthProcess = new GrowthProcess(getRank());
            growthProcess.init();
        } else {
            try {
                bioenProcess = new EnergyBudget(getRank());
                bioenProcess.init();
            } catch (IOException ex) {
                Logger.getLogger(SimulationStep.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        // Reproduction processes
        if (!getConfiguration().isBioenEnabled()) {
            reproductionProcess = new ReproductionProcess(getRank());
            reproductionProcess.init();
        } else {
            reproductionProcess = new BioenReproductionProcess(getRank());
            reproductionProcess.init();
        }


        // Incoming flux
        if (getConfiguration().isIncomingFluxEnabled()) {
            incomingFLuxProcess = new IncomingFluxProcess(getRank());
            incomingFLuxProcess.init();
        }

        // Movement of the schools
        movementProcess = new MovementProcess(getRank());
        movementProcess.init();

        // Indicators
        indicators = new OutputManager(getRank());
        indicators.init();

        // Record time step 0 in the output
        recordStep0 = getConfiguration().getBoolean("output.step0.include", false);
    }

    /**
     * This functions details what must be done in one time step.
     * It is called every time step of the simulation.
     * @param iStepSimu , the current time step of the simulation
     */
    public void step(int iStepSimu) {

        debug("  step " + iStepSimu);

        // Incoming flux
        if (getConfiguration().isIncomingFluxEnabled()) {
            incomingFLuxProcess.run();
        }

        // Reset the background schools for the given time step
        backgroundProcess.run();
        getBkgSchoolSet().getSchools().forEach((school) -> {
            school.init();
        });

        // Reset some school state variables
        getSchoolSet().getSchools().forEach((school) -> {
            school.init();
        });

        for(ResourceForcing resource : getResourceForcing()) {
            resource.update(iStepSimu);
        }

        // Spatial distribution
        movementProcess.run();

        // Some indicators might need a snapshot of the population
        // at the beginning of the step
        indicators.initStep();

        // Save 1st time step
        if (recordStep0 && iStepSimu == 0) {
            indicators.update(-1);
        }


        getSimulation().resetSSB();
        getSchoolSet().getSchools().forEach((school) -> {
            getSimulation().incrementSSB(school);
        });


        // Compute mortality
        // (predation + fishing + additional mortality + starvation)
        mortalityProcess.run();

        // If the bioen module is activated, then use of the
        // bioenProcess model instead.
        if (!getConfiguration().isBioenEnabled()) {
            // Growth
            growthProcess.run();
        } else {
            bioenProcess.run();
        }

        // Updates the aging mortality variable
        getSchoolSet().updateAgingMortality(iStepSimu);

        // Updates the aging mortality variable
        getSchoolSet().updateSpawningMortality(iStepSimu);

        // Reproduction
        reproductionProcess.run();

        // Save steps
        indicators.update(iStepSimu);

        // Remove all dead schools
        getSchoolSet().removeDeadSchools();

        // merge schools obtained by reproduction into the classic pool.
        getSchoolSet().mergeSchoolSets();

        double abundance_totale [] = new double[getConfiguration().getNSpecies()];

        if (getConfiguration().killIfNoSchool() && this.getSimulation().getIndexTimeSimu() > yearMaxSeeding) {
            // if enabled, check whether abundance for each species is > 0
            int error = 0;
            for (int iSpecies = 0; iSpecies < getConfiguration().getNSpecies(); iSpecies++) {
                abundance_totale[iSpecies] = 0;
                for (IAggregation sch : getSchoolSet().getSchools(getConfiguration().getSpecies(iSpecies))) {
                    abundance_totale[iSpecies] += sch.getInstantaneousAbundance();
                }

                // increments the error message.
                if (abundance_totale[iSpecies] == 0) {
                    warning("No school survived for species " + getConfiguration().getSpecies(iSpecies).getName() + ". The simulation will stop");
                    error += 1;
                }
            } // end of loop on species

            // if at least one species is collapsing, kill the simulation
            if(error > 0)  {
                System.exit(1);
            }
        } // end of check on whether to shut down the simu if collapse
    }

    /**
     * This function is called once at the end of the simulation.
     * It basically servers to cleanup and close down everything before exiting.
     */
    public void end() {
        // close indicators on last step
        indicators.close();
    }
}
