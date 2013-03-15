package fr.ird.osmose.util;

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
 * @author P.Verley
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
