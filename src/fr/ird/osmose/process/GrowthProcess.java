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
    public void init() {

        int nSpecies = getOsmose().getNumberSpecies();
        criticalPredSuccess = getOsmose().criticalPredSuccessMatrix;
        minDelta = new float[nSpecies][];
        maxDelta = new float[nSpecies][];
        deltaMeanLength = new float[nSpecies][];

        for (int i = 0; i < getOsmose().getNumberSpecies(); i++) {
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
        for (School school : getPopulation().getPresentSchools()) {
            school.predSuccessRate = PredationProcess.computePredSuccessRate(school.biomassToPredate, school.preyedBiomass);
            Species species = school.getSpecies();
            int i = species.getIndex();
            int age = school.getAgeDt();
            if ((age == 0) || school.isUnlocated()) {
                // Linear growth for eggs and migrating schools
                school.incrementLength(deltaMeanLength[i][age]);
            } else {
                // Growth based on predation success
                growth(school, minDelta[i][age], maxDelta[i][age]);
            }
        }
    }

    public void growth(School school, float minDelta, float maxDelta) {

        int iSpec = school.getSpeciesIndex();
        //calculation of lengths according to predation efficiency
        if (school.predSuccessRate >= criticalPredSuccess[iSpec]) {
            float dlength = (minDelta + (maxDelta - minDelta) * ((school.predSuccessRate - criticalPredSuccess[iSpec]) / (1 - criticalPredSuccess[iSpec])));
            school.incrementLength(dlength);
        }
    }
}
