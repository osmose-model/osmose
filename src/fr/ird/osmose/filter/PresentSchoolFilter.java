
package fr.ird.osmose.filter;

import fr.ird.osmose.School;
import fr.ird.osmose.process.MovementProcess;

/**
 *
 * @author pverley
 */
public class PresentSchoolFilter  implements IFilter<School> {

    @Override
    public boolean accept(School school) {
        return school.isAlive() && !MovementProcess.isOut(school);
    }
    
}
