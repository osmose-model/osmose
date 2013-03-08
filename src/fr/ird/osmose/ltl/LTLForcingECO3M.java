/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.ltl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayDouble.D3;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

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
    public void readLTLForcingFile(String planktonFileName) {
        FileInputStream LTLFile;
        try {
            LTLFile = new FileInputStream(new File(getConfiguration().resolveFile(planktonFileName)));
        } catch (FileNotFoundException ex) {
            System.out.println("LTL file " + planktonFileName + " doesn't exist");
            return;
        }

        Reader r = new BufferedReader(new InputStreamReader(LTLFile));
        StreamTokenizer st = new StreamTokenizer(r);
        st.slashSlashComments(true);
        st.slashStarComments(true);
        st.quoteChar(';');

        try {
            plktonNetcdfNames = new String[getConfiguration().getNPlankton()];
            for (int i = 0; i < getConfiguration().getNPlankton(); i++) {
                st.nextToken();
                plktonNetcdfNames[i] = st.sval;
            }

            planktonFileListNetcdf = new String[getConfiguration().getNumberLTLSteps()];
            for (int step = 0; step < getConfiguration().getNumberLTLSteps(); step++) {
                st.nextToken();
                planktonFileListNetcdf[step] = st.sval;
            }

            st.nextToken();
            zlevelName = st.sval;

        } catch (IOException ex) {
            System.out.println("Reading error of LTL file");
            System.exit(1);
        }
    }

    @Override
    public void initLTLGrid() {

        NetcdfFile ncGrid = null;
        String gridFilename = getConfiguration().resolveFile(planktonFileListNetcdf[0]);
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

        float[][][] dataInit  = new float[get_nz()][get_ny()][get_nx()];
        NetcdfFile nc = null;
        String name = getConfiguration().resolveFile(planktonFileListNetcdf[getIndexStepLTL(iStepSimu)]);
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
        } catch (IOException e) {
            Logger.getLogger(LTLForcingECO3M.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            if (nc != null) {
                try {
                    nc.close();
                } catch (IOException ioe) {
                    Logger.getLogger(LTLForcingECO3M.class.getName()).log(Level.SEVERE, null, ioe);
                }
            }
        }

        // vertical integration
        return verticalIntegration(dataInit, depthOfLayer, getConfiguration().getIntegrationDepth());
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
