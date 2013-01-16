package fr.ird.osmose.populator;

import fr.ird.osmose.SimulationLinker;

/**
 *
 * @author pverley
 */
public abstract class Populator extends SimulationLinker {

    abstract public void loadParameters();
    
    abstract public void populate();
}
