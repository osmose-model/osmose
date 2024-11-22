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
 * Nicolas BARRIER (nicolas.barrier@ird.fr)
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

package fr.ird.osmose.util.timeseries;

import fr.ird.osmose.util.OsmoseLinker;
import java.io.IOException;

/**
 *
 *
 * @author roliveros
 */
public class ForcingTimeSeries extends OsmoseLinker {

    private double[] values;
    private final String keyNstepY;
    private final String keyVal;
    private int freq;
    private double[] tmp;

    /**
     * Public constructor.Initialize the FisheryMortality pointer.
     *
     * @param keyNstepY Key for nsteps.year value
     * @param keyVal Key for data values
     */
    public ForcingTimeSeries(String keyNstepY, String keyVal) {
        this.keyNstepY = keyNstepY;
        this.keyVal = keyVal;
        values = new double[getConfiguration().getNStep()];
    }

    /**
     * Initialize the time varying index.
     */
    public void init() {

        int nStep = this.getConfiguration().getNStep();
        int nStepYear = this.getConfiguration().getNStepYear();
        int nYear = nStep/nStepYear;
        
        if (this.getConfiguration().isNull(keyNstepY)) {
            String warn = String.format("The %s argument was not found. Osmose assumes is 1.", keyNstepY);
            warning(warn);
            freq = 1;
        } else {
            freq = getConfiguration().getInt(this.keyNstepY);
        }
        
        // q is how many times a value needs to be repeated
        int q = nStepYear / freq;
        
        // Get the forcing values.
        double[] forcing = getConfiguration().getArrayDouble(this.keyVal);
        int need = nYear * freq;
        int have = forcing.length;
        tmp = new double[need];
        
        if(have < need) {
          debug("Values in {0} only contains {1} out of {2}. Osmose will loop over it.", new Object[]{this.keyVal, have, need});
        }
        
        if(need < have) {
          debug("Values in {0} contains {1} out of {2}. Osmose will ignore the exceeding values.", new Object[]{this.keyVal, have, need});
        }
        
        // read needed values, fill if necessary
        for (int i = 0; i < need; i++) {
          int k = i % have;
            tmp[i] = forcing[k];
        }
        
        //fill values
        int j = 0;
        for (int i = 0; i < need; i++) {
          for(int k=0; k < q; k++) {
            values[j] = tmp[i];
            j++;
          }
        }
        
    } // end of init method

    /**
     * Returns the value of the fbase array at a given time step.
     *
     * @param idt Time step
     * @return
     */
    public double[] getValues() {
        return this.values;
    }

} // end of class
