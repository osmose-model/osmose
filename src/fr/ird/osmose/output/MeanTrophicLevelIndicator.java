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
public class MeanTrophicLevelIndicator extends AbstractIndicator {

    private double[] meanTL;
    private double[] biomass;

    @Override
    public void initStep() {
        // Nothing to do
    }

    @Override
    public void reset() {
        meanTL = new double[getNSpecies()];
        biomass = new double[getNSpecies()];
    }

    @Override
    public void update() {
        for (School school : getPopulation().getAliveSchools()) {
            if (school.getAgeDt() >= school.getSpecies().indexAgeClass0) {
                int i = school.getSpeciesIndex();
                meanTL[i] += school.getBiomass() * school.getTrophicLevel();
                biomass[i] += school.getBiomass();
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
            if (biomass[i] > 0.d) {
                meanTL[i] = (float) (meanTL[i] / biomass[i]);
            } else {
                meanTL[i] = Double.NaN;
            }
        }
        writeVariable(time, meanTL);
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder("Trophic");
        filename.append(File.separatorChar);
        filename.append(getOsmose().outputPrefix);
        filename.append("_meanTL_Simu");
        filename.append(getSimulation().getReplica());
        filename.append(".csv");
        return filename.toString();
    }

    @Override
    String getDescription() {
        return "Mean Trophic Level of fish species, weighted by fish biomass, and including/excluding first ages specified in input (in calibration file)";
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
