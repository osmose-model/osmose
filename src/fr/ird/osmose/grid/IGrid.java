/*
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, natural and
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
package fr.ird.osmose.grid;

import fr.ird.osmose.Cell;
import java.util.ArrayList;
import java.util.List;

/**
 * This interface details the functions that an Osmose grid needs to implement.
 * The grid is defined in the Cartesian coordinate system, with the origin of
 * the grid on the bottom left corner, the x-axis pointing toward the right and
 * the y-axis towards the top.
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0b 2013/09/01
 */
public interface IGrid {

    /**
     * Initializes the grid. Reads the input parameters and makes the grid. It
     * must be called after creating a new instance of the grid.
     */
    public void init();

    /**
     * Get the grid cell at index (i, j)
     *
     * @param i, index i of the cell
     * @param j, index j of the cell
     * @return Cell(i, j)
     */
    public Cell getCell(int i, int j);

    /**
     * Get the grid cell at specified index. The index of the cell is defined
     * such as {@code index = j * nx + i} with nx being the dimension of the
     * grid in the x-direction (number of columns) and {i, j} the coordinates of
     * the cell in the Cartesian system.
     *
     * @param index of the cell
     * @return Cell(index)
     */
    public Cell getCell(int index);

    /**
     * Get a list of the cells.
     *
     * @return a List<Cell> of the cells.
     */
    public List<Cell> getCells();

    /**
     * Get the adjacent cells of a given cell (including itself) within a given
     * range of cells.
     *
     * For instance, for {@code cell(i, j)} with a range of 1, the function
     * returns 8 surrounding cells:
     * <ul>
     * <li>{@code cell(i - 1, j - 1)}</li>
     * <li>{@code cell(i - 1, j)}</li>
     * <li>{@code cell(i - 1, j + 1)}</li>
     * <li>{@code cell(i, j - 1)}</li>
     * <li>{@code cell(i, j + 1)}</li>
     * <li>{@code cell(i + 1, j - 1)}</li>
     * <li>{@code cell(i + 1, j)}</li>
     * <li>{@code cell(i + 1, j + 1)}</li>
     * </ul>
     * For cells at the edge of the grid, only returns 3 or 5 cells.
     *
     * @param cell, the cell for which to get the neighbors
     * @param range, and integer, the range of the neighborhood
     * @return an ArrayList of the cells surrounding <code>cell</code>
     */
    public ArrayList<Cell> getNeighbourCells(Cell cell, int range);

    /**
     * Returns the number of cells of the grid that are in the ocean.
     *
     * @return the number of cells of the grid that are in the ocean
     */
    public int getNOceanCell();

    /**
     * Returns the dimension of the grid along the y-axis. The number of lines.
     *
     * @return the number of lines
     */
    public int get_ny();

    /**
     * Returns the dimension of the grid along the y-axis. The number of
     * columns.
     *
     * @return the number of columns
     */
    public int get_nx();

    /**
     * Returns the latitude, in degree north, of the North West corner of the
     * grid.
     *
     * @return the latitude, in degree north, of the North West corner of the
     * grid
     */
    public float getLatMax();

    /**
     * Return the latitude, in degree north, of the South East corner of the
     * grid.
     *
     * @return the latitude, in degree north, of the South East corner of the
     * grid
     */
    public float getLatMin();

    /**
     * Returns the longitude, in degree east, of the South East corner of the
     * grid.
     *
     * @return the longitude, in degree east, of the South East corner of the
     * grid
     */
    public float getLongMax();

    /**
     * Returns the longitude, in degree east, of the North West corner of the
     * grid.
     *
     * @return the longitude, in degree east, of the North West corner of the
     * grid
     */
    public float getLongMin();

    /**
     * Returns the meridional size, in degree, of a cell. (delta latitude)
     *
     * @return the dLat of a cell, in degree
     */
    public float getdLat();

    /**
     * Returns the zonal size, in degree, of a cell. (delta longitude)
     *
     * @return the dLong of a cell, in degree
     */
    public float getdLong();
}
