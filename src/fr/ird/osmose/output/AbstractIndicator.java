
package fr.ird.osmose.output;

import fr.ird.osmose.School;
import fr.ird.osmose.SimulationLinker;

/**
 *
 * @author pverley
 */
public abstract class AbstractIndicator extends SimulationLinker {
    
    abstract public void reset();
    
    abstract public void update(School school);
    
    abstract public boolean isEnabled();
    
    abstract public void write(float time);
    
    public int getNSpecies() {
        return getSimulation().getNumberSpecies();
    }
    
    
}
