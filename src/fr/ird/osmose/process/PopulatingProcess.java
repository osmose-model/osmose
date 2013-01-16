/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process;

import fr.ird.osmose.populator.BiomassPopulator;
import fr.ird.osmose.populator.Populator;
import fr.ird.osmose.populator.SpectrumPopulator;

/**
 *
 * @author pverley
 */
public class PopulatingProcess extends AbstractProcess {
    
    private Populator populator;

    @Override
    public void loadParameters() {
        
        int numSerie = getOsmose().numSerie;
        if (getOsmose().calibrationMethod[numSerie].equalsIgnoreCase("biomass")) {
            populator = new BiomassPopulator();
        } else if (getOsmose().calibrationMethod[numSerie].equalsIgnoreCase("spectrum")) {
            populator = new SpectrumPopulator();
        } else if (getOsmose().calibrationMethod[numSerie].equalsIgnoreCase("random")) {
            throw new UnsupportedOperationException("Random initialization not supported yet.");
        }
    }

    @Override
    public void run() {
        populator.populate();
    }
    
}
