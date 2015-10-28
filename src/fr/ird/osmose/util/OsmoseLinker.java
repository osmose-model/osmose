/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.util;

import fr.ird.osmose.Configuration;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.Species;
import fr.ird.osmose.grid.IGrid;
import fr.ird.osmose.util.logging.OLogger;

/**
 *
 * @author pverley
 */
public class OsmoseLinker extends OLogger {
    
    final public Osmose getOsmose() {
        return Osmose.getInstance();
    }

    final public Configuration getConfiguration() {
        return getOsmose().getConfiguration();
    }

    final public Species getSpecies(int index) {
        return getConfiguration().getSpecies(index);
    }

    final public Species getSpecies(String name) {
        for (int i = 0; i < getNSpecies(); i++) {
            if (getSpecies(i).getName().equalsIgnoreCase(name)) {
                return getSpecies(i);
            }
        }
        return null;
    }

    /**
     * The number of simulated species
     *
     * @return the number of simulated species
     */
    final public int getNSpecies() {
        return getConfiguration().getNSpecies();
    }

    final public IGrid getGrid() {
        return getOsmose().getGrid();
    }

}
