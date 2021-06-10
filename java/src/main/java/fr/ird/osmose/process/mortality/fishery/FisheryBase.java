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

package fr.ird.osmose.process.mortality.fishery;

import fr.ird.osmose.util.OsmoseLinker;
import fr.ird.osmose.util.timeseries.ByRegimeTimeSeries;

/**
 *
 *
 * @author nbarrier
 */
public class FisheryBase extends OsmoseLinker {

    int fisheryIndex;
    public double[] fisheryBase;

    /**
     * Public constructor.Initialize the FisheryMortality pointer.
     *
     * @param fisheryIndex
     */
    public FisheryBase(int fisheryIndex) {
        this.fisheryIndex = fisheryIndex;
        fisheryBase = new double[getConfiguration().getNStep()];
    }

    /**
     * Initialize the time varying index.
     */
    public void init() {
            
        // If a fishing shift exists, take it to extract the fishing values
        String keyShift = String.format("fisheries.rate.base.shift.fsh%d", this.fisheryIndex);
        String keyVal = String.format("fisheries.rate.base.fsh%d", this.fisheryIndex);
        
        boolean useLog10 = getConfiguration().getBoolean("fisheries.rate.base.log.enabled.fsh" + this.fisheryIndex);

        ByRegimeTimeSeries ts = new ByRegimeTimeSeries(keyShift, keyVal);
        ts.init();

        fisheryBase = ts.getValues();
        if (useLog10) {
            for (int i = 0; i < fisheryBase.length; i++) {
                if (fisheryBase[i] > 0) {
                    String message = String.format("Fishing base mortality rate exponent for fishery %d is positive", this.fisheryIndex);
                    error(message, new IllegalArgumentException());
                }
                fisheryBase[i] = Math.exp(fisheryBase[i]);
            }
        }

    } // end of init method

    /**
     * Returns the value of the fbase array at a given time step.
     *
     * @param idt Time step
     * @return
     */
    public double getFisheryBase(int idt) {
        return this.fisheryBase[idt];
    }

} // end of class
