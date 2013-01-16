package fr.ird.osmose.process;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;

/**
 *
 * @author pverley
 */
public class GrowthProcess extends AbstractProcess {

    private float[][] minDelta;
    private float[][] maxDelta;
    private float[][] deltaMeanLength;
    private float[] criticalPredSuccess;

    @Override
    public void loadParameters() {

        int nSpecies = getSimulation().getNbSpecies();
        criticalPredSuccess = getOsmose().criticalPredSuccessMatrix[getOsmose().numSerie];
        minDelta = new float[nSpecies][];
        maxDelta = new float[nSpecies][];
        deltaMeanLength = new float[nSpecies][];

        for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
            Species species = getSpecies(i);
            int longevity = species.getLongevity();
            minDelta[i] = new float[longevity];
            maxDelta[i] = new float[longevity];
            deltaMeanLength[i] = new float[longevity];

            float[] meanLength = species.getMeanLength();
            for (int age = 0; age < longevity - 1; age++) {
                deltaMeanLength[i][age] = meanLength[age + 1] - meanLength[age];

                minDelta[i][age] = deltaMeanLength[i][age] - deltaMeanLength[i][age];
                maxDelta[i][age] = deltaMeanLength[i][age] + deltaMeanLength[i][age];
            }
        }
    }

    @Override
    public void run() {
        for (School school : getSimulation().getPresentSchools()) {
            school.predSuccessRate = PredationProcess.computePredSuccessRate(school.biomassToPredate, school.preyedBiomass);
            Species species = school.getSpecies();
            int i = species.getIndex();
            int j = school.getAgeDt();
            if ((j == 0) || species.isOut(j, getSimulation().getIndexTimeYear())) {
                // Linear growth for eggs and migrating schools
                school.setLength(school.getLength() + deltaMeanLength[i][j]);
                school.setWeight(species.computeWeight(school.getLength()));
            } else {
                // Growth based on predation success
                growth(school, minDelta[i][j], maxDelta[i][j]);
            }
        }
    }

    public void growth(School school, float minDelta, float maxDelta) {

        int iSpec = school.getSpeciesIndex();
        //calculation of lengths according to predation efficiency
        if (school.predSuccessRate >= criticalPredSuccess[iSpec]) {
            float length = (school.getLength() + minDelta + (maxDelta - minDelta) * ((school.predSuccessRate - criticalPredSuccess[iSpec]) / (1 - criticalPredSuccess[iSpec])));
            school.setLength(length);
            school.setWeight(school.getSpecies().computeWeight(length));
        }
    }
}
