/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.background;

import fr.ird.osmose.process.AbstractProcess;
import fr.ird.osmose.process.bioen.PhysicalData;

/**
 *
 * @author nbarrier
 */
public class GrossEnergy extends AbstractProcess {
    
        /** Variables used to compute f02 function. */
    private double tmin, tmax, topt;
    private double a, b, c;
    private double ap, bp, cp;  // a prime, b prime, cprime
    
    PhysicalData temperature_input;
    
    public GrossEnergy(int rank) { 
        
        super(rank);   
        temperature_input = new PhysicalData(rank, "temperature");
        
    }
    
    @Override
    public void init() {
        
        String key;
        
        key = "bioen.gross.energy.tmin";
        tmin = getConfiguration().getDouble(key);
        
        key = "bioen.gross.energy.tmax";
        tmax = getConfiguration().getDouble(key);
        
        key = "bioen.gross.energy.topt";
        topt = getConfiguration().getDouble(key);
        
        this.compute_abc();
        this.compute_abc_prime();
        
    }
    
    public void compute_abc() {
        this.b = 2 / (topt * (tmin / topt - 1));
        this.a = -this.b / (2 * topt);
        this.c = 1 - this.b / 2.d * topt;

    }

    public void compute_abc_prime() {
        this.bp = 2 / (topt * (tmax / topt - 1));
        this.ap = -this.bp / (2 * topt);
        this.cp = 1 - this.bp / 2.d * topt;

    }

    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public double get_phiT(double temp) {
        
        if((temp <= tmin) || (temp  > tmax)) {
            return 0;
        }

        double output = (temp <= topt) ? this.a * Math.pow(temp, 2) + this.b * Math.pow(temp, 1) + this.c : this.ap * Math.pow(temp, 2) + this.bp * Math.pow(temp, 1) + this.cp;
        return output;
        
    }
    
    
}
