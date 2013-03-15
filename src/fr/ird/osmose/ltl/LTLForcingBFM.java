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
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 *
 * @author pverley
 */
public class LTLForcingBFM extends AbstractLTLForcing {

    private String[] planktonFileListNetcdf;
    private String zlevelName;
    private String bathyFile;
    private String bathyName;
    private String[] planktonNetcdfNames;
    private float[][][] depthOfLayer;
    private int[][][] indexOceanpoint;
    private int im, jm, km;
    private int timeDim;

    @Override
    public void readLTLForcingFile() {

        planktonNetcdfNames = new String[getConfiguration().getNPlankton()];
        for (int i = 0; i < getConfiguration().getNPlankton(); i++) {
            planktonNetcdfNames[i] = getConfiguration().getString("ltl.netcdf.var.plankton.plk" + i);
        }

        planktonFileListNetcdf = new String[getConfiguration().findKeys("ltl.netcdf.file.t").size()];
        for (int i = 0; i < planktonFileListNetcdf.length; i++) {
            planktonFileListNetcdf[i] = getConfiguration().getFile("ltl.netcdf.file.t" + i);
        }

        timeDim = getConfiguration().getInt("ltl.netcdf.dim.ntime");
        bathyFile = getConfiguration().getFile("grid.netcdf.file");
        zlevelName = getConfiguration().getString("ltl.netcdf.var.zlevel");
        bathyName = getConfiguration().getString("ltl.netcdf.var.bathy");
    }

    @Override
    public void initLTLGrid() {

        NetcdfFile nc = null;
        /*
         * Open BFM temperature file that contains bathymetry variable
         */
        try {
            nc = NetcdfFile.open(bathyFile, null);
        } catch (IOException ex) {
            System.err.println("Failed to open BFM Temperature file " + bathyFile);
            Logger.getLogger(LTLForcingBFM.class.getName()).log(Level.SEVERE, null, ex);
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
            depthOfLayer = new float[get_nz()][get_ny()][get_nx()];
            for (int z = 0; z < get_nz(); z++) {
                for (int j = 0; j < get_ny(); j++) {
                    for (int i = 0; i < get_nx(); i++) {
                        depthOfLayer[z][j][i] = bathy[j][i] * zlevel[z];
                    }
                }
            }
            /*
             * Associate osmose cells to BFM cells
             */
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
                            jcoordLTLGrid[j][i].add(j * stride + jj);
                            icoordLTLGrid[j][i].add(i * stride + ii);
                        }
                    }
                }
            }

            indexMapping();

        } catch (IOException ex) {
            Logger.getLogger(LTLForcingBFM.class.getName()).log(Level.SEVERE, null, ex);
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
        String gridFile = getConfiguration().getFile("grid.netcdf.file");
        String strMask = getConfiguration().getString("grid.var.stride");
        NetcdfFile nc = NetcdfFile.open(gridFile, null);
        float[][] mask = (float[][]) nc.findVariable(strMask).read().copyToNDJavaArray();

        /*
         * Reads the BFM grid dimensions
         */
        km = nc.findDimension("zpos").getLength() - 1;
        jm = nc.findDimension("ypos").getLength();
        im = nc.findDimension("xpos").getLength();
        int cont = 0;
        /*
         * Compute the index that helps to convert oceanpoint coordinates to
         * (i, j, k) coordinates.
         */
        indexOceanpoint = new int[km][jm][im];
        for (int k = 0; k < km; k++) {
            for (int j = 0; j < jm; j++) {
                for (int i = 0; i < im; i++) {
                    if (mask[j][i] > 0) {
                        indexOceanpoint[k][j][i] = cont;
                        cont++;
                    } else {
                        indexOceanpoint[k][j][i] = -1;
                    }
                }
            }
        }
    }

    @Override
    float[][] getRawBiomass(int iPlankton, int iStepSimu) {

        //System.out.println("Reading " + name + " time " + dt);
        float[][][] data3d = new float[get_nz()][get_ny()][get_nx()];
        try {
            /*
             * Open the BFM Plankton NetCDF file
             */
            String name = planktonFileListNetcdf[getIndexStepLTL(iStepSimu)];
            NetcdfFile nc = NetcdfFile.open(name);
            /*
             * Loop over the plankton groups
             */
            int timestep = iStepSimu % timeDim;
            //System.out.println("iStepSimu " + iStepSimu + " " + name + " timestep " + timestep + " " + plankton.getName());
            /*
             * Read the concentration of plankton
             */
            Variable ncvar = nc.findVariable(planktonNetcdfNames[iPlankton]);
            int[] shape = ncvar.getShape();
            float[] variable = (float[]) ncvar.read(new int[]{timestep, 0}, new int[]{1, shape[1]}).reduce().copyToNDJavaArray();
            /*
             * Fill up the plankton.dataInit array with the concentrations
             */
            for (int i = 0; i < im; i++) {
                for (int j = 0; j < jm; j++) {
                    for (int k = 0; k < km; k++) {
                        int oceanpoint = ijk2oceanpoint(i, j, k);
                        if (oceanpoint >= 0) {
                            data3d[k][j][i] = variable[oceanpoint];
                        } else {
                            data3d[k][j][i] = 0.f;
                        }
                    }
                }
            }
            /*
             * Closes NetCDF file
             */
            nc.close();

        } catch (InvalidRangeException ex) {
            Logger.getLogger(LTLForcingBFM.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(LTLForcingBFM.class.getName()).log(Level.SEVERE, null, ex);
        }

        /*
         * Integrate plankton biomass on vertical dimension
         */
        return verticalIntegration(data3d, depthOfLayer, getConfiguration().getFloat("ltl.integration.depth"));
    }

    @Override
    public int getIndexStepLTL(int iStepSimu) {
        return (iStepSimu % getConfiguration().getNStepYear()) / timeDim;
    }

    @Override
    public String[] getPlanktonFieldName() {
        return planktonNetcdfNames;
    }

    @Override
    public String[] getNetcdfFile() {
        return planktonFileListNetcdf;
    }
}
