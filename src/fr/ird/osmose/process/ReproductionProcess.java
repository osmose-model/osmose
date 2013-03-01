/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process;

import fr.ird.osmose.Species;

/**
 *
 * @author pverley
 */
public class ReproductionProcess extends AbstractProcess {
    
    private float[][] seasonSpawning;

    public enum ReproductionType {
        /*
         * Species reproduce locally.
         * @see LocalReproductionProcess.java
         */

        LOCAL,
        /*
         * Species reproduce outside the simulated domain and we model it as
         * an incoming flux of biomass.
         * @see IncomingFluxProcess.java
         */
        FLUX;
    }
    /*
     * Reproduction processes for every Species
     */
    private AbstractProcess[] reproductionProcess;
    
    public ReproductionProcess(int replica) {
        super(replica);
    }

    @Override
    public void init() {

        seasonSpawning = getConfiguration().seasonSpawning;
        
        int nSpecies = getConfiguration().getNSpecies();
        reproductionProcess = new AbstractProcess[nSpecies];
        for (int i = 0; i < nSpecies; i++) {
            if (getConfiguration().reproduceLocally[i]) {
                reproductionProcess[i] = new LocalReproductionProcess(this, getReplica(), getSpecies(i));
            } else {
                reproductionProcess[i] = new IncomingFluxProcess(this, getReplica(), getSpecies(i));
            }
            reproductionProcess[i].init();
        }
    }

    @Override
    public void run() {
        for (int i = 0; i < getConfiguration().getNSpecies(); i++) {
            reproductionProcess[i].run();
        }
    }
    
    double getSeason(int iStepSimu, Species species) {
        int iSpec = species.getIndex();
        int iStep = seasonSpawning[iSpec].length > getConfiguration().getNumberTimeStepsPerYear()
                ? iStepSimu
                : getSimulation().getIndexTimeYear();
        return seasonSpawning[iSpec][iStep];
    }
}
