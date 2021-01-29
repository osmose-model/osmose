/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine Ecosystems)
 * http://www.osmose-model.org
 * 
 * Copyright (C) IRD (Institut de Recherche pour le Développement) 2009-2020
 * 
 * Osmose is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, additional and
 * starvation mortalities, reproduction and migration) and fishing mortalities
 * (Shin and Cury 2001, 2004).
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License). Full description
 * is provided on the LICENSE file.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * 
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

    private double[] E, A, Tpk;

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

        int cpt;
        int nSpecies = this.getNSpecies();

        E = new double[nSpecies];
        A = new double[nSpecies];
        Tpk = new double[nSpecies];
        c_t = new double[nSpecies];
        Tr = new double[nSpecies];

        String key;
        key = "bioen.AMR.E";
        cpt = 0;
        for (int i : getConfiguration().getFocalIndex()) {
            String keytmp = String.format("%s.sp%d", key, i);
            E[cpt] = getConfiguration().getDouble(keytmp);
            cpt++;
        }

        key = "bioen.AMR.A";
        cpt = 0;
        for (int i : getConfiguration().getFocalIndex()) {
            String keytmp = String.format("%s.sp%d", key, i);
            A[cpt] = getConfiguration().getDouble(keytmp);
            cpt++;
        }

        key = "bioen.AMR.TpK";
        cpt = 0;
        for (int i : getConfiguration().getFocalIndex()) {
            String keytmp = String.format("%s.sp%d", key, i);
            Tpk[cpt] = getConfiguration().getDouble(keytmp);
            cpt++;
        }

        key = "bioen.arrh.ct";
        cpt = 0;
        for (int i : getConfiguration().getFocalIndex()) {
            String keytmp = String.format("%s.sp%d", key, i);
            c_t[cpt] = getConfiguration().getDouble(keytmp);
            cpt++;
        }

        key = "bioen.maint.energy.Tr";
        cpt = 0;
        for (int i : getConfiguration().getFocalIndex()) {
            String keytmp = String.format("%s.sp%d", key, i);
            Tr[cpt] = getConfiguration().getDouble(keytmp);
            cpt++;
        }

    }

    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); // To change body of generated methods, choose
                                                                       // Tools | Templates.
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
        double k = 8.62e-5;

        double output = Math.exp((-this.E[i] / (k * (temp + 273.15))))
                / (1 + Math.exp((-1 / (k * (temp + 273.15))) * (this.A[i] + this.E[i]
                        - ((this.A[i] + this.E[i]) / this.Tpk[i] + k * Math.log(this.E[i] / (this.A[i])))
                                * (temp + 273.15))))
                / (Math.exp(-this.E[i] / (k * this.Tpk[i])) / (1 + Math.exp((-1 / (k * this.Tpk[i])) * (this.A[i]
                        + this.E[i] - ((this.A[i] + this.E[i]) / this.Tpk[i] + k * Math.log(this.E[i] / (this.A[i])))
                                * this.Tpk[i]))));

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

        return Math.exp(this.c_t[i] * (1 / this.Tr[i] - 1 / (temp + 273.15)));

    }

}
