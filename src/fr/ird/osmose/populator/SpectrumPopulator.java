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
    
    public SpectrumPopulator(int replica) {
        super(replica);
    }

    @Override
    public void loadParameters() {
    }

    /*
     * initialisation according to a spectrum [10cm], from 0 to 200cm
     */
    @Override
    public void populate() {

        int nbTimeStepsPerYear = getConfiguration().getNumberTimeStepsPerYear();

        long[] tempSpectrumAbd = new long[20];
        /*
         * tab of vectors of species belonging to [0-10[....[140-150[
         */
        List<Species>[] specInSizeClass10 = new ArrayList[20];    //20 classes size 0 a 200
        for (int i = 0; i < specInSizeClass10.length; i++) {
            specInSizeClass10[i] = new ArrayList(getConfiguration().getNumberSpecies());
        }

        double a = getConfiguration().SSslope;
        double b = getConfiguration().SSintercept;
        //Calculation of abd lacking in each size class
        //calculation apart for first size class because minSize=0.05 (and not 0)
        tempSpectrumAbd[0] = Math.round(Math.pow(5., a) * Math.exp(b));
        for (int i = 1; i < 20; i++) {
            tempSpectrumAbd[i] = Math.round(Math.pow((i * getConfiguration().classRange) + 5., a) * Math.exp(b));
        }
        //tabSizes10[i]+5 is mean length of [tabSizes10[i],tabSizes10[i+1][
        //Sort the Lmax of each species in each size class
        for (int i = 0; i < getConfiguration().getNumberSpecies(); i++) {
            int index1 = tempSpectrumAbd.length - 1;
            Species species = getSpecies(i);
            float[] meanLength = species.getMeanLength();
            while (meanLength[species.getLongevity() - 1] < (index1 * getConfiguration().classRange)) {
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
                float[] meanLength = speciesj.getMeanLength();
                float[] meanWeight = speciesj.getMeanWeight(meanLength);
                long[] abundanceIni = new long[speciesj.getLongevity()];
                abundanceIni[speciesj.getLongevity() - 1] = Math.round(((double) tempSpectrumAbd[i]) / specInSizeClass10[i].size());
                //we consider that D0->1 = 10 for the first age class (month or year, whatever nbDt), D0-1year->2 = 1 and D=0.4 otherwise
                //we calculate abd & biom of coh, and in parallel abd & biom of species & we create cohorts

                for (int k = speciesj.getLongevity() - 2; k >= (2 * nbTimeStepsPerYear); k--) {
                    abundanceIni[k] = Math.round(abundanceIni[k + 1] * Math.exp((0.5 / (float) nbTimeStepsPerYear)));
                    }
                int kTemp;
                if (speciesj.getLongevity() <= nbTimeStepsPerYear) {
                    kTemp = speciesj.getLongevity() - 2;
                } else {
                    kTemp = (2 * nbTimeStepsPerYear) - 1;
                }

                for (int k = kTemp; k >= 1; k--) {
                    abundanceIni[k] = Math.round(abundanceIni[k + 1] * Math.exp((1. / (float) nbTimeStepsPerYear)));
                }

                abundanceIni[0] = Math.round(abundanceIni[1] * Math.exp(10.));

                // Add schools to population
                for (int age = 0; age < speciesj.getLongevity(); age++) {
                    if (abundanceIni[age] > 0.d) {
                        int nbSchools = getConfiguration().nbSchools;
                        for (int k = 0; k < nbSchools; k++) {
                            getPopulation().add(new School(speciesj, abundanceIni[age] / nbSchools, meanLength[age], meanWeight[age], age));
                        }
                    }
                }
            }
        }
    }
}
