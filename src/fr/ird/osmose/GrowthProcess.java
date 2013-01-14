package fr.ird.osmose;

/**
 *
 * @author pverley
 */
public class GrowthProcess extends AbstractProcess {

    float[][] minDelta;
    float[][] maxDelta;
    float[][] deltaMeanLength;

    @Override
    public void loadParameters() {

        int nSpecies = getSimulation().getNbSpecies();
        minDelta = new float[nSpecies][];
        maxDelta = new float[nSpecies][];
        deltaMeanLength = new float[nSpecies][];

        for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
            Species species = getSimulation().getSpecies(i);
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
            school.predSuccessRate = getSimulation().computePredSuccessRate(school.biomassToPredate, school.preyedBiomass);
            Species species = school.getSpecies();
            int i = species.getIndex();
            int j = school.getAgeDt();
            if ((j == 0) || species.isOut(j, getSimulation().getIndexTimeYear())) {
                // Linear growth for eggs and migrating schools
                school.setLength(school.getLength() + deltaMeanLength[i][j]);
                school.setWeight((float) (species.c * Math.pow(school.getLength(), species.bPower)));
            } else {
                // Growth based on predation success
                school.growth(minDelta[i][j], maxDelta[i][j], species.c, species.bPower);
            }
        }
    }

    public void growth(School school, float minDelta, float maxDelta, float c, float bPower) {

        Species species = school.getSpecies();
        //calculation of lengths according to predation efficiency
        if (school.predSuccessRate >= species.criticalPredSuccess) {
            float length = (school.getLength() + minDelta + (maxDelta - minDelta) * ((school.predSuccessRate - species.criticalPredSuccess) / (1 - species.criticalPredSuccess)));
            school.setLength(length);
            school.setWeight((float) (c * Math.pow(school.getLength(), bPower)));
        }
    }
}
