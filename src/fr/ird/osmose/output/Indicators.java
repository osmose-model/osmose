/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.output;

import fr.ird.osmose.util.SimulationLinker;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author pverley
 */
public class Indicators extends SimulationLinker {

    // List of the indicators
    final private List<Indicator> indicators;

    public Indicators(int replica) {
        super(replica);
        
        indicators = new ArrayList();

        // Biomass
        indicators.add(new BiomassIndicator(replica));
        // Abundance
        indicators.add(new AbundanceIndicator(replica));
        // Mortality
        indicators.add(new MortalityIndicator(replica));
        // Yield
        indicators.add(new YieldIndicator(replica));
        indicators.add(new YieldNIndicator(replica));
        // Size
        indicators.add(new MeanSizeIndicator(replica));
        indicators.add(new MeanSizeCatchIndicator(replica));
        indicators.add(new SizeSpectrumIndicator(replica));
        indicators.add(new SizeSpectrumSpeciesNIndicator(replica));
        indicators.add(new SizeSpectrumSpeciesBIndicator(replica));
        indicators.add(new MeanSizeSpeciesIndicator(replica));
        // TL
        indicators.add(new MeanTrophicLevelIndicator(replica));
        indicators.add(new MeanTrophicLevelCatchIndicator(replica));
        indicators.add(new TrophicLevelSpectrumIndicator(replica));
        indicators.add(new MeanTrophicLevelSizeIndicator(replica));
        indicators.add(new MeanTrophicLevelAgeIndicator(replica));
        // Predation
        indicators.add(new DietIndicator(replica));
        indicators.add(new PredatorPressureIndicator(replica));
        // Spatialized
        indicators.add(new SpatialIndicator(replica));
        indicators.add(new LTLIndicator(replica));
    }
    
    public void init() {
        for (Indicator indicator : indicators) {
            if (indicator.isEnabled()) {
                indicator.init();
                indicator.reset();
            }
        }
    }

    public void close() {
        for (Indicator indicator : indicators) {
            if (indicator.isEnabled()) {
                indicator.close();
            }
        }
    }

    public void initStep() {
        for (Indicator indicator : indicators) {
            if (indicator.isEnabled()) {
                indicator.initStep();
            }
        }
    }

    public void update(int iStepSimu) {

        int year = getSimulation().getYear();
        int nStepsRecord = getConfiguration().getRecordFrequency();
        //
        // UPDATE
        if (year >= getConfiguration().yearStartSaving) {
            for (Indicator indicator : indicators) {
                if (indicator.isEnabled()) {
                    indicator.update();
                    if (((iStepSimu + 1) % nStepsRecord) == 0) {
                        float time = (float) (iStepSimu + 1) / getConfiguration().getNumberTimeStepsPerYear();
                        indicator.write(time);
                        indicator.reset();
                    }
                }
            }
        }
    }
}
