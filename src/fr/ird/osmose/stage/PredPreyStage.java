/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.stage;

import fr.ird.osmose.School;

/**
 *
 * @author pverley
 */
public class PredPreyStage extends AbstractStage {

    /**
     * Metrics used for splitting the stages (either age or size).
     */
    private String predPreyStageMetrics;
    /**
     * Threshold size (cm) of feeding stages. Array[nSpecies][nFeedingStage-1]
     * Array[nSpecies][nPredPreyStage]
     */
    private float[][] predPreyStageThreshold;

    @Override
    public void init() {

        int nSpec = getConfiguration().getNSpecies();
        int nPlankton = getConfiguration().getNPlankton();
        predPreyStageThreshold = new float[nSpec + nPlankton][];
        if (getConfiguration().canFind("predation.predPrey.stage.structure")) {
            predPreyStageMetrics = getConfiguration().getString("predation.predPrey.stage.structure");
        } else {
            predPreyStageMetrics = "null";
        }

        for (int i = 0; i < nSpec; i++) {
            // diet output stage
            int nDietOutputStage = !getConfiguration().isNull("predation.predPrey.stage.threshold.sp" + i)
                    ? getConfiguration().getArrayString("predation.predPrey.stage.threshold.sp" + i).length + 1
                    : 1;
            if (nDietOutputStage > 1) {
                predPreyStageThreshold[i] = getConfiguration().getArrayFloat("predation.predPrey.stage.threshold.sp" + i);
            } else {
                predPreyStageThreshold[i] = new float[0];
            }
        }
    }

    @Override
    public int getStage(School school) {
        
        int stage = 0;
        if (predPreyStageMetrics.equalsIgnoreCase("size")) {
            int iSpec = school.getSpeciesIndex();
            for (int i = 0; i < predPreyStageThreshold[iSpec].length; i++) {
                if (school.getLength() >= predPreyStageThreshold[iSpec][i]) {
                    stage++;
                } else {
                    break;
                }
            }
        } else if (predPreyStageMetrics.equalsIgnoreCase("age")) {
            int iSpec = school.getSpeciesIndex();
            for (int i = 0; i < predPreyStageThreshold[iSpec].length; i++) {
                int tempAge = Math.round(predPreyStageThreshold[iSpec][i] * getConfiguration().getNStepYear());
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
        return predPreyStageThreshold[iSpecies].length + 1;
    }

    @Override
    public float[] getThresholds(int iSpecies) {
        return predPreyStageThreshold[iSpecies];
    }
}
