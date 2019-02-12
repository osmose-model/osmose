/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose;

import fr.ird.osmose.process.mortality.MortalityCause;
import fr.ird.osmose.util.GridPoint;
import java.util.HashMap;
import java.util.UUID;

/**
 * Abstract class for school management (focal species and background).
 *
 * @author nbarrier
 */
public abstract class AbstractSchool extends GridPoint implements IAggregation {

    /**
     * A unique randomly generated {@link java.util.UUID} for identifying and
     * comparing schools.
     */
    final private UUID uuid = UUID.randomUUID();

    /**
     * Abstract function to update instantaneous biomass and abundance.
     */
    protected abstract void updateBiomAndAbd();

    /**
     * Number of fish in the school, estimated on the fly.
     * {@code instantaneousAbundance = abundance - ndead}
     */
    protected double instantaneousAbundance;

    /**
     * Biomass, in tonne, of the school, estimated on the fly.
     */
    protected double instantaneousBiomass;

    /**
     * Number of fish in the school at the beginning of the time step.
     */
    protected double abundance;

    /**
     * Biomass, in tonne, of the school at the beginning of the time step.
     */
    protected double biomass;

    /**
     * Predation success rate, the ratio of what is preyed on maximal ingestion.
     * {@code predation success rate = what has been preyed / maximal ingestion}
     */
    protected float predSuccessRate;

    /**
     * Number of dead fish in the current time step, for each mortality cause.
     */
    protected final double[] nDead = new double[MortalityCause.values().length];

    /**
     * Biomass of prey, in tonne, ingested by the school at current time step.
     */
    protected double preyedBiomass;

    /**
     * List of {@code PreyRecord}. It keeps track of what the school has eaten
     * during the time step.
     */
    protected HashMap<Integer, Prey> preys;

    /**
     * Array of temporary accessibility of preys for this predator
     */
    protected double[] accessibility;

    /**
     * Monitor whether the number of dead has changed. It helps to prevent
     * unnecessary recalculation of the instantaneous biomass.
     */
    protected boolean abundanceHasChanged;

    @Override
    public void incrementNdead(MortalityCause cause, double nDead) {
        this.nDead[cause.index] += nDead;
        abundanceHasChanged = true;
    }

    /**
     * Computes the sum of a given double array.
     *
     * @param array, the double array to be summed
     * @return the sum of the doubles of the array
     */
    protected double sum(double[] array) {
        double sum = 0.d;
        for (double d : array) {
            sum += d;
        }
        return sum;
    }

    /**
     * Reset a double array to zero.
     *
     * @param array to be reseted.
     */
    protected void reset(double[] array) {
        for (int i = 0; i < array.length; i++) {
            array[i] = 0.d;
        }
    }

    /**
     * Get the prey accessibility for this school (from a predator perspective)
     * in current cell at current time step.
     *
     * @return the prey accessibility of this school as a predator.
     */
    @Override
    public double[] getAccessibility() {
        return accessibility;
    }

    /**
     * Record a new predation event. If keepRecord is TRUE, Osmose keeps the
     * characteristics of the predation event (prey species index, TL, age and
     * length) otherwise it only increments the preyed biomass.
     *
     * @param indexPrey, the index of the prey
     * @param trophicLevel, the trophic level of the prey
     * @param preyedBiom, the biomass preyed on this prey
     * @param age, the age of the prey
     * @param length, the length of the prey
     * @param keepRecord, whether or not Osmose should keep the prey record in
     * memory.
     */
    @Override
    public void preyedUpon(int indexPrey, float trophicLevel, float age, float length, double preyedBiom, boolean keepRecord) {
        if (keepRecord) {
            Prey prey = new Prey(indexPrey, trophicLevel, age, length, preyedBiom);
            int hash = prey.hashCode();
            if (preys.containsKey(hash)) {
                preys.get(hash).incrementBiomass(prey.getBiomass());
            } else {
                preys.put(prey.hashCode(), prey);
            }
        }
        // Update school total preyed biomass
        this.preyedBiomass += preyedBiom;
    }

    /**
     * Gets the abundance of the school at the beginning of the time step.
     *
     * @return the abundance of the school at the beginning of the time step
     */
    @Override
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
    @Override
    public double getInstantaneousAbundance() {
        if (abundanceHasChanged) {
            updateBiomAndAbd();
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
    @Override
    public double getBiomass() {
        return biomass;
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
    @Override
    public double getInstantaneousBiomass() {
        if (abundanceHasChanged) {
            updateBiomAndAbd();
        }
        return instantaneousBiomass;
    }

    /**
     * Set prey accessibility for this school (from a predator perspective) in
     * current cell at current time step. Prey accessibility is only used in the
     * MortalityProcess and could be temporarily stored in that class but it
     * felt just easier and quicker to set it directly in the school object.
     *
     * @param accessibility, the prey accessibility of this school as a predator
     */
    public void setAccessibility(double[] accessibility) {
        this.accessibility = accessibility;
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
     * Increment the predation success rate.
     *
     * @param drate, the increment of the predation success rate.
     */
    @Override
    public void incrementPredSuccessRate(float drate) {
        this.predSuccessRate += drate;
    }

    /**
     * Returns a hash code value for the school. Overwrite the Cell
     * hashCode method.
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
     * Checks whether the object is equal to this school. Overwrite the Cell
     * equals method.
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
        final AbstractSchool other = (AbstractSchool) obj;
        return this.uuid.equals(other.uuid);
    }

}
