package fr.ird.osmose.process;

import fr.ird.osmose.SimulationLinker;

/**
 *
 * @author pverley
 */
public abstract class AbstractProcess extends SimulationLinker {

    abstract public void loadParameters();

    abstract public void run();
}
