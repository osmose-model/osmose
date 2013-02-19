/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.ird.osmose.ltl;

import fr.ird.osmose.Plankton;

/**
 *
 * @author pverley
 */
public interface LTLForcing {

    /**
     * Initializes the LTLForcing.
     * Reads configuration files and creates plankton groups.
     */
    public void init();
    /**
     * Update plankton biomass for current time step
     * @param iStepSimu, the current time index of the simulation
     */
    public void update(int iStepSimu);
    /**
     * Gets the number of plankton groups
     * @return the number of plankton groups
     */
    public int getNumberPlanktonGroups();
    /**
     * Gets the specified plankton group.
     * @param iPlankton, the index of the plankton group.
     * @return the plankton group number iPlankton.
     */
    public Plankton getPlankton(int iPlankton);
}
