
package fr.ird.osmose;

/**
 *
 * @author pverley
 */
public abstract class AbstractProcess {
    
    abstract public void loadParameters();
    
    abstract public void run();
    
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
