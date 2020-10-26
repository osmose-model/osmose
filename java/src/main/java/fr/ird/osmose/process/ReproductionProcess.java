/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine Ecosystems)
 * http://www.osmose-model.org
 * 
 * Copyright (C) IRD (Institut de Recherche pour le Développement) 2009-2020
 * 
 * Osmose is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, additional and
 * starvation mortalities, reproduction and migration) and fishing mortalities
 * (Shin and Cury 2001, 2004).
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License). Full description
 * is provided on the LICENSE file.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * 
 */
package fr.ird.osmose.process;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import fr.ird.osmose.util.timeseries.SingleTimeSeries;

/**
 * This class controls the reproduction process in the simulated domain. The
 * user defines the spawning season (a CSV file per Species) either annual or
 * interannual, the percentage of female in the population (sex ratio) and the
 * number of eggs per gramme of mature female (beta) for every species. Osmose
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
    private double[] beta;
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
        beta = new double[nSpecies];
        seasonSpawning = new double[nSpecies][];
        int cpt = 0;
        for (int i : getConfiguration().getFocalIndex()) {
            if (!getConfiguration().isNull("reproduction.season.file.sp" + i)) {
                SingleTimeSeries ts = new SingleTimeSeries();
                ts.read(getConfiguration().getFile("reproduction.season.file.sp" + i));
                seasonSpawning[cpt] = ts.getValues();
            } else {
                // Even spawning season throughout the year
                seasonSpawning[cpt] = new double[]{1.d / getConfiguration().getNStepYear()};
            }
            cpt++;
        }

        cpt = 0;
        for (int i : getConfiguration().getFocalIndex()) {
            float sum = 0;
            for (double d : seasonSpawning[cpt]) {
                sum += d;
            }
            if (sum > 0) {
                sexRatio[cpt] = getConfiguration().getDouble("species.sexratio.sp" + i);
                beta[cpt] = getConfiguration().getDouble("species.relativefecundity.sp" + i);
            }
            cpt++;
        }

        // Seeding biomass
        seedingBiomass = new double[nSpecies];
        cpt = 0;
        for (int i : getConfiguration().getFocalIndex()) {
            seedingBiomass[cpt] = getConfiguration().getDouble("population.seeding.biomass.sp" + i);
        }
        // Seeding duration (expressed in number of time steps)
        yearMaxSeeding = 0;
        if (!getConfiguration().isNull("population.seeding.year.max")) {
            yearMaxSeeding = getConfiguration().getInt("population.seeding.year.max") * getConfiguration().getNStepYear();
        } else {
            for (int i : getConfiguration().getFocalIndex()) {
                yearMaxSeeding = Math.max(yearMaxSeeding, getSpecies(i).getLifespanDt());
            }
            warning("Did not find parameter population.seeding.year.max. Osmose set it to " + ((float) yearMaxSeeding / getConfiguration().getNStepYear()) + " years, the lifespan of the longest-lived species.");
        }
    }

    @Override
    public void run() {

        int cpt;

        if (getConfiguration().isBioenEnabled()) {
            error("ReproductionProcess run method not usable in Osmose-PHYSIO", new Exception());
        }

        int nSpecies = this.getNSpecies();

        // spawning stock biomass per species
        double[] SSB = new double[nSpecies];

        // check whether the species do reproduce or not
        boolean[] reproduce = new boolean[nSpecies];
        cpt = 0;
        for (int i : getConfiguration().getFocalIndex()) {
            reproduce[cpt] = (sexRatio[cpt] > 0.d && beta[cpt] > 0.d);
            cpt++;
        }

        // loop over all the schools to compute SSB
        for (School school : getSchoolSet().getSchools()) {
            int i = school.getGlobalSpeciesIndex();
            // increment spawning stock biomass
            if (reproduce[i] && school.getSpecies().isSexuallyMature(school)) {
                SSB[i] += school.getInstantaneousBiomass();
            }
            // increment age
            school.incrementAge();
        }

        // loop over the species to lay cohort at age class 0
        cpt = -1;
        for (int i : getConfiguration().getFocalIndex()) {
            
            cpt++;
            
            // ignore species that do not reproduce
            if (!reproduce[cpt]) {
                continue;
            }

            Species species = getSpecies(cpt);
            // seeding process for collapsed species
            if (getSimulation().getIndexTimeSimu() < yearMaxSeeding && SSB[cpt] == 0.) {
                SSB[cpt] = seedingBiomass[cpt];
            }
            // compute nomber of eggs to be released
            double season = getSeason(getSimulation().getIndexTimeSimu(), species);
            double nEgg = sexRatio[cpt] * beta[cpt] * season * SSB[cpt] * 1000000;
            // lay age class zero
            int nSchool = getConfiguration().getNSchool(cpt);
            // nschool increases with time to avoid flooding the simulation with too many schools since the beginning
            //nSchool = Math.min(getConfiguration().getNSchool(i), nSchool * (getSimulation().getIndexTimeSimu() + 1) / (getConfiguration().getNStepYear() * 10));
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
        }  // end of focal species loop
    }

    protected double getSeason(int iStepSimu, Species species) {

        int iSpec = species.getGlobalSpeciesIndex();
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

    public double getSeedingBiomass(int i) {
        return this.seedingBiomass[i];
    }

    public double getSexRatio(int i) {
        return this.sexRatio[i];
    }

    public int getYearSeading() {
        return this.yearMaxSeeding;
    }

    public double getBeta(int i) {
        return this.beta[i];
    }

}
