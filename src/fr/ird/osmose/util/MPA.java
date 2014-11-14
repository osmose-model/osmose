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
public class MPA extends SimulationLinker {

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

    /**
     * Creates a new MPA.
     *
     * @param rank, the rank of the simulation
     * @param index, the index of the MPA
     */
    public MPA(int rank, int index) {
        super(rank);
        this.index = index;
    }

    /**
     * Initializes the MPA.
     */
    public void init() {

        cells = new ArrayList();
        if (!getConfiguration().isNull("mpa.file.mpa" + index)) {
            loadMPA(getConfiguration().getFile("mpa.file.mpa" + index));
            start = getConfiguration().getInt("mpa.start.year.mpa" + index);
            end = getConfiguration().getInt("mpa.end.year.mpa" + index);
        } else {
            start = Integer.MAX_VALUE;
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
            CSVReader reader = new CSVReader(new FileReader(csvFile), ';');
            List<String[]> lines = reader.readAll();

            int ny = getGrid().get_ny();
            for (int l = 0; l < lines.size(); l++) {
                String[] line = lines.get(l);
                int j = ny - l - 1;
                for (int i = 0; i < line.length; i++) {
                    float val = Float.valueOf(line[i]);
                    if (val > 0.f) {
                        cells.add(getGrid().getCell(i, j));
                    }
                }
            }
        } catch (IOException ex) {
            error("Error loading MPA " + csvFile, ex);
        } catch (NumberFormatException ex) {
            error("Error loading MPA " + csvFile, ex);
        }
    }
}
