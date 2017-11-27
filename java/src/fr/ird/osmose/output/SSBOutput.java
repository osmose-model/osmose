/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.output;

import fr.ird.osmose.School;

/**
 *
 * @author pverley
 */
public class SSBOutput extends AbstractOutput {

    private double[] ssb;

    public SSBOutput(int rank) {
        super(rank);
    }

    @Override
    public void initStep() {
        // Nothing to do
    }

    @Override
    public void reset() {
        ssb = new double[getNSpecies()];
    }

    @Override
    public void update() {
        for (School school : getSchoolSet().getAliveSchools()) {
            if (school.getLength() >= school.getSpecies().getSizeMaturity()) {
                ssb[school.getSpeciesIndex()] += school.getInstantaneousBiomass();
            }
        }
    }

    @Override
    public void write(float time) {

        double nsteps = getRecordFrequency();
        for (int i = 0; i < ssb.length; i++) {
            ssb[i] /= nsteps;
        }
        writeVariable(time, ssb);
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder(getConfiguration().getString("output.file.prefix"));
        filename.append("_SSB_Simu");
        filename.append(getRank());
        filename.append(".csv");
        return filename.toString();
    }

    @Override
    String getDescription() {
        return "Spawning Stock Biomass";
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
