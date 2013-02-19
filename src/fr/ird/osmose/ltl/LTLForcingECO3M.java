/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.ltl;

import fr.ird.osmose.Cell;
import fr.ird.osmose.Plankton;
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
            LTLFile = new FileInputStream(new File(getOsmose().resolveFile(planktonFileName)));
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
            plktonNetcdfNames = new String[getNumberPlanktonGroups()];
            for (int i = 0; i < getNumberPlanktonGroups(); i++) {
                st.nextToken();
                plktonNetcdfNames[i] = st.sval;
            }

            planktonFileListNetcdf = new String[getNbForcingDt()];
            for (int step = 0; step < getNbForcingDt(); step++) {
                st.nextToken();
                planktonFileListNetcdf[step] = st.sval;
            }

            st.nextToken();
            zlevelName = st.sval;

        } catch (IOException ex) {
            System.out.println("Reading error of LTL file");
            return;
        }
    }

    @Override
    public void initLTLGrid() {

        NetcdfFile ncGrid = null;
        String gridFilename = getOsmose().resolveFile(planktonFileListNetcdf[0]);
        try {
            ncGrid = NetcdfFile.open(gridFilename, null);
        } catch (IOException ex) {
            System.err.println("Failed to open plankton grid file " + gridFilename);
            ex.printStackTrace();
        }

        int[] shape = ncGrid.findVariable(zlevelName).getShape();
        setDimZ(shape[0]);
        setDimY(shape[1]);
        setDimX(shape[2]);

        depthOfLayer = new float[get_nx()][get_ny()][get_nz()];

        try {
            ArrayDouble.D3 arrDepth = (D3) ncGrid.findVariable(zlevelName).read().flip(1);
            for (int i = 0; i < get_nx(); i++) {
                for (int j = 0; j < get_ny(); j++) {
                    for (int z = 0; z < get_nz(); z++) {
                        depthOfLayer[i][j][z] = (float) arrDepth.get(z, j, i);
                    }
                }
            }
            ncGrid.close();

            icoordLTLGrid = new ArrayList[getGrid().getNbLines()][getGrid().getNbColumns()];
            jcoordLTLGrid = new ArrayList[getGrid().getNbLines()][getGrid().getNbColumns()];
            int stride = getGrid().getStride();
            for (int i = 0; i < getGrid().getNbLines(); i++) {
                for (int j = 0; j < getGrid().getNbColumns(); j++) {
                    for (int ii = 0; ii < stride; ii++) {
                        for (int jj = 0; jj < stride; jj++) {
                            if (null == icoordLTLGrid[i][j]) {
                                icoordLTLGrid[i][j] = new ArrayList();
                                jcoordLTLGrid[i][j] = new ArrayList();
                            }
                            icoordLTLGrid[i][j].add(j * stride + jj);
                            jcoordLTLGrid[i][j].add(i * stride + ii);
                        }
                    }
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    float[][] getRawBiomass(Plankton plankton, int iStepSimu) {

        String name = getOsmose().resolveFile(planktonFileListNetcdf[getIndexStepLTL(iStepSimu)]);
        int[] shape;
        ArrayDouble.D3 tempArray;
        float[][][] data3d = new float[get_nx()][get_ny()][get_nz()];

        try {
            NetcdfFile nc = NetcdfFile.open(name);
            // read data and put them in the local arrays

            Variable tempVar = nc.findVariable(plktonNetcdfNames[plankton.getIndex()]);
            tempArray = (ArrayDouble.D3) tempVar.read().flip(1);
            shape = tempVar.getShape();

            // fill dataInit of plankton classes from local arrays
            for (int i = 0; i < shape[2]; i++) {
                for (int j = 0; j < shape[1]; j++) {
                    for (int k = 0; k < shape[0]; k++) {
                        data3d[i][j][k] = (float) tempArray.get(k, j, i);    // carreful, index not in the same order
                    }
                }
            }
            nc.close();
        } catch (IOException ex) {
            Logger.getLogger(LTLForcingECO3M.class.getName()).log(Level.SEVERE, null, ex);
        }

        // vertical integration
        return verticalIntegration(data3d, depthOfLayer, getIntegrationDepth());
    }

    @Override
    public int getIndexStepLTL(int iStepSimu) {
        return iStepSimu % getOsmose().getNumberTimeStepsPerYear();
    }
}
