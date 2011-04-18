/********************************************************************************
 * <p>Titre : SchoolLengthComparator class</p>
 *
 * <p>Description : comparate school length </p>
 *
 * <p>Copyright : Copyright (c) 2011</p>
 *
 * <p>Society : IRD, France </p>
 *
 * @author Philippe Verley
 * @version 2.1
 ********************************************************************************
 */
package fr.ird.osmose.util;

import fr.ird.osmose.School;
import java.util.Comparator;

/**
 *
 * @author pverley
 */
public class SchoolLengthComparator implements Comparator<School> {

    @Override
    public int compare(School school1, School school2) {
        return Float.compare(school1.getLength(), school2.getLength());
    }
}
