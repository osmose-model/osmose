/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process.bioen;

import fr.ird.osmose.School;
import fr.ird.osmose.process.AbstractProcess;
import fr.ird.osmose.process.mortality.MortalityCause;

/**
 *
 * @author nbarrier
 */
public class BioenMortality extends AbstractProcess  {

    private double k_dam;

    public BioenMortality(int rank) {
        super(rank);
    }

    @Override
    public void init() {

        String key = "bioen.damage.k_dam";
        k_dam = getConfiguration().getDouble(key);
    
    }

    public double compute_oxydative_mort(School school) {
        // calculation of PhiT
        return this.k_dam * school.getEGross();
    }
    
    public void compute_starv_mort(School school) {
        
        double enet = school.getENet();
   
        if (enet > 0) {
            // If Enet > 0, maintenance needs have been paid, no starvation mortality
            return;
        }
        
        enet = Math.abs(enet);

        // if enet < 0, the absolute value is what should be added to maintenance
        if (school.getGonadWeight() >= enet) {

            // if the gonad weight is enough to fill the remaining maintenance cost,
            // no starvation mortality but removing of a quantity enet of gonads
            school.incrementGonadWeight((float) -enet);

        }
        
        else {
            
            // if gonad weight is not enough to pay for maintenance needs:
            
            // all the gonad weight goes to fill part of the maintenance needs
            enet -= school.getGonadWeight();   // part of the enet deficit is filled by gonad weight
            school.incrementGonadWeight(-school.getGonadWeight());   // all gonad weight is removed.
            
            // Computes the number of dead individuals
            double ndead = Math.abs(enet) * school.getWeight() / school.getInstantaneousBiomass();
            
            // set the number of dead individuals
            school.setNdead(MortalityCause.STARVATION, ndead);
                
        }
                
    }

    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
