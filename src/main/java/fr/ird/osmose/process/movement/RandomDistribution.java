/* 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
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
