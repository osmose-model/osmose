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

    final private int NSTEPS;

    AbstractStep(int indexSimulation) {
        super(indexSimulation);
        NSTEPS = getConfiguration().getNStepYear() * getConfiguration().getNYear();
    }

    abstract public void init();

    abstract public void step(int iStepSimu);

    public boolean isLastStep(int iStepSimu) {
        return iStepSimu == NSTEPS - 1;
    }
}
