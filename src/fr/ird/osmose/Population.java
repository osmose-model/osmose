package fr.ird.osmose;

import fr.ird.osmose.filter.DeadSchoolFilter;
import java.util.List;

/**
 *
 * @author pverley
 */
public class Population extends FilteredSet<School> {
    
    public void removeDeadSchools() {
        List<School> schoolsToRemove = FilteredSets.subset(this, new DeadSchoolFilter());
        removeAll(schoolsToRemove);
    }
}
