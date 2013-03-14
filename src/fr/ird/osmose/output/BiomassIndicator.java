package fr.ird.osmose.output;

import fr.ird.osmose.School;

/**
 *
 * @author pverley
 */
public class BiomassIndicator extends AbstractIndicator {

    private double[] biomass;

    public BiomassIndicator(int indexSimulation, String keyEnabled) {
        super(indexSimulation, keyEnabled);
    }

    @Override
    public void initStep() {
        // Nothing to do
    }

    @Override
    public void reset() {
        biomass = new double[getNSpecies()];
    }

    @Override
    public void update() {
        for (School school : getPopulation().getAliveSchools()) {
            if (!includeClassZero() && school.getAgeDt() < school.getSpecies().getAgeClassZero()) {
                continue;
            }
            biomass[school.getSpeciesIndex()] += school.getInstantaneousBiomass();
        }
    }

    @Override
    public void write(float time) {

        double nsteps = getRecordFrequency();
        for (int i = 0; i < biomass.length; i++) {
            biomass[i] /= nsteps;
        }
        writeVariable(time, biomass);
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder(getConfiguration().getString("output.file.prefix"));
        filename.append("_biomass_Simu");
        filename.append(getSimulation().getReplica());
        filename.append(".csv");
        return filename.toString();
    }

    @Override
    String getDescription() {
        StringBuilder str = new StringBuilder("Mean biomass (tons), ");
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
