package fr.ird.osmose.output;

import fr.ird.osmose.School;

/**
 *
 * @author pverley
 */
public class BiomassCalibrationIndicator extends SchoolBasedIndicator {

    private double[] biomassTot;
    private double[] biomassNoJuv;

    @Override
    public void init() {
        // Nothing to do
    }

    @Override
    public void reset() {
        biomassNoJuv = new double[getNSpecies()];
        biomassTot = new double[getNSpecies()];
    }

    @Override
    public void update(School school) {
        int i = school.getSpeciesIndex();
        biomassTot[i] += school.getBiomass();
        if (school.getAgeDt() >= school.getSpecies().indexAgeClass0) {
            biomassNoJuv[i] += school.getBiomass();
        }
    }

    @Override
    public boolean isEnabled() {
        return getOsmose().isCalibrationOutput();
    }

    @Override
    public void write(float time) {
        
        int nSpec = getSimulation().getNumberSpecies();
        double nsteps = getOsmose().savingDtMatrix;
        int year = getSimulation().getYear();
        int indexSaving = (int) (getSimulation().getIndexTimeYear() / nsteps);
        for (int i = 0; i < nSpec; i++) {
            biomassTot[i] /= nsteps;
            biomassNoJuv[i] /= nsteps;
        }
        for (int i = 0; i < nSpec; i++) {
            getOsmose().BIOMQuadri[getOsmose().numSimu][i][0][year - getOsmose().timeSeriesStart][indexSaving] = (float) biomassNoJuv[i];
            getOsmose().BIOMQuadri[getOsmose().numSimu][i][1][year - getOsmose().timeSeriesStart][indexSaving] = (float) biomassTot[i];
        }
    }
}
