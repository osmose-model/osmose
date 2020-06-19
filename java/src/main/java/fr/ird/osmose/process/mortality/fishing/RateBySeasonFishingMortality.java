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
package fr.ird.osmose.process.mortality.fishing;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import fr.ird.osmose.process.mortality.FishingMortality;
import fr.ird.osmose.util.timeseries.SingleTimeSeries;

/**
 *
 * @author pverley
 */
public class RateBySeasonFishingMortality extends AbstractFishingMortality {

    private double annualF;
    private double[] season;

    public RateBySeasonFishingMortality(int rank, Species species) {
        super(rank, species, FishingMortality.Type.RATE);
    }

    @Override
    public void readParameters() {
        int nStepYear = getConfiguration().getNStepYear();
        int iSpec = getIndexSpecies();
        annualF = getConfiguration().getDouble("mortality.fishing.rate.sp" + iSpec);

        // Fishing seasonality
        if (!getConfiguration().isNull("mortality.fishing.season.distrib.file.sp" + iSpec)) {
            // Read seasonality from CSV file
            SingleTimeSeries sts = new SingleTimeSeries();
            String filename = getConfiguration().getFile("mortality.fishing.season.distrib.file.sp" + iSpec);
            // Seasonality must be at least one year, and at max the length of the simulation
            sts.read(filename);
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
                ? annualF * season[getSimulation().getIndexTimeYear()]
                : 0.d;
    }

    @Override
    public double getCatches(School school) {
        throw new UnsupportedOperationException("No catches specified in this fishing scenario.");
    }
}
