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
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0 2013/09/01
 */
public class CatchesByDtByClassFishingMortality extends AbstractFishingMortality {

    /**
     * Catches by time step and by age/size class.
     */
    private double[][] catches;
    /**
     * Size thresholds in centimetre. Size stage k means null
     * {@code threshold[k] <= age < threshold[k+1]}
     */
    private float[] sizeClasses;
    /**
     * Age thresholds in time steps. It is provided in year in the input file
     * and converted in the {@code init} function in number of time steps. Age
     * stage k means {@code threshold[k] <= ageDt < threshold[k+1]}
     */
    private float[] ageClasses;
    /**
     * Fishable biomass, in tonne, per age/size class.
     */
    private double[] fishableBiomass;

    public CatchesByDtByClassFishingMortality(int rank, Species species) {
        super(rank, species, FishingMortality.Type.CATCHES);
    }

    @Override
    public void readParameters() {
        int iSpec = getFileSpeciesIndex();
        int nClasses = 0;
        if (!getConfiguration().isNull("mortality.fishing.catches.byDt.byAge.file.sp" + iSpec)) {
            ByClassTimeSeries timeSerieByAge = new ByClassTimeSeries();
            timeSerieByAge.read(getConfiguration().getFile("mortality.fishing.catches.byDt.byAge.file.sp" + iSpec));
            catches = timeSerieByAge.getValues();
            ageClasses = new float[timeSerieByAge.getNClass()];
            for (int k = 0; k < ageClasses.length; k++) {
                // Converts age in year into number of time steps
                ageClasses[k] = Math.round(timeSerieByAge.getClass(k) * getConfiguration().getNStepYear());
            }
            nClasses = ageClasses.length;
        } else if (!getConfiguration().isNull("mortality.fishing.catches.byDt.bySize.file.sp" + iSpec)) {
            ByClassTimeSeries timeSerieBySize = new ByClassTimeSeries();
            timeSerieBySize.read(getConfiguration().getFile("mortality.fishing.catches.byDt.bySize.file.sp" + iSpec));
            catches = timeSerieBySize.getValues();
            sizeClasses = timeSerieBySize.getClasses();
            nClasses = ageClasses.length;
        } else {
            error("Could not found parameters mortality.fishing.catches.byDt.byAge/bySize.file.sp" + iSpec, null);
        }
        fishableBiomass = new double[nClasses];
    }

    @Override
    public double getRate(School school) {
        throw new UnsupportedOperationException("No fishing mortality rates specified in this fishing scenario.");
    }

    @Override
    public double getCatches(School school) {
        int k = -1;
        if (null != sizeClasses) {
            // By size class
            k = indexOf(school.getLength(), sizeClasses);
        } else if (null != ageClasses) {
            // By age class
            k = indexOf(school.getAgeDt(), ageClasses);
        }
        if ((k >= 0) && (fishableBiomass[k] > 0.d)) {
            return (school.getInstantaneousBiomass() / fishableBiomass[k])
                    * catches[getSimulation().getIndexTimeSimu()][k];
        } else {
            return 0.d;
        }
    }

    private int indexOf(float value, float[] classes) {
        // 1. value < first threshold, index does not exist
        if (value < classes[0]) {
            return -1;
        }
        // 2. Normal case thresold[k] <= value < threshold[k+1]
        for (int k = 0; k < classes.length - 1; k++) {
            if ((classes[k] <= value) && (value < classes[k + 1])) {
                return k;
            }
        }
        // 3. value >= threshold[last]
        return classes.length - 1;
    }

    @Override
    public void resetFishableBiomass() {
        for (int i = 0; i < fishableBiomass.length; i++) {
            fishableBiomass[i] = 0.d;
        }
    }

    /*
     * Increment the fishable biomass, in tonne, of the species.
     */
    @Override
    public void incrementFishableBiomass(School school) {
        int k = -1;
        if (null != sizeClasses) {
            // By size class
            k = indexOf(school.getLength(), sizeClasses);
        } else if (null != ageClasses) {
            // By age class
            k = indexOf(school.getAgeDt(), ageClasses);
        }
        if (k >= 0) {
            fishableBiomass[k] += school.getInstantaneousBiomass();
        }
    }
}
