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

import fr.ird.osmose.util.OsmoseLinker;
import java.io.IOException;

/**
 *
 * @author Nicolas
 */
public class FisheryPeriod extends OsmoseLinker {
    
    private final int fisheryIndex;
    private final double[] fmortSeason;
    private int nSeasons;
    private double seasonOffset; 
    
    
    public FisheryPeriod(int fisheryIndex) {

        this.fisheryIndex = fisheryIndex;
        this.fmortSeason = new double[this.getConfiguration().getNStep()];
        
    }
    
    public void init() { 
        
        String key;
        
        // Number of time-steps per year.
        int nStepYear = this.getConfiguration().getNStepYear();

        // Number of time steps
        int nStep = this.getConfiguration().getNStep();
                
        // Init the number of seasons;
        key = String.format("fisheries.season.number.fsh%d", this.fisheryIndex);
        this.nSeasons = this.getConfiguration().getInt(key);
        
        // Init the season offset (in fraction of years)
        key = String.format("fisheries.season.start.fsh%d", this.fisheryIndex);
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

        key = String.format("fisheries.rate.bySeason.fsh%d", this.fisheryIndex);
        
        double[] fishingSeason = this.getConfiguration().getArrayDouble(key);
        if (fishingSeason.length == 1) {
            
            // If fishing season given as a single value, then
            // use it for all the season.
            for (int i = 0; i < nStep; i++) {
                fmortSeason[i] = fishingSeason[0];
            }

        } else {

            // Check that the length of the fishing season is ok.
            if (fishingSeason.length - 1 < fishIndex[nStep - 1]) {
                String msg = String.format("The %s parameter must have at least %d values. %d provided", key, fishIndex[nStep - 1] + 1, fishingSeason.length);
                error(msg, new IOException());
            }

            for (int i = 0; i < nStep; i++) {
                int k = fishIndex[i];
                fmortSeason[i] = fishingSeason[k];
            }
        }
        
    }
    
    /** Returns the seasonal fishing mortality for a given time step. 
     * 
     * @param idt Time step
     * @return Fishing mortality
     */
    public double getSeasonFishMort(int idt) { 
        return fmortSeason[idt];
    }
    
}
