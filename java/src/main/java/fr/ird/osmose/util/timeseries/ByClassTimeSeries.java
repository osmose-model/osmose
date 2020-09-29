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

package fr.ird.osmose.util.timeseries;

import au.com.bytecode.opencsv.CSVReader;
import fr.ird.osmose.util.OsmoseLinker;
import fr.ird.osmose.util.Separator;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author pverley
 */
public class ByClassTimeSeries extends OsmoseLinker {

    private float[] classes;
    private double[][] values;

    public void read(String filename) {
        int nStepYear = getConfiguration().getNStepYear();
        int nStepSimu = getConfiguration().getNStep();
        read(filename, nStepYear, nStepSimu);
    }

    public void read(String filename, int nMin, int nMax) {

        int nStepYear = getConfiguration().getNStepYear();
        int nStepSimu = getConfiguration().getNStep();
        try {
            // 1. Open the CSV file
            CSVReader reader = new CSVReader(new FileReader(filename), Separator.guess(filename).getSeparator());
            List<String[]> lines = reader.readAll();

            // 2. Read the threshold values
            String[] lineThreshold = lines.get(0);
            classes = new float[lineThreshold.length - 1];
            for (int k = 0; k < classes.length; k++) {
                classes[k] = Float.valueOf(lineThreshold[k + 1]);
            }

            // 3. Check the length of the time serie and inform the user about potential problems or inconsistencies
            int nTimeSerie = lines.size() - 1;
            if ((nTimeSerie != nStepSimu) && (nTimeSerie < nMin)) {
                throw new IOException("Found " + nTimeSerie + " time steps in the time serie. It must contain at least " + nMin + " time steps.");
            }
            if ((nTimeSerie != nStepSimu) && (nTimeSerie % nStepYear != 0)) {
                throw new IOException("Found " + nTimeSerie + " time steps in the time serie. It must be a multiple of the number of time steps per year.");
            }
            if (nTimeSerie > nMax) {
                debug("Time serie in file {0} contains {1} steps out of {2}. Osmose will ignore the exceeding steps.", new Object[]{filename, nTimeSerie, nMax});
            }
            nTimeSerie = Math.min(nTimeSerie, nMax);

            // 3. Read the mortality rates
            values = new double[nStepSimu][];
            for (int t = 0; t < nTimeSerie; t++) {
                values[t] = new double[lineThreshold.length - 1];
                String[] fval = lines.get(t + 1);
                for (int k = 0; k < values[t].length; k++) {
                    values[t][k] = Double.valueOf(fval[k + 1]);
                }
            }
            // 4. Fill up the time serie if necessary
            if (nTimeSerie < nStepSimu) {
                // There is less season in the file than number of years of the
                // simulation.
                int t = nTimeSerie;
                while (t < nStepSimu) {
                    for (int k = 0; k < nTimeSerie; k++) {
                        values[t] = values[k];
                        t++;
                        if (t == nStepSimu) {
                            break;
                        }
                    }
                }
                // 
                debug("Time serie in file {0} only contains {1} steps out of {2}. Osmose will loop over it.", new Object[]{filename, nTimeSerie, nStepSimu});
            }
        } catch (IOException ex) {
            error("Error reading CSV file " + filename, ex);
        }
    }

    public int classOf(float school) {
        // 1. value < first threshold, index does not exist
        if (school < classes[0]) {
            return -1;
        }
        // 2. Normal case thresold[k] <= value < threshold[k+1]
        for (int k = 0; k < classes.length - 1; k++) {
            if ((classes[k] <= school) && (school < classes[k + 1])) {
                return k;
            }
        }
        // 3. value >= threshold[last]
        return classes.length - 1;
    }

    public double getValue(int step, float school) {
        return values[step][classOf(school)];
    }

    public int getNClass() {
        return classes.length;
    }

    public float getClass(int k) {
        return classes[k];
    }

    public float[] getClasses() {
        return classes;
    }

    public double[][] getValues() {
        return values;
    }
}
