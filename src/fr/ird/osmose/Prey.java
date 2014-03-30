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
package fr.ird.osmose;

import fr.ird.osmose.util.GridPoint;
import static fr.ird.osmose.util.GridPoint.getGrid;

/**
 * This classes gathers together the variables and the functions of an
 * aggregation of preys. It manages the abundance, the biomass and the number of
 * dead individuals in this aggregation of preys.
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0b 2013/09/01
 */
public class Prey extends GridPoint {

    /**
     * The index of the species. From [0; nSpec-1] for fish and from [nSpec;
     * nSpec+nLTL-1] for plankton.
     */
    private final int index;
    /**
     * Weight of the fish in tonne. The unit has been set to tonne just because
     * it saves computation time for converting the biomass from gram to tonne
     */
    private float weight;
    /**
     * Trophic level of the fish.
     */
    private float trophicLevel;
    /**
     * Number of fish in the school at the beginning of the time step.
     */
    private double abundance;
    /**
     * Number of fish in the school, estimated on the fly.
     * {@code instantaneousAbundance = abundance - ndead}
     */
    private double instantaneousAbundance;
    /**
     * Number of dead fish in the current time step, for each mortality cause.
     */
    private final double[] nDead = new double[MortalityCause.values().length];
    /**
     * Monitor whether the number of dead has changed. It helps to prevent
     * unnecessary recalculation of the instantaneous biomass.
     */
    private boolean abundanceHasChanged;
    /**
     * A buffer variable that will temporarily retain some eggs inside a time
     * step. The reproduction process makes all the eggs available at the
     * beginning of the time step. The stochastic mortality algorithm uses a sub
     * time step and we want the eggs to be made available progressively
     * throughout the sub time step (instead of having them all accessible at
     * the 1st sub time step). This variable represents the amount of eggs that
     * must be subtracted to the abundance at the beginning of the time step for
     * getting the instantaneous accessible abundance of eggs.
     */
    private double eggRetained;

    /**
     * Create a new school, with given species, grid coordinates, abundance,
     * length weight, age and trophic level.
     *
     * @param index, the index of the species
     * @param x, x coordinate of the school on the grid
     * @param y, y coordinate of the school on the grid
     * @param abundance, the number of fish in the school
     * @param weight, the weight of the fish in gram
     * @param trophicLevel, the trophic level of the fish
     */
    public Prey(int index, float x, float y, double abundance, float weight, float trophicLevel) {
        this.index = index;
        this.abundance = abundance;
        instantaneousAbundance = abundance;
        this.weight = weight * 1.e-6f;
        this.trophicLevel = trophicLevel;
        if (x >= 0 && x < getGrid().get_nx() && y >= 0 && y < getGrid().get_ny()) {
            moveToCell(getGrid().getCell(Math.round(x), Math.round(y)));
        } else {
            setOffGrid();
        }
        eggRetained = 0.d;
    }

    /**
     * Make some more eggs accessible. This function assumes that the initial
     * abundance of egg at the beginning of the time step is homogeneously
     * released throughout every sub time step. Every sub time step, the amount
     * of egg to be released is equal to abundance / subdt.
     *
     * @param subdt, the sub time step of the mortality algorithm
     */
    public void releaseEgg(int subdt) {
        eggRetained = Math.max(0.d, eggRetained - abundance / subdt);
        abundanceHasChanged = true;
    }

    /**
     * Retain all the eggs. After calling this function the instantaneous
     * abundance is zero. One must call the {@link #releaseEgg} function to
     * release some eggs.
     */
    public void retainEgg() {
        eggRetained = abundance;
        abundanceHasChanged = true;
    }

    /**
     * Gets the abundance of the school at the beginning of the time step.
     *
     * @return the abundance of the school at the beginning of the time step
     */
    public double getAbundance() {
        return abundance;
    }

    /**
     * Evaluates the instantaneous abundance of the school.
     *
     * @return the instantaneous abundance of the school. {@code instantaneous
     * abundance = abundance at the beginning of the time step - ndead fish}
     * (due to any source of mortality, {@link MortalityCause}) at the time the
     * function is called. It is a snapshot of the abundance of the school
     * within the current time step.
     */
    public double getInstantaneousAbundance() {
        if (abundanceHasChanged) {
            instantaneousAbundance = (abundance - eggRetained) - sum(nDead);
            if (instantaneousAbundance < 1.d) {
                instantaneousAbundance = 0.d;
            }
            abundanceHasChanged = false;
        }
        return instantaneousAbundance;
    }

    /**
     *
     */
    public void updateAbundance() {

        // Update abundance
        abundance = getInstantaneousAbundance();
        // Rest number of dead fish
        for (int iDeath = 0; iDeath < MortalityCause.values().length; iDeath++) {
            nDead[iDeath] = 0.d;
        }
        abundanceHasChanged = false;
    }

    /**
     * Gets the biomass of the school, in tonne, at the beginning of the time
     * step.
     *
     * @return the biomass of the school at the beginning of the time step in
     * tonne
     */
    public double getBiomass() {
        return adb2biom(abundance);
    }

    /**
     * Evaluates the instantaneous biomass of the school.
     *
     * @return the instantaneous biomass of the school. {@code instantaneous
     * biomass = biomass at the beginning of the time step - biomass dead fish}
     * (due to any source of mortality, {@link MortalityCause}) at the time the
     * function is called. It is a snapshot of the biomass of the school within
     * the current time step.
     */
    public double getInstantaneousBiomass() {
        return adb2biom(getInstantaneousAbundance());
    }

    /**
     * Converts the specified biomass [tonne] into abundance [number of fish]
     *
     * @param biomass, the biomass of the school, in tonne
     * @return the number of fish weighting {@code weight} corresponding to this
     * level of biomass. {@code abundance = biomass / weight}
     */
    public double biom2abd(double biomass) {
        return biomass / weight;
    }

    /**
     * Converts the specified abundance [number of fish] into biomass [tonne]
     *
     * @param abundance, a number of fish
     * @return the corresponding biomass of this number of fish weighting
     * {@code weight}. {@code biomass = abundance * weight}
     */
    public double adb2biom(double abundance) {
        return abundance * weight;
    }

    /**
     * Returns the averaged trophic level of the plankton group. Parameter
     * <i>plankton.TL.plk#</i>
     *
     * @return the averaged trophic level of the plankton group
     */
    public float getTrophicLevel() {
        return trophicLevel;
    }

    /**
     * Sets the trophic level of the fish.
     *
     * @param trophicLevel, the new trophic level of the fish
     */
    public void setTrophicLevel(float trophicLevel) {
        this.trophicLevel = trophicLevel;
    }

    /**
     * Returns the index of the species
     *
     * @return the index of the species
     */
    public int getSpeciesIndex() {
        return index;
    }

    /**
     * Returns the number of dead fish for a given mortality cause.
     *
     * @see MortalityCause
     * @param cause, the mortality cause
     * @return the number of dead fish for this mortality cause
     */
    public double getNdead(MortalityCause cause) {
        return nDead[cause.index];
    }

    /**
     * Sets the number of dead fish for a given mortality cause.
     *
     * @see MortalityCause
     * @param cause, the mortality cause
     * @param nDead, the number of dead fish for this mortality cause
     */
    public void setNdead(MortalityCause cause, double nDead) {
        this.nDead[cause.index] = nDead;
        abundanceHasChanged = true;
    }

    /**
     * Increments the number of dead fish for a given mortality cause.
     *
     * @see MortalityCause
     * @param cause, the mortality cause
     * @param nDead, the number of dead fish to be incremented for this
     * mortality cause
     */
    public void incrementNdead(MortalityCause cause, double nDead) {
        this.nDead[cause.index] += nDead;
        abundanceHasChanged = true;
    }

    /**
     * Resets the number of dead fish for a given mortality cause.
     *
     * @see MortalityCause
     * @param cause, the mortality cause
     */
    public void resetNdead(MortalityCause cause) {
        nDead[cause.index] = 0;
        abundanceHasChanged = true;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    /**
     * Computes the sum of a given double array.
     *
     * @param array, the double array to be summed
     * @return the sum of the doubles of the array
     */
    private double sum(double[] array) {
        double sum = 0.d;
        for (double d : array) {
            sum += d;
        }
        return sum;
    }

    /**
     * A list of mortality causes.
     */
    public enum MortalityCause {

        /**
         * Predation mortality
         *
         * @see fr.ird.osmose.process.PredationProcess
         */
        PREDATION(0),
        /**
         * Starvation mortality
         *
         * @see fr.ird.osmose.process.StarvationProcess
         */
        STARVATION(1),
        /**
         * Natural mortality
         *
         * @see fr.ird.osmose.process.NaturalMortality
         */
        NATURAL(2),
        /**
         * Fishing mortality
         *
         * @see fr.ird.osmose.process.FishingProcess
         */
        FISHING(3),
        /**
         * Out of domain mortality
         *
         * @see fr.ird.osmose.process.OutMortalityProcess
         */
        OUT(4);
        /**
         * Index of the mortality cause
         */
        public final int index;

        /**
         * Initializes a mortality cause with a given index.
         *
         * @param index, the index of the mortality cause
         */
        private MortalityCause(int index) {
            this.index = index;
        }
    }

}
