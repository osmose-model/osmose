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
package fr.ird.osmose.grid;

import fr.ird.osmose.Cell;
import fr.ird.osmose.util.OsmoseLinker;
import fr.ird.osmose.util.io.IOTools;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

/**
 * This abstract class implements the function listed in the interface
 * {@code IGrid}. It limits the task of coding a new grid to reading the
 * parameters ({@link AbstractGrid#readParameters()}) and making the grid
 * ({@link AbstractGrid#makeGrid()}). Any new type of grid in Osmose should
 * extends an {@code AbstractGrid}.
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0b 2013/09/01
 */
public abstract class AbstractGrid extends OsmoseLinker implements IGrid {

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
    @Override
    public void init() {
        readParameters();
        matrix = makeGrid();
        getDimGeogArea();
    }

    @Override
    public Cell getCell(int i, int j) {
        return matrix[j][i];
    }

    @Override
    public Cell getCell(int index) {
        int j = index / nx;
        int i = index - j * nx;
        return matrix[j][i];
    }

    @Override
    public List<Cell> getCells() {
        ArrayList<Cell> cells = new ArrayList(ny * nx);
        for (int j = ny; j-- > 0;) {
            for (int i = nx; i-- > 0;) {
                cells.add(matrix[j][i]);
            }
        }
        return cells;
    }

    @Override
    public ArrayList<Cell> getNeighbourCells(Cell cell, int range) {

        int im1 = Math.max(cell.get_igrid() - range, 0);
        int ip1 = Math.min(cell.get_igrid() + range, get_nx() - 1);
        int jm1 = Math.max(cell.get_jgrid() - range, 0);
        int jp1 = Math.min(cell.get_jgrid() + range, get_ny() - 1);

        ArrayList<Cell> neighbours = new ArrayList();

        for (int i = im1; i <= ip1; i++) {
            for (int j = jm1; j <= jp1; j++) {
                neighbours.add(matrix[j][i]);
            }
        }

        return neighbours;
    }

    @Override
    public int getNOceanCell() {
        int nbCells = 0;

        for (int j = 0; j < ny; j++) {
            for (int i = 0; i < nx; i++) {
                if (!matrix[j][i].isLand()) {
                    nbCells++;
                }
            }
        }
        return nbCells;
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
    }

    @Override
    public int get_ny() {
        return ny;
    }

    @Override
    public int get_nx() {
        return nx;
    }

    @Override
    public float getLatMax() {
        return latMax;
    }

    @Override
    public float getLatMin() {
        return latMin;
    }

    @Override
    public float getLongMax() {
        return longMax;
    }

    @Override
    public float getLongMin() {
        return longMin;
    }

    @Override
    public float getdLat() {
        return dLat;
    }

    @Override
    public float getdLong() {
        return dLong;
    }

    @Override
    public void toNetCDF(String filename) {

        NetcdfFileWriteable nc = null;
        /*
         * Create NetCDF file
         */
        try {
            nc = NetcdfFileWriteable.createNew("");
            IOTools.makeDirectories(filename);
            nc.setLocation(filename);
        } catch (IOException ex) {
            error("Failed to created NetCDF grid file " + filename, ex);
        }
        /*
         * Create dimensions
         */
        Dimension nxDim = nc.addDimension("nx", nx);
        Dimension nyDim = nc.addDimension("ny", ny);
        /*
         * Add variables
         */
        nc.addVariable("latitude", DataType.DOUBLE, new Dimension[]{nyDim, nxDim});
        nc.addVariableAttribute("latitude", "units", "north degree");
        nc.addVariableAttribute("latitude", "description", "latitude of the center of the cell");
        nc.addVariable("longitude", DataType.DOUBLE, new Dimension[]{nyDim, nxDim});
        nc.addVariableAttribute("longitude", "units", "south degree");
        nc.addVariableAttribute("longitude", "description", "longitude of the center of the cell");
        nc.addVariable("mask", DataType.DOUBLE, new Dimension[]{nyDim, nxDim});
        nc.addVariableAttribute("mask", "units", "boolean");
        nc.addVariableAttribute("mask", "description", "mask of the grid, one means ocean and zero means continent");
        try {
            /*
             * Validates the structure of the NetCDF file.
             */
            nc.create();
            /*
             * Writes variable longitude and latitude
             */
            ArrayDouble.D2 arrLon = new ArrayDouble.D2(get_ny(), get_nx());
            ArrayDouble.D2 arrLat = new ArrayDouble.D2(get_ny(), get_nx());
            ArrayDouble.D2 arrMask = new ArrayDouble.D2(get_ny(), get_nx());
            for (Cell cell : getCells()) {
                arrLon.set(cell.get_jgrid(), cell.get_igrid(), cell.getLon());
                arrLat.set(cell.get_jgrid(), cell.get_igrid(), cell.getLat());
                arrMask.set(cell.get_jgrid(), cell.get_igrid(), cell.isLand() ? 0.d : 1.d);
            }
            nc.write("longitude", arrLon);
            nc.write("latitude", arrLat);
            nc.write("mask", arrMask);
        } catch (IOException ex) {
            error("Failed to write the NetCDF grid file", ex);
        } catch (InvalidRangeException ex) {
            error("Failed to write the NetCDF grid file", ex);
        }
        /*
         * CLose the NetCDF file 
         */
        try {
            nc.close();
        } catch (IOException ex) {
            // do nothing
        }
    }
}
