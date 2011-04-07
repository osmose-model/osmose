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
     * Deleted the identifyNeighbors function since neighbors variable has been
     * deleted in Cell.java.
     * Added function getAdjacentCells(Cell cell) that is called by Osmose when
     * doing the random sorting of the cells for the random spatial distribution
     * ***
     */

    Osmose osmose;
    Cell[][] matrix;
    int nbLines, nbColumns;
    float latMax, latMin, longMax, longMin;
    float dLat, dLong;

    public Grid(Osmose osmose, int nbl, int nbc, float upleftLat, float lowrightLat, float upleftLong, float lowrightLong) {
        this.osmose = osmose;
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

    public void identifySpatialGroups() {
        for (int i = 0; i < nbLines; i++) {
            for (int j = 0; j < nbColumns; j++) {
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
        int ip1 = Math.min(cell.getI() + 1, nbLines - 1);
        int jm1 = Math.max(cell.getJ() - 1, 0);
        int jp1 = Math.min(cell.getJ() + 1, nbColumns - 1);

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
}

