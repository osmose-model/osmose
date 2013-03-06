/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.step;

import fr.ird.osmose.School;
import fr.ird.osmose.output.Indicators;
import fr.ird.osmose.process.AbstractProcess;
import fr.ird.osmose.process.GrowthProcess;
import fr.ird.osmose.process.MPAProcess;
import fr.ird.osmose.process.MortalityProcess;
import fr.ird.osmose.process.MovementProcess;
import fr.ird.osmose.process.ReproductionProcess;

/**
 *
 * @author pverley
 */
public class ConcomitantMortalityStep extends AbstractStep {

    /*
     * Growth process
     */
    private AbstractProcess growthProcess;
    /*
     * Reproduction process
     */
    private AbstractProcess reproductionProcess;
    /*
     * Generic mortality process that encompasses all mortality processes
     */
    private AbstractProcess mortalityProcess;
    /*
     * Movement process
     */
    private AbstractProcess movementProcess;
    /*
     * MPA process
     */
    private AbstractProcess mpaProcess;
    /*
     * List of indicators
     */
    private Indicators indicators;
    
    public ConcomitantMortalityStep(int replica) {
        super(replica);
    }

    @Override
    public void init() {

        // Initialize general mortality process
        mortalityProcess = new MortalityProcess(getReplica());
        mortalityProcess.init();

        // initiliaza growth process
        growthProcess = new GrowthProcess(getReplica());
        growthProcess.init();

        // Reproduction processes
        reproductionProcess = new ReproductionProcess(getReplica());
        reproductionProcess.init();

        // Movement of the schools
        movementProcess = new MovementProcess(getReplica());
        movementProcess.init();
        
        // MPA
        mpaProcess = new MPAProcess(getReplica());
        mpaProcess.init();
        
        // Indicators
        indicators = new Indicators(getReplica());
        indicators.init();
    }

    @Override
    public void step(int iStepSimu) {

        // Reset some school state variables 
        for (School school : getPopulation()) {
            school.initStep();
        }

        // Some indicators might need a snapshot of the population
        // at the beginning of the step
        indicators.initStep();

        // Update plankton concentration
        for (int p = 0; p < getConfiguration().getNPlankton(); p++) {
            getSimulation().getPlankton(p).update(iStepSimu);
        }

        // Spatial distribution
        movementProcess.run();
        
        // Update MPA
        mpaProcess.run();

        // Compute mortality
        // (predation + fishing + natural mortality + starvation)
        mortalityProcess.run();

        // Growth
        growthProcess.run();

        // Save steps
        indicators.update(iStepSimu);

        // Reproduction
        reproductionProcess.run();

        // Remove all dead schools
        getPopulation().removeDeadSchools();
        
        // close indicators on last step
        if (isLastStep(iStepSimu)) {
            indicators.close();
        }
    }
}
