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
package fr.ird.osmose.process.fishing;

import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import fr.ird.osmose.process.AbstractMortalityScenario;
import fr.ird.osmose.util.timeseries.ByClassTimeSeries;

/**
 *
 * @author pverley
 */
public class ByDtByAgeSizeScenario extends AbstractMortalityScenario {

    /**
     * Fishing mortality rates by time step and by age/size class.
     */
    private float[][] f;
    /**
     * Size thresholds in centimeter. Size stage k means null     {@code threshold[k] <= age < threshold[k+1]}
     */
    private float[] sizeThreshold;
    /**
     * Age thresholds in time steps. It is provided in year in the input file
     * and converted in the {@code init} function in number of time steps. Age
     * stage k means {@code threshold[k] <= ageDt < threshold[k+1]}
     */
    private int[] ageThreshold;

    public ByDtByAgeSizeScenario(int rank, Species species) {
        super(rank, species);
    }

    @Override
    public void init() {
        int iSpec = getIndexSpecies();
        if (!getConfiguration().isNull("mortality.fishing.rate.byDt.byAge.file.sp" + iSpec)) {
            ByClassTimeSeries timeSerieByAge = new ByClassTimeSeries(getRank());
            timeSerieByAge.read(getConfiguration().getFile("mortality.fishing.rate.byDt.byAge.file.sp" + iSpec));
            f = timeSerieByAge.getValues();
            ageThreshold = new int[timeSerieByAge.getNClass() - 1];
            for (int k = 0; k < ageThreshold.length; k++) {
                // Converts age in year into number of time steps
                ageThreshold[k] = (int) Math.round(timeSerieByAge.getThreshold(k) * getConfiguration().getNStepYear());
            }
        } else if (!getConfiguration().isNull("mortality.fishing.rate.byDt.bySize.file.sp" + iSpec)) {
            ByClassTimeSeries timeSerieBySize = new ByClassTimeSeries(getRank());
            timeSerieBySize.read(getConfiguration().getFile("mortality.fishing.rate.byDt.bySize.file.sp" + iSpec));
            f = timeSerieBySize.getValues();
            sizeThreshold = timeSerieBySize.getThresholds();
        } else {
            getSimulation().error("Could not found parameters mortality.fishing.rate.byDt.byAge/bySize.file.sp" + iSpec, null);
        }
    }

    @Override
    public float getInstantaneousRate(School school) {
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
    public float getAnnualRate() {
        double F = 0;
        for (int iStep = 0; iStep < f.length; iStep++) {
            for (int k = 0; k < f[iStep].length; k++) {
                F += f[iStep][k];
            }
        }
        F = F / (f.length * getConfiguration().getNYear());
        return (float) F;
    }
}
