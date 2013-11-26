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
package fr.ird.osmose.process;

import fr.ird.osmose.Cell;
import fr.ird.osmose.School;
import fr.ird.osmose.Species;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author pverley
 */
public class RandomDistributionProcess extends AbstractProcess {

    private final MovementProcess parent;
    private final Species species;
    private int areaSize;
    private List<Cell> randomMap;

    public RandomDistributionProcess(int rank, Species species, MovementProcess parent) {
        super(rank);
        this.species = species;
        this.parent = parent;
    }

    @Override
    public void init() {
        areaSize = parent.getSizeRandomMap(species.getIndex());
        createRandomMap();
    }

    @Override
    public void run() {
        for (School school : getSchoolSet().getSchools(species)) {
            if (school.isUnlocated()) {
                school.moveToCell(parent.randomDeal(randomMap));
            } else {
                school.moveToCell(parent.randomDeal(parent.getAccessibleCells(school)));
            }
        }
    }

    private void createRandomMap() {

        int nbCasesDispos = getGrid().getNOceanCell();

        if (areaSize >= nbCasesDispos) {
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
            //Cell[] tabCellsArea = new Cell[speciesAreasSizeTab[numSerie][iSpec]];
            int coordi, coordj;
            coordi = (int) Math.round(Math.random() * (getGrid().get_ny() - 1));
            coordj = (int) Math.round(Math.random() * (getGrid().get_nx() - 1));
            while (getGrid().getCell(coordi, coordj).isLand()) {
                coordi = (int) Math.round(Math.random() * (getGrid().get_ny() - 1));
                coordj = (int) Math.round(Math.random() * (getGrid().get_nx() - 1));
            }
            randomMap.add(getGrid().getCell(coordi, coordj));
            alreadyChoosen[coordi][coordj] = true;
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
                        if (!cell.isLand() && !alreadyChoosen[cell.get_igrid()][cell.get_jgrid()]) {
                            index++;
                            alreadyChoosen[cell.get_igrid()][cell.get_jgrid()] = true;
                            randomMap.add(cell);
                        }
                    }
                }
                iFirstSorted = iLastSorted + 1;
                iLastSorted = index;
            }
        }
    }
}
