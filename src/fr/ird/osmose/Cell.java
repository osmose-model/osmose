package fr.ird.osmose;

public class Cell {
    
///////////////////////////////
// Declaration of the constants
///////////////////////////////
    /**
     * Identifier of cell on land
     */
    final public static float LAND_VALUE = -99.f; 

///////////////////////////////
// Declaration of the variables
///////////////////////////////
    /**
     * Cell index such as index = j * nColumns + i
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
     * latitude [°N] of the center of the cell
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

///////////////
// Constructors
///////////////
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
        // calculates index
        index = j * Osmose.getInstance().getGrid().get_nx() + i;
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
    
    @Override
    public boolean equals(Object other) {
        if (other instanceof Cell) {
            Cell otherCell = (Cell) other;
            return (otherCell.get_igrid() == i) && (otherCell.get_jgrid() == j);
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

