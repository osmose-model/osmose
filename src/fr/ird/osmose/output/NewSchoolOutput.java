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
public class NewSchoolOutput extends AbstractOutput {

    private double[] biomass;
    private double[] egg;

    public NewSchoolOutput(int rank) {
        super(rank);
    }

    @Override
    public void initStep() {
        for (School school : getSchoolSet().getAliveSchools()) {
            if (school.getAgeDt() == 0) {
                egg[school.getSpeciesIndex()] += school.getBiomass();
            }
            biomass[school.getSpeciesIndex()] += school.getBiomass();
        }
    }

    @Override
    public void reset() {
        biomass = new double[getNSpecies()];
        egg = new double[getNSpecies()];
    }

    @Override
    public void update() {
        // nothing to do
    }

    @Override
    public void write(float time) {

        double nsteps = getRecordFrequency();
        for (int i = 0; i < biomass.length; i++) {
            egg[i] = 100 * (egg[i] / biomass[i]) / nsteps;
        }
        writeVariable(time, egg);
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder(getConfiguration().getString("output.file.prefix"));
        filename.append("_egg_Simu");
        filename.append(getRank());
        filename.append(".csv");
        return filename.toString();
    }

    @Override
    String getDescription() {
        return "Percentage of egg biomass for each species";
    }

    @Override
    String[] getHeaders() {
        String[] species = new String[getNSpecies()];
        for (int i = 0; i < species.length; i++) {
            species[i] = getSpecies(i).getName();
        }
        return species;
    }
}
