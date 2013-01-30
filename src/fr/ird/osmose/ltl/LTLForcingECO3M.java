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
    public void readLTLConfigFile2(String planktonFileName) {
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
            plktonNetcdfNames = new String[getNbPlanktonGroups()];
            for (int i = 0; i < getNbPlanktonGroups(); i++) {
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
    public void initPlanktonMap() {

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

        depthOfLayer = new float[getPlanktonDimX()][getPlanktonDimY()][getPlanktonDimZ()];

        try {
            ArrayDouble.D3 arrDepth = (D3) ncGrid.findVariable(zlevelName).read().flip(1);
            for (int i = 0; i < getPlanktonDimX(); i++) {
                for (int j = 0; j < getPlanktonDimY(); j++) {
                    for (int z = 0; z < getPlanktonDimZ(); z++) {
                        depthOfLayer[i][j][z] = (float) arrDepth.get(z, j, i);
                    }
                }
            }
            ncGrid.close();

            int stride = getGrid().getStride();
            for (int i = 0; i < getGrid().getNbLines(); i++) {
                for (int j = 0; j < getGrid().getNbColumns(); j++) {
                    for (int ii = 0; ii < stride; ii++) {
                        for (int jj = 0; jj < stride; jj++) {
                                getGrid().getCell(i, j).icoordLTLGrid.addElement(j * stride + jj);
                                getGrid().getCell(i, j).jcoordLTLGrid.addElement(i * stride + ii);
                            }
                        }
                    }
                }

            initPlanktonList();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void updatePlankton(int dt) {
        for (int i = 0; i < getNbPlanktonGroups(); i++) {
            getPlanktonGroup(i).clearPlankton();      // put the biomass tables of plankton to 0
        }
        readNetCDFFile(getOsmose().resolveFile(planktonFileListNetcdf[dt]));
        mapInterpolation();
    }

    private void readNetCDFFile(String nameOfFile) {

        //System.out.println("Reading " + nameOfFile);

        NetcdfFile nc = null;
        String name = nameOfFile;
        Variable[] tempVar;
        int[] shape;
        ArrayDouble.D3[] tempArray;
        tempArray = new ArrayDouble.D3[getNbPlanktonGroups()];
        tempVar = new Variable[getNbPlanktonGroups()];

        try {
            nc = NetcdfFile.open(name);
            // read data and put them in the local arrays
            for (int i = 0; i < getNbPlanktonGroups(); i++) {
                tempVar[i] = nc.findVariable(plktonNetcdfNames[i]);
                tempArray[i] = (ArrayDouble.D3) tempVar[i].read().flip(1);
            }
            shape = tempVar[0].getShape();

            // fill dataInit of plankton classes from local arrays
            for (int i = 0; i < shape[2]; i++) {
                for (int j = 0; j < shape[1]; j++) {
                    for (int k = 0; k < shape[0]; k++) {
                        for (int p = 0; p < getNbPlanktonGroups(); p++) {
                            getPlanktonGroup(p).dataInit[i][j][k] = (float) tempArray[p].get(k, j, i);    // carreful, index not in the same order
                        }
                    }
                }
            }
            // integrates vertically plankton biomass, using depth files
            for (int p = 0; p < getNbPlanktonGroups(); p++) {
                getPlanktonGroup(p).verticalIntegration(depthOfLayer, getIntegrationDepth());
                //System.out.println(getPlanktonGroup(p).getName() + " " + getPlanktonGroup(p).integratedData[10][10]);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (nc != null) {
                try {
                    nc.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
    }

    // CASE SPECIFIC - depends of the LTL grid
    // from ECO3M (vertically integrated) towards OSMOSE
    @Override
    public void mapInterpolation() {

        int tempX, tempY;
        for (int i = 0; i < getGrid().getNbLines(); i++) {
            for (int j = 0; j < getGrid().getNbColumns(); j++) {
                if (!getGrid().getCell(i, j).isLand()) {
                    for (int k = 0; k < getGrid().getCell(i, j).getNbCellsLTLGrid(); k++) {
                        for (int p = 0; p < getNbPlanktonGroups(); p++) {
                            tempX = ((Integer) getGrid().getCell(i, j).icoordLTLGrid.elementAt(k)).intValue();
                            tempY = ((Integer) getGrid().getCell(i, j).jcoordLTLGrid.elementAt(k)).intValue();
                            /*if (p == 0) {
                                System.out.println("osmose cell (" + i + ", " + j + ") contains ECO3M cell (" + tempX + ", " + tempY + ")");
                            }*/
                            // interpolate the plankton concentrations from the LTL cells
                            getPlanktonGroup(p).addCell(i, j, tempX, tempY, getGrid().getCell(i, j).getNbCellsLTLGrid());
                        }
                    }
                }
            }
        }
    }
}
