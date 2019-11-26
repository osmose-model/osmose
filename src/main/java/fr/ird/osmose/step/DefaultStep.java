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
package fr.ird.osmose.step;

import fr.ird.osmose.output.OutputManager;
import fr.ird.osmose.process.GrowthProcess;
import fr.ird.osmose.process.IncomingFluxProcess;
import fr.ird.osmose.process.MortalityProcess;
import fr.ird.osmose.process.MovementProcess;
import fr.ird.osmose.process.ReproductionProcess;
import fr.ird.osmose.process.bioen.BioenReproductionProcess;
import fr.ird.osmose.process.bioen.EnergyBudget;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pverley
 */
public class DefaultStep extends AbstractStep {
    
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

    public DefaultStep(int rank) {
        super(rank);
    }

    @Override
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
                Logger.getLogger(DefaultStep.class.getName()).log(Level.SEVERE, null, ex);
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

    @Override
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

        // Update LTL biomass
        for (int iRsc = 0; iRsc < getConfiguration().getNRscSpecies(); iRsc++) {
            getResourceForcing(iRsc).update(iStepSimu);
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

    @Override
    public void end() {
        // close indicators on last step
        indicators.close();
    }
}
