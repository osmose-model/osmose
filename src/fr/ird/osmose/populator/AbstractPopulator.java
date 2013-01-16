package fr.ird.osmose.populator;

import fr.ird.osmose.SimulationLinker;

/**
 *
 * @author pverley
 */
public abstract class AbstractPopulator extends SimulationLinker {

    abstract public void loadParameters();
    
    abstract public void populate();
}
