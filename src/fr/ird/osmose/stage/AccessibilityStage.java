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
            accessStageMetrics = "null";
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
