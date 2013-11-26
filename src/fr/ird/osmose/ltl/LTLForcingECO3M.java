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
    private int stride;

    public LTLForcingECO3M(int rank) {
        super(rank);
    }

    @Override
    public void readParameters() {

        plktonNetcdfNames = new String[getConfiguration().getNPlankton()];
        for (int i = 0; i < getConfiguration().getNPlankton(); i++) {
            plktonNetcdfNames[i] = getConfiguration().getString("ltl.netcdf.var.plankton.plk" + i);
        }

        planktonFileListNetcdf = new String[getConfiguration().findKeys("ltl.netcdf.file.t*").size()];
        for (int i = 0; i < planktonFileListNetcdf.length; i++) {
            planktonFileListNetcdf[i] = getConfiguration().getFile("ltl.netcdf.file.t" + i);
        }

        zlevelName = getConfiguration().getString("ltl.netcdf.var.zlevel");
        // Osmose grid stride
        stride = getConfiguration().getInt("grid.stride");

    }

    @Override
    public void initLTLGrid() {
        try {
            String gridFilename = planktonFileListNetcdf[0];
            NetcdfFile ncGrid = NetcdfFile.open(gridFilename, null);
            
            
            int[] shape = ncGrid.findVariable(zlevelName).getShape();
            int nz = shape[0];
            int ny = shape[1];
            int nx = shape[2];
            
            depthOfLayer = new float[nz][ny][nx];
            
            ArrayDouble.D3 arrDepth = (ArrayDouble.D3) ncGrid.findVariable(zlevelName).read();
            for (int i = 0; i < nx; i++) {
                for (int j = 0; j < ny; j++) {
                    for (int z = 0; z < nz; z++) {
                        depthOfLayer[z][j][i] = (float) arrDepth.get(z, j, i);
                    }
                }
            }
            ncGrid.close();
            
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
                            icoordLTLGrid[j][i].add(i * stride + jj);
                            jcoordLTLGrid[j][i].add(j * stride + ii);
                        }
                    }
                }
            }
        } catch (IOException ex) {
            error("Error while reading the LTL grid", ex);
        }
    }

    @Override
    float[][] getRawBiomass(int iPlankton, int iStepSimu) {

        float[][][] dataInit = null;
        NetcdfFile nc = null;
        String name = planktonFileListNetcdf[getIndexStepLTL(iStepSimu)];
        ArrayDouble.D3 tempArray;

        try {
            nc = NetcdfFile.open(name);
            // read data and put them in the local arrays
            tempArray = (ArrayDouble.D3) nc.findVariable(plktonNetcdfNames[iPlankton]).read();
            int[] shape = tempArray.getShape();
            dataInit = new float[shape[0]][shape[1]][shape[2]];
                    
            // fill dataInit of plankton classes from local arrays
            for (int i = 0; i < shape[2]; i++) {
                for (int j = 0; j < shape[1]; j++) {
                    for (int k = 0; k < shape[0]; k++) {
                        dataInit[k][j][i] = (float) tempArray.get(k, j, i);
                    }
                }
            }
        } catch (IOException e) {
            error("Error reading variable " + plktonNetcdfNames[iPlankton] + " from file " + name, e);
        } finally {
            if (nc != null) {
                try {
                    nc.close();
                } catch (IOException ioe) {
                    // do nothing
                }
            }
        }

        // vertical integration
        return verticalIntegration(dataInit, depthOfLayer, getConfiguration().getFloat("ltl.integration.depth"));
    }
}
