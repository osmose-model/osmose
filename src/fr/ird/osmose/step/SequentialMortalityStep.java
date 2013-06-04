/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.step;

import fr.ird.osmose.School;
import fr.ird.osmose.output.Indicators;
import fr.ird.osmose.process.AbstractProcess;
import fr.ird.osmose.process.FishingProcess;
import fr.ird.osmose.process.GrowthProcess;
import fr.ird.osmose.process.IncomingFluxProcess;
import fr.ird.osmose.process.MPAProcess;
import fr.ird.osmose.process.MigrationProcess;
import fr.ird.osmose.process.MovementProcess;
import fr.ird.osmose.process.NaturalMortalityProcess;
import fr.ird.osmose.process.PredationProcess;
import fr.ird.osmose.process.ReproductionProcess;
import fr.ird.osmose.process.StarvationProcess;

/**
 *
 * @author pverley
 */
public class SequentialMortalityStep extends AbstractStep {

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
     * Fishing process
     */
    private AbstractProcess fishingProcess;
    /*
     * Natural mortality process
     */
    private AbstractProcess naturalMortalityProcess;
    /*
     * Starvation mortality process
     */
    private AbstractProcess starvationProcess;
    /*
     * Predation mortality process
     */
    private AbstractProcess predationProcess;
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
    private Indicators indicators;

    public SequentialMortalityStep(int indexSimulation) {
        super(indexSimulation);
    }

    @Override
    public void init() {
        // initialize natural mortality process
        naturalMortalityProcess = new NaturalMortalityProcess(getIndexSimulation());
        naturalMortalityProcess.init();

        // initialize starvation process
        predationProcess = new PredationProcess(getIndexSimulation());
        predationProcess.init();

        // initialize starvation process
        starvationProcess = new StarvationProcess(getIndexSimulation());
        starvationProcess.init();


        // initialize fishing process
        fishingProcess = new FishingProcess(getIndexSimulation());
        fishingProcess.init();

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

        // Natural mortality (due to other predators)
        naturalMortalityProcess.run();

        // Predation
        predationProcess.run();

        // Starvation
        starvationProcess.run();

        // Growth
        growthProcess.run();

        // Fishing
        fishingProcess.run();

        // Save steps
        indicators.update(iStepSimu);

        // Reproduction
        reproductionProcess.run();

        // Remove dead school
        getSchoolSet().removeDeadSchools();
    }
    
    @Override
    public void end() {
        // close indicators on last step
        indicators.close();
    }
}
