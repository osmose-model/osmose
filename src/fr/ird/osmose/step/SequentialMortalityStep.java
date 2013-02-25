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
import fr.ird.osmose.process.MPAProcess;
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
     * MPA process
     */
    private AbstractProcess mpaProcess;
    /*
     * List of indicators
     */
    private Indicators indicators;

    @Override
    public void init() {
        // initialize natural mortality process
        naturalMortalityProcess = new NaturalMortalityProcess();
        naturalMortalityProcess.init();

        // initialize starvation process
        predationProcess = new PredationProcess();
        predationProcess.init();

        // initialize starvation process
        starvationProcess = new StarvationProcess();
        starvationProcess.init();


        // initialize fishing process
        fishingProcess = new FishingProcess();
        fishingProcess.init();

        // initiliaza growth process
        growthProcess = new GrowthProcess();
        growthProcess.init();

        // Reproduction processes
        reproductionProcess = new ReproductionProcess();
        reproductionProcess.init();

        // Movement of the schools
        movementProcess = new MovementProcess();
        movementProcess.init();

        // MPA
        mpaProcess = new MPAProcess();
        mpaProcess.init();

        // Indicators
        indicators = new Indicators();
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

        // Spatial distribution
        movementProcess.run();

        // Update MPA
        mpaProcess.run();

        // Natural mortality (due to other predators)
        naturalMortalityProcess.run();

        // Update plankton concentration
        for (int p = 0; p < getOsmose().getNumberLTLGroups(); p++) {
            getSimulation().getPlankton(p).update(iStepSimu);
        }

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
        getPopulation().removeDeadSchools();

        // close indicators on last step
        if (isLastStep(iStepSimu)) {
            indicators.close();
        }
    }
}
