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
public class OxygenFunction extends AbstractProcess {
    
    /** Variables used to compute f02 function. */
    private double[] c1, c2;

    PhysicalData o2_input;
    
    public OxygenFunction(int rank) throws IOException { 
        
        super(rank);
        
        // Initialisation of the O2 input as read from the NetCDF file.
        o2_input = new PhysicalData(rank, "oxygen");
        o2_input.init();
           
    }

    @Override
    public void init() {
        String key;
        
        // Recovering the values of C1 and C2 used for fO2 function (one
        // per focal species)
        int nSpecies = this.getNSpecies();
        c1 = new double[nSpecies];
        c2 = new double[nSpecies];
        int cpt = 0;
        for(int i : getConfiguration().getFocalIndex()) {
            key = String.format("species.c1.sp%d", i);
            c1[cpt] = getConfiguration().getDouble(key); 
            
            key = String.format("species.c2.sp%d", i);
            c2[cpt] = getConfiguration().getDouble(key); 
            cpt++;
        }       
    }

    @Override
    public void run() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /** Computes the Oxygen function used in the ingestion formulae (equation 2).
     * 
     * @param school School whose ingestion is computed.
     * @return 
     */
    public double compute_fO2(School school) {
        
        int k = school.getSpecies().getDepthLayer();
        int iSpecies = school.getSpeciesIndex();
        // computation of the
        double o2 = o2_input.getValue(k, school.getCell());
        double output = c1[iSpecies] * o2/ (o2 + c2[iSpecies]);
        
        return output;
    }   
}
