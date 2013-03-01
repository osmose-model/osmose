package fr.ird.osmose.populator;

import fr.ird.osmose.util.SimulationLinker;

/**
 *
 * @author pverley
 */
public abstract class AbstractPopulator extends SimulationLinker {
    
    AbstractPopulator(int replica) {
        super(replica);
    }

    abstract public void loadParameters();
    
    abstract public void populate();
    
    
}
