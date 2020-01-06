/* 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, additional and
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
package fr.ird.osmose;

/**
 * This class defines a cell of the spatial grid of the model. A cell is defined
 * by a unique cell index, some (i, j) coordinates in the Cartesian coordinate
 * system, a longitude and a latitude and whether it is on land or ocean.
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0b 2013/09/01
 */
public class Cell {

///////////////////////////////
// Declaration of the constants
///////////////////////////////
    /**
     * Identifier of cell on land
     */
    final public static float LAND_VALUE = Float.NaN;

///////////////////////////////
// Declaration of the variables
///////////////////////////////
    /**
     * Cell index. Suggested index = j * nColumns + i. The index is set in the
     * constructor though and will be defined in the Grid object. Different
     * index calculation could be used. Just make sure that it is unique for
     * every cell.
     */
    final private int index;
    /**
     * Grid i-coordinate
     */
    final private int i;
    /**
     * Grid j-coordinate
     */
    final private int j;
    /**
     * Latitude [°N] of the center of the cell
     */
    final private float lat;
    /**
     * Longitude [°E] of the center of cell
     */
    final private float lon;
    /**
     * Whether the cell is inland
     */
    final private boolean land;
    
    /** Surface of the cell (m2). */
    private float surf;

///////////////
// Constructors
///////////////
    /**
     * Create a new cell at grid position (i, j) and geographical position (lon,
     * lat)
     *
     * @param index ,index of the cell {@code index = j * nx + i}
     * @param i an integer, the i-grid position
     * @param j an integer, the j-grid
     * @param lat a float, the latitude of the cell, North degree
     * @param lon a float, the longitude of the cell, East degree
     * @param surf a float, the surface of the cell (in m2)
     * @param land a boolean, {@code true} if the cell is on land, {@code false}
     * if the cell is in ocean
     */
    public Cell(int index, int i, int j, float lat, float lon, float surf, boolean land) {
        this.index = index;
        this.i = i;
        this.j = j;
        this.lat = lat;
        this.lon = lon;
        this.surf = surf;
        this.land = land;
    }

////////////////////////////
// Definition of the methods
////////////////////////////
    /**
     * Gets the index of the cell. We recommend for the index to be calculated
     * as {@code index = j * nx + i} with {@code nx} the dimension of the grid
     * along the x-axis (i.e. the number of columns). As it is only a
     * recommendation, the index should not be used to deduce either i or j
     * coordinates. Some Grid objects may use other indexation than the
     * suggested one.
     *
     * @return the index of the cell
     */
    public int getIndex() {
        return index;
    }

    /**
     * Gets the x-coordinate of the cell in the Cartesian coordinate system.
     *
     * @return i, the x-coordinate of the cell.
     */
    public int get_igrid() {
        return i;
    }

    /**
     * Gets the y-coordinate of the cell in the Cartesian coordinate system.
     *
     * @return j, the y-coordinate of the cell.
     */
    public int get_jgrid() {
        return j;
    }

    /**
     * Gets the latitude of the center of the cell, expressed in North degree.
     *
     * @return the latitude of the center of the cell, in North degree
     */
    public float getLat() {
        return lat;
    }

    /**
     * Gets the longitude of the center of the cell, expressed in East degree.
     *
     * @return the longitude of the center of the cell, in East degree
     */
    public float getLon() {
        return lon;
    }

    /**
     * Tells whether the cell is on land or is in the ocean.
     *
     * @return {@code true} if the cell is on land, {@code false} if the cell is
     * in the ocean
     */
    public boolean isLand() {
        return land;
    }

    /**
     * Gives a human readable representation of the cells.
     *
     * @return a String that describes the cell by the coordinates (i, j), the
     * geographical coordinates (lat, lon) and the mask.
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("Cell i:");
        str.append(i);
        str.append(" j:");
        str.append(j);
        str.append(" lat:");
        str.append((float) lat);
        str.append(" lon:");
        str.append((float) lon);
        str.append(" land:");
        str.append(land);
        return str.toString();
    }

    /**
     * Determines whether the current is equal to an other cell. Two cells are
     * considered equal if both (i, j) coordinates are the same.
     *
     * @param other, an other {@code Cell} object.
     * @return true if the current cell is equal to the cell given as argument.
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof Cell) {
            Cell otherCell = (Cell) other;
            return (otherCell.get_igrid() == i) && (otherCell.get_jgrid() == j);
        }
        return false;
    }

    /**
     * Generates a hash code value for this cell. Every cell has a unique hash
     * code value, its index.
     *
     * @return a hash code value for this cell.
     */
    @Override
    public int hashCode() {
        return index;
    }    
    
    /** Returns the cell surface (m2). 
     * 
     * @return  Surface (m2)
     */
    public float getSurface() {
        return this.surf;
    }
    
    /** Sets the cell surface (m2). 
     * 
     * @param surf Surface (m2)
     */
    public void setSurface(float surf) {
        this.surf = surf;
    } 
}
