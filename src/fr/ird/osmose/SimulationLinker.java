/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose;

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
    
    public Osmose getOsmose() {
        return Osmose.getInstance();
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
        return Osmose.getInstance().resolveFile(filename);
    }
    /**
     * The number of simulated species
     *
     * @return the number of simulated species
     */
    public int getNSpecies() {
        return Osmose.getInstance().getNumberSpecies();
    }
}
