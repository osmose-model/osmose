/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.output;

import fr.ird.osmose.SimulationLinker;
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
        indicators.add(new BiomassNoJuvIndicator(replica));
        indicators.add(new BiomassTotIndicator(replica));
        // Abundance
        indicators.add(new AbundanceNoJuvIndicator(replica));
        indicators.add(new AbundanceTotIndicator(replica));
        // Mortality
        indicators.add(new MortalityIndicator(replica));
        // Yield
        indicators.add(new YieldIndicator(replica));
        indicators.add(new YieldNIndicator(replica));
        // Size
        indicators.add(new MeanSizeIndicator(replica));
        indicators.add(new MeanSizeCatchIndicator(replica));
        indicators.add(new SizeSpectrumIndicator(replica));
        indicators.add(new SizeSpectrumSpeciesIndicator(replica));
        // TL
        indicators.add(new MeanTrophicLevelIndicator(replica));
        indicators.add(new MeanTrophicLevelCatchIndicator(replica));
        indicators.add(new TrophicLevelSpectrumIndicator(replica));
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
        int nStepsRecord = getConfiguration().savingDtMatrix;
        //
        // UPDATE
        if (year >= getConfiguration().timeSeriesStart) {
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
