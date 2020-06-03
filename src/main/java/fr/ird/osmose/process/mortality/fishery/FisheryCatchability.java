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
package fr.ird.osmose.process.mortality.fishery;

import fr.ird.osmose.Configuration;
import fr.ird.osmose.util.OsmoseLinker;
import fr.ird.osmose.util.timeseries.BySpeciesTimeSeries;
import java.io.IOException;

/**
 *
 * @author Nicolas Barrier (nicolas.barrier@ird.fr)
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0b 2013/09/01
 */
public class FisheryCatchability extends OsmoseLinker {

    /**
     * Accessibility (in percentage). Dimensions = [step][species]
     */
    private double[][] values;

    /**
     * Number of Fisheries.
     */
    private final int fisheryIndex;
    
    private final String prefix;
    
    public FisheryCatchability(int index, String prefix) {
        this.fisheryIndex = index;
        this.prefix = prefix;
    }
      
    public void init() throws IOException {
        
        String key;
        Configuration cfg = this.getConfiguration();
        
        key = String.format("%s.file.fsh%d", prefix, fisheryIndex);
        if(!cfg.isNull(key)) {
            BySpeciesTimeSeries ts = new BySpeciesTimeSeries();
            ts.read(cfg.getFile(key));
            values = ts.getValues();
        }
        
        else {
            key = String.format("%s.fsh%d", prefix, fisheryIndex);
            double[] array = cfg.getArrayDouble(key);
            if(array.length != cfg.getNBkgSpecies() + cfg.getNSpecies()) {
                String msg = String.format("The %s param must have %d values (nspecies + nbackgrounds). %d given",
                        key, cfg.getNBkgSpecies() + cfg.getNSpecies(), array.length);
                error(msg, new IOException());
            }
            values = new double[cfg.getNStep()][];
            for(int i = 0; i<values.length; i++) {
                values[i] = array;
            }
        }

        this.checkValues();
        
    }
    
    private void checkValues() throws IOException { 
        int nstep = values.length;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (int i = 0; i < nstep; i++) {
            int ncol = values[i].length;
            for (int j = 0; j < ncol; j++) {
                min = Math.min(min, values[i][j]);
                max = Math.min(max, values[i][j]);
            }
        }
        
        if (max > 1) {
            String error = String.format("Maximum access. for fishery %d should be 1, %f provided", this.fisheryIndex, max);
            throw new IOException(error);
        }

        if (min < 0) {
            String error = String.format("Minimum access. for fishery %d should be 0, %f provided", this.fisheryIndex, max);
            throw new IOException(error);
        }

    }
    
    /**
     * Returns the [nFishery, nSpecies] accessibility matrix.
     * @return 
     */
    public double[][] getValues() {
        return values;
    }

    /**
     * Returns the [nSpecies] accessibility matrix for a given fisheries
     *
     * @param iFishery Fishery index
     * @return 
     */
    public double[] getValues(int istep) {
        return values[istep];
    }

    /**
     * Returns the accessibility matrix for a given fisheries and a given
     * specie.
     *
     * @param iFishery Fishery index
     * @param iSpec Species index
     * @return 
     */
    public double getValues(int istep, int iSpec) {
        return values[istep][iSpec];
    }

}
