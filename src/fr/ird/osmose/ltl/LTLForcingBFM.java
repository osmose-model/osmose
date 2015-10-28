/*
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, natural and
 * starvation mortalities, reproduction and migration) and fishing mortalities
 * (Shin and Cury 2001, 2004).
 * 
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */
package fr.ird.osmose.ltl;

import java.io.IOException;
import java.util.ArrayList;
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
    private int stride;

    public LTLForcingBFM(int rank) {
        super(rank);
    }

    @Override
    public void readParameters() {

        planktonNetcdfNames = new String[getConfiguration().getNPlankton()];
        for (int i = 0; i < getConfiguration().getNPlankton(); i++) {
            planktonNetcdfNames[i] = getConfiguration().getString("ltl.netcdf.var.plankton.plk" + i);
        }

        planktonFileListNetcdf = new String[getConfiguration().findKeys("ltl.netcdf.file.t*").size()];
        for (int i = 0; i < planktonFileListNetcdf.length; i++) {
            planktonFileListNetcdf[i] = getConfiguration().getFile("ltl.netcdf.file.t" + i);
        }

        timeDim = getConfiguration().getInt("ltl.netcdf.dim.ntime");
        bathyFile = getConfiguration().getFile("grid.netcdf.file");
        zlevelName = getConfiguration().getString("ltl.netcdf.var.zlevel");
        bathyName = getConfiguration().getString("ltl.netcdf.var.bathy");
        // Osmose grid stride
        stride = getConfiguration().getInt("grid.stride");
    }

    @Override
    public void initLTLGrid() {

        try {
            /*
             * Open BFM temperature file that contains bathymetry variable
             */
            NetcdfFile nc = NetcdfFile.open(bathyFile, null);

            /*
             * Read the zlevel variable
             */
            float[] zlevel = (float[]) nc.findVariable(zlevelName).read().copyToNDJavaArray();
            /*
             * Read the BFM grid dimensions
             */
            /*
             * Reads the BFM grid dimensions
             */
            km = nc.findDimension("zpos").getLength() - 1;
            jm = nc.findDimension("ypos").getLength();
            im = nc.findDimension("xpos").getLength();
            /*
             * Read the bathymetry variable
             */
            float[][] bathy = (float[][]) nc.findVariable(bathyName).read().copyToNDJavaArray();
            nc.close();
            /*
             * Compute the depth of every cell in meter
             */
            depthOfLayer = new float[km][jm][im];
            for (int z = 0; z < km; z++) {
                for (int j = 0; j < jm; j++) {
                    for (int i = 0; i < im; i++) {
                        depthOfLayer[z][j][i] = bathy[j][i] * zlevel[z];
                    }
                }
            }
            /*
             * Associate osmose cells to BFM cells
             */
            icoordLTLGrid = new ArrayList[getGrid().get_ny()][getGrid().get_nx()];
            jcoordLTLGrid = new ArrayList[getGrid().get_ny()][getGrid().get_nx()];
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
            error("Error reading BFM grid from file " + bathyFile, ex);
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
        String strMask = getConfiguration().getString("grid.var.mask");
        NetcdfFile nc = NetcdfFile.open(gridFile, null);
        float[][] mask = (float[][]) nc.findVariable(strMask).read().copyToNDJavaArray();

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
    double[][] getRawBiomass(int iPlankton, int iStepSimu) {

        String name = planktonFileListNetcdf[getIndexStepLTL(iStepSimu)];

        float[][][] data3d = new float[km][jm][im];
        try {
            /*
             * Open the BFM Plankton NetCDF file
             */

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
            error("Error loading plankton variable " + planktonNetcdfNames[iPlankton] + " from file " + name, ex);
        } catch (IOException ex) {
            error("Error loading plankton variable " + planktonNetcdfNames[iPlankton] + " from file " + name, ex);
        }


        /*
         * Integrate plankton biomass on vertical dimension
         */
        return LTLUtil.verticalIntegration(data3d, depthOfLayer, getConfiguration().getFloat("ltl.integration.depth"));
    }

    @Override
    public int getIndexStepLTL(int iStepSimu) {
        return (iStepSimu % getConfiguration().getNStepYear()) / timeDim;
    }
}
