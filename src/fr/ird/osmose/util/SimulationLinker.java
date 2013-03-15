/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.util;

import fr.ird.osmose.Configuration;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.SchoolSet;
import fr.ird.osmose.Simulation;
import fr.ird.osmose.Species;
import fr.ird.osmose.grid.IGrid;
import fr.ird.osmose.ltl.LTLForcing;
import java.util.logging.Logger;

/**
 *
 * @author pverley
 */
public class SimulationLinker {

    private final int indexSimulation;

    public SimulationLinker(int indexSimulation) {
        this.indexSimulation = indexSimulation;
    }

    final public int getIndexSimulation() {
        return indexSimulation;
    }

    public Configuration getConfiguration() {
        return Osmose.getInstance().getConfiguration();
    }

    public IGrid getGrid() {
        return Osmose.getInstance().getGrid();
    }

    public Simulation getSimulation() {
        return Osmose.getInstance().getSimulation(indexSimulation);
    }

    public SchoolSet getPopulation() {
        return Osmose.getInstance().getSimulation(indexSimulation).getSchoolSet();
    }

    public LTLForcing getForcing() {
        return Osmose.getInstance().getForcing();
    }

    public Species getSpecies(int index) {
        return Osmose.getInstance().getSimulation(indexSimulation).getSpecies(index);
    }

    public Species getSpecies(String name) {
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
    public int getNSpecies() {
        return Osmose.getInstance().getConfiguration().getNSpecies();
    }

    public Logger getLogger() {
        return getSimulation().getLogger();
    }
}
