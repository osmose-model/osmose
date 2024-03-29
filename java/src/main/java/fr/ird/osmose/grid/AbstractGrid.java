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

import java.util.ArrayList;
import java.util.List;

import fr.ird.osmose.Cell;
import fr.ird.osmose.util.OsmoseLinker;

/**
 * This abstract class details the functions that an Osmose grid needs to
 * implement. The grid is defined in the Cartesian coordinate system, with the
 * origin of the grid on the bottom left corner, the x-axis pointing toward the
 * right and the y-axis towards the top. It limits the task of coding a new grid
 * to reading the parameters ({@link AbstractGrid#readParameters()}) and making
 * the grid ({@link AbstractGrid#makeGrid()}). Any new type of grid in Osmose
 * should extends an {@code AbstractGrid}.
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0b 2013/09/01
 */
public abstract class AbstractGrid extends OsmoseLinker {

///////////////////////////////
// Declaration of the variables
///////////////////////////////
    /**
     * The array of cells constituting the grid.
     */
    private Cell[][] matrix;
    /**
     * Dimension of the grid along the y-axis. Number of lines.
     */
    private int ny;
    /**
     * Dimension of the grid along the x-axis. Number of columns.
     */
    private int nx;
    /**
     * Number of ocean cells
     */
    private int nOceanCell;
    /**
     * List of cells
     */
    ArrayList<Cell> cells;
    /**
     * List of ocean cells only
     */
    ArrayList<Cell> oceanCells;
    /**
     * Latitude, in degree north, of the North West corner of the grid.
     */
    private float latMax;
    /**
     * Latitude, in degree north, of the South East corner of the grid.
     */
    private float latMin;
    /**
     * Longitude, in degree east, of the South East corner of the grid.
     */
    private float longMax;
    /**
     * Longitude, in degree east, of the North West corner of the grid.
     */
    private float longMin;
    /**
     * Meridional size, in degree, of a cell (delta latitude).
     */
    private float dLat;
    /**
     * Zonal size, in degree, of a cell (delta longitude).
     */
    private float dLong;

    private boolean readSurf = false;

/////////////////////////////////////
// Definition of the abstract methods
/////////////////////////////////////
    /**
     * Creates an array of {@code Cell} that represents the grid.
     *
     * @return an array of {@code Cell} that represents the grid
     */
    abstract Cell[][] makeGrid();

    /**
     * Read the parameters of the grid in the configuration file.
     */
    abstract void readParameters();

////////////////////////////
// Definition of the methods
////////////////////////////
    /**
     * Initialises the grid. Reads the input parameters and makes the grid. It
     * must be called after creating a new instance of the grid.
     */
    public void init() {
        readParameters();
        matrix = makeGrid();
        // geographical extent
        getDimGeogArea();
        // compute number of ocean cells
        nOceanCell = 0;
        for (int j = 0; j < ny; j++) {
            for (int i = 0; i < nx; i++) {
                if (!matrix[j][i].isLand()) {
                    nOceanCell++;
                }
            }
        }
        // List of cells and ocean cells
        cells = new ArrayList<>(ny * nx);
        oceanCells = new ArrayList<>(nOceanCell);
        for (int j = ny; j-- > 0;) {
            for (int i = nx; i-- > 0;) {
                cells.add(matrix[j][i]);
                if (!matrix[j][i].isLand()) {
                    oceanCells.add(matrix[j][i]);
                }
            }
        }
    }

    /**
     * Get the grid cell at index (i, j)
     *
     * @param i, index i of the cell
     * @param j, index j of the cell
     * @return Cell(i, j)
     */
    public Cell getCell(int i, int j) {
        return matrix[j][i];
    }

    /**
     * Get the grid cell at specified index. The index of the cell is defined
     * such as {@code index = j * nx + i} with nx being the dimension of the
     * grid in the x-direction (number of columns) and {i, j} the coordinates of
     * the cell in the Cartesian system.
     *
     * @param index of the cell
     * @return Cell(index)
     */
    public Cell getCell(int index) {
        int j = index / nx;
        int i = index - j * nx;
        return matrix[j][i];
    }

    /**
     * Get a list of the cells.
     *
     * @return a List of the cells.
     */
    public List<Cell> getCells() {
        return cells;
    }

    /**
     * Get a list of the cells.
     *
     * @return a List of the cells.
     */
    public List<Cell> getOceanCells() {
        return oceanCells;
    }

    /**
     * Get the adjacent cells of a given cell (including itself) within a given
     * range of cells.
     *
     * For instance, for {@code cell(i, j)} with a range of 1, the function
     * returns 8 surrounding cells:
     * <ul>
     * <li>{@code cell(i - 1, j - 1)}</li>
     * <li>{@code cell(i - 1, j)}</li>
     * <li>{@code cell(i - 1, j + 1)}</li>
     * <li>{@code cell(i, j - 1)}</li>
     * <li>{@code cell(i, j + 1)}</li>
     * <li>{@code cell(i + 1, j - 1)}</li>
     * <li>{@code cell(i + 1, j)}</li>
     * <li>{@code cell(i + 1, j + 1)}</li>
     * </ul>
     * For cells at the edge of the grid, only returns 3 or 5 cells.
     *
     * @param cell, the cell for which to get the neighbors
     * @param range, and integer, the range of the neighborhood
     * @return an ArrayList of the cells surrounding <code>cell</code>
     */
    public ArrayList<Cell> getNeighbourCells(Cell cell, int range) {

        int im1 = Math.max(cell.get_igrid() - range, 0);
        int ip1 = Math.min(cell.get_igrid() + range, get_nx() - 1);
        int jm1 = Math.max(cell.get_jgrid() - range, 0);
        int jp1 = Math.min(cell.get_jgrid() + range, get_ny() - 1);

        ArrayList<Cell> neighbours = new ArrayList<>();

        for (int i = im1; i <= ip1; i++) {
            for (int j = jm1; j <= jp1; j++) {
                neighbours.add(matrix[j][i]);
            }
        }

        return neighbours;
    }

    /**
     * Returns the number of cells of the grid that are in the ocean.
     *
     * @return the number of cells of the grid that are in the ocean
     */
    public int getNOceanCell() {
        return nOceanCell;
    }

    /**
     * Computes longMin, latMin, longMax, latMax and computes the size of a cell
     * assuming that we work in a regular grid.
     */
    private void getDimGeogArea() {

        /*
         * Set nbLines and nbColumns. It is useless for OriginalGrid since it is
         * a parameter given by the user. But it has to be done for the Netcdf
         * grids.
         */
        ny = matrix.length;
        nx = matrix[0].length;

        //--------------------------------------
        // Calculate the Physical Space extrema
        longMin = Float.MAX_VALUE;
        longMax = -1.f * Float.MAX_VALUE;
        latMin = Float.MAX_VALUE;
        latMax = -1.f * Float.MAX_VALUE;
        int j = ny;

        while (j-- > 0) {
            int i = nx;
            while (i-- > 0) {
                if (matrix[j][i].getLon() >= longMax) {
                    longMax = matrix[j][i].getLon();
                }
                if (matrix[j][i].getLon() <= longMin) {
                    longMin = matrix[j][i].getLon();
                }
                if (matrix[j][i].getLat() >= latMax) {
                    latMax = matrix[j][i].getLat();
                }
                if (matrix[j][i].getLat() <= latMin) {
                    latMin = matrix[j][i].getLat();
                }
            }
        }
        //System.out.println("lonmin " + longMin + " lonmax " + longMax + " latmin " + latMin + " latmax " + latMax);
        //System.out.println("depth max " + depthMax);

        float float_tmp;
        if (longMin > longMax) {
            float_tmp = longMin;
            longMin = longMax;
            longMax = float_tmp;
        }

        if (latMin > latMax) {
            float_tmp = latMin;
            latMin = latMax;
            latMax = float_tmp;
        }

        /*
         * Average size of a cell
         */
        dLat = (latMax - latMin) / (float) ny;
        dLong = (longMax - longMin) / (float) nx;

        if(!this.readSurf) {
            for(int i=0; i<nx; i++) {
                 for(j=0; j<ny; j++) {
                     double surf = this.computeSurface(matrix[j][i].getLat(), matrix[j][i].getLon());
                     matrix[j][i].setSurface((float) surf);
                 }
            }
        }

    }

    /**
     * Returns the dimension of the grid along the y-axis. The number of lines.
     *
     * @return the number of lines
     */
    public int get_ny() {
        return ny;
    }

    /**
     * Returns the dimension of the grid along the y-axis. The number of
     * columns.
     *
     * @return the number of columns
     */
    public int get_nx() {
        return nx;
    }

    /**
     * Returns the latitude, in degree north, of the North West corner of the
     * grid.
     *
     * @return the latitude, in degree north, of the North West corner of the
     * grid
     */
    public float getLatMax() {
        return latMax;
    }

    /**
     * Return the latitude, in degree north, of the South East corner of the
     * grid.
     *
     * @return the latitude, in degree north, of the South East corner of the
     * grid
     */
    public float getLatMin() {
        return latMin;
    }

    /**
     * Returns the longitude, in degree east, of the South East corner of the
     * grid.
     *
     * @return the longitude, in degree east, of the South East corner of the
     * grid
     */
    public float getLongMax() {
        return longMax;
    }

    /**
     * Returns the longitude, in degree east, of the North West corner of the
     * grid.
     *
     * @return the longitude, in degree east, of the North West corner of the
     * grid
     */
    public float getLongMin() {
        return longMin;
    }

    /**
     * Returns the meridional size, in degree, of a cell. (delta latitude)
     *
     * @return the dLat of a cell, in degree
     */
    public float getdLat() {
        return dLat;
    }

    /**
     * Returns the zonal size, in degree, of a cell. (delta longitude)
     *
     * @return the dLong of a cell, in degree
     */
    public float getdLong() {
        return dLong;
    }

    /** Computes the surface of a cell on a regular grid.
     *
     *
     * @param lat Latitude (degrees)
     * @param lon Longitude (degrees)
     * @return Surface (m2)
     */
    public double computeSurface(double lat, double lon) {

        float dlat = getGrid().getdLat();
        float dlon = getGrid().getdLong();

        // Earth radius in m
        double Rt = 6371 * 1e3;
        double surf = Rt * deg2rad(dlat) * Rt * deg2rad(dlon) * Math.cos(deg2rad(lat));
        return surf;
    }

    /**
     * Converts a degree angle into radian.
     *
     * @param value
     * @return
     */
    private double deg2rad(double value) {
        return value * Math.PI / 180.;
    }

    public boolean isReadSurf() {
        return this.readSurf;
    }

    public void setReadSurf(boolean readSurf) {
        this.readSurf = readSurf;
    }

}
