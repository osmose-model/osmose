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

import au.com.bytecode.opencsv.CSVReader;
import fr.ird.osmose.Cell;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * This class is an instance of the original Osmose grid as it has been created
 * by Yunne SHIN in previous version of Osmose. It is a regular orthogonal grid
 * defined by the following parameters:
 * <ul>
 * <li>geographical coordinates of the top left corner of the grid</li>
 * <li>geographical coordinates of the bottom right corner of the grid</li>
 * <li>number of lines</li>
 * <li>number of columns</li>
 * </ul>
 * This class has been kept for backward compatibility with older configurations
 * of Osmose. Since Osmose version 3, {@link NcGrid} should preferably be used.
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0b 2013/09/01
 */
public class OriginalGrid extends AbstractGrid {

///////////////////////////////
// Declaration of the variables
///////////////////////////////
    /**
     * Number of lines. Parameter <i>grid.nline</i>
     */
    private int nline;
    /**
     * Number of columns. Parameter <i>grid.ncolumn</i>
     */
    private int ncolumn;
    /**
     * Latitude, in degree north, of the North West corner of the grid.
     * Parameter <i>grid.upleft.lat</i>
     */
    private float latmax;
    /**
     * Latitude, in degree north, of the South East corner of the grid.
     * Parameter <i>grid.lowright.lat</i>
     */
    private float latmin;
    /**
     * Longitude, in degree east, of the South East corner of the grid.
     * Parameter <i>grid.lowright.lon</i>
     */
    private float lonmax;
    /**
     * Longitude, in degree east, of the North West corner of the grid.
     * Parameter <i>grid.upleft.lon</i>
     */
    private float lonmin;

////////////////////////////
// Definition of the methods
////////////////////////////
    @Override
    public void readParameters() {

        /* grid dimension */
        nline = getConfiguration().getInt("grid.nline");
        ncolumn = getConfiguration().getInt("grid.ncolumn");

        /* geographical extension of the grid */
        latmin = getConfiguration().getFloat("grid.lowright.lat");
        latmax = getConfiguration().getFloat("grid.upleft.lat");
        lonmax = getConfiguration().getFloat("grid.lowright.lon");
        lonmin = getConfiguration().getFloat("grid.upleft.lon");
    }

    /**
     * Create a regular orthogonal grid and specify latitude and longitude of
     * each cell.
     *
     * @return an array of {@code Cell} that represents the grid
     */
    @Override
    public Cell[][] makeGrid() {

        float dLat = (latmax - latmin) / (float) nline;
        float dLong = (lonmax - lonmin) / (float) ncolumn;

        Cell[][] grid = new Cell[nline][ncolumn];
        float latitude, longitude;
        String filename = getConfiguration().getFile("grid.mask.file");
        boolean[][] land = readMaskAsCSV(filename);
        for (int j = 0; j < nline; j++) {
            latitude = latmin + (float) (j + 0.5f) * dLat;
            for (int i = 0; i < ncolumn; i++) {
                longitude = lonmin + (float) (i + 0.5) * dLong;
                //System.out.print(isLand(i, j) ? "0 ":"1 ");
                grid[j][i] = new Cell(i, j, latitude, longitude, land[j][i]);
            }
            //System.out.println();
        }
        return grid;
    }

    /**
     * Reads the mask of the grid from a CSV file. The CSV file must "look like"
     * the grid. This means that the first row of the CSV file (the top one)
     * corresponds to the line
     *
     * @param csvFile, the path of the CSV file
     * @return an array of boolean, {@code true} for land and {@code false} for
     * ocean
     */
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
            error("Error reading grid mask from file " + csvFile, ex);
        }
        return land;
    }
}
