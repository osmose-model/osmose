
package fr.ird.osmose;

/**
 *
 * @author pverley
 */
public abstract class AbstractProcess {
    
    abstract public void loadParameters();
    
    abstract public void run();
    
    public static Osmose getOsmose() {
        return Osmose.getInstance();
    }
    
    public static IGrid getGrid() {
        return getOsmose().getGrid();
    }

    public static Simulation getSimulation() {
        return getOsmose().getSimulation();
    }

    public static Population getPopulation() {
        return getSimulation().getPopulation();
    }
    
}
