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
import java.util.*;

class Cell {

    int posi, posj; 		//position in matrix i,j
    float coordLat, coordLong;           // coordinates in degrees
    Vector vectPresentSchools;
    int nbPresentSchools;
    Cell neighbors[];
    boolean coast;
    boolean isMPA;
    boolean alreadyChosen;
    int spatialGroup;
    // for spatial distribution of schools
    int nbMapsConcerned;        // number of distribution maps in which this cell is involved
    Vector numMapsConcerned;    // list of these maps
    // for spatial interpolation with the grid of a LTL (low trophic levels) model
    int nbCellsLTLGrid;        // number of cells of the LTL grid used to compute the LTL biomass of this cell
    Vector icoordLTLGrid, jcoordLTLGrid;     // positions of the cells of the LTL grid used to compute the LTL biomass (position within the LTL grid)

    public Cell(int posi, int posj, float latitude, float longitude) {
        this.posi = posi;
        this.posj = posj;
        this.coordLat = latitude;
        this.coordLong = longitude;
        vectPresentSchools = new Vector();
        nbPresentSchools = 0;
        coast = false;
        isMPA = false;
        nbMapsConcerned = 0;
        numMapsConcerned = new Vector();
        nbCellsLTLGrid = 0;
        icoordLTLGrid = new Vector();
        jcoordLTLGrid = new Vector();
    }
}

