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
package fr.ird.osmose.util.version;

/**
 *
 * @author pverley
 */
public class VersionNumber implements Comparable<VersionNumber> {

    final int major;
    final int minor;
    final int patch;

    public VersionNumber(int major, int minor, int patch) {

        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    /**
     * Returns an {@code VersionNumber} object holding the value of the
     * specified {@code String}.
     *
     * @param s the string to be parsed.
     * @return an {@code VersionNumber} object holding the value represented by
     * the string argument.
     * @exception VersionNumberFormatException if the string cannot be parsed as
     * a version number.
     */
    public static VersionNumber valueOf(String s) throws VersionNumberFormatException {

        if (s == null) {
            throw new VersionNumberFormatException("null");
        }

        if (!s.isEmpty()) {
            try {
                int smajor, sminor = 0, spatch = 0;
                String[] numbers = s.split("\\.");
                smajor = Integer.valueOf(numbers[0]);
                if (numbers.length > 1) {
                    sminor = Integer.valueOf(numbers[1]);
                }
                if (numbers.length > 2) {
                    spatch = Integer.valueOf(numbers[2]);
                }
                return new VersionNumber(smajor, sminor, spatch);
            } catch (NumberFormatException ex) {
                throw VersionNumberFormatException.forInputString(s);
            }
        } else {
            throw VersionNumberFormatException.forInputString(s);
        }
    }

    @Override
    public int compareTo(VersionNumber otherVersion) {

        // Compare major version number
        if (major != otherVersion.major) {
            return Integer.compare(major, otherVersion.major);
        }

        // Same major version number, compare minor version number
        if (minor != otherVersion.minor) {
            return Integer.compare(minor, otherVersion.minor);
        }

        // Same major/minor version number, compare patch number
        if (patch != otherVersion.patch) {
            return Integer.compare(patch, otherVersion.patch);
        }

        // Same major, minor and patch version numbers, versions are equal
        return 0;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }

    /**
     * Thrown to indicate that the application has attempted to convert a string
     * to version number {major}.{minor}.{patch}, but that the string does not
     * have the appropriate format. Copy of NumberFormatException.java
     */
    private static class VersionNumberFormatException extends IllegalArgumentException {

        /**
         * Constructs a <code>VersionNumberFormatException</code> with no detail
         * message.
         */
        public VersionNumberFormatException() {
            super();
        }

        /**
         * Constructs a <code>VersionNumberFormatException</code> with the
         * specified detail message.
         *
         * @param s the detail message.
         */
        public VersionNumberFormatException(String s) {
            super(s);
        }

        /**
         * Factory method for making a <code>VersionNumberFormatException</code>
         * given the specified input which caused the error.
         *
         * @param s the input causing the error
         */
        static VersionNumberFormatException forInputString(String s) {
            return new VersionNumberFormatException("For input string: \"" + s + "\" (expected {major}(.{minor}(.{patch})))");
        }
    }
}
