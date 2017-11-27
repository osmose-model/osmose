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
package fr.ird.osmose.util;

import au.com.bytecode.opencsv.CSVReader;
import fr.ird.osmose.Cell;
import fr.ird.osmose.Osmose;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author pverley
 */
public class GridMap {

    private final float[][] matrix;

    public GridMap(int defaultValue) {
        matrix = new float[getOsmose().getGrid().get_ny()][getOsmose().getGrid().get_nx()];
        for (Cell cell : getOsmose().getGrid().getCells()) {
            if (cell.isLand()) {
                matrix[cell.get_jgrid()][cell.get_igrid()] = Cell.LAND_VALUE;
            } else {
                matrix[cell.get_jgrid()][cell.get_igrid()] = defaultValue;
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
            int ny = getOsmose().getGrid().get_ny();
            for (int l = 0; l < lines.size(); l++) {
                String[] line = lines.get(l);
                int j = ny - l - 1;
                for (int i = 0; i < line.length; i++) {
                    try {
                        float value = Float.valueOf(line[i]);
                        if (value > 0.f) {
                            matrix[j][i] = value;
                        }
                    } catch (NumberFormatException ex) {
                        getOsmose().error("Error parsing CSV map " + csvFile + " row " + (l + 1) + " column " + (i + 1), ex);
                    }
                }
            }
            reader.close();
        } catch (IOException ex) {
            getOsmose().error("Error reading CSV map " + csvFile, ex);
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

    public static Osmose getOsmose() {
        return Osmose.getInstance();
    }
}
