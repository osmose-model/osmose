
package fr.ird.osmose.filter;

import fr.ird.osmose.School;

/**
 *
 * @author pverley
 */
public class PresentSchoolFilter  implements IFilter<School> {

    @Override
    public boolean accept(School school) {
        return school.isAlive() && !school.isUnlocated();
    }
    
}
