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
public class AbundanceIndicator extends SchoolBasedIndicator {

    private double[] abundanceTot;
    private double[] abundanceNoJuv;
    
    @Override
    public void init() {
        // Nothing to do
    }

    @Override
    public void reset() {
        abundanceNoJuv = new double[getNSpecies()];
        if (getOsmose().isIncludeClassZero() || getOsmose().isCalibrationOutput()) {
            abundanceTot = new double[getNSpecies()];
        }
    }

    @Override
    public void update(School school) {

        int i = school.getSpeciesIndex();
        if (getOsmose().isIncludeClassZero()) {
            abundanceTot[i] += school.getAbundance();
        }
        if (school.getAgeDt() >= school.getSpecies().indexAgeClass0) {
            abundanceNoJuv[i] += school.getAbundance();
        }

    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void write(float time) {
        StringBuilder filename;
        int nSpec = getSimulation().getNumberSpecies();

        double nsteps = getOsmose().savingDtMatrix;
        for (int i = 0; i < nSpec; i++) {
            if (getOsmose().isIncludeClassZero() || getOsmose().isCalibrationOutput()) {
                abundanceTot[i] /= nsteps;
            }
            abundanceNoJuv[i] /= nsteps;
        }

        filename = new StringBuilder(getOsmose().outputPrefix);
        filename.append("_abundance_Simu");
        filename.append(getSimulation().getReplica());
        filename.append(".csv");
        Indicators.writeVariable(time, abundanceNoJuv, filename.toString(), "Mean abundance (number of fish), excluding first ages specified in input (typically in calibration file)");

        if (getOsmose().isIncludeClassZero()) {
            filename = new StringBuilder(getOsmose().outputPrefix);
            filename.append("_abundance-total_Simu");
            filename.append(getSimulation().getReplica());
            filename.append(".csv");
            Indicators.writeVariable(time, abundanceTot, filename.toString(), "Mean abundance (number of fish), including first ages specified in input (typically in calibration file)");
        }
    }
}
