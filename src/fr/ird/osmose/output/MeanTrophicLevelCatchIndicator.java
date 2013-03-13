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
public class MeanTrophicLevelCatchIndicator extends AbstractIndicator {

    private double[] meanTLCatch;
    private double[] yield;

    public MeanTrophicLevelCatchIndicator(int replica, String keyEnabled) {
        super(replica, keyEnabled);
    }

    @Override
    public void initStep() {
        // Nothing to do
    }

    @Override
    public void reset() {
        meanTLCatch = new double[getNSpecies()];
        yield = new double[getNSpecies()];
    }

    @Override
    public void update() {
        for (School school : getPopulation().getAliveSchools()) {
            if (!includeClassZero() && school.getAgeDt() < school.getSpecies().getAgeClassZero()) {
                continue;
            }
            int i = school.getSpeciesIndex();
            meanTLCatch[i] += school.getTrophicLevel() * school.adb2biom(school.getNdeadFishing());
            yield[i] += school.adb2biom(school.getNdeadFishing());
        }
    }

    @Override
    public void write(float time) {

        for (int i = 0; i < getConfiguration().getNSpecies(); i++) {
            if (yield[i] > 0) {
                meanTLCatch[i] = meanTLCatch[i] / yield[i];
            } else {
                meanTLCatch[i] = Double.NaN;
            }
        }
        writeVariable(time, meanTLCatch);
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder("Trophic");
        filename.append(File.separatorChar);
        filename.append(getConfiguration().getString("output.file.prefix"));
        filename.append("_meanTLCatch_Simu");
        filename.append(getSimulation().getReplica());
        filename.append(".csv");
        return filename.toString();
    }

    @Override
    String getDescription() {
        StringBuilder str = new StringBuilder("Mean Trophic Level of fish species, weighted by fish catch, and ");
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
