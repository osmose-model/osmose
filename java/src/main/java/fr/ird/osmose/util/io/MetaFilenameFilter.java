/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (C) IRD (Institut de Recherche pour le Développement) 2009-2020
 * 
 * Osmose is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, additional and
 * starvation mortalities, reproduction and migration) and fishing mortalities
 * (Shin and Cury 2001, 2004).
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License). Full description
 * is provided on the LICENSE file.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * 
 */

package fr.ird.osmose.util.io;

/** import java.util */
import java.util.regex.Pattern;

/** import java.io */
import java.io.File;

/**
 * This file name filter only uses shell meta-characters.
 * It accepts the following meta-character:
 * <ul>
 * <li> <b>?</b> for any single character
 * <li> <b>*</b> for any String.
 * </ul>
 * The class manipulates regex.
 *
 * <p>Copyright: Copyright (c) 2007 - Free software under GNU GPL</p>
 *
 * @author adiGuba (http://adiguba.developpez.com/)
 * @see java.io.FilenameFilter
 */
public class MetaFilenameFilter implements java.io.FilenameFilter {

///////////////////////////////
// Declaration of the variables
///////////////////////////////

    /**
     * The regex pattern
     */
    private final Pattern pattern;

///////////////
// Constructors
///////////////

    /**
     * Constructs a new MetaFilenameFilter with the specified file mask.
     *
     * @param fileMask The file mask String.
     */
    public MetaFilenameFilter(String fileMask) {

        /** Add \Q \E around substrings of fileMask that are not
         * meta-characters */
        String regexpPattern = fileMask.replaceAll("[^\\*\\?]+", "\\\\Q$0\\\\E");
        /** Replace all "*" by the corresponding java regex meta-characters */
        regexpPattern = regexpPattern.replaceAll("\\*", ".*");
        /** Replace all "?" by the corresponding java regex meta-characters */
        regexpPattern = regexpPattern.replaceAll("\\?", ".");
        /** Create the pattern */
        this.pattern = Pattern.compile(regexpPattern);
    }

    /**
     * Tests if a specified file should be included in a file list.
     *
     * @param   dir    the directory in which the file was found, not used here
     * @param   name   the name of the file.
     * @return  <code>true</code> if and only if the name matches the pattern;
     * <code>false</code> otherwise.
     */
    public boolean accept(File dir, String name) {
        return pattern.matcher(name).matches();
    }

    //---------- End of class
}
