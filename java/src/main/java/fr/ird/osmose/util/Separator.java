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

package fr.ird.osmose.util;

import au.com.bytecode.opencsv.CSVReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * List of accepted separators in a parameter <i>key separator value</i> and in
 * an array of values <i>key separator1 value1 separator2 value2 separator2
 * value3 separator2 value4</i>.
 * <ul>
 * <li>equals =</li>
 * <li>semicolon ;</li>
 * <li>coma ,</li>
 * <li>colon :</li>
 * <li>tab \t</li>
 * </ul>
 */
public enum Separator {

    EQUALS('='),
    SEMICOLON(';'),
    COMA(','),
    COLON(':'),
    TAB('\t');
    private final char separator;

    private Separator(char separator) {
        this.separator = separator;
    }

    @Override
    public String toString() {
        return String.valueOf(separator);
    }

    public char getSeparator() {
        return separator;
    }

    public static String asList() {
        StringBuilder list = new StringBuilder();
        for (Separator sep : Separator.values()) {
            list.append(sep.name()).append(" ");
        }
        return list.toString();
    }

    /**
     * This function tries to guess what is the separator in the given string
     * assuming that it is an array of at least two values. It will look for
     * separators {@code = ; : \t} in this order. If none of these separators
     * are found then it will return the fallback separator given as a
     * parameter.
     *
     * @param string, the string you assume to be an array of strings separator
     * by one of the accepted {@link Separator}.
     * @param fallback, the fallback separator returned by the function if the
     * guess fails
     * @see Separator
     * @return the separator contained in the {@code string}
     */
    public static Separator guess(String string, Separator fallback) {

        for (Separator guess : values()) {
            if (string.contains(guess.toString()) && string.split(guess.toString()).length >= 1) {
                return guess;
            }
        }
        return fallback;
    }

    /**
     * This function tries to guess what is the separator in the CSV file given
     * as argument. It will try separators {@code = ; : \t} in this order. The
     * guess is based on two tests:
     * <ol>
     * <li>line k of the CSV file split around the separator contains at least 2
     * elements</li>
     * <li>line k and line k+1 split around the separator contains the same
     * number of elements</li>
     * </ol>
     * The test is obviously fallible but will work in most unambiguous cases.
     *
     * @param filename, the path of the CSV file
     * @return the separator of the CSV file and {
     * @null} if the guess failed
     * @throws java.io.IOException if the guess is unsuccessful
     */
    public static Separator guess(String filename) throws IOException {

        for (Separator guess : Separator.values()) {
            try {
                CSVReader reader = new CSVReader(new FileReader(filename), guess.separator);
                List<String[]> lines = reader.readAll();
                int n = Math.min(lines.size(), 50);
                boolean consistant = true;
                for (int i = 0; i < n - 1; i++) {
                    if (!consistant) {
                        break;
                    }
                    consistant = (lines.get(i).length > 1) && (lines.get(i).length == lines.get(i + 1).length);
                }
                if (consistant) {
                    Logger.getLogger(Separator.class.getName()).log(Level.FINE, "CSV separator for {0} is {1}", new Object[]{filename, guess.name()});
                    return guess;
                }
            } catch (IOException ex) {
                Logger.getLogger(Separator.class.getName()).log(Level.SEVERE, "Could not read CSV file " + filename, ex);
            }
        }
        throw new IOException("Failed to guess CSV separator");
    }
}
