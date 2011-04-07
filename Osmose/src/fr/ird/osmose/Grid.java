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

class Grid {

    /*
     * ********
     * * Logs *
     * ********
     * 2011/04/07 phv
     * Encapsulated all the variables and propagated the changes to the other
     * classes.
     * Deleted the identifyNeighbors function since neighbors variable has been
     * deleted in Cell.java.
     * Added function getAdjacentCells(Cell cell) that is called by Osmose when
     * doing the random sorting of the cells for the random spatial distribution
     * ***
     */

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

    public Grid(int nbl, int nbc, float upleftLat, float lowrightLat, float upleftLong, float lowrightLong) {
        this.nbLines = nbl;
        this.nbColumns = nbc;

        this.latMax = upleftLat;
        this.latMin = lowrightLat;
        this.longMax = lowrightLong;
        this.longMin = upleftLong;

        dLat = (latMax - latMin) / (float) nbLines;
        dLong = (longMax - longMin) / (float) nbColumns;
        matrix = new Cell[nbLines][nbColumns];

        // create the grid and specify latitude and longitude of each cell
        float latitude, longitude;
        for (int i = 0; i < nbLines; i++) {
            latitude = latMax - (float) (i + 0.5f) * dLat;
            for (int j = 0; j < nbColumns; j++) {
                longitude = longMin + (float) (j + 0.5) * dLong;
                matrix[i][j] = new Cell(i, j, latitude, longitude);
            }
        }

        identifySpatialGroups();
    }

    public Cell getCell(int i, int j) {
        return matrix[i][j];
    }

    public void identifySpatialGroups() {
        for (int i = 0; i < getNbLines(); i++) {
            for (int j = 0; j < getNbColumns(); j++) {
                if (matrix[i][j].getLat() >= (matrix[i][j].getLon() - 52.5)) {
                    matrix[i][j].setSpatialGroup(Cell.SpatialGroup.UPWELLING);
                } else {
                    matrix[i][j].setSpatialGroup(Cell.SpatialGroup.AGULHAS_BANK);
                }
            }
        }
    }

    public ArrayList<Cell> getAdjacentCells(Cell cell) {

        int im1 = Math.max(cell.getI() - 1, 0);
        int ip1 = Math.min(cell.getI() + 1, getNbLines() - 1);
        int jm1 = Math.max(cell.getJ() - 1, 0);
        int jp1 = Math.min(cell.getJ() + 1, getNbColumns() - 1);

        ArrayList<Cell> neighbors = new ArrayList();

        for (int i = im1; i <= ip1; i++) {
            for (int j = jm1; j <= jp1; j++) {
                neighbors.add(matrix[i][j]);
            }
        }
        neighbors.remove(cell);
        Collections.shuffle(neighbors);
        //return neighbors.toArray(new Cell[neighbors.size()]);
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

