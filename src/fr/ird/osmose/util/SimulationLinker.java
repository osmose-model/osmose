/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.util;

import fr.ird.osmose.OldConfiguration;
import fr.ird.osmose.Osmose;
import fr.ird.osmose.Population;
import fr.ird.osmose.Simulation;
import fr.ird.osmose.Species;
import fr.ird.osmose.grid.IGrid;
import fr.ird.osmose.ltl.LTLForcing;

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
    
    public OldConfiguration getConfiguration() {
        return Osmose.getInstance().getOldConfiguration();
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
    
    public String resolveFile(String filename) {
        return Osmose.getInstance().getOldConfiguration().resolveFile(filename);
    }
    /**
     * The number of simulated species
     *
     * @return the number of simulated species
     */
    public int getNSpecies() {
        return Osmose.getInstance().getOldConfiguration().getNSpecies();
    }
}
