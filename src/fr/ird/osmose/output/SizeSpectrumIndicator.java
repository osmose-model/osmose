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
    // Minimal size (cm) of the size spectrum.
    public float spectrumMinSize;
    // Maximal size (cm) of the size spectrum.
    public float spectrumMaxSize;
    // Range (cm) of size classes.
    private float classRange;
    // discrete size spectrum
    private float[] tabSizes;
    // log of the discrete size spectrum
    private float[] tabSizesLn;
    // Number of size classes in the discrete spectrum
    private int nSizeClass;

    public SizeSpectrumIndicator(int replica, String keyEnabled) {
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

        tabSizesLn = new float[nSizeClass];
        tabSizesLn[0] = (float) (Math.log(classRange / 2f));
        for (int i = 1; i < nSizeClass; i++) {
            tabSizesLn[i] = (float) (Math.log(tabSizes[i] + (classRange / 2f)));
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
            sizeSpectrum[school.getSpeciesIndex()][getSizeRank(school)] += school.getInstantaneousAbundance();
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

        double[][] values = new double[nSizeClass][4];
        for (int iSize = 0; iSize < nSizeClass; iSize++) {
            // Size
            values[iSize][0] = tabSizes[iSize];
            // Abundance
            double sum = 0f;
            for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
                sum += sizeSpectrum[iSpec][iSize] / getRecordFrequency();
            }
            values[iSize][1] = sum;
            // ln(Size)
            values[iSize][2] = tabSizesLn[iSize];
            // ln(Abundance)
            values[iSize][3] = Math.log(sum);
        }
        
        writeVariable(time, values);
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder("SizeIndicators");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
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
