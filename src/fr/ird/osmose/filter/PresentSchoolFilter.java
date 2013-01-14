
package fr.ird.osmose.filter;

import fr.ird.osmose.School;

/**
 *
 * @author pverley
 */
public class PresentSchoolFilter  implements IFilter<School> {
    
    private int currentTimeStep;
    
    public PresentSchoolFilter(int currentTimeStep) {
        this.currentTimeStep = currentTimeStep;
    }
    

    @Override
    public boolean accept(School school) {
        return school.isAlive() && !school.getSpecies().isOut(school.getAgeDt(), currentTimeStep);
    }
    
}
