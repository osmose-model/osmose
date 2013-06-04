/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.step;

import fr.ird.osmose.util.SimulationLinker;

/**
 *
 * @author pverley
 */
abstract public class AbstractStep extends SimulationLinker {

    AbstractStep(int indexSimulation) {
        super(indexSimulation);
    }

    /**
     * Initialization of the step.
     * It is called once at the beginning of the simulation.
     */
    abstract public void init();

    /**
     * This functions details what must be done in one time step.
     * It is called every time step of the simulation.
     * @param iStepSimu , the current time step of the simulation
     */
    abstract public void step(int iStepSimu);
    
    /**
     * This function is called once at the end of the simulation.
     * It basically servers to cleanup and close down everything before exiting.
     */
    abstract public void end();
}
