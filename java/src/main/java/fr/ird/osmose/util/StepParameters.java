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
 * Class that manages the initialisation of step parameters.
 * 
 * Seasons can be provided as a list of steps
 * 
 * Seasons that exceed the number of steps per year are discarded.
 *
 * @author Nicolas
 */
public class StepParameters extends OsmoseLinker {

    private final String prefix;
    private final String suffix;
    private int[] season;

    public StepParameters(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
        this.init();
    }

    public void init() {
        
        int ymax, ymin;
        int[] tempYears;

        Configuration conf = this.getConfiguration();

        int nseason = getConfiguration().getNStepYear();
        String key = String.format("%s.steps.%s", this.prefix, this.suffix);
        
        int[] tmpSeason;
        if (!conf.isNull(key)) {
            tmpSeason = conf.getArrayInt(key);
        } else {
            tmpSeason = new int[nseason];
            for (int s = 0; s < nseason; s++) {
                tmpSeason[s] = s;
            }
        }
        
        int goodseason = 0;
        for(int s : tmpSeason) {
            if(s < nseason) {
                goodseason++;
            }
        }
        
        season = new int[goodseason];
        int cpt = 0;
        for (int s : tmpSeason) {
            if (s < nseason) {
                season[cpt] = s;
                cpt++;
            }
        }
        
    }

    public int[] getSeasons() {
        return this.season;
    }

}
