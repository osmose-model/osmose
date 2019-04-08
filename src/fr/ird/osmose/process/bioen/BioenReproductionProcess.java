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
package fr.ird.osmose.process.bioen;

import fr.ird.osmose.process.*;
import fr.ird.osmose.School;
import fr.ird.osmose.SchoolSet;
import fr.ird.osmose.Species;
import fr.ird.osmose.util.timeseries.SingleTimeSeries;

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
public class BioenReproductionProcess extends ReproductionProcess {
        
    public BioenReproductionProcess(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        super.init();
        
    }

    @Override
    public void run() {

        // spawning stock biomass per species
        // used only for collasping species.
        double[] SSB = new double[getConfiguration().getNSpecies()];

        // loop over all the schools to compute 
        for (School school : getSchoolSet().getSchools()) {

            int i = school.getSpeciesIndex();
            if (school.isMature()) {
                SSB[i] += school.getInstantaneousBiomass();
            }
            // increment age
            school.incrementAge();
        }

        // loop over the species to lay cohort at age class 0
        for (int i = 0; i < getConfiguration().getNSpecies(); i++) {

            Species species = getSpecies(i);

            // initialisation of the number of eggs to be released
            double nEgg = 0.d;
            // compute nomber of eggs to be released
            double season = getSeason(getSimulation().getIndexTimeSimu(), species);

            if (getSimulation().getIndexTimeSimu() < this.getYearSeading() && SSB[i] == 0.) {
                // seeding process for collapsed species
                // if seeding biomass is 0 (no mature indivials, release eggs in the
                // old fashioned way.
                SSB[i] = this.getSeedingBiomass(i);
                nEgg += this.getSexRatio(i) * this.getAlpha(i) * season * SSB[i] * 1000000;
                
            } else {
                // if the seeding biomass is not null, loop over sexually mature schools
                for (School school : getSchoolSet().getSchools(getSpecies(i))) {
                    if (!school.isMature()) {
                        continue;
                    }
                    
                    // recovers the weight of the gonad that is lost for reproduction.
                    // in this case, the gonad weight times the season variable.
                    float wEgg = school.getGonadWeight() * (float) season;

                    // the wEgg content is removed from the gonad
                    school.incrementGonadWeight(-wEgg);

                    // the number of eggs is equal to the total gonad weight that is gone
                    // divided by the egg weight.
                    
                    // barrier.n: change in conversion from tone to gram
                    nEgg += wEgg * this.getSexRatio(i) / species.getEggWeight() * 1000000 * school.getInstantaneousAbundance();

                }  // end of loop over the school that belong to species i            
            }  // end of SSB statement

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

}
