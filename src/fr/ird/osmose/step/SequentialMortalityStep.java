/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.step;

import fr.ird.osmose.Indicators;
import fr.ird.osmose.process.AbstractProcess;
import fr.ird.osmose.process.FishingProcess;
import fr.ird.osmose.process.GrowthProcess;
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
    
    @Override
    public void init() {
        // initialize natural mortality process
        naturalMortalityProcess = new NaturalMortalityProcess();
        naturalMortalityProcess.loadParameters();
        
        // initialize starvation process
        predationProcess = new PredationProcess();
        predationProcess.loadParameters();
        
        // initialize starvation process
        starvationProcess = new StarvationProcess();
        starvationProcess.loadParameters();


        // initialize fishing process
        fishingProcess = new FishingProcess();
        fishingProcess.loadParameters();

        // initiliaza growth process
        growthProcess = new GrowthProcess();
        growthProcess.loadParameters();

        // Reproduction processes
        reproductionProcess = new ReproductionProcess();
        reproductionProcess.loadParameters();
    }

    @Override
    public void step() {
        // Update some stages at the begining of the step
            getSimulation().updateStages();

            // Spatial distribution (distributeSpeciesIni() for year0 & indexTime0)
                getSimulation().distributeSpecies();
            

            // Natural mortality (due to other predators)
            naturalMortalityProcess.run();

            getForcing().updatePlankton(getSimulation().getIndexTimeYear());

            getSimulation().saveBiomassBeforeMortality();
            
            // Predation
            predationProcess.run();
            
            // Starvation
            starvationProcess.run();

            // Growth
            growthProcess.run();

            // Fishing
            fishingProcess.run();

            // Save steps
            Indicators.updateAndWriteIndicators();
            if (getOsmose().spatializedOutputs[getOsmose().numSerie]) {
                getSimulation().saveSpatializedStep();
            }

            // Reproduction
            reproductionProcess.run();

            // Remove dead school
            getPopulation().removeDeadSchools();
    }
    
}
