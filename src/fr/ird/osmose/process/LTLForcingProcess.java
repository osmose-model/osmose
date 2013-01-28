/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process;

import fr.ird.osmose.ltl.LTLForcing;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pverley
 */
public class LTLForcingProcess extends AbstractProcess {

    @Override
    public void init() {
        try {
            getSimulation().setForcing((LTLForcing) Class.forName(getOsmose().getLTLClassName()).newInstance());
        } catch (IllegalAccessException ex) {
            Logger.getLogger(LTLForcingProcess.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(LTLForcingProcess.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(LTLForcingProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
        getForcing().readLTLConfigFile1(getOsmose().planktonStructureFileNameTab[getOsmose().numSerie]);
        getForcing().readLTLConfigFile2(getOsmose().planktonFileNameTab[getOsmose().numSerie]);
        getForcing().initPlanktonMap();
    }

    @Override
    public void run() {
        getForcing().updatePlankton(getSimulation().getIndexTimeYear());
    }
}
