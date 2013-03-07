package fr.ird.osmose.ltl;

import fr.ird.osmose.Plankton;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.nc2.NetcdfFile;

/**
 *
 * @author pverley
 */
public class LTLFastForcing extends AbstractLTLForcing {

    private String ncFile;
    private float[][][][] data;

    @Override
    public void readLTLForcingFile(String planktonFileName) {

        ncFile = getConfiguration().resolveFile(planktonFileName);
        if (!new File(ncFile).exists()) {
            System.out.println("LTL NetCDF file " + ncFile + " doesn't exist");
            System.exit(1);
        }
    }

    @Override
    public void initLTLGrid() {

        /*
         * set dimensions
         */
        setDimX(getGrid().getNbLines());
        setDimY(getGrid().getNbColumns());

        loadData();
    }

    @Override
    public float[][] computeBiomass(Plankton plankton, int iStepSimu) {

        float[][] biomass = new float[getGrid().getNbLines()][getGrid().getNbColumns()];
        int nl = getGrid().getNbLines() - 1;
        for (int i = 0; i < getGrid().getNbLines(); i++) {
            for (int j = 0; j < getGrid().getNbColumns(); j++) {
                if (!getGrid().getCell(i, j).isLand()) {
                    biomass[i][j] = data[getIndexStepLTL(iStepSimu)][plankton.getIndex()][nl - i][j];
                }
            }
        }
        return biomass;
    }

    private void loadData() {
        try {
            System.out.println("Loading all plankton data, it might take a while...");
            System.out.println("Forcing file " + getConfiguration().resolveFile(ncFile));

            NetcdfFile nc = NetcdfFile.open(getConfiguration().resolveFile(ncFile));
            data = (float[][][][]) nc.findVariable("ltl_biomass").read().copyToNDJavaArray();

            System.out.println("All plankton data loaded !");
        } catch (IOException ex) {
            Logger.getLogger(LTLFastForcing.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    float[][] getRawBiomass(Plankton plankton, int iStepYear) {
        return null;
    }

    @Override
    public String[] getPlanktonFieldName() {
        return null;
    }

    @Override
    public String[] getNetcdfFile() {
        return new String[] {ncFile};
    }
}
