/* 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
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
package fr.ird.osmose.grid;

import fr.ird.osmose.Cell;
import java.io.IOException;
import ucar.nc2.NetcdfFile;

/**
 * This class creates the Osmose grid from a NetCDF grid file. It is the
 * recommended way to define a grid since Osmose version 3. The function reads
 * longitude, latitude and mask fields from a NetCDF grid file. Osmose assumes
 * that the three fields are two dimensions double arrays. It also expects the
 * latitude and longitude fields to be respectively in degree north and degree
 * east. The mask field must have either zero or negative values for the land.
 * The function does not expect the NetCDF grid file to respect any NetCDF
 * convention (e.g. CF or other OGC convention) for reading it. This should be
 * improved in future release, by the use of GeoToolKit for instance.
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0b 2013/09/01
 */
public class NcGrid extends AbstractGrid {

    /**
     * Path of the NetCDF grid file. Either relative or absolute. Parameter
     * <i>grid.netcdf.file</i>.
     */
    private String gridFile;
    /**
     * Name of the longitude variable in the NetCDF grid file. Parameter
     * <i>grid.var.lon</i>.
     */
    private String strLon;
    /**
     * Name of the latitude variable in the NetCDF grid file. Parameter
     * <i><grid.var.lat/i>.
     */
    private String strLat;
    /**
     * Name of the mask variable in the NetCDF grid file. Parameter
     * <i>grid.var.mask</i>.
     */
    private String strMask;

    @Override
    void readParameters() {
        this.gridFile = getConfiguration().getFile("grid.netcdf.file");
        this.strLat = getConfiguration().getString("grid.var.lat");
        this.strLon = getConfiguration().getString("grid.var.lon");
        this.strMask = getConfiguration().getString("grid.var.mask");
    }

    /**
     * Reads the latitude, longitude and mask fields from the NetCDF grid file
     * and creates the {@code Cell} accordingly to the values of these fields.
     *
     * @return an array of {@code Cell} that represents the grid
     */
    @Override
    Cell[][] makeGrid() {

        NetcdfFile ncGrid = openNetcdfFile(gridFile);
        double[][] lon = readVariable(ncGrid, strLon);
        double[][] lat = readVariable(ncGrid, strLat);

        double[][] mask = readVariable(ncGrid, strMask);
        int ny = lon.length;
        int nx = lon[0].length;
        Cell[][] grid = new Cell[ny][nx];
        for (int j = 0; j < ny; j++) {
            for (int i = 0; i < nx; i++) {
                boolean land = (mask[j][i] <= 0);
                grid[j][i] = new Cell((j * nx + i), i, j, (float) lat[j][i], (float) lon[j][i], land);
            }
        }

        return grid;
    }

    /**
     * Opens the NetCDF grid file.
     *
     * @param gridFile, the path of the NetCDF grid file
     * @return a {@code NetcdfFile} object
     */
    private NetcdfFile openNetcdfFile(String gridFile) {

        try {
            return NetcdfFile.open(gridFile, null);
        } catch (IOException ex) {
            error("Failed to open NetCDF grid file " + gridFile, ex);
        }
        return null;
    }

    /**
     * Load a NetCDF variable from the NetCDF grid file. The function assumes
     * that the variables is a two-dimensional double array.
     *
     * @param nc, the {@code NetcdfFile} that contains the variable
     * @param varname, the name of the variable in the NetCDF file
     * @return the values of the variable as a two-dimensional array of double
     */
    private double[][] readVariable(NetcdfFile nc, String varname) {
        try {
            return (double[][]) nc.findVariable(varname).read().copyToNDJavaArray();
        } catch (IOException ex) {
            error("Error while reading variable " + varname + " in NetCDF grid file " + nc.getLocation(), ex);
        }
        return null;
    }
}