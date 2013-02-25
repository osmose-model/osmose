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

    public Indicators() {
        indicators = new ArrayList();

        // Biomass
        indicators.add(new BiomassNoJuvIndicator());
        indicators.add(new BiomassTotIndicator());
        // Abundance
        indicators.add(new AbundanceNoJuvIndicator());
        indicators.add(new AbundanceTotIndicator());
        // Mortality
        indicators.add(new MortalityIndicator());
        // Yield
        indicators.add(new YieldIndicator());
        indicators.add(new YieldNIndicator());
        // Size
        indicators.add(new MeanSizeIndicator());
        indicators.add(new MeanSizeCatchIndicator());
        indicators.add(new SizeSpectrumIndicator());
        indicators.add(new SizeSpectrumSpeciesIndicator());
        // TL
        indicators.add(new MeanTrophicLevelIndicator());
        indicators.add(new MeanTrophicLevelCatchIndicator());
        indicators.add(new TrophicLevelSpectrumIndicator());
        // Predation
        indicators.add(new DietIndicator());
        indicators.add(new PredatorPressureIndicator());
        // Spatialized
        indicators.add(new SpatialIndicator());
        indicators.add(new LTLIndicator());
        // Temporary indicator for calib that will be deleted soon
        indicators.add(new BiomassCalibrationIndicator());
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

    public void update() {

        int year = getSimulation().getYear();
        int index = getSimulation().getIndexTimeYear();
        int nStepsYear = getOsmose().getNumberTimeStepsPerYear();
        int nStepsRecord = getOsmose().savingDtMatrix;
        //
        // UPDATE
        if (year >= getOsmose().timeSeriesStart) {
            for (Indicator indicator : indicators) {
                if (indicator.isEnabled()) {
                    indicator.update();
                    if (((index + 1) % nStepsRecord) == 0) {
                        float time = getSimulation().getYear() + (getSimulation().getIndexTimeYear() + 1f) / (float) nStepsYear;
                        indicator.write(time);
                        indicator.reset();
                    }
                }
            }
        }
    }
}
