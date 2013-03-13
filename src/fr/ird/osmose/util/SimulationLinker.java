/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.util;

import fr.ird.osmose.Configuration;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.Population;
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

    private final int replica;

    public SimulationLinker(int replica) {
        this.replica = replica;
    }

    final public int getReplica() {
        return replica;
    }

    public Configuration getConfiguration() {
        return Osmose.getInstance().getConfiguration();
    }

    public IGrid getGrid() {
        return Osmose.getInstance().getGrid();
    }

    public Simulation getSimulation() {
        return Osmose.getInstance().getSimulation(replica);
    }

    public Population getPopulation() {
        return Osmose.getInstance().getSimulation(replica).getPopulation();
    }

    public LTLForcing getForcing() {
        return Osmose.getInstance().getForcing();
    }

    public Species getSpecies(int index) {
        return Osmose.getInstance().getSimulation(replica).getSpecies(index);
    }

    public Species getSpecies(String name) {
        for (int i = 0; i < getNSpecies(); i++) {
            if (getSpecies(i).getName().equalsIgnoreCase(name)) {
                return getSpecies(i);
            }
        }
        return null;
    }

    public String resolveFile(String filename) {
        return Osmose.getInstance().getConfiguration().resolveFile(filename);
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
