/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.output;

import fr.ird.osmose.School;

/**
 *
 * @author pverley
 */
public class AbundanceIndicator extends AbstractIndicator {

    private double[] abundance;

    public AbundanceIndicator(int replica) {
        super(replica);
    }

    @Override
    public void initStep() {
        // Nothing to do
    }

    @Override
    public void reset() {
        abundance = new double[getNSpecies()];
    }

    @Override
    public void update() {

        for (School school : getPopulation().getAliveSchools()) {
            if (!includeClassZero() && school.getAgeDt() < school.getSpecies().getAgeClassZero()) {
                continue;
            }
            abundance[school.getSpeciesIndex()] += school.getInstantaneousAbundance();
        }
    }

    @Override
    public boolean isEnabled() {
        return !getConfiguration().isCalibrationOutput();
    }

    @Override
    public void write(float time) {

        double nsteps = getConfiguration().getRecordFrequency();
        for (int i = 0; i < abundance.length; i++) {
            abundance[i] /= nsteps;
        }
        writeVariable(time, abundance);
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder(getConfiguration().getOutputPrefix());
        filename.append("_abundance_Simu");
        filename.append(getSimulation().getReplica());
        filename.append(".csv");
        return filename.toString();
    }

    @Override
    String getDescription() {
        StringBuilder str = new StringBuilder("Mean abundance (number of fish), ");
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
