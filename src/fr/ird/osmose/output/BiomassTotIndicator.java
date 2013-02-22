package fr.ird.osmose.output;

import fr.ird.osmose.School;

/**
 *
 * @author pverley
 */
public class BiomassTotIndicator extends AbstractIndicator {

    private double[] biomass;

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
            biomass[school.getSpeciesIndex()] += school.getBiomass();
        }
    }

    @Override
    public boolean isEnabled() {
        return !getOsmose().isCalibrationOutput() && getOsmose().isIncludeClassZero();
    }

    @Override
    public void write(float time) {

        double nsteps = getOsmose().savingDtMatrix;
        for (int i = 0; i < biomass.length; i++) {
            biomass[i] /= nsteps;
        }
        writeVariable(time, biomass);
    }

    @Override
    String getFilename() {
        StringBuilder filename = new StringBuilder(getOsmose().outputPrefix);
        filename.append("_biomass-total_Simu");
        filename.append(getSimulation().getReplica());
        filename.append(".csv");
        return filename.toString();
    }

    @Override
    String getDescription() {
        return "Mean biomass (tons), including first ages specified in input (typically in calibration file)";
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
