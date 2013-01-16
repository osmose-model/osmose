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

    @Override
    public void init() {

        int nSpecies = getSimulation().getNbSpecies();
        int numSerie = getOsmose().numSerie;
        reproductionProcess = new AbstractProcess[nSpecies];
        for (int i = 0; i < nSpecies; i++) {
            if (getOsmose().reproduceLocallyTab[numSerie][i]) {
                reproductionProcess[i] = new LocalReproductionProcess(getSpecies(i));
            } else {
                reproductionProcess[i] = new IncomingFluxProcess(getSpecies(i));
            }
            reproductionProcess[i].init();
        }
    }

    @Override
    public void run() {
        for (int i = 0; i < getSimulation().getNbSpecies(); i++) {
            reproductionProcess[i].run();
        }
    }
}
