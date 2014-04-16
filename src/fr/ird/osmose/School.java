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
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

/**
 * This class represents a school of fish, it is the individual of the
 * Individual Based Model. A school is constituted by a pool of identical fish.
 * This feature allows Osmose to attribute to the school a set of state
 * variables characterizing the typical fish of the school:
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
 * located with both grid coordinates and geographical coordinates.
 *
 * @see GridPoint
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0b 2013/09/01
 */
public class School extends Prey implements ISchool {

///////////////////////////////
// Declaration of the variables
///////////////////////////////
    /**
     * {@link Species} of the fish.
     */
    final private Species species;
    /**
     * Age of the fish expressed in number of time step.
     */
    private int ageDt;
    /**
     * Age of the fish in year.
     */
    private float age;
    /**
     * Length of the fish in centimeter.
     */
    private float length;
    /**
     * Total length of the fish in centimeter.
     */
    private float totalLength;
    /**
     * Length of the fish in centimeter at the beginning of the time step.
     */
    private float lengthi;
    /**
     * List of {@code PreyRecord}. It keeps track of what the school has eaten
     * during the time step.
     */
    final private HashMap<Integer, PreyRecord> preyRecords;
    /**
     * Biomass of prey, in tonne, ingested by the school at current time step.
     */
    private double preyedBiomass;
    /**
     * Predation success rate, the ratio of what is preyed on maximal ingestion.
     * {@code predation success rate = what has been preyed / maximal ingestion}
     */
    private float predSuccessRate;
    /**
     * Starvation mortality rate.
     */
    private double starvationRate;
    /**
     * Whether the school is out of the simulated domain at current time step.
     */
    private boolean out;
    /**
     * Array of temporary accessibilities of preys for this predator
     */
    private double[] accessibilities;
    /**
     * A unique randomly generated {@link java.util.UUID} for identifying and
     * comparing schools.
     */
    final private UUID uuid = UUID.randomUUID();

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
     * @param length, the length of the fish in centimeter
     * @param weight, the weight of the fish in gram
     * @param ageDt, the age of the fish in number of time step
     * @param trophicLevel, the trophic level of the fish
     */
    public School(Species species, float x, float y, double abundance, float length, float weight, int ageDt, float trophicLevel) {
        super(species.getIndex(), x, y, abundance, weight, trophicLevel);
        this.species = species;
        this.length = length;
        totalLength = species.toTotalLength(length);
        this.ageDt = ageDt;
        this.age = ageDt / (float) getConfiguration().getNStepYear();
        out = false;
        preyRecords = new HashMap();
        starvationRate = 0.d;
    }

////////////////////////////
// Definition of the methods
////////////////////////////
    /**
     * Initializes and reset some state variables
     */
    public void init() {

        // Update abundance and reset number of dead
        updateAbundance();
        // Reset diet variables
        preyRecords.clear();
        preyedBiomass = 0.d;
        predSuccessRate = 0.f;
        // by default the school is in the simulated area, and migration might
        // change this state.
        out = false;
        // Set length at the beginning of the time step
        lengthi = length;
    }

    /**
     * Add a new prey record.
     *
     * @param indexPrey, the index of the prey
     * @param trophicLevel, the trophic level of the prey
     * @param preyedBiomass, the biomass preyed on this prey
     * @param age, the age of the prey
     * @param length, the length of the prey
     * @param keepRecord, whether or not Osmose should keep the prey record in
     * memory.
     */
    public void addPreyRecord(int indexPrey, float trophicLevel, float age, float length, double preyedBiomass, boolean keepRecord) {
        if (keepRecord) {
            PreyRecord newRecord = new PreyRecord(indexPrey, trophicLevel, age, length, preyedBiomass);
            int hash = newRecord.hashCode();
            if (preyRecords.containsKey(hash)) {
                preyRecords.get(hash).incrementBiomass(newRecord.getBiomass());
            } else {
                preyRecords.put(newRecord.hashCode(), newRecord);
            }
        }
        // Update school total preyed biomass
        this.preyedBiomass += preyedBiomass;
    }

    /**
     * Returns a list of the prey records at current time step.
     *
     * @return a list of the prey records at current time step.
     */
    public Collection<PreyRecord> getPreyRecords() {
        return preyRecords.values();
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
     * Returns the length of the fish, in centimeter.
     *
     * @return the length of the fish, in centimeter
     */
    @Override
    public float getLength() {
        return length;
    }

    public float getTotalLength() {
        return totalLength;
    }

    /**
     * Increments the length of the fish from given number of centimeter.
     *
     * @param dlength, the length increment in centimeter
     */
    public void incrementLength(float dlength) {
        if (dlength != 0.f) {
            length += dlength;
            totalLength = species.toTotalLength(length);
            setWeight(species.computeWeight(length) * 1e-6f);
        }
    }

    /**
     * Gets the {@link Configuration} instance.
     *
     * @return the {@code Configuration} instance
     */
    private Configuration getConfiguration() {
        return Osmose.getInstance().getConfiguration();
    }

    /**
     * Checks whether the object is equal to this school.
     *
     * @param obj, the object with which to compare to this school
     * @return {@code true} is the object is a {@code School} and has the same
     * {@code UUID}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final School other = (School) obj;
        return this.uuid.equals(other.uuid);
    }

    /**
     * Returns a hash code value for the school
     *
     * @see Object#hashCode()
     * @return a hash code value for the school
     */
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 23 * hash + uuid.hashCode();
        return hash;
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
     * Returns the preyed biomass at current time step.
     *
     * @return the preyed biomass at current time step.
     */
    public double getPreyedBiomass() {
        return preyedBiomass;
    }

    /**
     * Returns the predation success rate.
     *
     * @return the predation success rate
     */
    public float getPredSuccessRate() {
        return predSuccessRate;
    }

    /**
     * Sets the predation success rate.
     *
     * @param rate the predSuccessRate to set
     */
    public void setPredSuccessRate(float rate) {
        this.predSuccessRate = rate;
    }

    public void incrementPredSuccessRate(float drate) {
        this.predSuccessRate += drate;
    }

    /**
     * @return the starvationRate
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
     * @return the lengthi
     */
    @Override
    public float getLengthIniStep() {
        return lengthi;
    }

    public void setAccessibilities(double[] accessibilities) {
        this.accessibilities = accessibilities;
    }

    public double[] getAccessibilities() {
        return accessibilities;
    }
}
