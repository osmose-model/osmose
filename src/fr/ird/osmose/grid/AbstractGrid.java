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
    private int nbLines;
    /*
     * Number od columns
     */
    private int nbColumns;
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
        return matrix[i][j];
    }

    @Override
    public Cell getCell(int index) {
        int i = index / nbColumns;
        int j = index - i * nbColumns;
        return matrix[i][j];
    }

    /**
     * Get a list of the cells.
     *
     * @return a List<Cell> of the cells.
     */
    @Override
    public List<Cell> getCells() {
        ArrayList<Cell> cells = new ArrayList(nbLines * nbColumns);
        for (int i = nbLines; i-- > 0;) {
            for (int j = nbColumns; j-- > 0;) {
                cells.add(matrix[i][j]);
            }
        }
        return cells;
    }

    /**
     * Get the adjacent cells of a given cell (cell included) within a given
     * range of cells.
     * Cells are randomly sorted.
     *
     * @see Collections.shuffle() For cell(i, j) returns 8 surrounding cells:
     * cell(i - 1, j - 1) cell(i - 1, j) cell(i - 1, j + 1) cell(i, j - 1)
     * cell(i, j + 1) cell(i + 1, j - 1) cell(i + 1, j) cell(i + 1, j + 1) For
     * cells at the edge of the grid, only returns 3 or 5 cells.
     * @param cell
     * @param range, and integer, the range of the neighbourhood
     * @return an ArrayList of the cells surrounding
     * <code>cell</code>
     */
    @Override
    public ArrayList<Cell> getNeighbourCells(Cell cell, int range) {

        int im1 = Math.max(cell.get_igrid() - range, 0);
        int ip1 = Math.min(cell.get_igrid() + range, getNbLines() - 1);
        int jm1 = Math.max(cell.get_jgrid() - range, 0);
        int jp1 = Math.min(cell.get_jgrid() + range, getNbColumns() - 1);

        ArrayList<Cell> neighbours = new ArrayList();

        for (int i = im1; i <= ip1; i++) {
            for (int j = jm1; j <= jp1; j++) {
                neighbours.add(matrix[i][j]);
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

        for (int i = 0; i < nbLines; i++) {
            for (int j = 0; j < nbColumns; j++) {
                if (!matrix[i][j].isLand()) {
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
        nbLines = matrix.length;
        setNbColumns(matrix[0].length);

        //--------------------------------------
        // Calculate the Physical Space extrema

        setLongMin(Float.MAX_VALUE);
        setLongMax(-longMin);
        setLatMin(Float.MAX_VALUE);
        setLatMax(-latMin);
        int i = nbLines;
        int j = 0;

        while (i-- > 0) {
            j = nbColumns;
            while (j-- > 0) {
                if (matrix[i][j].getLon() >= longMax) {
                    setLongMax(matrix[i][j].getLon());
                }
                if (matrix[i][j].getLon() <= longMin) {
                    setLongMin(matrix[i][j].getLon());
                }
                if (matrix[i][j].getLat() >= latMax) {
                    setLatMax(matrix[i][j].getLat());
                }
                if (matrix[i][j].getLat() <= latMin) {
                    setLatMin(matrix[i][j].getLat());
                }
            }
        }
        //System.out.println("lonmin " + lonMin + " lonmax " + lonMax + " latmin " + latMin + " latmax " + latMax);
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
        dLat = (latMax - latMin) / (float) nbLines;
        dLong = (longMax - longMin) / (float) nbColumns;
    }

    /**
     * @return the number of lines
     */
    @Override
    public int getNbLines() {
        return nbLines;
    }

    void setNbLines(int nbLines) {
        this.nbLines = nbLines;
    }

    /**
     * @return the number of columns
     */
    @Override
    public int getNbColumns() {
        return nbColumns;
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
     * @param nbColumns the nbColumns to set
     */
    public void setNbColumns(int nbColumns) {
        this.nbColumns = nbColumns;
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
