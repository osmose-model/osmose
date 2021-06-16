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
import fr.ird.osmose.util.Separator;
import fr.ird.osmose.util.OsmoseLinker;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author P.Verley (philippe.verley@ird.fr)
 * @version 3.0b 2013/09/01
 */
public class GenericTimeSeries extends OsmoseLinker {

    private double[] values;

    public void read(String filename, int nValues) {

        try {
            // 1. Open the CSV file
            CSVReader reader = new CSVReader(new FileReader(filename), Separator.guess(filename).getSeparator());
            List<String[]> lines = reader.readAll();

            // 2. Check the length of the time serie and inform the user about potential problems or inconsistencies
            int nTimeSerie = lines.size() - 1;
            if ((nTimeSerie != nValues)) {
                throw new IOException("Found " + nTimeSerie + " time steps in the time serie. It must contain " + nValues + " time steps.");
            }

            // 3. Read the time series
            values = new double[nValues];
            for (int t = 0; t < nValues; t++) {
                String[] line = lines.get(t + 1);
                values[t] = Double.valueOf(line[1]);
            }
            
        } catch (IOException ex) {
            error("Error reading CSV file " + filename, ex);
        }
    }

    public double[] getValues() {
        return values;
    }
}
