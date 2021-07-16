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

    /**
     * Parameters for the energy mobilization.
     */

    private double[] e_M, e_D, Tp;

    /**
     * Parameters for the energy maintenance.
     */
    private double[] e_m;

    PhysicalData temperature_input;

    private boolean isPhiTActivated;

    /** Interface for the computation of PhiT */
    @FunctionalInterface
    private interface LambdaPhiT {
        double getPhiT(School school);
    }

    LambdaPhiT lambdaPhiT;

    public TempFunction(int rank) throws IOException {

        super(rank);

        // Initialisation of the O2 input as read from the NetCDF file.
        temperature_input = new PhysicalData(rank, "temperature");
        temperature_input.init();

    }

    @Override
    public void init() {

        int cpt;
        String key;
        int nSpecies = this.getNSpecies();

        e_M = new double[nSpecies];
        e_D = new double[nSpecies];
        Tp = new double[nSpecies];
        e_m = new double[nSpecies];

        // Check whether the phiT calculation is activated or not.
        // Default is trur
        key = "simulation.bioen.phit.enabled";
        if (getConfiguration().isNull(key)) {
            this.isPhiTActivated = true;
        } else {
            this.isPhiTActivated = getConfiguration().getBoolean(key);
        }

        // If PhiT is on, we use the computePhiT function
        // if PhiT is off, returns 1.0
        if (this.isPhiTActivated) {
            lambdaPhiT = this::computePhiT;
        } else {
            lambdaPhiT = (school) -> 1.0;
        }

        key = "species.bioen.mobilized.e.mobi";
        cpt = 0;
        for (int i : getConfiguration().getFocalIndex()) {
            String keytmp = String.format("%s.sp%d", key, i);
            e_M[cpt] = getConfiguration().getDouble(keytmp);
            cpt++;
        }

        key = "species.bioen.mobilized.e.D";
        cpt = 0;
        for (int i : getConfiguration().getFocalIndex()) {
            String keytmp = String.format("%s.sp%d", key, i);
            e_D[cpt] = getConfiguration().getDouble(keytmp);
            cpt++;
        }

        key = "species.bioen.mobilized.Tp";
        cpt = 0;
        for (int i : getConfiguration().getFocalIndex()) {
            String keytmp = String.format("%s.sp%d", key, i);
            Tp[cpt] = getConfiguration().getDouble(keytmp);
            cpt++;
        }

        cpt = 0;
        key = "species.bioen.maint.e.maint";
        for (int i : getConfiguration().getFocalIndex()) {
            String keytmp = String.format("%s.sp%d", key, i);
            e_m[cpt] = getConfiguration().getDouble(keytmp);
            cpt++;
        }

    }

    @Override
    public void run() {
        this.temperature_input.update(this.getSimulation().getIndexTimeSimu());
    }

    /**
     * Computes the phiT coefficients function for mobilized energy. Equation 4
     */
    /**
     * Returns the temperature for a given school.
     */
    public double getTemp(School school) {
        return temperature_input.getValue(school);

    }

    /**
     * Returns the phiT function for mobilized energy. Equation 4
     *
     * @param school
     * @return
     */
    public double computePhiT(School school) {

        // Recovers the temperature of the school cell
        double temp = this.getTemp(school);
        int i = school.getSpeciesIndex();
        double k = 8.62e-5;

        double output = Math.exp(-this.e_M[i] / (k * (temp + 273.15)))
                / (1 + (this.e_M[i] / (this.e_D[i] - this.e_M[i]))
                        * Math.exp((this.e_D[i]) / k * (1 / (this.Tp[i] + 273.15) - 1 / (temp + 273.15))))
                / (Math.exp(-this.e_M[i] / (k * (this.Tp[i] + 273.15)))
                        / (1 + this.e_M[i] / (this.e_D[i] - this.e_M[i])));

        return output;

    }

    /**
     * Returns the Arrhenius function for a given school for maintenance. Cf.
     * equation 6
     *
     * @param school
     * @return
     */
    public double get_Arrhenius(School school) {

        // Recovers the temperature of the school cell
        // Autre formulation de Arrhénius : la plus récente des deux
        double temp = this.getTemp(school);
        int i = school.getSpeciesIndex();
        double k = 8.62e-5;

        return Math.exp(-this.e_m[i] / (k * (temp + 273.15)));

    }

    public double getPhiT(School school) {
        return this.lambdaPhiT.getPhiT(school);
    }

}
