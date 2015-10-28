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
package fr.ird.osmose.process;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import fr.ird.osmose.util.timeseries.SingleTimeSeries;
import java.util.List;

/**
 * This class controls the reproduction process in the simulated domain. The
 * user defines the spawning season (a CSV file per Species) either annual or
 * interannual, the percentage of female in the population (sex ratio) and the
 * number of eggs per gramme of mature female (alpha) for every species. Osmose
 * estimates the spawning stock biomass (SSB) and calculates the number of eggs
 * to be released in the system at every time steps.<br />
 * During the spin-up of the simulation (duration of spin-up either set by the
 * user or set by default to the lifespan of the longest-lived species) Osmose
 * prevents species collapse by artificially setting the SSB to a predefined
 * level (user defined, this parameter could/should be calibrated) in order to 
 * guarantee egg release.
 */
public class ReproductionProcess extends AbstractProcess {

    /**
     * Distribution of the spawning throughout the year
     */
    private double[][] seasonSpawning;
    /*
     * Percentage of female in the population
     */
    private double[] sexRatio;
    /*
     * Number of eggs per gram of mature female
     */
    private double[] alpha;
    /*
     * Seeding biomass in tonne
     */
    private double[] seedingBiomass;
    /*
     * Year max for seeding collapsed species, in number of time steps 
     */
    private int yearMaxSeeding;

    public ReproductionProcess(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        int nSpecies = getNSpecies();
        sexRatio = new double[nSpecies];
        alpha = new double[nSpecies];
        seasonSpawning = new double[nSpecies][];
        for (int i = 0; i < nSpecies; i++) {
            if (!getConfiguration().isNull("reproduction.season.file.sp" + i)) {
                SingleTimeSeries ts = new SingleTimeSeries();
                ts.read(getConfiguration().getFile("reproduction.season.file.sp" + i));
                seasonSpawning[i] = ts.getValues();
            } else {
                // Even spawning season throughout the year
                seasonSpawning[i] = new double[]{1.d / getConfiguration().getNStepYear()};
            }
        }

        for (int i = 0; i < nSpecies; i++) {
            float sum = 0;
            for (double d : seasonSpawning[i]) {
                sum += d;
            }
            if (sum > 0) {
                sexRatio[i] = getConfiguration().getDouble("species.sexratio.sp" + i);
                alpha[i] = getConfiguration().getDouble("species.relativefecundity.sp" + i);
            }
        }

        // Seeding biomass
        seedingBiomass = new double[getNSpecies()];
        for (int i = 0; i < getNSpecies(); i++) {
            seedingBiomass[i] = getConfiguration().getDouble("population.seeding.biomass.sp" + i);
        }
        // Seeding duration (expressed in number of time steps)
        yearMaxSeeding = 0;
        if (!getConfiguration().isNull("population.seeding.year.max")) {
            yearMaxSeeding = getConfiguration().getInt("population.seeding.year.max") * getConfiguration().getNStepYear();
        } else {
            for (int i = 0; i < getNSpecies(); i++) {
                yearMaxSeeding = Math.max(yearMaxSeeding, getSpecies(i).getLifespanDt());
            }
            warning("Did not find parameter population.seeding.year.max. Osmose set it to " + ((float) yearMaxSeeding/getConfiguration().getNStepYear()) + " years, the lifespan of the longest-lived species.");
        }
    }

    @Override
    public void run() {
        for (int i = 0; i < getConfiguration().getNSpecies(); i++) {
            double nEgg = 0.d;
            Species species = getSpecies(i);
            List<School> schools = getSchoolSet().getSchools(species);
            if (sexRatio[i] > 0.d && alpha[i] > 0.d) {
                double SSB = 0;
                for (School school : schools) {
                    if (species.isSexuallyMature(school)) {
                        SSB += school.getInstantaneousBiomass();
                    }
                }
                // Seeding for collapsed species
                if (getSimulation().getIndexTimeSimu() < yearMaxSeeding && SSB == 0.) {
                    SSB = seedingBiomass[i];
                }
                double season = getSeason(getSimulation().getIndexTimeSimu(), species);
                nEgg = sexRatio[i] * alpha[i] * season * SSB * 1000000;
            }

            /*
             * Making cohorts going up to the upper age class
             */
            for (School school : schools) {
                school.incrementAge();
            }

            //UPDATE AGE CLASS 0
            int nSchool = getConfiguration().getNSchool(i);
            if (nEgg == 0.d) {
                // do nothing, zero school
            } else if (nEgg < nSchool) {
                School school0 = new School(species, nEgg);
                getSchoolSet().add(school0);
            } else if (nEgg >= nSchool) {
                for (int s = 0; s < nSchool; s++) {
                    School school0 = new School(species, nEgg / nSchool);
                    getSchoolSet().add(school0);
                }
            }
        }
    }

    private double getSeason(int iStepSimu, Species species) {

        int iSpec = species.getIndex();
        int length = seasonSpawning[iSpec].length;
        int iStep;
        if (length > getConfiguration().getNStepYear()) {
            iStep = iStepSimu;
        } else if (length == 1) {
            iStep = 0;
        } else {
            iStep = getSimulation().getIndexTimeYear();
        }
        return seasonSpawning[iSpec][iStep];
    }
}
