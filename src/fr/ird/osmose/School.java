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
import java.util.ArrayList;
import java.util.List;
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
public class School extends GridPoint implements Prey {

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
    private int age;
    /**
     * Length of the fish in centimeter.
     */
    private float length;
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
     * List of {@code PreyRecord}. It keeps track of what the school has eaten
     * during the time step.
     */
    final private List<PreyRecord> preyRecords;
    /**
     * Biomass of prey, in tonne, ingested by the school at current time step.
     */
    private double preyedBiomass;
    /**
     * Number of dead fish in the current time step, for each mortality cause.
     */
    private final double[] nDead = new double[MortalityCause.values().length];
    /**
     * Predation success rate, the ratio of what is preyed on maximal ingestion.
     * {@code predation success rate = what has been preyed / maximal ingestion}
     */
    private float predSuccessRate;
    /**
     * Monitor whether the number of dead has changed. It helps to prevent
     * unnecessary recalculation of the instantaneous biomass.
     */
    private boolean ndeadHasChanged;
    /**
     * Whether the school is out of the simulated domain at current time step.
     */
    private boolean out;
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
     * @param age, the age of the fish in number of time step
     * @param trophicLevel, the trophic level of the fish
     */
    public School(Species species, float x, float y, double abundance, float length, float weight, int age, float trophicLevel) {
        this.species = species;
        this.abundance = abundance;
        instantaneousAbundance = abundance;
        this.length = length;
        this.weight = weight * 1.e-6f;
        this.age = age;
        this.trophicLevel = trophicLevel;
        if (x >= 0 && x < getGrid().get_nx() && y >= 0 && y < getGrid().get_ny()) {
            moveToCell(getGrid().getCell(Math.round(x), Math.round(y)));
        } else {
            setOffGrid();
        }
        out = false;
        preyRecords = new ArrayList();
    }

////////////////////////////
// Definition of the methods
////////////////////////////
    /**
     * Initializes and reset some state variables
     */
    public void init() {

        // Update abundance
        abundance = getInstantaneousAbundance();
        // Rest number of dead fish
        for (int iDeath = 0; iDeath < MortalityCause.values().length; iDeath++) {
            nDead[iDeath] = 0.d;
        }
        ndeadHasChanged = false;
        // Reset diet variables
        preyRecords.clear();
        preyedBiomass = 0.d;
        predSuccessRate = 0.f;
        // by default the school is in the simulated area, and migration might
        // change this state.
        out = false;
    }

    /**
     * Add a new prey record.
     *
     * @param prey, the {@code Prey} that has been ingested by this school
     * @param preyedBiomass, the biomass preyed on this prey.
     * @param preyStage, the stage of the prey,
     * {@link fr.ird.osmose.stage.DietOutputStage}
     */
    public void addPreyRecord(Prey prey, double preyedBiomass, int preyStage) {
        preyRecords.add(new PreyRecord(prey, preyedBiomass, preyStage));
        // Update school total preyed biomass
        this.preyedBiomass += preyedBiomass;
    }

    /**
     * Returns a list of the prey records at current time step.
     *
     * @return a list of the prey records at current time step.
     */
    public List<PreyRecord> getPreyRecords() {
        return preyRecords;
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
        if (ndeadHasChanged) {
            instantaneousAbundance = abundance - sum(nDead);
            if (instantaneousAbundance < 1.d) {
                instantaneousAbundance = 0.d;
            }
            ndeadHasChanged = false;
        }
        return instantaneousAbundance;
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
     * Checks whether the school is alive. A school is alive if it fulfills both
     * conditions: {@code instantaneous abundance > 0} and
     * {@code age <= lifespan - 1}
     *
     * @return whether the school is alive or not
     */
    public boolean isAlive() {
        return (getInstantaneousAbundance() > 0) && (age <= species.getLifespanDt() - 1);
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
        float ageInYear = getAgeDt() / (float) getConfiguration().getNStepYear();
        str.append(ageInYear);
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
     * Returns the index of the species
     *
     * @return the index of the species
     */
    public int getSpeciesIndex() {
        return species.getIndex();
    }

    /**
     * Returns the age of the fish in number of time step.
     *
     * @return the age of the fish in number of time step
     */
    public int getAgeDt() {
        return age;
    }

    /**
     * Increments the age of the fish of one time step.
     */
    public void incrementAge() {
        age += 1;
    }

    /**
     * Returns the length of the fish, in centimeter.
     *
     * @return the length of the fish, in centimeter
     */
    public float getLength() {
        return length;
    }

    /**
     * Returns the weight of the fish, in tonne.
     *
     * @return the weight of the fish, in tonne
     */
    public float getWeight() {
        return weight;
    }

    /**
     * Increments the length of the fish from given number of centimeter.
     *
     * @param dlength, the length increment in centimeter
     */
    public void incrementLength(float dlength) {
        if (dlength != 0.f) {
            length += dlength;
            weight = species.computeWeight(length) * 1e-6f;
        }
    }

    /**
     * Returns the trophic level of the fish.
     *
     * @return the trophic level of the fish
     */
    @Override
    public float getTrophicLevel() {
        return (age <= 1)
                ? Species.TL_EGG
                : trophicLevel;
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
        ndeadHasChanged = true;
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
        ndeadHasChanged = true;
    }

    /**
     * Resets the number of dead fish for a given mortality cause.
     *
     * @see MortalityCause
     * @param cause, the mortality cause
     */
    public void resetNdead(MortalityCause cause) {
        nDead[cause.index] = 0;
        ndeadHasChanged = true;
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

    /**
     * This class provides a record of a predation event by the school on a
     * given prey. A prey can be either an other school or a plankton group. A
     * record keep tracks of: the type of prey (which species or plankton
     * group), the biomass preyed, the {@code DietOutputStage} of the prey and
     * the trophic level of the prey.
     *
     * @see Prey
     */
    public class PreyRecord {

        /**
         * The preyed biomass, in tonne.
         */
        private final double biomass;
        /**
         * The {@link fr.ird.osmose.stage.DietOutputStage} of the prey.
         */
        private final int dietOutputStage;
        /**
         * The trophic level of the prey.
         */
        private final float trophicLevel;
        /**
         * The index of the species or / plankton group of the prey. In order to
         * distinguish a school from a plankton, the index follows the following
         * convention: index = index species if the prey is an instance of
         * {@code School} and index = nSpecies + index plankton group if the
         * prey is an instance of {@code Plankton}
         */
        private final int index;

        /**
         * Created a new prey record.
         *
         * @param prey, the prey, either a {@code School} or a {@code Plankton}
         * group
         * @param biomass, the preyed biomass, in tonne
         * @param dietOutputStage, the {@code DietOutputStage} of the prey
         */
        PreyRecord(Prey prey, double biomass, int dietOutputStage) {
            this.biomass = biomass;
            this.dietOutputStage = dietOutputStage;
            this.trophicLevel = prey.getTrophicLevel();
            if (prey instanceof School) {
                index = ((School) prey).getSpeciesIndex();
            } else {
                index = ((Plankton) prey).getIndex() + getConfiguration().getNSpecies();
            }
        }

        /**
         * Returns the preyed biomass, in tonne.
         *
         * @return the preyed biomass, in tonne
         */
        public double getBiomass() {
            return biomass;
        }

        /**
         * Returns the the {@code DietOutputStage} of the prey.
         *
         * @see fr.ird.osmose.stage.DietOutputStage
         * @return the {@code DietOutputStage} of the prey
         */
        public int getStage() {
            return dietOutputStage;
        }

        /**
         * Returns the trophic level of the prey.
         *
         * @return the trophic level of the prey
         */
        public float getTrophicLevel() {
            return trophicLevel;
        }

        /**
         * Returns the index of the prey. In order to distinguish a school from
         * a plankton, the index follows the following convention: index = index
         * species if the prey is an instance of {@code School} and index =
         * nSpecies + index plankton group if the prey is an instance of
         * {@code Plankton}
         *
         * @return the index of the prey
         */
        public int getIndex() {
            return index;
        }
    }
}
