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
public class OxygenFunction extends AbstractProcess {
    
    /** Variables used to compute f02 function. */
    private double [] o2_crit;
    private double c1, c2;

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
        
        // Initialisation of an array of O2 crit values (one
        // per focal species)
        o2_crit = new double[this.getNSpecies()];
        for(int i=0; i<this.getNSpecies(); i++) {
            key = String.format("species.o2_crit.sp%d", i);
            o2_crit[i] = getConfiguration().getDouble(key); 
        }
        
        // Recovering the values of C1 and C2 used for fO2 function
        key = "bioen.fo2.c1";
        c1 = getConfiguration().getDouble(key);

        key = "bioen.fo2.c2";
        c2 = getConfiguration().getDouble(key);
        
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
        double spec_o2crit = o2_crit[school.getSpecies().getIndex()];
        
        // computation of the
        double o2 = o2_input.getValue(k, school.getCell());
        double output = (o2 <= spec_o2crit) ? 0 : (c1 * (o2 - spec_o2crit) / ((o2 - spec_o2crit) + c2));
        
        return output;
    }   
}
