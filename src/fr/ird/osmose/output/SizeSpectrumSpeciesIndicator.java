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
public class SizeSpectrumSpeciesIndicator extends AbstractIndicator {

    private double[][] sizeSpectrum;
    
     public SizeSpectrumSpeciesIndicator(int replica) {
        super(replica);
    }

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

        double[][] values = new double[getOsmose().nbSizeClass][getNSpecies() + 1];
        for (int iSize = 0; iSize < getOsmose().nbSizeClass; iSize++) {
            values[iSize][0] = getOsmose().tabSizes[iSize];
            for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
                values[iSize][iSpec] = sizeSpectrum[iSpec][iSize] / getOsmose().getRecordFrequency();
            }
        }
        writeVariable(time, values);
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder("SizeIndicators");
        filename.append(File.separatorChar);
        filename.append(getOsmose().outputPrefix);
        filename.append("_SizeSpectrumSpecies_Simu");
        filename.append(getSimulation().getReplica());
        filename.append(".csv");
        return filename.toString();

    }

    @Override
    String getDescription() {
        return "Distribution of fish species abundance in size classes (cm). For size class i, the number of fish in [i,i+1[ is reported.";
    }

    @Override
    String[] getHeaders() {
        String[] headers = new String[getNSpecies() + 1];
        headers[0] = "Size";
        for (int i = 0; i < getNSpecies(); i++) {
            headers[i + 1] = getSimulation().getSpecies(i).getName();
        }
        return headers;
    }
}
