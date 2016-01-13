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

import fr.ird.osmose.Cell;
import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import ucar.ma2.ArrayDouble;
import ucar.nc2.NetcdfFile;

/**
 *
 * @author pverley
 */
public class LTLForcingECO3M extends AbstractLTLForcing {

    private String[] planktonFileListNetcdf;
    private String zlevelName;
    private float[][][] depthLevel;
    private String[] plktonNetcdfNames;
    private int stride;
    /**
     * List of LTL cells that are contained within Osmose cells. The map is
     * indexed by Osmose cell index.
     */
    private HashMap<Integer, List<Point>> ltlCells;

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
    public void initLTL() {
        try {
            String gridFilename = planktonFileListNetcdf[0];
            NetcdfFile ncGrid = NetcdfFile.open(gridFilename, null);

            int[] shape = ncGrid.findVariable(zlevelName).getShape();
            int nz = shape[0];
            int ny = shape[1];
            int nx = shape[2];

            depthLevel = new float[ny][nx][nz];

            ArrayDouble.D3 arrDepth = (ArrayDouble.D3) ncGrid.findVariable(zlevelName).read();
            for (int i = 0; i < nx; i++) {
                for (int j = 0; j < ny; j++) {
                    for (int z = 0; z < nz; z++) {
                        depthLevel[j][i][z] = (float) arrDepth.get(z, j, i);
                    }
                }
            }
            ncGrid.close();

            ltlCells = new HashMap();
            for (int j = 0; j < getGrid().get_ny(); j++) {
                for (int i = 0; i < getGrid().get_nx(); i++) {
                    Cell cell = getGrid().getCell(i, j);
                    if (!cell.isLand()) {
                        if (!ltlCells.containsKey(cell.getIndex())) {
                            ltlCells.put(cell.getIndex(), new ArrayList());
                        }
                        for (int ii = 0; ii < stride; ii++) {
                            for (int jj = 0; jj < stride; jj++) {
                                int iLTL = i * stride + jj;
                                int jLTL = j * stride + ii;
                                // Only add ECO3M ocean cells
                                // I use depthlevel values as a proxy for the mask
                                // Land values are usually -9999
                                if (Math.abs(depthLevel[jLTL][iLTL][0]) < 6e3) {
                                    ltlCells.get(cell.getIndex()).add(new Point(iLTL, jLTL));
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            error("Error while reading the LTL grid", ex);
        }
    }

    @Override
    float[] getDepthLevel(int iLTL, int jLTL) {
        return depthLevel[jLTL][iLTL];
    }

    @Override
    float[][][] getRawBiomass(int iPlankton, int iStepSimu) {

        float[][][] rawBiomass = null;
        NetcdfFile nc = null;
        String name = planktonFileListNetcdf[getIndexStepLTL(iStepSimu)];

        try {
            // Open NetCDF file
            nc = NetcdfFile.open(name);
            // Read LTL biomass in NetCDF array
            ArrayDouble.D3 array = (ArrayDouble.D3) nc.findVariable(plktonNetcdfNames[iPlankton]).read();
            // Get the shape of the LTL variable
            int[] shape = array.getShape();
            // Permute the dimensions in order to have rawBiomass[j][i][k]
            rawBiomass = new float[shape[1]][shape[2]][shape[0]];
            // Fill up the rawBiomass variable
            for (int i = 0; i < shape[2]; i++) {
                for (int j = 0; j < shape[1]; j++) {
                    for (int k = 0; k < shape[0]; k++) {
                        rawBiomass[j][i][k] = (float) array.get(k, j, i);
                    }
                }
            }
        } catch (IOException e) {
            error("Error reading variable " + plktonNetcdfNames[iPlankton] + " from file " + name, e);
        } finally {
            // Close the NetCDF file
            if (nc != null) {
                try {
                    nc.close();
                } catch (IOException ioe) {
                    // do nothing
                }
            }
        }

        // vertical integration
        return rawBiomass;
    }

    @Override
    List<Point> getLTLCells(Cell cell) {
        return ltlCells.get(cell.getIndex());
    }
}
