/*
 *
 * OSMOSE (Object-oriented Simulator of Marine Ecosystems)
 * http://www.osmose-model.org
 *
 * Copyright (C) IRD (Institut de Recherche pour le Développement) 2009-2020
 *
 * Osmose is a computer program whose purpose is to simulate fish
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
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License). Full description
 * is provided on the LICENSE file.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package fr.ird.osmose.util;

import au.com.bytecode.opencsv.CSVReader;
import fr.ird.osmose.Cell;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;

/**
 *
 * @author pverley
 */
public class GridMap extends OsmoseLinker {

    protected float[][] matrix;

    public GridMap(int defaultValue) {
        matrix = new float[getGrid().get_ny()][getGrid().get_nx()];
        for (Cell cell : getGrid().getCells()) {
            if (cell.isLand()) {
                matrix[cell.get_jgrid()][cell.get_igrid()] = Cell.LAND_VALUE;
            } else {
                matrix[cell.get_jgrid()][cell.get_igrid()] = defaultValue;
            }
        }
    }

    public GridMap(double[][] input) {
        matrix = new float[getGrid().get_ny()][getGrid().get_nx()];
        for (Cell cell : getGrid().getCells()) {
            if (cell.isLand()) {
                matrix[cell.get_jgrid()][cell.get_igrid()] = Cell.LAND_VALUE;
            } else {
                matrix[cell.get_jgrid()][cell.get_igrid()] = (float) input[cell.get_jgrid()][cell.get_igrid()];
            }
        }
    }

    public GridMap() {
        this(0);
    }

    public GridMap(String csvFile) {
        this();
        read(csvFile);
    }

    private void read(String csvFile) {

        try {
            /*
             * Read the CSV file
             */
            CSVReader reader = new CSVReader(new FileReader(csvFile), Separator.guess(csvFile).getSeparator());
            List<String[]> lines = reader.readAll();
            /*
             * Read the map
             */
            int ny = getGrid().get_ny();
            for (int l = 0; l < lines.size(); l++) {
                String[] line = lines.get(l);
                int j = ny - l - 1;
                for (int i = 0; i < line.length; i++) {
                    try {
                        String val = line[i].toLowerCase();
                        if (val.equals("na") || val.equals("nan")) {
                            this.matrix[j][i] = Float.NaN;
                        } else {
                            float value = Float.valueOf(line[i]);
                            this.matrix[j][i] = value;
                        }
                    } catch (NumberFormatException ex) {
                        error("Error parsing CSV map " + csvFile + " row " + (l + 1) + " column " + (i + 1), ex);
                    }
                }
            }
            reader.close();
        } catch (IOException ex) {
            error("Error reading CSV map " + csvFile, ex);
        }
    }

    public void setValue(int i, int j, float value) {
        matrix[j][i] = value;
    }

    public float getValue(int i, int j) {
        return matrix[j][i];
    }

    public void setValue(Cell cell, float value) {
        setValue(cell.get_igrid(), cell.get_jgrid(), value);
    }

    public float getValue(Cell cell) {
        return getValue(cell.get_igrid(), cell.get_jgrid());
    }

    /**
     * Sums the matrix over space
     *
     * @author Nicolas Barrier
     * @return
     */
    public float count() {
        return this.count(false);
    }

    /**
     * Sums the matrix over space
     *
     * @author Nicolas Barrier
     * @return
     */
    public float count(boolean exclude_zeros) {

        float output = 0;

        if (!exclude_zeros) {
            // exclude only NaN values
            for (int j = 0; j < getGrid().get_ny(); j++) {
                for (int i = 0; i < getGrid().get_nx(); i++) {
                    if (!Float.isNaN(matrix[j][i])) {
                        output += matrix[j][i];
                    }
                }
            }
        } else {
            // Exclude NaN values and also 0 values
            for (int j = 0; j < getGrid().get_ny(); j++) {
                for (int i = 0; i < getGrid().get_nx(); i++) {
                    if ((!Float.isNaN(matrix[j][i])) & (matrix[j][i] != 0)) {
                        output += matrix[j][i];
                    }
                }
            }
        }

        return output;

    }

    /**
     * Compares two grid map
     *
     * @author Nicolas Barrier
     * @param map2
     * @return
     */
    public boolean equals(GridMap map2) {

        for (int j = 0; j < getGrid().get_ny(); j++) {
            for (int i = 0; i < getGrid().get_nx(); i++) {
                float val1 = matrix[j][i];
                float val2 =  map2.getValue(i, j);
                // manage the case where both values are NaN
                if(Float.isNaN(val1) && Float.isNaN(val2)) {
                    continue;
                }
                if (val1 != val2) {
                    return false;
                }
            }
        }

        return true;

    }

    /**
     * Reads the matrix from a NetCDF file.
     *
     * @param nc NetcdfFile object
     * @param mapIndex Index of the map to read in the file (first dimension)
     * @throws IOException
     * @throws InvalidRangeException
     */
    public void read(NetcdfFile nc, int mapIndex) throws IOException, InvalidRangeException {
        this.read(nc, mapIndex, "map");
    }

    public void read(NetcdfFile nc, int mapIndex, String varname) throws IOException, InvalidRangeException {

        // Defines the indexes of the NetCDF variable to read
        int nx = getConfiguration().getGrid().get_nx();
        int ny = getConfiguration().getGrid().get_ny();
        int start[] = {mapIndex, 0, 0};
        int count[] = {1, ny, nx};

        // Extracts the FillValue attribute
        double fillValue = -99;
        if (!nc.findVariable(varname).getAttributes().isEmpty()) {
            // Loop over all the attributes to define a size.
            for (int k = 0; k < nc.findVariable(varname).getAttributes().size(); k++) {
                if(nc.findVariable(varname).getAttributes().get(k).getFullName().toLowerCase().equals("_fillvalue")) {
                    fillValue = nc.findVariable(varname).getAttributes().get(k).getNumericValue().doubleValue();
                    break;
                }
            }
        }

        // Reads the NetCDF variable
        Array temp = nc.findVariable(varname).read(start, count).reduce();
        Index index = temp.getIndex();
        for (int j = 0; j < ny; j++) {
            for (int i = 0; i < nx; i++) {
                index.set(j, i);
                matrix[j][i] = (temp.getDouble(index) == fillValue) ? 0.f : (float) temp.getDouble(index);
            }
        }
    }

    public void read(NetcdfFile nc, int mapIndex, int classIndex, String varname) throws IOException, InvalidRangeException {

        // Defines the indexes of the NetCDF variable to read
        int nx = getConfiguration().getGrid().get_nx();
        int ny = getConfiguration().getGrid().get_ny();
        int start[] = {mapIndex, classIndex, 0, 0};
        int count[] = {1, 1, ny, nx};

        // Extracts the FillValue attribute
        double fillValue = -99;
        if (!nc.findVariable(varname).getAttributes().isEmpty()) {
            // Loop over all the attributes to define a fill value.
            for (int k = 0; k < nc.findVariable(varname).getAttributes().size(); k++) {
                if(nc.findVariable(varname).getAttributes().get(k).getFullName().toLowerCase().equals("_fillvalue")) {
                    fillValue = nc.findVariable(varname).getAttributes().get(k).getNumericValue().doubleValue();
                    break;
                }
            }
        }

        // Reads the NetCDF variable
        Array temp = nc.findVariable(varname).read(start, count).reduce();
        Index index = temp.getIndex();
        for (int j = 0; j < ny; j++) {
            for (int i = 0; i < nx; i++) {
                index.set(j, i);
                matrix[j][i] = (temp.getDouble(index) == fillValue) ? 0.f : (float) temp.getDouble(index);
            }
        }
    }

}
