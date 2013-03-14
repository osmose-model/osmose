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
public class MeanSizeCatchIndicator extends AbstractIndicator {

    private double[] meanSizeCatch;
    private double[] yieldN;

    public MeanSizeCatchIndicator(int indexSimulation, String keyEnabled) {
        super(indexSimulation, keyEnabled);
    }

    @Override
    public void initStep() {
        // Nothing to do
    }

    @Override
    public void reset() {
        meanSizeCatch = new double[getNSpecies()];
        yieldN = new double[getNSpecies()];
    }

    @Override
    public void update() {
        for (School school : getPopulation().getAliveSchools()) {
            if (!includeClassZero() && school.getAgeDt() < school.getSpecies().getAgeClassZero()) {
                continue;
            }
            int i = school.getSpeciesIndex();
            meanSizeCatch[i] += school.getNdeadFishing() * school.getLength();
            yieldN[i] += school.getNdeadFishing();
        }
    }

    @Override
    public void write(float time) {

        for (int i = 0; i < getConfiguration().getNSpecies(); i++) {
            if (yieldN[i] > 0) {
                meanSizeCatch[i] = meanSizeCatch[i] / yieldN[i];
            } else {
                meanSizeCatch[i] = Double.NaN;
            }
        }
        writeVariable(time, meanSizeCatch);
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder("SizeIndicators");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_meanSizeCatch_Simu");
        filename.append(getSimulation().getReplica());
        filename.append(".csv");
        return filename.toString();
    }

    @Override
    String getDescription() {
        StringBuilder str = new StringBuilder("Mean size of fish species in cm, weighted by fish numbers in the catches, and ");
        if (includeClassZero()) {
            str.append("including ");
        } else {
            str.append("excluding ");
        }
        str.append("first ages specified in input");
        return str.toString();
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
