/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.step;

import fr.ird.osmose.SimulationLinker;

/**
 *
 * @author pverley
 */
abstract public class AbstractStep extends SimulationLinker {

    final private int NSTEPS;

    AbstractStep() {
        NSTEPS = getOsmose().getNumberTimeStepsPerYear() * getOsmose().getNumberYears();
    }

    abstract public void init();

    abstract public void step();

    public boolean isLastStep() {
        return getSimulation().getIndexTimeSimu() == NSTEPS - 1;
    }
}
