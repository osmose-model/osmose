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
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
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
import java.util.HashMap;

/**
 * Class that handles the ingestion in the Bioenergetic model
 *
 * @author nbarrier
 */
public class TempFunction extends AbstractProcess {

    private HashMap<Integer, Double> km, gamma;

    /**
     * Parameters for the energy maintenance.
     */
    private HashMap<Integer, Double> c_t, Tr;

    PhysicalData temperature_input;

    public TempFunction(int rank) throws IOException {

        super(rank);

        // Initialisation of the O2 input as read from the NetCDF file.
        temperature_input = new PhysicalData(rank, "temperature");
        temperature_input.init();

    }

    @Override
    public void init() {

        km = new HashMap();
        gamma = new HashMap();
        c_t = new HashMap();
        Tr =new HashMap();

        String key;

        key = "bioen.gross.energy.km";
        for (int i : getConfiguration().getFocalIndex()) {
            String keytmp = String.format("%s.sp%d", key, i);
            km.put(i, getConfiguration().getDouble(keytmp));
        }

        key = "bioen.gross.energy.gamma";
        for (int i : getConfiguration().getFocalIndex()) {
            String keytmp = String.format("%s.sp%d", key, i);
            gamma.put(i, getConfiguration().getDouble(keytmp));
        }

        key = "bioen.arrh.ct";
        for (int i : getConfiguration().getFocalIndex()) {
            String keytmp = String.format("%s.sp%d", key, i);
            c_t.put(i, getConfiguration().getDouble(keytmp));
        }

        key = "bioen.maint.energy.Tr";
        for (int i : getConfiguration().getFocalIndex()) {
            String keytmp = String.format("%s.sp%d", key, i);
            Tr.put(i, getConfiguration().getDouble(keytmp));
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

        double output = (temp - this.gamma.get(i)) / (temp - this.gamma.get(i) + this.km.get(i));
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

        return Math.exp(this.c_t.get(i) * (1 / this.Tr.get(i) - 1 / (temp + 273.15)));

    }

}
