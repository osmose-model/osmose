/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process;

/**
 *
 * @author pverley
 */
public class ReproductionProcess extends AbstractProcess {

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

        int nSpecies = getConfiguration().getNSpecies();
        reproductionProcess = new AbstractProcess[nSpecies];
        for (int i = 0; i < nSpecies; i++) {
            if (getConfiguration().reproduceLocally[i]) {
                reproductionProcess[i] = new LocalReproductionProcess(getReplica(), getSpecies(i));
            } else {
                reproductionProcess[i] = new IncomingFluxProcess(getReplica(), getSpecies(i));
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
}
