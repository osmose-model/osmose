/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.step;

import fr.ird.osmose.Indicators;
import fr.ird.osmose.School;
import fr.ird.osmose.process.AbstractProcess;
import fr.ird.osmose.process.FishingProcess;
import fr.ird.osmose.process.GrowthProcess;
import fr.ird.osmose.process.MortalityProcess;
import fr.ird.osmose.process.NaturalMortalityProcess;
import fr.ird.osmose.process.PredationProcess;
import fr.ird.osmose.process.ReproductionProcess;
import fr.ird.osmose.process.StarvationProcess;

/**
 *
 * @author pverley
 */
public class ConcomitantMortalityStep extends AbstractStep {
    
        /*
     * Growth process
     */
    AbstractProcess growthProcess;
    /*
     * Reproduction process
     */
    AbstractProcess reproductionProcess;
    /*
     * Generic mortality process that encompasses all mortality processes
     */
    AbstractProcess mortalityProcess;

    @Override
    public void init() {
        
        /*
         * We don't need to instantiate separately the four mortality processes
         * but we do need to initialize them in order to load their parameters
         * for they will be called by the other processes.
         */
        // initialize natural mortality process
        new NaturalMortalityProcess().loadParameters();

        // initialize starvation process
        new PredationProcess().loadParameters();

        // initialize starvation process
       new StarvationProcess().loadParameters();

        // initialize fishing process
        new FishingProcess().loadParameters();

        // Initialize general mortality process
        mortalityProcess = new MortalityProcess();
        mortalityProcess.loadParameters();

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

            getForcing().updatePlankton(getSimulation().getIndexTimeYear());

            // Spatial distribution (distributeSpeciesIni() for year0 & indexTime0)
            if (getSimulation().getIndexTimeSimu() > 0) {
                getSimulation().distributeSpecies();
            }

            // Preliminary actions before mortality processes
            getSimulation().saveBiomassBeforeMortality();

            // Compute mortality
            // (predation + fishing + natural mortality + starvation)
            for (School school : getPopulation()) {
                school.resetDietVariables();
                school.nDeadFishing = 0;
                school.nDeadNatural = 0;
                school.nDeadPredation = 0;
                school.nDeadStarvation = 0;
                school.biomassToPredate = PredationProcess.computeBiomassToPredate(school, 1);
                school.preyedBiomass = 0;
            }
            mortalityProcess.run();


            // Growth
            growthProcess.run();

            // Save steps
            if (getOsmose().spatializedOutputs[getOsmose().numSerie]) {
                getSimulation().saveSpatializedStep();
            }
            Indicators.updateAndWriteIndicators();

            // Reproduction
            reproductionProcess.run();

            // Remove all dead schools
            getPopulation().removeDeadSchools();
    }
    
}
