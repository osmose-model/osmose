/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
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
import fr.ird.osmose.util.timeseries.SingleTimeSeries;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

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

        String keyval = String.format("fisheries.seasonality.fsh%d", this.fisheryIndex);      
        String keyfile = String.format("fisheries.seasonality.file.fsh%d", this.fisheryIndex);
        
        if(this.getConfiguration().isNull(keyval) && this.getConfiguration().isNull(keyfile)) {
            String msg = String.format("%s or %s must be defined.", keyval, keyfile);
            error(msg, new IOException());
        }
        
        if (!this.getConfiguration().isNull(keyval)) {

            // Read the season array
            double[] seasonTmp = this.getConfiguration().getArrayDouble(keyval);

            // Checks that the array has the same size as the season duration
            if (seasonTmp.length < seasonDuration) {
                String error = String.format("Seasonality array length should have a minimum size "
                        + "of season duration (%d). %d provided", seasonDuration, seasonTmp.length);
                error(error, new IOException());
            }

            // Fills the final seasonality array
            for (int i = 0; i < nStep; i++) {
                int k = (i - ioff) % seasonDuration;
                k = (k < 0) ? -k : k;
                this.seasonality[i] = seasonTmp[k];
            }

        } else {
            SingleTimeSeries sts = new SingleTimeSeries();
            String filename = getConfiguration().getFile(keyfile);
            // Seasonality must be at least one year, and at max the length of the simulation
            sts.read(filename);
            seasonality = sts.getValues();
        }

        // Normalizes between 0 and ioff (corresponding to F0)
        // Only if ioff different from 0.
        if (ioff != 0) {
            this.checkNorm(0, ioff);
        }

        // Then normalizes for all the given seasons.
        for (int i = ioff; i < nStep; i += seasonDuration) {
            this.checkNorm(i, i + seasonDuration);
        }
    }

    /**
     * Normalize the seasonality array between two given time-steps.
     *
     * @param istart First time step
     * @param iend Last time step
     */
    public void checkNorm(int istart, int iend) {

        double total = 0.d;

        iend = Math.min(iend, this.getConfiguration().getNStep());
        istart = Math.min(istart, this.getConfiguration().getNStep());

        for (int i = istart; i < iend; i++) {
            total += this.seasonality[i];
        }
        
        BigDecimal bd = new BigDecimal(total).setScale(3, RoundingMode.HALF_EVEN);
        total = bd.doubleValue();

        if (total != 1.d) {
            String msg = String.format("Fishery %d: the seasonality for steps %d to %d summed to %f.\n"
                    + "Should sum to 1. Please verify that seasonality is properly set.", this.fisheryIndex, istart, iend, total);
            StringBuilder stb = new StringBuilder();
            stb.append("\n@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\n");
            stb.append(msg);
            stb.append("\n@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
            warning(stb.toString());

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
