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
public class DietOutputStage extends AbstractStage {

    /**
     * Metrics used for splitting the stages (either age or size).
     */
    private String dietOutputMetrics;
    /**
     * Threshold age (year) or size (cm) between the diet stages.
     */
    private float[][] dietOutputStageThreshold;

    @Override
    public void init() {
        
        int nSpec = getConfiguration().getNSpecies();
        dietOutputStageThreshold = new float[nSpec][];
        if (getConfiguration().canFind("output.diet.stage.structure")) {
            dietOutputMetrics = getConfiguration().getString("output.diet.stage.structure");
        }

        for (int i = 0; i < nSpec; i++) {
            // diet output stage
            int nDietOutputStage = !getConfiguration().isNull("output.diet.stage.threshold.sp" + i)
                    ? getConfiguration().getArrayString("output.diet.stage.threshold.sp" + i).length + 1
                    : 1;
            if (nDietOutputStage > 1) {
                dietOutputStageThreshold[i] = getConfiguration().getArrayFloat("output.diet.stage.threshold.sp" + i);
            } else {
                dietOutputStageThreshold[i] = new float[0];
            }
        }
    }

    @Override
    public int getStage(School school) {

        int iSpec = school.getSpeciesIndex();
        int stage = 0;
        if (dietOutputMetrics.equalsIgnoreCase("size")) {
            for (int i = 0; i < dietOutputStageThreshold[iSpec].length; i++) {
                if (school.getLength() >= dietOutputStageThreshold[iSpec][i]) {
                    stage++;
                } else {
                    break;
                }
            }
        } else if (dietOutputMetrics.equalsIgnoreCase("age")) {
            for (int i = 0; i < dietOutputStageThreshold[iSpec].length; i++) {
                int tempAge = Math.round(dietOutputStageThreshold[iSpec][i] * getConfiguration().getNStepYear());
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
        return dietOutputStageThreshold[iSpecies].length + 1;
    }
    
    @Override
    public float[] getThresholds(int iSpecies) {
        return dietOutputStageThreshold[iSpecies];
    }
}
