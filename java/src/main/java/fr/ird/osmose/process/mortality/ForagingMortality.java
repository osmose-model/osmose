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
package fr.ird.osmose.process.mortality;

import fr.ird.osmose.School;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pverley
 */
public class ForagingMortality extends AbstractMortality {

    private double[] k1_for; // parameter for foraging mortality with evo on 
    private double[] k2_for; // parameter for foraging mortality with evo on 
    private double[] k_for; // parameter for foraging mortality with evo off 
    private double[] I_max;

    public ForagingMortality(int rank) {
        super(rank);
    }

    @Override
    public void init() {
        int nspec = this.getNSpecies();
        k1_for = new double[nspec];
        k2_for = new double[nspec];
        k_for = new double[nspec];
        I_max = new double[nspec];
        int cpt = 0;
        for (int i : getFocalIndex()) {
            
            if (this.getConfiguration().isGeneticEnabled()) {
            k1_for[cpt] = getConfiguration().getDouble("species.bioen.forage.k1_for.sp" + i);
            k2_for[cpt] = getConfiguration().getDouble("species.bioen.forage.k2_for.sp" + i);
        } else {
             k_for[cpt] = getConfiguration().getDouble("species.bioen.forage.k_for.sp" + i);
        }
        I_max[cpt] = getConfiguration().getDouble("predation.ingestion.rate.max.bioen.sp" + i);
            cpt++;
        }
    }

    @Override
    public double getRate(School school) {

        double output = 0;
        int nstepYear = this.getConfiguration().getNStepYear();
        
        if (this.getConfiguration().isGeneticEnabled()) {
            String key = "imax";

            try {
//                output = school.getTrait(key) * this.k_for[school.getSpeciesIndex()] / nstepYear;
                output = this.k1_for[school.getSpeciesIndex()] * Math.exp(this.k2_for[school.getSpeciesIndex()]*(school.getTrait(key) - I_max[school.getSpeciesIndex()])) / nstepYear;
            } catch (Exception ex) {
                Logger.getLogger(ForagingMortality.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            output = this.k_for[school.getSpeciesIndex()] / nstepYear;
        }
        if (output < 0) {
            output = 0;
        }
        return output;
    }

}
