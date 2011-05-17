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
import java.io.IOException;
import java.util.*;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayDouble.D2;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;

public class Grid {

    /*
     * ********
     * * Logs *
     * ********
     * 2011/04/18 phv
     * Added a getCells() function
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

    /**
     * Create a new grid by reading longitude and latitude in a NetCDF file.
     * 
     * @param gridFile, filename of the NetCDF grid file
     * @param strLon, name of the longitude variable
     * @param strLat, name of the latitude variable
     * @param strMask, name of the mask variable (land / ocean)
     */
    public Grid(String gridFile, String strLon, String strLat, String strMask) {

        matrix = readGrid(gridFile, strLon, strLat, strMask);
        getDimGeogArea();
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
     * Reads the longitude and latitude of the grid in a NetCDF file and returns
     * a Cell[][] array.
     * @param gridFile
     * @param strLon
     * @param strLat
     * @param strMask
     * @return
     */private Cell[][] readGrid(String gridFile, String strLon, String strLat, String strMask) {

        NetcdfFile ncGrid = null;

        try {
            ncGrid = NetcdfDataset.openFile(gridFile, null);
        } catch (IOException ex) {
            System.err.println("Failed to open grid file " + gridFile);
            ex.printStackTrace();
            return null;
        }

        int[] shape = ncGrid.findVariable(strLat).getShape();
        nbLines = shape[0];
        nbColumns = shape[1];
        System.out.println("Grid ==> nbLines: " + nbLines + " nbColumns: " + nbColumns);
        Cell[][] grid = new Cell[nbLines][nbColumns];
        try {
            ArrayDouble.D2 arrLon = (D2) ncGrid.findVariable(strLon).read();
            ArrayDouble.D2 arrLat = (D2) ncGrid.findVariable(strLat).read();
            ArrayDouble.D2 arrMask = (D2) ncGrid.findVariable(strMask).read();
            for (int i = 0; i < nbLines; i++) {
                for (int j = 0; j < nbColumns; j++) {
                    boolean land = arrMask.get(i, j) == 0;
                    float lat = (float) arrLat.get(i, j);
                    float lon = (float) arrLon.get(i, j);
                    grid[i][j] = new Cell(i, j, lat, lon, land);
                    //System.out.println("Cell(" + i + ", " + j + ") lat=" + lat + " lon=" + lon + " land=" + land);
                }
            }
        } catch (IOException ex) {
            System.err.println("Problem reading lon lat mask fields in grid file.");
            ex.printStackTrace();
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

    /**
     * Get a list of the cells.
     * @return a List<Cell> of the cells.
     */
    public List<Cell> getCells() {
        ArrayList<Cell> cells = new ArrayList(nbLines * nbColumns);
        for (int i = nbLines; i-- > 0;) {
            for (int j = nbColumns; j-- > 0;) {
                cells.add(matrix[i][j]);
            }
        }
        return cells;
    }

    /*
     * phv: looks like a specific method to Benguela system...
     * 2011/04/11 Tagged as deprecated
     */
    @Deprecated
    public void identifySpatialGroups() {
    }

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

    /*
     * Returns the number of cells of the grid that are not inland.
     */
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
     * Computes longMin, latMin, longMax, latMax.
     * Computes the dimension of a cell, assuming that we work in a regular grid
     */
    private void getDimGeogArea() {

        //--------------------------------------
        // Calculate the Physical Space extrema

        longMin = Float.MAX_VALUE;
        longMax = -longMin;
        latMin = Float.MAX_VALUE;
        latMax = -latMin;
        int i = nbLines;
        int j = 0;

        while (i-- > 0) {
            j = nbColumns;
            while (j-- > 0) {
                if (matrix[i][j].getLon() >= longMax) {
                    longMax = matrix[i][j].getLon();
                }
                if (matrix[i][j].getLon() <= longMin) {
                    longMin = matrix[i][j].getLon();
                }
                if (matrix[i][j].getLat() >= latMax) {
                    latMax = matrix[i][j].getLat();
                }
                if (matrix[i][j].getLat() <= latMin) {
                    latMin = matrix[i][j].getLat();
                }
            }
        }
        //System.out.println("lonmin " + lonMin + " lonmax " + lonMax + " latmin " + latMin + " latmax " + latMax);
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

        /* size of a cell */
        dLat = (latMax - latMin) / (float) nbLines;
        dLong = (longMax - longMin) / (float) nbColumns;
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

