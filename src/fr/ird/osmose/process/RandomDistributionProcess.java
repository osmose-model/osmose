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

    private MovementProcess parent;
    private Species species;
    private int areaSize;
    private List<Cell> randomMap;

    public RandomDistributionProcess(Species species, MovementProcess parent) {
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
        for (School school : getPopulation().getSchools(species)) {
            if (school.isUnlocated()) {
                school.moveToCell(parent.randomDeal(randomMap));
            } else {
                school.moveToCell(parent.randomDeal(parent.getAccessibleCells(school)));
            }
        }
    }

    private void createRandomMap() {

        int nbCasesDispos = getGrid().getNumberAvailableCells();

        if (areaSize >= nbCasesDispos) {
            /*
             * Whole grid
             */
            areaSize = nbCasesDispos;

            randomMap = new ArrayList(areaSize);
            for (int l = 0; l < getGrid().getNbLines(); l++) {
                for (int m = 0; m < getGrid().getNbColumns(); m++) {
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
            boolean[][] alreadyChoosen = new boolean[getGrid().getNbLines()][getGrid().getNbColumns()];
            //Cell[] tabCellsArea = new Cell[speciesAreasSizeTab[numSerie][iSpec]];
            int coordi, coordj;
            coordi = (int) Math.round(Math.random() * (getGrid().getNbLines() - 1));
            coordj = (int) Math.round(Math.random() * (getGrid().getNbColumns() - 1));
            while (getGrid().getCell(coordi, coordj).isLand()) {
                coordi = (int) Math.round(Math.random() * (getGrid().getNbLines() - 1));
                coordj = (int) Math.round(Math.random() * (getGrid().getNbColumns() - 1));
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
