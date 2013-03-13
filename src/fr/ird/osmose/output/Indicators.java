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
        indicators.add(new BiomassIndicator(replica, "output.biomass.enabled"));
        // Abundance
        indicators.add(new AbundanceIndicator(replica, "output.abundance.enabled"));
        // Mortality
        indicators.add(new MortalityIndicator(replica, "output.mortality.enabled"));
        // Yield
        indicators.add(new YieldIndicator(replica, "output.yield.biomass.enabled"));
        indicators.add(new YieldNIndicator(replica, "output.yield.abundance.enabled"));
        // Size
        indicators.add(new MeanSizeIndicator(replica, "output.size.enabled"));
        indicators.add(new MeanSizeCatchIndicator(replica, "output.size.catch.enabled"));
        indicators.add(new SizeSpectrumIndicator(replica, "output.size.spectrum.enabled"));
        indicators.add(new SizeSpectrumSpeciesNIndicator(replica, "output.size.spectrum.perSpecies.N.enabled"));
        indicators.add(new SizeSpectrumSpeciesBIndicator(replica, "output.size.spectrum.perSpecies.B.enabled"));
        indicators.add(new MeanSizeSpeciesIndicator(replica, "output.size.perSpecies.enabled"));
        // TL
        indicators.add(new MeanTrophicLevelIndicator(replica, "output.tl.enabled"));
        indicators.add(new MeanTrophicLevelCatchIndicator(replica, "output.tl.catch.enabled"));
        indicators.add(new TrophicLevelSpectrumIndicator(replica, "output.tl.spectrum.enabled"));
        indicators.add(new MeanTrophicLevelSizeIndicator(replica, "output.tl.perSize.enabled"));
        indicators.add(new MeanTrophicLevelAgeIndicator(replica, "output.tl.perAge.enabled"));
        // Predation
        indicators.add(new DietIndicator(replica, "output.diet.composition.enabled"));
        indicators.add(new PredatorPressureIndicator(replica, "output.diet.pressure.enabled"));
        // Spatialized
        indicators.add(new SpatialIndicator(replica, "output.spatial.enabled"));
        indicators.add(new LTLIndicator(replica, "output.spatial.ltl.enabled"));
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
                        float time = (float) (iStepSimu + 1) / getConfiguration().getNumberTimeStepsPerYear();
                        indicator.write(time);
                        indicator.reset();
                    }
                }
            }
        }
    }
}
