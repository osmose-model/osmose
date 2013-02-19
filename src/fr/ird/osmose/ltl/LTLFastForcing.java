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
    private int iStep;

    @Override
    public void readLTLConfigFile2(String planktonFileName) {

        ncFile = getOsmose().resolveFile(planktonFileName);
        if (!new File(ncFile).exists()) {
            System.out.println("LTL NetCDF file " + ncFile + " doesn't exist");
            System.exit(1);
        }

        iStep = 0;
    }

    @Override
    public void initPlanktonMap() {

        /*
         * set dimensions
         */
        setDimX(getGrid().getNbLines());
        setDimY(getGrid().getNbColumns());

        loadData();
    }

    @Override
    public void updatePlankton(int iStepSimu) {

        // clear & update biomass
        for (int p = 0; p < getNbPlanktonGroups(); p++) {
            // clear biomass
            getPlanktonGroup(p).clearPlankton();
            // update biomass
            float[][] biomass = new float[getGrid().getNbLines()][getGrid().getNbColumns()];
            int nl = getGrid().getNbLines() - 1;
            for (int i = 0; i < getGrid().getNbLines(); i++) {
                for (int j = 0; j < getGrid().getNbColumns(); j++) {
                    if (!getGrid().getCell(i, j).isLand()) {
                        biomass[i][j] = data[getIndexStepLTL(iStepSimu)][p][nl - i][j];
                    }
                }
            }
            getPlanktonGroup(p).updateBiomass(biomass);
        }
        // increment ltl time step
        iStep++;
        if (iStep >= data.length) {
            iStep = 0;
        }
    }

    private void loadData() {
        try {
            System.out.println("Loading all plankton data, it might take a while...");
            System.out.println("Forcing file " + getOsmose().resolveFile(ncFile));

            NetcdfFile nc = NetcdfFile.open(getOsmose().resolveFile(ncFile));
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
    public int getIndexStepLTL(int iStepSimu) {
        return iStep;
    }
}
