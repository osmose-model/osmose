/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
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
import fr.ird.osmose.process.mortality.AbstractMortality;

/**
 *
 * @author nbarrier
 */
public class BioenStarvationMortality extends AbstractMortality {

    public BioenStarvationMortality(int rank) {
        super(rank);
    }

    @Override
    public void init() {
        // nothing to do
        // Bioen starvation mortality does not directly rely on user-defined parameters
    }

    public double computeStarvation(School school, int subdt) {

        if (school.getENet() >= 0) {
            // If Enet > 0, maintenance needs have been paid, no starvation mortality
            return 0.d;
        }

        // dead individuals (zero by default)
        double ndead = 0.d;

        // fraction of ENet deficit at current sub time step (turned into positive value)
        double eNetSubDt = Math.abs(school.getENet()) / subdt;

        // check whether ENet deficit can be compensated with gonadic energy
        if (school.getGonadWeight() >= eNetSubDt) {
            // 1. enough gonadic energy
            // pay maintenance with gonadic energy and decrease gonadic energy accordingly
            school.incrementGonadWeight((float) -eNetSubDt);
            school.incrementEnet(eNetSubDt);
        } else {
            // 2. not enough gonadic energy
            // flush gonadic energy
            school.incrementGonadWeight(-school.getGonadWeight());
            // partially repay ENet with available gonadic energy
            school.incrementEnet(school.getGonadWeight());
            // starvation occurs, as a fraction of energy deficit
            double deathToll = eNetSubDt - school.getGonadWeight();
            ndead = deathToll / school.getWeight();
        }

        // return number of dead fish in school at current sub time step
        return ndead;
    }

    @Override
    public double getRate(School school) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
