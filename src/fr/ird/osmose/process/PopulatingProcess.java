/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process;

import fr.ird.osmose.populator.AbstractPopulator;
import fr.ird.osmose.populator.BiomassPopulator;
import fr.ird.osmose.populator.SpectrumPopulator;

/**
 *
 * @author pverley
 */
public class PopulatingProcess extends AbstractProcess {

    private AbstractPopulator populator;

    public PopulatingProcess(int replica) {
        super(replica);
    }

    @Override
    public void init() {

        String method = getConfiguration().getString("population.initialization.method");
        if (method.equalsIgnoreCase("biomass")) {
            populator = new BiomassPopulator(getReplica());
        } else if (method.equalsIgnoreCase("spectrum")) {
            populator = new SpectrumPopulator(getReplica());
        } else if (method.equalsIgnoreCase("random")) {
            throw new UnsupportedOperationException("Random initialization not supported yet.");
        }
    }

    @Override
    public void run() {
        populator.populate();
    }
}
