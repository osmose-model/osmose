#ifndef CELL_H
#define CELL_H

#include <math.h>

using namespace std;

class Cell {

    ///////////////////////////////
    // Declaration of the constants
    ///////////////////////////////

  private:

    /**
     * Identifier of cell on land
     */
    static const float LAND_VALUE;
    ///////////////////////////////
    // Declaration of the variables
    ///////////////////////////////
    /**
     * Cell index. Suggested index = j * nColumns + i. The index is set in the
     * constructor though and will be defined in the Grid object. Different
     * index calculation could be used. Just make sure that it is unique for
     * every cell.
     */
    int index;
    /**
     * Grid i-coordinate
     */
    int i;
    /**
     * Grid j-coordinate
     */
    int j;
    /**
     * Latitude [°N] of the center of the cell
     */
    float lat;
    /**
     * Longitude [°E] of the center of cell
     */
    float lon;
    /**
     * Whether the cell is inland
     */
    bool land;

    /** Surface of the cell (m2). */
    float surf;

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
     * @param land a bool, {@code true} if the cell is on land, {@code false}
     * if the cell is in ocean
     */
    Cell(int index, int i, int j, float lat, float lon, bool land);

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
    int getIndex();

    /**
     * Gets the x-coordinate of the cell in the Cartesian coordinate system.
     *
     * @return i, the x-coordinate of the cell.
     */
    int get_igrid();

    /**
     * Gets the y-coordinate of the cell in the Cartesian coordinate system.
     *
     * @return j, the y-coordinate of the cell.
     */
    int get_jgrid();

    /**
     * Gets the latitude of the center of the cell, expressed in North degree.
     *
     * @return the latitude of the center of the cell, in North degree
     */
    float getLat();

    /**
     * Gets the longitude of the center of the cell, expressed in East degree.
     *
     * @return the longitude of the center of the cell, in East degree
     */
    float getLon();

    /**
     * Tells whether the cell is on land or is in the ocean.
     *
     * @return {@code true} if the cell is on land, {@code false} if the cell is
     * in the ocean
     */
    bool isLand();

    /**
     * Determines whether the current is equal to an other cell. Two cells are
     * considered equal if both (i, j) coordinates are the same.
     *
     * @param other, an other {@code Cell} object.
     * @return true if the current cell is equal to the cell given as argument.
     */
    bool equals(Cell other);

    /**
     * Generates a hash code value for this cell. Every cell has a unique hash
     * code value, its index.
     *
     * @return a hash code value for this cell.
     */
    int hashCode();

    /** Returns the cell surface (m2).
     *
     * @return  Surface (m2)
     */
    float getSurface();

    /** Sets the cell surface (m2).
     *
     * @param surf Surface (m2)
     */
    void setSurface(float surf);
    
};
    
const float Cell::LAND_VALUE = NAN;

#endif