package fr.ird.osmose;

/**
 *
 * @author pverley
 */
public class BiomassPopulator extends Populator {

    @Override
    public void loadParameters() {
    }

    @Override
    public void populate() {

        float correctingFactor;
        double abdIni;
        int numSerie = getOsmose().numSerie;
        float nbTimeStepsPerYear = getSimulation().getNbTimeStepsPerYear();

        for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
            //We calculate abd & biom ini of cohorts, and in parallel biom of species
            Species species = getSimulation().getSpecies(i);
            double biomass = 0;
            double sumExp = 0;
            long[] abundanceIni = new long[species.getLongevity()];
            double[] biomassIni = new double[species.getLongevity()];
            float[] meanLength = species.getMeanLength();
            float[] meanWeight = species.getMeanWeight(meanLength);
            /*
             * phv 2011/11/24 For species that do not reproduce locally, initial
             * biomass is set to zero.
             */
            if (!species.isReproduceLocally()) {
                for (int j = 0; j < species.getLongevity(); j++) {
                    abundanceIni[j] = 0;
                    biomassIni[j] = 0;
                }
            } else {
                double larvalSurvival = getLarvalMortalityRate(species);
                double F = FishingProcess.getFishingMortalityRate(species);
                
                abdIni = getOsmose().spBiomIniTab[numSerie][i] / (meanWeight[(int) Math.round(species.getLongevity() / 2)] / 1000000);
                for (int j = species.indexAgeClass0; j < species.getLongevity(); j++) {
                    sumExp += Math.exp(-(j * (species.D + F + 0.5f) / (float) nbTimeStepsPerYear)); //0.5 = approximation of average natural mortality (by predation, senecence...)
                }

                abundanceIni[0] = (long) ((abdIni) / (Math.exp(-larvalSurvival / (float) nbTimeStepsPerYear) * (1 + sumExp)));
                biomassIni[0] = ((double) abundanceIni[0]) * meanWeight[0] / 1000000.;
                if (species.indexAgeClass0 <= 0) {
                    biomass += biomassIni[0];
                }
                abundanceIni[1] = Math.round(abundanceIni[0] * Math.exp(-larvalSurvival / (float) nbTimeStepsPerYear));
                biomassIni[1] = ((double) abundanceIni[1]) * meanWeight[1] / 1000000.;
                if (species.indexAgeClass0 <= 1) {
                    biomass += biomassIni[1];
                }
                for (int j = 2; j < species.getLongevity(); j++) {
                    abundanceIni[j] = Math.round(abundanceIni[j - 1] * Math.exp(-(species.D + 0.5f + F) / (float) nbTimeStepsPerYear));
                    biomassIni[j] = ((double) abundanceIni[j]) * meanWeight[j] / 1000000.;
                    if (species.indexAgeClass0 <= j) {
                        biomass += biomassIni[j];
                    }
                }

                correctingFactor = (float) (getOsmose().spBiomIniTab[numSerie][i] / biomass);
                // we make corrections on initial abundance to fit the input biomass
                abundanceIni[0] = (long) ((abdIni * correctingFactor) / (Math.exp(-larvalSurvival / (float) nbTimeStepsPerYear) * (1 + sumExp)));
                biomassIni[0] = ((double) abundanceIni[0]) * meanWeight[0] / 1000000.;
                abundanceIni[1] = Math.round(abundanceIni[0] * Math.exp(-larvalSurvival / (float) nbTimeStepsPerYear));
                biomassIni[1] = ((double) abundanceIni[1]) * meanWeight[1] / 1000000.;
                for (int j = 2; j < species.getLongevity(); j++) {
                    abundanceIni[j] = Math.round(abundanceIni[j - 1] * Math.exp(-(species.D + 0.5f + F) / (float) nbTimeStepsPerYear));
                    biomassIni[j] = ((double) abundanceIni[j]) * meanWeight[j] / 1000000.;
                }
            }

            // create the cohorts
            for (int age = 0; age < species.getLongevity(); age++) {
                if (abundanceIni[age] > 0.d) {
                    int nbSchools = getOsmose().nbSchools[getOsmose().numSerie];
                    for (int k = 0; k < nbSchools; k++) {
                        getPopulation().add(new School(species, abundanceIni[age] / nbSchools, meanLength[age], age));
                    }
                }
            }
        }
    }

    /*
     * The annual mortality rate is calculated as the annual average of
     * the larval mortality rates over the years.
     */
    private double getLarvalMortalityRate(Species species) {

        double rate = 0.d;
        for (int iStep = 0; iStep < species.larvalMortalityRates.length; iStep++) {
            rate += species.larvalMortalityRates[iStep];
        }
        rate /= species.larvalMortalityRates.length;
        return rate;
    }
}
