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
public class SizeSpectrumSpeciesBIndicator extends AbstractIndicator {

    private double[][] sizeSpectrum;
    // Minimal size (cm) of the size spectrum.
    public float spectrumMinSize;
    // Maximal size (cm) of the size spectrum.
    public float spectrumMaxSize;
    // Range (cm) of size classes.
    private float classRange;
    // discrete size spectrum
    private float[] tabSizes;
    // Number of size classes in the discrete spectrum
    private int nSizeClass;

    public SizeSpectrumSpeciesBIndicator(int replica, String keyEnabled) {
        super(replica, keyEnabled);
        initializeSizeSpectrum();
    }

    private void initializeSizeSpectrum() {
        
        if (!isEnabled()) {
            return;
        }

        spectrumMinSize = getConfiguration().getFloat("output.size.spectrum.size.min");
        spectrumMaxSize = getConfiguration().getFloat("output.size.spectrum.size.max");
        classRange = getConfiguration().getFloat("output.size.spectrum.size.range");
        
        //initialisation of the size spectrum features
        nSizeClass = (int) Math.ceil(spectrumMaxSize / classRange);//size classes of 5 cm

        tabSizes = new float[nSizeClass];
        tabSizes[0] = spectrumMinSize;
        for (int i = 1; i < nSizeClass; i++) {
            tabSizes[i] = i * classRange;
        }
    }

    @Override
    public void initStep() {
        // Nothing to do
    }

    @Override
    public void reset() {
        sizeSpectrum = new double[getNSpecies()][tabSizes.length];
    }

    @Override
    public void update() {
        for (School school : getPopulation().getAliveSchools()) {
            sizeSpectrum[school.getSpeciesIndex()][getSizeRank(school)] += school.getInstantaneousBiomass();
        }
    }

    private int getSizeRank(School school) {

        int iSize = tabSizes.length - 1;
        if (school.getLength() <= spectrumMaxSize) {
            while (school.getLength() < tabSizes[iSize]) {
                iSize--;
            }
        }
        return iSize;
    }

    @Override
    public void write(float time) {

        double[][] values = new double[nSizeClass][getNSpecies() + 1];
        for (int iSize = 0; iSize < nSizeClass; iSize++) {
            values[iSize][0] = tabSizes[iSize];
            for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
                values[iSize][iSpec + 1] = sizeSpectrum[iSpec][iSize] / getRecordFrequency();
            }
        }
        writeVariable(time, values);
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder("SizeIndicators");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_SizeSpectrumSpeciesB_Simu");
        filename.append(getSimulation().getReplica());
        filename.append(".csv");
        return filename.toString();

    }

    @Override
    String getDescription() {
        return "Distribution of fish species biomass in size classes (cm). For size class i, the biomass of fish in [i,i+1[ is reported.";
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
