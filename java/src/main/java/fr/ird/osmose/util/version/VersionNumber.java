/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine Ecosystems)
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
         *
         */
        private static final long serialVersionUID = 6543003129025100357L;

        /**
         * Constructs a <code>VersionNumberFormatException</code> with no detail
         * message.
         */
        /*public VersionNumberFormatException() {
            super();
        }
        */

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
