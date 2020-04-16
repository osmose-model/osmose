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

    private double[] km,gamma;


    /**
     * Parameters for the energy maintenance.
     */
    private double[] c_t, Tr;

    PhysicalData temperature_input;

    public TempFunction(int rank) throws IOException {

        super(rank);

        // Initialisation of the O2 input as read from the NetCDF file.
        temperature_input = new PhysicalData(rank, "temperature");
        temperature_input.init();

    }

    @Override
    public void init() {

        km = new double[this.getNSpecies()];
        gamma = new double[this.getNSpecies()];
        c_t = new double[this.getNSpecies()];
        Tr = new double[this.getNSpecies()];

        String key;

        key = "bioen.gross.energy.km";
        for (int i = 0; i < this.getNSpecies(); i++) {
            String keytmp = String.format("%s.sp%d", key, i);
            km[i] = getConfiguration().getDouble(keytmp);
        }

        key = "bioen.gross.energy.gamma";
        for (int i = 0; i < this.getNSpecies(); i++) {
            String keytmp = String.format("%s.sp%d", key, i);
            gamma[i] = getConfiguration().getDouble(keytmp);
        }

        
        key = "bioen.arrh.ct";
        for (int i = 0; i < this.getNSpecies(); i++) {
            String keytmp = String.format("%s.sp%d", key, i);
            c_t[i] = getConfiguration().getDouble(keytmp);
        }

        key = "bioen.maint.energy.Tr";
        for (int i = 0; i < this.getNSpecies(); i++) {
            String keytmp = String.format("%s.sp%d", key, i);
            Tr[i] = getConfiguration().getDouble(keytmp);
        }


    }

    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Computes the phiT coefficients function for gross energy. Equation 4
     */
    
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

        double output = (temp-this.gamma[i])/(temp-this.gamma[i]+this.km[i]);
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

        return Math.exp(this.c_t[i] * (1 / (temp + 273.15) - 1 / this.Tr[i]));

    }

}
