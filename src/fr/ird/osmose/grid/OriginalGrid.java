package fr.ird.osmose.grid;

import au.com.bytecode.opencsv.CSVReader;
import fr.ird.osmose.Cell;
import fr.ird.osmose.Osmose;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        set_ny(getConfiguration().getInt("grid.nline"));
        set_nx(getConfiguration().getInt("grid.ncolumn"));

        /* geographical extension of the grid */
        setLatMin(getConfiguration().getFloat("grid.lowright.lat"));
        setLatMax(getConfiguration().getFloat("grid.upleft.lat"));
        setLongMax(getConfiguration().getFloat("grid.lowright.lon"));
        setLongMin(getConfiguration().getFloat("grid.upleft.lon"));
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
        String filename = getConfiguration().getFile("grid.mask.file");
        boolean[][] land = readMaskAsCSV(filename);
        for (int j = 0; j < get_ny(); j++) {
            latitude = getLatMin() + (float) (j + 0.5f) * dLat;
            for (int i = 0; i < get_nx(); i++) {
                longitude = getLongMin() + (float) (i + 0.5) * dLong;
                //System.out.print(isLand(i, j) ? "0 ":"1 ");
                grid[j][i] = new Cell(i, j, latitude, longitude, land[j][i]);
            }
            //System.out.println();
        }
        return grid;
    }

    @Override
    public int getStride() {
        return 1;
    }

    private boolean[][] readMaskAsCSV(String csvFile) {
        
        boolean[][] land = null;
        try {
            /*
             * Read the CSV file
             */
            CSVReader reader = new CSVReader(new FileReader(csvFile), ';');
            List<String[]> lines = reader.readAll();
            land = new boolean[lines.size()][];
            int ny = lines.size();
            for (int l = lines.size(); l-- > 0;) {
                String[] line = lines.get(l);
                int j = ny - l - 1;
                land[j] = new boolean[line.length];
                for (int i = 0; i < line.length; i++) {
                    float val = Float.valueOf(line[i]);
                    if (val < 0.f) {
                        land[j][i] = true;
                    }
                }
            }
        } catch (IOException ex) {
            Osmose.getInstance().getLogger().log(Level.SEVERE, "Error reading grid mask from file " + csvFile, ex);
        }
        return land;
    }
}
