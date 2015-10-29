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
package fr.ird.osmose.process.mortality;

import fr.ird.osmose.process.AbstractProcess;
import fr.ird.osmose.School;
import fr.ird.osmose.PreyRecord;
import java.util.Arrays;
import java.util.Collection;

/**
 *
 * @author pverley
 */
public class MortalityProcess extends AbstractProcess {

    /*
     * Implementation of the mortality algorithm (either iterative or
     * stochastic) for schools inside the simulated domain
     */
    private AbstractProcess insideMortalityProcess;
    /*
     * Mortality process for particles outside the simulated domain
     */
    private OutMortality outsideMortalityProcess;
    /*
     * Private instance of the starvation process
     */
    private StarvationMortality starvationMortality;

    /**
     * Types of mortality algorithm
     */
    private enum MortalityAlgorithm {

        /**
         * Mortality rates are obtained through an iterative process.
         * <ul>
         * <li>It is assumed that every cause is independant and
         * concomitant.</li>
         * <li>No stochasticity neither competition within predation process:
         * every predator sees preys as they are at the begining of the
         * time-step.</li>
         * <li>Synchromous updating of school biomass.</li>
         * </ul>
         */
        ITERATIVE,
        /**
         * Mortality processes compete stochastically.
         * <ul>
         * <li>It is assumed that every cause compete with each other.</li>
         * <li>Stochasticity and competition within predation process.</li>
         * <li>Asynchronous updating of school biomass (it means biomass are
         * updated on the fly).</li>
         * </ul>
         */
        STOCHASTIC;
    }
    
    public MortalityProcess(int rank) {
        super(rank);
    }
    
    @Override
    public void init() {

        // Chooses the mortality algorithm for schools inside the simulated domain
        MortalityAlgorithm mortalityAlgorithm = MortalityAlgorithm.STOCHASTIC;
        if (!getConfiguration().isNull("mortality.algorithm")) {
            try {
                mortalityAlgorithm = MortalityAlgorithm.valueOf(getConfiguration().getString("mortality.algorithm").toUpperCase());
            } catch (IllegalArgumentException ex) {
                StringBuilder msg = new StringBuilder();
                msg.append("Unrecognised value for parameter mortality.algorithm = ");
                msg.append(getConfiguration().getString("mortality.algorithm"));
                msg.append(". It should be either ");
                msg.append(Arrays.toString(MortalityAlgorithm.values()));
                warning(msg.toString());
            }
        }
        info("Mortality algorithm set to " + mortalityAlgorithm);
        
        switch (mortalityAlgorithm) {
            case ITERATIVE:
                insideMortalityProcess = new IterativeMortalityProcess(getRank());
                break;
            case STOCHASTIC:
                insideMortalityProcess = new StochasticMortalityProcess(getRank());
                break;
        }
        // Initialises the mortality algorithm
        insideMortalityProcess.init();

        // Mortality that occurs outside the simulated domain is handled separatly
        outsideMortalityProcess = new OutMortality(getRank());
        outsideMortalityProcess.init();

        // The starvation process is needed to update the starvation mortality
        // rate at the end of the mortality algorithm
        starvationMortality = new StarvationMortality(getRank());
        starvationMortality.init();
    }
    
    @Override
    public void run() {

        // Run mortality algorithm (iterative or stochastic)
        // Only applies to schools that are inside the simulated domain
        // at current time step
        insideMortalityProcess.run();

        // Update starvation mortality rate and trophic level
        for (School school : getSchoolSet().getSchools()) {
            // Calculate starvation mortality rate that will be apply at next time step
            school.setStarvationRate(starvationMortality.getRate(school));
            // Update trophic level
            if (school.getPreyedBiomass() > 0) {
                Collection<PreyRecord> preys = school.getPreyRecords();
                if (!preys.isEmpty()) {
                    double trophicLevel = 0.d;
                    for (PreyRecord preyRecord : preys) {
                        trophicLevel += preyRecord.getTrophicLevel() * preyRecord.getBiomass() / school.getPreyedBiomass();
                    }
                    trophicLevel += 1;
                    school.setTrophicLevel((float) trophicLevel);
                }
            }
        }

        // Apply Zout mortality on schools out of the simulated domain
        for (School school : getSchoolSet().getOutSchools()) {
            double nDead = school.getInstantaneousAbundance() * (1 - Math.exp(-outsideMortalityProcess.getRate(school)));
            if (nDead > 0.d) {
                school.setNdead(MortalityCause.OUT, nDead);
            }
        }
    }
}
