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
    
    public double compute_starv_mort(School school) {
        
        double output = 0;
        double enet = school.getENet();
        
        if(school.getGonadWeight() >= -enet) {
            output = 0;
        }
        
        else {
            
            // Calculation of the number of dead individuals (equation 17)
            double ndead = enet / school.getWeight();
            
            output = ndead * school.getWeight() / school.getInstantaneousBiomass();
                
        }
        
        return output;
        
    }

    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
