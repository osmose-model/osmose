package fr.ird.osmose.ltl;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.InvalidRangeException;
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
        
        for (int i = 0; i < getNbPlanktonGroups(); i++) {
            getPlanktonGroup(i).clearPlankton();      // put the biomass tables of plankton to 0
        }
        mapInterpolation();
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
            int nSteps = nc.getUnlimitedDimension().getLength();
            int nx = getPlanktonDimX();
            int ny = getPlanktonDimY();
            data = (float[][][][]) nc.findVariable("ltl_biomass").read().copyToNDJavaArray();
//            data = new float[nSteps][getNbPlanktonGroups()][][];
//            for (int t = 0; t < nSteps; t++) {
//                for (int p = 0; p < getNbPlanktonGroups(); p++) {
//                    try {
//                        data[t][p] = (float[][]) nc.findVariable("ltl_biomass").read(new int[]{t, p, 0, 0}, new int[]{1, 1, nx, ny}).reduce().copyToNDJavaArray();
//                    } catch (InvalidRangeException ex) {
//                        Logger.getLogger(LTLFastForcing.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                }
//            }

            System.out.println("All plankton data loaded !");
        } catch (IOException ex) {
            Logger.getLogger(LTLFastForcing.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void mapInterpolation() {
        int nl = getGrid().getNbLines() - 1;
        for (int i = 0; i < getGrid().getNbLines(); i++) {
            for (int j = 0; j < getGrid().getNbColumns(); j++) {
                if (!getGrid().getCell(i, j).isLand()) {
                    for (int p = 0; p < getNbPlanktonGroups(); p++) {
                        getPlanktonGroup(p).setBiomass(i, j, data[iStep][p][nl - i][j]);
                    }
                }
            }
        }
    }
}
