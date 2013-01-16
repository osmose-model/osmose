package fr.ird.osmose.populator;

import fr.ird.osmose.Osmose;
import fr.ird.osmose.Population;
import fr.ird.osmose.Simulation;
import fr.ird.osmose.grid.IGrid;

/**
 *
 * @author pverley
 */
public abstract class Populator {

    abstract public void loadParameters();
    
    abstract public void populate();

    public Osmose getOsmose() {
        return Osmose.getInstance();
    }
    
    public IGrid getGrid() {
        return getOsmose().getGrid();
    }

    public Simulation getSimulation() {
        return getOsmose().getSimulation();
    }

    public Population getPopulation() {
        return getSimulation().getPopulation();
    }
}
