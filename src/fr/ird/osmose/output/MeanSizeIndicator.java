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
    
     public MeanSizeIndicator(int replica) {
        super(replica);
    }

    @Override
    public void initStep() {
        // Nothing to do
    }

    @Override
    public void reset() {

        meanSize = new double[getNSpecies()];
        abundance = new double[getNSpecies()];
    }

    @Override
    public void update() {
        for (School school : getPopulation().getAliveSchools()) {
            if (school.getAgeDt() > school.getSpecies().getIndexAgeClass0()) {
                int i = school.getSpeciesIndex();
                meanSize[i] += school.getAbundance() * school.getLength();
                abundance[i] += school.getAbundance();
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return getConfiguration().isMeanSizeOutput();
    }

    @Override
    public void write(float time) {

        for (int i = 0; i < getConfiguration().getNSpecies(); i++) {
            if (abundance[i] > 0) {
                meanSize[i] = (float) (meanSize[i] / abundance[i]);
            } else {
                meanSize[i] = Double.NaN;
            }
        }
        writeVariable(time, meanSize);
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder("SizeIndicators");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getOutputPrefix());
        filename.append("_meanSize_Simu");
        filename.append(getSimulation().getReplica());
        filename.append(".csv");
        return filename.toString();
    }

    @Override
    String getDescription() {
        return "Mean size of fish species in cm, weighted by fish numbers, and excluding first ages specified in input (in calibration file)";
    }
    
    @Override
    String[] getHeaders() {
        String[] species = new String[getNSpecies()];
        for (int i = 0; i < species.length; i++) {
            species[i] = getSimulation().getSpecies(i).getName();
        }
        return species;
    }
}
