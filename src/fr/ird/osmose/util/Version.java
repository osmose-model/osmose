/*
 * OSMOSE (Object-oriented Simulator of Marine ecOSystems Exploitation)
 * http://www.osmose-model.org
 * 
 * Copyright (c) IRD (Institut de Recherche pour le Développement) 2009-2013
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * 
 * This software is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, natural and
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
package fr.ird.osmose.util;

import java.text.ParseException;

/**
 *
 * @author pverley
 */
public class Version {

    /*
     * Careful with the dates. It should reflect the last change done in a
     * given version.
     * Osmose always expect that the dates of older version are always prior to
     * newer versions.
     */
    final public static Version v3_0 = new Version("3.0", "2013/09/01");
    //final public static Version v3_1 = new Version("3.1", "201?/??/??");
    //final public static Version v3_? = new Version("3.?", "201?/??/??");
    /**
     * List of all the versions
     */
    final public static Version[] VERSIONS = new Version[]{v3_0};
    /*
     * Current version is always the newest one.
     */
    final public static Version CURRENT = VERSIONS[VERSIONS.length - 1];
    /*
     * 
     */
    private final String number;
    private final String[] dates;

    public Version(String number, String... dates) {
        this.dates = (dates.length == 0) ? null : dates;
        this.number = number;
    }

    public boolean priorTo(Version version) {
        if (null == dates) {
            /*
             * I am an undated version so we can assume that I am older.
             */
            return true;
        } else if (null == version.dates) {
            /*
             * I am a dated version and I am compared to an undated version so
             * we can assume that I am newer.
             */
            return false;
        } else {
            /*
             * We both are dated versions so let's compare dates.
             */
            return (dates[dates.length - 1].compareTo(version.dates[version.dates.length - 1]) < 0);
        }
    }

    public String getNumber() {
        return number;
    }

    public String getDate() {
        return dates[dates.length - 1];
    }

    public String getReleaseDate() {
        return dates[0];
    }

    public static Version parse(String sversion) throws ParseException {
        String[] split = sversion.split(" ");
        boolean numberValidated = false;
        Version version = null;
        for (Version oversion : VERSIONS) {
            if (oversion.number.equals(split[0])) {
                numberValidated = true;
                version = oversion;
                break;
            }
        }
        // Version number does not match any of the predefined version numbers
        if (!numberValidated) {
            throw new ParseException("Version number " + split[0] + " is not a valid Osmose version number.", 0);
        }
        // Version number is OK, let's check the date
        String date = split.length > 1 ? split[1].trim() : null;
        if (null != date) {
            if (date.startsWith("(") && date.endsWith(")")) {
                date = date.substring(1, date.length() - 1);
            }
            for (String odate : version.dates) {
                if (date.equals(odate)) {
                    return new Version(version.getNumber(), date);
                }
            }
        }
        throw new ParseException("Version date " + date + " is not a valid Osmose version date.", split[0].length());
    }

    public String toShortString() {
        StringBuilder version = new StringBuilder();
        version.append(number);
        if (null != dates) {
            version.append(" (");
            version.append(dates[dates.length - 1]);
            version.append(")");
        }
        return version.toString();
    }

    @Override
    public String toString() {
        StringBuilder version = new StringBuilder();
        version.append(number);
        if (null != dates) {
            if (dates.length > 1) {
                version.append(" update ");
                version.append(dates.length - 1);
            }
            version.append(" (");
            version.append(dates[dates.length - 1]);
            version.append(")");
        } else {
            version.append(" (undated)");
        }
        return version.toString();
    }
}
