/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process.bioen;

import fr.ird.osmose.School;
import fr.ird.osmose.process.AbstractProcess;

/**
 *
 * @author nbarrier
 */
public class BioenStarvationMortality extends AbstractProcess {

    

    public BioenStarvationMortality(int rank) {
        super(rank);
    }

    @Override
    public void init() {
        // nothing to do
        // Bioen starvation mortality does not directly rely on user-defined parameters
    }

   public double computeStarvation(School school, int subdt) {

        if (school.getENet() >= 0) {
            // If Enet > 0, maintenance needs have been paid, no starvation mortality
            return 0.d;
        }
        
        // dead individuals (zero by default)
        double ndead = 0.d;
        
        // fraction of ENet deficit at current sub time step (turned into positive value)
        double eNetSubDt = Math.abs(school.getENet()) / subdt;
        
        // check whether ENet deficit can be compensated with gonadic energy
        if (school.getGonadWeight() >= eNetSubDt) {
            // 1. enough gonadic energy
            // pay maintenance with gonadic energy and decrease gonadic energy accordingly
            school.incrementGonadWeight((float) -eNetSubDt);
            school.incrementEnet(eNetSubDt); 
        } else {
            // 2. not enough gonadic energy
            // flush gonadic energy
            school.incrementGonadWeight(-school.getGonadWeight());
            // partially repay ENet with available gonadic energy
            school.incrementEnet(school.getGonadWeight());
            // starvation occurs, as a fraction of energy deficit
            double deathToll = eNetSubDt - school.getGonadWeight();
            ndead = deathToll / school.getWeight();
        }
        
        // return number of dead fish in school at current sub time step
        return ndead;
    }

    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
