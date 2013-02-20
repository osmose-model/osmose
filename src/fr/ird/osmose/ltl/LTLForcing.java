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
     * Compute plankton biomass for current time step
     * @param plankton, a plankton group
     * @param iStepSimu, the current time index of the simulation
     */
    public float[][] computeBiomass(Plankton plankton, int iStepSimu);
}
