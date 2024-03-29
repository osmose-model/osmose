/*
 *
 * OSMOSE (Object-oriented Simulator of Marine Ecosystems)
 * http://www.osmose-model.org
 *
 * Copyright (C) IRD (Institut de Recherche pour le Développement) 2009-2020
 *
 * Osmose is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, additional and
 * starvation mortalities, reproduction and migration) and fishing mortalities
 * (Shin and Cury 2001, 2004).
 *
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License). Full description
 * is provided on the LICENSE file.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package fr.ird.osmose.grid;

import fr.ird.osmose.Cell;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDatasets;

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

    /**
     * Name of the mask variable in the NetCDF grid file. Parameter
     * <i>grid.var.surf</i>.
     */
    private String strSurf = null;

    @Override
    void readParameters() {
        this.gridFile = getConfiguration().getFile("grid.netcdf.file");
        this.strLat = getConfiguration().getString("grid.var.lat");
        this.strLon = getConfiguration().getString("grid.var.lon");
        this.strMask = getConfiguration().getString("grid.var.mask");
        if(this.getConfiguration().canFind("grid.var.surf")) {
            // If a surface string parameter is found, assumes surface
            // is read.
            this.setReadSurf(true);
            this.strSurf = this.getConfiguration().getString("grid.var.surf");
        }
    }

    /**
     * Reads the latitude, longitude and mask fields from the NetCDF grid file
     * and creates the {@code Cell} accordingly to the values of these fields.
     *
     * @return an array of {@code Cell} that represents the grid
     */
    @Override
    Cell[][] makeGrid() {

        // true if lon array is 1D (lon), else assumed to be 2D
        boolean isCoord1D;

        // Init index and netcdf arrays
        Index surfIndex = null;
        Array arrLon = null;
        Array arrLat = null;
        Array arrMask = null;

        NetcdfFile ncGrid = openNetcdfFile(gridFile);

        try {
            arrLon = ncGrid.findVariable(strLon).read().reduce();
            arrLat = ncGrid.findVariable(strLat).read().reduce();
            arrMask = ncGrid.findVariable(strMask).read().reduce();
        } catch (IOException ex) {
            Logger.getLogger(NcGrid.class.getName()).log(Level.SEVERE, null, ex);
        }

        isCoord1D = (arrLon.getRank() == 1);

        int ndims = arrMask.getRank();
        int ny = arrMask.getShape()[ndims - 2];
        int nx = arrMask.getShape()[ndims - 1];

        if(this.isReadSurf()) {
            try {
                // if the surf string has been set,
                // read surface from NetCDF.
                Array arrSurf = ncGrid.findVariable(strSurf).read().reduce();
                surfIndex = arrSurf.getIndex();
            } catch (IOException ex) {
                Logger.getLogger(NcGrid.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        Index maskIndex = arrMask.getIndex();
        Index lonIndex = arrLon.getIndex();
        Index latIndex = arrLat.getIndex();

        Cell[][] grid = new Cell[ny][nx];
        for (int j = 0; j < ny; j++) {
            for (int i = 0; i < nx; i++) {
                if (isCoord1D) {
                    lonIndex.set(i);
                    latIndex.set(j);
                } else {
                    lonIndex.set(j, i);
                    latIndex.set(j, i);
                }

                boolean land = false;
                double maskVal;
                switch (ndims) {
                    case 2:
                        // if the mask is 2D (lat, lon)
                        maskIndex.set(j, i);
                        maskVal = arrMask.getDouble(maskIndex);
                        land = (maskVal <= 0) || (Double.isNaN(maskVal));
                        break;
                    case 3:
                        // if the mask is 3D (time, lat, lon)
                        land = false;
                        int nz = arrMask.getShape()[0];
                        for (int k = 0; k < nz; k++) {
                            maskIndex.set(k, j, i);
                            maskVal = arrMask.getDouble(maskIndex);
                            land = land || (maskVal <= 0) || (Double.isNaN(maskVal));
                            if(land) {
                                break;
                            }
                        }
                        break;
                    default:
                        IOException ex = new IOException("Mask variable must either be 2D or 3D, not " + ndims + "D");
                        Logger.getLogger(NcGrid.class.getName()).log(Level.SEVERE, null, ex);

                }

                double tmpLat = arrLat.getDouble(latIndex);
                double tmpLon = arrLon.getDouble(lonIndex);
                grid[j][i] = new Cell((j * nx + i), i, j, (float) tmpLat, (float) tmpLon, land);
                if (this.strSurf != null) {
                    surfIndex.set(j, i);
                    double surf = arrLat.getDouble(surfIndex);
                    grid[j][i].setSurface((float) surf);
                }

            }
        }

        this.closeNetcdfFile(ncGrid);

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
            return NetcdfDatasets.openDataset(gridFile);
        } catch (IOException ex) {
            error("Failed to open NetCDF grid file " + gridFile, ex);
        }
        return null;
    }

       /**
     * Opens the NetCDF grid file.
     *
     * @param gridFile, the path of the NetCDF grid file
     * @return a {@code NetcdfFile} object
     */
    private void closeNetcdfFile(NetcdfFile nc) {

        try {
            nc.close();
        } catch (IOException ex) {
            error("Failed to close NetCDF grid file " + nc.getLocation(), ex);
        }
    }

}