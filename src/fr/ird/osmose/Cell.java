package fr.ird.osmose;

/*******************************************************************************
 * <p>Titre : Cell class </p>
 *
 * <p>Description : spatial unit of the model Osmose - constitutes the grid (Grid) </p>
 *
 * <p>Copyright : Copyright (c) may 2009 </p>
 *
 * <p>Society : IRD, France </p>
 *
 * @author Yunne Shin, Morgane Travers
 * @version 2.1 
 ******************************************************************************* 
 */
import fr.ird.osmose.grid.IGrid;

public class Cell {
    
    final public static float LAND_VALUE = -99.f; 

///////////////////////////////
// Declaration of the variables
///////////////////////////////
    
    /*
     * Cell index such as index = j * nColumns + i
     */
    private final int index;
    /*
     * Grid i-coordinate
     */
    private final int i;
    /*
     * Grid j-coordinate
     */
    private final int j;
    /*
     * latitude [°N] of the center of the cell
     */
    private final float lat;
    /*
     * Longitude [°E] of the center of cell
     */
    private final float lon;
    /*
     * Whether the cell is inland
     */
    private boolean land;
    /*
     * Whether the cell belongs to a MPA
     */
    private boolean mpa;

//////////////
// Constructor
//////////////
    /**
     * Create a new cell at grid position (i, j)
     * and geographical postion (lon, lat)
     * 
     * @param i an integer, the i-grid position
     * @param j an integer, the j-grid
     * @param lat a float, the latitude of the cell, °N
     * @param lon a float, the longitude of the cell, °E
     */
    public Cell(int i, int j, float lat, float lon) {
        this(i, j, lat, lon, false);
    }

    public Cell(int i, int j, float lat, float lon, boolean land) {
        this.i = i;
        this.j = j;
        index = i * getGrid().getNbColumns() + j;
        this.lat = lat;
        this.lon = lon;
        this.land = land;
        mpa = false;
    }

////////////////////////////
// Definition of the methods
////////////////////////////
    /**
     * 
     * @return the index of the cell 
     */
    public int getIndex() {
        return index;
    }
    
    /**
     * @return the i
     */
    public int get_igrid() {
        return i;
    }

    /**
     * @return the j
     */
    public int get_jgrid() {
        return j;
    }

    /**
     * @return the lat
     */
    public float getLat() {
        return lat;
    }

    /**
     * @return the lon
     */
    public float getLon() {
        return lon;
    }

    /**
     * @return the land
     */
    public boolean isLand() {
        return land;
    }

    /**
     * @param land the land to set
     */
    public void setLand(boolean land) {
        this.land = land;
    }

    /**
     * @return the mpa
     */
    public boolean isMPA() {
        return mpa;
    }

    /**
     * @param mpa the mpa to set
     */
    public void setMPA(boolean mpa) {
        this.mpa = mpa;
    }
    
    public static IGrid getGrid() {
        return Osmose.getInstance().getGrid();
    }
    
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("Cell (i:");
        str.append(i);
        str.append(", j:");
        str.append(j);
        str.append(")\n  lat: ");
        str.append((float) lat);
        str.append(" lon: ");
        str.append((float) lon);
        str.append("\n land? ");
        str.append(land);
        str.append("  mpa? ");
        str.append(mpa);
        return str.toString();
    }
    
    @Override
    public boolean equals(Object other) {
        if (other instanceof Cell) {
            Cell otherCell = (Cell) other;
            return otherCell.get_igrid() == i && otherCell.get_jgrid() == j;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 17 * hash + this.i;
        hash = 17 * hash + this.j;
        return hash;
    }
}

