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

import fr.ird.osmose.process.mortality.MortalityCause;
import fr.ird.osmose.util.GridPoint;
import java.util.Collection;
import java.util.HashMap;

/**
 * This class represents a school of fish, it is the individual of the
 * Individual Based Model. A school is constituted by a pool of identical fish.
 * This feature allows Osmose to attribute to the school a set of state
 * variables characterising the typical fish of the school:
 * <ul>
 * <li>species<li>
 * <li>age</li>
 * <li>length</li>
 * <li>weight</li>
 * <li>trophic level</li>
 * </ul>
 * The school also has proper state variables:
 * <ul>
 * <li>abundance</li>
 * <li>biomass</li>
 * </ul>
 * Stricto sensus, the fish should be the individual of the IBM but it is
 * unrealistic in terms of computational power to manage such large number of
 * individuals. The concept of school made of fish with identical
 * characteristics offers a good comprise between computational cost and
 * biological relevancy. Later on, when the documentation refers to "fish" it
 * must be understood as the typical fish constituting the school.<br>
 * The {@code School} extends a {@code GridPoint}. It means the school is
 * located with both grid coordinates and geographical coordinates. The way
 * Osmose presently handles spatial movements, it is unnecessarily "fancy" for a
 * school to extends a GridPoint. Indeed Osmose locates the schools in grid
 * cells and does not need more precise coordinates. A mere Cell attribute in
 * the School object would be enough at the moment. The GridPoint opens avenue
 * for future improvement or refinement in the spatial movements of the schools.
 *
 * @see GridPoint
 * @author P.Verley (philippe.verley@ird.fr)
 */
public class School extends AbstractSchool {

///////////////////////////////
// Declaration of the variables
///////////////////////////////
    /**
     * {@link Species} of the fish.
     */
    final private Species species;
    /**
     * The index of the species. From [0; nSpec-1] for fish and from [nSpec;
     * nSpec+nLTL-1] for plankton.
     */
    private final int index;
    /**
     * Weight of the fish in tonne. The unit has been set to tonne just because
     * it saves computation time for converting the biomass from gramme to tonne
     */
    private float weight;
    /**
     * Trophic level of the fish.
     */
    private float trophicLevel;

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
     * Age of the fish expressed in number of time step.
     */
    private int ageDt;
    /**
     * Age of the fish in year.
     */
    private float age;
    /**
     * Length of the fish in centimetre.
     */
    private float length;
    /**
     * Length of the fish in centimetre at the beginning of the time step.
     */
    private float lengthi;

    /**
     * Starvation mortality rate.
     */
    private double starvationRate;
    /**
     * Whether the school is out of the simulated domain at current time step.
     */
    private boolean out;
    
    private double ingestion;

///////////////
// Constructors
///////////////
    /**
     * Create a new school at egg stage with a given number of eggs. State
     * variables are preset:
     * <ul>
     * <li>age set to zero</li>
     * <li>length set from parameter <i>species.egg.size.sp#</i></li>
     * <li>weight set from parameter <i>species.egg.weight.sp#</i></li>
     * <li>trophic level set from constant {@link Species#TL_EGG}</li>
     * <li>the school is not yet located on the grid</li>
     * </ul>
     *
     * @param species, the {@link Species} of the fish
     * @param abundance, the number of eggs in the school
     */
    public School(Species species, double abundance) {
        this(species, abundance, species.getEggSize(), species.getEggWeight(), 0);
    }

    /**
     * Create a new school, with given species, abundance, length, weight and
     * age. Trophic level is preset to {@link Species#TL_EGG} and the school is
     * not located on the grid.
     *
     * @param species, the {@link Species} of the fish
     * @param abundance, the number of fish in the school
     * @param length, the length of the fish in centimeter
     * @param weight, the weight of the fish in gram
     * @param age, the age of the fish in number of time step
     */
    public School(Species species, double abundance, float length, float weight, int age) {
        this(species, -1, -1, abundance, length, weight, age, Species.TL_EGG);
    }

    /**
     * Create a new school, with given species, grid coordinates, abundance,
     * length weight, age and trophic level.
     *
     * @param species, the {@link Species} of the fish
     * @param x, x coordinate of the school on the grid
     * @param y, y coordinate of the school on the grid
     * @param abundance, the number of fish in the school
     * @param length, the length of the fish in centimetre
     * @param weight, the weight of the fish in gramme
     * @param ageDt, the age of the fish in number of time step
     * @param trophicLevel, the trophic level of the fish
     */
    public School(Species species, float x, float y, double abundance, float length, float weight, int ageDt, float trophicLevel) {
        this.index = species.getIndex();
        this.abundance = abundance;
        instantaneousAbundance = abundance;
        this.weight = weight * 1.e-6f;
        biomass = instantaneousBiomass = abundance * this.weight;
        abundanceHasChanged = false;
        this.trophicLevel = trophicLevel;
        if (x >= 0 && x < getGrid().get_nx() && y >= 0 && y < getGrid().get_ny()) {
            moveToCell(getGrid().getCell(Math.round(x), Math.round(y)));
        } else {
            setOffGrid();
        }
        eggRetained = 0.d;
        this.species = species;
        this.length = length;
        this.ageDt = ageDt;
        this.age = ageDt / (float) getConfiguration().getNStepYear();
        out = false;
        preys = new HashMap();
        starvationRate = 0.d;
    }

////////////////////////////
// Definition of the methods
////////////////////////////
    /**
     * Initialises and reset some state variables
     */
    public void init() {

        // Update abundance
        abundance = getInstantaneousAbundance();
        // Update biomass
        biomass = abundance * weight;
        // Rest number of dead fish
        reset(nDead);
        // Reset diet variables
        preys.clear();
        preyedBiomass = 0.d;
        predSuccessRate = 0.f;
        // by default the school is in the simulated area, and migration might
        // change this state.
        out = false;
        // Set length at the beginning of the time step
        lengthi = length;
    }
    
    @Override
    public void incrementIngestion(double cumPreyUpon) {
        this.ingestion += cumPreyUpon;
    }

    /**
     * Make some more eggs accessible. This function assumes that the initial
     * abundance of egg at the beginning of the predation process (abundance -
     * eggLoss) is homogeneously released throughout every sub time step. Every
     * sub time step, the amount of egg to be released is equal to
     * (abundance-eggLoss) / subdt.
     *
     * @param subdt, the sub time step of the mortality algorithm
     */
    public void releaseEgg(int subdt) {
        eggRetained = Math.max(0.d, eggRetained - (abundance - nDead[MortalityCause.ADDITIONAL.index]) / (double) subdt);
        abundanceHasChanged = true;
    }

    /**
     * Retain all the eggs left available for predation process. The eggs
     * available for predation means the initial abundance minus the egg loss.
     * After calling this function the instantaneous abundance is zero. One must
     * call the {@link #releaseEgg} function to release some eggs.
     */
    public void retainEgg() {
        eggRetained = abundance - nDead[MortalityCause.ADDITIONAL.index];
        abundanceHasChanged = true;
    }

    /**
     * Converts the specified biomass [tonne] into abundance [number of fish]
     *
     * @param biomass, the biomass of the school, in tonne
     * @return the number of fish weighting {@code weight} corresponding to this
     * level of biomass. {@code abundance = biomass / weight}
     */
    @Override
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
    @Override
    public double abd2biom(double abundance) {
        return abundance * weight;
    }

    /**
     * Returns the averaged trophic level of the plankton group. Parameter
     * <i>plankton.TL.plk#</i>
     *
     * @return the averaged trophic level of the plankton group
     */
    @Override
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
    @Override
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
     * Resets the number of dead fish for a given mortality cause.
     *
     * @see MortalityCause
     * @param cause, the mortality cause
     */
    public void resetNdead(MortalityCause cause) {
        nDead[cause.index] = 0;
        abundanceHasChanged = true;
    }

    /**
     * The weight of the fish (not the whole school), in tonne.
     *
     * @return the weight on the fish, in tonne.
     */
    @Override
    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    /**
     * Returns a list of the prey records at current time step.
     *
     * @return a list of the prey records at current time step.
     */
    public Collection<Prey> getPreys() {
        return preys.values();
    }

    /**
     * Sets the school out of the simulated domain.
     */
    public void out() {
        out = true;
        setOffGrid();
    }

    /**
     * Checks whether the school is out of the simulated domain.
     *
     * @return {@code true} if the school is out of the simulated domain.
     */
    public boolean isOut() {
        return out;
    }

    /**
     * Checks whether the school is alive. A school is alive if it fulfills both
     * conditions: {@code instantaneous abundance > 0} and null
     * {@code age <= lifespan - 1}
     *
     * @return whether the school is alive or not
     */
    public boolean isAlive() {
        return (getInstantaneousAbundance() > 0) && (ageDt <= species.getLifespanDt() - 1);
    }

    /**
     * Returns a string representation of the school (species, cohort and
     * location).
     *
     * @return a string representation of the school
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("School");
        str.append("\n  Species: ");
        str.append(getSpecies().getName());
        str.append("\n  Cohort: ");
        str.append(getAge());
        str.append(" [year]");
        str.append("\n  Cell: ");
        str.append(getCell().getIndex());
        return str.toString();
    }

    /**
     * Returns the species of the school.
     *
     * @see Species
     * @return the species of the school
     */
    public Species getSpecies() {
        return species;
    }

    /**
     * Returns the age of the fish in number of time step.
     *
     * @return the age of the fish in number of time step
     */
    @Override
    public int getAgeDt() {
        return ageDt;
    }

    @Override
    public float getAge() {
        return age;
    }

    /**
     * Increments the age of the fish of one time step.
     */
    public void incrementAge() {
        ageDt += 1;
        age = ageDt / (float) getConfiguration().getNStepYear();
    }

    /**
     * Returns the length of the fish, in centimetre.
     *
     * @return the length of the fish, in centimetre
     */
    @Override
    public float getLength() {
        return length;
    }

    /**
     * Increments the length of the fish from given number of centimetre.
     *
     * @param dlength, the length increment in centimetre
     */
    public void incrementLength(float dlength) {
        if (dlength != 0.f) {
            length += dlength;
            setWeight(species.computeWeight(length) * 1e-6f);
        }
    }

    /**
     * Get the starvation rate at current time step.
     *
     * @return the starvation rate
     */
    public double getStarvationRate() {
        return starvationRate;
    }

    /**
     * @param starvationRate the starvationRate to set
     */
    public void setStarvationRate(double starvationRate) {
        this.starvationRate = starvationRate;
    }

    /**
     * Length of the fish at the beginning of the time step, in centimetre.
     *
     * @return the length at the beginning of the time step, in centimetre.
     */
    public float getLengthIniStep() {
        return lengthi;
    }

    @Override
    public void updateBiomAndAbd() {
        instantaneousAbundance = (abundance - eggRetained) - sum(nDead);
        if (instantaneousAbundance < 1.d) {
            instantaneousAbundance = 0.d;
        }
        instantaneousBiomass = instantaneousAbundance * weight;
        abundanceHasChanged = false;
    }
}
