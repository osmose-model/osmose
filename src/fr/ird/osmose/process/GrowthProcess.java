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

    public GrowthProcess(int indexSimulation) {
        super(indexSimulation);
    }

    @Override
    public void init() {

        int nSpecies = getConfiguration().getNSpecies();
        criticalPredSuccess = new float[nSpecies];
        minDelta = new float[nSpecies][];
        maxDelta = new float[nSpecies][];
        deltaMeanLength = new float[nSpecies][];

        for (int i = 0; i < nSpecies; i++) {
            criticalPredSuccess[i] = getConfiguration().getFloat("predation.efficiency.critical.sp" + i);
            Species species = getSpecies(i);
            int lifespan = species.getLifespanDt();
            minDelta[i] = new float[lifespan];
            maxDelta[i] = new float[lifespan];
            deltaMeanLength[i] = new float[lifespan];

            float meanAge1 = species.computeMeanLength(0);
            for (int age = 0; age < lifespan - 1; age++) {
                float meanAge0 = meanAge1;
                meanAge1 = species.computeMeanLength(age + 1);
                deltaMeanLength[i][age] = meanAge1 - meanAge0;

                minDelta[i][age] = deltaMeanLength[i][age] - deltaMeanLength[i][age];
                maxDelta[i][age] = deltaMeanLength[i][age] + deltaMeanLength[i][age];
            }
        }
    }

    @Override
    public void run() {
        for (School school : getPopulation().getAliveSchools()) {
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
