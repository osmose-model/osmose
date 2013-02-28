/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.util;

import fr.ird.osmose.Cell;
import fr.ird.osmose.Osmose;

/**
 *
 * @author pverley
 */
public class GridMap {

    private float[][] matrix;

    public GridMap() {
        matrix = new float[getOsmose().getGrid().getNbLines()][getOsmose().getGrid().getNbColumns()];
        for (Cell cell : getOsmose().getGrid().getCells()) {
            if (cell.isLand()) {
                matrix[cell.get_igrid()][cell.get_jgrid()] = Cell.LAND_VALUE;
            }
        }
    }

    public void setValue(int i, int j, float value) {
        matrix[i][j] = value;
    }

    public float getValue(int i, int j) {
        return matrix[i][j];
    }

    public void setValue(Cell cell, float value) {
        setValue(cell.get_igrid(), cell.get_jgrid(), value);
    }

    public float getValue(Cell cell) {
        return getValue(cell.get_igrid(), cell.get_jgrid());
    }

    public static Osmose getOsmose() {
        return Osmose.getInstance();
    }
}
