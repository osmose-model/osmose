package fr.ird.osmose.populator;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author pverley
 */
public class SpectrumPopulator extends AbstractPopulator {
    
    public SpectrumPopulator(int indexSimulation) {
        super(indexSimulation);
    }

    @Override
    public void loadParameters() {
    }

    /*
     * initialisation according to a spectrum [10cm], from 0 to 200cm
     */
    @Override
    public void populate() {

        int nbTimeStepsPerYear = getConfiguration().getNStepYear();

        long[] tempSpectrumAbd = new long[20];
        /*
         * tab of vectors of species belonging to [0-10[....[140-150[
         */
        List<Species>[] specInSizeClass10 = new ArrayList[20];    //20 classes size 0 a 200
        for (int i = 0; i < specInSizeClass10.length; i++) {
            specInSizeClass10[i] = new ArrayList(getConfiguration().getNSpecies());
        }

        double a = getConfiguration().getDouble("population.initialization.spectrum.slope");
        double b = getConfiguration().getDouble("population.initialization.spectrum.intercept");
        double range = getConfiguration().getDouble("population.initialization.spectrum.range");
        //Calculation of abd lacking in each size class
        //calculation apart for first size class because minSize=0.05 (and not 0)
        tempSpectrumAbd[0] = Math.round(Math.pow(5., a) * Math.exp(b));
        for (int i = 1; i < 20; i++) {
            tempSpectrumAbd[i] = Math.round(Math.pow((i * range) + 5., a) * Math.exp(b));
        }
        //tabSizes10[i]+5 is mean length of [tabSizes10[i],tabSizes10[i+1][
        //Sort the Lmax of each species in each size class
        for (int i = 0; i < getConfiguration().getNSpecies(); i++) {
            int index1 = tempSpectrumAbd.length - 1;
            Species species = getSpecies(i);
            float meanLength = species.computeMeanLength(species.getLifespanDt() - 1);
            while (meanLength < (index1 * range)) {
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
                long[] abundanceIni = new long[speciesj.getLifespanDt()];
                abundanceIni[speciesj.getLifespanDt() - 1] = Math.round(((double) tempSpectrumAbd[i]) / specInSizeClass10[i].size());
                //we consider that D0->1 = 10 for the first age class (month or year, whatever nbDt), D0-1year->2 = 1 and D=0.4 otherwise
                //we calculate abd & biom of coh, and in parallel abd & biom of species & we create cohorts

                for (int k = speciesj.getLifespanDt() - 2; k >= (2 * nbTimeStepsPerYear); k--) {
                    abundanceIni[k] = Math.round(abundanceIni[k + 1] * Math.exp((0.5 / (float) nbTimeStepsPerYear)));
                    }
                int kTemp;
                if (speciesj.getLifespanDt() <= nbTimeStepsPerYear) {
                    kTemp = speciesj.getLifespanDt() - 2;
                } else {
                    kTemp = (2 * nbTimeStepsPerYear) - 1;
                }

                for (int k = kTemp; k >= 1; k--) {
                    abundanceIni[k] = Math.round(abundanceIni[k + 1] * Math.exp((1. / (float) nbTimeStepsPerYear)));
                }

                abundanceIni[0] = Math.round(abundanceIni[1] * Math.exp(10.));

                // Add schools to population
                for (int age = 0; age < speciesj.getLifespanDt(); age++) {
                    if (abundanceIni[age] > 0.d) {
                        int nbSchools = getConfiguration().getSeed();
                        float length = speciesj.computeMeanLength(age);
                        float weight = speciesj.computeMeanWeight(age);
                        for (int k = 0; k < nbSchools; k++) {
                            getPopulation().add(new School(speciesj, abundanceIni[age] / nbSchools, length, weight, age));
                        }
                    }
                }
            }
        }
    }
}
