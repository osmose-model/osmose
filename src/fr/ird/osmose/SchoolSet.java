package fr.ird.osmose;

import fr.ird.osmose.grid.IGrid;
import fr.ird.osmose.util.filter.AliveSchoolFilter;
import fr.ird.osmose.util.filter.DeadSchoolFilter;
import fr.ird.osmose.util.filter.FilteredSet;
import fr.ird.osmose.util.filter.FilteredSets;
import fr.ird.osmose.util.filter.IFilter;
import fr.ird.osmose.util.filter.PresentSchoolFilter;
import fr.ird.osmose.util.filter.SpeciesFilter;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author pverley
 */
public class SchoolSet extends FilteredSet<School> {

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
        return schoolMap[cell.get_jgrid()][cell.get_igrid()];
    }
    
    /**
     * Get a list of the schools that are physically located on the grid at 
     * current time step.
     * 
     * @return a List of the schools present on the grid at current time step
     */
    public List<School> getPresentSchools() {
        return FilteredSets.subset(this, new PresentSchoolFilter());
    }
    
    /**
     * Get a list of the alive schools.
     * 
     * @return a list of the alive schools
     */
    public List<School> getAliveSchools() {
        return FilteredSets.subset(this, new AliveSchoolFilter());
    }

    /**
     * Take a snapshot of the distribution of the schools on the grid
     */
    public void updateSchoolMap() {

        if (null == schoolMap) {
            schoolMap = new ArrayList[getGrid().get_ny()][getGrid().get_nx()];
        }

        // reset the map
        for (int j = 0; j < getGrid().get_ny(); j++) {
            for (int i = 0; i < getGrid().get_nx(); i++) {
                if (!getGrid().getCell(i, j).isLand()) {
                    if (null == schoolMap[j][i]) {
                        schoolMap[j][i] = new ArrayList();
                    } else {
                        schoolMap[j][i].clear();
                    }
                }
            }
        }

        // fill up the map
        for (School school : this) {
            if (!school.isUnlocated()) {
                schoolMap[school.getCell().get_jgrid()][school.getCell().get_igrid()].add(school);
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
