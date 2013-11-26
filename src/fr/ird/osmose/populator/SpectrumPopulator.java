/*
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, natural and
 * starvation mortalities, reproduction and migration) and fishing mortalities
 * (Shin and Cury 2001, 2004).
 * 
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */
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
    
    private double a, b, range;
    
    public SpectrumPopulator(int rank) {
        super(rank);
    }

    @Override
    public void init() {
        a = getConfiguration().getDouble("population.initialization.spectrum.slope");
        b = getConfiguration().getDouble("population.initialization.spectrum.intercept");
        range = getConfiguration().getDouble("population.initialization.spectrum.range");
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
                        int nbSchools = getConfiguration().getNSchool(i);
                        float length = speciesj.computeMeanLength(age);
                        float weight = speciesj.computeMeanWeight(age);
                        for (int k = 0; k < nbSchools; k++) {
                            getSchoolSet().add(new School(speciesj, abundanceIni[age] / nbSchools, length, weight, age));
                        }
                    }
                }
            }
        }
    }
}
