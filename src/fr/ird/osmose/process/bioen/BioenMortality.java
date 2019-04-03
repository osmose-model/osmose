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
    
    public void compute_starv_mort(School school, int subdt) {
        
        double enet = school.getENet();
   
        if (enet >= 0) {
            // If Enet > 0, maintenance needs have been paid, no starvation mortality
            return;
        }
        
        // Here, enet is converted into positive values
        enet = Math.abs(enet);
        
        // if enet < 0, the absolute value is what should be added to maintenance
        if (school.getGonadWeight() >= enet) {

            // if the gonad weight is enough to fill the remaining maintenance cost,
            // no starvation mortality but removing of a quantity enet of gonads
            school.incrementGonadWeight((float) -enet / subdt);  // barrier.n: division by subdt: reduction of gonad weight
            school.incrementEnet(enet / subdt);   // reduction in Enet deficit: Enet > 0 (cf row 47) and should equal 0 at the end of the mortality time step
            
        }
        
        else {
            
            // if gonad weight is not enough to pay for maintenance needs.
            // enet_extract = fraction of enet that is subtracted at the current time
            // step.
            double enet_extract = Math.min(enet / subdt, school.getGonadWeight());   // maintenance to pay on the current time-step
            
            // if, at the current time step, the paying of the maintenance cost is
            // filled by gonadic weight, just decrease the gonad weight and the enet deficit
            if (enet_extract == enet / subdt) {
                school.incrementGonadWeight((float) -enet / subdt);  // barrier.n: division by subdt: reduction of gonad weight
                school.incrementEnet(enet / subdt);   // reduction in Enet deficit: Enet > 0 (cf row 47) and should equal 0 at the end of the mortality time step
            } else {
                // if the extraction of e_net is less than expected, there is mortality
                // at the current time step
                double diff = enet / subdt - enet_extract;
                school.incrementEnet(enet_extract);   // fills what can be paid last by the few remaining gonads       
                school.incrementGonadWeight((float) -enet_extract);  // gonad weight should be 0 here.

                // Computes the number of dead individuals
                double ndead = Math.abs(diff) / (school.getWeight());

                // set the number of dead individuals
                school.setNdead(MortalityCause.STARVATION, ndead);
                                                 
            }                           
        }   
    }

    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
