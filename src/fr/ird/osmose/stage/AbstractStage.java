/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.stage;

import fr.ird.osmose.Configuration;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.School;
import java.util.logging.Logger;

/**
 *
 * @author pverley
 */
public abstract class AbstractStage {
    
    abstract public void init();
    
    abstract public int getStage(School school);
    
    abstract public int getNStage(int iSpecies);
    
    abstract public float[] getThresholds(int iSpecies);
    
    Configuration getConfiguration() {
        return Osmose.getInstance().getConfiguration();
    }
    
    Logger getLogger() {
        return Osmose.getInstance().getLogger();
    }
    
}
