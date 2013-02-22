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
            if (school.getAgeDt() >= school.getSpecies().indexAgeClass0) {
                int i = school.getSpeciesIndex();
                meanTLCatch[i] += school.trophicLevel * school.adb2biom(school.nDeadFishing);
                yield[i] += school.adb2biom(school.nDeadFishing);
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return getOsmose().isTLOutput();
    }

    @Override
    public void write(float time) {

        for (int i = 0; i < getOsmose().getNumberSpecies(); i++) {
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
        filename.append(getOsmose().outputPrefix);
        filename.append("_meanTLCatch_Simu");
        filename.append(getSimulation().getReplica());
        filename.append(".csv");
        return filename.toString();
    }

    @Override
    String getDescription() {
        return "Mean Trophic Level of fish species, weighted by fish catch";
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
