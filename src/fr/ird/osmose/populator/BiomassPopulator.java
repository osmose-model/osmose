package fr.ird.osmose.populator;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import fr.ird.osmose.process.FishingProcess;
import fr.ird.osmose.process.NaturalMortalityProcess;

/**
 *
 * @author pverley
 */
public class BiomassPopulator extends AbstractPopulator {

    final private int replica;

    public BiomassPopulator(int replica) {
        super(replica);
        this.replica = replica;
    }

    @Override
    public void loadParameters() {
    }

    @Override
    public void populate() {

        float correctingFactor;
        double abdIni;
        float nbTimeStepsPerYear = getConfiguration().getNumberTimeStepsPerYear();

        for (int i = 0; i < getConfiguration().getNSpecies(); i++) {
            //We calculate abd & biom ini of cohorts, and in parallel biom of species
            Species species = getSpecies(i);
            double biomass = 0;
            double sumExp = 0;
            long[] abundanceIni = new long[species.getLifespanDt()];
            double[] biomassIni = new double[species.getLifespanDt()];
            float[] meanLength = new float[species.getLifespanDt()];
            float[] meanWeight = new float[species.getLifespanDt()];
            for (int age = 0; age < meanLength.length; age++) {
                meanLength[age] = species.computeMeanLength(age);
                meanWeight[age] = species.computeMeanWeight(age);
            }


            NaturalMortalityProcess naturalMortalityProcess = new NaturalMortalityProcess(replica);
            naturalMortalityProcess.init();
            double larvalSurvival = naturalMortalityProcess.getLarvalMortalityRate(species);
            double D = naturalMortalityProcess.getNaturalMortalityRate(species);

            FishingProcess fishingProcess = new FishingProcess(replica);
            fishingProcess.init();
            double F = fishingProcess.getFishingMortalityRate(species);
            

            float targetBiomass = getConfiguration().getFloat("population.initialization.biomass.sp" + i);
            abdIni = targetBiomass / (meanWeight[(int) Math.round(species.getLifespanDt() / 2)] / 1000000);
            for (int j = species.getAgeClassZero(); j < species.getLifespanDt(); j++) {
                sumExp += Math.exp(-(j * (D + F + 0.5f) / (float) nbTimeStepsPerYear)); //0.5 = approximation of average natural mortality (by predation, senecence...)
            }

            abundanceIni[0] = (long) ((abdIni) / (Math.exp(-larvalSurvival / (float) nbTimeStepsPerYear) * (1 + sumExp)));
            biomassIni[0] = ((double) abundanceIni[0]) * meanWeight[0] / 1000000.;
            if (species.getAgeClassZero() <= 0) {
                biomass += biomassIni[0];
            }
            abundanceIni[1] = Math.round(abundanceIni[0] * Math.exp(-larvalSurvival / (float) nbTimeStepsPerYear));
            biomassIni[1] = ((double) abundanceIni[1]) * meanWeight[1] / 1000000.;
            if (species.getAgeClassZero() <= 1) {
                biomass += biomassIni[1];
            }
            for (int j = 2; j < species.getLifespanDt(); j++) {
                abundanceIni[j] = Math.round(abundanceIni[j - 1] * Math.exp(-(D + 0.5f + F) / (float) nbTimeStepsPerYear));
                biomassIni[j] = ((double) abundanceIni[j]) * meanWeight[j] / 1000000.;
                if (species.getAgeClassZero() <= j) {
                    biomass += biomassIni[j];
                }
            }

            correctingFactor = (float) (targetBiomass / biomass);
            // we make corrections on initial abundance to fit the input biomass
            abundanceIni[0] = (long) ((abdIni * correctingFactor) / (Math.exp(-larvalSurvival / (float) nbTimeStepsPerYear) * (1 + sumExp)));
            biomassIni[0] = ((double) abundanceIni[0]) * meanWeight[0] / 1000000.;
            abundanceIni[1] = Math.round(abundanceIni[0] * Math.exp(-larvalSurvival / (float) nbTimeStepsPerYear));
            biomassIni[1] = ((double) abundanceIni[1]) * meanWeight[1] / 1000000.;
            for (int j = 2; j < species.getLifespanDt(); j++) {
                abundanceIni[j] = Math.round(abundanceIni[j - 1] * Math.exp(-(D + 0.5f + F) / (float) nbTimeStepsPerYear));
                biomassIni[j] = ((double) abundanceIni[j]) * meanWeight[j] / 1000000.;
            }


            // create the cohorts
            for (int age = 0; age < species.getLifespanDt(); age++) {
                if (abundanceIni[age] > 0.d) {
                    int nSchool = getConfiguration().getSeed();
                    for (int k = 0; k < nSchool; k++) {
                        getPopulation().add(new School(species, abundanceIni[age] / nSchool, meanLength[age], meanWeight[age], age));
                    }
                }
            }
        }
    }
}
