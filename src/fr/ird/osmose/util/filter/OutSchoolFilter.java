/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.util.filter;

import fr.ird.osmose.School;

/**
 *
 * @author pverley
 */
public class OutSchoolFilter implements IFilter<School> {

    @Override
    public boolean accept(School school) {
        return school.isAlive() && school.isOut();
    }
    
}
