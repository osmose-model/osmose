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
public class NDeadSchoolOutput extends AbstractOutput {

    private double[] nDeadSchool;

    public NDeadSchoolOutput(int rank) {
        super(rank);
    }

    @Override
    public void initStep() {
        // Nothing to do
    }

    @Override
    public void reset() {
        nDeadSchool = new double[getNSpecies()];
    }

    @Override
    public void update() {

        for (School school : getSchoolSet()) {
            if (!school.isAlive()) {
                nDeadSchool[school.getSpeciesIndex()] += 1;
            }
        }
    }

    @Override
    public void write(float time) {
        writeVariable(time, nDeadSchool);
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder(getConfiguration().getString("output.file.prefix"));
        filename.append("_ndeadschool_Simu");
        filename.append(getRank());
        filename.append(".csv");
        return filename.toString();
    }

    @Override
    String getDescription() {
        return "Ratio of the number of dead schools over the total number of schools per species per time step of saving.";
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
