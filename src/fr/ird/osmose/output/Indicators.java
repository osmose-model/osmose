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

    public Indicators(int indexSimulation) {
        super(indexSimulation);
        
        indicators = new ArrayList();

        // Biomass
        indicators.add(new BiomassIndicator(indexSimulation, "output.biomass.enabled"));
        // Abundance
        indicators.add(new AbundanceIndicator(indexSimulation, "output.abundance.enabled"));
        // Mortality
        indicators.add(new MortalityIndicator(indexSimulation, "output.mortality.enabled"));
        // Yield
        indicators.add(new YieldIndicator(indexSimulation, "output.yield.biomass.enabled"));
        indicators.add(new YieldNIndicator(indexSimulation, "output.yield.abundance.enabled"));
        // Size
        indicators.add(new MeanSizeIndicator(indexSimulation, "output.size.enabled"));
        indicators.add(new MeanSizeCatchIndicator(indexSimulation, "output.size.catch.enabled"));
        indicators.add(new SizeSpectrumIndicator(indexSimulation, "output.size.spectrum.enabled"));
        indicators.add(new SizeSpectrumSpeciesNIndicator(indexSimulation, "output.size.spectrum.perSpecies.N.enabled"));
        indicators.add(new SizeSpectrumSpeciesBIndicator(indexSimulation, "output.size.spectrum.perSpecies.B.enabled"));
        indicators.add(new MeanSizeSpeciesIndicator(indexSimulation, "output.size.perSpecies.enabled"));
        // TL
        indicators.add(new MeanTrophicLevelIndicator(indexSimulation, "output.tl.enabled"));
        indicators.add(new MeanTrophicLevelCatchIndicator(indexSimulation, "output.tl.catch.enabled"));
        indicators.add(new TrophicLevelSpectrumIndicator(indexSimulation, "output.tl.spectrum.enabled"));
        indicators.add(new MeanTrophicLevelSizeIndicator(indexSimulation, "output.tl.perSize.enabled"));
        indicators.add(new MeanTrophicLevelAgeIndicator(indexSimulation, "output.tl.perAge.enabled"));
        // Predation
        indicators.add(new DietIndicator(indexSimulation, "output.diet.composition.enabled"));
        indicators.add(new PredatorPressureIndicator(indexSimulation, "output.diet.pressure.enabled"));
        // Spatialized
        indicators.add(new SpatialIndicator(indexSimulation, "output.spatial.enabled"));
        indicators.add(new LTLIndicator(indexSimulation, "output.spatial.ltl.enabled"));
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
        int nStepsRecord = getConfiguration().getInt("output.recordfrequency.ndt");
        //
        // UPDATE
        if (year >= getConfiguration().getInt("output.start.year")) {
            for (Indicator indicator : indicators) {
                if (indicator.isEnabled()) {
                    indicator.update();
                    if (((iStepSimu + 1) % nStepsRecord) == 0) {
                        float time = (float) (iStepSimu + 1) / getConfiguration().getNStepYear();
                        indicator.write(time);
                        indicator.reset();
                    }
                }
            }
        }
    }
}
