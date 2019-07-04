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
public class TempFunction extends AbstractProcess {

    private double[] a, b, c;
    private double[] ap, bp, cp;  // a prime, b prime, cprime    
    private double[] tmin, tmax, topt;

    /**
     * Parameters for the energy maintenance.
     */
    private double[] c_t2, Tr;

    PhysicalData temperature_input;

    public TempFunction(int rank) throws IOException {

        super(rank);

        // Initialisation of the O2 input as read from the NetCDF file.
        temperature_input = new PhysicalData(rank, "temperature");
        temperature_input.init();

    }

    @Override
    public void init() {

        tmin = new double[this.getNSpecies()];
        tmax = new double[this.getNSpecies()];
        topt = new double[this.getNSpecies()];
        a = new double[this.getNSpecies()];
        b = new double[this.getNSpecies()];
        c = new double[this.getNSpecies()];
        ap = new double[this.getNSpecies()];
        bp = new double[this.getNSpecies()];
        cp = new double[this.getNSpecies()];
        c_t2 = new double[this.getNSpecies()];
        Tr = new double[this.getNSpecies()];

        String key;

        key = "bioen.gross.energy.tmin";
        for (int i = 0; i < this.getNSpecies(); i++) {
            String keytmp = String.format("%s.sp%d", key, i);
            tmin[i] = getConfiguration().getDouble(keytmp);
        }

        key = "bioen.gross.energy.tmax";
        for (int i = 0; i < this.getNSpecies(); i++) {
            String keytmp = String.format("%s.sp%d", key, i);
            tmax[i] = getConfiguration().getDouble(keytmp);
        }

        key = "bioen.gross.energy.topt";
        for (int i = 0; i < this.getNSpecies(); i++) {
            String keytmp = String.format("%s.sp%d", key, i);
            topt[i] = getConfiguration().getDouble(keytmp);
        }

        key = "bioen.arrh.ct1";
        for (int i = 0; i < this.getNSpecies(); i++) {
            String keytmp = String.format("%s.sp%d", key, i);
        }

        key = "bioen.arrh.ct2";
        for (int i = 0; i < this.getNSpecies(); i++) {
            String keytmp = String.format("%s.sp%d", key, i);
            c_t2[i] = getConfiguration().getDouble(keytmp);
        }

        key = "bioen.maint.energy.Tr";
        for (int i = 0; i < this.getNSpecies(); i++) {
            String keytmp = String.format("%s.sp%d", key, i);
            Tr[i] = getConfiguration().getDouble(keytmp);
        }

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

        for (int i = 0; i < this.getNSpecies(); i++) {
            this.b[i] = 2 / (topt[i] * Math.pow(tmin[i] / topt[i] - 1, 2));
            this.a[i] = -this.b[i] / (2 * topt[i]);
            this.c[i] = 1 - (this.b[i] / 2.d) * topt[i];

            this.bp[i] = 2 / (topt[i] * Math.pow(1 - tmax[i] / topt[i], 2));
            this.ap[i] = -this.bp[i] / (2 * topt[i]);
            this.cp[i] = 1 - (this.bp[i] / 2.d) * topt[i];
        }

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
        int i = school.getSpeciesIndex();

        if ((temp < tmin[i]) || (temp > tmax[i])) {
            return 0;
        }

        double output = (temp <= topt[i]) ? this.a[i] * Math.pow(temp, 2) + this.b[i] * Math.pow(temp, 1) + this.c[i] : this.ap[i] * Math.pow(temp, 2) + this.bp[i] * Math.pow(temp, 1) + this.cp[i];
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
        // Autre formulation de Arrhénius : la plus récente des deux 
        double temp = this.getTemp(school);
        int i = school.getSpeciesIndex();

        return Math.exp(this.c_t2[i] * (1 / (temp+273) - 1 / this.Tr[i]));

    }

}
