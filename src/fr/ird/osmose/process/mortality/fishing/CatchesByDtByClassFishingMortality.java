/*
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, natural and
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
     * Size thresholds in centimeter. Size stage k means null
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
        super(rank, species);
    }

    @Override
    public void init() {
        int iSpec = getIndexSpecies();
        if (!getConfiguration().isNull("mortality.fishing.catches.byDt.byAge.file.sp" + iSpec)) {
            ByClassTimeSeries timeSerieByAge = new ByClassTimeSeries();
            timeSerieByAge.read(getConfiguration().getFile("mortality.fishing.catches.byDt.byAge.file.sp" + iSpec));
            catches = timeSerieByAge.getValues();
            ageClasses = new float[timeSerieByAge.getNClass() - 1];
            for (int k = 0; k < ageClasses.length; k++) {
                // Converts age in year into number of time steps
                ageClasses[k] = Math.round(timeSerieByAge.getClass(k) * getConfiguration().getNStepYear());
            }
        } else if (!getConfiguration().isNull("mortality.fishing.catches.byDt.bySize.file.sp" + iSpec)) {
            ByClassTimeSeries timeSerieBySize = new ByClassTimeSeries();
            timeSerieBySize.read(getConfiguration().getFile("mortality.fishing.catches.byDt.bySize.file.sp" + iSpec));
            catches = timeSerieBySize.getValues();
            sizeClasses = timeSerieBySize.getClasses();
        } else {
            error("Could not found parameters mortality.fishing.catches.byDt.byAge/bySize.file.sp" + iSpec, null);
        }
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
    public void assessFishableBiomass() {

        // reset fishable biomass
        for (int i = 0; i < fishableBiomass.length; i++) {
            fishableBiomass[i] = 0.d;
        }
        int k = -1;
        for (School school : getSchoolSet().getSchools(getSpecies(), false)) {
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
}
