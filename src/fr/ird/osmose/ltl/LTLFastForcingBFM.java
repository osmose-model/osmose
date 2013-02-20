/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.ltl;

import fr.ird.osmose.Plankton;
import java.io.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 *
 * @author pverley
 */
public class LTLFastForcingBFM extends AbstractLTLForcing {

    private String[] planktonFileListNetcdf;
    private String zlevelName;
    private String bathyFile;
    private String bathyName;
    private String[] planktonNetcdfNames;
    private float[][][] depthOfLayer;
    private int[][][] indexOceanpoint;
    private int im, jm, km;
    private int timeDim;
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
            /*
             * Read name of plankton variable in the BFM NetCDF file
             */
            planktonNetcdfNames = new String[getNumberPlanktonGroups()];
            for (int i = 0; i < getNumberPlanktonGroups(); i++) {
                st.nextToken();
                planktonNetcdfNames[i] = st.sval;
            }
            /*
             * Reads number of records in BFM NetCDF file
             * Reads number of BFM NetCDF files
             */
            st.nextToken();
            timeDim = Integer.valueOf(st.sval);
            st.nextToken();
            int nbFiles = Integer.valueOf(st.sval);
            /*
             * Read list of BFM NetCDF files
             */
            planktonFileListNetcdf = new String[nbFiles];
            for (int step = 0; step < nbFiles; step++) {
                st.nextToken();
                planktonFileListNetcdf[step] = st.sval;
            }
            /*
             * Read NetCDF filename with the bathymetry variable
             * Read name of the zlevel variable in the NetCDF file
             * Read name of the bathymetry variable in the NetCDF file
             */
            st.nextToken();
            bathyFile = st.sval;
            st.nextToken();
            zlevelName = st.sval;
            st.nextToken();
            bathyName = st.sval;

        } catch (IOException ex) {
            System.out.println("Reading error of LTL file");
            System.exit(1);
        }
    }

    @Override
    public void initLTLGrid() {

        NetcdfFile nc = null;
        /*
         * Open BFM temperature file that contains bathymetry variable
         */
        try {
            nc = NetcdfFile.open(getOsmose().resolveFile(bathyFile), null);
        } catch (IOException ex) {
            System.err.println("Failed to open BFM Temperature file " + bathyFile);
            Logger.getLogger(LTLFastForcingBFM.class.getName()).log(Level.SEVERE, null, ex);
        }


        try {
            /*
             * Read the zlevel variable
             */
            float[] zlevel = (float[]) nc.findVariable(zlevelName).read().copyToNDJavaArray();
            /*
             * Read the BFM grid dimensions
             */
            int[] shape = nc.findVariable(zlevelName).getShape();
            setDimZ(shape[0] - 1);
            shape = nc.findVariable(bathyName).getShape();
            setDimY(shape[0]);
            setDimX(shape[1]);
            /*
             * Read the bathymetry variable
             */
            float[][] bathy = (float[][]) nc.findVariable(bathyName).read().copyToNDJavaArray();
            nc.close();
            /*
             * Compute the depth of every cell in meter
             */
            depthOfLayer = new float[get_nx()][get_ny()][get_nz()];
            for (int i = 0; i < get_nx(); i++) {
                for (int j = 0; j < get_ny(); j++) {
                    for (int z = 0; z < get_nz(); z++) {
                        depthOfLayer[i][j][z] = bathy[j][i] * zlevel[z];
                    }
                }
            }
            /*
             * Associate osmose cells to BFM cells
             */
            icoordLTLGrid = new ArrayList[getGrid().getNbLines()][getGrid().getNbColumns()];
            jcoordLTLGrid = new ArrayList[getGrid().getNbLines()][getGrid().getNbColumns()];
            int stride = getGrid().getStride();
            for (int i = 0; i < getGrid().getNbLines(); i++) {
                for (int j = 0; j < getGrid().getNbColumns(); j++) {
                    for (int ii = 0; ii < stride; ii++) {
                        for (int jj = 0; jj < stride; jj++) {
                            int posiTemp = getGrid().getNbLines() - i - 1;
                            if (null == icoordLTLGrid[posiTemp][j]) {
                                icoordLTLGrid[posiTemp][j] = new ArrayList();
                                jcoordLTLGrid[posiTemp][j] = new ArrayList();
                            }
                            icoordLTLGrid[posiTemp][j].add(j * stride + jj);
                            jcoordLTLGrid[posiTemp][j].add(i * stride + ii);
                        }
                    }
                }
            }

            indexMapping();
            loadData();

        } catch (IOException ex) {
            Logger.getLogger(LTLFastForcingBFM.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /*
     * (i, j, k) ==> oceanpoint coordinate for BFM variables
     */
    private int ijk2oceanpoint(int i, int j, int k) {
        return indexOceanpoint[k][j][i];
    }

    /*
     * BFM variables are indexed with a single dimension 'oceanpoint'
     * This function creates a map for translating (i, j, k) usual coordinates
     * int oceanpoint coordinates.
     */
    private void indexMapping() throws IOException {
        /*
         * Load the mask
         */
        String gridFile = getOsmose().gridFileTab;
        String strMask = getOsmose().maskFieldTab;
        NetcdfFile nc = NetcdfFile.open(gridFile, null);
        float[][][] mask = (float[][][]) nc.findVariable(strMask).read().copyToNDJavaArray();

        /*
         * Reads the BFM grid dimensions
         */
        km = nc.findDimension("z").getLength();
        jm = nc.findDimension("y").getLength();
        im = nc.findDimension("x").getLength();
        int cont = 0;
        /*
         * Compute the index that helps to convert oceanpoint coordinates to
         * (i, j, k) coordinates.
         */
        indexOceanpoint = new int[km][jm][im];
        for (int k = 0; k < km; k++) {
            for (int j = 0; j < jm; j++) {
                for (int i = 0; i < im; i++) {
                    if (mask[k][j][i] > 0) {
                        indexOceanpoint[k][j][i] = cont;
                        cont++;
                    } else {
                        indexOceanpoint[k][j][i] = -1;
                    }
                }
            }
        }
    }

    private void loadData() {

        System.out.println("Loading all plankton data, it might take a while...");

        data = new float[getOsmose().getNumberTimeStepsPerYear()][getNumberPlanktonGroups()][get_nx()][get_ny()];
        for (int iStep = 0; iStep < getOsmose().getNumberTimeStepsPerYear(); iStep++) {
            String ncfile = getOsmose().resolveFile(planktonFileListNetcdf[iStep / timeDim]);
            int timestep = iStep % timeDim;
            data[iStep] = getIntegratedData(ncfile, timestep);
        }

        System.out.println("All plankton data loaded !");
    }

    private float[][][] getIntegratedData(String ncfile, int timestep) {

        float[][][] integratedData = new float[getNumberPlanktonGroups()][get_nx()][get_ny()];
        float[][][] dataInit;

        try {
            NetcdfFile nc = NetcdfFile.open(ncfile);
            /*
             * Loop over the plankton groups
             */
            for (int p = 0; p < getNumberPlanktonGroups(); p++) {
                /*
                 * Read the concentration of plankton
                 */
                Variable ncvar = nc.findVariable(planktonNetcdfNames[p]);
                int[] shape = ncvar.getShape();
                dataInit = new float[get_nx()][get_ny()][get_nz()];
                float[] variable = (float[]) ncvar.read(new int[]{timestep, 0}, new int[]{1, shape[1]}).reduce().copyToNDJavaArray();
                /*
                 * Fill up the plankton.dataInit array with the concentrations
                 */
                for (int i = 0; i < im; i++) {
                    for (int j = 0; j < jm; j++) {
                        for (int k = 0; k < km; k++) {
                            int oceanpoint = ijk2oceanpoint(i, j, k);
                            if (oceanpoint >= 0) {
                                dataInit[i][j][k] = variable[oceanpoint];
                            } else {
                                dataInit[i][j][k] = 0.f;
                            }
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
            nc.close();
        } catch (InvalidRangeException ex) {
            Logger.getLogger(LTLFastForcingBFM.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException e) {
            Logger.getLogger(LTLFastForcingBFM.class.getName()).log(Level.SEVERE, null, e);
        }

        return integratedData;
    }

    @Override
    float[][] getRawBiomass(Plankton plankton, int iStepSimu) {
        return data[getIndexStepLTL(iStepSimu)][plankton.getIndex()];
    }
}
