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
            System.exit(0);
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
            Logger.getLogger(LTLFastForcingECO3M.class.getName()).log(Level.SEVERE, null, ex);
        }

        int[] shape = ncGrid.findVariable(zlevelName).getShape();
        setDimZ(shape[0]);
        setDimY(shape[1]);
        setDimX(shape[2]);

        depthOfLayer = new float[get_nx()][get_ny()][get_nz()];

        try {
            ArrayDouble.D3 arrDepth = (ArrayDouble.D3) ncGrid.findVariable(zlevelName).read().flip(1);
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
            Logger.getLogger(LTLFastForcingECO3M.class.getName()).log(Level.SEVERE, null, ex);
        }

        loadData();


    }

    private void loadData() {

        System.out.println("Loading all plankton data, it might take a while...");

        data = new float[getOsmose().nStepYear][getNumberPlanktonGroups()][get_nx()][get_ny()];
        for (int t = 0; t < getOsmose().nStepYear; t++) {
            data[t] = getIntegratedData(getOsmose().resolveFile(planktonFileListNetcdf[t]));
        }

        System.out.println("All plankton data loaded !");
    }

    private float[][][] getIntegratedData(String nameOfFile) {

        float[][][] integratedData = new float[getNumberPlanktonGroups()][get_nx()][get_ny()];
        float[][][] dataInit;
        NetcdfFile nc = null;
        String name = nameOfFile;
        ArrayDouble.D3 tempArray;

        try {
            nc = NetcdfFile.open(name);
            // read data and put them in the local arrays
            for (int p = 0; p < getNumberPlanktonGroups(); p++) {
                tempArray = (ArrayDouble.D3) nc.findVariable(plktonNetcdfNames[p]).read().flip(1);
                dataInit = new float[get_nx()][get_ny()][get_nz()];
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
    float[][] getRawBiomass(Plankton plankton, int iStepSimu) {
        return data[getIndexStepLTL(iStepSimu)][plankton.getIndex()];
    }

    @Override
    public int getIndexStepLTL(int iStepSimu) {
        return iStepSimu % getOsmose().getNumberTimeStepsPerYear();
    }
}
