/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process.bioen;

import fr.ird.osmose.School;
import fr.ird.osmose.process.AbstractProcess;
import fr.ird.osmose.process.bioen.PhysicalData;

/**
 *
 * @author nbarrier
 */
public class EnergyBudget extends AbstractProcess {
    
    /**
     * Parameters used to compute energy gross function.
     */
    private double tmin, tmax, topt;
    private double a, b, c;
    private double ap, bp, cp;  // a prime, b prime, cprime

    private double[] alpha;
    private double csmr;
    
    private double m0, m1;
    
    
    /**
     * Parameters for the energy maintenance.
     */
    private double c_t1, c_t2;

    PhysicalData temperature_input;

    public EnergyBudget(int rank) {

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
 
        key = "bioen.arrh.ct1";
        c_t1 = getConfiguration().getDouble(key);
        
        key = "bioen.arrh.ct2";
        c_t2 = getConfiguration().getDouble(key);
        
        key = "bioen.maint.energy.csmr";
        csmr = getConfiguration().getDouble(key);
        
        key = "bioen.maturity.m0";
        m0 = getConfiguration().getDouble(key);

        key = "bioen.maturity.m1";
        m1 = getConfiguration().getDouble(key);
        
        
        this.compute_abc();
        
        // Redundant with the alpha of the BioenPredationMortality class.
        int nBack = this.getNBkgSpecies();
        int nspec = this.getNSpecies();
        
        // Recovers the alpha coefficient for focal + background species
        alpha = new double[nspec + nBack];
        for (int i = 0; i < this.getNSpecies(); i++) {
            key = String.format("species.alpha.sp%d", i);
            alpha[i] = this.getConfiguration().getDouble(key);
        }

        for (int i = 0; i < nBack; i++) {
            key = String.format("species.alpha.bkg%d", i);
            alpha[i + nspec] = this.getConfiguration().getDouble(key);
        }   
        
    }
    
     /** Computes the phiT coefficients function for gross energy.
     * Equation 4
     * @param school
     * @return 
     */
    public void compute_abc() {
        this.b = 2 / (topt * (tmin / topt - 1));
        this.a = -this.b / (2 * topt);
        this.c = 1 - this.b / 2.d * topt;

        this.bp = 2 / (topt * (tmax / topt - 1));
        this.ap = -this.bp / (2 * topt);
        this.cp = 1 - this.bp / 2.d * topt;

    }

    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /** Returns the phiT function for gross energy.
     * Equation 4
     * @param school
     * @return 
     */
    public double get_phiT(School school) {
                
        // Recovers the temperature of the school cell
        double temp = temperature_input.getValue(school);
        
        if ((temp <= tmin) || (temp > tmax)) {
            return 0;
        }
        
        double output = (temp <= topt) ? this.a * Math.pow(temp, 2) + this.b * Math.pow(temp, 1) + this.c : this.ap * Math.pow(temp, 2) + this.bp * Math.pow(temp, 1) + this.cp;
        return output;
        
    }
    
    /** Returns the Arrhenius function for a given school.
     * Cf. equation 6
     * 
     * @param school
     * @return 
     */
    public double get_Arrhenius(School school) { 
        
        // Recovers the temperature of the school cell
        double temp = temperature_input.getValue(school);
        return Math.exp(this.c_t1 - (this.c_t2 / temp));
        
    }
    
    /** Computes the maintenance coefficient. 
     * Equation 5
     * @param school
     * @return 
     */
    public double get_maintenance(School school) {
        
        int ispec = school.getSpeciesIndex();
        double output = this.csmr * Math.pow(school.getBiomass(), alpha[ispec]) * this.get_Arrhenius(school);
        return output;
                
    }
    
    public double get_egross(School school) { 
        return school.getIngestion() * this.get_phiT(school);
    }
    
    public double get_enet(School school) { 
        return this.get_egross(school) - this.get_maintenance(school);
    }
               
    public int get_maturation(School school) {
        
        double age = school.getAge();  // returns the age in years
        double length = school.getLength();     // warning: length in cm.
        double llim = this.m0 * age + this.m1;   // computation of a maturity
        
        int output = (length >= llim) ? 1 : 0;
        return output;
        
    }
    
}
