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
public class TempFunction extends AbstractProcess {

    private double a, b, c;
    private double ap, bp, cp;  // a prime, b prime, cprime    
    private double tmin, tmax, topt;

    /**
     * Parameters for the energy maintenance.
     */
    private double c_t1, c_t2;

    PhysicalData temperature_input;

    public TempFunction(int rank) {

        super(rank);

        // Initialisation of the O2 input as read from the NetCDF file.
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
        

        this.compute_abc();

    }

    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Computes the phiT coefficients function for gross energy. Equation 4
     */
    public void compute_abc() {
        this.b = 2 / (topt * (tmin / topt - 1));
        this.a = -this.b / (2 * topt);
        this.c = 1 - this.b / 2.d * topt;

        this.bp = 2 / (topt * (tmax / topt - 1));
        this.ap = -this.bp / (2 * topt);
        this.cp = 1 - this.bp / 2.d * topt;

    }

    /**
     * Returns the temperature for a given school.
     */
    public double getTemp(School school) {

        return temperature_input.getValue(school);

    }

    /**
     * Returns the phiT function for gross energy. Equation 4
     *
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
    
    /**
     * Returns the Arrhenius function for a given school. Cf. equation 6
     *
     * @param school
     * @return
     */
    public double get_Arrhenius(School school) {

        // Recovers the temperature of the school cell
        double temp = this.getTemp(school);
        return Math.exp(this.c_t1 - (this.c_t2 / temp));

    }
    
    
}
