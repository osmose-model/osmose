/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.ltl;

import fr.ird.osmose.Cell;

/**
 *
 * @author pverley
 */
public interface LTLForcing {

    /**
     * Initialises the LTLForcing. Read configuration files; load the LTL grid
     * (as it may be different from the Osmose grid); ensure that the LTL
     * forcing files matches the description of the plankton groups; etc. Such
     * are the actions to be undertaken in this function.
     */
    public void init();

    /**
     * Returns the biomass, in tonne, of a specified LTL group in a specified
     * cell at current time step of the simulation.
     *
     * @param iPlk, the index of the LTL group
     * @param cell, a {@code Cell} of the grid
     * @return the biomass, in tonne, of the LTL group at index {@code iLTL} in
     * this {@code cell}}
     */
    public double getBiomass(int iPlk, Cell cell);

    /**
     * Updates the biomass of the LTL groups at the current time step of the
     * simulation.
     *
     * @param iStepSimu, the current step of the simulation
     */
    public void update(int iStepSimu);

}
