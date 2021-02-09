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
import java.io.IOException;

/**
 *
 * @author Nicolas
 */
public class FisheryPeriod extends OsmoseLinker {
    
    private final int fileFisheryIndex;
    private final double[] fisheryPeriod;
    private int nSeasons;
    private double seasonOffset; 
    
    
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
        key = String.format("fisheries.season.number.fsh%d", this.fileFisheryIndex);
        this.nSeasons = this.getConfiguration().getInt(key);
        
        // Init the season offset (in fraction of years)
        key = String.format("fisheries.season.start.fsh%d", this.fileFisheryIndex);
        this.seasonOffset = this.getConfiguration().getDouble(key);
        
        // Gets the season offset in number of time steps
        int ioff = (int) (seasonOffset * nStepYear);
        
        // Recovers the season duration (in number of time steps    )
        int seasonDuration = nStepYear / this.nSeasons;
        
        // 0 if no offset, else 1
        int do_offset = (ioff != 0) ? 1 : 0;
        
        int[] fishIndex = new int[this.getConfiguration().getNStep()];

        // If ioff = 0, nothing is done.
        for (int i = 0; i < ioff; i++) {
            fishIndex[i] = 0;
        }
        
        for (int i = ioff; i < nStep; i++) {
            int k = (i - ioff) / (seasonDuration);
            fishIndex[i] = k + do_offset;
        }

        key = String.format("fisheries.rate.byPeriod.fsh%d", this.fileFisheryIndex);
        
        double[] fishingSeason = this.getConfiguration().getArrayDouble(key);
        if (fishingSeason.length == 1) {
            
            // If fishing season given as a single value, then
            // use it for all the season.
            for (int i = 0; i < nStep; i++) {
                fisheryPeriod[i] = fishingSeason[0];
            }

        } else {

            // Check that the length of the fishing season is ok.
            if (fishingSeason.length - 1 < fishIndex[nStep - 1]) {
                String msg = String.format("The %s parameter must have at least %d values. %d provided", key, fishIndex[nStep - 1] + 1, fishingSeason.length);
                error(msg, new IOException());
            }

            for (int i = 0; i < nStep; i++) {
                int k = fishIndex[i];
                fisheryPeriod[i] = fishingSeason[k];
            }
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
