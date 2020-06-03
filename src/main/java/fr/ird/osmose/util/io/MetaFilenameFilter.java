/* 
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
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
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
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
