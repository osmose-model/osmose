package fr.ird.osmose.output;

import fr.ird.osmose.School;

/**
 *
 * @author pverley
 */
public class BiomassIndicator extends AbstractIndicator {

    private double[] biomassTot;
    private double[] biomassNoJuv;

    @Override
    public void init() {
        // Nothing to do
    }

    @Override
    public void reset() {
        biomassNoJuv = new double[getNSpecies()];
        if (getOsmose().isIncludeClassZero()) {
            biomassTot = new double[getNSpecies()];
        }
    }

    @Override
    public void update() {
        for (School school : getPopulation().getAliveSchools()) {
            int i = school.getSpeciesIndex();
            if (getOsmose().isIncludeClassZero()) {
                biomassTot[i] += school.getBiomass();
            }
            if (school.getAgeDt() >= school.getSpecies().indexAgeClass0) {
                biomassNoJuv[i] += school.getBiomass();
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return !getOsmose().isCalibrationOutput();
    }

    @Override
    public void write(float time) {
        StringBuilder filename;
        int nSpec = getSimulation().getNumberSpecies();

        double nsteps = getOsmose().savingDtMatrix;
        for (int i = 0; i < nSpec; i++) {
            if (getOsmose().isIncludeClassZero() || getOsmose().isCalibrationOutput()) {
                biomassTot[i] /= nsteps;
            }
            biomassNoJuv[i] /= nsteps;
        }

        filename = new StringBuilder(getOsmose().outputPrefix);
        filename.append("_biomass_Simu");
        filename.append(getSimulation().getReplica());
        filename.append(".csv");
        Indicators.writeVariable(time, biomassNoJuv, filename.toString(), "Mean biomass (tons), excluding first ages specified in input (typically in calibration file)");

        if (getOsmose().isIncludeClassZero()) {
            filename = new StringBuilder(getOsmose().outputPrefix);
            filename.append("_biomass-total_Simu");
            filename.append(getSimulation().getReplica());
            filename.append(".csv");
            Indicators.writeVariable(time, biomassTot, filename.toString(), "Mean biomass (tons), including first ages specified in input (typically in calibration file)");
        }
    }
}
