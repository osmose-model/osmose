/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author pverley
 */
public interface IGrid {

    /**
     * Initializes the grid : reads parameters and makes the grid.
     */
    public void init();

    /**
     * Get the grid cell at index (i, j)
     * @param i, index i of the cell
     * @param j, index j of the cell
     * @return Cell(i, j)
     */
    public Cell getCell(int i, int j);

    /**
     * Get a list of the cells.
     * @return a List<Cell> of the cells.
     */
    public List<Cell> getCells();

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
    public ArrayList<Cell> getNeighborCells(Cell cell);

    /*
     * Returns the number of cells of the grid that are not inland.
     */
    public int getNumberAvailableCells();

    /**
     * @return the number of lines
     */
    public int getNbLines();

    /**
     * @return the number of columns
     */
    public int getNbColumns();

    /**
     * @return the latitude °N of the upper left corner of the grid
     */
    public float getLatMax();

    /**
     * @return the latitude °N of the lower right corner of the grid
     */
    public float getLatMin();

    /**
     * @return the longitude °E of the lower right corner of the grid
     */
    public float getLongMax();

    /**
     * @return the longitude °E of the upper left corner of the grid
     */
    public float getLongMin();

    /**
     * @return the dLat of one cell
     */
    public float getdLat();

    /**
     * @return the dLong of one cell
     */
    public float getdLong();
}
