package fr.ird.osmose.process;

import fr.ird.osmose.util.SimulationLinker;

/**
 *
 * @author pverley
 */
public abstract class AbstractProcess extends SimulationLinker {
    
    AbstractProcess(int indexSimulation) {
        super(indexSimulation);
    }

    abstract public void init();

    abstract public void run();
}
