/* 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
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
import java.util.HashMap;
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
    private HashMap<Integer, double[]> seasonSpawning;
    /*
     * Percentage of female in the population
     */
    private HashMap<Integer, Double> sexRatio;
    /*
     * Number of eggs per gram of mature female
     */
    private HashMap<Integer, Double> alpha;
    /*
     * Seeding biomass in tonne
     */
    private HashMap<Integer, Double> seedingBiomass;
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
        sexRatio = new HashMap();
        alpha = new HashMap();
        seasonSpawning = new HashMap();
        for (int i : getConfiguration().getFocalIndex()) {
            if (!getConfiguration().isNull("reproduction.season.file.sp" + i)) {
                SingleTimeSeries ts = new SingleTimeSeries();
                ts.read(getConfiguration().getFile("reproduction.season.file.sp" + i));
                seasonSpawning.put(i, ts.getValues());
            } else {
                // Even spawning season throughout the year
                seasonSpawning.put(i, new double[]{1.d / getConfiguration().getNStepYear()});
            }
        }

        for (int i : getConfiguration().getFocalIndex()) {
            float sum = 0;
            for (double d : seasonSpawning.get(i)) {
                sum += d;
            }
            if (sum > 0) {
                sexRatio.put(i, getConfiguration().getDouble("species.sexratio.sp" + i));
                alpha.put(i, getConfiguration().getDouble("species.relativefecundity.sp" + i));
            }
        }

        // Seeding biomass
        seedingBiomass = new HashMap();
        for (int i : getConfiguration().getFocalIndex()) {
            seedingBiomass.put(i, getConfiguration().getDouble("population.seeding.biomass.sp" + i));
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

        if (getConfiguration().isBioenEnabled()) {
            error("ReproductionProcess run method not usable in Osmose-PHYSIO", new Exception());
        }

        // spawning stock biomass per species
        HashMap<Integer, Double> SSB = new HashMap();

        // check whether the species do reproduce or not
        HashMap<Integer, Boolean> reproduce = new HashMap();
        for (int i : getConfiguration().getFocalIndex()) {
            reproduce.put(i, (sexRatio.get(i) > 0.d && alpha.get(i) > 0.d));
        }

        // loop over all the schools to compute SSB
        for (School school : getSchoolSet().getSchools()) {
            int i = school.getSpeciesIndex();
            // increment spawning stock biomass
            if (reproduce.get(i) && school.getSpecies().isSexuallyMature(school)) {
                SSB.put(i, SSB.get(i) + school.getInstantaneousBiomass());
            }
            // increment age
            school.incrementAge();
        }

        // loop over the species to lay cohort at age class 0
        for (int i : getConfiguration().getFocalIndex()) {
            // ignore species that do not reproduce
            if (!reproduce.get(i)) {
                continue;
            }
            Species species = getSpecies(i);
            // seeding process for collapsed species
            if (getSimulation().getIndexTimeSimu() < yearMaxSeeding && SSB.get(i) == 0.) {
                SSB.put(i, seedingBiomass.get(i));
            }
            // compute nomber of eggs to be released
            double season = getSeason(getSimulation().getIndexTimeSimu(), species);
            double nEgg = sexRatio.get(i) * alpha.get(i) * season * SSB.get(i) * 1000000;
            // lay age class zero
            int nSchool = getConfiguration().getNSchool(i);
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
        }
    }

    protected double getSeason(int iStepSimu, Species species) {

        int iSpec = species.getIndex();
        int length = seasonSpawning.get(iSpec).length;
        int iStep;
        if (length > getConfiguration().getNStepYear()) {
            iStep = iStepSimu;
        } else if (length == 1) {
            iStep = 0;
        } else {
            iStep = getSimulation().getIndexTimeYear();
        }
        return seasonSpawning.get(iSpec)[iStep];
    }

    public double getSeedingBiomass(int i) {
        return this.seedingBiomass.get(i);
    }

    public double getSexRatio(int i) {
        return this.sexRatio.get(i);
    }

    public int getYearSeading() {
        return this.yearMaxSeeding;
    }

    public double getAlpha(int i) {
        return this.alpha.get(i);
    }

}
