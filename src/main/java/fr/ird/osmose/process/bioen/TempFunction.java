/* 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, natural and
 * starvation mortalities, reproduction and migration) and fishing mortalities
 * (Shin and Cury 2001, 2004).
 * 
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
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

        return Math.exp(this.c_t2[i] * (1 / (temp + 273.15) - 1 / this.Tr[i]));

    }

}
