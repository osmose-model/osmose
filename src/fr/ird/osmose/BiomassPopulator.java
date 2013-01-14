package fr.ird.osmose;

/**
 *
 * @author pverley
 */
public class BiomassPopulator extends Populator {
    
    @Override
    public void loadParameters() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void populate() {

        float correctingFactor;
        double abdIni;
        int numSerie = getOsmose().numSerie;
        float nbTimeStepsPerYear = getSimulation().getNbTimeStepsPerYear();

        for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
            //We calculate abd & biom ini of cohorts, and in parallel biom of species
            Species speci = getSimulation().getSpecies(i);
            double biomass = 0;
            double sumExp = 0;
            /*
             * phv 2011/11/24 For species that do not reproduce locally, initial
             * biomass is set to zero.
             */
            if (!speci.isReproduceLocally()) {
                for (int j = 0; j < speci.getLongevity(); j++) {
                    speci.tabAbdIni[j] = 0;
                    speci.tabBiomIni[j] = 0;
                }
            } else {
                abdIni = getOsmose().spBiomIniTab[numSerie][i] / (speci.tabMeanWeight[(int) Math.round(speci.getLongevity() / 2)] / 1000000);
                for (int j = speci.indexAgeClass0; j < speci.getLongevity(); j++) {
                    sumExp += Math.exp(-(j * (speci.D + speci.F + 0.5f) / (float) nbTimeStepsPerYear)); //0.5 = approximation of average natural mortality (by predation, senecence...)
                }

                speci.tabAbdIni[0] = (long) ((abdIni) / (Math.exp(-speci.larvalSurvival / (float) nbTimeStepsPerYear) * (1 + sumExp)));
                speci.tabBiomIni[0] = ((double) speci.tabAbdIni[0]) * speci.tabMeanWeight[0] / 1000000.;
                if (speci.indexAgeClass0 <= 0) {
                    biomass += speci.tabBiomIni[0];
                }
                speci.tabAbdIni[1] = Math.round(speci.tabAbdIni[0] * Math.exp(-speci.larvalSurvival / (float) nbTimeStepsPerYear));
                speci.tabBiomIni[1] = ((double) speci.tabAbdIni[1]) * speci.tabMeanWeight[1] / 1000000.;
                if (speci.indexAgeClass0 <= 1) {
                    biomass += speci.tabBiomIni[1];
                }
                for (int j = 2; j < speci.getLongevity(); j++) {
                    speci.tabAbdIni[j] = Math.round(speci.tabAbdIni[j - 1] * Math.exp(-(speci.D + 0.5f + speci.F) / (float) nbTimeStepsPerYear));
                    speci.tabBiomIni[j] = ((double) speci.tabAbdIni[j]) * speci.tabMeanWeight[j] / 1000000.;
                    if (speci.indexAgeClass0 <= j) {
                        biomass += speci.tabBiomIni[j];
                    }
                }

                correctingFactor = (float) (getOsmose().spBiomIniTab[numSerie][i] / biomass);
                // we make corrections on initial abundance to fit the input biomass
                speci.tabAbdIni[0] = (long) ((abdIni * correctingFactor) / (Math.exp(-speci.larvalSurvival / (float) nbTimeStepsPerYear) * (1 + sumExp)));
                speci.tabBiomIni[0] = ((double) speci.tabAbdIni[0]) * speci.tabMeanWeight[0] / 1000000.;
                speci.tabAbdIni[1] = Math.round(speci.tabAbdIni[0] * Math.exp(-speci.larvalSurvival / (float) nbTimeStepsPerYear));
                speci.tabBiomIni[1] = ((double) speci.tabAbdIni[1]) * speci.tabMeanWeight[1] / 1000000.;
                for (int j = 2; j < speci.getLongevity(); j++) {
                    speci.tabAbdIni[j] = Math.round(speci.tabAbdIni[j - 1] * Math.exp(-(speci.D + 0.5f + speci.F) / (float) nbTimeStepsPerYear));
                    speci.tabBiomIni[j] = ((double) speci.tabAbdIni[j]) * speci.tabMeanWeight[j] / 1000000.;
                }
            }

            // create the cohorts
            for (int age = 0; age < speci.getLongevity(); age++) {
                if (speci.tabAbdIni[age] > 0.d) {
                    int nbSchools = getOsmose().nbSchools[getOsmose().numSerie];
                    for (int k = 0; k < nbSchools; k++) {
                        getPopulation().add(new School(speci, speci.tabAbdIni[age] / nbSchools, speci.tabMeanLength[age], speci.tabMeanWeight[age], age));
                    }
                }
            }
        }
    }
}
