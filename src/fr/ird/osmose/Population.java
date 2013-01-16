package fr.ird.osmose;

import fr.ird.osmose.filter.AliveSchoolFilter;
import fr.ird.osmose.filter.DeadSchoolFilter;
import fr.ird.osmose.filter.IFilter;
import fr.ird.osmose.filter.SpeciesFilter;
import fr.ird.osmose.grid.IGrid;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author pverley
 */
public class Population extends FilteredSet<School> {

///////////////////////////////
// Declaration of the variables
///////////////////////////////
    /*
     * Snapshot of the distribution of the schools on the grid
     */
    private List<School>[][] schoolMap;

///////////////////////////////
// Definition of the functions
///////////////////////////////
    /**
     * Remove dead schools from the population
     */
    public void removeDeadSchools() {
        List<School> schoolsToRemove = FilteredSets.subset(this, new DeadSchoolFilter());
        removeAll(schoolsToRemove);
    }

    public List<School> getSchools(Species species) {
        return FilteredSets.subset(this, new IFilter[]{new SpeciesFilter(species.getIndex()), new AliveSchoolFilter()});
    }

    /**
     * Get the list of the schools that are located in the given cell at current
     * time.
     *
     * @param cell, a cell of the grid
     * @return the list of the schools located in the cell. The list might be
     * empty if it contains no school or null if the cell is on land.
     */
    public List<School> getSchools(Cell cell) {
        return schoolMap[cell.get_igrid()][cell.get_jgrid()];
    }

    /**
     * Take a snapshot of the distribution of the schools on the grid
     */
    public void updateSchoolMap() {

        if (null == schoolMap) {
            schoolMap = new ArrayList[getGrid().getNbLines()][getGrid().getNbColumns()];
        }

        // reset the map
        for (int i = 0; i < getGrid().getNbLines(); i++) {
            for (int j = 0; j < getGrid().getNbColumns(); j++) {
                if (!getGrid().getCell(i, j).isLand()) {
                    if (null == schoolMap[i][j]) {
                        schoolMap[i][j] = new ArrayList();
                    } else {
                        schoolMap[i][j].clear();
                    }
                }
            }
        }

        // fill up the map
        for (School school : this) {
            if (!school.isUnlocated()) {
                schoolMap[school.getCell().get_igrid()][school.getCell().get_jgrid()].add(school);
            }
        }
    }

    /**
     * Returns an instance of the grid
     */
    private IGrid getGrid() {
        return Osmose.getInstance().getGrid();
    }
}
