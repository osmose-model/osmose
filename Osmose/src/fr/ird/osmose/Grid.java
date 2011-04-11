package fr.ird.osmose;

/********************************************************************************
 * <p>Title : Grid class</p>
 *
 * <p>Description : grid of Osmose model, divided into cells (Cell) 
 * Include a function defining neighbors of each cell </p>
 *
 * <p>Copyright : Copyright (c) may 2009</p>
 *
 * <p>Society : IRD, France </p>
 *
 * @author Yunne Shin, Morgane Travers
 * @version 2.1 
 ******************************************************************************** 
 */
import java.util.*;

public class Grid {

    /*
     * ********
     * * Logs *
     * ********
     * 2011/04/11 phv
     * Deprecated identifySpatialGroups since it looks Benguela specific
     * function.
     * ***
     * 2011/04/07 phv
     * Encapsulated all the variables and propagated the changes to the other
     * classes.
     * Deleted the identifyNeighbors function since neighbors variable has been
     * deleted in Cell.java.
     * Added function getNeighborCells(Cell cell) that is called by Osmose when
     * doing the random sorting of the cells for the random spatial distribution
     * ***
     */
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

//////////////
// Constructor
//////////////
    /**
     * 
     * @param nbl, the number of lines
     * @param nbc, the number of columns
     * @param upleftLat, latitude of the upper left corner
     * @param lowrightLat, latitude of the lower right corner
     * @param upleftLong, longitude of the upper left corner
     * @param lowrightLong, longitude of the lower right corner
     */
    public Grid(int nbl, int nbc, float upleftLat, float lowrightLat, float upleftLong, float lowrightLong) {

        /* grid dimension */
        this.nbLines = nbl;
        this.nbColumns = nbc;

        /* geographical extension of the grid */
        this.latMax = upleftLat;
        this.latMin = lowrightLat;
        this.longMax = lowrightLong;
        this.longMin = upleftLong;

        /* size of a cell */
        dLat = (latMax - latMin) / (float) nbLines;
        dLong = (longMax - longMin) / (float) nbColumns;

        /* make the grid */
        matrix = makeGrid();
    }

////////////////////////////
// Definition of the methods
////////////////////////////
    /*
     * Create a regular orthogonal grid and specify latitude and longitude
     * of each cell.
     */
    private Cell[][] makeGrid() {

        Cell[][] grid = new Cell[nbLines][nbColumns];
        float latitude, longitude;
        for (int i = 0; i < nbLines; i++) {
            latitude = latMax - (float) (i + 0.5f) * dLat;
            for (int j = 0; j < nbColumns; j++) {
                longitude = longMin + (float) (j + 0.5) * dLong;
                grid[i][j] = new Cell(i, j, latitude, longitude);
            }
        }
        return grid;
    }

    /**
     * Get the grid cell at index (i, j)
     * @param i, index i of the cell
     * @param j, index j of the cell
     * @return Cell(i, j)
     */
    public Cell getCell(int i, int j) {
        return matrix[i][j];
    }

    /*
     * phv: looks like a specific method to Benguela system...
     * 2011/04/11 Tagged as deprecated
     */
    @Deprecated
    public void identifySpatialGroups() {}

    /**
     * Get the adjacent cells of a given cell. Cells are randomly sorted.
     * @see Collections.shuffle()
     * For cell(i, j) returns 8 surrounding cells:
     * cell(i - 1, j - 1)
     * cell(i - 1, j)
     * cell(i - 1, j + 1)
     * cell(i, j - 1)
     * cell(i, j + 1)
     * cell(i + 1, j - 1)
     * cell(i + 1, j)
     * cell(i + 1, j + 1)
     * For cells at the edge of the grid, only returns 3 or 5 cells.
     * @param cell
     * @return an ArrayList of the cells surrounding <code>cell</code>
     */
    public ArrayList<Cell> getNeighborCells(Cell cell) {

        int im1 = Math.max(cell.get_igrid() - 1, 0);
        int ip1 = Math.min(cell.get_igrid() + 1, getNbLines() - 1);
        int jm1 = Math.max(cell.get_jgrid() - 1, 0);
        int jp1 = Math.min(cell.get_jgrid() + 1, getNbColumns() - 1);

        ArrayList<Cell> neighbors = new ArrayList();

        for (int i = im1; i <= ip1; i++) {
            for (int j = jm1; j <= jp1; j++) {
                neighbors.add(matrix[i][j]);
            }
        }
        neighbors.remove(cell);
        /* Random sorting of the adjacent cells */
        Collections.shuffle(neighbors);
        return neighbors;
    }

    /**
     * @return the number of lines
     */
    public int getNbLines() {
        return nbLines;
    }

    /**
     * @return the number of columns
     */
    public int getNbColumns() {
        return nbColumns;
    }

    /**
     * @return the latitude °N of the upper left corner of the grid
     */
    public float getLatMax() {
        return latMax;
    }

    /**
     * @return the latitude °N of the lower right corner of the grid
     */
    public float getLatMin() {
        return latMin;
    }

    /**
     * @return the longitude °E of the lower right corner of the grid
     */
    public float getLongMax() {
        return longMax;
    }

    /**
     * @return the longitude °E of the upper left corner of the grid
     */
    public float getLongMin() {
        return longMin;
    }

    /**
     * @return the dLat of one cell
     */
    public float getdLat() {
        return dLat;
    }

    /**
     * @return the dLong of one cell
     */
    public float getdLong() {
        return dLong;
    }
}

