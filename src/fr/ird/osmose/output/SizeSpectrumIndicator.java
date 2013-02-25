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
public class SizeSpectrumIndicator extends AbstractIndicator {

    private double[][] sizeSpectrum;

    @Override
    public void initStep() {
        // Nothing to do
    }

    @Override
    public void reset() {
        sizeSpectrum = new double[getNSpecies()][getOsmose().tabSizes.length];
    }

    @Override
    public void update() {
        for (School school : getPopulation().getAliveSchools()) {
            sizeSpectrum[school.getSpeciesIndex()][getSizeRank(school)] += school.getAbundance();
        }
    }

    @Override
    public boolean isEnabled() {
        return getOsmose().isSizeSpectrumOutput() || getOsmose().isSizeSpectrumSpeciesOutput();
    }

    private int getSizeRank(School school) {

        int iSize = getOsmose().tabSizes.length - 1;
        if (school.getLength() <= getOsmose().spectrumMaxSize) {
            while (school.getLength() < getOsmose().tabSizes[iSize]) {
                iSize--;
            }
        }
        return iSize;
    }

    @Override
    public void write(float time) {

        float[][] values = new float[getOsmose().nbSizeClass][4];
        for (int iSize = 0; iSize < getOsmose().nbSizeClass; iSize++) {
            // Size
            values[iSize][0] = getOsmose().tabSizes[iSize];
            // Abundance
            double sum = 0f;
            for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
                sum += sizeSpectrum[iSpec][iSize] / getOsmose().getRecordFrequency();
            }
            values[iSize][1] = (float) sum;
            // ln(Size)
            values[iSize][0] = getOsmose().tabSizesLn[iSize];        
            // ln(Abundance)
            values[iSize][1] = (float) Math.log(sum);
        }
        

    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder("SizeIndicators");
        filename.append(File.separatorChar);
        filename.append(getOsmose().outputPrefix);
        filename.append("_SizeSpectrum_Simu");
        filename.append(getSimulation().getReplica());
        filename.append(".csv");
        return filename.toString();

    }

    @Override
    String getDescription() {
        return "Distribution of fish abundance in size classes (cm). For size class i, the number of fish in [i,i+1[ is reported. In logarithm, we consider the median of the size class, ie Ln(size [i]) = Ln((size [i]+size[i+1])/2)";
    }

    @Override
    String[] getHeaders() {
        return new String[]{"Size", "Abundance", "ln(size)", "ln(Abd)"};
    }
}
