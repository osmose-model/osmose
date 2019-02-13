/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process.bioen;

import fr.ird.osmose.School;
import fr.ird.osmose.process.AbstractProcess;

/** 
 * Class that handles the ingestion in the Bioenergetic model
 *
 * @author nbarrier
 */
public class BioenIngestion extends AbstractProcess {
    
    /** Variables used to compute f02 function. */
    private double [] o2_crit;
    private double c1, c2;
    
    PhysicalData o2_input;
    
    public BioenIngestion(int rank) { 
        
        super(rank);
        
        // Initialisation of the O2 input as read from the NetCDF file.
        o2_input = new PhysicalData(rank, "oxygen");
           
    }

    @Override
    public void init() {
        String key;
        
        // Initialisation of an array of O2 crit values (one
        // per focal species)
        o2_crit = new double[this.getNSpecies()];
        for(int i=0; i<this.getNSpecies(); i++) {
            key = String.format("species.o2_crit.sp%d", i);
            o2_crit[i] = getConfiguration().getDouble(key); 
        }
        
        // Recovering the values of C1 and C2 used for fO2 function
        key = "bioen.fo2.c1";
        c1 = getConfiguration().getDouble(key);

        key = "bioen.fo2.c2";
        c2 = getConfiguration().getDouble(key);
        
    }

    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /** Computes the Oxygen function used in the ingestion formulae (equation 2).
     * 
     * @param school School whose ingestion is computed.
     * @return 
     */
    public double compute_fO2(School school) {
        
        int k = school.getSpecies().getDepthLayer();
        double spec_o2crit = o2_crit[school.getSpecies().getIndex()];
        
        // computation of the
        double o2 = o2_input.getValue(k, school.getCell());
        double output = (o2 <= spec_o2crit) ? 0 : (c1 * (o2 - spec_o2crit) / ((o2 - spec_o2crit) + c2));
        
        return output;
    }
    
}
