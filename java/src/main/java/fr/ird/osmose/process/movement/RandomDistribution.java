/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
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

package fr.ird.osmose.process.movement;

import fr.ird.osmose.Cell;
import fr.ird.osmose.School;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author pverley
 */
public class RandomDistribution extends AbstractDistribution {

    private final int iSpecies;
    private int areaSize;
    private List<Cell> randomMap;
    /*
     * Ranges of movement in cell during one Osmose time step
     */
    private int range;

    public RandomDistribution(int species) {
        this.iSpecies = species;
    }

    @Override
    public void init() {

        if (!getConfiguration().isNull("movement.distribution.ncell.sp" + iSpecies)) {
            areaSize = getConfiguration().getInt("movement.distribution.ncell.sp" + iSpecies);
        } else {
            areaSize = getGrid().getNOceanCell();
            warning("Could not find parameter movement.distribution.ncell.sp" + iSpecies + ". Osmose assumes that schools of " + getSpecies(iSpecies).getName() + " are distrubuted over the whole domain.");
        }
        createRandomMap();
        
        if (!getConfiguration().isNull("movement.randomwalk.range.sp" + iSpecies)) {
            range = getConfiguration().getInt("movement.randomwalk.range.sp" + iSpecies);
        } else {
            range = 1;
        }
    }

    @Override
    public void move(School school, int iStepSimu) {
        if (school.isUnlocated()) {
            school.moveToCell(randomDeal(randomMap));
        } else {
            school.moveToCell(randomDeal(getAccessibleCells(school)));
        }
    }

    private void createRandomMap() {

        int nbCasesDispos = getGrid().getNOceanCell();

        if (areaSize > nbCasesDispos) {
            /*
             * Whole grid
             */
            areaSize = nbCasesDispos;

            randomMap = new ArrayList(areaSize);
            for (int l = 0; l < getGrid().get_ny(); l++) {
                for (int m = 0; m < getGrid().get_nx(); m++) {
                    if (!getGrid().getCell(l, m).isLand()) {
                        randomMap.add(getGrid().getCell(l, m));
                    }
                }
            }
        } else {
            /*
             * Random patch of connex cells
             */
            int nCells = areaSize;
            randomMap = new ArrayList(nCells);
            boolean[][] alreadyChoosen = new boolean[getGrid().get_ny()][getGrid().get_nx()];
            //Cell[] tabCellsArea = new Cell[speciesAreasSizeTab[numSerie][iSpecies]];
            int i, j;
            i = (int) Math.round(Math.random() * (getGrid().get_nx() - 1));
            j = (int) Math.round(Math.random() * (getGrid().get_ny() - 1));
            while (getGrid().getCell(i, j).isLand()) {
                i = (int) Math.round(Math.random() * (getGrid().get_nx() - 1));
                j = (int) Math.round(Math.random() * (getGrid().get_ny() - 1));
            }
            randomMap.add(getGrid().getCell(i, j));
            alreadyChoosen[j][i] = true;
            /*
             * From initial cell, successive random sorting of the
             * adjacent cells until tabCellsArea is full
             */
            int iFirstSorted = 0;
            int iLastSorted = 0;
            int index = 0;
            while (index < (nCells - 1)) {
                for (int iCell = iFirstSorted; iCell <= iLastSorted; iCell++) {
                    ArrayList<Cell> neigbors = getGrid().getNeighbourCells(randomMap.get(iCell), 1);
                    Iterator<Cell> iter = neigbors.iterator();
                    while ((index < (nCells - 1)) && iter.hasNext()) {
                        Cell cell = iter.next();
                        if (!cell.isLand() && !alreadyChoosen[cell.get_jgrid()][cell.get_igrid()]) {
                            index++;
                            alreadyChoosen[cell.get_jgrid()][cell.get_igrid()] = true;
                            randomMap.add(cell);
                        }
                    }
                }
                iFirstSorted = iLastSorted + 1;
                iLastSorted = index;
            }
        }
    }

    /**
     * Randomly choose a cell among the given list of cells.
     *
     * @param cells, a list of cells
     * @return a cell from the list of cells.
     */
    Cell randomDeal(List<Cell> cells) {
        int index = (int) Math.round((cells.size() - 1) * Math.random());
        return cells.get(index);
    }

    /**
     * Create a list of the accessible cells for a given cell: neighbour cells
     * that are not in land + current cell
     *
     * @param school
     * @return the list of cells accessible to the school
     */
    private List<Cell> getAccessibleCells(School school) {

        Cell cell = school.getCell();
        List<Cell> accessibleCells = new ArrayList();
        Iterator<Cell> neighbors = getGrid().getNeighbourCells(cell, range).iterator();
        while (neighbors.hasNext()) {
            Cell neighbor = neighbors.next();
            if (!neighbor.isLand()) {
                accessibleCells.add(neighbor);
            }
        }
        return accessibleCells;
    }
}
