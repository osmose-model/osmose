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
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a Marine Protected Area. In Osmose a MPA is defined by:
 * <ul>
 * <li>the spatial extension, provided in a CSV file, parameter
 * <i>mpa.file.mpa#</i></li>
 * <li>the start year, included, parameter <i>mpa.start.mpa#</i></li>
 * <li>the end year, included, parameter <i>mpa.end.mpa#</i></li>
 * </ul>
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0b 2013/09/01
 */
public class MPA extends OsmoseLinker {

    /**
     * The index of the MPA.
     */
    private final int index;
    /**
     * List of cells that constitute the geographical extension of the MPA.
     */
    private List<Cell> cells;
    /**
     * The start and end year (both inclusive) the MPA takes effect.
     */
    private int start, end;

    /** Surface of the cell occupied by an MPA.
     *  Dimension = [ny, nx]
     */
    private float[][] percentageMPA;

    /**
     * Creates a new MPA.
     *
     * @param rank, the rank of the simulation
     * @param index, the index of the MPA
     */
    public MPA(int rank, int index) {
        this.index = index;
    }

    /**
     * Initializes the MPA.
     */
    public void init() {

        cells = new ArrayList<>();
        if (!getConfiguration().isNull("mpa.file.mpa" + index)) {
            percentageMPA = new float[getGrid().get_ny()][getGrid().get_nx()];
            loadMPA(getConfiguration().getFile("mpa.file.mpa" + index));
            start = getConfiguration().getInt("mpa.start.year.mpa" + index);
            end = getConfiguration().getInt("mpa.end.year.mpa" + index);
        } else {
            start = Integer.MAX_VALUE;
        }

        if(end < start) {
            String message = String.format("End (%d) and start (%d) dates have been swapped for MPA index %d", end, start, index);
            warning(message);
            int temp = end;
            end = start;
            start = temp;
        }

    }

    /**
     * Returns the year the MPA starts to take effect.
     *
     * @return the year the MPA starts to take effect
     */
    public int getYearStart() {
        return start;
    }

    /**
     * Returns the year (inclusive) the MPA stops to take effect.
     *
     * @return the year (inclusive) the MPA stops to take effect
     */
    public int getYearEnd() {
        return end;
    }

    /**
     * Checks whether the MPA is active at given time step of the simulation.
     *
     * @param iStepSimu, the time step of the simulation
     * @return {@code true} if the MPA is active at the given time step.
     */
    public boolean isActive(int iStepSimu) {
        if (iStepSimu < 0) {
            return false;
        }
        int year = iStepSimu / getConfiguration().getNStepYear();
        return (year >= start) && (year <= end);
    }

    /**
     * Checks whether the given cell belongs to the MPA.
     *
     * @param cell, the cell to be checked
     * @return {@code true} if the {@code cell} belongs to the MPA
     */
    public boolean contains(Cell cell) {
        return cells.contains(cell);
    }

    public List<Cell> getCells() {
        return cells;
    }

    /**
     * Loafs the MPA from a CSV file. The CSV file must have the same dimension
     * than the grid of the model. The following values are expected: -99 for a
     * cell on land, zero for a cell outside the MPA, one for a cell inside the
     * MPA.
     *
     * @param csvFile, the path of the CSV file that defines the geographical
     * extension of the MPA.
     */
    private void loadMPA(String csvFile) {
        try {
            /*
             * Read the CSV file
             */
            CSVReader reader = new CSVReader(new FileReader(csvFile), Separator.guess(csvFile).getSeparator());
            List<String[]> lines = reader.readAll();

            int ny = getGrid().get_ny();
            for (int l = 0; l < lines.size(); l++) {
                String[] line = lines.get(l);
                int j = ny - l - 1;
                for (int i = 0; i < line.length; i++) {
                    float val = Float.valueOf(line[i]);
                    if (val > 0.f) {
                        cells.add(getGrid().getCell(i, j));
                        percentageMPA[j][i] = val;
                    }
                }
            }
        } catch (IOException ex) {
            error("Error loading MPA " + csvFile, ex);
        } catch (NumberFormatException ex) {
            error("Error loading MPA " + csvFile, ex);
        }
    }

    /**
     * Returns the percentage of the cell occupied by a MPA.
     *
     * @param cell
     *            Cell value
     * @return Percentage of MPA
     */
    public float getPercentageMPA(Cell cell) {
        return this.percentageMPA[cell.get_jgrid()][cell.get_igrid()];
    }

    /**
     * Returns the percentage of the cell occupied by a MPA
     *
     * @param cell
     *            Cell value
     * @return Percentage of MPA
     */
    public float getPercentageMPA(int i, int j) {
        return this.percentageMPA[j][i];
    }

}
