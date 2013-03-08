package fr.ird.osmose.grid;

import fr.ird.osmose.Cell;

/**
 * ******************************************************************************
 * <p>Title : Grid class</p>
 *
 * <p>Description : grid of Osmose model, divided into cells (Cell) Include a
 * function defining neighbors of each cell </p>
 *
 * <p>Copyright : Copyright (c) may 2009</p>
 *
 * <p>Society : IRD, France </p>
 *
 * @author Yunne Shin, Morgane Travers
 * @version 2.1
 * *******************************************************************************
 */
public class OriginalGrid extends AbstractGrid {

////////////////////////////
// Definition of the methods
////////////////////////////
    /*
     * 
     */
    @Override
    public void readParameters() {

        /* grid dimension */
        set_ny(getConfiguration().nLine);
        set_nx(getConfiguration().nColumn);

        /* geographical extension of the grid */
        setLatMax(getConfiguration().upLeftLat);
        setLatMin(getConfiguration().lowRightLat);
        setLongMax(getConfiguration().lowRightLon);
        setLongMin(getConfiguration().upLeftLon);
    }

    /*
     * Create a regular orthogonal grid and specify latitude and longitude
     * of each cell.
     */
    @Override
    public Cell[][] makeGrid() {

        float dLat = (getLatMax() - getLatMin()) / (float) get_ny();
        float dLong = (getLongMax() - getLongMin()) / (float) get_nx();

        Cell[][] grid = new Cell[get_ny()][get_nx()];
        float latitude, longitude;
        for (int j = 0; j < get_ny(); j++) {
            latitude = getLatMin() + (float) (j + 0.5f) * dLat;
            for (int i = 0; i < get_nx(); i++) {
                longitude = getLongMin() + (float) (i + 0.5) * dLong;
                //System.out.print(isLand(i, j) ? "0 ":"1 ");
                grid[j][i] = new Cell(i, j, latitude, longitude, isLand(i, j));
            }
            //System.out.println();
        }
        return grid;
    }

    private boolean isLand(int i, int j) {
        if (null != getConfiguration().icoordLand) {
            int nym1 = get_ny() - 1;
            for (int k = 0; k < getConfiguration().icoordLand.length; k++) {
                if (((nym1 - j) == getConfiguration().icoordLand[k]) && (i == getConfiguration().jcoordLand[k])) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int getStride() {
        return 1;
    }
}
