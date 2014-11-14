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
import fr.ird.osmose.process.FishingProcess;
import fr.ird.osmose.process.NaturalMortalityProcess;

/**
 *
 * @author pverley
 */
public class BiomassPopulator extends AbstractPopulator {

    final private int rank;
    private double[] iniBiomass;

    public BiomassPopulator(int rank) {
        super(rank);
        this.rank = rank;
    }

    @Override
    public void init() {
        iniBiomass = new double[getNSpecies()];
        for (int i = 0; i < getConfiguration().getNSpecies(); i++) {
            iniBiomass[i] = getConfiguration().getDouble("population.initialization.biomass.sp" + i);
        }
    }

    @Override
    public void populate() {

        double correctingFactor;
        double nStepYear = getConfiguration().getNStepYear();

        FishingProcess fishingProcess = new FishingProcess(rank);
        fishingProcess.init();
        NaturalMortalityProcess naturalMortalityProcess = new NaturalMortalityProcess(rank);
        naturalMortalityProcess.init();
        for (int i = 0; i < getConfiguration().getNSpecies(); i++) {
            //We calculate abd & biom ini of cohorts, and in parallel biom of species
            Species species = getSpecies(i);
            double biomass = 0;
            double sumExp = 0;
            double[] abundanceIni = new double[species.getLifespanDt()];
            double[] biomassIni = new double[species.getLifespanDt()];
            double[] meanWeight = new double[species.getLifespanDt()];
            for (int age = 0; age < meanWeight.length; age++) {
                meanWeight[age] = species.computeMeanWeight(age);
            }

            double larvalSurvival = naturalMortalityProcess.getEggAnnualRate(species);
            double D = naturalMortalityProcess.getAnnualRate(species);

            double F = fishingProcess.getAnnualRate(species);

            double iniAbundance = iniBiomass[i] / (meanWeight[species.getLifespanDt() / 2] / 1e6);
            for (int j = species.getAgeClassZero(); j < species.getLifespanDt(); j++) {
                sumExp += Math.exp(-(j * (D + F + 0.5d) / nStepYear)); //0.5 = approximation of average natural mortality (by predation, senecence...)
            }

            abundanceIni[0] = iniAbundance / (Math.exp(-larvalSurvival / nStepYear) * (1 + sumExp));
            biomassIni[0] = abundanceIni[0] * meanWeight[0] / 1e6;
            if (species.getAgeClassZero() <= 0) {
                biomass += biomassIni[0];
            }
            abundanceIni[1] = abundanceIni[0] * Math.exp(-larvalSurvival / nStepYear);
            biomassIni[1] = abundanceIni[1] * meanWeight[1] / 1e6;
            if (species.getAgeClassZero() <= 1) {
                biomass += biomassIni[1];
            }
            for (int j = 2; j < species.getLifespanDt(); j++) {
                abundanceIni[j] = Math.round(abundanceIni[j - 1] * Math.exp(-(D + 0.5d + F) / nStepYear));
                biomassIni[j] = abundanceIni[j] * meanWeight[j] / 1e6;
                if (species.getAgeClassZero() <= j) {
                    biomass += biomassIni[j];
                }
            }

            correctingFactor = iniBiomass[i] / biomass;
            // we make corrections on initial abundance to fit the input biomass
            abundanceIni[0] = (iniAbundance * correctingFactor) / (Math.exp(-larvalSurvival / nStepYear) * (1 + sumExp));
            biomassIni[0] = abundanceIni[0] * meanWeight[0] / 1e6;
            abundanceIni[1] = abundanceIni[0] * Math.exp(-larvalSurvival / nStepYear);
            biomassIni[1] = abundanceIni[1] * meanWeight[1] / 1000000.;
            for (int j = 2; j < species.getLifespanDt(); j++) {
                abundanceIni[j] = abundanceIni[j - 1] * Math.exp(-(D + 0.5d + F) / nStepYear);
                biomassIni[j] = abundanceIni[j] * meanWeight[j] / 1e6;
            }


            // create the cohorts
            for (int age = 0; age < species.getLifespanDt(); age++) {
                if (abundanceIni[age] > 0.d) {
                    float meanLength = species.computeMeanLength(age);
                    int nSchool = getConfiguration().getNSchool(i);
                    for (int k = 0; k < nSchool; k++) {
                        getSchoolSet().add(new School(species, abundanceIni[age] / nSchool, meanLength, (float) meanWeight[age], age));
                    }
                }
            }
        }
    }
}
