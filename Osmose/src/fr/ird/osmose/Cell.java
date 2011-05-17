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
import fr.ird.osmose.util.SchoolLengthComparator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

public class Cell extends ArrayList<School> {

    /*
     * ********
     * * Logs *
     * ********
     * 2011/04/18 phv
     * Added a sortSchoolsByLength() function
     * 2011/04/07 phv
     * Deleted the spatialGroup variable.
     * ***
     * 2011/04/07 phv
     * Deleted the vectPresentSchools variable.
     * Cell extends ArrayList<School> it means that a cell is now an array of
     * schools. Therefore to add, remove or get a school it is straighforward:
     * cell.add(school) cell.get(k) cell.remove(scholl), etc.
     * Deleted numPresentSchools, replaced by cell.size().
     * Deleted the neigbhors variable since it is not necessary to keep it
     * in memory. It is sufficient to find them just when the program need them.
     * ***
     * 2011/04/01 phv
     * Renamed most variables
     * Encapsulated variables i, j, lat, lon, land, map, spatialGroup,
     * alreadyChoosen.
     * Deleted useless variables such as nbPresentSchools, nbMapsConcerned. It
     * is sufficient to request the associated vector size.
     * ***
     */
///////////////////////////////
// Declaration of the variables
///////////////////////////////

    /*
     * Grid i-coordinate
     */
    private int i;
    /*
     * Grid j-coordinate
     */
    private int j;
    /*
     * latitude [°N] of the center of the cell
     */
    private float lat;
    /*
     * Longitude [°E] of the center of cell
     */
    private float lon;
    /*
     * Whether the cell is inland
     */
    private boolean land;
    /*
     * Whether the cell belongs to a MPA
     */
    private boolean mpa;
    /*
     * Whether the cell is already used when randomly dealing the schools in
     * the grid.
     */
    private boolean alreadyChosen;
    /*
     * list of the maps in which this cell is involved
     */
    Vector numMapsConcerned;
    /*
     * For spatial interpolation with the grid of
     * a LTL (low trophic levels) model.
     * Positions of the cells of the LTL grid used to compute the LTL biomass
     * (position within the LTL grid)
     */
    Vector icoordLTLGrid, jcoordLTLGrid;

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
        this.lat = lat;
        this.lon = lon;
        this.land = land;
        mpa = false;
        numMapsConcerned = new Vector();
        icoordLTLGrid = new Vector();
        jcoordLTLGrid = new Vector();
    }

////////////////////////////
// Definition of the methods
////////////////////////////
    /**
     * @return the i
     */
    public int get_igrid() {
        return i;
    }

    /**
     * @param i the i to set
     */
    public void set_igrid(int i) {
        this.i = i;
    }

    /**
     * @return the j
     */
    public int get_jgrid() {
        return j;
    }

    /**
     * @param j the j to set
     */
    public void set_jgrid(int j) {
        this.j = j;
    }

    /**
     * @return the lat
     */
    public float getLat() {
        return lat;
    }

    /**
     * @param lat the lat to set
     */
    public void setLat(float lat) {
        this.lat = lat;
    }

    /**
     * @return the lon
     */
    public float getLon() {
        return lon;
    }

    /**
     * @param lon the lon to set
     */
    public void setLon(float lon) {
        this.lon = lon;
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

    /**
     * The number of distribution maps in which this cell is involved
     * @return the nbMapsConcerned
     */
    public int getNbMapsConcerned() {
        return numMapsConcerned.size();
    }

    /**
     * @return the alreadyChosen
     */
    public boolean isAlreadyChosen() {
        return alreadyChosen;
    }

    /**
     * @param alreadyChosen the alreadyChosen to set
     */
    public void setAlreadyChosen(boolean alreadyChosen) {
        this.alreadyChosen = alreadyChosen;
    }

    /**
     * @return the nbCellsLTLGrid
     */
    public int getNbCellsLTLGrid() {
        return icoordLTLGrid.size();
    }

    /**
     * Returns the school(index) in the cell
     * @param index
     * @return the school(index)
     */
    public School getSchool(int index) {
        return get(index);
    }

    /**
     * Sort all the schools in this cell according to their length.
     */
    public void sortSchoolsByLength() {
        Collections.sort(this, new SchoolLengthComparator());
    }
}

