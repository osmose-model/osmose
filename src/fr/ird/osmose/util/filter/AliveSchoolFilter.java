package fr.ird.osmose.util.filter;

import fr.ird.osmose.School;

/**
 *
 * @author pverley
 */
public class AliveSchoolFilter implements IFilter<School> {

    @Override
    public boolean accept(School school) {
        return school.isAlive();
    }
    
}
