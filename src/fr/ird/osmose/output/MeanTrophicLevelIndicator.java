/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.output;

import fr.ird.osmose.School;
import java.io.File;

/**
 *
 * @author pverley
 */
public class MeanTrophicLevelIndicator extends SchoolBasedIndicator {

    private double[] meanTL;
    private double[] biomass;
    // Catches
    private double[] meanTLCatch;
    private double[] yield;
    
    @Override
    public void init() {
        // Nothing to do
    }

    @Override
    public void reset() {
        meanTL = new double[getNSpecies()];
        biomass = new double[getNSpecies()];
        // Catches
        meanTLCatch = new double[getNSpecies()];
        yield = new double[getNSpecies()];
    }

    @Override
    public void update(School school) {
        if (school.getAgeDt() >= school.getSpecies().indexAgeClass0) {
            int i = school.getSpeciesIndex();
            meanTL[i] += school.getBiomass() * school.trophicLevel;
            biomass[i] += school.getBiomass();
            // Catches
            meanTLCatch[i] += school.trophicLevel * school.adb2biom(school.nDeadFishing);
            yield[i] += school.adb2biom(school.nDeadFishing);
        }
    }

    @Override
    public boolean isEnabled() {
        return getOsmose().isTLOutput();
    }

    @Override
    public void write(float time) {
        StringBuilder filename;
        String description;

        for (int i = 0; i < getSimulation().getNumberSpecies(); i++) {
            if (biomass[i] > 0.d) {
                meanTL[i] = (float) (meanTL[i] / biomass[i]);
            } else {
                meanTL[i] = Double.NaN;
            }
            if (yield[i] > 0) {
                meanTLCatch[i] = meanTLCatch[i] / yield[i];
            } else {
                meanTLCatch[i] = Double.NaN;
            }
        }

        // Mean TL
        filename = new StringBuilder("Trophic");
        filename.append(File.separatorChar);
        filename.append(getOsmose().outputPrefix[getOsmose().numSerie]);
        filename.append("_meanTL_Simu");
        filename.append(getOsmose().numSimu);
        filename.append(".csv");
        description = "Mean Trophic Level of fish species, weighted by fish biomass, and including/excluding first ages specified in input (in calibration file)";
        Indicators.writeVariable(time, meanTL, filename.toString(), description);

        // Mean TL for catches
        filename = new StringBuilder("Trophic");
        filename.append(File.separatorChar);
        filename.append(getOsmose().outputPrefix[getOsmose().numSerie]);
        filename.append("_meanTLCatch_Simu");
        filename.append(getOsmose().numSimu);
        filename.append(".csv");
        description = "Mean Trophic Level of fish species, weighted by fish catch";
        Indicators.writeVariable(time, meanTLCatch, filename.toString(), description);
    }
}
