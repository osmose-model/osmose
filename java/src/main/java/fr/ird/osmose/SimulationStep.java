/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
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

    public SimulationStep(int rank) {
        super(rank);
    }

    /**
     * Initialization of the step.
     * It is called once at the beginning of the simulation.
     */
    public void init() {
        
        // Initialize general mortality process
        mortalityProcess = new MortalityProcess(getRank());
        mortalityProcess.init();

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

        // Reset some school state variables
        getSchoolSet().getSchools().forEach((school) -> {
            school.init();
        });

        for(ResourceForcing resource : getResourceForcing().values()) {
            resource.update(iStepSimu);
        }

        // Some indicators might need a snapshot of the population
        // at the beginning of the step
        indicators.initStep();

        // Spatial distribution
        movementProcess.run();

        // Save 1st time step
        if (recordStep0 && iStepSimu == 0) {
            indicators.update(-1);
        }

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

        // Save steps
        indicators.update(iStepSimu);

        // Reproduction
        reproductionProcess.run();

        // Remove all dead schools
        getSchoolSet().removeDeadSchools();
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
