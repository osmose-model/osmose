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
import fr.ird.osmose.util.timeseries.GenericTimeSeries;

import java.io.IOException;
import java.util.List;

/**
 *
 * @author Nicolas
 */
public class FisheryPeriod extends OsmoseLinker {
    
    private final int fileFisheryIndex;
    private final double[] fisheryPeriod;
    private int nPeriods;
    private double periodOffset; 
    
    
    public FisheryPeriod(int fisheryIndex) {

        this.fileFisheryIndex = fisheryIndex;
        this.fisheryPeriod = new double[this.getConfiguration().getNStep()];
        
    }
    
    public void init() { 
        
        String key;
        
        // Number of time-steps per year.
        int nStepYear = this.getConfiguration().getNStepYear();

        // Number of time steps
        int nStep = this.getConfiguration().getNStep();
                
        // Init the number of seasons;
        key = String.format("fisheries.period.number.fsh%d", this.fileFisheryIndex);
        this.nPeriods = this.getConfiguration().getInt(key);
        
        // Init the season offset (in fraction of years)
        key = String.format("fisheries.period.start.fsh%d", this.fileFisheryIndex);
        this.periodOffset = this.getConfiguration().getDouble(key);
        
        // Gets the season offset in number of time steps
        int ioff = (int) (periodOffset * nStepYear);
        
        // Recovers the season duration (in number of time steps    )
        int seasonDuration = nStepYear / this.nPeriods;
        
        // 0 if no offset, else 1
        int do_offset = (ioff != 0) ? 1 : 0;
        
        // time index of the fishery time step for fperiod
        int[] fishIndex = new int[this.getConfiguration().getNStep()];

        // If ioff = 0, nothing is done.
        for (int i = 0; i < ioff; i++) {
            fishIndex[i] = 0;
        }
        
        for (int i = ioff; i < nStep; i++) {
            int k = (i - ioff) / (seasonDuration);
            fishIndex[i] = k + do_offset;
        }
        
        // List the parameters for byPeriod for the current fishery.
        List<String> keysList = getConfiguration().findKeys("fisheries.rate.byperiod.*.fsh" + this.fileFisheryIndex);
        if(keysList.size() != 1) { 
            String message = String.format("Fishery %d must contains only 1 parameter related to fishery rates by period. Currently %d provided.\n", this.fileFisheryIndex, keysList.size());   
            error(message, new Exception());
        }
        
        key = keysList.get(0);
        boolean useLog10 = key.contains(".log");
        boolean useFile = key.contains(".file");
                
        double[] fishingSeason;
        if (useFile) {
            GenericTimeSeries ts = new GenericTimeSeries();
            String fileName = getConfiguration().getFile(key);
            ts.read(fileName);
            fishingSeason = ts.getValues();
        } else {
            fishingSeason = this.getConfiguration().getArrayDouble(key);
        }
        
        if(useLog10) { 
            for (int i = 0; i < fishingSeason.length; i++) {
                if(fishingSeason[i] > 0) {
                    String message = String.format("Fishing period mortality rate exponent for fishery %d is positive", this.fileFisheryIndex);
                    error(message, new IllegalArgumentException());
                }
                fishingSeason[i] = Math.exp(fishingSeason[i]);
            }
        }
        
        if (fishingSeason.length == 1) {
            // If fishing season given as a single value, then
            // use it for all the season.
            for (int i = 0; i < nStep; i++) {
                fisheryPeriod[i] = fishingSeason[0];
            }

        } else if (fishingSeason.length == this.nPeriods) {
            // In this case, values are provided for one year and N periods
            for (int i = 0; i < nStep; i++) {
                // k is the index of the previous time step. it has been shifted by nperiods to prevent negatve index at the beginning
                int k = (fishIndex[i] + do_offset * this.nPeriods - do_offset) % this.nPeriods;
                fisheryPeriod[i] = fishingSeason[k];
            }
        } else if (fishingSeason.length - 1 == fishIndex[nStep - 1]) {
            // In this case, values are provided for all years and all periods
            for (int i = 0; i < nStep; i++) {
                int k = fishIndex[i];
                fisheryPeriod[i] = fishingSeason[k];
            }
        } else {
            String msg = String.format("The fishing period rates for fsh%d must have at least 1, %d or %d values. %d provided", this.fileFisheryIndex,
                    this.nPeriods, fishIndex[nStep - 1] + 1, fishingSeason.length);
            error(msg, new IOException());
        }
    }
    
    /** Returns the seasonal fishing mortality for a given time step. 
     * 
     * @param idt Time step
     * @return Fishing mortality
     */
    public double getFisheryPeriod(int idt) { 
        return fisheryPeriod[idt];
    }
    
}
