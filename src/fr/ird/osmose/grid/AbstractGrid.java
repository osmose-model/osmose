/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.grid;

import fr.ird.osmose.Cell;
import fr.ird.osmose.OldConfiguration;
import fr.ird.osmose.Osmose;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author pverley
 */
public abstract class AbstractGrid implements IGrid {

    ///////////////////////////////
// Declaration of the variables
///////////////////////////////
    /*
     * The array of cells
     */
    private Cell[][] matrix;
    /*
     * Number of lines
     */
    private int ny;
    /*
     * Number od columns
     */
    private int nx;
    /*
     * Latitude °N of upper left corner of the grid
     */
    private float latMax;
    /*
     * Latitude °N of lower right corner of the grid
     */
    private float latMin;
    /*
     * Longitude °E of lower right corner of the grid
     */
    private float longMax;
    /*
     * Longitude °E of upper left corner of the grid
     */
    private float longMin;
    /*
     * Latitudinal dimension of one cell
     */
    private float dLat;
    /*
     * Longitudinal dimension of one cell
     */
    private float dLong;

/////////////////////////////////////
// Definition of the abstract methods
/////////////////////////////////////
    abstract Cell[][] makeGrid();

    abstract void readParameters();

////////////////////////////
// Definition of the methods
////////////////////////////

    /*
     * Must be called after creating a new instance of grid.
     */
    @Override
    public void init() {
        readParameters();
        matrix = makeGrid();
        getDimGeogArea();
    }

    /**
     * Get the grid cell at index (i, j)
     *
     * @param i, index i of the cell
     * @param j, index j of the cell
     * @return Cell(i, j)
     */
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

    /**
     * Get a list of the cells.
     *
     * @return a List<Cell> of the cells.
     */
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

    /**
     * Get the adjacent cells of a given cell (cell included) within a given
     * range of cells. Cells are randomly sorted.
     *
     * @see Collections.shuffle() For cell(i, j) returns 8 surrounding cells:
     * cell(i - 1, j - 1) cell(i - 1, j) cell(i - 1, j + 1) cell(i, j - 1)
     * cell(i, j + 1) cell(i + 1, j - 1) cell(i + 1, j) cell(i + 1, j + 1) For
     * cells at the edge of the grid, only returns 3 or 5 cells.
     * @param cell
     * @param range, and integer, the range of the neighbourhood
     * @return an ArrayList of the cells surrounding <code>cell</code>
     */
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
        //neighbors.remove(cell);
        /*
         * Random sorting of the adjacent cells
         */
        Collections.shuffle(neighbours);
        return neighbours;
    }

    /*
     * Returns the number of cells of the grid that are not inland.
     */
    @Override
    public int getNumberAvailableCells() {
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
     * Computes longMin, latMin, longMax, latMax. Computes the dimension of a
     * cell, assuming that we work in a regular grid
     */
    private void getDimGeogArea() {

        /*
         * Set nbLines and nbColumns Useless for Osmose grid since it is a
         * parameter given by the user But has to be done for the Netcdf grids.
         */
        ny = matrix.length;
        nx = matrix[0].length;

        //--------------------------------------
        // Calculate the Physical Space extrema

        setLongMin(Float.MAX_VALUE);
        setLongMax(-longMin);
        setLatMin(Float.MAX_VALUE);
        setLatMax(-latMin);
        int j = ny;

        while (j-- > 0) {
            int i = nx;
            while (i-- > 0) {
                if (matrix[j][i].getLon() >= longMax) {
                    setLongMax(matrix[j][i].getLon());
                }
                if (matrix[j][i].getLon() <= longMin) {
                    setLongMin(matrix[j][i].getLon());
                }
                if (matrix[j][i].getLat() >= latMax) {
                    setLatMax(matrix[j][i].getLat());
                }
                if (matrix[j][i].getLat() <= latMin) {
                    setLatMin(matrix[j][i].getLat());
                }
            }
        }
        //System.out.println("lonmin " + longMin + " lonmax " + longMax + " latmin " + latMin + " latmax " + latMax);
        //System.out.println("depth max " + depthMax);

        float float_tmp;
        if (longMin > longMax) {
            float_tmp = longMin;
            setLongMin(longMax);
            setLongMax(float_tmp);
        }

        if (latMin > latMax) {
            float_tmp = latMin;
            setLatMin(latMax);
            setLatMax(float_tmp);
        }

        /*
         * size of a cell
         */
        dLat = (latMax - latMin) / (float) ny;
        dLong = (longMax - longMin) / (float) nx;
    }

    /**
     * @return the number of lines
     */
    @Override
    public int get_ny() {
        return ny;
    }

    void set_ny(int ny) {
        this.ny = ny;
    }

    /**
     * @return the number of columns
     */
    @Override
    public int get_nx() {
        return nx;
    }

    /**
     * @return the latitude °N of the upper left corner of the grid
     */
    @Override
    public float getLatMax() {
        return latMax;
    }

    /**
     * @return the latitude °N of the lower right corner of the grid
     */
    @Override
    public float getLatMin() {
        return latMin;
    }

    /**
     * @return the longitude °E of the lower right corner of the grid
     */
    @Override
    public float getLongMax() {
        return longMax;
    }

    /**
     * @return the longitude °E of the upper left corner of the grid
     */
    @Override
    public float getLongMin() {
        return longMin;
    }

    /**
     * @return the dLat of one cell
     */
    @Override
    public float getdLat() {
        return dLat;
    }

    /**
     * @return the dLong of one cell
     */
    @Override
    public float getdLong() {
        return dLong;
    }

    OldConfiguration getConfiguration() {
        return Osmose.getInstance().getOldConfiguration();
    }

    /**
     * @param nx the nbColumns to set
     */
    public void set_nx(int nx) {
        this.nx = nx;
    }

    /**
     * @param latMax the latMax to set
     */
    public void setLatMax(float latMax) {
        this.latMax = latMax;
    }

    /**
     * @param latMin the latMin to set
     */
    public void setLatMin(float latMin) {
        this.latMin = latMin;
    }

    /**
     * @param longMax the longMax to set
     */
    public void setLongMax(float longMax) {
        this.longMax = longMax;
    }

    /**
     * @param longMin the longMin to set
     */
    public void setLongMin(float longMin) {
        this.longMin = longMin;
    }
}
