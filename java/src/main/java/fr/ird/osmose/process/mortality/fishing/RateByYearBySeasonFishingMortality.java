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

package fr.ird.osmose.process.mortality.fishing;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import fr.ird.osmose.process.mortality.FishingMortality;
import fr.ird.osmose.util.timeseries.ByYearTimeSeries;
import fr.ird.osmose.util.timeseries.SingleTimeSeries;

/**
 *
 * @author pverley
 */
public class RateByYearBySeasonFishingMortality extends AbstractFishingMortality {

    private double[] annualF;
    private double[] season;

    public RateByYearBySeasonFishingMortality(int rank, Species species) {
        super(rank, species, FishingMortality.Type.RATE);
    }

    @Override
    public void readParameters() {
        int nStepYear = getConfiguration().getNStepYear();
        int iSpec = getFileSpeciesIndex();

        String filename;
        ByYearTimeSeries yts;
        // Read annual F by year
        filename = getConfiguration().getFile("mortality.fishing.rate.byYear.file.sp" + iSpec);
        yts = new ByYearTimeSeries();
        yts.read(filename);
        annualF = yts.getValues();

        // Fishing seasonality
        if (!getConfiguration().isNull("mortality.fishing.season.distrib.file.sp" + iSpec)) {
            // Read seasonality from CSV file
            SingleTimeSeries sts = new SingleTimeSeries();
            filename = getConfiguration().getFile("mortality.fishing.season.distrib.file.sp" + iSpec);
            // Seasonality must be exactly of one year
            sts.read(filename, nStepYear, nStepYear);
            season = sts.getValues();
        } else {
            // Uniform fishing throughout the year
            season = new double[nStepYear];
            for (int iTime = 0; iTime < season.length; iTime++) {
                season[iTime] = 1.d / nStepYear;
            }
        }
    }

    @Override
    public double getRate(School school) {
        return isFishable(school)
                ? annualF[getSimulation().getYear()] * season[getSimulation().getIndexTimeYear()]
                : 0.d;
    }

    @Override
    public double getCatches(School school) {
        throw new UnsupportedOperationException("No catches specified in this fishing scenario.");
    }
}
