/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose;

import fr.ird.osmose.process.mortality.MortalityCause;

/**
 * The Swarm is the equivalent of the School in Osmose for the plankton group.
 * It is simplified though as the Swarm is only used in the predation process.
 * We only need to keep track of the preyed biomass to provide the instantaneous
 * biomass. In this class we make no distinction between abundance and biomass
 * 
 * 
 * @author P. Verley
 */
public class Swarm implements IAggregation {

    /**
     * Pointer to the plankton group
     */
    final private Plankton plankton;
    /**
     * Pointer to the cell where this swarm is located
     */
    final private Cell cell;
    /**
     * Species index of the swarm, which ranges from nSpecies to nSpecis + nLTL
     */
    final private int index;
    /**
     * Number of organisms in the swarm at the beginning of the time step
     */
    private double abundance;
    /**
     * Number of dead plankton in the current time step, killed by predation
     */
    private double nDead;

    public Swarm(Plankton plankton, Cell cell) {
        this.plankton = plankton;
        this.cell = cell;
        this.index = plankton.getIndex() + Osmose.getInstance().getConfiguration().getNSpecies();
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
        return Math.max(0.d, abundance - nDead);
    }
  
    public void setBiomass(double biomass) {

        // Update abundance
        // (for plankton Osmose makes no difference between abundance and biomass)
        abundance = biomass;
        // Rest number of dead fish
        nDead = 0.d;
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
        return abundance;
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
        return getInstantaneousAbundance();
    }

    /**
     * Increments the number of dead fish for a given mortality cause.
     *
     * @see MortalityCause
     * @param cause, the mortality cause
     * @param nDead, the number of dead fish to be incremented for this
     * mortality cause
     */
    @Override
    public void incrementNdead(MortalityCause cause, double nDead) {
        if (cause != MortalityCause.PREDATION) {
            throw new UnsupportedOperationException("MortalityCause for Swarm must be PREDATION only.");
        }
        this.nDead += nDead;
    }

    @Override
    public double biom2abd(double biomass) {
        return biomass;
    }

    @Override
    public int getSpeciesIndex() {
        return index;
    }

    @Override
    public float getAge() {
        return Float.NaN;
    }

    @Override
    public float getLength() {
        return Float.NaN;
    }

    @Override
    public float getTrophicLevel() {
        return plankton.getTrophicLevel();
    }

    @Override
    public float getWeight() {
        return 1.f;
    }
    
    public Cell getCell() {
        return cell;
    }
    
    public int getLTLIndex() {
        return plankton.getIndex();
    }

}
