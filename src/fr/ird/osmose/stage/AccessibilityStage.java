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
package fr.ird.osmose.stage;

import fr.ird.osmose.School;

/**
 *
 * @author pverley
 */
public class AccessibilityStage extends AbstractStage {

    /**
     * Metrics used for splitting the stages (either age or size).
     */
    private String accessStageMetrics;
    /**
     * Threshold age (year) between accessibility stages.
     * Array[nSpecies][nAccessStage]
     */
    private float[][] accessStageThreshold;

    @Override
    public void init() {

        int nSpec = getConfiguration().getNSpecies();
        int nPlankton = getConfiguration().getNPlankton();
        accessStageThreshold = new float[nSpec + nPlankton][];
        if (getConfiguration().canFind("predation.accessibility.stage.structure")) {
            accessStageMetrics = getConfiguration().getString("predation.accessibility.stage.structure");
        } else {
            accessStageMetrics = "age";
            getOsmose().warning("Could not find parameter 'predation.accessibility.stage.structure'. Osmose assumes it is age-based threshold.");
        }

        for (int i = 0; i < nSpec; i++) {
            // diet output stage
            int nDietOutputStage = !getConfiguration().isNull("predation.accessibility.stage.threshold.sp" + i)
                    ? getConfiguration().getArrayString("predation.accessibility.stage.threshold.sp" + i).length + 1
                    : 1;
            if (nDietOutputStage > 1) {
                accessStageThreshold[i] = getConfiguration().getArrayFloat("predation.accessibility.stage.threshold.sp" + i);
            } else {
                accessStageThreshold[i] = new float[0];
            }
        }
        for (int i = 0; i < nPlankton; i++) {
            accessStageThreshold[nSpec + i] = new float[0];
        }
    }

    @Override
    public int getStage(School school) {

        int stage = 0;
        if (accessStageMetrics.equalsIgnoreCase("size")) {
            int iSpec = school.getSpeciesIndex();
            for (int i = 0; i < accessStageThreshold[iSpec].length; i++) {
                if (school.getLength() >= accessStageThreshold[iSpec][i]) {
                    stage++;
                } else {
                    break;
                }
            }
        } else if (accessStageMetrics.equalsIgnoreCase("age")) {
            int iSpec = school.getSpeciesIndex();
            for (int i = 0; i < accessStageThreshold[iSpec].length; i++) {
                int tempAge = Math.round(accessStageThreshold[iSpec][i] * getConfiguration().getNStepYear());
                if (school.getAgeDt() >= tempAge) {
                    stage++;
                } else {
                    break;
                }
            }
        }
        return stage;
    }

    @Override
    public int getNStage(int iSpecies) {
        return accessStageThreshold[iSpecies].length + 1;
    }

    @Override
    public float[] getThresholds(int iSpecies) {
        return accessStageThreshold[iSpecies];
    }
}
