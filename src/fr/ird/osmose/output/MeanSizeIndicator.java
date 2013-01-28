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
public class MeanSizeIndicator extends AbstractIndicator {

    private double[] meanSize;
    private double[] abundance;
    // Catches
    private double[] meanSizeCatch;    
    private double[] yieldN;
    
    @Override
    public void init() {
        // Nothing to do
    }

    @Override
    public void reset() {

        meanSize = new double[getNSpecies()];
        abundance = new double[getNSpecies()];
        // Catches
        meanSizeCatch = new double[getNSpecies()];
        yieldN = new double[getNSpecies()];
    }

    @Override
    public void update(School school) {
        if (school.getAgeDt() > school.getSpecies().indexAgeClass0) {
            int i = school.getSpeciesIndex();
            meanSize[i] += school.getAbundance() * school.getLength();
            abundance[i] += school.getAbundance();
            // Catches
            meanSizeCatch[i] += school.nDeadFishing * school.getLength();
            yieldN[i] += school.nDeadFishing;
        }
    }

    @Override
    public boolean isEnabled() {
        return getOsmose().isMeanSizeOutput();
    }

    @Override
    public void write(float time) {
        StringBuilder filename;
        String description;

        for (int i = 0; i < getSimulation().getNumberSpecies(); i++) {
            if (abundance[i] > 0) {
                meanSize[i] = (float) (meanSize[i] / abundance[i]);
            } else {
                meanSize[i] = Double.NaN;
            }
            if (yieldN[i] > 0) {
                meanSizeCatch[i] = meanSizeCatch[i] / yieldN[i];
            } else {
                meanSizeCatch[i] = Double.NaN;
            }
        }

        filename = new StringBuilder("SizeIndicators");
        filename.append(File.separatorChar);
        filename.append(getOsmose().outputPrefix[getOsmose().numSerie]);
        filename.append("_meanSize_Simu");
        filename.append(getOsmose().numSimu);
        filename.append(".csv");
        description = "Mean size of fish species in cm, weighted by fish numbers, and excluding first ages specified in input (in calibration file)";
        Indicators.writeVariable(time, meanSize, filename.toString(), description);

        filename = new StringBuilder("SizeIndicators");
        filename.append(File.separatorChar);
        filename.append(getOsmose().outputPrefix[getOsmose().numSerie]);
        filename.append("_meanSizeCatch_Simu");
        filename.append(getOsmose().numSimu);
        filename.append(".csv");
        description = "Mean size of fish species in cm, weighted by fish numbers in the catches";
        Indicators.writeVariable(time, meanSizeCatch, filename.toString(), description);
    }
}
