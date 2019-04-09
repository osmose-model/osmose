/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.util.filter;

import fr.ird.osmose.School;

/** 
 * To keep only schools which reach their maximum age (LifespanDt -1).
 * @author amorell
 */
public class OldSchoolFilter implements IFilter<School>{

    @Override
    public boolean accept(School school) {
        return (school.getAgeDt() > school.getSpecies().getLifespanDt()-2);
    }
}
