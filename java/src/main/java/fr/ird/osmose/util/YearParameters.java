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

import fr.ird.osmose.Configuration;

/** 
 * Class that manages the initialisation of year parameters.
 * 
 * Years can be provided as a list of years or as an initial and final year.
 * 
 * Years that exceed the number of simulated years are discarded.
 *
 * @author Nicolas
 */
public class YearParameters extends OsmoseLinker {

    /** Prefix of the parameters. */
    private final String prefix;
    /** Prefix of the parameters. */
    private final String suffix;
    private int[] years;

    public YearParameters(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
        this.init();
    }

    public void init() {
        
        String key;
        int ymax, ymin;
        int[] tempYears;

        Configuration conf = this.getConfiguration();

        int nseason = getConfiguration().getNStepYear();
        int nyear = (int) Math.ceil(this.getConfiguration().getNStep() / (float) nseason);

        key = String.format("%s.years.%s", this.prefix, this.suffix);
        if (!conf.isNull(key)) {
            tempYears = conf.getArrayInt(key);    
        } else {           
            key = String.format("%s.initialYear.%s", this.prefix, this.suffix);
            if (!conf.isNull(key)) {
                ymin = conf.getInt(key);
            } else {
                ymin = 0;
            }

            key = String.format("%s.lastYear.%s", this.prefix, this.suffix);
            if (!conf.isNull(key)) {
                ymax = conf.getInt(key);
            } else {
                ymax = nyear - 1;
            }

            int nyears = ymax - ymin + 1;
            tempYears = new int[nyears];
            int cpt = 0;
            for (int y = ymin; y < ymax + 1; y++) {
                tempYears[cpt] = y;
                cpt++;
            }
        }

        // Get rid off years that are beyond number of simulated years.
        int goodyear = 0;
        for (int y : tempYears) {
            if (y < nyear) {
                goodyear++;
            }
        }

        years = new int[goodyear];
        int cpt = 0;
        for (int y : tempYears) {
            if (y < nyear) {
                years[cpt] = y;
                cpt++;
            }
        }
    }

    public int[] getYears() {
        return this.years;
    }

}
