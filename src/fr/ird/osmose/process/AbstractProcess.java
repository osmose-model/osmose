package fr.ird.osmose.process;

import fr.ird.osmose.SimulationLinker;

/**
 *
 * @author pverley
 */
public abstract class AbstractProcess extends SimulationLinker {
    
    AbstractProcess(int replica) {
        super(replica);
    }

    abstract public void init();

    abstract public void run();
}
