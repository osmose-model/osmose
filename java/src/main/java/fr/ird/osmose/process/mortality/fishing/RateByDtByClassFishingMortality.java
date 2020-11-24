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
import fr.ird.osmose.util.timeseries.ByClassTimeSeries;

/**
 *
 * @author pverley
 */
public class RateByDtByClassFishingMortality extends AbstractFishingMortality {

    /**
     * Fishing mortality rates by time step and by age/size class.
     */
    private double[][] f;
    /**
     * Size thresholds in centimetre. Size stage k means null     {@code threshold[k] <= age < threshold[k+1]}
     */
    private float[] sizeThreshold;
    /**
     * Age thresholds in time steps. It is provided in year in the input file
     * and converted in the {@code init} function in number of time steps. Age
     * stage k means {@code threshold[k] <= ageDt < threshold[k+1]}
     */
    private int[] ageThreshold;

    public RateByDtByClassFishingMortality(int rank, Species species) {
        super(rank, species, FishingMortality.Type.RATE);
    }

    @Override
    public void readParameters() {
        int iSpec = getFileSpeciesIndex();
        if (!getConfiguration().isNull("mortality.fishing.rate.byDt.byAge.file.sp" + iSpec)) {
            ByClassTimeSeries timeSerieByAge = new ByClassTimeSeries();
            timeSerieByAge.read(getConfiguration().getFile("mortality.fishing.rate.byDt.byAge.file.sp" + iSpec));
            f = timeSerieByAge.getValues();
            ageThreshold = new int[timeSerieByAge.getNClass() - 1];
            for (int k = 0; k < ageThreshold.length; k++) {
                // Converts age in year into number of time steps
                ageThreshold[k] = (int) Math.round(timeSerieByAge.getClass(k) * getConfiguration().getNStepYear());
            }
        } else if (!getConfiguration().isNull("mortality.fishing.rate.byDt.bySize.file.sp" + iSpec)) {
            ByClassTimeSeries timeSerieBySize = new ByClassTimeSeries();
            timeSerieBySize.read(getConfiguration().getFile("mortality.fishing.rate.byDt.bySize.file.sp" + iSpec));
            f = timeSerieBySize.getValues();
            sizeThreshold = timeSerieBySize.getClasses();
        } else {
            error("Could not found parameters mortality.fishing.rate.byDt.byAge/bySize.file.sp" + iSpec, null);
        }
    }

    @Override
    public double getRate(School school) {
        if (null != sizeThreshold) {
            // By size class
            float length = school.getLength();
            // 1. Length < value of the first size threshold, it means there is
            // no value provided. Osmose assume it is zero.
            if (length < sizeThreshold[0]) {
                return 0.f;
            }
            // 2. Normal case thresold[k] <= length < threshold[k+1]
            for (int k = 0; k < sizeThreshold.length - 1; k++) {
                if ((sizeThreshold[k] <= length) && (length < sizeThreshold[k + 1])) {
                    return f[getSimulation().getIndexTimeSimu()][k];
                }
            }
            // 3. length >= threshold[last]
            return f[getSimulation().getIndexTimeSimu()][sizeThreshold.length - 1];
        } else if (null != ageThreshold) {
            // By age class
            float age = school.getAgeDt();
            // 1. age < threshold[0] it means there is no value provided.
            // Osmose assume it is zero.
            if (age < ageThreshold[0]) {
                return 0.f;
            }
            // 2. Normal case thresold[k] <= age < threshold[k+1]
            for (int k = 0; k < ageThreshold.length - 1; k++) {
                if ((ageThreshold[k] <= age) && (age < ageThreshold[k + 1])) {
                    return f[getSimulation().getIndexTimeSimu()][k];
                }
            }
            // 3. age >= threshold[last]
            return f[getSimulation().getIndexTimeSimu()][ageThreshold.length - 1];
        }
        // We should never reach that stage. If we do it is because there is
        // something wrong in the thresholds and then we return a NaN value.
        return Float.NaN;
    }
    
    @Override
    public double getCatches(School school) {
        throw new UnsupportedOperationException("No catches specified in this fishing scenario.");
    }
}
