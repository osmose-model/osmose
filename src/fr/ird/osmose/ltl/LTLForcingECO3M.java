/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.ltl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.ArrayDouble;
import ucar.nc2.NetcdfFile;

/**
 *
 * @author pverley
 */
public class LTLForcingECO3M extends AbstractLTLForcing {

    private String[] planktonFileListNetcdf;
    private String zlevelName;
    private float[][][] depthOfLayer;       // table of height of layers of ROMS model used in vertical integration
    private String[] plktonNetcdfNames;

    @Override
    public void readLTLForcingFile() {

        plktonNetcdfNames = new String[getConfiguration().getNPlankton()];
        for (int i = 0; i < getConfiguration().getNPlankton(); i++) {
            plktonNetcdfNames[i] = getConfiguration().getString("ltl.netcdf.var.plankton.plk" + i);
        }

        planktonFileListNetcdf = new String[getConfiguration().findKeys("ltl.netcdf.file.t*").size()];
        for (int i = 0; i < planktonFileListNetcdf.length; i++) {
            planktonFileListNetcdf[i] = getConfiguration().getFile("ltl.netcdf.file.t" + i);
        }

        zlevelName = getConfiguration().getString("ltl.netcdf.var.zlevel");

    }

    @Override
    public void initLTLGrid() {

        NetcdfFile ncGrid = null;
        String gridFilename = planktonFileListNetcdf[0];
        try {
            ncGrid = NetcdfFile.open(gridFilename, null);
        } catch (IOException ex) {
            System.err.println("Failed to open plankton grid file " + gridFilename);
            System.exit(1);
        }

        int[] shape = ncGrid.findVariable(zlevelName).getShape();
        setDimZ(shape[0]);
        setDimY(shape[1]);
        setDimX(shape[2]);

        depthOfLayer = new float[get_nz()][get_ny()][get_nx()];

        try {
            ArrayDouble.D3 arrDepth = (ArrayDouble.D3) ncGrid.findVariable(zlevelName).read();
            for (int i = 0; i < get_nx(); i++) {
                for (int j = 0; j < get_ny(); j++) {
                    for (int z = 0; z < get_nz(); z++) {
                        depthOfLayer[z][j][i] = (float) arrDepth.get(z, j, i);
                    }
                }
            }
            ncGrid.close();

            icoordLTLGrid = new ArrayList[getGrid().get_ny()][getGrid().get_nx()];
            jcoordLTLGrid = new ArrayList[getGrid().get_ny()][getGrid().get_nx()];
            int stride = getGrid().getStride();
            for (int j = 0; j < getGrid().get_ny(); j++) {
                for (int i = 0; i < getGrid().get_nx(); i++) {
                    for (int ii = 0; ii < stride; ii++) {
                        for (int jj = 0; jj < stride; jj++) {
                            if (null == icoordLTLGrid[j][i]) {
                                icoordLTLGrid[j][i] = new ArrayList();
                                jcoordLTLGrid[j][i] = new ArrayList();
                            }
                            icoordLTLGrid[j][i].add(i * stride + jj);
                            jcoordLTLGrid[j][i].add(j * stride + ii);
                        }
                    }
                }
            }

        } catch (IOException ex) {
            Logger.getLogger(LTLForcingECO3M.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    float[][] getRawBiomass(int iPlankton, int iStepSimu) {

        float[][][] dataInit = new float[get_nz()][get_ny()][get_nx()];
        NetcdfFile nc = null;
        String name = planktonFileListNetcdf[getIndexStepLTL(iStepSimu)];
        ArrayDouble.D3 tempArray = null;

        try {
            nc = NetcdfFile.open(name);
            // read data and put them in the local arrays
            tempArray = (ArrayDouble.D3) nc.findVariable(plktonNetcdfNames[iPlankton]).read();
            int[] shape = tempArray.getShape();

            // fill dataInit of plankton classes from local arrays
            for (int i = 0; i < shape[2]; i++) {
                for (int j = 0; j < shape[1]; j++) {
                    for (int k = 0; k < shape[0]; k++) {
                        dataInit[k][j][i] = (float) tempArray.get(k, j, i);
                    }
                }
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to read variable " + plktonNetcdfNames[iPlankton] + " from file " + name, e);
        } finally {
            if (nc != null) {
                try {
                    nc.close();
                } catch (IOException ioe) {
                    getLogger().log(Level.SEVERE, "Failed to close NetCDF file " + name, ioe);
                }
            }
        }

        // vertical integration
        return verticalIntegration(dataInit, depthOfLayer, getConfiguration().getFloat("ltl.integration.depth"));
    }

    @Override
    public String[] getPlanktonFieldName() {
        return plktonNetcdfNames;
    }

    @Override
    public String[] getNetcdfFile() {
        return planktonFileListNetcdf;
    }
}
