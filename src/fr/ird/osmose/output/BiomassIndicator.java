package fr.ird.osmose.output;

import fr.ird.osmose.School;

/**
 *
 * @author pverley
 */
public class BiomassIndicator extends AbstractIndicator {

    private static double[] biomassTot;
    private static double[] biomassNoJuv;
    
    @Override
    public void reset() {
        biomassNoJuv = new double[getNSpecies()];
        if (getOsmose().isIncludeClassZero() || getOsmose().isCalibrationOutput()) {
            biomassTot = new double[getNSpecies()];
        }
    }

    @Override
    public void update(School school) {
        int i = school.getSpeciesIndex();
        if (getOsmose().isIncludeClassZero() || getOsmose().isCalibrationOutput()) {
            biomassTot[i] += school.getBiomass();
        }
        if (school.getAgeDt() >= school.getSpecies().indexAgeClass0) {
            biomassNoJuv[i] += school.getBiomass();
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
    
    @Override
    public void write(float time) {
        StringBuilder filename;
        int nSpec = getSimulation().getNumberSpecies();

        double nsteps = getOsmose().savingDtMatrix[getOsmose().numSerie];
        int year = getSimulation().getYear();
        int indexSaving = (int) (getSimulation().getIndexTimeYear() / nsteps);
        for (int i = 0; i < nSpec; i++) {
            if (getOsmose().isIncludeClassZero() || getOsmose().isCalibrationOutput()) {
                biomassTot[i] /= nsteps;
            }
            biomassNoJuv[i] /= nsteps;
        }

//        if (getOsmose().isCalibrationOutput()) {
//            for (int i = 0; i < nSpec; i++) {
//                getOsmose().BIOMQuadri[getOsmose().numSimu][i][0][year - getOsmose().timeSeriesStart][indexSaving] = (float) biomassNoJuv[i];
//                getOsmose().BIOMQuadri[getOsmose().numSimu][i][1][year - getOsmose().timeSeriesStart][indexSaving] = (float) biomassTot[i];
//            }
//            for (int i = nSpec; i < nSpec + getSimulation().getForcing().getNbPlanktonGroups(); i++) {
//                getOsmose().BIOMQuadri[getOsmose().numSimu][i][0][year - getOsmose().timeSeriesStart][indexSaving] = (float) (biomPerStage[i][0] / nsteps);
//            }
//        }

        filename = new StringBuilder(getOsmose().outputPrefix[getOsmose().numSerie]);
        filename.append("_biomass_Simu");
        filename.append(getOsmose().numSimu);
        filename.append(".csv");
        Indicators.writeVariable(time, biomassNoJuv, filename.toString(), "Mean biomass (tons), excluding first ages specified in input (typically in calibration file)");

        if (getOsmose().isIncludeClassZero()) {

            filename = new StringBuilder(getOsmose().outputPrefix[getOsmose().numSerie]);
            filename.append("_biomass-total_Simu");
            filename.append(getOsmose().numSimu);
            filename.append(".csv");
            Indicators.writeVariable(time, biomassTot, filename.toString(), "Mean biomass (tons), including first ages specified in input (typically in calibration file)");
        }
    }
}
