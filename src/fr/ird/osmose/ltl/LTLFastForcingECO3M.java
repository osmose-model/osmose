package fr.ird.osmose.ltl;

/**
 *
 * @author pverley
 */
public class LTLFastForcingECO3M extends LTLForcingECO3M {

    private float[][][][] data;

    @Override
    public void initLTLGrid() {

        super.initLTLGrid();
        loadData();
    }

    private void loadData() {

        getLogger().info("Loading all plankton data...");
        data = new float[getConfiguration().getNStepYear()][getConfiguration().getNPlankton()][][];
        for (int iStep = 0; iStep < getConfiguration().getNStepYear(); iStep++) {
            for (int iPlankton = 0; iPlankton < getConfiguration().getNPlankton(); iPlankton++) {
                data[iStep][iPlankton] = super.getRawBiomass(iPlankton, iStep);
            }
        }
        getLogger().info("All plankton data loaded !");
    }

    @Override
    float[][] getRawBiomass(int iPlankton, int iStepSimu) {
        return data[getIndexStepLTL(iStepSimu)][iPlankton];
    }
}
