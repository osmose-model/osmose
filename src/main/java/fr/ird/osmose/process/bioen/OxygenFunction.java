/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process.bioen;

import fr.ird.osmose.School;
import fr.ird.osmose.process.AbstractProcess;
import java.io.IOException;

/** 
 * Class that handles the ingestion in the Bioenergetic model
 *
 * @author nbarrier
 */
public class OxygenFunction extends AbstractProcess {
    
    /** Variables used to compute f02 function. */
    private double []  c1, c2;

    PhysicalData o2_input;
    
    public OxygenFunction(int rank) throws IOException { 
        
        super(rank);
        
        // Initialisation of the O2 input as read from the NetCDF file.
        o2_input = new PhysicalData(rank, "oxygen");
        o2_input.init();
           
    }

    @Override
    public void init() {
        String key;
        
        // Recovering the values of C1 and C2 used for fO2 function (one
        // per focal species)
        c1 = new double[this.getNSpecies()];
        c2 = new double[this.getNSpecies()];
        for(int i=0; i<this.getNSpecies(); i++) {
            key = String.format("species.c1.sp%d", i);
            c1[i] = getConfiguration().getDouble(key); 
            
            key = String.format("species.c2.sp%d", i);
            c2[i] = getConfiguration().getDouble(key); 
        }       
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
        // computation of the
        double o2 = o2_input.getValue(k, school.getCell());
        double output = c1[school.getSpecies().getIndex()] * o2/ (o2 + c2[school.getSpecies().getIndex()]);
        
        return output;
    }   
}
