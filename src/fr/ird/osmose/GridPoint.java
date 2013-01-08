/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose;

/**
 * 
 * @author pverley
 */
public class GridPoint {

    private float x, y;
    private float lon, lat;

    public boolean isInWater() {
        return !getCell().isLand();
    }

    public boolean isOnEdge() {
        return ((x > (getGrid().getNbColumns() - 2.0f))
                || (x < 1.0f)
                || (y > (getGrid().getNbLines() - 2.0f))
                || (y < 1.0f));
    }

    /**
     * @return the x
     */
    public float getX() {
        return x;
    }
    
    public void setX(float x) {
        this.x = x;
    }

    /**
     * @return the y
     */
    public float getY() {
        return y;
    }
    
    public void setY(float y) {
        this.y = y;
    }

    /**
     * @return the lon
     */
    public float getLon() {
        return lon;
    }

    /**
     * @return the lat
     */
    public float getLat() {
        return lat;
    }

    public boolean isUnlocated() {
        return ((x < 0) || (y < 0));
    }
    
    public void setOffGrid() {
        x = y = -1;
    }
    
    /**
     * Gets the current location of the school
     *
     * @return the cell where is located the school
     */
    public Cell getCell() {
        return getGrid().getCell(Math.round(x), Math.round(y));
    }
    
    public void moveToCell(Cell cell) {
        setX(cell.get_igrid());
        setY(cell.get_jgrid());
    }
    
    public static IGrid getGrid() {
        return getOsmose().getGrid();
    }
    
    /*
     * Get the current Osmose instance
     */
    public static Osmose getOsmose() {
        return Osmose.getInstance();
    }
    
    /*
     * Get current Simulation
     */
    public static Simulation getSimulation() {
        return getOsmose().getSimulation();
    }
}
