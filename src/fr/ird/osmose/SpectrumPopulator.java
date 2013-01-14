package fr.ird.osmose;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author pverley
 */
public class SpectrumPopulator extends Populator {

    @Override
    public void loadParameters() {
    }

    /*
     * initialisation according to a spectrum [10cm], from 0 to 200cm
     */
    @Override
    public void populate() {

        int numSerie = getOsmose().numSerie;
        int nbTimeStepsPerYear = getSimulation().getNbTimeStepsPerYear();

        long[] tempSpectrumAbd = new long[20];
        /*
         * tab of vectors of species belonging to [0-10[....[140-150[
         */
        List<Species>[] specInSizeClass10 = new ArrayList[20];    //20 classes size 0 a 200
        for (int i = 0; i < specInSizeClass10.length; i++) {
            specInSizeClass10[i] = new ArrayList(getSimulation().getNbSpecies());
        }

        double a = getOsmose().SSslope[numSerie];
        double b = getOsmose().SSintercept[numSerie];
        //Calculation of abd lacking in each size class
        //calculation apart for first size class because minSize=0.05 (and not 0)
        tempSpectrumAbd[0] = Math.round(Math.pow(5., a) * Math.exp(b));
        for (int i = 1; i < 20; i++) {
            tempSpectrumAbd[i] = Math.round(Math.pow((i * getOsmose().classRange) + 5., a) * Math.exp(b));
        }
        //tabSizes10[i]+5 is mean length of [tabSizes10[i],tabSizes10[i+1][
        //Sort the Lmax of each species in each size class
        for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
            int index1 = tempSpectrumAbd.length - 1;
            Species species = getSimulation().getSpecies(i);
            while (species.tabMeanLength[species.getLongevity() - 1] < (index1 * getOsmose().classRange)) {
                index1--;
            }
            specInSizeClass10[index1].add(species);
        }
        //calculate spectrumMaxIndex
        int spectrumMaxIndex = specInSizeClass10.length - 1;
        while (specInSizeClass10[spectrumMaxIndex].isEmpty()) {
            spectrumMaxIndex--;
        }

        //Calculate abd species and cohorts
        for (int i = spectrumMaxIndex; i >= 0; i--) {
            for (int j = 0; j < specInSizeClass10[i].size(); j++) {
                Species speciesj = ((Species) specInSizeClass10[i].get(j));
                speciesj.tabAbdIni[speciesj.getLongevity() - 1] = Math.round(((double) tempSpectrumAbd[i])
                        / specInSizeClass10[i].size());
                speciesj.tabBiomIni[speciesj.getLongevity() - 1] = ((double) speciesj.tabAbdIni[speciesj.getLongevity() - 1]) * speciesj.tabMeanWeight[speciesj.getLongevity() - 1] / 1000000.;
                //we consider that D0->1 = 10 for the first age class (month or year, whatever nbDt), D0-1year->2 = 1 and D=0.4 otherwise
                //we calculate abd & biom of coh, and in parallel abd & biom of species & we create cohorts

                for (int k = speciesj.getLongevity() - 2; k >= (2 * nbTimeStepsPerYear); k--) {
                    speciesj.tabAbdIni[k] = Math.round(speciesj.tabAbdIni[k + 1] * Math.exp((0.5 / (float) nbTimeStepsPerYear)));
                    speciesj.tabBiomIni[k] = ((double) speciesj.tabAbdIni[k]) * speciesj.tabMeanWeight[k] / 1000000.;
                }
                int kTemp;
                if (speciesj.getLongevity() <= nbTimeStepsPerYear) {
                    kTemp = speciesj.getLongevity() - 2;
                } else {
                    kTemp = (2 * nbTimeStepsPerYear) - 1;
                }

                for (int k = kTemp; k >= 1; k--) {
                    speciesj.tabAbdIni[k] = Math.round(speciesj.tabAbdIni[k + 1] * Math.exp((1. / (float) nbTimeStepsPerYear)));
                    speciesj.tabBiomIni[k] = ((double) speciesj.tabAbdIni[k]) * speciesj.tabMeanWeight[k] / 1000000.;
                }

                speciesj.tabAbdIni[0] = Math.round(speciesj.tabAbdIni[1] * Math.exp(10.));
                speciesj.tabBiomIni[0] = ((double) speciesj.tabAbdIni[0]) * speciesj.tabMeanWeight[0] / 1000000.;

                // Add schools to population
                for (int age = 0; age < speciesj.getLongevity(); age++) {
                    if (speciesj.tabAbdIni[age] > 0.d) {
                        int nbSchools = getOsmose().nbSchools[getOsmose().numSerie];
                        for (int k = 0; k < nbSchools; k++) {
                            getPopulation().add(new School(speciesj, speciesj.tabAbdIni[age] / nbSchools, speciesj.tabMeanLength[age], speciesj.tabMeanWeight[age], age));
                        }
                    }
                }
            }
        }
    }
}
