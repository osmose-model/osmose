package fr.ird.osmose.ltl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.nc2.NetcdfFile;

/**
 *
 * @author pverley
 */
public class LTLFastForcingECO3M extends AbstractLTLForcing {

    private String[] planktonFileListNetcdf;
    private String zlevelName;
    private float[][][] depthOfLayer;       // table of height of layers of ROMS model used in vertical integration
    private String[] plktonNetcdfNames;
    private float[][][][] data;

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
            System.exit(0);
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
            ArrayDouble.D3 arrDepth = (ArrayDouble.D3) ncGrid.findVariable(zlevelName).read().flip(1);
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
            Logger.getLogger(LTLFastForcingECO3M.class.getName()).log(Level.SEVERE, null, ex);
        }

        loadData();


    }

    private void loadData() {

        System.out.println("Loading all plankton data, it might take a while...");

        data = new float[getSimulation().getNumberTimeStepsPerYear()][getNbPlanktonGroups()][getPlanktonDimX()][getPlanktonDimY()];
        for (int t = 0; t < getSimulation().getNumberTimeStepsPerYear(); t++) {
            data[t] = getIntegratedData(getOsmose().resolveFile(planktonFileListNetcdf[t]));
        }

        System.out.println("All plankton data loaded !");
    }

    private float[][][] getIntegratedData(String nameOfFile) {

        float[][][] integratedData = new float[getNbPlanktonGroups()][getPlanktonDimX()][getPlanktonDimY()];
        float[][][] dataInit;
        NetcdfFile nc = null;
        String name = nameOfFile;
        ArrayDouble.D3 tempArray;

        try {
            nc = NetcdfFile.open(name);
            // read data and put them in the local arrays
            for (int p = 0; p < getNbPlanktonGroups(); p++) {
                tempArray = (ArrayDouble.D3) nc.findVariable(plktonNetcdfNames[p]).read().flip(1);
                dataInit = new float[getPlanktonDimX()][getPlanktonDimY()][getPlanktonDimZ()];
                int[] shape = tempArray.getShape();

                // fill dataInit of plankton classes from local arrays
                for (int i = 0; i < shape[2]; i++) {
                    for (int j = 0; j < shape[1]; j++) {
                        for (int k = 0; k < shape[0]; k++) {
                            dataInit[i][j][k] = (float) tempArray.get(k, j, i);
                        }
                    }
                }

                // integrates vertically plankton biomass, using depth files
                float integr;
                for (int i = 0; i < depthOfLayer.length; i++) {
                    for (int j = 0; j < depthOfLayer[i].length; j++) {
                        integr = 0f;
                        for (int k = 0; k < depthOfLayer[i][j].length - 1; k++) {
                            if (depthOfLayer[i][j][k] > getIntegrationDepth()) {
                                if (dataInit[i][j][k] >= 0 && dataInit[i][j][k + 1] >= 0) {
                                    integr += (Math.abs(depthOfLayer[i][j][k] - depthOfLayer[i][j][k + 1])) * ((dataInit[i][j][k] + dataInit[i][j][k + 1]) / 2f);
                                }
                            }
                        }
                        integratedData[p][i][j] = integr;
                    }
                }
            }
        } catch (IOException e) {
            Logger.getLogger(LTLForcingRomsPisces.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            if (nc != null) {
                try {
                    nc.close();
                } catch (IOException ioe) {
                    Logger.getLogger(LTLForcingRomsPisces.class.getName()).log(Level.SEVERE, null, ioe);
                }
            }
        }

        return integratedData;
    }

    @Override
    public void updatePlankton(int dt) {

        for (int i = 0; i < getNbPlanktonGroups(); i++) {
            getPlanktonGroup(i).clearPlankton();      // put the biomass tables of plankton to 0
        }
        updateData(dt);
        mapInterpolation();
    }

    private void updateData(int dt) {

        for (int p = 0; p < getNbPlanktonGroups(); p++) {
            getPlankton(p).integratedData = data[dt][p];
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
