/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.ltl;

/**
 *
 * @author pverley
 */
public class LTLFastForcingBFM extends LTLForcingBFM {

    private float[][][][] data;

    @Override
    public void initLTLGrid() {
        super.initLTLGrid();
        loadData();
    }

    private void loadData() {

        getLogger().info("Loading all plankton data, it might take a while...");
        data = new float[getConfiguration().getNumberTimeStepsPerYear()][getConfiguration().getNPlankton()][get_nx()][get_ny()];
        for (int iStep = 0; iStep < getConfiguration().getNumberTimeStepsPerYear(); iStep++) {
            for (int p = 0; p < getConfiguration().getNPlankton(); p++) {
                data[iStep][p] = super.getRawBiomass(p, iStep);
            }
        }
        getLogger().info("All plankton data loaded !");
    }

    @Override
    float[][] getRawBiomass(int iPlankton, int iStepSimu) {
        return data[iStepSimu % getConfiguration().getNumberTimeStepsPerYear()][iPlankton];
    }
}
