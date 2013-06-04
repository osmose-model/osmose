/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.step;

import fr.ird.osmose.School;
import fr.ird.osmose.output.Indicators;
import fr.ird.osmose.process.AbstractProcess;
import fr.ird.osmose.process.GrowthProcess;
import fr.ird.osmose.process.IncomingFluxProcess;
import fr.ird.osmose.process.MPAProcess;
import fr.ird.osmose.process.MigrationProcess;
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
     * Incoming flux of biomass
     */
    private AbstractProcess incomingFLuxProcess;
    /*
     * Generic mortality process that encompasses all mortality processes
     */
    private AbstractProcess mortalityProcess;
    /*
     * Movement process
     */
    private AbstractProcess movementProcess;
        /*
     * Migration process
     */
    private AbstractProcess migrationProcess;
    /*
     * MPA process
     */
    private AbstractProcess mpaProcess;
    /*
     * List of indicators
     */
    Indicators indicators;
    
    public ConcomitantMortalityStep(int indexSimulation) {
        super(indexSimulation);
    }

    @Override
    public void init() {

        // Initialize general mortality process
        mortalityProcess = new MortalityProcess(getIndexSimulation());
        mortalityProcess.init();

        // initiliaza growth process
        growthProcess = new GrowthProcess(getIndexSimulation());
        growthProcess.init();

        // Reproduction processes
        reproductionProcess = new ReproductionProcess(getIndexSimulation());
        reproductionProcess.init();
        
        // Incoming flux
        incomingFLuxProcess = new IncomingFluxProcess(getIndexSimulation());
        incomingFLuxProcess.init();

        // Movement of the schools
        movementProcess = new MovementProcess(getIndexSimulation());
        movementProcess.init();
        
        // Migratrion process
        migrationProcess = new MigrationProcess(getIndexSimulation());
        migrationProcess.init();
        
        // MPA
        mpaProcess = new MPAProcess(getIndexSimulation());
        mpaProcess.init();
        
        // Indicators
        indicators = new Indicators(getIndexSimulation());
        indicators.init();
    }

    @Override
    public void step(int iStepSimu) {
        
        // Incoming flux
        incomingFLuxProcess.run();

        // Reset some school state variables 
        for (School school : getSchoolSet()) {
            school.initStep();
        }
        
        // Update plankton concentration
        for (int p = 0; p < getConfiguration().getNPlankton(); p++) {
            getSimulation().getPlankton(p).update(iStepSimu);
        }

        // Some indicators might need a snapshot of the population
        // at the beginning of the step
        indicators.initStep();
        
        // Migration
        migrationProcess.run();

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
        getSchoolSet().removeDeadSchools();
    }
    
    @Override
    public void end() {
        // close indicators on last step
        indicators.close();
    }
}
