package fr.ird.osmose.output;

import fr.ird.osmose.School;
import java.io.File;

/**
 *
 * @author pverley
 */
public class MeanTrophicLevelSizeIndicator extends AbstractIndicator {

    private double[][] meanTL;
    private double[][] biomass;
    // Maximal size (cm) of the size spectrum.
    public float spectrumMaxSize;
    // Range (cm) of size classes.
    private float classRange;
    // Number of size classes in the discrete spectrum
    private int nSizeClass;
    // discrete size spectrum
    private float[] tabSizes;

    public MeanTrophicLevelSizeIndicator(int replica) {
        super(replica);
        initializeSizeSpectrum();
    }

    private void initializeSizeSpectrum() {

        if (!isEnabled()) {
            return;
        }

        spectrumMaxSize = getConfiguration().getSpectrumMaxSize();
        // Minimal size (cm) of the size spectrum.
        float spectrumMinSize = getConfiguration().getSpectrumMinSize();
        // Range (cm) of size classes.
        classRange = getConfiguration().getSpectrumClassRange();

        //initialisation of the size spectrum features
        nSizeClass = (int) Math.ceil(spectrumMaxSize / classRange);//size classes of 5 cm

        tabSizes = new float[nSizeClass];
        tabSizes[0] = spectrumMinSize;
        for (int i = 1; i < nSizeClass; i++) {
            tabSizes[i] = i * classRange;
        }
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder("Trophic");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getOutputPrefix());
        filename.append("_meanTLPerSize_Simu");
        filename.append(getSimulation().getReplica());
        filename.append(".csv");
        return filename.toString();
    }

    @Override
    String getDescription() {
        return "Mean Trophic Level of fish species by size class of " + classRange + " cm";
    }

    @Override
    String[] getHeaders() {
        String[] headers = new String[getNSpecies() + 1];
        headers[0] = "size";
        for (int iSpecies = 0; iSpecies < getNSpecies(); iSpecies++) {
            headers[iSpecies + 1] = getSpecies(iSpecies).getName();
        }
        return headers;
    }

    @Override
    public void initStep() {
        // nothing to do
    }

    @Override
    public void reset() {
        meanTL = new double[getNSpecies()][tabSizes.length];
        biomass = new double[getNSpecies()][tabSizes.length];
    }

    @Override
    public void update() {
        for (School school : getPopulation().getAliveSchools()) {
            int i = school.getSpeciesIndex();
            double biom = school.getInstantaneousBiomass();
            int rank = getSizeRank(school);
            meanTL[i][rank] += biom * school.getTrophicLevel();
            biomass[i][rank] += biom;
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
    public boolean isEnabled() {
        return getConfiguration().isTLOutput();
    }

    @Override
    public void write(float time) {

        double[][] values = new double[nSizeClass][getNSpecies() + 1];
        for (int iSize = 0; iSize < nSizeClass; iSize++) {
            // Size
            values[iSize][0] = tabSizes[iSize];
            for (int iSpec = 0; iSpec < getNSpecies(); iSpec++) {
                // TL
                if (biomass[iSpec][iSize] > 0.d) {
                    meanTL[iSpec][iSize] = (float) (meanTL[iSpec][iSize] / biomass[iSpec][iSize]);
                } else {
                    meanTL[iSpec][iSize] = Double.NaN;
                }
                values[iSize][iSpec + 1] = meanTL[iSpec][iSize];
            }
        }

        writeVariable(time, values);
    }
}
