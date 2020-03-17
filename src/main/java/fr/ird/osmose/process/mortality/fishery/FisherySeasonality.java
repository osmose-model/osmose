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
import fr.ird.osmose.util.timeseries.SingleTimeSeries;
import java.io.IOException;

/**
 *
 * @author Nicolas
 */
public class FisherySeasonality extends OsmoseLinker {

    private final int fisheryIndex;
    private double[] seasonality;

    public FisherySeasonality(int fisheryIndex) {

        this.fisheryIndex = fisheryIndex;
        this.seasonality = new double[this.getConfiguration().getNStep()];

    }

    public void init() {

        // Recovers the time-step per year and total number of time steps
        int nStepYear = this.getConfiguration().getNStepYear();
        int nStep = this.getConfiguration().getNStep();

        String key;

        // Init the number of seasons;
        key = String.format("fisheries.season.number.fsh%d", this.fisheryIndex);
        int nSeasons = this.getConfiguration().getInt(key);

        // Init the season offset (in fraction of years)
        key = String.format("fisheries.season.start.fsh%d", this.fisheryIndex);
        double seasonOffset = this.getConfiguration().getDouble(key);

        // Season offset in time steps
        int ioff = (int) (seasonOffset * nStepYear);

        // Season duration in time steps
        int seasonDuration = nStepYear / nSeasons;

        // If a seaonaly vector exitsts
        key = String.format("fisheries.seasonality.fsh%d", this.fisheryIndex);
        if (this.getConfiguration().canFind(key)) {

            // Read the season array
            double[] seasonTmp = this.getConfiguration().getArrayDouble(key);

            // Checks that the array has the same size as the season duration
            if (seasonTmp.length != seasonDuration) {
                error("Seasonality array should have the same size as season duration", new IOException());
            }

            // Fills the final seasonality array
            for (int i = 0; i < nStep; i++) {
                int k = (i - ioff) % seasonDuration;
                this.seasonality[i] = seasonTmp[k];
            }

        } else {
            key = String.format("fisheries.seasonality.file.fsh%d", this.fisheryIndex);
            SingleTimeSeries sts = new SingleTimeSeries();
            String filename = getConfiguration().getFile(key);
            // Seasonality must be at least one year, and at max the length of the simulation
            sts.read(filename);
            seasonality = sts.getValues();
        }

        // Normalizes between 0 and ioff (corresponding to F0)
        this.norm(0, ioff);

        // Then normalizes for all the given seasons.
        for (int i = ioff; i < nStep; i += seasonDuration) {
            this.norm(ioff, ioff + seasonDuration);
        }
    }

    /**
     * Normalize the seasonality array between two given time-steps.
     *
     * @param istart First time step
     * @param iend Last time step
     */
    public void norm(int istart, int iend) {

        double total = 0.d;
        
        iend = Math.min(iend, this.getConfiguration().getNStep());
        istart = Math.min(istart, this.getConfiguration().getNStep());
        
        for (int i = istart; i < iend; i++) {
            total += this.seasonality[i];
        }
        
        if (total != 1.d) {
            for (int i = istart; i < iend; i++) {
                this.seasonality[i] = (total == 0) ? 1 / (iend - istart) : (this.seasonality[i] / total);
            }
        }

    }
    
    /**
     * Returns the fishing seasonality mortality for a given time step.
     *
     * @param idt Time step
     * @return Fishing mortality
     */
    public double getSeasonalityFishMort(int idt) {
        return this.seasonality[idt];
    }

}

